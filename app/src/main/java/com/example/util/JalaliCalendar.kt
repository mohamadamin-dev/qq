package com.example.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class JalaliDate(val year: Int, val month: Int, val day: Int) {
    override fun toString(): String = "$year/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}"
    
    fun formatPersian(): String {
        return "$day ${JalaliCalendar.getMonthName(month)} $year"
    }
}

object JalaliCalendar {
    val MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val WEEKDAY_NAMES_SHORT = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    
    val WEEKDAY_NAMES_FULL = listOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
    )

    // Reference epoch: 1400/01/01 (Farvardin 1, 1400 Jalali) corresponds to 2021-03-21 Gregorian
    private val EPOCH_GREGORIAN = LocalDate.of(2021, 3, 21)
    private const val EPOCH_JALALI_YEAR = 1400

    fun getMonthName(month: Int): String {
        return MONTH_NAMES.getOrNull(month - 1) ?: ""
    }

    fun gregorianToJalali(localDate: LocalDate): JalaliDate {
        val daysDiff = ChronoUnit.DAYS.between(EPOCH_GREGORIAN, localDate)
        return addDaysToJalali(EPOCH_JALALI_YEAR, 1, 1, daysDiff)
    }

    fun jalaliToGregorian(jDate: JalaliDate): LocalDate {
        val daysDiff = getDaysBetweenJalali(EPOCH_JALALI_YEAR, 1, 1, jDate.year, jDate.month, jDate.day)
        return EPOCH_GREGORIAN.plusDays(daysDiff)
    }

    private fun getDaysBetweenJalali(y1: Int, m1: Int, d1: Int, y2: Int, m2: Int, d2: Int): Long {
        return getDaysSinceJalaliEpoch(y2, m2, d2) - getDaysSinceJalaliEpoch(y1, m1, d1)
    }

    private fun getDaysSinceJalaliEpoch(year: Int, month: Int, day: Int): Long {
        var totalDays: Long = 0
        for (y in 1300 until year) {
            totalDays += if (isJalaliLeap(y)) 366 else 365
        }
        for (m in 1 until month) {
            totalDays += getJalaliDaysInMonth(year, m)
        }
        totalDays += (day - 1)
        return totalDays
    }

    private fun addDaysToJalali(startYear: Int, startMonth: Int, startDay: Int, daysToAdd: Long): JalaliDate {
        var remainingDays = daysToAdd
        var year = startYear
        var month = startMonth
        var day = startDay

        if (remainingDays >= 0) {
            // First tick off remaining days of current month
            val daysInCurrentMonth = getJalaliDaysInMonth(year, month) - day + 1
            if (remainingDays < daysInCurrentMonth) {
                day += remainingDays.toInt()
                return JalaliDate(year, month, day)
            }
            remainingDays -= daysInCurrentMonth
            day = 1
            month++
            if (month > 12) {
                month = 1
                year++
            }

            // Year-by-year jump
            while (true) {
                val daysInY = if (isJalaliLeap(year)) 366 else 365
                if (remainingDays < daysInY) break
                remainingDays -= daysInY
                year++
            }

            // Month-by-month jump
            while (true) {
                val daysInM = getJalaliDaysInMonth(year, month)
                if (remainingDays < daysInM) break
                remainingDays -= daysInM
                month++
                if (month > 12) {
                    month = 1
                    year++
                }
            }

            day += remainingDays.toInt()
        } else {
            // Negative adjustment for past dates relative to reference date
            remainingDays = -remainingDays
            val daysFromStart = day - 1
            if (remainingDays <= daysFromStart) {
                day -= remainingDays.toInt()
                return JalaliDate(year, month, day)
            }
            remainingDays -= (daysFromStart + 1)
            day = 1
            month--
            if (month < 1) {
                month = 12
                year--
            }

            // Year-by-year back
            while (true) {
                val daysInY = if (isJalaliLeap(year)) 366 else 365
                if (remainingDays < daysInY) break
                remainingDays -= daysInY
                year--
            }

            // Month-by-month back
            while (true) {
                val daysInM = getJalaliDaysInMonth(year, month)
                if (remainingDays < daysInM) break
                remainingDays -= daysInM
                month--
                if (month < 1) {
                    month = 12
                    year--
                }
            }

            val totalDaysPrevM = getJalaliDaysInMonth(year, month)
            day = totalDaysPrevM - remainingDays.toInt()
        }

        return JalaliDate(year, month, day)
    }

    fun isJalaliLeap(year: Int): Boolean {
        val r = (year - 474) % 2820
        val leapRegistry = ((r + 38) * 31) % 128
        return leapRegistry < 31
    }

    fun getJalaliDaysInMonth(year: Int, month: Int): Int {
        return when {
            month <= 6 -> 31
            month <= 11 -> 30
            month == 12 -> if (isJalaliLeap(year)) 30 else 29
            else -> 30
        }
    }

    fun getMonthDaysGrid(year: Int, month: Int): List<LocalDate?> {
        val firstDayOfMonthJalali = JalaliDate(year, month, 1)
        val firstDayGregorian = jalaliToGregorian(firstDayOfMonthJalali)
        
        // DayOfWeek values: 1 = Monday, 7 = Sunday
        // We map Sunday=1, Monday=2 ... Saturday=0 in Persian Week offset
        val gDayOfWeek = firstDayGregorian.dayOfWeek.value
        val pDayOfWeekOffset = when (gDayOfWeek) {
            6 -> 0 // Saturday (شنه)
            7 -> 1 // Sunday (یکشنبه)
            1 -> 2 // Monday (دوشنبه)
            2 -> 3 // Tuesday (سه شنبه)
            3 -> 4 // Wednesday (چهارشنبه)
            4 -> 5 // Thursday (پنجشنبه)
            5 -> 6 // Friday (جمعه)
            else -> 0
        }

        val totalDays = getJalaliDaysInMonth(year, month)
        val grid = mutableListOf<LocalDate?>()

        // Empty cells for alignment
        for (i in 0 until pDayOfWeekOffset) {
            grid.add(null)
        }

        // Add correct LocalDates corresponding to actual Jalali days
        for (day in 1..totalDays) {
            grid.add(jalaliToGregorian(JalaliDate(year, month, day)))
        }

        // Pad grid to have multiples of 7
        while (grid.size % 7 != 0) {
            grid.add(null)
        }

        return grid
    }

    fun getPersianWeekdayName(localDate: LocalDate): String {
        val gDayOfWeek = localDate.dayOfWeek.value
        val index = when (gDayOfWeek) {
            6 -> 0
            7 -> 1
            1 -> 2
            2 -> 3
            3 -> 4
            4 -> 5
            5 -> 6
            else -> 0
        }
        return WEEKDAY_NAMES_FULL[index]
    }
}
