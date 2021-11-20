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

   fun isAccuradioInstalled(context: Context): Boolean{
      return try {
          context.packageManager.getPackageInfo(context.getString(R.string.accuradio_pkg_name),0)
          true
      } catch (e: PackageManager.NameNotFoundException){
         Log.v(TAG, "exception: ${e.toString()}" )
         false
      }
   }

}

