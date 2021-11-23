package co.id.rikihikmianto.myworkmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.SyncHttpClient
import cz.msebera.android.httpclient.Header
import org.json.JSONObject
import java.text.DecimalFormat

class MyWorker(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {
    companion object {
        private val TAG = MyWorker::class.java.simpleName
        const val APP_ID = "YOUR_KEY_HERE"
        const val EXTRA_CITY = "city"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "channel_01"
        const val CHANNEL_NAME = "Decoding channel"
    }

    private var resultStatus: Result? = null

    override fun doWork(): Result {
        val dataCity = inputData.getString(EXTRA_CITY)
        return getCurrentWeather(dataCity)
    }

    private fun getCurrentWeather(dataCity: String?): Result {
        Log.d(TAG, "getCurrentWeather: Started....")
        Looper.prepare()
        val client = SyncHttpClient()
        val url =
            "https://api.openweathermap.org/data/2.5/weather?q=$dataCity&appid=8a0d7b4e7c38fbb156adedd93306f46a"
        Log.d(TAG, "getCurrentWeather: $url")
        client.post(url, object : AsyncHttpResponseHandler() {
            override fun onSuccess(
                statusCode: Int,
                headers: Array<out Header>?,
                responseBody: ByteArray?
            ) {
                val result = String(responseBody!!)
                Log.d(TAG, result)
                try {
                    val responseObjects = JSONObject(result)
                    val currentWeather: String =
                        responseObjects.getJSONArray("weather").getJSONObject(0).getString("main")
                    val description: String =
                        responseObjects.getJSONArray("weather").getJSONObject(0)
                            .getString("description")
                    val tempInKelvin = responseObjects.getJSONObject("main").getDouble("temp")
                    val tempInCelsius = tempInKelvin - 273
                    val temperature: String = DecimalFormat("##.##").format(tempInCelsius)
                    val title = "Current Weather in $dataCity"
                    val message = "$currentWeather, $description with $temperature celsius"
                    showNotification(title, message)
                    Log.d(TAG, "onSuccess: Finished....")
                    resultStatus = Result.success()
                } catch (e: Exception) {
                    showNotification("Get Current Weather Not Success", e.message.toString())
                    Log.d(TAG, "onSuccess: Failed...")
                    resultStatus = Result.failure()
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Array<out Header>?,
                responseBody: ByteArray?,
                error: Throwable?
            ) {
                Log.d(TAG, "onFailure: Failed...")
                // ketika proses gagal, maka jobFinished diset dengan parameter true. yang artinya job perlu di reshedule
                showNotification("Get Current Weather Failed", error?.message.toString())
                resultStatus = Result.failure()
            }

        })

        return resultStatus as Result
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification: NotificationCompat.Builder =
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_notifications_24).setContentTitle(title)
                .setContentText(message).setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notification.setChannelId(CHANNEL_ID)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }
}