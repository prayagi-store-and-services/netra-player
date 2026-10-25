package com.prayagi.netraplayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/** Shows today's festival, Independence Day or condolence. Hidden when there is nothing to show. Data is bundled, works offline. */
@Composable
fun FestivalBannerCard(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(Date()) }
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) now = Date()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val banner: Banner = remember(java.text.SimpleDateFormat("yyyyMMdd").format(now)) {
        FestivalBanner.pick(Calendar.getInstance())
    } ?: return
    val b = Color(banner.accent2)
    val sorrow = banner.kind == BannerKind.SORROW
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(banner.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (sorrow) b else MaterialTheme.colorScheme.primary)
        Text(banner.text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(now),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
