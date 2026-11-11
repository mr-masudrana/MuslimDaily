package com.muslimdaily.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector
import com.muslimdaily.app.R

enum class Tab(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    Home("home", R.string.tab_home, Icons.Outlined.Home),
    Quran("quran", R.string.tab_quran, Icons.Outlined.MenuBook),
    Ibadah("ibadah", R.string.tab_ibadah, Icons.Outlined.VolunteerActivism),
    Settings("settings", R.string.tab_settings, Icons.Outlined.Settings),
}
