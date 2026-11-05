package com.Group19.listhub

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.Group19.listhub.notifications.ReminderScheduler
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.Group19.listhub.data.FirestoreRepository
import com.Group19.listhub.data.PRIORITY_HIGH
import com.Group19.listhub.data.PRIORITY_LOW
import com.Group19.listhub.data.PRIORITY_MEDIUM
import com.Group19.listhub.data.Task
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEditTaskActivity : AppCompatActivity() {

    private val repository = FirestoreRepository()

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnPickDateTime: Button
    private lateinit var spinnerPriority: Spinner
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerRecurrence: Spinner
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button
    private lateinit var tvScreenTitle: TextView

    private var existingTaskId: String? = null
    private var existingTask: Task? = null
    private var selectedDueDate: Calendar? = null

    private var datePickerDialog: DatePickerDialog? = null
    private var timePickerDialog: TimePickerDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_task)

        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        btnPickDateTime = findViewById(R.id.btnPickDateTime)
        spinnerPriority = findViewById(R.id.spinnerPriority)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerRecurrence = findViewById(R.id.spinnerRecurrence)

        val recurrenceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Never", "Daily", "Weekly", "Monthly"))
        recurrenceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRecurrence.adapter = recurrenceAdapter

        requestNotificationPermissionIfNeeded()

        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)
        tvScreenTitle = findViewById(R.id.tvScreenTitle)
        val btnBack: ImageButton = findViewById(R.id.btnBack)

        setupSpinners()

        btnBack.setOnClickListener { finish() }
        btnPickDateTime.setOnClickListener { showDatePicker() }
        btnSave.setOnClickListener { saveTask() }
        btnDelete.setOnClickListener { confirmDelete() }

        existingTaskId = intent.getStringExtra(EXTRA_TASK_ID)

        if (existingTaskId != null) {
            tvScreenTitle.text = getString(R.string.edit_task_title)
            btnDelete.visibility = android.view.View.VISIBLE
            loadExistingTask(existingTaskId!!)
        } else {
            tvScreenTitle.text = getString(R.string.add_task_title)
            etTitle.requestFocus()
        }
    }

    private fun setupSpinners() {
        val priorityAdapter = ArrayAdapter.createFromResource(
            this, R.array.priority_options, android.R.layout.simple_spinner_item
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = priorityAdapter
        spinnerPriority.setSelection(1) // default Medium

        val categoryAdapter = ArrayAdapter.createFromResource(
            this, R.array.category_options, android.R.layout.simple_spinner_item
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter
    }

    private fun loadExistingTask(taskId: String) {
        lifecycleScope.launch {
            val task = repository.getTaskById(taskId)
            if (task == null) {
                Toast.makeText(this@AddEditTaskActivity, "Task not found", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            existingTask = task

            etTitle.setText(task.title)
            etDescription.setText(task.description ?: "")

            spinnerPriority.setSelection(task.priority - 1)

            val categories = resources.getStringArray(R.array.category_options)
            val categoryIndex = categories.indexOf(task.categoryId)
            if (categoryIndex >= 0) spinnerCategory.setSelection(categoryIndex)

            spinnerRecurrence.setSelection(listOf("Never", "Daily", "Weekly", "Monthly").indexOf(task.recurrence).coerceAtLeast(0))

            if (task.dueDate != null) {
                val cal = Calendar.getInstance()
                cal.timeInMillis = task.dueDate!!
                selectedDueDate = cal
                updateDateTimeButtonText()
            }
        }
    }

    private fun showDatePicker() {
        val cal = selectedDueDate ?: Calendar.getInstance()

        datePickerDialog?.dismiss()
        datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                showTimePicker(cal)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog?.show()
    }

    private fun showTimePicker(cal: Calendar) {
        timePickerDialog?.dismiss()
        timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                selectedDueDate = cal
                updateDateTimeButtonText()
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        )
        timePickerDialog?.show()
    }

    override fun onPause() {
        super.onPause()
        datePickerDialog?.dismiss()
        timePickerDialog?.dismiss()
    }

    private fun updateDateTimeButtonText() {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy 'at' HH:mm", Locale.getDefault())
        btnPickDateTime.text = sdf.format(selectedDueDate!!.time)
    }

    private fun saveTask() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (title.length > 100) {
            Toast.makeText(this, "Title must be 100 characters or fewer", Toast.LENGTH_SHORT).show()
            return
        }
        if (description.length > 500) {
            Toast.makeText(this, "Description must be 500 characters or fewer", Toast.LENGTH_SHORT).show()
            return
        }

        val priority = when (spinnerPriority.selectedItemPosition) {
            0 -> PRIORITY_LOW
            2 -> PRIORITY_HIGH
            else -> PRIORITY_MEDIUM
        }
        val category = spinnerCategory.selectedItem?.toString()

        val ownerId = repository.getCurrentUser()?.uid
        if (ownerId == null) {
            Toast.makeText(this, "You've been signed out, please log in again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                if (existingTask != null) {
                    val updated = existingTask!!.copy(
                        title = title,
                        description = description.ifEmpty { null },
                        dueDate = selectedDueDate?.timeInMillis,
                        priority = priority,
                        categoryId = category,
                        recurrence = spinnerRecurrence.selectedItem.toString()
                    )
                    repository.updateTask(updated)
                    ReminderScheduler.cancel(this@AddEditTaskActivity, updated.taskId)
                    updated.dueDate?.takeIf { it > System.currentTimeMillis() }?.let {
                        ReminderScheduler.schedule(this@AddEditTaskActivity, updated.taskId, updated.title, it, updated.recurrence)
                    }
                    Toast.makeText(this@AddEditTaskActivity, "Task updated", Toast.LENGTH_SHORT).show()
                } else {
                    val newTask = Task(
                        ownerId = ownerId,
                        title = title,
                        description = description.ifEmpty { null },
                        dueDate = selectedDueDate?.timeInMillis,
                        priority = priority,
                        categoryId = category,
                        recurrence = spinnerRecurrence.selectedItem.toString()
                    )
                    val newId = repository.insertTask(newTask)
                    newTask.dueDate?.takeIf { it > System.currentTimeMillis() }?.let {
                        ReminderScheduler.schedule(this@AddEditTaskActivity, newId, newTask.title, it, newTask.recurrence)
                    }
                    Toast.makeText(this@AddEditTaskActivity, "Task added", Toast.LENGTH_SHORT).show()
                }
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddEditTaskActivity, "Couldn't save task: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 410)
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete task")
            .setMessage("Are you sure you want to delete this task? This can't be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    existingTask?.let { ReminderScheduler.cancel(this@AddEditTaskActivity, it.taskId); repository.deleteTask(it) }
                    Toast.makeText(this@AddEditTaskActivity, "Task deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
