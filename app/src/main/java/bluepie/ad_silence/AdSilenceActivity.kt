package bluepie.ad_silence

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html.fromHtml
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class AdSilenceActivity : Activity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ad_silence_activity)
        configurePermission()
        configureToggle()
        configureAdditionalViews()
    }

    override fun onResume() {
        super.onResume()
        Log.v(TAG, "onResume")
        // when resuming after permission is granted
        configurePermission()
        configureToggle()
        configureAdditionalViews()
    }

    private fun configurePermission() {
        findViewById<Button>(R.id.grant_permission)?.run {
            when (checkNotificationPermission(applicationContext)) {
                true -> {
                    this.text = getString(R.string.permission_granted)
                    this.isEnabled = false
                }
                false -> {
                    findViewById<Switch>(R.id.status_toggle)?.text =
                        getString(R.string.app_status_permission_not_granted)
                    this.isEnabled = true
                    this.setOnClickListener {
                        Log.v(TAG, "Opening Notification Settings")
                        startActivity(Intent(getString(R.string.notification_listener_settings_intent)))
                    }
                }
            }
        }
    }


    private fun configureToggle() {
        val preference = Preference(applicationContext)
        val statusToggle = findViewById<Switch>(R.id.status_toggle)
        val appNotificationHelper = AppNotificationHelper(applicationContext)
        val utils = Utils()

        if (!checkNotificationPermission(applicationContext)) {
            // even if appNotification is disabled, while granting permission
            //   force it to be enabled, otherwise it wont be listed in permission window
            appNotificationHelper.enable()
            utils.disableSwitch(statusToggle)
            statusToggle.text = getString(R.string.app_status_permission_not_granted)
            return
        } else {
            statusToggle.text = getString(R.string.app_status)
            utils.enableSwitch(statusToggle)
        }

        statusToggle.setOnClickListener {
            val toChange: Boolean = !preference.isEnabled()
            preference.setEnabled(toChange)
            if (toChange) appNotificationHelper.enable() else appNotificationHelper.disable()

        }

        if (preference.isEnabled()) {
            statusToggle.isChecked = true
            appNotificationHelper.start()
        } else {
            statusToggle.isChecked = false
        }

    }

    @SuppressLint("InflateParams")
    private fun configureAdditionalViews() {

        val utils = Utils()
        val dpi = resources.displayMetrics.density
        val isAccuradioInstalled = utils.isAccuradioInstalled(applicationContext)
        val isSpotifyInstalled = utils.isSpotifyInstalled(applicationContext)

        findViewById<Button>(R.id.about_btn)?.setOnClickListener {
            layoutInflater.inflate(R.layout.about, null)?.run {
                this.findViewById<Button>(R.id.github_button).setOnClickListener {
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.github_link))
                    ).run { startActivity(this) }
                }

                this.findViewById<Button>(R.id.report_issue_btn)?.setOnClickListener {
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(context.getString(R.string.github_issues_link))
                    ).run { startActivity(this) }
                }

                this.findViewById<TextView>(R.id.about_text_view).also {
                    it.movementMethod = LinkMovementMethod.getInstance()
                    it.text = fromHtml(getString(R.string.about_window_text))
                }

                with(AlertDialog.Builder(this@AdSilenceActivity).setView(this)) {
                    val linearLayout = LinearLayout(context).also {
                        it.setPadding(0, 16 * dpi.toInt(), 0, 0)
                        it.gravity = Gravity.CENTER
                        it.addView(
                            ImageView(context).also { imageView ->
                                imageView.layoutParams =
                                    ViewGroup.LayoutParams(56 * dpi.toInt(), 56 * dpi.toInt())
                                imageView.setBackgroundResource(R.mipmap.icon_launcher_round)
                            }
                        )
                        it.addView(
                            TextView(context).also { textView ->
                                textView.text = getString(R.string.app_name)
                                textView.setPadding(8 * dpi.toInt(), 0, 0, 0)
                                textView.textSize = 6 * dpi
                            }
                        )
                    }
                    this.setTitle(getString(R.string.app_name))
                        .setIcon(R.mipmap.icon_launcher_round)
                    this.setCustomTitle(linearLayout)
                    this.show()
                }
            }
        }


        findViewById<Button>(R.id.select_apps_btn)?.setOnClickListener {
            val appSelectionView = layoutInflater.inflate(R.layout.app_selection, null)
            val preference = Preference(applicationContext)

            appSelectionView.findViewById<Switch>(R.id.accuradio_selection_switch)?.run {
                this.isEnabled = isAccuradioInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.ACCURADIO)
                "${getString(R.string.accuradio)} ${
                    if (isAccuradioInstalled) "" else context.getString(
                        R.string.not_installed
                    )
                }".also { this.text = it }
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.ACCURADIO,
                        !preference.isAppConfigured(SupportedApps.ACCURADIO)
                    )
                }
            }

            appSelectionView.findViewById<Switch>(R.id.spotify_selection_switch)?.run {
                // change this to isSpotifyInstalled
                this.isEnabled = isSpotifyInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.SPOTIFY)
                "${getString(R.string.spotify)} ${
                    if (isSpotifyInstalled) "" else context.getString(
                        R.string.not_installed
                    )
                }".also {
                    this.text = it
                }
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.SPOTIFY,
                        !preference.isAppConfigured(SupportedApps.SPOTIFY)
                    )
                }
            }

            AlertDialog.Builder(this).setView(appSelectionView).show()
        }
    }

}



