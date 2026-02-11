package com.study.aiapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {
    private val SPEECH_CODE = 101
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ye layout file res/layout/activity_main.xml se judi hai
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val btnVoice = findViewById<Button>(R.id.btnVoice)

        btnVoice.setOnClickListener {
            startVoiceRecognition()
        }
    }

    // Voice recognition window open karne ke liye
    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Boliye: 'Start Study 30' ya 'Open WhatsApp'")
        
        try {
            startActivityForResult(intent, SPEECH_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice support aapke device par nahi hai", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == SPEECH_CODE && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val command = result?.get(0)?.lowercase(Locale.ROOT) ?: ""

            // Agar command mein 'study' word hai
            if (command.contains("study")) {
                val mins = command.filter { it.isDigit() }.toLongOrNull() ?: 60L
                startStudy(mins)
            } 
            // Agar command mein 'open' word hai
            else if (command.contains("open")) {
                val appToOpen = command.replace("open", "").trim()
                openApp(appToOpen)
            } else {
                Toast.makeText(this, "Command samajh nahi aaya: $command", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // App search karke open karne ka logic
    private fun openApp(name: String) {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (app in apps) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label.contains(name)) {
                val intent = pm.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    startActivity(intent)
                    return
                }
            }
        }
        Toast.makeText(this, "App '$name' nahi mila", Toast.LENGTH_SHORT).show()
    }

    // Study Timer aur Blocker ON karne ka logic
    private fun startStudy(mins: Long) {
        val prefs = getSharedPreferences("StudyPrefs", Context.MODE_PRIVATE)
        // Blocker ko signal dena ki ab apps block karne hain
        prefs.edit().putBoolean("isActive", true).apply()

        Toast.makeText(this, "Study Mode ON: $mins minutes ke liye", Toast.LENGTH_LONG).show()

        object : CountDownTimer(mins * 60000, 1000) {
            override fun onTick(ms: Long) {
                val minutesLeft = ms / 60000
                val secondsLeft = (ms / 1000) % 60
                statusText.text = String.format("Padhai Mode ON: %02d:%02d", minutesLeft, secondsLeft)
            }

            override fun onFinish() {
                // Timer khatam hote hi Blocker OFF kar dena
                prefs.edit().putBoolean("isActive", false).apply()
                statusText.text = "Time Up! Apps Unlocked."
                Toast.makeText(this@MainActivity, "Ab aap apps use kar sakte hain", Toast.LENGTH_LONG).show()
            }
        }.start()
    }
}
