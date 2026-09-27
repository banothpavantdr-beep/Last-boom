package com.jarvis.assistant

import android.Manifest
import android.app.Application
import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale

class JarvisApplication : Application()

class JarvisDeviceAdminReceiver : DeviceAdminReceiver()

class JarvisOverlayService : Service() {

    override fun onCreate() {
        super.onCreate()

        val channelId = "jarvis_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "JARVIS Assistant",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            manager.createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("JARVIS")
            .setContentText("JARVIS is active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(1001, notification)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

class MainActivity : ComponentActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    private var speechRecognizer: SpeechRecognizer? = null

    private var status by mutableStateOf("JARVIS IS READY")

    private val microphonePermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                startListening()
            } else {
                status = "Microphone permission required"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        devicePolicyManager =
            getSystemService(Context.DEVICE_POLICY_SERVICE)
                    as DevicePolicyManager

        adminComponent =
            ComponentName(
                this,
                JarvisDeviceAdminReceiver::class.java
            )

        if (SpeechRecognizer.isRecognitionAvailable(this)) {

            speechRecognizer =
                SpeechRecognizer.createSpeechRecognizer(this)

            speechRecognizer?.setRecognitionListener(
                object : RecognitionListener {

                    override fun onReadyForSpeech(params: Bundle?) {
                        status = "LISTENING..."
                    }

                    override fun onBeginningOfSpeech() {
                        status = "LISTENING..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        status = "PROCESSING..."
                    }

                    override fun onError(error: Int) {
                        status = "NO SPEECH DETECTED"
                    }

                    override fun onResults(results: Bundle?) {

                        val text =
                            results?.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                            )?.firstOrNull()
                                ?.lowercase(Locale.getDefault())
                                ?: ""

                        if (isLockCommand(text)) {
                            lockScreen()
                        } else {
                            status = "COMMAND NOT RECOGNIZED"
                        }
                    }

                    override fun onPartialResults(
                        partialResults: Bundle?
                    ) {}

                    override fun onEvent(
                        eventType: Int,
                        params: Bundle?
                    ) {}
                }
            )
        }

        setContent {

            MaterialTheme {

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        Text(
                            text = "JARVIS",
                            style = MaterialTheme.typography.headlineLarge
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = "Your Personal Android Assistant"
                        )

                        Spacer(
                            modifier = Modifier.height(30.dp)
                        )

                        Text(
                            text = status
                        )

                        Spacer(
                            modifier = Modifier.height(30.dp)
                        )

                        Button(
                            onClick = {
                                enableScreenLock()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ENABLE SCREEN LOCK")
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Button(
                            onClick = {
                                lockScreen()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("LOCK SCREEN NOW")
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Button(
                            onClick = {
                                startListening()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ACTIVATE JARVIS")
                        }
                    }
                }
            }
        }
    }

    private fun enableScreenLock() {

        val intent =
            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)

        intent.putExtra(
            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
            adminComponent
        )

        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "JARVIS needs permission to lock your screen."
        )

        startActivity(intent)
    }

    private fun lockScreen() {

        if (!devicePolicyManager.isAdminActive(adminComponent)) {

            status = "ENABLE SCREEN LOCK FIRST"

            enableScreenLock()

            return
        }

        try {

            devicePolicyManager.lockNow()

            status = "LOCKED"

        } catch (e: SecurityException) {

            status = "LOCK PERMISSION ERROR"
        }
    }

    private fun startListening() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            microphonePermission.launch(
                Manifest.permission.RECORD_AUDIO
            )

            return
        }

        if (speechRecognizer == null) {

            status = "SPEECH NOT AVAILABLE"

            return
        }

        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault()
                )

                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    3
                )
            }

        speechRecognizer?.startListening(intent)
    }

    private fun isLockCommand(text: String): Boolean {

        return text.contains("screen off") ||
                text.contains("screen lock") ||
                text.contains("lock my phone") ||
                text.contains("phone lock") ||
                text.contains("screen off chey") ||
                text.contains("phone lock chey") ||
                text.contains("screen ni off chey") ||
                text.contains("lock chey")
    }

    override fun onDestroy() {

        speechRecognizer?.destroy()
        speechRecognizer = null

        super.onDestroy()
    }
}
