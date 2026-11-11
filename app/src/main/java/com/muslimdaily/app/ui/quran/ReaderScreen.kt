package com.muslimdaily.app.ui.quran

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muslimdaily.app.R
import com.muslimdaily.app.data.local.AyahEntity
import com.muslimdaily.app.data.local.SurahEntity
import com.muslimdaily.app.ui.common.localizeNumber
import com.muslimdaily.app.ui.theme.ArabicStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.Locale

private val DIACRITICS = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")

/**
 * The API puts the Bismillah at the start of verse 1 of most surahs.
 * We show it once as a header and remove it from verse 1 (not for Al-Fatihah or At-Tawbah).
 * Returns (bismillah header or null, verse text).
 */
private fun splitBismillah(surah: Int, ayah: Int, text: String): Pair<String?, String> {
    if (ayah != 1 || surah == 1 || surah == 9) return null to text
    val words = text.trim().split(" ")
    val startsWithBismillah = words.size > 4 && words[0].replace(DIACRITICS, "").startsWith("بسم")
    if (!startsWithBismillah) return null to text
    return words.take(4).joinToString(" ") to words.drop(4).joinToString(" ")
}

@Composable
fun ReaderScreen(onBack: () -> Unit, vm: ReaderViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val isBangla = locale.language == "bn"
    val listState = rememberLazyListState()
    val hasAyahs = ui.ayahs.isNotEmpty()

    // Jump to the requested verse once, then remember the reading position.
    LaunchedEffect(hasAyahs) {
        if (!hasAyahs) return@LaunchedEffect
        val target = if (vm.initialAyah <= 1) 0 else vm.initialAyah.coerceAtMost(ui.ayahs.size)
        listState.scrollToItem(target)
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collectLatest { index ->
                delay(1_000)
                vm.saveLastRead(index.coerceAtLeast(1))
            }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(ui.surah?.nameEnglish ?: "", style = MaterialTheme.typography.titleLarge)
        }

        when {
            !hasAyahs && ui.load == LoadState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            !hasAyahs ->
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.error_quran), style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = vm::retry) { Text(stringResource(R.string.retry)) }
                    }
                }

            else -> {
                val bismillah = remember(ui.ayahs) {
                    ui.ayahs.firstOrNull()?.let { splitBismillah(vm.surahNumber, it.ayah, it.arabic).first }
                }
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { SurahHeader(ui.surah, bismillah, locale) }
                    items(ui.ayahs, key = { it.ayah }) { a ->
                        AyahCard(
                            ayah = a,
                            arabic = splitBismillah(vm.surahNumber, a.ayah, a.arabic).second,
                            translation = if (isBangla) a.bengali else a.english,
                            bookmarked = a.ayah in ui.bookmarked,
                            locale = locale,
                            onBookmark = { vm.toggleBookmark(a.ayah) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahHeader(surah: SurahEntity?, bismillah: String?, locale: Locale) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (surah != null) {
                Text(surah.nameArabic, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    "${surah.nameEnglish} • ${surah.translationEnglish}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    stringResource(R.string.verses_count, localizeNumber(surah.ayahCount, locale)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (bismillah != null) {
                Text(
                    bismillah,
                    style = ArabicStyle.copy(textDirection = TextDirection.Rtl),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun AyahCard(
    ayah: AyahEntity,
    arabic: String,
    translation: String,
    bookmarked: Boolean,
    locale: Locale,
    onBookmark: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        localizeNumber(ayah.ayah, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Box(Modifier.weight(1f))
                IconButton(onClick = onBookmark) {
                    Icon(
                        if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(R.string.bookmark),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                arabic,
                style = ArabicStyle.copy(textDirection = TextDirection.Rtl),
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                translation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
