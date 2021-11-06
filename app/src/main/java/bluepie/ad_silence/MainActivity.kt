package bluepie.ad_silence

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button

const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        (findViewById<Button>(R.id.vol_inc_tmp)).setOnClickListener {
            var msg = "Volume Inc"
            Log.v(TAG, msg)
        }

        (findViewById<Button>(R.id.vol_dec_tmp)).setOnClickListener {
            var msg = "Volume Dec"
            Log.v(TAG, msg)
        }
    }
}