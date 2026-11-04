package com.example.listhub

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.listhub.data.FirestoreRepository
import kotlinx.coroutines.launch
import java.util.Locale

class FocusActivity : AppCompatActivity() {
    private val repo = FirestoreRepository()
    private var timer: CountDownTimer? = null
    private var remainingMs = 25L * 60 * 1000
    private var sessionType = "focus"
    private var focusMinutes = 25
    private var shortMinutes = 5
    private var longMinutes = 15
    private var completedFocus = 0
    private var focusElapsedMs = 0L
    private lateinit var clock: TextView
    private lateinit var status: TextView
    private lateinit var start: Button
    private lateinit var taskInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus)
        clock = findViewById(R.id.tvTimer)
        status = findViewById(R.id.tvFocusStatus)
        start = findViewById(R.id.btnStartTimer)
        taskInput = findViewById(R.id.etFocusTask)
        findViewById<Button>(R.id.btnShortBreak).setOnClickListener { chooseMode("short") }
        findViewById<Button>(R.id.btnLongBreak).setOnClickListener { chooseMode("long") }
        findViewById<Button>(R.id.btnFocusLength).setOnClickListener { customise() }
        findViewById<Button>(R.id.btnBackFocus).setOnClickListener { finish() }
        start.setOnClickListener { if (timer == null) startTimer() else pauseTimer() }
        val prefs = getSharedPreferences("focus_settings", MODE_PRIVATE)
        focusMinutes = prefs.getInt("focus", 25); shortMinutes = prefs.getInt("short", 5); longMinutes = prefs.getInt("long", 15)
        remainingMs = focusMinutes * 60_000L
        updateClock()
    }

    private fun chooseMode(mode: String) {
        pauseTimer()
        start.text = "Start"
        sessionType = mode
        if (mode == "focus") focusElapsedMs = 0L
        remainingMs = (when (mode) { "short" -> shortMinutes; "long" -> longMinutes; else -> focusMinutes } * 60_000L)
        status.text = when (mode) { "short" -> "Short break"; "long" -> "Long break"; else -> "Focus session" }
        updateClock()
    }

    private fun customise() {
        val fields = arrayOf(EditText(this), EditText(this), EditText(this))
        fields[0].hint = "Focus minutes (default 25)"; fields[0].setText(focusMinutes.toString())
        fields[1].hint = "Short break (default 5)"; fields[1].setText(shortMinutes.toString())
        fields[2].hint = "Long break (15–30)"; fields[2].setText(longMinutes.toString())
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 8, 40, 8); fields.forEach { addView(it) } }
        android.app.AlertDialog.Builder(this).setTitle("Customise intervals").setView(container)
            .setPositiveButton("Save") { _, _ ->
                focusMinutes = fields[0].text.toString().toIntOrNull()?.coerceIn(1, 180) ?: 25
                shortMinutes = fields[1].text.toString().toIntOrNull()?.coerceIn(1, 30) ?: 5
                longMinutes = fields[2].text.toString().toIntOrNull()?.coerceIn(15, 30) ?: 15
                getSharedPreferences("focus_settings", MODE_PRIVATE).edit().putInt("focus", focusMinutes).putInt("short", shortMinutes).putInt("long", longMinutes).apply()
                chooseMode("focus")
            }.setNegativeButton("Cancel", null).show()
    }

    private fun startTimer() {
        start.text = "Pause"
        timer = object : CountDownTimer(remainingMs, 1000) {
            override fun onTick(millis: Long) { remainingMs = millis; if (sessionType == "focus") focusElapsedMs += 1000L; updateClock() }
            override fun onFinish() {
                if (sessionType == "focus") focusElapsedMs += 1000L
                remainingMs = 0; timer = null; updateClock(); start.text = "Start"
                if (sessionType == "focus") saveCompletedSession()
                Toast.makeText(this@FocusActivity, "${if (sessionType == "focus") "Focus session" else "Break"} completed", Toast.LENGTH_LONG).show()
                if (sessionType == "focus") { completedFocus++; chooseMode(if (completedFocus % 4 == 0) "long" else "short") } else chooseMode("focus")
            }
        }.start()
    }

    private fun pauseTimer() { timer?.cancel(); timer = null; start.text = "Resume" }
    private fun updateClock() { val seconds = remainingMs / 1000; clock.text = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60) }

    private fun saveCompletedSession() {
        val uid = repo.getCurrentUser()?.uid ?: return
        val elapsed = (focusElapsedMs / 1000).coerceAtLeast(0)
        val taskName = taskInput.text.toString().trim()
        lifecycleScope.launch { try { repo.recordFocusSession(uid, taskName, elapsed, focusMinutes) } catch (_: Exception) { } }
        focusElapsedMs = 0L
    }

    override fun onDestroy() { timer?.cancel(); super.onDestroy() }
}
