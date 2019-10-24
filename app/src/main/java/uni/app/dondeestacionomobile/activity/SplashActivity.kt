package uni.app.dondeestacionomobile.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import uni.app.dondeestacionomobile.R

const val SPLASH_TIME = 1L

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Handler().postDelayed(
            {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            },
            SPLASH_TIME
        )
    }

}
