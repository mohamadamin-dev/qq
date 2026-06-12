package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Task
import com.example.data.TaskDatabase
import com.example.data.TaskRepository
import com.example.util.JalaliCalendar
import com.example.util.JalaliDate
import com.example.util.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    private val sharedPrefs = application.getSharedPreferences("eli_planner_prefs", Context.MODE_PRIVATE)

    // Daily note for the selected date
    private val _currentDailyNote = MutableStateFlow("")
    val currentDailyNote: StateFlow<String> = _currentDailyNote.asStateFlow()

    init {
        val taskDao = TaskDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)
        NotificationHelper.initNotificationChannel(application)
        _currentDailyNote.value = getDailyNote(LocalDate.now())
    }

    // Selected Date in UI (Gregorian)
    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Current calendar month view (Jalali year & month)
    private val initialJalali = JalaliCalendar.gregorianToJalali(LocalDate.now())
    private val _currentYearJalali = MutableStateFlow(initialJalali.year)
    val currentYearJalali: StateFlow<Int> = _currentYearJalali.asStateFlow()

    private val _currentMonthJalali = MutableStateFlow(initialJalali.month)
    val currentMonthJalali: StateFlow<Int> = _currentMonthJalali.asStateFlow()

    // Active screen navigation/tab in main layout: 0 = "برنامه روزانه", 1 = "گزارش پیشرفت", 2 = "تنظیمات"
    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    // Preferences state flows
    private val _themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(sharedPrefs.getBoolean("notifications_enabled", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _userNickname = MutableStateFlow(sharedPrefs.getString("user_nickname", "کاربر الی") ?: "کاربر الی")
    val userNickname: StateFlow<String> = _userNickname.asStateFlow()

    // Tasks for the selected Gregorian date
    @OptIn(ExperimentalCoroutinesApi::class)
    val tasksForSelectedDate: StateFlow<List<Task>> = _selectedDate
        .flatMapLatest { date ->
            repository.getTasksForDate(date.toString())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Tasks (for progress statistics)
    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weekly statistics: calculates the last 7 days task completion rates
    val weeklyStats: StateFlow<List<DayProgress>> = allTasks.map { taskList ->
        val today = LocalDate.now()
        (0..6).map { offset ->
            val day = today.minusDays(offset.toLong())
            val dateStr = day.toString()
            val dayTasks = taskList.filter { it.gregorianDate == dateStr }
            val completedCount = dayTasks.count { it.isCompleted }
            val totalCount = dayTasks.size
            val ratio = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
            
            val jDate = JalaliCalendar.gregorianToJalali(day)
            val dayName = JalaliCalendar.getPersianWeekdayName(day)
            
            DayProgress(
                dayName = dayName,
                jalaliString = "${jDate.month}/${jDate.day}",
                completedCount = completedCount,
                totalCount = totalCount,
                progressRatio = ratio
            )
        }.reversed() // Ascending order of days (oldest to newest)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category distribution completion rates
    val categoryStats: StateFlow<Map<String, CategoryProgress>> = allTasks.map { taskList ->
        listOf("کار", "شخصی", "سلامتی", "تحصیل").associateWith { cat ->
            val catTasks = taskList.filter { it.category == cat }
            val completed = catTasks.count { it.isCompleted }
            val total = catTasks.size
            val ratio = if (total > 0) completed.toFloat() / total else 0f
            CategoryProgress(category = cat, completed = completed, total = total, ratio = ratio)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun getDailyNote(date: LocalDate): String {
        return sharedPrefs.getString("dailynote_${date}", "") ?: ""
    }

    fun updateDailyNote(date: LocalDate, note: String) {
        _currentDailyNote.value = note
        sharedPrefs.edit().putString("dailynote_${date}", note).apply()
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        // Sync calendar month view with selected date's month
        val jDate = JalaliCalendar.gregorianToJalali(date)
        _currentYearJalali.value = jDate.year
        _currentMonthJalali.value = jDate.month
        // Update current daily note
        _currentDailyNote.value = getDailyNote(date)
    }

    fun setActiveTab(tab: Int) {
        _activeTab.value = tab
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode).apply()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        sharedPrefs.edit().putBoolean("notifications_enabled", enabled).apply()
        
        if (enabled) {
            // Trigger a quick toast notification to confirm activation
            triggerTestNotification()
        }
    }

    fun setUserNickname(name: String) {
        val cleanName = name.trim().ifEmpty { "کاربر الی" }
        _userNickname.value = cleanName
        sharedPrefs.edit().putString("user_nickname", cleanName).apply()
    }

    fun triggerTestNotification() {
        if (_notificationsEnabled.value) {
            NotificationHelper.showNotification(
                getApplication(),
                999,
                "🔔 الی پلنر فعال است!",
                "سلام ${_userNickname.value}، یادآوری کارهای شما با موفقیت فعال شد."
            )
        }
    }

    fun nextMonth() {
        if (_currentMonthJalali.value == 12) {
            _currentMonthJalali.value = 1
            _currentYearJalali.value += 1
        } else {
            _currentMonthJalali.value += 1
        }
    }

    fun prevMonth() {
        if (_currentMonthJalali.value == 1) {
            _currentMonthJalali.value = 12
            _currentYearJalali.value -= 1
        } else {
            _currentMonthJalali.value -= 1
        }
    }

    fun addTask(title: String, description: String, category: String, priority: String, date: LocalDate) {
        viewModelScope.launch {
            val jDate = JalaliCalendar.gregorianToJalali(date)
            val newTask = Task(
                title = title.trim(),
                description = description.trim(),
                category = category,
                priority = priority,
                gregorianDate = date.toString(),
                jalaliDate = jDate.toString(),
                isCompleted = false
            )
            repository.insert(newTask)

            // Trigger notification if enabled
            if (_notificationsEnabled.value) {
                NotificationHelper.showNotification(
                    getApplication(),
                    System.currentTimeMillis().toInt(),
                    "📌 کار جدید ثبت شد",
                    "فعالیت \"${title}\" برای تاریخ ${jDate.month}/${jDate.day} برنامه ریزی شد."
                )
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            repository.update(updatedTask)

            // Trigger supportive notification if newly completed and notifications enabled
            if (updatedTask.isCompleted && _notificationsEnabled.value) {
                NotificationHelper.showNotification(
                    getApplication(),
                    task.id.toInt(),
                    "👏 آفرین، یک کار انجام شد!",
                    "شما کار \"${task.title}\" را به موقع به پایان رساندید."
                )
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }
}

data class DayProgress(
    val dayName: String,
    val jalaliString: String,
    val completedCount: Int,
    val totalCount: Int,
    val progressRatio: Float
)

data class CategoryProgress(
    val category: String,
    val completed: Int,
    val total: Int,
    val ratio: Float
)
