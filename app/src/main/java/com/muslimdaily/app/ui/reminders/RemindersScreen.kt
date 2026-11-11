package com.muslimdaily.app.ui.reminders

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muslimdaily.app.R
import com.muslimdaily.app.data.model.Prayer
import com.muslimdaily.app.data.model.ReminderType
import com.muslimdaily.app.ui.common.localizeNumber
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@StringRes
private fun ReminderType.labelRes(): Int = when (this) {
    ReminderType.Quran -> R.string.reminder_quran
    ReminderType.MorningDhikr -> R.string.reminder_morning
    ReminderType.EveningDhikr -> R.string.reminder_evening
}

private val LEAD_OPTIONS = listOf(0, 5, 10, 15, 30)

@Composable
fun RemindersScreen(onBack: () -> Unit, vm: RemindersViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val timeFmt = remember(locale) { DateTimeFormatter.ofPattern("hh:mm a", locale) }

    var exactOk by remember { mutableStateOf(vm.canScheduleExact()) }
    var systemEnabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var timePickerFor by remember { mutableStateOf<ReminderType?>(null) }

    // Re-check permissions when coming back from system settings.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        exactOk = vm.canScheduleExact()
        systemEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        vm.reschedule()
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setMaster(granted)
    }
    val toggleMaster: (Boolean) -> Unit = { on ->
        val needsPermission = on && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.setMaster(on)
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.notifications_title), style = MaterialTheme.typography.titleLarge)
        }

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 1.dp) {
                SwitchRow(stringResource(R.string.enable_notifications), s.master, onChange = toggleMaster)
            }

            if (s.master && !systemEnabled) {
                NoticeCard(R.string.notif_blocked) {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }
            }
            if (s.master && !exactOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                NoticeCard(R.string.notif_exact_needed) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                    )
                }
            }

            SectionTitle(R.string.prayer_notifications)
            Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 1.dp) {
                Column {
                    Prayer.entries.forEach { prayer ->
                        SwitchRow(stringResource(prayer.nameRes), prayer in s.prayers, enabled = s.master) {
                            vm.setPrayer(prayer, it)
                        }
                    }
                }
            }

            Text(stringResource(R.string.remind_before), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LEAD_OPTIONS.forEach { minutes ->
                    FilterChip(
                        selected = s.leadMinutes == minutes,
                        onClick = { vm.setLeadMinutes(minutes) },
                        enabled = s.master,
                        label = {
                            Text(
                                if (minutes == 0) stringResource(R.string.at_time)
                                else stringResource(R.string.minutes_short, localizeNumber(minutes, locale))
                            )
                        },
                    )
                }
            }

            SectionTitle(R.string.reminders_section)
            Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 1.dp) {
                Column {
                    ReminderType.entries.forEach { type ->
                        val r = s.reminders.getValue(type)
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(type.labelRes()), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    LocalTime.of(r.minuteOfDay / 60, r.minuteOfDay % 60).format(timeFmt),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable(enabled = s.master) { timePickerFor = type }.padding(vertical = 4.dp),
                                )
                            }
                            Switch(
                                checked = r.enabled,
                                onCheckedChange = { vm.setReminderEnabled(type, it) },
                                enabled = s.master,
                            )
                        }
                    }
                }
            }
        }
    }

    timePickerFor?.let { type ->
        val current = s.reminders.getValue(type).minuteOfDay
        TimeDialog(
            initialMinute = current,
            onConfirm = { minute ->
                vm.setReminderMinute(type, minute)
                timePickerFor = null
            },
            onDismiss = { timePickerFor = null },
        )
    }
}

@Composable
private fun SectionTitle(@StringRes res: Int) {
    Text(stringResource(res), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun NoticeCard(@StringRes message: Int, onOpen: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(message), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpen) { Text(stringResource(R.string.open_settings)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initialMinute: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initialMinute / 60,
        initialMinute = initialMinute % 60,
        is24Hour = false,
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TimePicker(state = state)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text(stringResource(R.string.ok)) }
                }
            }
        }
    }
}
