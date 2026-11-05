package com.Group19.listhub.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Group19.listhub.R
import com.Group19.listhub.data.PRIORITY_HIGH
import com.Group19.listhub.data.PRIORITY_LOW
import com.Group19.listhub.data.Task
import java.text.SimpleDateFormat
import java.util.Locale

class TaskAdapter(
    private var tasks: MutableList<Task>,
    private val onTaskClick: (Task) -> Unit,
    private val onCheckChanged: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkbox: CheckBox = itemView.findViewById(R.id.checkboxDone)
        val title: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val category: TextView = itemView.findViewById(R.id.tvCategory)
        val priority: TextView = itemView.findViewById(R.id.tvPriority)
        val dueDate: TextView = itemView.findViewById(R.id.tvDueDate)
        val categoryDot: View = itemView.findViewById(R.id.viewCategoryDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.title.text = task.title
        holder.category.text = task.categoryId ?: "General"

        // priority colour + label
        val priorityColor = when (task.priority) {
            PRIORITY_LOW -> Color.parseColor("#33CFB0")
            PRIORITY_HIGH -> Color.parseColor("#FF6F5E")
            else -> Color.parseColor("#FFB648")
        }
        holder.priority.text = when (task.priority) {
            PRIORITY_LOW -> "Low"
            PRIORITY_HIGH -> "High"
            else -> "Medium"
        }
        tintDrawable(holder.priority, priorityColor)

        // category dot colour - just cycling through our palette based on category name hash
        val dotColor = categoryColor(task.categoryId)
        tintDrawable(holder.categoryDot, dotColor)

        if (task.dueDate != null) {
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            holder.dueDate.text = sdf.format(task.dueDate)
            holder.dueDate.visibility = View.VISIBLE
        } else {
            holder.dueDate.visibility = View.GONE
        }

        // remove listener before setting checked state to avoid triggering it while binding
        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.isChecked = task.isCompleted
        if (task.isCompleted) {
            holder.title.paintFlags = holder.title.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            holder.itemView.alpha = 0.55f
        } else {
            holder.title.paintFlags = holder.title.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.itemView.alpha = 1.0f
        }

        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            onCheckChanged(task, isChecked)
        }

        holder.itemView.setOnClickListener { onTaskClick(task) }
    }

    override fun getItemCount(): Int = tasks.size

    fun moveTask(from: Int, to: Int) {
        if (from !in tasks.indices || to !in tasks.indices) return
        val moved = tasks.removeAt(from)
        tasks.add(to, moved)
        notifyItemMoved(from, to)
    }

    fun currentTasks(): List<Task> = tasks.toList()

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks.toMutableList()
        notifyDataSetChanged() // yes, notifyDataSetChanged is not "best practice" with DiffUtil,
        // but this keeps things simple and readable for the demo
    }

    private fun tintDrawable(view: View, color: Int) {
        val drawable = view.background.mutate()
        if (drawable is GradientDrawable) {
            drawable.setColor(color)
        }
    }

    private fun categoryColor(category: String?): Int {
        return when (category) {
            "Work" -> Color.parseColor("#5B4CFF")
            "Health" -> Color.parseColor("#FF6F5E")
            "Home" -> Color.parseColor("#33CFB0")
            "Finance" -> Color.parseColor("#FFB648")
            "Personal" -> Color.parseColor("#FFC3D6")
            else -> Color.parseColor("#5B4CFF")
        }
    }
}
