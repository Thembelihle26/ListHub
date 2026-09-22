package com.example.listhub

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.listhub.adapter.TaskAdapter
import com.example.listhub.data.FirestoreRepository
import com.example.listhub.data.PRIORITY_MEDIUM
import com.example.listhub.data.Task
import com.example.listhub.network.GoogleTasksClient
import com.example.listhub.utils.GoogleAuthHelper
import com.example.listhub.utils.KarmaManager
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.util.Calendar

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

        adapter = TaskAdapter(
            tasks = mutableListOf(),
            onTaskClick = { task -> openEditTask(task.taskId) },
            onCheckChanged = { task, isChecked -> onTaskCheckedChanged(task, isChecked) }
        )
        recyclerTasks.layoutManager = LinearLayoutManager(this)
        recyclerTasks.adapter = adapter

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
            tvEmptyState.visibility = if (tasks.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun onTaskCheckedChanged(task: Task, isChecked: Boolean) {
        val uid = repository.getCurrentUser()?.uid ?: return
        lifecycleScope.launch {
            val wasCompleted = task.isCompleted
            task.isCompleted = isChecked
            repository.updateTask(task)

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
