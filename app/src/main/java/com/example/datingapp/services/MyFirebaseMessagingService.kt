package com.example.datingapp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager // Import PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat // Import ContextCompat for permission check
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.datingapp.models.FcmTokenRequest
import com.example.datingapp.R // Import your R file to access drawable/mipmap resources
import com.example.datingapp.api.RetrofitClient
import com.example.datingapp.activities.HomeActivity
import com.yourpackage.yourapp.auth.SessionManager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"
    private val CHANNEL_ID = "date_request_channel_id"

    companion object {
        const val ACTION_NEW_CHAT_MESSAGE = "com.example.datingapp.ACTION_NEW_CHAT_MESSAGE"
        const val ACTION_LIKE_NOTIFICATION = "com.example.datingapp.ACTION_LIKE_NOTIFICATION"
        const val ACTION_DATE_REQUEST = "com.example.datingapp.ACTION_DATE_REQUEST"
        const val ACTION_GENERIC_NOTIFICATION = "com.example.datingapp.ACTION_GENERIC_NOTIFICATION"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }



    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "onMessageReceived triggered! Message from: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            val sessionManager = SessionManager(this)
            val currentUserId = sessionManager.getUserId()
            val notiType = remoteMessage.data["type"]?.toString() ?: ""
            val receiverId = remoteMessage.data["receiver_id"]?.toString()

            if (currentUserId != -1 && receiverId != null && receiverId == currentUserId.toString()) {

                val broadcastIntent = Intent()
                broadcastIntent.putExtra("notification_type", notiType)

                for ((key, value) in remoteMessage.data) {
                    broadcastIntent.putExtra(key, value)
                }

                when (notiType) {
                    "New message" -> {
                        broadcastIntent.action = ACTION_NEW_CHAT_MESSAGE
                        Log.d(TAG, "Local broadcast sent: ACTION_NEW_CHAT_MESSAGE")
                    }
                    "Someone liked you" -> {
                        broadcastIntent.action = ACTION_LIKE_NOTIFICATION
                        Log.d(TAG, "Local broadcast sent: ACTION_LIKE_NOTIFICATION")
                    }
                    "date_request" -> { // <-- THIS IS THE KEY CHANGE FOR DATE REQUESTS
                        broadcastIntent.action = ACTION_DATE_REQUEST
                        Log.d(TAG, "Local broadcast sent: ACTION_DATE_REQUEST")
                    }
                    else -> {
                        broadcastIntent.action = ACTION_GENERIC_NOTIFICATION
                        Log.d(TAG, "Local broadcast sent: ACTION_GENERIC_NOTIFICATION (unknown type: $notiType)")
                    }
                }

                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
                Log.d(TAG, "Local broadcast sent with action: ${broadcastIntent.action}. Receiver ID: ${receiverId}, Current User ID: ${currentUserId}")

                val titleForSystemNoti = remoteMessage.notification?.title ?: remoteMessage.data["title"]
                val bodyForSystemNoti = remoteMessage.notification?.body ?: remoteMessage.data["body"]

                if (titleForSystemNoti != null && bodyForSystemNoti != null) {
                    handleCustomNotification(remoteMessage.data.toMutableMap().apply {
                        put("title", titleForSystemNoti)
                        put("body", bodyForSystemNoti)
                    })
                } else {
                    Log.w(TAG, "Missing title or body for system notification for type: $notiType")
                }

            } else {
                Log.d(TAG, "Message received but receiver_id (${receiverId}) does not match current userId (${currentUserId}). Or user not logged in.")
            }
        } else {
            Log.d(TAG, "Remote message data payload is empty. Checking for direct 'notification' payload.")
            remoteMessage.notification?.let { notification ->
                Log.d(TAG, "Message Notification Body: ${notification.body}")
                handleCustomNotification(mapOf("title" to notification.title.orEmpty(), "body" to notification.body.orEmpty()))
            }
        }
    }

    private fun sendRegistrationToServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionManager = SessionManager(applicationContext)
                val currentUserId = sessionManager.getUserId()

                if (currentUserId != -1) {
                    val requestBody = FcmTokenRequest(token = token)
                    val response = RetrofitClient.apiService.registerFCMToken(requestBody)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Log.d(TAG, "FCM token successfully registered on backend for user ID: $currentUserId!")
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e(TAG, "Failed to register FCM token for user ID: $currentUserId: ${response.code()} - $errorBody")
                        }
                    }
                } else {
                    Log.w(TAG, "User not logged in. Skipping FCM token registration on backend.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering FCM token: ${e.message}", e)
            }
        }
    }

    private fun handleCustomNotification(data: Map<String, String>) {
        Log.d(TAG, "handleCustomNotification called with data: $data")
        val title = data["title"] ?: "New Notification"
        val body = data["body"] ?: "You have a new update."

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission denied. Cannot show system notification on API 33+.")
                return
            }
        }

        val notificationId = System.currentTimeMillis().toInt()

        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action = "com.example.datingapp.VIEW_NOTIFICATION"
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        NotificationManagerCompat.from(this).notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "System notification shown with ID: $notificationId, Title: $title")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Date Request Notifications"
            val descriptionText = "Notifications for incoming date requests and updates."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification Channel '$CHANNEL_ID' created.")
        }
    }
}