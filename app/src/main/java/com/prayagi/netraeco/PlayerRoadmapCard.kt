package com.prayagi.netraplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Live estimates from the same source as the family website, never mockup dates. */
@Composable
fun PlayerRoadmapCard() {
    var items by remember { mutableStateOf<List<RoadmapItem>?>(null) }
    LaunchedEffect(Unit) {
        items = withContext(Dispatchers.IO) {
            runCatching {
                val c = java.net.URL(Roadmap.URL).openConnection() as java.net.HttpURLConnection
                c.connectTimeout = 10000; c.readTimeout = 10000
                try {
                    val root = org.json.JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                    val source = root.getJSONArray("items")
                    val filtered = org.json.JSONArray()
                    for (i in 0 until source.length()) {
                        val item = source.getJSONObject(i)
                        val apps = item.optJSONArray("apps")
                        if ((0 until (apps?.length() ?: 0)).any { apps!!.optString(it) == "netra-player" }) filtered.put(item)
                    }
                    root.put("items", filtered)
                    Roadmap.parse(root.toString())
                } finally { c.disconnect() }
            }.getOrDefault(emptyList())
        }
    }
    RoadmapSection(items)
}
