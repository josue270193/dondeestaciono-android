package uni.app.dondeestacionomobile.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.threeten.bp.OffsetDateTime
import uni.app.dondeestacionomobile.R
import uni.app.dondeestacionomobile.activity.HomeActivity
import uni.app.dondeestacionomobile.model.PhoneRegistrationDto
import uni.app.dondeestacionomobile.service.rest.NotificationService

const val NOTIFICATION_ID: String = "1000"
const val TOKEN_TAG: String = "TOKEN"

class PushMessageService : FirebaseMessagingService() {

    private val notificationService by lazy {
        NotificationService.create()
    }

    @SuppressLint("CheckResult")
    override fun onNewToken(token: String) {
        Log.d(TOKEN_TAG, "token: $token")
        val phoneRegistration = PhoneRegistrationDto()
        phoneRegistration.token = token
        phoneRegistration.dateTime = OffsetDateTime.now()
        notificationService.register(phoneRegistration)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({}, {
                Log.e(TOKEN_TAG, it.message)
            })
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        createNotificationChannel()
        val builder = buildNotification()
        showNotification(builder)
    }

    private fun showNotification(builder: NotificationCompat.Builder) {
        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID.toInt(), builder.build())
        }
    }

    private fun buildNotification(): NotificationCompat.Builder {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        return NotificationCompat.Builder(applicationContext, NOTIFICATION_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("Prueba Notification")
            .setContentText("Much longer text that cannot fit one line...")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Much longer text that cannot fit one line1\n" +
                                "Much longer text that cannot fit one line2\n" +
                                "Much longer text that cannot fit one line3"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}
