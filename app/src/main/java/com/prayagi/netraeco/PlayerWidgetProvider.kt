package com.prayagi.netraplayer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Netra Player widget: the last file played and where it stopped. Real data only: it reads what the app itself saved
 * on this phone. Nothing played yet shows "Unavailable". No timer and no background work: the app asks for a redraw
 * only when a new position is saved.
 */
class PlayerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) draw(context, appWidgetManager, id)
    }

    companion object {
        fun refresh(context: Context) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            for (id in mgr.getAppWidgetIds(ComponentName(context, PlayerWidgetProvider::class.java))) draw(context, mgr, id)
        }

        private fun draw(context: Context, mgr: AppWidgetManager, id: Int) {
            val last = LastPlayed.load(context)
            val views = RemoteViews(context.packageName, R.layout.widget_player)
            if (last == null) {
                views.setTextViewText(R.id.pw_title, "Unavailable")
                views.setTextViewText(R.id.pw_detail, "Nothing played yet")
            } else {
                views.setTextViewText(R.id.pw_title, last.name)
                val at = SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(Date(last.savedAtMs))
                views.setTextViewText(R.id.pw_detail, "Stopped at ${LastPlayed.formatPosition(last.positionMs)}  |  saved $at")
            }
            val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
            views.setOnClickPendingIntent(R.id.pw_root, PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            mgr.updateAppWidget(id, views)
        }
    }
}
