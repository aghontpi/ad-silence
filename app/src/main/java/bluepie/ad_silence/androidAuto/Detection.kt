package bluepie.ad_silence.androidAuto

import android.widget.TextView
import bluepie.ad_silence.R
import bluepie.ad_silence.detections.isInCarMode

private fun configureCarViews() {
    var carPlayStatusTextView = findViewById<TextView>(R.id.car_play_status);
    if (isInCarMode(applicationContext)) {
        carPlayStatusTextView.text = getString(R.string.car_play_status_connected)
    } else {
        carPlayStatusTextView.text = getString(R.string.car_play_status_not_connected)
    }
}
