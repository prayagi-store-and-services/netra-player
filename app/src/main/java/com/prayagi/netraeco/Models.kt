package com.prayagi.netraplayer

/** Message shown when the device has no system file picker (some TVs). */
const val NO_PICKER_MESSAGE = "This device has no file picker. Install a file manager app, then tap Open again."

/** Video area height: taller on a TV so it is readable from the sofa. */
fun playerHeightDp(tv: Boolean): Int = if (tv) 360 else 240

/** True when running on a TV (Android TV and Google TV, including Xiaomi Mi TV). */
fun isTelevision(context: android.content.Context): Boolean {
    val ui = context.getSystemService(android.content.Context.UI_MODE_SERVICE) as? android.app.UiModeManager
    return ui?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
}
