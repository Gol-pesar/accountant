package com.example.data.backup

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.AccountingDao
import com.example.data.local.BankAccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CustomBankEntity
import com.example.data.local.PersonEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class BackupStats(
    val transactionCount: Int,
    val categoryCount: Int,
    val personCount: Int,
    val bankAccountCount: Int,
    val savingsGoalCount: Int
)

data class AutoBackupWriteResult(
    val stats: BackupStats,
    val savedMediaUri: String?
)

sealed class AutoRestoreResult {
    data class Restored(val stats: BackupStats, val sourceLabel: String) : AutoRestoreResult()
    data class NeedsFolderAccess(val message: String) : AutoRestoreResult()
    data class NotFound(val message: String) : AutoRestoreResult()
    data class Error(val message: String) : AutoRestoreResult()
}

/**
 * Manages BOTH:
 * 1) Real-time automatic backup into a persistent public folder named "من را حذف نکنید" inside Documents
 *    (survives app uninstall, zero storage permissions required on Android 10+).
 * 2) One-click Auto Restore from "من را حذف نکنید" (with seamless SAF folder confirmation on Android 11+
 *    after uninstall/reinstall) AND manual SAF/Google Drive file export & restore.
 */
class DriveBackupManager(
    private val context: Context,
    private val dao: AccountingDao
) {
    companion object {
        const val AUTO_BACKUP_FOLDER_NAME = "من را حذف نکنید"
        const val AUTO_BACKUP_FILE_NAME = "hesabdar_auto_backup.json"
        private const val AUTO_BACKUP_PREFIX = "hesabdar_auto_backup"
        private const val TAG = "DriveBackupManager"

        /**
         * Builds an initial URI pointing directly to `Documents/من را حذف نکنید` for ACTION_OPEN_DOCUMENT_TREE.
         */
        fun buildInitialFolderUri(): Uri? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching {
                    DocumentsContract.buildDocumentUri(
                        "com.android.externalstorage.documents",
                        "primary:${Environment.DIRECTORY_DOCUMENTS}/$AUTO_BACKUP_FOLDER_NAME"
                    )
                }.getOrNull()
            } else null
        }
    }

    private val autoBackupMutex = Mutex()

    // =========================================================================
    // 1. Manual Export & Manual Restore (SAF / Google Drive / Any Chosen File)
    // =========================================================================

    suspend fun exportBackupToUri(uri: Uri): Result<BackupStats> = withContext(Dispatchers.IO) {
        runCatching {
            val (jsonText, stats) = buildBackupJson()
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(jsonText.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(jsonText.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: error("امکان نوشتن در فایل انتخابی وجود نداشت")
            stats
        }
    }

    suspend fun restoreBackupFromUri(uri: Uri): Result<BackupStats> = withContext(Dispatchers.IO) {
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("امکان خواندن فایل پشتیبان وجود نداشت")
            restoreFromJsonString(content)
        }
    }

    // =========================================================================
    // 2. Real-Time Auto-Backup to Persistent Public Folder ("من را حذف نکنید")
    // =========================================================================

    suspend fun writeRealtimeAutoBackup(
        savedTreeUri: String,
        savedMediaUri: String
    ): Result<AutoBackupWriteResult> = withContext(Dispatchers.IO) {
        autoBackupMutex.withLock {
            runCatching {
                val (jsonText, stats) = buildBackupJson()
                val bytes = jsonText.toByteArray(Charsets.UTF_8)

                // Tier 1: If user granted persistent SAF access to the "من را حذف نکنید" folder, write directly there
                if (savedTreeUri.isNotBlank()) {
                    val wroteTree = writeToSafTreeFolder(Uri.parse(savedTreeUri), bytes)
                    if (wroteTree) {
                        return@runCatching AutoBackupWriteResult(stats = stats, savedMediaUri = savedMediaUri.ifBlank { null })
                    }
                }

                // Tier 2: On Android 10+ (API 29+), write via MediaStore.Files to Documents/من را حذف نکنید (zero permissions, survives uninstall)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val updatedUri = writeToMediaStoreDocuments(bytes, savedMediaUri)
                    if (updatedUri != null) {
                        return@runCatching AutoBackupWriteResult(stats = stats, savedMediaUri = updatedUri)
                    }
                }

                // Tier 3: On Android 7-9 (API 24-28), write to public Documents/من را حذف نکنید if writable
                val legacyWritten = writeToLegacyPublicDocuments(bytes)
                if (legacyWritten) {
                    return@runCatching AutoBackupWriteResult(stats = stats, savedMediaUri = savedMediaUri.ifBlank { null })
                }

                error("دسترسی نوشتن در پوشه «$AUTO_BACKUP_FOLDER_NAME» نیاز به تأیید کاربر دارد")
            }
        }
    }

    /**
     * One-click Auto Restore: searches for the latest valid backup inside `Documents/من را حذف نکنید`.
     * Returns [AutoRestoreResult.NeedsFolderAccess] if Android 11+ Scoped Storage requires the user
     * to confirm folder access after an uninstall + reinstall.
     */
    suspend fun restoreFromAutoBackupFolder(
        savedTreeUri: String,
        savedMediaUri: String
    ): AutoRestoreResult = withContext(Dispatchers.IO) {
        // Tier 1: Persisted SAF folder tree URI (works even after reinstall once linked)
        if (savedTreeUri.isNotBlank()) {
            val fromTree = readAndRestoreFromSafTree(Uri.parse(savedTreeUri))
            if (fromTree != null) {
                return@withContext AutoRestoreResult.Restored(
                    stats = fromTree,
                    sourceLabel = "پوشه «$AUTO_BACKUP_FOLDER_NAME»"
                )
            }
        }

        // Tier 2: Persisted MediaStore URI
        if (savedMediaUri.isNotBlank()) {
            val res = runCatching {
                val content = context.contentResolver.openInputStream(Uri.parse(savedMediaUri))?.use {
                    it.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!content.isNullOrBlank()) restoreFromJsonString(content) else null
            }.getOrNull()
            if (res != null) {
                return@withContext AutoRestoreResult.Restored(
                    stats = res,
                    sourceLabel = "Documents / $AUTO_BACKUP_FOLDER_NAME"
                )
            }
        }

        // Tier 3: Query MediaStore.Files in Documents/من را حذف نکنید/ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fromMediaStore = readAndRestoreFromMediaStoreDocuments()
            if (fromMediaStore != null) {
                return@withContext AutoRestoreResult.Restored(
                    stats = fromMediaStore,
                    sourceLabel = "Documents / $AUTO_BACKUP_FOLDER_NAME"
                )
            }
        }

        // Tier 4: Direct public Documents file check (API 24-28 or permissive devices)
        val fromLegacy = readAndRestoreFromLegacyPublicDocuments()
        if (fromLegacy != null) {
            return@withContext AutoRestoreResult.Restored(
                stats = fromLegacy,
                sourceLabel = "Documents / $AUTO_BACKUP_FOLDER_NAME"
            )
        }

        // Tier 5: On Android 10+ after uninstall/reinstall, Scoped Storage hides files created by the
        // previous install until the user confirms access to Documents/من را حذف نکنید once.
        AutoRestoreResult.NeedsFolderAccess(
            "برای دسترسی به فایل‌های پوشه «$AUTO_BACKUP_FOLDER_NAME» پس از نصب مجدد، در صفحه بعد پوشه «$AUTO_BACKUP_FOLDER_NAME» را انتخاب کرده و «استفاده از این پوشه» را بزنید."
        )
    }

    /**
     * Restores the latest backup JSON from a user-confirmed SAF tree URI (Documents/من را حذف نکنید).
     */
    suspend fun restoreFromSelectedTreeUri(treeUri: Uri): AutoRestoreResult = withContext(Dispatchers.IO) {
        try {
            val stats = readAndRestoreFromSafTree(treeUri)
            if (stats != null) {
                AutoRestoreResult.Restored(
                    stats = stats,
                    sourceLabel = "پوشه «$AUTO_BACKUP_FOLDER_NAME»"
                )
            } else {
                AutoRestoreResult.NotFound(
                    "فایل پشتیبان معتبری در پوشه انتخاب‌شده پیدا نشد. مطمئن شوید پوشه «$AUTO_BACKUP_FOLDER_NAME» داخل Documents را انتخاب کرده‌اید."
                )
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error restoring from selected folder tree", t)
            AutoRestoreResult.Error(t.message ?: "خطا در بازیابی از پوشه پشتیبان")
        }
    }

    // =========================================================================
    // Storage Helpers (MediaStore, SAF DocumentsContract, Legacy Public Docs)
    // =========================================================================

    private fun writeToMediaStoreDocuments(bytes: ByteArray, existingSavedUri: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val resolver = context.contentResolver

        // 1. Try overwriting previously saved MediaStore URI in-place
        if (existingSavedUri.isNotBlank()) {
            val uri = Uri.parse(existingSavedUri)
            val wrote = runCatching {
                resolver.openOutputStream(uri, "wt")?.use { out ->
                    out.write(bytes)
                    out.flush()
                    true
                } ?: false
            }.getOrDefault(false)
            if (wrote) return existingSavedUri
        }

        // 2. Query MediaStore for an existing writable backup file in Documents/من را حذف نکنید
        val collection = MediaStore.Files.getContentUri("external")
        val existingUri = findMediaStoreBackupUris().firstOrNull()
        if (existingUri != null) {
            val wrote = runCatching {
                resolver.openOutputStream(existingUri, "wt")?.use { out ->
                    out.write(bytes)
                    out.flush()
                    true
                } ?: false
            }.getOrDefault(false)
            if (wrote) return existingUri.toString()
        }

        // 3. Create new MediaStore entry inside Documents/من را حذف نکنید/
        val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/$AUTO_BACKUP_FOLDER_NAME"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, AUTO_BACKUP_FILE_NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val insertedUri = runCatching { resolver.insert(collection, values) }.getOrNull() ?: return null
        return try {
            resolver.openOutputStream(insertedUri, "wt")?.use { out ->
                out.write(bytes)
                out.flush()
            } ?: return null
            val finalize = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(insertedUri, finalize, null, null)
            insertedUri.toString()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed writing to MediaStore backup file", t)
            runCatching { resolver.delete(insertedUri, null, null) }
            null
        }
    }

    private fun findMediaStoreBackupUris(): List<Uri> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
        val result = mutableListOf<Uri>()
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$AUTO_BACKUP_FOLDER_NAME%", "%.json")
        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val candidates = mutableListOf<Pair<Boolean, Uri>>()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                    val uri = ContentUris.withAppendedId(collection, id)
                    candidates.add(name.startsWith(AUTO_BACKUP_PREFIX, ignoreCase = true) to uri)
                }
                candidates
                    .sortedByDescending { it.first }
                    .forEach { result.add(it.second) }
            }
        }
        return result
    }

    private suspend fun readAndRestoreFromMediaStoreDocuments(): BackupStats? {
        val uris = findMediaStoreBackupUris()
        for (uri in uris) {
            val restored = runCatching {
                val content = context.contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!content.isNullOrBlank()) restoreFromJsonString(content) else null
            }.getOrNull()
            if (restored != null) return restored
        }
        return null
    }

    private fun resolveTargetDirDocId(treeUri: Uri): String {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        if (rootDocId.endsWith("/$AUTO_BACKUP_FOLDER_NAME") || rootDocId.endsWith(":$AUTO_BACKUP_FOLDER_NAME")) {
            return rootDocId
        }
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
                val idIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (c.moveToNext()) {
                    val name = if (nameIdx >= 0) c.getString(nameIdx).orEmpty() else ""
                    val mime = if (mimeIdx >= 0) c.getString(mimeIdx).orEmpty() else ""
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR && name == AUTO_BACKUP_FOLDER_NAME) {
                        val subId = if (idIdx >= 0) c.getString(idIdx) else null
                        if (!subId.isNullOrBlank()) return subId
                    }
                }
            }
        }
        return rootDocId
    }

    private fun writeToSafTreeFolder(treeUri: Uri, bytes: ByteArray): Boolean {
        return runCatching {
            val resolver = context.contentResolver
            val targetDirDocId = resolveTargetDirDocId(treeUri)
            val existingDocUri = findChildDocumentUrisInTree(treeUri, targetDirDocId)
                .firstOrNull { it.first.equals(AUTO_BACKUP_FILE_NAME, ignoreCase = true) }
                ?.second

            val targetUri = existingDocUri ?: run {
                val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, targetDirDocId)
                DocumentsContract.createDocument(resolver, parentUri, "application/json", AUTO_BACKUP_FILE_NAME)
            } ?: return@runCatching false

            resolver.openOutputStream(targetUri, "wt")?.use { out ->
                out.write(bytes)
                out.flush()
                true
            } ?: false
        }.getOrDefault(false)
    }

    private suspend fun readAndRestoreFromSafTree(treeUri: Uri): BackupStats? {
        val targetDirDocId = resolveTargetDirDocId(treeUri)
        val candidates = findChildDocumentUrisInTree(treeUri, targetDirDocId)
        for ((_, docUri) in candidates) {
            val restored = runCatching {
                val content = context.contentResolver.openInputStream(docUri)?.use {
                    it.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!content.isNullOrBlank()) restoreFromJsonString(content) else null
            }.getOrNull()
            if (restored != null) return restored
        }
        return null
    }

    private fun findChildDocumentUrisInTree(treeUri: Uri, parentDocId: String): List<Pair<String, Uri>> {
        val list = mutableListOf<Triple<String, Long, Uri>>()
        runCatching {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
                val idIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val modIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                while (c.moveToNext()) {
                    val docId = if (idIdx >= 0) c.getString(idIdx) else continue
                    val name = if (nameIdx >= 0) c.getString(nameIdx).orEmpty() else ""
                    val modified = if (modIdx >= 0 && !c.isNull(modIdx)) c.getLong(modIdx) else 0L
                    if (name.endsWith(".json", ignoreCase = true)) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        list.add(Triple(name, modified, docUri))
                    }
                }
            }
        }
        return list
            .sortedWith(
                compareByDescending<Triple<String, Long, Uri>> { it.first.startsWith(AUTO_BACKUP_PREFIX, ignoreCase = true) }
                    .thenByDescending { it.second }
            )
            .map { it.first to it.third }
    }

    @Suppress("DEPRECATION")
    private fun writeToLegacyPublicDocuments(bytes: ByteArray): Boolean {
        return runCatching {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val folder = File(docsDir, AUTO_BACKUP_FOLDER_NAME)
            if (!folder.exists() && !folder.mkdirs()) return@runCatching false
            val target = File(folder, AUTO_BACKUP_FILE_NAME)
            target.writeBytes(bytes)
            true
        }.getOrDefault(false)
    }

    @Suppress("DEPRECATION")
    private suspend fun readAndRestoreFromLegacyPublicDocuments(): BackupStats? {
        return runCatching {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val folder = File(docsDir, AUTO_BACKUP_FOLDER_NAME)
            if (!folder.exists() || !folder.isDirectory) return@runCatching null
            val files = folder.listFiles { f -> f.isFile && f.name.endsWith(".json", ignoreCase = true) }
                ?.sortedWith(
                    compareByDescending<File> { it.name.startsWith(AUTO_BACKUP_PREFIX, ignoreCase = true) }
                        .thenByDescending { it.lastModified() }
                ) ?: return@runCatching null
            for (file in files) {
                if (!file.canRead()) continue
                val restored = runCatching { restoreFromJsonString(file.readText(Charsets.UTF_8)) }.getOrNull()
                if (restored != null) return@runCatching restored
            }
            null
        }.getOrNull()
    }

    // =========================================================================
    // JSON Snapshot Serialization & Deserialization
    // =========================================================================

    private suspend fun buildBackupJson(): Pair<String, BackupStats> {
        val categories = dao.getCategoriesOnce()
        val people = dao.getPeopleOnce()
        val accounts = dao.getBankAccountsOnce()
        val transactions = dao.getTransactionsOnce()
        val goals = dao.getSavingsGoalsOnce()
        val customBanks = dao.getCustomBanksOnce()

        val root = JSONObject().apply {
            put("app", "HesabdarMan")
            put("version", 3)
            put("exportedAtMillis", System.currentTimeMillis())

            put("categories", JSONArray().apply {
                categories.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("isIncome", c.isIncome)
                        put("iconKey", c.iconKey)
                        put("colorHex", c.colorHex)
                        put("monthlyBudgetToman", c.monthlyBudgetToman)
                    })
                }
            })

            put("people", JSONArray().apply {
                people.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("phone", p.phone)
                        put("relationNote", p.relationNote)
                        put("avatarColorHex", p.avatarColorHex)
                        put("createdAtMillis", p.createdAtMillis)
                    })
                }
            })

            put("bankAccounts", JSONArray().apply {
                accounts.forEach { a ->
                    put(JSONObject().apply {
                        put("id", a.id)
                        put("bankCode", a.bankCode)
                        put("cardNumber", a.cardNumber)
                        put("shebaNumber", a.shebaNumber)
                        put("accountTitle", a.accountTitle)
                        put("balanceToman", a.balanceToman)
                        put("isDefault", a.isDefault)
                    })
                }
            })

            put("transactions", JSONArray().apply {
                transactions.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("amountToman", t.amountToman)
                        put("isIncome", t.isIncome)
                        if (t.categoryId != null) put("categoryId", t.categoryId)
                        put("bankCode", t.bankCode)
                        if (t.personId != null) put("personId", t.personId)
                        put("personImpact", t.personImpact)
                        put("timestampMillis", t.timestampMillis)
                        put("note", t.note)
                        put("fromSms", t.fromSms)
                        if (t.smsHash != null) put("smsHash", t.smsHash)
                    })
                }
            })

            put("savingsGoals", JSONArray().apply {
                goals.forEach { g ->
                    put(JSONObject().apply {
                        put("id", g.id)
                        put("title", g.title)
                        put("targetAmountToman", g.targetAmountToman)
                        put("savedAmountToman", g.savedAmountToman)
                        put("colorHex", g.colorHex)
                        put("iconKey", g.iconKey)
                        put("createdAtMillis", g.createdAtMillis)
                    })
                }
            })

            put("customBanks", JSONArray().apply {
                customBanks.forEach { b ->
                    put(JSONObject().apply {
                        put("id", b.id)
                        put("code", b.code)
                        put("persianName", b.persianName)
                        put("shortName", b.shortName)
                        put("primaryColorHex", b.primaryColorHex)
                        put("cardPrefix", b.cardPrefix)
                        put("smsKeyword", b.smsKeyword)
                    })
                }
            })
        }

        val stats = BackupStats(
            transactionCount = transactions.size,
            categoryCount = categories.size,
            personCount = people.size,
            bankAccountCount = accounts.size,
            savingsGoalCount = goals.size
        )
        return root.toString(2) to stats
    }

    private suspend fun restoreFromJsonString(content: String): BackupStats {
        val root = JSONObject(content)
        require(root.optString("app") == "HesabdarMan") {
            "فایل انتخابی مربوط به نسخه پشتیبان «حسابدار من» نیست"
        }

        val categories = parseArray(root.optJSONArray("categories")) { o ->
            CategoryEntity(
                id = o.optLong("id", 0L),
                name = o.getString("name"),
                isIncome = o.optBoolean("isIncome", false),
                iconKey = o.optString("iconKey", "shopping_bag"),
                colorHex = o.optLong("colorHex", 0xFF0B4F4A),
                monthlyBudgetToman = o.optLong("monthlyBudgetToman", 0L)
            )
        }

        val people = parseArray(root.optJSONArray("people")) { o ->
            PersonEntity(
                id = o.optLong("id", 0L),
                name = o.getString("name"),
                phone = o.optString("phone", ""),
                relationNote = o.optString("relationNote", ""),
                avatarColorHex = o.optLong("avatarColorHex", 0xFF0B4F4A),
                createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis())
            )
        }

        val accounts = parseArray(root.optJSONArray("bankAccounts")) { o ->
            BankAccountEntity(
                id = o.optLong("id", 0L),
                bankCode = o.optString("bankCode", "CASH"),
                cardNumber = o.optString("cardNumber", ""),
                shebaNumber = o.optString("shebaNumber", ""),
                accountTitle = o.optString("accountTitle", ""),
                balanceToman = o.optLong("balanceToman", 0L),
                isDefault = o.optBoolean("isDefault", false)
            )
        }

        val transactions = parseArray(root.optJSONArray("transactions")) { o ->
            TransactionEntity(
                id = o.optLong("id", 0L),
                title = o.getString("title"),
                amountToman = o.getLong("amountToman"),
                isIncome = o.optBoolean("isIncome", false),
                categoryId = if (o.has("categoryId")) o.optLong("categoryId") else null,
                bankCode = o.optString("bankCode", "CASH"),
                personId = if (o.has("personId")) o.optLong("personId") else null,
                personImpact = o.optInt("personImpact", 0),
                timestampMillis = o.optLong("timestampMillis", System.currentTimeMillis()),
                note = o.optString("note", ""),
                fromSms = o.optBoolean("fromSms", false),
                smsHash = if (o.has("smsHash")) o.optString("smsHash") else null
            )
        }

        val goals = parseArray(root.optJSONArray("savingsGoals")) { o ->
            SavingsGoalEntity(
                id = o.optLong("id", 0L),
                title = o.getString("title"),
                targetAmountToman = o.getLong("targetAmountToman"),
                savedAmountToman = o.optLong("savedAmountToman", 0L),
                colorHex = o.optLong("colorHex", 0xFF10B981),
                iconKey = o.optString("iconKey", "savings"),
                createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis())
            )
        }

        val customBanks = parseArray(root.optJSONArray("customBanks")) { o ->
            CustomBankEntity(
                id = o.optLong("id", 0L),
                code = o.optString("code", "CUSTOM_${System.currentTimeMillis()}"),
                persianName = o.getString("persianName"),
                shortName = o.optString("shortName", o.getString("persianName")),
                primaryColorHex = o.optLong("primaryColorHex", 0xFF0B4F4A),
                cardPrefix = o.optString("cardPrefix", ""),
                smsKeyword = o.optString("smsKeyword", "")
            )
        }

        dao.replaceAllData(
            categories = categories,
            people = people,
            bankAccounts = accounts,
            transactions = transactions,
            savingsGoals = goals,
            customBanks = customBanks
        )

        return BackupStats(
            transactionCount = transactions.size,
            categoryCount = categories.size,
            personCount = people.size,
            bankAccountCount = accounts.size,
            savingsGoalCount = goals.size
        )
    }

    private inline fun <T> parseArray(array: JSONArray?, mapper: (JSONObject) -> T): List<T> {
        if (array == null) return emptyList()
        val result = ArrayList<T>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            result.add(mapper(obj))
        }
        return result
    }
}
