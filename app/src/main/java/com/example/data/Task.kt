package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String, // "کار" (Work), "شخصی" (Personal), "سلامتی" (Health), "تحصیل" (Study)
    val priority: String, // "کم" (Low), "متوسط" (Medium), "زیاد" (High)
    val gregorianDate: String, // "YYYY-MM-DD" style to match Calendar selected day
    val jalaliDate: String, // "1405/03/18" style for Persian displays
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
