package com.muslimdaily.app.ui.ibadah

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muslimdaily.app.R
import com.muslimdaily.app.data.dua.DuaItem
import com.muslimdaily.app.data.dua.text
import com.muslimdaily.app.ui.common.localizeNumber
import java.util.Locale

@Composable
fun IbadahScreen(onOpen: (String) -> Unit, vm: IbadahViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val isBangla = locale.language == "bn"
    var selected by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            stringResource(R.string.tab_ibadah),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )

        if (ui.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        val cats = ui.categories
        val favoritesIndex = cats.size
        val current = selected.coerceIn(0, favoritesIndex)
        val labels = cats.map { it.title.text(isBangla) } + stringResource(R.string.favorites)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
            itemsIndexed(labels) { index, label ->
                FilterChip(selected = current == index, onClick = { selected = index }, label = { Text(label) })
            }
        }

        val shown: List<DuaItem> = if (current < favoritesIndex) {
            cats[current].items.orEmpty()
        } else {
            cats.flatMap { it.items.orEmpty() }.filter { it.id in ui.favorites }
        }

        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(if (current < favoritesIndex) R.string.empty_category else R.string.no_favorites),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(shown, key = { it.id.orEmpty() }) { item ->
                    DuaRow(item, isBangla, locale) { onOpen(item.id.orEmpty()) }
                }
            }
        }
    }
}

@Composable
private fun DuaRow(item: DuaItem, isBangla: Boolean, locale: Locale, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title.text(isBangla), style = MaterialTheme.typography.titleMedium)
                val repeat = item.repeat ?: 1
                if (repeat > 1) {
                    Text(
                        stringResource(R.string.repeat_times, localizeNumber(repeat, locale)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
