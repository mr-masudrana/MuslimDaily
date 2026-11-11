package com.muslimdaily.app.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muslimdaily.app.R
import com.muslimdaily.app.data.local.BookmarkEntity
import com.muslimdaily.app.data.local.SurahEntity
import com.muslimdaily.app.ui.common.localizeNumber
import java.util.Locale

@Composable
fun QuranScreen(
    onOpen: (surah: Int, ayah: Int) -> Unit,
    vm: QuranViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val filtered = remember(ui.surahs, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) ui.surahs
        else ui.surahs.filter {
            it.nameEnglish.lowercase().contains(q) ||
                it.translationEnglish.lowercase().contains(q) ||
                it.nameArabic.contains(q) ||
                it.number.toString() == q
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            stringResource(R.string.tab_quran),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text(stringResource(R.string.search_surah_hint)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        )
        Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text(stringResource(R.string.tab_surah)) })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text(stringResource(R.string.tab_bookmarks)) })
        }

        if (tab == 0) {
            when {
                ui.surahs.isEmpty() && ui.load == LoadState.Loading -> CenterBox { CircularProgressIndicator() }
                ui.surahs.isEmpty() -> CenterBox {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.error_quran), style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = vm::refresh) { Text(stringResource(R.string.retry)) }
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val last = ui.lastRead
                    val lastSurah = last?.let { l -> ui.surahs.firstOrNull { it.number == l.first } }
                    if (query.isBlank() && last != null && lastSurah != null) {
                        item { LastReadCard(lastSurah, last.second, locale) { onOpen(last.first, last.second) } }
                    }
                    if (filtered.isEmpty()) {
                        item { Text(stringResource(R.string.no_results), style = MaterialTheme.typography.bodyMedium) }
                    }
                    items(filtered, key = { it.number }) { s ->
                        SurahRow(s, locale) { onOpen(s.number, 1) }
                    }
                }
            }
        } else {
            BookmarkList(ui.bookmarks, ui.surahs, locale, onOpen)
        }
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun LastReadCard(surah: SurahEntity, ayah: Int, locale: Locale, onContinue: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.last_read),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
                Text(
                    surah.nameEnglish,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    stringResource(R.string.ayah_label, localizeNumber(ayah, locale)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Button(onClick = onContinue) { Text(stringResource(R.string.continue_reading)) }
        }
    }
}

@Composable
private fun SurahRow(s: SurahEntity, locale: Locale, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    localizeNumber(s.number, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(s.nameEnglish, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${s.translationEnglish} • " + stringResource(R.string.verses_count, localizeNumber(s.ayahCount, locale)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(s.nameArabic, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BookmarkList(
    bookmarks: List<BookmarkEntity>,
    surahs: List<SurahEntity>,
    locale: Locale,
    onOpen: (Int, Int) -> Unit,
) {
    if (bookmarks.isEmpty()) {
        CenterBox { Text(stringResource(R.string.no_bookmarks), style = MaterialTheme.typography.bodyMedium) }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        items(bookmarks, key = { it.id }) { b ->
            val name = surahs.firstOrNull { it.number == b.surah }?.nameEnglish ?: localizeNumber(b.surah, locale)
            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().clickable { onOpen(b.surah, b.ayah) },
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.ayah_label, localizeNumber(b.ayah, locale)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
