package com.prayagi.netraplayer

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat

object Net {
    /** Installed version code and name, or null when the app is not installed. */
    fun installed(context: Context, packageName: String): Pair<Long, String>? = try {
        val info = context.packageManager.getPackageInfo(packageName, 0)
        Pair(PackageInfoCompat.getLongVersionCode(info), info.versionName ?: "")
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
