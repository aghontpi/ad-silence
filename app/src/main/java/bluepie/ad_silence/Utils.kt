package bluepie.ad_silence

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Switch

class Utils {
   private val TAG = "Utils"

   fun disableSwitch(toggle: Switch){
      toggle.isChecked = false
      toggle.isEnabled =  false
   }

   fun enableSwitch(toggle: Switch){
      toggle.isChecked = true
      toggle.isEnabled = true
   }

   private fun isPackageInstalled(context: Context, packageName: String): Boolean{
      return try {
          context.packageManager.getPackageInfo(packageName,0)
          true
      } catch (e: PackageManager.NameNotFoundException){
         Log.v(TAG, "exception: $e" )
         false
      }
   }

   fun isAccuradioInstalled(context: Context) = isPackageInstalled(context, context.getString(R.string.accuradio_pkg_name))

   fun isSpotifyInstalled(context: Context) = isPackageInstalled(context, context.getString(R.string.spotify_package_name))
}

