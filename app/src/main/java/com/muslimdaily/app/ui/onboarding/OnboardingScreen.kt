package com.muslimdaily.app.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muslimdaily.app.R
import com.muslimdaily.app.ui.common.setAppLanguage

@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    val isBangla = LocalConfiguration.current.locales[0].language == "bn"
    val nameParts = stringResource(R.string.app_name).split(" ", limit = 2)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Language choice (the app supports Bangla and English)
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            FilterChip(selected = !isBangla, onClick = { setAppLanguage("en") }, label = { Text(stringResource(R.string.language_en)) })
            FilterChip(selected = isBangla, onClick = { setAppLanguage("bn") }, label = { Text(stringResource(R.string.language_bn)) })
        }

        Image(
            painter = painterResource(R.drawable.onboarding_mosque),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            contentScale = ContentScale.FillWidth,
        )

        Spacer(Modifier.height(12.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) { append(nameParts.first()) }
                if (nameParts.size > 1) {
                    append(" ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(nameParts[1]) }
                }
            },
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.onb_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )

        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Feature(Icons.Outlined.Schedule, R.string.onb_feature_prayer, Modifier.weight(1f))
            Feature(Icons.Outlined.MenuBook, R.string.onb_feature_quran, Modifier.weight(1f))
            Feature(Icons.Outlined.TaskAlt, R.string.onb_feature_deeds, Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.onb_start), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Feature(icon: ImageVector, labelRes: Int, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        }
        Text(
            stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
