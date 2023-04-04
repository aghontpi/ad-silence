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
    fun aboutBuilder(context: Context, view: View, versionName: String, versionCode: Int) {
        val dpi = context.resources.displayMetrics.density
        with(AlertDialog.Builder(context).setView(view)) {
            val linearLayout = LinearLayout(context).also {
                it.setPadding(0, 16 * dpi.toInt(), 0, 0)
                it.gravity = Gravity.CENTER
                it.addView(
                    ImageView(context).also { imageView ->
                        imageView.layoutParams =
                            ViewGroup.LayoutParams(56 * dpi.toInt(), 56 * dpi.toInt())
                        imageView.setBackgroundResource(R.mipmap.ic_launcher_round)
                    }
                )
                it.addView(
                    LinearLayout(context).also { titleLayout ->
                        titleLayout.orientation = LinearLayout.VERTICAL
                        titleLayout.addView(
                            TextView(context).also { textView ->
                                textView.text =
                                    context.getString(R.string.app_name)
                                textView.setPadding(8 * dpi.toInt(), 0, 0, 0)
                                textView.textSize = 6 * dpi
                            }
                        )
                        titleLayout.addView(
                            TextView(context).also { textView ->
                                "v${versionName} (${versionCode})".also { textView.text = it }
                                textView.setPadding(8 * dpi.toInt(), 0, 0, 0)
                                textView.textSize = 4 * dpi
                            }
                        )
                    }
                )
            }
            this.setTitle(context.getString(R.string.app_name))
                .setIcon(R.mipmap.ic_launcher_round)
            this.setCustomTitle(linearLayout)
            this.show()
        }
    }
}