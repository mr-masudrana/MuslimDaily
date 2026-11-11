package com.muslimdaily.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muslimdaily.app.R
import com.muslimdaily.app.data.HijriUtil
import com.muslimdaily.app.data.model.HijriDate
import com.muslimdaily.app.data.model.NextPrayer
import com.muslimdaily.app.data.model.Prayer
import com.muslimdaily.app.data.model.next
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private fun Long.localized(locale: Locale): String =
    NumberFormat.getInstance(locale).apply { isGroupingUsed = false }.format(this)

@Composable
fun HomeScreen(onOpenCalendar: () -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val hijriOffset by vm.hijriOffset.collectAsStateWithLifecycle()
    val hijri = HijriUtil.of(LocalDate.now(), hijriOffset)
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val timeFmt = remember(locale) { DateTimeFormatter.ofPattern("hh:mm a", locale) }

    val permission = Manifest.permission.ACCESS_COARSE_LOCATION
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.load() }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(permission)
        }
    }

    val day = state.day
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Header(day?.date ?: LocalDate.now(), hijri, locale) }

        if (day == null) {
            item {
                if (state.loading) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    ErrorCard(onRetry = vm::load)
                }
            }
        } else {
            val next = day.next(state.now)
            item { NextPrayerCard(next, timeFmt, locale) }
            if (state.usingDefaultLocation) {
                item { LocationNote(onAllow = { launcher.launch(permission) }) }
            }
            items(Prayer.entries) { prayer ->
                PrayerRow(prayer, day.times.getValue(prayer), prayer == next.prayer, timeFmt)
            }
        }
        item { CalendarLink(onOpenCalendar) }
    }
}

@Composable
private fun Header(date: LocalDate, hijri: HijriDate?, locale: Locale) {
    val gregorian = remember(date, locale) {
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(R.string.greeting), style = MaterialTheme.typography.headlineMedium)
        Text(gregorian, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (hijri != null) {
            val months = stringArrayResource(R.array.hijri_months)
            Text(
                stringResource(
                    R.string.hijri_date,
                    hijri.day.toLong().localized(locale),
                    months[(hijri.month - 1).coerceIn(0, 11)],
                    hijri.year.toLong().localized(locale),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NextPrayerCard(next: NextPrayer, fmt: DateTimeFormatter, locale: Locale) {
    val totalMin = next.remaining.toMinutes()
    val h = totalMin / 60
    val m = totalMin % 60
    val left = if (h > 0) {
        stringResource(R.string.time_left_hm, h.localized(locale), m.localized(locale))
    } else {
        stringResource(R.string.time_left_m, m.localized(locale))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.next_prayer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            )
            Text(
                stringResource(next.prayer.nameRes),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                next.time.format(fmt),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(left, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun PrayerRow(prayer: Prayer, time: LocalTime, isNext: Boolean, fmt: DateTimeFormatter) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isNext) 0.dp else 1.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(prayer.nameRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold,
            )
            Text(time.format(fmt), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun LocationNote(onAllow: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.default_location_note),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAllow) { Text(stringResource(R.string.allow)) }
        }
    }
}

@Composable
private fun ErrorCard(onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.error_no_data), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun CalendarLink(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.view_calendar), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
