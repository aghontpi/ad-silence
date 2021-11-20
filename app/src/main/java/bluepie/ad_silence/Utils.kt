package bluepie.ad_silence

import android.widget.Switch

class Utils {

   fun disableSwitch(toggle: Switch){
      toggle.isChecked = false
      toggle.isEnabled =  false
   }

   fun enableSwitch(toggle: Switch){
      toggle.isChecked = true
      toggle.isEnabled = true
   }
}

