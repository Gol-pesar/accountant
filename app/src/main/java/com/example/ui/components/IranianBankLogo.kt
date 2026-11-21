package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.BankEmblemType
import com.example.domain.IranianBank
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws a crisp, distinctive geometric emblem representing each Iranian bank's visual identity.
 */
@Composable
fun IranianBankLogo(
    bank: IranianBank,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    circular: Boolean = false
) {
    val primary = Color(bank.primaryColorHex)
    val secondary = Color(bank.secondaryColorHex)
    val accent = Color(bank.accentColorHex)
    val shape = if (circular) CircleShape else RoundedCornerShape(size * 0.28f)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(primary, secondary)
                )
            )
            .semantics { contentDescription = "لوگوی ${bank.persianName}" },
        contentAlignment = Alignment.Center
    ) {
        if (bank.emblemType == BankEmblemType.CUSTOM) {
            val glyph = bank.shortName.trim().firstOrNull()?.toString() ?: "ب"
            Canvas(modifier = Modifier.size(size * 0.86f)) {
                drawCircle(
                    color = accent.copy(alpha = 0.45f),
                    radius = minOf(this.size.width, this.size.height) * 0.45f,
                    style = Stroke(width = this.size.width * 0.06f)
                )
            }
            Text(
                text = glyph,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.42f).sp
            )
            return@Box
        }
        Canvas(modifier = Modifier.size(size * 0.66f)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = minOf(w, h) * 0.44f
            val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round)

            when (bank.emblemType) {
                BankEmblemType.MELLI -> {
                    // Iconic golden Melli emblem: stylized double wing / diamond dome
                    val path = Path().apply {
                        moveTo(cx, cy - r)
                        lineTo(cx + r * 0.85f, cy)
                        lineTo(cx, cy + r)
                        lineTo(cx - r * 0.85f, cy)
                        close()
                    }
                    drawPath(path, color = accent, style = stroke)
                    drawCircle(color = Color.White, radius = r * 0.24f, center = Offset(cx, cy))
                }

                BankEmblemType.MELLAT -> {
                    // Mellat: bold interlocked crimson/gold V-ribbon / folded diamond
                    val topRibbon = Path().apply {
                        moveTo(cx - r * 0.8f, cy - r * 0.35f)
                        lineTo(cx, cy - r * 0.95f)
                        lineTo(cx + r * 0.8f, cy - r * 0.35f)
                        lineTo(cx, cy + r * 0.2f)
                        close()
                    }
                    drawPath(topRibbon, color = Color.White)
                    drawArc(
                        color = accent,
                        startAngle = 15f,
                        sweepAngle = 150f,
                        useCenter = false,
                        topLeft = Offset(cx - r * 0.78f, cy - r * 0.45f),
                        size = Size(r * 1.56f, r * 1.35f),
                        style = Stroke(width = w * 0.13f, cap = StrokeCap.Round)
                    )
                }

                BankEmblemType.SADERAT -> {
                    // Saderat: concentric geometric shield / coin ring
                    drawCircle(color = Color.White, radius = r * 0.85f, style = stroke)
                    val inner = Path().apply {
                        moveTo(cx, cy - r * 0.48f)
                        lineTo(cx + r * 0.48f, cy)
                        lineTo(cx, cy + r * 0.48f)
                        lineTo(cx - r * 0.48f, cy)
                        close()
                    }
                    drawPath(inner, color = accent)
                }

                BankEmblemType.TEJARAT -> {
                    // Tejarat: twin parallel flowing diagonal bars
                    drawLine(
                        color = Color.White,
                        start = Offset(cx - r * 0.75f, cy + r * 0.55f),
                        end = Offset(cx + r * 0.25f, cy - r * 0.75f),
                        strokeWidth = w * 0.16f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = accent,
                        start = Offset(cx - r * 0.25f, cy + r * 0.75f),
                        end = Offset(cx + r * 0.75f, cy - r * 0.55f),
                        strokeWidth = w * 0.16f,
                        cap = StrokeCap.Round
                    )
                }

                BankEmblemType.SEPAH -> {
                    // Sepah: heritage 8-point geometric star crest
                    val star = Path()
                    val points = 8
                    for (i in 0 until points * 2) {
                        val rad = if (i % 2 == 0) r else r * 0.48f
                        val angle = (i * PI / points - PI / 2).toFloat()
                        val px = cx + rad * cos(angle)
                        val py = cy + rad * sin(angle)
                        if (i == 0) star.moveTo(px, py) else star.lineTo(px, py)
                    }
                    star.close()
                    drawPath(star, color = accent)
                    drawCircle(color = primary, radius = r * 0.26f)
                }

                BankEmblemType.PASARGAD -> {
                    // Pasargad: iconic golden sun disc over black/obsidian stepped arch
                    drawCircle(color = accent, radius = r * 0.52f, center = Offset(cx, cy - r * 0.15f))
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(cx - r * 0.85f, cy + r * 0.45f),
                        size = Size(r * 1.7f, r * 0.28f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }

                BankEmblemType.BLUBANK -> {
                    // BluBank: sleek modern lowercase 'b' geometric wave circle
                    drawCircle(
                        color = Color.White,
                        radius = r * 0.58f,
                        center = Offset(cx + r * 0.08f, cy + r * 0.16f),
                        style = Stroke(width = w * 0.15f)
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(cx - r * 0.48f, cy - r * 0.85f),
                        end = Offset(cx - r * 0.48f, cy + r * 0.72f),
                        strokeWidth = w * 0.15f,
                        cap = StrokeCap.Round
                    )
                }

                BankEmblemType.SAMAN -> {
                    // Saman: dynamic cyan S-curve ribbon
                    drawArc(
                        color = Color.White,
                        startAngle = 140f,
                        sweepAngle = 210f,
                        useCenter = false,
                        topLeft = Offset(cx - r * 0.7f, cy - r * 0.85f),
                        size = Size(r * 1.4f, r * 0.95f),
                        style = Stroke(width = w * 0.15f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accent,
                        startAngle = 320f,
                        sweepAngle = 210f,
                        useCenter = false,
                        topLeft = Offset(cx - r * 0.7f, cy - r * 0.1f),
                        size = Size(r * 1.4f, r * 0.95f),
                        style = Stroke(width = w * 0.15f, cap = StrokeCap.Round)
                    )
                }

                BankEmblemType.PARSIAN -> {
                    // Parsian: burgundy/gold hexagonal crystal
                    val hex = Path()
                    for (i in 0 until 6) {
                        val angle = (i * PI / 3 - PI / 2).toFloat()
                        val px = cx + r * cos(angle)
                        val py = cy + r * sin(angle)
                        if (i == 0) hex.moveTo(px, py) else hex.lineTo(px, py)
                    }
                    hex.close()
                    drawPath(hex, color = accent, style = stroke)
                    drawCircle(color = Color.White, radius = r * 0.3f)
                }

                BankEmblemType.KESHAVARZI -> {
                    // Keshavarzi: wheat leaf / twin emerald sprouts
                    val leftLeaf = Path().apply {
                        moveTo(cx, cy + r * 0.8f)
                        quadraticTo(cx - r * 0.95f, cy, cx - r * 0.25f, cy - r * 0.85f)
                        quadraticTo(cx + r * 0.1f, cy - r * 0.1f, cx, cy + r * 0.8f)
                    }
                    val rightLeaf = Path().apply {
                        moveTo(cx, cy + r * 0.8f)
                        quadraticTo(cx + r * 0.95f, cy, cx + r * 0.25f, cy - r * 0.85f)
                        quadraticTo(cx - r * 0.1f, cy - r * 0.1f, cx, cy + r * 0.8f)
                    }
                    drawPath(leftLeaf, color = Color.White)
                    drawPath(rightLeaf, color = accent)
                }

                BankEmblemType.MASKAN -> {
                    // Maskan: architectural roof / house chevron
                    val roof = Path().apply {
                        moveTo(cx - r * 0.9f, cy + r * 0.05f)
                        lineTo(cx, cy - r * 0.85f)
                        lineTo(cx + r * 0.9f, cy + r * 0.05f)
                    }
                    drawPath(roof, color = Color.White, style = Stroke(width = w * 0.15f, cap = StrokeCap.Round))
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(cx - r * 0.45f, cy + r * 0.15f),
                        size = Size(r * 0.9f, r * 0.7f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }

                BankEmblemType.REFAH -> {
                    // Refah: dual interlocking upward arches
                    drawArc(
                        color = Color.White,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(cx - r * 0.85f, cy - r * 0.65f),
                        size = Size(r * 1.7f, r * 1.5f),
                        style = Stroke(width = w * 0.14f, cap = StrokeCap.Round)
                    )
                    drawCircle(color = accent, radius = r * 0.32f, center = Offset(cx, cy + r * 0.25f))
                }

                BankEmblemType.AYANDEH -> {
                    // Ayandeh: rising geometric horizon / layered sunrise bars
                    drawCircle(color = accent, radius = r * 0.42f, center = Offset(cx, cy - r * 0.25f))
                    drawLine(
                        color = Color.White,
                        start = Offset(cx - r * 0.85f, cy + r * 0.35f),
                        end = Offset(cx + r * 0.85f, cy + r * 0.35f),
                        strokeWidth = w * 0.13f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = accent,
                        start = Offset(cx - r * 0.55f, cy + r * 0.72f),
                        end = Offset(cx + r * 0.55f, cy + r * 0.72f),
                        strokeWidth = w * 0.12f,
                        cap = StrokeCap.Round
                    )
                }

                BankEmblemType.SHAHR -> {
                    // Shahr: stylized city towers
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(cx - r * 0.75f, cy - r * 0.1f),
                        size = Size(r * 0.42f, r * 0.9f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(cx - r * 0.21f, cy - r * 0.8f),
                        size = Size(r * 0.42f, r * 1.6f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(cx + r * 0.33f, cy - r * 0.4f),
                        size = Size(r * 0.42f, r * 1.2f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }

                BankEmblemType.EGHTESAD_NOVIN -> {
                    // ENBank: rotated square frame with inner orbit
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(cx - r * 0.7f, cy - r * 0.7f),
                        size = Size(r * 1.4f, r * 1.4f),
                        cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
                        style = stroke
                    )
                    drawCircle(color = accent, radius = r * 0.34f)
                }

                BankEmblemType.SINA -> {
                    // Sina: triple chevron wave
                    val p = Path().apply {
                        moveTo(cx - r * 0.8f, cy + r * 0.4f)
                        lineTo(cx, cy - r * 0.65f)
                        lineTo(cx + r * 0.8f, cy + r * 0.4f)
                    }
                    drawPath(p, color = Color.White, style = stroke)
                    drawCircle(color = accent, radius = r * 0.26f, center = Offset(cx, cy + r * 0.45f))
                }

                BankEmblemType.DAY -> {
                    // Day: radiant sunburst ring
                    drawCircle(color = accent, radius = r * 0.45f)
                    for (i in 0 until 8) {
                        val a = (i * PI / 4).toFloat()
                        drawLine(
                            color = Color.White,
                            start = Offset(cx + r * 0.65f * cos(a), cy + r * 0.65f * sin(a)),
                            end = Offset(cx + r * 0.95f * cos(a), cy + r * 0.95f * sin(a)),
                            strokeWidth = w * 0.1f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                BankEmblemType.POST_BANK -> {
                    // Post Bank: envelope / winged seal
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(cx - r * 0.85f, cy - r * 0.55f),
                        size = Size(r * 1.7f, r * 1.1f),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = stroke
                    )
                    val flap = Path().apply {
                        moveTo(cx - r * 0.8f, cy - r * 0.5f)
                        lineTo(cx, cy + r * 0.1f)
                        lineTo(cx + r * 0.8f, cy - r * 0.5f)
                    }
                    drawPath(flap, color = accent, style = stroke)
                }

                BankEmblemType.RESALAT -> {
                    // Resalat: dual concentric harmony rings
                    drawCircle(color = Color.White, radius = r * 0.82f, style = stroke)
                    drawCircle(color = accent, radius = r * 0.42f, style = stroke)
                }

                BankEmblemType.MEHR_IRAN -> {
                    // Mehr Iran: warm sun-petal crest
                    drawCircle(color = accent, radius = r * 0.52f)
                    drawCircle(color = Color.White, radius = r * 0.84f, style = Stroke(width = w * 0.09f))
                }

                BankEmblemType.KARAFARIN -> {
                    // Karafarin: ascending precision step bars
                    drawLine(
                        color = accent,
                        start = Offset(cx - r * 0.7f, cy + r * 0.6f),
                        end = Offset(cx + r * 0.7f, cy - r * 0.6f),
                        strokeWidth = w * 0.15f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = Color.White, radius = r * 0.25f, center = Offset(cx - r * 0.55f, cy - r * 0.45f))
                    drawCircle(color = Color.White, radius = r * 0.25f, center = Offset(cx + r * 0.55f, cy + r * 0.45f))
                }

                BankEmblemType.GARDESHGARI -> {
                    // Gardeshgari: compass / globe meridians
                    drawCircle(color = Color.White, radius = r * 0.8f, style = stroke)
                    val needle = Path().apply {
                        moveTo(cx, cy - r * 0.6f)
                        lineTo(cx + r * 0.35f, cy)
                        lineTo(cx, cy + r * 0.6f)
                        lineTo(cx - r * 0.35f, cy)
                        close()
                    }
                    drawPath(needle, color = accent)
                }

                BankEmblemType.CUSTOM -> {
                    // Handled above with monogram text
                }

                BankEmblemType.CASH -> {
                    // Wallet emblem
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(cx - r * 0.85f, cy - r * 0.58f),
                        size = Size(r * 1.7f, r * 1.16f),
                        cornerRadius = CornerRadius(w * 0.15f, w * 0.15f),
                        style = stroke
                    )
                    drawCircle(
                        color = accent,
                        radius = r * 0.22f,
                        center = Offset(cx + r * 0.38f, cy)
                    )
                }
            }
        }
    }
}
