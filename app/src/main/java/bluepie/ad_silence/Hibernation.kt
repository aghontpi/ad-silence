package bluepie.ad_silence

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.content.UnusedAppRestrictionsConstants
import com.google.common.util.concurrent.ListenableFuture

class Hibernation(private val context: Context, private val activity: AdSilenceActivity) {
    private val TAG = "Hibernation"
    val HIBERNATION_REQUEST_CODE = 6868

    private var future: ListenableFuture<Int> =
        PackageManagerCompat.getUnusedAppRestrictionsStatus(context)

    fun isAppWhitelisted(): Boolean {
        val pm = context.packageManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val result = pm.isAutoRevokeWhitelisted
            Log.v(TAG, "[isAppWhitelisted] $result")
            result
        } else {
            // todo
            Log.v(TAG, "[isAppWhitelisted] version < R")
            false
        }
    }

    fun redirectToDisableHibernation() {
        this.future.addListener({ onResult(future.get()) }, ContextCompat.getMainExecutor(context))
    }

    fun onResult(appRestrictionsStatus: Int) {
        when(appRestrictionsStatus) {
            // Couldn't fetch status. Check logs for details.
            UnusedAppRestrictionsConstants.ERROR -> {
               Log.v(TAG, "[hibernation] error fetching app restriction status")
            }

            UnusedAppRestrictionsConstants.DISABLED -> {
                Log.v(TAG, "[hibernation] user has disabled hibernation for the app")
            }

            // Restrictions don't apply to your app on this device.
            UnusedAppRestrictionsConstants.FEATURE_NOT_AVAILABLE -> {
                Log.v(TAG, "[hibernation] feature_not_available")
            }

            // If the user doesn't start your app for a few months, the system will
            // place restrictions on it. See the API_* constants for details.
            UnusedAppRestrictionsConstants.API_30_BACKPORT, UnusedAppRestrictionsConstants.API_30, UnusedAppRestrictionsConstants.API_31 -> this.handleRestrictions(appRestrictionsStatus)
        }
    }

    fun handleRestrictions(appRestrictionsStatus: Int) {
        // todo check the statuscode & update shareedPreference to show the status in the mainActivity


        // If your app works primarily in the background, you can ask the user
        // to disable these restrictions. Check if you have already asked the
        // user to disable these restrictions. If not, you can show a message to
        // the user explaining why permission auto-reset or app hibernation should be
        // disabled. Then, redirect the user to the page in system settings where they
        // can disable the feature.
         val intent = IntentCompat.createManageUnusedAppRestrictionsIntent(context, BuildConfig.APPLICATION_ID)

        // You must use startActivityForResult(), not startActivity(), even if
        // you don't use the result code returned in onActivityResult().
        this.activity.startActivityForResult(intent, HIBERNATION_REQUEST_CODE)
    }
}
