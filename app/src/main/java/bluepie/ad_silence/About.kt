package bluepie.ad_silence

import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class About {
    fun aboutBuilder(context: Context, view: View, versionName: String, versionCode: Int): AlertDialog {
        with(AlertDialog.Builder(context).setView(view)) {
            val dialog = this.create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
            return dialog
        }
    }
}