package bluepie.ad_silence

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Html
import android.text.Html.fromHtml
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.MotionEvent
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import android.media.MediaRouter
import android.media.AudioDeviceInfo
import android.media.AudioManager

class AdSilenceActivity : Activity() {

    private val TAG = "AdSilence.Activity"
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    private var aboutDialog: AlertDialog? = null
    private var batteryOptimizationDialog: AlertDialog? = null
    private var debugLogDialog: AlertDialog? = null
    private var appSelectionDialog: AlertDialog? = null
    private var addCustomAppDialog: AlertDialog? = null
    private var deleteCustomAppDialog: AlertDialog? = null
    private var settingsDialog: AlertDialog? = null
    private var miuiAutostartDialog: AlertDialog? = null
    private var hasOpenedAutostartSettings: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ad_silence)
        configurePermission()
        configureToggle()
        configureAdditionalViews()
        // handleHibernation()
        configureViewsWithLinks()
        configureBatteryOptimization()
    }

    override fun onResume() {
        super.onResume()

        Log.v(TAG, "onResume")
        // when resuming after permission is granted
        configurePermission()
        configureToggle()
        configureAdditionalViews()
        // handleHibernation()
        configureViewsWithLinks()
        configureBatteryOptimization()
        checkAndPromptForMiuiAutostart()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (debugLogDialog != null && debugLogDialog!!.isShowing) {
            Log.v(TAG, "Dismissing debug log dialog")
            debugLogDialog!!.dismiss()
        }
        if (aboutDialog != null && aboutDialog!!.isShowing) {
            Log.v(TAG, "Dismissing about dialog")
            aboutDialog!!.dismiss()
        }
        if (batteryOptimizationDialog != null && batteryOptimizationDialog!!.isShowing) {
            Log.v(TAG, "Dismissing battery optimization dialog")
            batteryOptimizationDialog!!.dismiss()
        }
        if (appSelectionDialog != null && appSelectionDialog!!.isShowing) {
            Log.v(TAG, "Dismissing app selection dialog")
            appSelectionDialog!!.dismiss()
        }
        if (addCustomAppDialog != null && addCustomAppDialog!!.isShowing) {
            Log.v(TAG, "Dismissing add custom app dialog")
            addCustomAppDialog!!.dismiss()
        }
        if (deleteCustomAppDialog != null && deleteCustomAppDialog!!.isShowing) {
            Log.v(TAG, "Dismissing delete custom app dialog")
            deleteCustomAppDialog!!.dismiss()
        }
        if (settingsDialog != null && settingsDialog!!.isShowing) {
            Log.v(TAG, "Dismissing settings dialog")
            settingsDialog!!.dismiss()
        }

        if (miuiAutostartDialog != null && miuiAutostartDialog!!.isShowing) {
            Log.v(TAG, "Dismissing miui autostart dialog")
            miuiAutostartDialog!!.dismiss()
        }
    }

    private fun configurePermission() {
        notificationListenerPermission()
        configureNotificationSwitch()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPostingPermission()
        }
    }

    private fun notificationListenerPermission() {
        // for notification listener
        findViewById<Button>(R.id.grant_permission)?.run {
            when (checkNotificationListenerPermission(applicationContext)) {
                true -> {
                    this.text = getString(R.string.permission_granted)
                    this.isEnabled = false
                }

                false -> {
                    findViewById<TextView>(R.id.status_subtitle)?.text =
                        getString(R.string.app_status_permission_not_granted)
                    this.isEnabled = true
                    this.setOnClickListener {
                        Log.v(TAG, "Opening Notification Settings")
                        val intent = Intent(getString(R.string.notification_listener_settings_intent))
                        if (intent.resolveActivity(applicationContext.packageManager) != null) {
                            startActivity(intent)
                        } else {
                            Log.w(TAG, "Notification settings activity not found!")
                            Toast.makeText(applicationContext, "Unable to open notification settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun configureNotificationSwitch() {
        val preference = Preference(applicationContext)
        val activity = this

        findViewById<Switch>(R.id.enable_notifications_switch)?.run {
            this.visibility = View.VISIBLE
            this.isChecked = preference.isNotificationsEnabled()

            this.setOnClickListener {
                val newState = !preference.isNotificationsEnabled()
                this.isChecked = newState
                preference.setNotificationEnabled(newState)

                if (newState) {
                    // Enabled
                    Log.v(TAG, "Notification Updates Enabled")
                    // If service is running, tell it to start foreground
                    val intent = Intent(activity, NotificationListener::class.java)
                    intent.action = "START_SERVICE"
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }

                    // On Android 13+, check/request permission immediately
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        setNotificationPostingRequestPermission(true)
                        if (!preference.isNotificationPostingPermissionGranted()) {
                             if (preference.isNotificationPermissionRequested()) {
                                // launch settings if already requested and denied
                                navigateToSettingsPageToGrantNotificationPostingPermission()
                            } else {
                                // Request permission directly
                                ActivityCompat.requestPermissions(
                                    activity,
                                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                    NOTIFICATION_PERMISSION_REQUEST_CODE
                                )
                            }
                        }
                    }
                } else {
                    // Disabled
                    Log.v(TAG, "Notification Updates Disabled")
                    // Tell service to stop foreground
                    val intent = Intent(activity, NotificationListener::class.java)
                    intent.action = "STOP_FOREGROUND"
                    startService(intent) // stopForeground logic doesn't require promotion
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        setNotificationPostingRequestPermission(false)
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // setup views for notification settings
            setNotificationPostingRequestPermission(preference.isNotificationsEnabled())
            
             findViewById<Button>(R.id.grant_notification_posting_perimisison)?.run {

                if (preference.isNotificationPostingPermissionGranted()) {
                    this.text = getString(R.string.granted)
                    this.isEnabled = false
                }

                this.setOnClickListener {
                    when (preference.isNotificationPermissionRequested()) {
                        true -> {
                            // launch permission only if it is not triggered for the user already
                            navigateToSettingsPageToGrantNotificationPostingPermission()
                        }

                        false -> {
                            ActivityCompat.requestPermissions(
                                activity,
                                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                NOTIFICATION_PERMISSION_REQUEST_CODE
                            )
                        }
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

        val statusSubtitle = findViewById<TextView>(R.id.status_subtitle)
        statusToggle.text = "" // Ensure switch has no text

        if (!checkNotificationListenerPermission(applicationContext)) {
            // even if appNotification is disabled, while granting permission
            //   force it to be enabled, otherwise it wont be listed in permission window
            appNotificationHelper.enable()
            utils.disableSwitch(statusToggle)
            statusSubtitle?.text = getString(R.string.app_status_permission_not_granted)
            return
        } else {
            statusSubtitle?.text = getString(R.string.app_status)
            utils.enableSwitch(statusToggle)
        }

        statusToggle.setOnClickListener {
            val toChange: Boolean = !preference.isEnabled()
            preference.setEnabled(toChange)
            
            if (toChange) {
                // Turning ON
                    startNotificationService()
            } else {
                // Turning OFF
                Log.v(TAG, "Toggling OFF: Sending STOP_SERVICE intent")
                if (preference.isDebugLogEnabled()) {
                    LogManager.addLifecycleLog(LogEntry(
                        appName = "AdSilence",
                        timestamp = System.currentTimeMillis(),
                        isAd = false,
                        title = "Service Stop",
                        text = "Toggling OFF: Sending STOP_SERVICE intent",
                        subText = "Lifecycle Event"
                    ))
                }
                val intent = Intent(this, NotificationListener::class.java)
                intent.action = "STOP_SERVICE"
                startService(intent)
            }
        }

        if (preference.isEnabled()) {
            statusToggle.isChecked = true
        } else {
            statusToggle.isChecked = false
        }

    }

    private fun configureAdditionalViews() {

        val utils = Utils()
        val isAccuradioInstalled = utils.isAccuradioInstalled(applicationContext)
        val isSpotifyInstalled = utils.isSpotifyInstalled(applicationContext)
        val isTidalInstalled = utils.isTidalInstalled(applicationContext)
        val isSpotifyLiteInstalled = utils.isSpotifyLiteInstalled(applicationContext)
        val isPandoraInstalled = utils.isPandoraInstalled(applicationContext)
        val isLiveOneInstalled = utils.isLiveOneInstalled(applicationContext)
        val isSoundcloudInstalled = utils.isSoundcloudInstalled(applicationContext)
        val versionCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME

        findViewById<Button>(R.id.settings_btn)?.setOnClickListener {
            showSettingsDialog()
        }

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

                this.findViewById<TextView>(R.id.tv_app_version)?.text = "$versionName ($versionCode)"

                aboutDialog = About().aboutBuilder(context, this, versionName, versionCode)
                aboutDialog?.setOnDismissListener {
                    aboutDialog = null
                }
                aboutDialog?.show()
            }
        }



        findViewById<Button>(R.id.debug_log_btn)?.setOnClickListener {
            showDebugLogDialog()
        }


        findViewById<Button>(R.id.select_apps_btn)?.setOnClickListener {
            val appSelectionView = layoutInflater.inflate(R.layout.app_selection, null)
            val preference = Preference(applicationContext)

            val updateResetButtonVisibility = { pkgName: String, resetBtn: View ->
                if (preference.isCustomAppConfigured(SupportedApps.CUSTOM, pkgName)) {
                    resetBtn.visibility = View.VISIBLE
                } else {
                    resetBtn.visibility = View.GONE
                }
            }

            appSelectionView.findViewById<Switch>(R.id.accuradio_selection_switch)?.run {
                this.isEnabled = isAccuradioInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.ACCURADIO)
                this.text = "" 
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.ACCURADIO,
                        !preference.isAppConfigured(SupportedApps.ACCURADIO)
                    )
                }
            }
            appSelectionView.findViewById<TextView>(R.id.tv_accuradio)?.text = 
                "${getString(R.string.accuradio)} ${if (isAccuradioInstalled) "" else getString(R.string.not_installed)}"
            val accuradioResetBtn = appSelectionView.findViewById<ImageView>(R.id.accuradio_reset_btn)
            updateResetButtonVisibility(getString(R.string.accuradio_pkg_name), accuradioResetBtn)
            accuradioResetBtn.setOnClickListener {
                showResetConfirmationDialog(getString(R.string.accuradio)) {
                    preference.removeCustomApp(getString(R.string.accuradio_pkg_name))
                    updateResetButtonVisibility(getString(R.string.accuradio_pkg_name), accuradioResetBtn)
                    Toast.makeText(this, "Reset to default", Toast.LENGTH_SHORT).show()
                }
            }
            appSelectionView.findViewById<ImageView>(R.id.accuradio_edit_btn)?.setOnClickListener {
                handlePreloadedAppEdit(SupportedApps.ACCURADIO, appSelectionView.findViewById(R.id.custom_apps_container), preference) {
                    updateResetButtonVisibility(getString(R.string.accuradio_pkg_name), accuradioResetBtn)
                }
            }

            appSelectionView.findViewById<Switch>(R.id.spotify_selection_switch)?.run {
                this.isEnabled = isSpotifyInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.SPOTIFY)
                this.text = ""
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.SPOTIFY,
                        !preference.isAppConfigured(SupportedApps.SPOTIFY)
                    )
                }
            }
            appSelectionView.findViewById<TextView>(R.id.tv_spotify)?.text = 
                "${getString(R.string.spotify)} ${if (isSpotifyInstalled) "" else getString(R.string.not_installed)}"
            val spotifyResetBtn = appSelectionView.findViewById<ImageView>(R.id.spotify_reset_btn)
            updateResetButtonVisibility(getString(R.string.spotify_package_name), spotifyResetBtn)
            spotifyResetBtn.setOnClickListener {
                showResetConfirmationDialog(getString(R.string.spotify)) {
                    preference.removeCustomApp(getString(R.string.spotify_package_name))
                    updateResetButtonVisibility(getString(R.string.spotify_package_name), spotifyResetBtn)
                    Toast.makeText(this, "Reset to default", Toast.LENGTH_SHORT).show()
                }
            }
            appSelectionView.findViewById<ImageView>(R.id.spotify_edit_btn)?.setOnClickListener {
                handlePreloadedAppEdit(SupportedApps.SPOTIFY, appSelectionView.findViewById(R.id.custom_apps_container), preference) {
                    updateResetButtonVisibility(getString(R.string.spotify_package_name), spotifyResetBtn)
                }
            }

            appSelectionView.findViewById<Switch>(R.id.tidal_selection_switch)?.run {
                this.isEnabled = isTidalInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.TIDAL)
                this.text = ""
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.TIDAL,
                        !preference.isAppConfigured(SupportedApps.TIDAL)
                    )
                }
            }
            appSelectionView.findViewById<TextView>(R.id.tv_tidal)?.text = 
                "${getString(R.string.tidal)} ${if (isTidalInstalled) "" else getString(R.string.not_installed)}"
            val tidalResetBtn = appSelectionView.findViewById<ImageView>(R.id.tidal_reset_btn)
            updateResetButtonVisibility(getString(R.string.tidal_package_name), tidalResetBtn)
            tidalResetBtn.setOnClickListener {
                 showResetConfirmationDialog(getString(R.string.tidal)) {
                    preference.removeCustomApp(getString(R.string.tidal_package_name))
                    updateResetButtonVisibility(getString(R.string.tidal_package_name), tidalResetBtn)
                    Toast.makeText(this, "Reset to default", Toast.LENGTH_SHORT).show()
                 }
            }
            appSelectionView.findViewById<ImageView>(R.id.tidal_edit_btn)?.setOnClickListener {
                handlePreloadedAppEdit(SupportedApps.TIDAL, appSelectionView.findViewById(R.id.custom_apps_container), preference) {
                    updateResetButtonVisibility(getString(R.string.tidal_package_name), tidalResetBtn)
                }
            }

            appSelectionView.findViewById<Switch>(R.id.spotify_lite_selection_switch)?.run {
                this.isEnabled = isSpotifyLiteInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.SPOTIFY_LITE)
                this.text = ""
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.SPOTIFY_LITE,
                        !preference.isAppConfigured(SupportedApps.SPOTIFY_LITE)
                    )
                }
            }
            appSelectionView.findViewById<TextView>(R.id.tv_spotify_lite)?.text = 
                "${getString(R.string.spotify_lite)} ${if (isSpotifyLiteInstalled) "" else getString(R.string.not_installed)}"
            val spotifyLiteResetBtn = appSelectionView.findViewById<ImageView>(R.id.spotify_lite_reset_btn)
            updateResetButtonVisibility(getString(R.string.spotify_lite_package_name), spotifyLiteResetBtn)
            spotifyLiteResetBtn.setOnClickListener {
                showResetConfirmationDialog(getString(R.string.spotify_lite)) {
                    preference.removeCustomApp(getString(R.string.spotify_lite_package_name))
                    updateResetButtonVisibility(getString(R.string.spotify_lite_package_name), spotifyLiteResetBtn)
                    Toast.makeText(this, "Reset to default", Toast.LENGTH_SHORT).show()
                }
            }
            appSelectionView.findViewById<ImageView>(R.id.spotify_lite_edit_btn)?.setOnClickListener {
                handlePreloadedAppEdit(SupportedApps.SPOTIFY_LITE, appSelectionView.findViewById(R.id.custom_apps_container), preference) {
                    updateResetButtonVisibility(getString(R.string.spotify_lite_package_name), spotifyLiteResetBtn)
                }
            }

            appSelectionView.findViewById<Switch>(R.id.pandora_selection_switch)?.run {
                this.isEnabled = isPandoraInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.PANDORA)
                this.text = ""
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.PANDORA,
                        !preference.isAppConfigured(SupportedApps.PANDORA)
                    )
                }
            }
            appSelectionView.findViewById<TextView>(R.id.tv_pandora)?.text = 
                "${getString(R.string.pandora)} ${if (isPandoraInstalled) getString(R.string.beta) else getString(R.string.not_installed)}"
            val pandoraResetBtn = appSelectionView.findViewById<ImageView>(R.id.pandora_reset_btn)
            updateResetButtonVisibility(getString(R.string.pandora_package_name), pandoraResetBtn)
            pandoraResetBtn.setOnClickListener {
                showResetConfirmationDialog(getString(R.string.pandora)) {
                    preference.removeCustomApp(getString(R.string.pandora_package_name))
                    updateResetButtonVisibility(getString(R.string.pandora_package_name), pandoraResetBtn)
                    Toast.makeText(this, "Reset to default", Toast.LENGTH_SHORT).show()
                }
            }
            appSelectionView.findViewById<ImageView>(R.id.pandora_edit_btn)?.setOnClickListener {
                handlePreloadedAppEdit(SupportedApps.PANDORA, appSelectionView.findViewById(R.id.custom_apps_container), preference) {
                    updateResetButtonVisibility(getString(R.string.pandora_package_name), pandoraResetBtn)
                }
            }

            appSelectionView.findViewById<Switch>(R.id.liveone_selection_switch)?.run {
                this.isEnabled = isLiveOneInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.LiveOne)
                this.text = ""
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.LiveOne,
                        !preference.isAppConfigured(SupportedApps.LiveOne)
                    )
                }
            }
            appSelectionView.findViewById<TextView>(R.id.tv_liveone)?.text = 
                "${getString(R.string.liveone)} ${if (isLiveOneInstalled) getString(R.string.beta) else getString(R.string.not_installed)}"
            val liveOneResetBtn = appSelectionView.findViewById<ImageView>(R.id.liveone_reset_btn)
            updateResetButtonVisibility(getString(R.string.liveOne_package_name), liveOneResetBtn)
            liveOneResetBtn.setOnClickListener {
                showResetConfirmationDialog(getString(R.string.liveone)) {
                    preference.removeCustomApp(getString(R.string.liveOne_package_name))
                    updateResetButtonVisibility(getString(R.string.liveOne_package_name), liveOneResetBtn)
                    Toast.makeText(this, "Reset to default", Toast.LENGTH_SHORT).show()
                }
            }
            appSelectionView.findViewById<ImageView>(R.id.liveone_edit_btn)?.setOnClickListener {
                handlePreloadedAppEdit(SupportedApps.LiveOne, appSelectionView.findViewById(R.id.custom_apps_container), preference) {
                    updateResetButtonVisibility(getString(R.string.liveOne_package_name), liveOneResetBtn)
                }
            }

            appSelectionView.findViewById<Switch>(R.id.soundcloud_selection_switch)?.run {
                this.isEnabled = isSoundcloudInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.Soundcloud)
                this.text = ""
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.Soundcloud,
                        !preference.isAppConfigured(SupportedApps.Soundcloud)
                    )
                }
            }
            appSelectionView.findViewById<TextView>(R.id.tv_soundcloud)?.text = 
                "${getString(R.string.soundcloud)} ${if (isSoundcloudInstalled) "" else getString(R.string.not_installed)}"
            val soundcloudResetBtn = appSelectionView.findViewById<ImageView>(R.id.soundcloud_reset_btn)
            updateResetButtonVisibility(getString(R.string.soundcloud_package_name), soundcloudResetBtn)
            soundcloudResetBtn.setOnClickListener {
                 showResetConfirmationDialog(getString(R.string.soundcloud)) {
                    preference.removeCustomApp(getString(R.string.soundcloud_package_name))
                    updateResetButtonVisibility(getString(R.string.soundcloud_package_name), soundcloudResetBtn)
                    Toast.makeText(this, "Reset to default", Toast.LENGTH_SHORT).show()
                 }
            }
            appSelectionView.findViewById<ImageView>(R.id.soundcloud_edit_btn)?.setOnClickListener {
                handlePreloadedAppEdit(SupportedApps.Soundcloud, appSelectionView.findViewById(R.id.custom_apps_container), preference) {
                    updateResetButtonVisibility(getString(R.string.soundcloud_package_name), soundcloudResetBtn)
                }
            }

            val isJioSaavnInstalled = utils.isJioSaavnInstalled(applicationContext)

            appSelectionView.findViewById<Switch>(R.id.jio_saavn_selection_switch)?.run {
                this.isEnabled = isJioSaavnInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.JIO_SAAVN)
                this.text = ""
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.JIO_SAAVN,
                        !preference.isAppConfigured(SupportedApps.JIO_SAAVN)
                    )
                }
            }
            appSelectionView.findViewById<TextView>(R.id.tv_jio_saavn)?.text = 
                "${getString(R.string.jio_saavn)} ${if (isJioSaavnInstalled) "" else getString(R.string.not_installed)}"
            val jioSaavnResetBtn = appSelectionView.findViewById<ImageView>(R.id.jio_saavn_reset_btn)
            updateResetButtonVisibility(getString(R.string.jio_saavn_pkg_name), jioSaavnResetBtn)
            jioSaavnResetBtn.setOnClickListener {
                 showResetConfirmationDialog(getString(R.string.jio_saavn)) {
                    preference.removeCustomApp(getString(R.string.jio_saavn_pkg_name))
                    updateResetButtonVisibility(getString(R.string.jio_saavn_pkg_name), jioSaavnResetBtn)
                    Toast.makeText(this, "Reset to default", Toast.LENGTH_SHORT).show()
                 }
            }
            appSelectionView.findViewById<ImageView>(R.id.jio_saavn_edit_btn)?.setOnClickListener {
                handlePreloadedAppEdit(SupportedApps.JIO_SAAVN, appSelectionView.findViewById(R.id.custom_apps_container), preference) {
                    updateResetButtonVisibility(getString(R.string.jio_saavn_pkg_name), jioSaavnResetBtn)
                }
            }

            val container = appSelectionView.findViewById<LinearLayout>(R.id.custom_apps_container)
            populateCustomApps(container, preference)

            appSelectionView.findViewById<Button>(R.id.btn_add_custom_app)?.setOnClickListener {
                showAddCustomAppDialog {
                    populateCustomApps(container, preference)
                }
            }

            val dialog = AlertDialog.Builder(this)
                .setView(appSelectionView)
                .create()

            appSelectionView.findViewById<Button>(R.id.close_btn)?.setOnClickListener {
                dialog.dismiss()
            }

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            appSelectionDialog = dialog
            appSelectionDialog?.setOnDismissListener {
                appSelectionDialog = null
            }
            appSelectionDialog?.show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val preference = Preference(applicationContext)

        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "[permission] permission granted in dialog")
                    if (preference.isDebugLogEnabled()) {
                        LogManager.addLifecycleLog(LogEntry(
                            appName = "AdSilence",
                            timestamp = System.currentTimeMillis(),
                            isAd = false,
                            title = "Permission Granted",
                            text = "Notification Posting Permission Granted",
                            subText = "Permission"
                        ))
                    }
                    preference.setNotificationPostingPermission(true)
                } else {
                    Log.v(TAG, "[permission] permission not granted in dialog")
                    if (preference.isDebugLogEnabled()) {
                        LogManager.addLifecycleLog(LogEntry(
                            appName = "AdSilence",
                            timestamp = System.currentTimeMillis(),
                            isAd = false,
                            title = "Permission Denied",
                            text = "Notification Posting Permission Denied",
                            subText = "Permission"
                        ))
                    }
                    preference.setNotificationPostingPermission(false)
                }
                preference.setNotificationPermissionRequested(true)
            }

        }
    }

    private fun setNotificationPostingRequestPermission(state: Boolean) {
        val status = if (state) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.notification_posting_permission_text_view)?.run {
            this.visibility = status
        }

        findViewById<Button>(R.id.grant_notification_posting_perimisison)?.run {
            this.visibility = status
        }
    }

    private fun navigateToSettingsPageToGrantNotificationPostingPermission() {
        val intent = Intent()
        val uri = Uri.fromParts("package", packageName, null)
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.data = uri
        startActivity(intent)
        finish()
    }

    private fun postANotificationA13() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // checking if notification is not present.. start it
            val mNotificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val notifications = mNotificationManager.activeNotifications
            Log.v(TAG, "[notification-a13][count]" + notifications.size)
            if (notifications.isEmpty()) {
                AppNotificationHelper(applicationContext).updateNotification("AdSilence, listening for ads")
            }
        }
    }

    private fun handleHibernation() {
        // todo handle hibernation for android <= 10
        val hibernation = Hibernation(applicationContext, this)
        val preference = Preference(applicationContext)

        val hibernationStaus = hibernation.isAppWhitelisted()
        preference.setHibernatonDisabledStatus(hibernationStaus)

        val disableHibernationButton = findViewById<Button>(R.id.disable_hibernation_button)
        val disableHibernationTextView = findViewById<TextView>(R.id.disable_hibernation_text_view)

        Log.v(TAG, "sdkInt -> ${Build.VERSION.SDK_INT}: ${Build.VERSION_CODES.R}")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            disableHibernationButton?.also {
                it.visibility = View.GONE
            }
            disableHibernationTextView?.also {
                it.visibility = View.GONE
            }

            return
        }

        disableHibernationTextView?.also {
            it.visibility = View.VISIBLE
            it.movementMethod = LinkMovementMethod.getInstance()
        }

        if (preference.isHibernationDisabled()) {
            // todo set up ui elements to show hibernation is disabled
            disableHibernationButton?.also {
                it.text = resources.getString(R.string.disable_hibernation_granted)
                it.isEnabled = false
            }
            return
        }

        disableHibernationButton?.also {
            it.text = resources.getString(R.string.disable_hibernation)
            it.isEnabled = true

            it.setOnClickListener {
                // todo investigate, when notification permission is granted, "Unused app settings" is greyed out
                //   when notification listener is granted permission (unused app settings is not required?)
                hibernation.redirectToDisableHibernation()
            }
        }

    }

    private fun setTextFromHtml(view: TextView, value: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            view.text = fromHtml(value, Html.FROM_HTML_MODE_LEGACY)
        } else {
            view.text = fromHtml(value);
        }

    }

    private fun configureViewsWithLinks() {
        val unableToGrantPermissionHelp = findViewById<TextView>(R.id.permission_issue_info)
        val disableHibernationTextView = findViewById<TextView>(R.id.disable_hibernation_text_view)
        disableHibernationTextView?.also {
            setTextFromHtml(it, getString(R.string.disable_hibernation_text_view));
            it.movementMethod = LinkMovementMethod.getInstance();
        }
        unableToGrantPermissionHelp?.also {
            setTextFromHtml(it, getString(R.string.cant_grant_permission_see_help_here))
            it.movementMethod = LinkMovementMethod.getInstance();
        }
    }

    private fun configureBatteryOptimization() {
        val batteryOptimizationContainer = findViewById<View>(R.id.battery_optimization_container)
        val batteryOptimizationSwitch = findViewById<Switch>(R.id.disable_battery_optimization_switch)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            batteryOptimizationContainer?.visibility = View.VISIBLE
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            val packageName = packageName
            
            val isIgnoring = powerManager.isIgnoringBatteryOptimizations(packageName)
            batteryOptimizationSwitch?.isChecked = isIgnoring
            
            val clickListener = View.OnClickListener {
                val dialogView = layoutInflater.inflate(R.layout.dialog_background_usage, null)
                val dialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create()

                dialogView.findViewById<Button>(R.id.btn_open_settings).setOnClickListener {
                    val intent = Intent()
                    intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                    startActivity(intent)
                    dialog.dismiss()
                }

                dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
                    dialog.dismiss()
                    batteryOptimizationSwitch?.isChecked = isIgnoring
                }
                
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                batteryOptimizationDialog = dialog
                batteryOptimizationDialog?.setOnDismissListener {
                    batteryOptimizationDialog = null
                }
                batteryOptimizationDialog?.show()
            }
            
            batteryOptimizationSwitch?.setOnClickListener(clickListener)
            batteryOptimizationContainer?.setOnClickListener(clickListener)
            
        } else {
            batteryOptimizationContainer?.visibility = View.GONE
        }
    }

    private fun populateCustomApps(container: LinearLayout, preference: Preference) {
        Log.v(TAG, "Populating custom apps")
        container.removeAllViews()
        val customApps = preference.getCustomApps()
        Log.v(TAG, "Found ${customApps.size} custom apps")
        
        val preloadedPackageNames = listOf(
            getString(R.string.accuradio_pkg_name),
            getString(R.string.spotify_package_name),
            getString(R.string.spotify_lite_package_name),
            getString(R.string.tidal_package_name),
            getString(R.string.pandora_package_name),
            getString(R.string.liveOne_package_name),
            getString(R.string.soundcloud_package_name),
            getString(R.string.jio_saavn_pkg_name)
        )

        customApps.filter { !preloadedPackageNames.contains(it.packageName) }.forEach { customApp ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = android.view.Gravity.CENTER_VERTICAL
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val appNameView = TextView(this)
            appNameView.text = customApp.name + " (custom)"
            
            val textParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            appNameView.layoutParams = textParams

            val switch = Switch(this)
            switch.isChecked = customApp.isEnabled
            switch.minHeight = (48 * resources.displayMetrics.density).toInt()
            val switchParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            switch.layoutParams = switchParams
            switch.setOnCheckedChangeListener { _, isChecked ->
                preference.setCustomAppEnabled(customApp.packageName, isChecked)
            }

            val editIcon = ImageView(this)
            editIcon.setImageResource(R.drawable.ic_edit)
            val iconParams = LinearLayout.LayoutParams(
                (48 * resources.displayMetrics.density).toInt(),
                (48 * resources.displayMetrics.density).toInt()
            )
            editIcon.layoutParams = iconParams
            editIcon.setPadding(16, 16, 16, 16)
            editIcon.setOnClickListener {
                showAddCustomAppDialog(customApp) {
                    populateCustomApps(container, preference)
                }
            }

            val deleteIcon = ImageView(this)
            deleteIcon.setImageResource(R.drawable.ic_delete)
            deleteIcon.layoutParams = iconParams
            deleteIcon.setPadding(16, 16, 16, 16)
            deleteIcon.setOnClickListener {
                val dialogView = layoutInflater.inflate(R.layout.dialog_delete_custom_app, null)
                deleteCustomAppDialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create()
                val dialog = deleteCustomAppDialog!!

                dialogView.findViewById<TextView>(R.id.tv_dialog_message).text = 
                    getString(R.string.delete_custom_app_message_format, customApp.name)

                dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
                    dialog.dismiss()
                }

                dialogView.findViewById<Button>(R.id.btn_delete).setOnClickListener {
                    preference.removeCustomApp(customApp.packageName)
                    if (preference.isDebugLogEnabled()) {
                        LogManager.addLog(LogEntry(
                            appName = "AdSilence",
                            timestamp = System.currentTimeMillis(),
                            isAd = false,
                            title = "Custom App Deleted",
                            text = "Deleted custom app: ${customApp.name} (${customApp.packageName})",
                            subText = "Settings"
                        ))
                    }
                    populateCustomApps(container, preference)
                    dialog.dismiss()
                }

                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                
                dialog.setOnDismissListener {
                    deleteCustomAppDialog = null
                }
                dialog.show()
            }

            // Order: Label -> Edit -> Delete -> Switch
            row.addView(appNameView)
            row.addView(editIcon)
            row.addView(deleteIcon)
            row.addView(switch)
            container.addView(row)
        }
    }

    private fun handlePreloadedAppEdit(
        app: SupportedApps, 
        container: LinearLayout, 
        preference: Preference, 
        onSuccess: () -> Unit = {}
    ) {
        val pkgNameId = when (app) {
            SupportedApps.ACCURADIO -> R.string.accuradio_pkg_name
            SupportedApps.SPOTIFY -> R.string.spotify_package_name
            SupportedApps.SPOTIFY_LITE -> R.string.spotify_lite_package_name
            SupportedApps.TIDAL -> R.string.tidal_package_name
            SupportedApps.PANDORA -> R.string.pandora_package_name
            SupportedApps.LiveOne -> R.string.liveOne_package_name
            SupportedApps.Soundcloud -> R.string.soundcloud_package_name
            SupportedApps.JIO_SAAVN -> R.string.jio_saavn_pkg_name
            else -> return
        }
        val pkgName = getString(pkgNameId)
        val appNameId = when(app) {
            SupportedApps.ACCURADIO -> R.string.accuradio
            SupportedApps.SPOTIFY -> R.string.spotify
            SupportedApps.SPOTIFY_LITE -> R.string.spotify_lite
            SupportedApps.TIDAL -> R.string.tidal
            SupportedApps.PANDORA -> R.string.pandora
            SupportedApps.LiveOne -> R.string.liveone
            SupportedApps.Soundcloud -> R.string.soundcloud
            SupportedApps.JIO_SAAVN -> R.string.jio_saavn
             else -> return
        }
        val appName = getString(appNameId)

        val customApps = preference.getCustomApps()
        val existingApp = customApps.find { it.packageName == pkgName }
        
        val appToEdit = existingApp ?: CustomApp(
            name = appName,
            packageName = pkgName,
            keywords = DefaultAppConfig.getDefaultKeywords(app, applicationContext),
            isEnabled = true, 
            unmuteDelay = Utils().getUnmuteDelay(app, pkgName, preference)
        )
        
        showAddCustomAppDialog(appToEdit, isPreloaded = true) {
            populateCustomApps(container, preference)
            onSuccess()
        }
    }

    private fun showResetConfirmationDialog(appName: String, onConfirm: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_custom_app, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.tv_dialog_title).text = getString(R.string.reset_app_configuration)

        dialogView.findViewById<TextView>(R.id.tv_dialog_message).text = 
            getString(R.string.reset_custom_app_message_format, appName)
        
        val deleteBtn = dialogView.findViewById<Button>(R.id.btn_delete)
        deleteBtn.text = getString(R.string.reset)
        deleteBtn.setOnClickListener {
            onConfirm()
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showAddCustomAppDialog(appToEdit: CustomApp? = null, isPreloaded: Boolean = false, onSuccess: () -> Unit = {}) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_custom_app, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val titleView = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        if (appToEdit != null) {
            titleView.text = if (isPreloaded) "Edit App Configuration" else "Edit Custom App"
            dialogView.findViewById<EditText>(R.id.et_app_name).setText(appToEdit.name)
            val pkgEdit = dialogView.findViewById<EditText>(R.id.et_package_name)
            pkgEdit.setText(appToEdit.packageName)
            if (isPreloaded) {
                pkgEdit.isEnabled = false
                pkgEdit.alpha = 0.5f
            }
            dialogView.findViewById<EditText>(R.id.et_keywords).setText(appToEdit.keywords.joinToString(", "))
            dialogView.findViewById<EditText>(R.id.et_unmute_delay).setText(appToEdit.unmuteDelay.toString())
        } else {
            titleView.text = getString(R.string.add_custom_app)
        }

        dialogView.findViewById<TextView>(R.id.tv_help_link)?.run {
            setTextFromHtml(this, getString(R.string.custom_app_help_text))
            this.movementMethod = LinkMovementMethod.getInstance()
        }

        dialogView.findViewById<EditText>(R.id.et_keywords)?.setOnTouchListener { v, event ->
            if (v.id == R.id.et_keywords) {
                val scrollView = dialogView.findViewById<ScrollView>(R.id.sv_content)
                scrollView.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_UP -> scrollView.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }

        dialogView.findViewById<Button>(R.id.btn_cancel)?.setOnClickListener {
            dialog.dismiss()
        }



        dialogView.findViewById<Button>(R.id.btn_save)?.setOnClickListener {
            val appName = dialogView.findViewById<EditText>(R.id.et_app_name).text.toString()
            val packageName = dialogView.findViewById<EditText>(R.id.et_package_name).text.toString()
            val keywordsText = dialogView.findViewById<EditText>(R.id.et_keywords).text.toString()

            if (appName.isBlank() || packageName.isBlank() || keywordsText.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val preference = Preference(applicationContext)
            val customApps = preference.getCustomApps()

            // Check for duplicates only if we are adding new or changing package name/name to something that exists (excluding self)
            val isRename = appToEdit != null && (appToEdit.packageName != packageName || appToEdit.name != appName)
            val isNew = appToEdit == null

            if (isNew || isRename) {
                if (customApps.any { (it.packageName == packageName || it.name == appName) && (appToEdit == null || it.packageName != appToEdit.packageName) }) {
                    Toast.makeText(this, "App with this name or package already exists", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val unmuteDelayText = dialogView.findViewById<EditText>(R.id.et_unmute_delay).text.toString()
            val unmuteDelay = unmuteDelayText.toLongOrNull() ?: 0L
            val keywords = keywordsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val customApp = CustomApp(appName, packageName, keywords, appToEdit?.isEnabled ?: true, unmuteDelay)

            if (appToEdit != null && appToEdit.packageName != packageName) {
                preference.removeCustomApp(appToEdit.packageName)
            }

            preference.addCustomApp(customApp)
            if (preference.isDebugLogEnabled()) {
                LogManager.addLog(LogEntry(
                    appName = "AdSilence",
                    timestamp = System.currentTimeMillis(),
                    isAd = false,
                    title = if (appToEdit != null) "Custom App Updated" else "Custom App Added",
                    text = "${if (appToEdit != null) "Updated" else "Added"} custom app: $appName ($packageName)",
                    subText = "Settings"
                ))
            }

            Toast.makeText(this, if (appToEdit != null) "Custom app updated" else "Custom app added", Toast.LENGTH_SHORT).show()
            onSuccess()
            dialog.dismiss()
        }



        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        addCustomAppDialog = dialog
        addCustomAppDialog?.setOnDismissListener {
            addCustomAppDialog = null
        }
        addCustomAppDialog?.show()
    }

    private fun showDebugLogDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_debug_log, null)
        debugLogDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val dialog = debugLogDialog!!

        val listView = dialogView.findViewById<ListView>(R.id.log_list_view)
    
        try {
            val uptimeTextView = dialogView.findViewById<TextView>(R.id.dialog_uptime_text_view)
            val startTime = NotificationListener.startTime
            if (startTime > 0) {
                val uptimeMillis = System.currentTimeMillis() - startTime
                val hours = uptimeMillis / (1000 * 60 * 60)
                val minutes = (uptimeMillis / (1000 * 60)) % 60
                val seconds = (uptimeMillis / 1000) % 60
                uptimeTextView?.text = String.format("Service running for: %dh %dm %ds", hours, minutes, seconds)
                uptimeTextView?.visibility = View.VISIBLE
            } else {
                uptimeTextView?.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting uptime text", e)
        }
    
        val adapter = LogAdapter(LogManager.getLogs())
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val log = adapter.getItem(position)
            log.isExpanded = !log.isExpanded
            adapter.notifyDataSetChanged()
        }

        val logListener = {
            runOnUiThread {
                adapter.updateLogs(LogManager.getLogs())
            }
        }
        LogManager.addListener(logListener)

        val preference = Preference(applicationContext)
        dialogView.findViewById<Switch>(R.id.debug_log_toggle)?.run {
            this.isChecked = preference.isDebugLogEnabled()
            this.setOnCheckedChangeListener { _, isChecked ->
                preference.setDebugLogEnabled(isChecked)
            }
        }


        val testMuteBtn = dialogView.findViewById<Button>(R.id.test_mute_btn)
        val castingStatusTextView = dialogView.findViewById<TextView>(R.id.dialog_casting_status_text_view)
        
        // MediaRouter and Status Logic FIRST so buttons can use it
        var mediaRouter: MediaRouter? = null
        var mediaRouterCallback: MediaRouter.Callback? = null
        var isStatusExpanded = false
        // function variable first to allow recursion/usage
        var updateCastingStatusRef: (() -> Unit)? = null

        try {
            mediaRouter = applicationContext.getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter

            fun updateCastingStatus() {
                val preference = Preference(applicationContext)
                if (!preference.isCastingMuteEnabled()) {
                    castingStatusTextView?.text = "Casting settings turned off"
                    castingStatusTextView?.visibility = View.VISIBLE
                    return
                }

                val sb = StringBuilder()
                var summaryText = ""

                // 1. Check MediaRouter
                val route = mediaRouter?.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
                val isRemoteRoute = route != null && route.playbackType == MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE
                
                if (isRemoteRoute) {
                    val info = "Endpoint (Router): ${route?.name} (Vol: ${route?.volume})"
                    sb.append("$info\n")
                    summaryText = "Casting: ${route?.name}"
                } else {
                    sb.append("Endpoint (Router): Local Phone\n")
                    summaryText = "Casting: None"
                }

                // 2. Check MediaController (Fallback)
                val mediaController = NotificationListener.instance?.getMediaControllerForCasting()
                if (mediaController != null) {
                    val pkg = mediaController.packageName
                    val playbackInfo = mediaController.playbackInfo
                    // Using literals to avoid import resolution issues with inner class PlaybackInfo
                    val type = if(playbackInfo?.playbackType == 2) "Remote" else "Local" // 2 = PLAYBACK_TYPE_REMOTE
                    
                    val volControl = when(playbackInfo?.volumeControl) {
                        2 -> "Absolute" // VOLUME_CONTROL_ABSOLUTE
                        1 -> "Relative" // VOLUME_CONTROL_RELATIVE
                        0 -> "Fixed"    // VOLUME_CONTROL_FIXED
                        else -> "Unknown"
                    }
                    
                    sb.append("Session (Token): $pkg\n")
                    sb.append(" - Type: $type\n")
                    sb.append(" - VolControl: $volControl\n")
                    sb.append(" - Vol: ${playbackInfo?.currentVolume}/${playbackInfo?.maxVolume}")
                    
                    Log.d(TAG, "Debug Controller: Pkg=$pkg, Type=$type, Control=$volControl, Vol=${playbackInfo?.currentVolume}")
                    
                    if (summaryText == "Casting: None") {
                        summaryText = "Casting: $pkg (Session)"
                    }
                } else {
                    sb.append("Session (Token): None found")
                }
                
                if (isStatusExpanded) {
                    castingStatusTextView?.text = "$summaryText ▼\n$sb"
                } else {
                    castingStatusTextView?.text = "$summaryText (Expand) ▶"
                }
                castingStatusTextView?.visibility = View.VISIBLE
            }
            // Assign to ref
            updateCastingStatusRef = ::updateCastingStatus
            
            castingStatusTextView?.setOnClickListener {
                isStatusExpanded = !isStatusExpanded
                updateCastingStatus()
            }
            
            updateCastingStatus()

            mediaRouterCallback = object : MediaRouter.SimpleCallback() {
                override fun onRouteSelected(router: MediaRouter, type: Int, route: MediaRouter.RouteInfo) {
                    updateCastingStatus()
                }
                override fun onRouteUnselected(router: MediaRouter, type: Int, route: MediaRouter.RouteInfo) {
                    updateCastingStatus()
                }
                override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
                    updateCastingStatus()
                }
            }
            mediaRouter?.addCallback(MediaRouter.ROUTE_TYPE_LIVE_AUDIO, mediaRouterCallback!!, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaRouter for debug dialog", e)
            castingStatusTextView?.text = "Casting: Error accessing MediaRouter"
            castingStatusTextView?.visibility = View.VISIBLE
        }

        fun updateMuteButtonState() {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            
            var isMuted = currentMusicVolume == 0

            // Check MediaRouter first (Primary Casting Method)
            val mediaRouter = getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter
            val route = mediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
            
            if (route != null && route.playbackType == MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE) {
                 Log.v(TAG, "Debug: Found Remote Route: ${route.name}, Volume: ${route.volume}")
                 // For remote, if volume is 0, consider it muted
                 isMuted = route.volume == 0
            } else {
                // Fallback to MediaController
                val mediaController = NotificationListener.instance?.getMediaControllerForCasting()
                if (mediaController != null) {
                    try {
                        val playbackInfo = mediaController.playbackInfo
                        if (playbackInfo != null) {
                            Log.v(TAG, "Cast PlaybackInfo Volume: ${playbackInfo.currentVolume}, Max: ${playbackInfo.maxVolume}")
                            isMuted = playbackInfo.currentVolume == 0 
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting playback info", e)
                    }
                }
            }
            
            testMuteBtn?.text = if (isMuted) "Unmute" else "Mute"
        }    
        
        updateMuteButtonState()

        var originalVolume = -1

        testMuteBtn?.setOnClickListener {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager 
            
            // 1. Check MediaRouter (Preferred for Cast)
            val mediaRouter = getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter
            val route = mediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
            val isRemoteCasting = route != null && route.playbackType == MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE

            // 2. Check MediaController (Fallback)
            val mediaController = if (!isRemoteCasting) NotificationListener.instance?.getMediaControllerForCasting() else null
            
            var isCurrentlyMuted = false
            
            if (isRemoteCasting) {
                 isCurrentlyMuted = route!!.volume == 0
            } else if (mediaController != null) {
                try {
                     isCurrentlyMuted = mediaController.playbackInfo?.currentVolume == 0
                } catch (e: Exception) {
                    isCurrentlyMuted = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
                }
            } else {
                isCurrentlyMuted = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
            }
            
            if (!isCurrentlyMuted) {
                // MUTE ACTION
                if (isRemoteCasting) {
                     Log.v(TAG, "Debug Mute: Muting Remote Route ${route?.name}")
                     route!!.requestSetVolume(0)
                     Toast.makeText(applicationContext, "Muted Cast (MediaRouter)", Toast.LENGTH_SHORT).show()
                } else if (mediaController != null) {
                    Log.v("AdSilence", "Muting via MediaController")
                    try {
                        originalVolume = mediaController.playbackInfo?.currentVolume ?: -1
                    } catch (e: Exception) {
                        originalVolume = -1
                    }
                    mediaController.setVolumeTo(0, 0)
                    if (preference.isDebugLogEnabled()) {
                        LogManager.addLog(LogEntry(
                            appName = "AdSilence",
                            timestamp = System.currentTimeMillis(),
                            isAd = false,
                            title = "Test Mute",
                            text = "Muted Cast Stream via MediaController",
                            subText = "Debug Test"
                        ))
                    }
                    Toast.makeText(applicationContext, "Muted Cast (Session)", Toast.LENGTH_SHORT).show()
                } else {
                    Log.v("AdSilence", "Muting via AudioManager (Fallback)")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                    } else {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                    }
                    Toast.makeText(applicationContext, "Muted Local Stream", Toast.LENGTH_SHORT).show()
                }
            } else {
                // UNMUTE ACTION
                 if (isRemoteCasting) {
                     Log.v(TAG, "Debug Unmute: Unmuting Remote Route ${route?.name}")
                     val maxVol = route!!.volumeMax
                     val targetVol = if (maxVol > 0) maxVol / 2 else 5
                     route.requestSetVolume(targetVol)
                     Toast.makeText(applicationContext, "Unmuted Cast (MediaRouter)", Toast.LENGTH_SHORT).show()
                } else if (mediaController != null) {
                    Log.v("AdSilence", "Unmuting via MediaController")
                    if (originalVolume != -1) {
                        mediaController.setVolumeTo(originalVolume, 0)
                    } else {
                        val max = mediaController.playbackInfo?.maxVolume ?: 50
                        val safeVol = (max / 3).coerceAtLeast(1)
                        mediaController.setVolumeTo(safeVol, 0)
                    }
                    if (preference.isDebugLogEnabled()) {
                         LogManager.addLog(LogEntry(
                            appName = "AdSilence",
                            timestamp = System.currentTimeMillis(),
                            isAd = false,
                            title = "Test Unmute",
                            text = "Unmuted Cast Stream via MediaController",
                            subText = "Debug Test"
                        ))
                    }
                    Toast.makeText(applicationContext, "Unmuted Cast (Session)", Toast.LENGTH_SHORT).show()
                } else {
                     Log.v("AdSilence", "Unmuting via AudioManager (Fallback)")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                    } else {
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume / 3, 0)
                    }
                    Toast.makeText(applicationContext, "Unmuted Local Stream", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Delay update to allow async volume change
            listView.postDelayed({
                 updateMuteButtonState()
                 updateCastingStatusRef?.invoke() // Update text, when changed volume by using mute/unmute button
            }, 500)
        }

        dialogView.findViewById<Button>(R.id.clear_log_btn)?.setOnClickListener {
            LogManager.clearLogs()
        }

        dialog.setOnDismissListener {
            LogManager.removeListener(logListener)
            mediaRouterCallback?.let {
                 mediaRouter?.removeCallback(it)
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        if (!isFinishing) {
            dialog.show()
        }
    }

    private fun checkNotificationListenerPermission(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkNotificationPostingPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun notificationPostingPermission() {
        //todo if greater than android 13, show button below grant permission showing grant notification posting permission.
        //     clicking on it will tirgger this code, if user cancels, launch settings directly and ask them to grant the permission.

        val preference = Preference(applicationContext)
        Log.v(
            TAG,
            "[permission][isAlreadyRequestedNotificationPosting] -> " + preference.isNotificationPermissionRequested()
        )
        Log.v(
            TAG,
            "[permission][isGrantedPostingPermission] -> " + preference.isNotificationPostingPermissionGranted()
        )

        if (checkNotificationPostingPermission(applicationContext)) {
            Log.v(TAG, "[permission][notification][permissionGranted]")
            preference.setNotificationPostingPermission(true)
            return
        } else {
            Log.v(TAG, "[permission][notification][permissionDenied]")
            preference.setNotificationPostingPermission(false)
        }
        if (preference.isNotificationPermissionRequested()) {
            Log.v(TAG, "[permission] notification permission already requested,user denied")
            return
        }
        Log.v(TAG, "[permission] notification permission not granted")
    }


    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val preference = Preference(applicationContext)

        dialogView.findViewById<TextView>(R.id.tv_mute_behavior_help)?.run {
            setTextFromHtml(this, getString(R.string.mute_behavior_help))
            this.movementMethod = LinkMovementMethod.getInstance()
        }

        dialogView.findViewById<Switch>(R.id.switch_mute_entire_device)?.apply {
            isChecked = preference.isMuteEntireDeviceEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                preference.setMuteEntireDeviceEnabled(isChecked)
                if (preference.isDebugLogEnabled()) {
                    LogManager.addLog(LogEntry(
                        appName = "AdSilence",
                        timestamp = System.currentTimeMillis(),
                        isAd = false,
                        title = "Setting Changed",
                        text = "Mute Entire Device: $isChecked",
                        subText = "Settings"
                    ))
                }
            }
        }

        dialogView.findViewById<Switch>(R.id.switch_force_mute_no_check)?.apply {
            isChecked = preference.isForceMuteNoCheckEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                preference.setForceMuteNoCheckEnabled(isChecked)
                if (preference.isDebugLogEnabled()) {
                    LogManager.addLog(LogEntry(
                        appName = "AdSilence",
                        timestamp = System.currentTimeMillis(),
                        isAd = false,
                        title = "Setting Changed",
                        text = "Force Mute: $isChecked",
                        subText = "Settings"
                    ))
                }
            }
        }

        dialogView.findViewById<Switch>(R.id.switch_casting_mute)?.apply {
            isChecked = preference.isCastingMuteEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                preference.setCastingMuteEnabled(isChecked)
                if (preference.isDebugLogEnabled()) {
                    LogManager.addLog(LogEntry(
                        appName = "AdSilence",
                        timestamp = System.currentTimeMillis(),
                        isAd = false,
                        title = "Setting Changed",
                        text = "Casting Mute: $isChecked",
                        subText = "Settings"
                    ))
                }
            }
        }

        dialogView.findViewById<Button>(R.id.btn_close_settings)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        settingsDialog = dialog
        dialog.setOnDismissListener {
            settingsDialog = null
        }
        dialog.show()
    }


    private fun isXiaomi(): Boolean {
        return Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) ||
               Build.MANUFACTURER.equals("redmi", ignoreCase = true) ||
               Build.MANUFACTURER.equals("poco", ignoreCase = true)
    }

    private fun checkAndPromptForMiuiAutostart() {
        if (!isXiaomi()) return
        
        val preference = Preference(applicationContext)
        
        // If we just came back from Autostart settings, try to restart the service
        if (hasOpenedAutostartSettings) {
             Log.v(TAG, "Returned from Autostart Settings. Attempting to restart service.")
             startNotificationService()
             hasOpenedAutostartSettings = false
             return // Don't show dialog immediately again
        }

        // Only checking if
        // 1. App is enabled
        // 2. Notification permission is granted (so we expect service to run)
        // 3. Service instance is null (it's not running)
        
        if (preference.isEnabled() && 
            checkNotificationListenerPermission(applicationContext) && 
            NotificationListener.instance == null) {
                
            Log.v(TAG, "MIUI Device detected & Service not running. Prompting Autostart.")
            showMiuiAutostartDialog()
        }
    }

    private fun showMiuiAutostartDialog() {
        if (miuiAutostartDialog != null && miuiAutostartDialog!!.isShowing) {
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_custom_app, null) // Reuse generic dialog layout
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.tv_dialog_title).text = getString(R.string.miui_autostart_title)
        dialogView.findViewById<TextView>(R.id.tv_dialog_message).text = getString(R.string.miui_autostart_message)
        
        val openSettingsBtn = dialogView.findViewById<Button>(R.id.btn_delete)
        openSettingsBtn.text = getString(R.string.miui_autostart_button)
        openSettingsBtn.setOnClickListener {
            try {
                val intent = Intent()
                intent.component = android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
                startActivity(intent)
                hasOpenedAutostartSettings = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open MIUI Autostart settings", e)
                Toast.makeText(this, "Could not open Autostart settings directly. Please go to Autostart settings and enable it manually.", Toast.LENGTH_LONG).show()
            }
            dialog.dismiss()
        }

        val ignoreBtn = dialogView.findViewById<Button>(R.id.btn_cancel)
        ignoreBtn.text = getString(R.string.miui_autostart_ignore)
        ignoreBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        miuiAutostartDialog = dialog
        dialog.setOnDismissListener {
            miuiAutostartDialog = null
        }
        dialog.show()
    }

    private fun startNotificationService() {
        val preference = Preference(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.v(TAG, "Requesting Rebind (API >= 24)")
            if (preference.isDebugLogEnabled()) {
                LogManager.addLifecycleLog(LogEntry(
                    appName = "AdSilence",
                    timestamp = System.currentTimeMillis(),
                    isAd = false,
                    title = "Service Rebind",
                    text = "Requesting Rebind (API >= 24)",
                    subText = "Lifecycle Event"
                ))
            }
            try {
                android.service.notification.NotificationListenerService.requestRebind(
                    android.content.ComponentName(this, NotificationListener::class.java)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request rebind", e)
            }
        } else {
            Log.v(TAG, "Sending START_SERVICE intent (API < 24)")
             if (preference.isDebugLogEnabled()) {
                LogManager.addLifecycleLog(LogEntry(
                    appName = "AdSilence",
                    timestamp = System.currentTimeMillis(),
                    isAd = false,
                    title = "Service Start",
                    text = "Sending START_SERVICE intent (API < 24)",
                    subText = "Lifecycle Event"
                ))
            }
            val intent = Intent(this, NotificationListener::class.java)
            intent.action = "START_SERVICE"
            startService(intent)
        }
    }
}



