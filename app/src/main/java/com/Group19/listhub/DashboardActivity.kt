package com.Group19.listhub

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.Group19.listhub.adapter.TaskAdapter
import com.Group19.listhub.data.FirestoreRepository
import com.Group19.listhub.data.PRIORITY_MEDIUM
import com.Group19.listhub.data.Task
import com.Group19.listhub.network.GoogleTasksClient
import com.Group19.listhub.utils.GoogleAuthHelper
import com.Group19.listhub.utils.KarmaManager
import com.Group19.listhub.notifications.ReminderScheduler
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private val repository = FirestoreRepository()
    private lateinit var adapter: TaskAdapter

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerTasks: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var tvKarma: TextView
    private lateinit var tvGreeting: TextView

    private var currentTabPosition = 0 // 0 = All, 1 = Today, 2 = Upcoming

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        if (!repository.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        drawerLayout = findViewById(R.id.drawerLayout)
        recyclerTasks = findViewById(R.id.recyclerTasks)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvKarma = findViewById(R.id.tvKarma)
        tvGreeting = findViewById(R.id.tvGreeting)

        val btnMenu: ImageButton = findViewById(R.id.btnMenu)
        val navigationView: NavigationView = findViewById(R.id.navigationView)
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        val fabAddTask: Button = findViewById(R.id.fabAddTask)
        val btnSync: Button = findViewById(R.id.btnSync)
        val etQuickAdd: EditText = findViewById(R.id.etQuickAdd)
        val btnQuickAdd: Button = findViewById(R.id.btnQuickAdd)
        val btnFocusMode: Button = findViewById(R.id.btnFocusMode)
        btnFocusMode.setOnClickListener { startActivity(Intent(this, FocusActivity::class.java)) }
        findViewById<Button>(R.id.btnShareList).setOnClickListener { shareTaskList() }
        btnQuickAdd.setOnClickListener { createQuickTask(etQuickAdd) }

        adapter = TaskAdapter(
            tasks = mutableListOf(),
            onTaskClick = { task -> openEditTask(task.taskId) },
            onCheckChanged = { task, isChecked -> onTaskCheckedChanged(task, isChecked) }
        )
        recyclerTasks.layoutManager = LinearLayoutManager(this)
        recyclerTasks.adapter = adapter
        val dragCallback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                adapter.moveTask(vh.bindingAdapterPosition, target.bindingAdapterPosition); return true
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) = Unit
        }
        ItemTouchHelper(dragCallback).attachToRecyclerView(recyclerTasks)

        btnMenu.setOnClickListener { drawerLayout.openDrawer(Gravity.START) }

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_logout -> {
                    repository.logout()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTabPosition = tab.position
                loadTasks()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        fabAddTask.setOnClickListener {
            startActivity(Intent(this, AddEditTaskActivity::class.java))
        }

        btnSync.setOnClickListener { syncFromGoogleTasksApi() }
    }

    override fun onResume() {
        super.onResume()
        loadUserHeader()
        loadTasks()
    }

    private fun loadUserHeader() {
        val uid = repository.getCurrentUser()?.uid ?: return
        lifecycleScope.launch {
            val profile = repository.getUserProfile(uid) ?: return@launch
            val level = KarmaManager.calculateLevel(profile.karmaPoints)
            tvKarma.text = "Lvl $level · ${profile.karmaPoints}"
            tvGreeting.text = "Hi, ${profile.fullName.split(" ").firstOrNull() ?: profile.fullName} \uD83D\uDC4B"
        }
    }

    private fun loadTasks() {
        val uid = repository.getCurrentUser()?.uid ?: return
        lifecycleScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startOfDay = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = calendar.timeInMillis - 1

            val tasks: List<Task> = try {
                when (currentTabPosition) {
                    1 -> repository.getTodayTasks(uid, startOfDay, endOfDay)
                    2 -> repository.getUpcomingTasks(uid, endOfDay)
                    else -> repository.getAllTasks(uid)
                }
            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "Couldn't load tasks: ${e.message}", Toast.LENGTH_LONG).show()
                emptyList()
            }

            adapter.updateTasks(tasks)
            val completed = tasks.count { it.isCompleted }
            val pending = tasks.count { !it.isCompleted && (it.dueDate == null || it.dueDate!! >= System.currentTimeMillis()) }
            val overdue = tasks.count { !it.isCompleted && it.dueDate != null && it.dueDate!! < System.currentTimeMillis() }
            findViewById<TextView>(R.id.tvProductivity).text = "Productivity · $completed of ${tasks.size} completed (${if (tasks.isEmpty()) 0 else completed * 100 / tasks.size}%)"
            findViewById<ProductivityChartView>(R.id.productivityChart).setStats(completed, pending, overdue)
            tvEmptyState.visibility = if (tasks.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun onTaskCheckedChanged(task: Task, isChecked: Boolean) {
        val uid = repository.getCurrentUser()?.uid ?: return
        lifecycleScope.launch {
            val wasCompleted = task.isCompleted
            task.isCompleted = isChecked
            repository.updateTask(task)
            if (isChecked) ReminderScheduler.cancel(this@DashboardActivity, task.taskId)
            else task.dueDate?.takeIf { it > System.currentTimeMillis() }?.let {
                ReminderScheduler.schedule(this@DashboardActivity, task.taskId, task.title, it, task.recurrence)
            }

            if (isChecked && !wasCompleted) {
                val profile = repository.getUserProfile(uid)
                if (profile != null) {
                    val newPoints = profile.karmaPoints + KarmaManager.pointsForCompletingTask()
                    val newLevel = KarmaManager.calculateLevel(newPoints)
                    val leveledUp = newLevel > profile.level
                    profile.karmaPoints = newPoints
                    profile.level = newLevel
                    repository.updateUserProfile(profile)

                    if (leveledUp) {
                        Toast.makeText(this@DashboardActivity, "Level up! You're now level $newLevel \uD83C\uDF89", Toast.LENGTH_SHORT).show()
                    }
                    loadUserHeader()
                }
            }
            loadTasks()
        }
    }

    private fun shareTaskList() {
        val lines = adapter.currentTasks().joinToString("
") { task ->
            "${if (task.isCompleted) "[x]" else "[ ]"} ${task.title}" + (task.dueDate?.let { " — ${java.text.SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(it))}" } ?: "")
        }
        val text = if (lines.isBlank()) "My List Hub task list is empty." else "My List Hub tasks
$lines"
        val share = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
        startActivity(Intent.createChooser(share, "Share task list"))
    }

    private fun createQuickTask(input: EditText) {
        val raw = input.text.toString().trim()
        if (raw.isBlank()) { input.error = "Enter a task"; return }
        var due: Calendar? = null
        var title = raw
        val lower = raw.lowercase(Locale.getDefault())
        val day = when { lower.contains("tomorrow") -> "tomorrow"; lower.contains("today") -> "today"; else -> null }
        val timeMatch = Regex("(?i)\\b(1[0-2]|0?[1-9])(?::([0-5][0-9]))?\\s*(am|pm)\\b").find(raw)
        if (day != null || timeMatch != null) {
            due = Calendar.getInstance()
            if (day == "tomorrow") due.add(Calendar.DAY_OF_MONTH, 1)
            timeMatch?.let { m ->
                var hour = m.groupValues[1].toInt(); val minute = m.groupValues[2].ifBlank { "0" }.toInt()
                if (m.groupValues[3].equals("pm", true) && hour < 12) hour += 12
                if (m.groupValues[3].equals("am", true) && hour == 12) hour = 0
                due!!.set(Calendar.HOUR_OF_DAY, hour); due!!.set(Calendar.MINUTE, minute)
            }
            title = title.replace(Regex("(?i)\\btomorrow\\b|\\btoday\\b"), "")
                .replace(Regex("(?i)\\b(1[0-2]|0?[1-9])(?::[0-5][0-9])?\\s*(am|pm)\\b"), "")
                .replace(Regex("\\s+"), " ").trim()
        }
        if (title.isBlank()) { input.error = "Enter a task title"; return }
        val uid = repository.getCurrentUser()?.uid ?: return
        lifecycleScope.launch {
            try {
                repository.insertTask(Task(ownerId = uid, title = title, dueDate = due?.timeInMillis, priority = PRIORITY_MEDIUM, categoryId = "Personal"))
                input.text.clear(); Toast.makeText(this@DashboardActivity, "Task added", Toast.LENGTH_SHORT).show(); loadTasks()
            } catch (e: Exception) { Toast.makeText(this@DashboardActivity, "Couldn't add task: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }

    private fun openEditTask(taskId: String) {
        val intent = Intent(this, AddEditTaskActivity::class.java)
        intent.putExtra(AddEditTaskActivity.EXTRA_TASK_ID, taskId)
        startActivity(intent)
    }

    /**
     * Pulls the signed-in Google account's real task lists from the Google Tasks
     * API and imports the first list's tasks into our own Firestore "tasks"
     * collection. Needs the user to have signed in with Google (not just email/
     * password) since we need an OAuth token with the tasks scope.
     */
    private fun syncFromGoogleTasksApi() {
        val account = GoogleAuthHelper.getLastSignedInAccount(this)
        if (account == null) {
            Toast.makeText(
                this,
                "Sign in with Google on the Login screen first to sync your real Google Tasks",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val uid = repository.getCurrentUser()?.uid ?: return

        Toast.makeText(this, "Connecting to Google Tasks API...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val accessToken = GoogleAuthHelper.getTasksApiAccessToken(this@DashboardActivity, account)
                val headers = GoogleTasksClient.bearerHeader(accessToken)

                val listsResponse = GoogleTasksClient.api.getTaskLists(headers)
                val firstList = listsResponse.items?.firstOrNull()
                if (firstList == null) {
                    Toast.makeText(this@DashboardActivity, "No Google Task lists found on this account", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val tasksResponse = GoogleTasksClient.api.getTasks(firstList.id, headers)
                val googleTasks = tasksResponse.items ?: emptyList()

                var imported = 0
                for (gTask in googleTasks) {
                    val task = Task(
                        ownerId = uid,
                        title = gTask.title,
                        description = "Imported from Google Tasks list \"${firstList.title}\"",
                        priority = PRIORITY_MEDIUM,
                        categoryId = "Work",
                        isCompleted = gTask.status == "completed"
                    )
                    repository.insertTask(task)
                    imported++
                }

                Toast.makeText(this@DashboardActivity, "Imported $imported tasks from Google Tasks", Toast.LENGTH_SHORT).show()
                loadTasks()
            } catch (e: Exception) {
                // most likely causes for a student demo: Tasks API not enabled on the
                // Cloud project, or the OAuth consent screen isn't published/testing
                // account isn't added yet - see README troubleshooting section
                Toast.makeText(this@DashboardActivity, "Google Tasks sync failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
