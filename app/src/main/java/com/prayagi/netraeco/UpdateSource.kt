package com.prayagi.netraplayer

import android.content.Context

/**
 * Netra Player does not update itself. The Netra Eco app is the only update path: it checks,
 * downloads, verifies and installs new versions. This file only reports that arrangement honestly,
 * depending on whether Netra Eco is actually installed on this phone.
 */
const val NETRA_ECO_PACKAGE = "com.prayagi.netraeco"

fun isPackageInstalled(context: Context, packageName: String): Boolean =
    Net.installed(context, packageName) != null

/** Update card text. Truthful in both cases: Eco present on this phone or not. */
fun updateSourceMessage(ecoInstalled: Boolean): String =
    if (ecoInstalled)
        "Netra Player no longer checks for, downloads or installs updates by itself. New versions are checked, downloaded and installed by the Netra Eco app on this phone."
    else
        "Netra Player no longer checks for, downloads or installs updates by itself. New versions come from the Netra Eco app - install Netra Eco from the Netra website to keep getting updates."
