package com.muslimdaily.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.muslimdaily.app.ui.calendar.CalendarScreen
import com.muslimdaily.app.ui.home.HomeScreen
import com.muslimdaily.app.ui.ibadah.DuaDetailScreen
import com.muslimdaily.app.ui.ibadah.IbadahScreen
import com.muslimdaily.app.ui.quran.QuranScreen
import com.muslimdaily.app.ui.quran.ReaderScreen
import com.muslimdaily.app.ui.reminders.RemindersScreen
import com.muslimdaily.app.ui.settings.SettingsScreen

private const val READER_ROUTE = "reader/{surah}?ayah={ayah}"

@Composable
fun MuslimDailyNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBottomBar = current == null || Tab.entries.any { it.route == current }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Home.route) { HomeScreen(onOpenCalendar = { navController.navigate("calendar") }) }
            composable("calendar") {
                CalendarScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSurah = { surah -> navController.navigate("reader/$surah?ayah=1") },
                )
            }
            composable(Tab.Quran.route) {
                QuranScreen(onOpen = { surah, ayah -> navController.navigate("reader/$surah?ayah=$ayah") })
            }
            composable(
                route = READER_ROUTE,
                arguments = listOf(
                    navArgument("surah") { type = NavType.IntType },
                    navArgument("ayah") { type = NavType.IntType; defaultValue = 1 },
                ),
            ) { ReaderScreen(onBack = { navController.popBackStack() }) }
            composable(Tab.Ibadah.route) {
                IbadahScreen(onOpen = { id -> navController.navigate("dua/${Uri.encode(id)}") })
            }
            composable(
                route = "dua/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { DuaDetailScreen(onBack = { navController.popBackStack() }) }
            composable(Tab.Settings.route) {
                SettingsScreen(onOpenReminders = { navController.navigate("reminders") })
            }
            composable("reminders") { RemindersScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
