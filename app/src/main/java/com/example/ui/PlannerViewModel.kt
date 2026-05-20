package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.model.CalendarEvent
import com.example.data.model.Goal
import com.example.data.model.Task
import com.example.data.remote.GeminiPlannerService
import com.example.data.repository.PlanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class PlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = PlanRepository(
        database.calendarEventDao(),
        database.taskDao(),
        database.goalDao()
    )
    private val geminiService = GeminiPlannerService()

    // Current selected date for scheduling (defaults to current day start)
    private val _selectedDateMs = MutableStateFlow<Long>(getStartOfDay(System.currentTimeMillis()))
    val selectedDateMs: StateFlow<Long> = _selectedDateMs.asStateFlow()

    // Reactive lists from the repository
    val allEvents: StateFlow<List<CalendarEvent>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoals: StateFlow<List<Goal>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Events and Tasks sorted/filtered specifically for the currently selected day
    val selectedDayEvents: StateFlow<List<CalendarEvent>> = _selectedDateMs
        .flatMapLatest { dateMs ->
            val range = getStartAndEndOfDay(dateMs)
            repository.getEventsForDay(range.first, range.second)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDayTasks: StateFlow<List<Task>> = _selectedDateMs
        .flatMapLatest { dateMs ->
            repository.getTasksForDay(dateMs)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI dynamic planner execution states
    private val _isGeneratingPlan = MutableStateFlow(false)
    val isGeneratingPlan: StateFlow<Boolean> = _isGeneratingPlan.asStateFlow()

    private val _aiPlanAdvice = MutableStateFlow<String?>(null)
    val aiPlanAdvice: StateFlow<String?> = _aiPlanAdvice.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    val isGeminiKeyConfigured: Boolean
        get() = BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    // --- Actions ---

    fun selectDate(timestamp: Long) {
        _selectedDateMs.value = getStartOfDay(timestamp)
    }

    // --- Task Actions ---
    fun addTask(title: String, description: String, dateMs: Long, time: String?, priority: String, category: String) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                description = description,
                dueDate = getStartOfDay(dateMs),
                dueTime = time,
                priority = priority,
                category = category,
                isCompleted = false
            )
            repository.insertTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            repository.deleteTaskById(taskId)
        }
    }

    // --- Event Actions ---
    fun addEvent(title: String, description: String, dateMs: Long, startTimeMs: Long, endTimeMs: Long, location: String?, color: Int) {
        viewModelScope.launch {
            val event = CalendarEvent(
                title = title,
                description = description,
                startTime = startTimeMs,
                endTime = endTimeMs,
                location = location,
                color = color,
                isSynced = false
            )
            repository.insertEvent(event)
        }
    }

    fun deleteEvent(eventId: Int) {
        viewModelScope.launch {
            repository.deleteEventById(eventId)
        }
    }

    // --- Goal Actions ---
    fun addGoal(title: String, description: String, target: Float, unit: String, category: String, deadlineMs: Long) {
        viewModelScope.launch {
            val goal = Goal(
                title = title,
                description = description,
                targetValue = target,
                currentValue = 0f,
                unit = unit,
                category = category,
                deadline = deadlineMs
            )
            repository.insertGoal(goal)
        }
    }

    fun logGoalProgress(goal: Goal, delta: Float) {
        viewModelScope.launch {
            val newProgress = (goal.currentValue + delta).coerceIn(0f, goal.targetValue)
            repository.updateGoal(goal.copy(currentValue = newProgress))
        }
    }

    fun deleteGoal(goalId: Int) {
        viewModelScope.launch {
            repository.deleteGoalById(goalId)
        }
    }

    // --- Calendar System Sync Action ---
    fun syncCalendar(context: Context) {
        viewModelScope.launch {
            try {
                val count = repository.syncWithSystemCalendar(context)
                _syncMessage.value = "Successfully synchronized $count calendar events!"
            } catch (e: SecurityException) {
                _syncMessage.value = "Calendar Permission Denied"
            } catch (e: Exception) {
                _syncMessage.value = "Sync Error: ${e.localizedMessage}"
            }
        }
    }

    fun loadDemoDataset() {
        viewModelScope.launch {
            repository.importDemoCalendarEvents()
            _syncMessage.value = "Successfully loaded polished demo dataset!"
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // --- Dynamic AI Planner Generation ---
    fun generateDynamicAiPlan() {
        viewModelScope.launch {
            _isGeneratingPlan.value = true
            _aiPlanAdvice.value = null
            
            // Gather items for selected day + all goals
            val events = selectedDayEvents.value
            val tasks = selectedDayTasks.value
            val goals = allGoals.value

            val advice = geminiService.generateDynamicDailyPlan(events, tasks, goals)
            _aiPlanAdvice.value = advice
            _isGeneratingPlan.value = false
        }
    }

    fun resetAiAdvice() {
        _aiPlanAdvice.value = null
    }

    // --- Date Calculations Helpers ---
    private fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartAndEndOfDay(timestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }
}
