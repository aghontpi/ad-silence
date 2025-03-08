package bluepie.ad_silence.detections

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration

fun isInCarMode(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    val currentMode = context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    return (currentMode == Configuration.UI_MODE_TYPE_CAR) ||
            (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_CAR)
}