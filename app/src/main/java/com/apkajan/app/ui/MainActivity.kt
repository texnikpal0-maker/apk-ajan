package com.apkajan.app.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apkajan.app.container
import com.apkajan.app.core.LocaleHelper
import com.apkajan.app.data.settings.SettingsRepository
import com.apkajan.app.ui.apps.AppPickerScreen
import com.apkajan.app.ui.main.MainScreen
import com.apkajan.app.ui.main.MainViewModel
import com.apkajan.app.ui.pomodoro.PomodoroScreen
import com.apkajan.app.ui.stats.StatsScreen
import com.apkajan.app.ui.theme.ApkAjanTheme

enum class Screen { MAIN, STATS, POMODORO, APPS }

class MainActivity : ComponentActivity() {

    /** Seçili dil kaynakları burada uygulanır; dil değişince recreate() ile ekran yeniden çizilir. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase, SettingsRepository.readLanguage(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val c = applicationContext.container
        setContent {
            ApkAjanTheme {
                AppRoot(
                    onLanguageChange = { lang ->
                        if (lang != c.settings.language.value) {
                            c.settings.setLanguage(lang)
                            recreate()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AppRoot(onLanguageChange: (String) -> Unit) {
    var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }
    val vm: MainViewModel = viewModel(factory = MainViewModel.Factory)

    BackHandler(enabled = screen != Screen.MAIN) { screen = Screen.MAIN }
    val back = { screen = Screen.MAIN }

    when (screen) {
        Screen.MAIN -> MainScreen(
            vm = vm,
            onLanguageChange = onLanguageChange,
            onOpenStats = { screen = Screen.STATS },
            onOpenPomodoro = { screen = Screen.POMODORO },
            onOpenApps = { screen = Screen.APPS }
        )
        Screen.STATS -> StatsScreen(onBack = back)
        Screen.POMODORO -> PomodoroScreen(vm = vm, onBack = back)
        Screen.APPS -> AppPickerScreen(onBack = back)
    }
}
