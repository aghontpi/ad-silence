package bluepie.ad_silence

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.ArrayList
import java.util.LinkedList
import java.util.Objects


sealed class NotificationListener : NotificationListenerService() {
    private val TAG = "NotificationListenerService"
    var mConnected  = false
    var isMuted = false

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

        if (sbn != null && sbn.packageName == "com.slipstream.accuradio"){

            // parse the notificaion with refection
            val notification = sbn.notification


            var fields: ArrayList<Any>? = null
            for(field in notification.contentView.javaClass.declaredFields){
                if(field.name == "mActions"){
                    field.isAccessible = true
                    fields = field.get(notification.contentView) as ArrayList<Any>
                }

            }

            Log.v(TAG, "some test")
            val info = java.util.LinkedList<String>()
            fields?.toArray()?.forEach {
               if (it!= null){
                  var fieldFilter = it.javaClass.declaredFields.filter{ it.name == "value"}
                   if (fieldFilter.size == 1){
                       var field = fieldFilter.get(0)
                       field.isAccessible = true

                       when(val _v = field.get(it)){
                          is String -> info.push(_v)
                          else -> "Not applicable"
                       }
                   }
               }
            }

            // necessary fields

            // mute
            // unmute
        }

    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        Log.v(TAG, "notification removed: ${sbn.toString()}")
    }

    override fun onLowMemory() {
        Log.v(TAG, "low memory trigger")
    }
}