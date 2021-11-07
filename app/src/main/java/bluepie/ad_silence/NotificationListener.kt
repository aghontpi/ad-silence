package bluepie.ad_silence

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log


class NotificationListener : NotificationListenerService() {
    private val TAG = "NotificationListenerService"
    var mConnected  = false

    override fun onCreate() {
        super.onCreate()
        Log.v(TAG,"listener created")
    }

    override fun onListenerConnected() {
        Log.v(TAG, "notification listener connected")
        mConnected = true
    }

    override fun onListenerDisconnected() {
        Log.v(TAG, "notification listener disconnected")
        mConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        Log.v(TAG, "new notification posted: ${sbn.toString()}")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        Log.v(TAG, "notification removed: ${sbn.toString()}")
    }

    override fun onLowMemory() {
        Log.v(TAG, "low memory trigger")
    }
}