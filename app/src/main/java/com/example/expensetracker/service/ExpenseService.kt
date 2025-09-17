package com.example.expensetracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.expensetracker.R
import com.example.expensetracker.data.database.Expense
import com.example.expensetracker.data.database.ExpenseDatabase
import com.example.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

class ExpenseService : Service() {

    private lateinit var repository: ExpenseRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    companion object {
        private const val CHANNEL_ID = "ExpenseServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val SIMULATION_INTERVAL = 60000L // 1 minute

        private val SAMPLE_EXPENSES = listOf(
            "Coffee" to "Food",
            "Bus Ticket" to "Transportation",
            "Lunch" to "Food",
            "Movie Ticket" to "Entertainment",
            "Grocery Shopping" to "Shopping",
            "Gas" to "Transportation",
            "Phone Bill" to "Bills"
        )
    }

    override fun onCreate() {
        super.onCreate()

        val database = ExpenseDatabase.getDatabase(applicationContext)
        repository = ExpenseRepository(database.expenseDao())

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        handler = Handler(Looper.getMainLooper())
        setupPeriodicTask()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupPeriodicTask() {
        runnable = object : Runnable {
            override fun run() {
                simulateAutomaticExpense()
                handler.postDelayed(this, SIMULATION_INTERVAL)
            }
        }
        handler.post(runnable)
    }

    private fun simulateAutomaticExpense() {
        serviceScope.launch {
            // Randomly decide whether to add an expense (30% chance)
            if (Random.nextFloat() < 0.3f) {
                val (title, category) = SAMPLE_EXPENSES.random()
                val amount = Random.nextDouble(5.0, 100.0)

                val expense = Expense(
                    title = "$title (Auto)",
                    amount = amount,
                    category = category,
                    date = Date(),
                    description = "Automatically recorded transaction"
                )

                try {
                    repository.insertExpense(expense)
                    updateNotification("Added: $title - ${String.format("%.2f", amount)}")
                } catch (e: Exception) {
                    updateNotification("Error adding automatic expense")
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Expense Tracker Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background service for automatic expense tracking"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Expense Tracker")
            .setContentText("Monitoring for automatic transactions...")
            .setSmallIcon(R.drawable.ic_add)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Expense Tracker")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_add)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
}