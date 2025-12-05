package bluepie.ad_silence

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
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
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

class AdSilenceActivity : Activity() {

    private val TAG = "MainActivity"
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 6969
    private val SHOW_MOCK_DATA = false
    private var debugLogDialog: AlertDialog? = null

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
    }

    override fun onDestroy() {
        super.onDestroy()
        if (debugLogDialog != null && debugLogDialog!!.isShowing) {
            Log.v(TAG, "Dismissing debug log dialog")
            debugLogDialog!!.dismiss()
        }
    }

    private fun configurePermission() {
        notificationListenerPermission()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // android 13 and above
            notificationPostingPermission()
            decideAndActivePostingPermissionRequest()
        } else {
            // hide the notifications for android 13
            findViewById<Switch>(R.id.enable_notifications_switch)?.run {
                this.visibility = View.GONE
            }

            // hide for requesting permission for notification posting
            setNotificationPostingRequestPermission(false)

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


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun decideAndActivePostingPermissionRequest() {
        val activity = this
        val preference = Preference(applicationContext)


        findViewById<Switch>(R.id.enable_notifications_switch)?.run {
            this.isChecked = preference.isNotificationsEnabled()

            this.setOnClickListener {
                val changedState = !preference.isNotificationsEnabled()
                this.isChecked = changedState
                preference.setNotificationEnabled(changedState)
                setNotificationPostingRequestPermission(changedState)

                // remove any existing notifications
                // forground service notifications should be cancelled from service directly
                Log.v(TAG, "[configNotifications] removing any existing notifications")
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(
                    NOTIFICATION_ID
                )
            }

            // setup views for notification settings
            setNotificationPostingRequestPermission(preference.isNotificationsEnabled())
        }

        findViewById<Button>(R.id.grant_notification_posting_perimisison)?.run {

            if (preference.isNotificationPostingPermissionGranted()) {
                this.text = getString(R.string.granted)
                this.isEnabled = false
            }

            this.setOnClickListener {
                // todo
                // show alert dialog for information
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Log.v(TAG, "Toggling ON: Requesting Rebind (API >= 24)")
                    LogManager.addLifecycleLog(LogEntry(
                        appName = "AdSilence",
                        timestamp = System.currentTimeMillis(),
                        isAd = false,
                        title = "Service Rebind",
                        text = "Toggling ON: Requesting Rebind (API >= 24)",
                        subText = "Lifecycle Event"
                    ))
                    android.service.notification.NotificationListenerService.requestRebind(
                        android.content.ComponentName(this, NotificationListener::class.java)
                    )
                } else {
                    Log.v(TAG, "Toggling ON: Sending START_SERVICE intent (API < 24)")
                    LogManager.addLifecycleLog(LogEntry(
                        appName = "AdSilence",
                        timestamp = System.currentTimeMillis(),
                        isAd = false,
                        title = "Service Start",
                        text = "Toggling ON: Sending START_SERVICE intent (API < 24)",
                        subText = "Lifecycle Event"
                    ))
                    val intent = Intent(this, NotificationListener::class.java)
                    intent.action = "START_SERVICE"
                    startService(intent)
                }
            } else {
                // Turning OFF
                Log.v(TAG, "Toggling OFF: Sending STOP_SERVICE intent")
                LogManager.addLifecycleLog(LogEntry(
                    appName = "AdSilence",
                    timestamp = System.currentTimeMillis(),
                    isAd = false,
                    title = "Service Stop",
                    text = "Toggling OFF: Sending STOP_SERVICE intent",
                    subText = "Lifecycle Event"
                ))
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
        findViewById<TextView>(R.id.app_version)?.text = "$versionName"

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

                About().aboutBuilder(context, this, versionName, versionCode)
            }
        }

        findViewById<Button>(R.id.add_custom_app_btn)?.setOnClickListener {
            showAddCustomAppDialog()
        }

        findViewById<Button>(R.id.debug_log_btn)?.setOnClickListener {
            showDebugLogDialog()
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

            appSelectionView.findViewById<Switch>(R.id.tidal_selection_switch)?.run {
                this.isEnabled = isTidalInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.TIDAL)
                "${context.getString(R.string.tidal)} ${
                    if (isTidalInstalled) "" else context.getString(
                        R.string.not_installed
                    )
                }".also {
                    this.text = it
                }
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.TIDAL,
                        !preference.isAppConfigured(SupportedApps.TIDAL)
                    )
                }
            }

            appSelectionView.findViewById<Switch>(R.id.spotify_lite_selection_switch)?.run {
                this.isEnabled = isSpotifyLiteInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.SPOTIFY_LITE)
                "${context.getString(R.string.spotify_lite)} ${
                    if (isSpotifyLiteInstalled) "" else context.getString(
                        R.string.not_installed
                    )
                }".also {
                    this.text = it
                }
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.SPOTIFY_LITE,
                        !preference.isAppConfigured(SupportedApps.SPOTIFY_LITE)
                    )
                }
            }

            appSelectionView.findViewById<Switch>(R.id.pandora_selection_switch)?.run {
                this.isEnabled = isPandoraInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.PANDORA)
                "${context.getString(R.string.pandora)} ${
                    if (isPandoraInstalled) applicationContext.getString(R.string.beta) else context.getString(
                        R.string.not_installed
                    )
                }".also {
                    this.text = it
                }
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.PANDORA,
                        !preference.isAppConfigured(SupportedApps.PANDORA)
                    )
                }
            }

            appSelectionView.findViewById<Switch>(R.id.liveone_selection_switch)?.run {
                this.isEnabled = isLiveOneInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.LiveOne)
                "${context.getString(R.string.liveone)} ${
                    if (isLiveOneInstalled) applicationContext.getString(R.string.beta) else context.getString(
                        R.string.not_installed
                    )
                }".also {
                    this.text = it
                }
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.LiveOne,
                        !preference.isAppConfigured(SupportedApps.LiveOne)
                    )
                }
            }

            appSelectionView.findViewById<Switch>(R.id.soundcloud_selection_switch)?.run {
                this.isEnabled = isSoundcloudInstalled
                this.isChecked = preference.isAppConfigured(SupportedApps.Soundcloud)
                "${context.getString(R.string.soundcloud)} ${
                    if (isSoundcloudInstalled) "" else context.getString(R.string.not_installed)
                }".also {
                    this.text = it
                }
                this.setOnClickListener {
                    preference.setAppConfigured(
                        SupportedApps.Soundcloud,
                        !preference.isAppConfigured(SupportedApps.Soundcloud)
                    )
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
            dialog.show()
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
                    preference.setNotificationPostingPermission(true)
                } else {
                    Log.v(TAG, "[permission] permission not granted in dialog")
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
                dialog.show()
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
        
        customApps.forEach { customApp ->
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
                AlertDialog.Builder(this)
                    .setTitle("Delete Custom App")
                    .setMessage("Do you want to delete ${customApp.name}? It's not recoverable.")
                    .setPositiveButton("Yes, delete") { _, _ ->
                        preference.removeCustomApp(customApp.packageName)
                        populateCustomApps(container, preference)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            // Order: Label -> Edit -> Delete -> Switch
            row.addView(appNameView)
            row.addView(editIcon)
            row.addView(deleteIcon)
            row.addView(switch)
            container.addView(row)
        }
    }

    private fun showAddCustomAppDialog(appToEdit: CustomApp? = null, onSuccess: () -> Unit = {}) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_custom_app, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val titleView = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        if (appToEdit != null) {
            titleView.text = "Edit Custom App"
            dialogView.findViewById<EditText>(R.id.et_app_name).setText(appToEdit.name)
            dialogView.findViewById<EditText>(R.id.et_package_name).setText(appToEdit.packageName)
            dialogView.findViewById<EditText>(R.id.et_keywords).setText(appToEdit.keywords.joinToString(", "))
        } else {
            titleView.text = getString(R.string.add_custom_app)
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

            val keywords = keywordsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val customApp = CustomApp(appName, packageName, keywords, appToEdit?.isEnabled ?: true)

            if (appToEdit != null && appToEdit.packageName != packageName) {
                preference.removeCustomApp(appToEdit.packageName)
            }

            preference.addCustomApp(customApp)

            Toast.makeText(this, if (appToEdit != null) "Custom app updated" else "Custom app added", Toast.LENGTH_SHORT).show()
            onSuccess()
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showDebugLogDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_debug_log, null)
        debugLogDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val dialog = debugLogDialog!!

        val listView = dialogView.findViewById<ListView>(R.id.log_list_view)
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

        dialogView.findViewById<Button>(R.id.mock_log_btn)?.run {
            if (SHOW_MOCK_DATA) {
                this.visibility = View.VISIBLE
                this.setOnClickListener {
                    LogManager.addMockData()
                }
            } else {
                this.visibility = View.GONE
            }
        }

        dialogView.findViewById<Button>(R.id.clear_log_btn)?.setOnClickListener {
            LogManager.clearLogs()
        }

        dialog.setOnDismissListener {
            LogManager.removeListener(logListener)
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        if (!isFinishing) {
            dialog.show()
        }
    }

}



