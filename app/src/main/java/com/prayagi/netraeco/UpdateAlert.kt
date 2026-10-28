package com.prayagi.netraplayer
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
object UpdateAlert {
 const val EXTRA_AUTO_UPDATE="netra_auto_update"
 fun start(a:android.app.Activity){AutomaticUpdateNotice.start(a);handle(a,a.intent)}
 fun handle(a:android.app.Activity,i:android.content.Intent?){i?.removeExtra(EXTRA_AUTO_UPDATE)}
}
/** Persisted legacy workers are terminal; no network, notification, download or installer. */
class UpdateAlertWorker(c:Context,p:WorkerParameters):CoroutineWorker(c,p){override suspend fun doWork():Result=Result.success()}
class UpdateDownloadWorker(c:Context,p:WorkerParameters):CoroutineWorker(c,p){override suspend fun doWork():Result=Result.success()}
