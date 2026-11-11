package com.muslimdaily.app.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muslimdaily.app.R
import com.muslimdaily.app.ui.common.localizeNumber

@Composable
fun SettingsScreen(onOpenReminders: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val locale = LocalConfiguration.current.locales[0]
    val isBangla = locale.language == "bn"
    val offset by vm.hijriOffset.collectAsStateWithLifecycle()

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(stringResource(R.string.tab_settings), style = MaterialTheme.typography.headlineMedium)

        Surface(
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).clickable(onClick = onOpenReminders),
        ) {
            Row(
                Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.settings_notifications_row),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text(
            stringResource(R.string.language),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )
        LanguageOption(stringResource(R.string.language_en), selected = !isBangla) { setLanguage("en") }
        LanguageOption(stringResource(R.string.language_bn), selected = isBangla) { setLanguage("bn") }

        Text(
            stringResource(R.string.hijri_adjust),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            stringResource(R.string.hijri_adjust_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        val shown = when {
            offset > 0 -> "+" + localizeNumber(offset, locale)
            offset < 0 -> "−" + localizeNumber(-offset, locale)
            else -> localizeNumber(0, locale)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            IconButton(onClick = { vm.changeHijriOffset(-1) }, enabled = offset > -2) {
                Icon(Icons.Filled.Remove, contentDescription = null)
            }
            Text(stringResource(R.string.days_offset, shown), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { vm.changeHijriOffset(1) }, enabled = offset < 2) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        }
    }
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun setLanguage(tag: String) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
}
