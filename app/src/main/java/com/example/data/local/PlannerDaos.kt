package com.example.data.local

import androidx.room.*
import com.example.data.model.CalendarEvent
import com.example.data.model.Goal
import com.example.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events ORDER BY startTime ASC")
    fun getAllEvents(): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM calendar_events WHERE startTime >= :startOfDay AND startTime <= :endOfDay ORDER BY startTime ASC")
    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<CalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEvent)

    @Update
    suspend fun updateEvent(event: CalendarEvent)

    @Delete
    suspend fun deleteEvent(event: CalendarEvent)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteEventById(id: Int)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY priority DESC, dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE dueDate = :dayTimestamp ORDER BY isCompleted ASC, priority DESC")
    fun getTasksForDay(dayTimestamp: Long): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY deadline ASC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE category = :category ORDER BY deadline ASC")
    fun getGoalsByCategory(category: String): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Int)
}
