package com.example.expensetracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.expensetracker.R
import com.example.expensetracker.data.database.Expense
import com.example.expensetracker.data.database.ExpenseDatabase
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

class ExpenseService : Service() {

    private lateinit var repository: ExpenseRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var transactionCount = 0

    companion object {
        private const val TAG = "ExpenseService"
        private const val CHANNEL_ID = "ExpenseServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val SIMULATION_INTERVAL = 30000L // 30 seconds for demo

        private val SAMPLE_EXPENSES = listOf(
            "Morning Coffee" to "Food" to (3.50 to 5.99),
            "Bus Ticket" to "Transportation" to (2.50 to 4.00),
            "Lunch" to "Food" to (8.99 to 15.50),
            "Movie Ticket" to "Entertainment" to (12.99 to 18.99),
            "Grocery Shopping" to "Shopping" to (25.00 to 85.00),
            "Gas Station" to "Transportation" to (35.00 to 65.00),
            "Phone Bill" to "Bills" to (45.00 to 75.00),
            "Parking Fee" to "Transportation" to (2.00 to 8.00),
            "Snack" to "Food" to (1.50 to 4.99),
            "Uber Ride" to "Transportation" to (8.50 to 25.00),
            "Streaming Service" to "Entertainment" to (9.99 to 14.99),
            "Pharmacy" to "Healthcare" to (12.50 to 35.00)
        )
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate()")

        val database = ExpenseDatabase.getDatabase(applicationContext)
        repository = ExpenseRepository(database.expenseDao())

        createNotificationChannel()

        // 检查权限并启动前台服务
        if (hasRequiredPermissions()) {
            try {
                startForeground(NOTIFICATION_ID, createNotification())
                Log.d(TAG, "Foreground service started successfully")
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to start foreground service - permission denied", e)
                stopSelf()
                return
            }
        } else {
            Log.w(TAG, "Missing required permissions for foreground service")
            stopSelf()
            return
        }

        handler = Handler(Looper.getMainLooper())
        setupPeriodicTask()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand()")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasRequiredPermissions(): Boolean {
        val foregroundServicePermission = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.FOREGROUND_SERVICE
        ) == PackageManager.PERMISSION_GRANTED

        // 检查Android 14+的数据同步权限
        val dataSyncPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 14以下不需要这个权限
        }

        Log.d(TAG, "Permissions - Foreground: $foregroundServicePermission, DataSync: $dataSyncPermission")
        return foregroundServicePermission && dataSyncPermission
    }

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
            try {
                // Higher probability for demo purposes (50% chance)
                if (Random.nextFloat() < 0.5f) {
                    val selectedExpense = SAMPLE_EXPENSES.random()
                    val (title, category) = selectedExpense.first
                    val (minAmount, maxAmount) = selectedExpense.second
                    val amount = Random.nextDouble(minAmount, maxAmount)

                    // Add some variance to the date (within last 24 hours)
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.HOUR_OF_DAY, -Random.nextInt(24))

                    val expense = Expense(
                        title = title,
                        amount = amount,
                        category = category,
                        date = calendar.time,
                        description = "Automatically detected transaction #${++transactionCount}"
                    )

                    repository.insertExpense(expense)
                    val formattedAmount = String.format("%.2f", amount)
                    updateNotification("New expense detected: $title - $$formattedAmount")

                    Log.d(TAG, "Auto expense added: $title - $$formattedAmount")

                    // Show a more detailed notification occasionally
                    if (transactionCount % 5 == 0) {
                        showDetailedNotification("$transactionCount transactions processed automatically!")
                    }

                } else {
                    updateNotification("Monitoring for transactions... ($transactionCount processed)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in automatic expense simulation", e)
                updateNotification("Error processing automatic expense")
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
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Expense Tracker")
            .setContentText("Monitoring for automatic transactions...")
            .setSmallIcon(R.drawable.ic_add)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(message: String) {
        // 检查是否有通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "No notification permission, skipping notification update")
                return
            }
        }

        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Expense Tracker")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_add)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to update notification - permission denied", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification", e)
        }
    }

    private fun showDetailedNotification(message: String) {
        // 检查通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        try {
            val detailedNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Auto-Expense Update")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_add)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(Random.nextInt(), detailedNotification)

        } catch (e: Exception) {
            Log.e(TAG, "Error showing detailed notification", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy()")

        try {
            handler.removeCallbacks(runnable)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing handler callbacks", e)
        }

        try {
            serviceScope.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling service scope", e)
        }
    }
}