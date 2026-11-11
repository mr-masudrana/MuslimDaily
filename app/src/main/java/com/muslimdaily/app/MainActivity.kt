package com.muslimdaily.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muslimdaily.app.ui.navigation.MuslimDailyNavHost
import com.muslimdaily.app.ui.onboarding.OnboardingScreen
import com.muslimdaily.app.ui.theme.MuslimDailyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the splash screen until we know whether to show onboarding or the app.
        splash.setKeepOnScreenCondition { viewModel.onboardingDone.value == null }
        enableEdgeToEdge()
        setContent {
            MuslimDailyTheme {
                val done by viewModel.onboardingDone.collectAsStateWithLifecycle()
                when (done) {
                    null -> Unit
                    false -> OnboardingScreen(onStart = viewModel::finishOnboarding)
                    true -> MuslimDailyNavHost()
                }
            }
        }
    }
}
