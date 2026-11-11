package com.muslimdaily.app.ui.ibadah

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muslimdaily.app.R
import com.muslimdaily.app.data.dua.DuaItem
import com.muslimdaily.app.data.dua.text
import com.muslimdaily.app.ui.common.localizeNumber
import com.muslimdaily.app.ui.theme.ArabicStyle

@Composable
fun DuaDetailScreen(onBack: () -> Unit, vm: DuaDetailViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val isBangla = locale.language == "bn"
    var count by rememberSaveable { mutableIntStateOf(0) }
    val item = ui.item

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                item?.title.text(isBangla),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 2,
            )
            if (item != null) {
                IconButton(onClick = vm::toggleFavorite) {
                    Icon(
                        if (ui.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.favorite),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText(item, isBangla))
                    }
                    context.startActivity(Intent.createChooser(send, null))
                }) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (item == null) {
            if (!ui.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.not_found), style = MaterialTheme.typography.bodyMedium)
                }
            }
            return@Column
        }

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    item.arabic.orEmpty(),
                    style = ArabicStyle.copy(textDirection = TextDirection.Rtl),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                )
            }

            val translit = item.transliteration.text(isBangla)
            if (translit.isNotBlank()) {
                Text(translit, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(item.translation.text(isBangla), style = MaterialTheme.typography.bodyMedium)

            if (!item.reference.isNullOrBlank()) {
                Text(
                    stringResource(R.string.reference_label, item.reference),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val target = item.repeat ?: 1
            if (target > 1) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.repeat_times, localizeNumber(target, locale)), style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { count = if (count >= target) 1 else count + 1 }, shape = CircleShape, modifier = Modifier.size(120.dp)) {
                        Text("${localizeNumber(count, locale)}/${localizeNumber(target, locale)}", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(stringResource(R.string.counter_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { count = 0 }) { Text(stringResource(R.string.reset)) }
                }
            }
        }
    }
}

private fun shareText(item: DuaItem, bangla: Boolean): String = buildString {
    appendLine(item.arabic.orEmpty())
    val t = item.translation.text(bangla)
    if (t.isNotBlank()) { appendLine(); appendLine(t) }
    if (!item.reference.isNullOrBlank()) { appendLine(); append(item.reference) }
}.trim()
