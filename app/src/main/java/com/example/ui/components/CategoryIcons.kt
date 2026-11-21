package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class IconOption(val key: String, val label: String, val icon: ImageVector)

object CategoryIcons {
    val OPTIONS: List<IconOption> = listOf(
        IconOption("restaurant", "خوراک", Icons.Rounded.Restaurant),
        IconOption("grocery", "سوپرمارکت", Icons.Rounded.LocalGroceryStore),
        IconOption("coffee", "کافه", Icons.Rounded.Coffee),
        IconOption("directions_car", "حمل‌ونقل", Icons.Rounded.DirectionsCar),
        IconOption("shopping_bag", "خرید", Icons.Rounded.ShoppingBag),
        IconOption("bolt", "قبوض", Icons.Rounded.Bolt),
        IconOption("phone", "موبایل", Icons.Rounded.PhoneAndroid),
        IconOption("home", "مسکن", Icons.Rounded.Home),
        IconOption("medical", "درمان", Icons.Rounded.LocalHospital),
        IconOption("flight", "سفر", Icons.Rounded.Flight),
        IconOption("movie", "تفریح", Icons.Rounded.Movie),
        IconOption("fitness", "ورزش", Icons.Rounded.FitnessCenter),
        IconOption("school", "آموزش", Icons.Rounded.School),
        IconOption("pets", "حیوانات", Icons.Rounded.Pets),
        IconOption("work", "حقوق", Icons.Rounded.Work),
        IconOption("business", "کسب‌وکار", Icons.Rounded.BusinessCenter),
        IconOption("trending_up", "سرمایه‌گذاری", Icons.Rounded.TrendingUp),
        IconOption("card_giftcard", "هدیه", Icons.Rounded.CardGiftcard),
        IconOption("savings", "پس‌انداز", Icons.Rounded.Savings),
        IconOption("bank", "بانک", Icons.Rounded.AccountBalance),
        IconOption("wallet", "کیف پول", Icons.Rounded.AccountBalanceWallet)
    )

    val COLOR_SWATCHES: List<Long> = listOf(
        0xFF0B4F4A, 0xFF059669, 0xFF16A34A, 0xFF0891B2,
        0xFF0284C7, 0xFF4F46E5, 0xFF9333EA, 0xFFEC4899,
        0xFFDC2626, 0xFFEA580C, 0xFFD97706, 0xFF64748B
    )

    fun resolve(key: String?): ImageVector =
        OPTIONS.firstOrNull { it.key == key }?.icon ?: Icons.Rounded.AccountBalanceWallet
}
