package com.example.expensetracker.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val fullFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    fun formatDate(date: Date): String = dateFormatter.format(date)

    fun formatTime(date: Date): String = timeFormatter.format(date)

    fun formatFullDate(date: Date): String = fullFormatter.format(date)

    fun getCurrentDate(): Date = Date()
}