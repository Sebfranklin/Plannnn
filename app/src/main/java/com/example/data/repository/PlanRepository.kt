package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.CalendarContract
import com.example.data.local.CalendarEventDao
import com.example.data.local.GoalDao
import com.example.data.local.TaskDao
import com.example.data.model.CalendarEvent
import com.example.data.model.Goal
import com.example.data.model.Task
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class PlanRepository(
    private val calendarEventDao: CalendarEventDao,
    private val taskDao: TaskDao,
    private val goalDao: GoalDao
) {
    // --- Calendar Events ---
    val allEvents: Flow<List<CalendarEvent>> = calendarEventDao.getAllEvents()

    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<CalendarEvent>> {
        return calendarEventDao.getEventsForDay(startOfDay, endOfDay)
    }

    suspend fun insertEvent(event: CalendarEvent) = calendarEventDao.insertEvent(event)
    suspend fun updateEvent(event: CalendarEvent) = calendarEventDao.updateEvent(event)
    suspend fun deleteEvent(event: CalendarEvent) = calendarEventDao.deleteEvent(event)
    suspend fun deleteEventById(id: Int) = calendarEventDao.deleteEventById(id)

    // --- Tasks ---
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    fun getTasksForDay(dayTimestamp: Long): Flow<List<Task>> {
        return taskDao.getTasksForDay(dayTimestamp)
    }

    suspend fun insertTask(task: Task) = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    suspend fun deleteTaskById(id: Int) = taskDao.deleteTaskById(id)

    // --- Goals ---
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()

    fun getGoalsByCategory(category: String): Flow<List<Goal>> {
        return goalDao.getGoalsByCategory(category)
    }

    suspend fun insertGoal(goal: Goal) = goalDao.insertGoal(goal)
    suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal)
    suspend fun deleteGoal(goal: Goal) = goalDao.deleteGoal(goal)
    suspend fun deleteGoalById(id: Int) = goalDao.deleteGoalById(id)

    // --- Real System Calendar Sync ---
    suspend fun syncWithSystemCalendar(context: Context): Int {
        var syncedCount = 0
        try {
            val contentResolver: ContentResolver = context.contentResolver
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_LOCATION
            )

            // Sync events from 3 days ago until 7 days from now
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -3)
            val startTimeWindow = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 10)
            val endTimeWindow = cal.timeInMillis

            val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?)"
            val selectionArgs = arrayOf(startTimeWindow.toString(), endTimeWindow.toString())

            val cursor: android.database.Cursor? = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            cursor?.use {
                val indexId = it.getColumnIndex(CalendarContract.Events._ID)
                val indexTitle = it.getColumnIndex(CalendarContract.Events.TITLE)
                val indexDesc = it.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val indexStart = it.getColumnIndex(CalendarContract.Events.DTSTART)
                val indexEnd = it.getColumnIndex(CalendarContract.Events.DTEND)
                val indexLocation = it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)

                while (it.moveToNext()) {
                    val title = if (indexTitle != -1) it.getString(indexTitle) ?: "No Title" else "No Title"
                    val desc = if (indexDesc != -1) it.getString(indexDesc) ?: "" else ""
                    val start = if (indexStart != -1) it.getLong(indexStart) else System.currentTimeMillis()
                    val end = if (indexEnd != -1) it.getLong(indexEnd) else System.currentTimeMillis() + 3600000
                    val location = if (indexLocation != -1) it.getString(indexLocation) else null

                    // Prevent importing empty titled system events
                    if (title.isNotEmpty()) {
                        val calendarEvent = CalendarEvent(
                            title = title,
                            description = desc,
                            startTime = start,
                            endTime = end,
                            location = location,
                            color = 0xFF4CAF50.toInt(), // Green for imported system events
                            isSynced = true
                        )
                        calendarEventDao.insertEvent(calendarEvent)
                        syncedCount++
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return syncedCount
    }

    // --- Generate Styled Realistic Demo Events ---
    suspend fun importDemoCalendarEvents() {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val todayMs = today.timeInMillis

        val demoEvents = listOf(
            CalendarEvent(
                title = "🚀 Morning Standup & Alignment",
                description = "Review team progress, blockages, goals for the week",
                startTime = todayMs + 9 * 3600000, // 09:00 AM
                endTime = todayMs + 10 * 3600000, // 10:00 AM
                location = "Zoom",
                color = 0xFF1E88E5.toInt(), // Blue Accent
                isSynced = false
            ),
            CalendarEvent(
                title = "🧠 Deep Work: Feature Design Docs",
                description = "Draft technical specifications for the smart daily scheduler applet, design DB schemas and Room persistence",
                startTime = todayMs + 10 * 3600000 + 30 * 60000, // 10:30 AM
                endTime = todayMs + 12 * 3600000 + 30 * 60000, // 12:30 PM
                location = "Office Suite",
                color = 0xFF5E35B1.toInt(), // Deep Purple
                isSynced = false
            ),
            CalendarEvent(
                title = "🥗 Lunch & Regeneration",
                description = "Walk outside, get high-quality sunlight & nutrients.",
                startTime = todayMs + 13 * 3600000, // 01:00 PM
                endTime = todayMs + 14 * 3600000, // 02:00 PM
                location = "Central Park Food Hall",
                color = 0xFF43A047.toInt(), // Green
                isSynced = false
            ),
            CalendarEvent(
                title = "👥 Client Sync & Showcase",
                description = "Showcase mockups, get vital feedback on product interface, goals dashboard, and reminder triggers.",
                startTime = todayMs + 14 * 3600000 + 30 * 60000, // 02:30 PM
                endTime = todayMs + 15 * 3600000 + 30 * 60000, // 03:30 PM
                location = "Meet Room C",
                color = 0xFFE53935.toInt(), // Red
                isSynced = false
            )
        )

        for (event in demoEvents) {
            calendarEventDao.insertEvent(event)
        }

        // Also pre-populate some demo Tasks and Goals to make the first launch gorgeous!
        val demoTasks = listOf(
            Task(
                title = "Complete core schema design",
                description = "Add Room models for scheduling and goals",
                dueDate = todayMs,
                dueTime = "11:00",
                priority = "High",
                isCompleted = true,
                category = "Work"
            ),
            Task(
                title = "Prepare Client Demo Slides",
                description = "Outline architecture, dynamic AI planning, and accessibility features",
                dueDate = todayMs,
                dueTime = "14:00",
                priority = "High",
                isCompleted = false,
                category = "Work"
            ),
            Task(
                title = "Buy groceries & meal prep",
                description = "Organic chicken breast, avocados, organic berries, sweet potatoes",
                dueDate = todayMs,
                dueTime = "18:00",
                priority = "Medium",
                isCompleted = false,
                category = "Personal"
            ),
            Task(
                title = "Evening 30 min cardio run",
                description = "Zone 2 aerobic run on treadmill or trail",
                dueDate = todayMs,
                dueTime = "19:30",
                priority = "Low",
                isCompleted = false,
                category = "Fitness"
            )
        )

        val demoGoals = listOf(
            Goal(
                title = "Optimal Hydration",
                description = "Drink water regularly to stay sharp and maintain cognitive performance",
                targetValue = 8.0f,
                currentValue = 3.0f,
                unit = "glasses",
                category = "Wellness",
                deadline = todayMs + 5 * 24 * 3600000
            ),
            Goal(
                title = "Continuous Learning",
                description = "Read non-fiction / technical literature daily",
                targetValue = 10.0f,
                currentValue = 4.0f,
                unit = "books",
                category = "Learning",
                deadline = todayMs + 30 * 24 * 3600000
            ),
            Goal(
                title = "Zone 2 Fitness Cardio",
                description = "Complete weekly running milestones",
                targetValue = 4.0f,
                currentValue = 2.0f,
                unit = "sessions",
                category = "Fitness",
                deadline = todayMs + 10 * 24 * 3600000
            )
        )

        for (task in demoTasks) {
            taskDao.insertTask(task)
        }
        for (goal in demoGoals) {
            goalDao.insertGoal(goal)
        }
    }
}
