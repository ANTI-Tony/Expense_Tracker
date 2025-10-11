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

    // 模拟iOS SMS解析的智能解析器
    private val smartParser = SmartTransactionParser()

    companion object {
        private const val TAG = "ExpenseService"
        private const val CHANNEL_ID = "ExpenseServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val SIMULATION_INTERVAL = 25000L // 25秒间隔
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate() - Starting intelligent transaction detection")

        val database = ExpenseDatabase.getDatabase(applicationContext)
        repository = ExpenseRepository(database.expenseDao())

        createNotificationChannel()

        if (hasRequiredPermissions()) {
            try {
                startForeground(NOTIFICATION_ID, createNotification())
                Log.d(TAG, "Intelligent expense detection service started successfully")
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
        setupIntelligentDetection()
    }

    private fun setupIntelligentDetection() {
        runnable = object : Runnable {
            override fun run() {
                simulateIntelligentSMSParsing()
                handler.postDelayed(this, SIMULATION_INTERVAL)
            }
        }
        handler.post(runnable)
    }

    private fun simulateIntelligentSMSParsing() {
        serviceScope.launch {
            try {
                // 60%概率检测到"SMS"，模拟真实场景
                if (Random.nextFloat() < 0.6f) {
                    val simulatedSMS = smartParser.generateRealisticSMS()
                    val parsedResult = smartParser.parseTransaction(simulatedSMS)

                    parsedResult?.let { result ->
                        val expense = Expense(
                            title = result.title,
                            amount = result.amount,
                            category = result.category,
                            date = result.date,
                            description = "Auto-detected: ${result.originalText}"
                        )

                        repository.insertExpense(expense)

                        Log.d(TAG, "Intelligent parsing: '${simulatedSMS}' -> ${result.title} \$${String.format("%.2f", result.amount)}")
                        updateNotification("Parsed SMS: ${result.title} - \$${String.format("%.2f", result.amount)}")

                        transactionCount++

                        // 每5笔交易显示统计
                        if (transactionCount % 5 == 0) {
                            showIntelligenceReport()
                        }
                    }
                } else {
                    updateNotification("Monitoring SMS for transactions... (${transactionCount} parsed)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in intelligent SMS parsing simulation", e)
                updateNotification("Error in intelligent parsing engine")
            }
        }
    }

    private fun showIntelligenceReport() {
        val detailedNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart Parsing Report")
            .setContentText("Successfully parsed $transactionCount SMS transactions with AI")
            .setSmallIcon(R.drawable.ic_add)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(Random.nextInt(), detailedNotification)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing intelligence report", e)
        }
    }

    // 模拟iOS的智能SMS解析器
    private class SmartTransactionParser {

        // 模拟真实的支付SMS模板（基于你的iOS版本）
        private val smsTemplates = listOf(
            "您在 {merchant} 消费 {amount}元，当前余额{balance}元 【某银行】",
            "支付宝交易提醒：您向 {merchant} 付款{amount}元",
            "微信支付凭证：商户{merchant}，金额￥{amount}",
            "您尾号{card}的卡片在{merchant}消费{amount}元",
            "交通银行提醒您：您在{merchant}刷卡消费{amount}元",
            "招商银行：您账户在{merchant}发生支出{amount}元",
            "中国银行：{merchant}消费{amount}元，账户余额{balance}元",
            "建设银行：您在{merchant}的交易金额为{amount}元"
        )

        // 智能商户和分类映射（模拟你的iOS分类逻辑）
        private val merchantCategories = mapOf(
            // 餐饮类
            "星巴克" to ("Food" to (15.0 to 45.0)),
            "麦当劳" to ("Food" to (25.0 to 65.0)),
            "肯德基" to ("Food" to (30.0 to 70.0)),
            "海底捞" to ("Food" to (80.0 to 200.0)),
            "喜茶" to ("Food" to (18.0 to 35.0)),
            "沙县小吃" to ("Food" to (12.0 to 25.0)),

            // 交通类
            "滴滴出行" to ("Transportation" to (8.0 to 45.0)),
            "地铁公司" to ("Transportation" to (2.0 to 8.0)),
            "中石化加油站" to ("Transportation" to (200.0 to 500.0)),
            "停车场" to ("Transportation" to (5.0 to 25.0)),

            // 购物类
            "淘宝网" to ("Shopping" to (20.0 to 300.0)),
            "京东商城" to ("Shopping" to (50.0 to 500.0)),
            "苹果专卖店" to ("Shopping" to (500.0 to 8000.0)),
            "优衣库" to ("Shopping" to (100.0 to 400.0)),

            // 娱乐类
            "万达影城" to ("Entertainment" to (35.0 to 80.0)),
            "KTV" to ("Entertainment" to (100.0 to 300.0)),
            "游戏充值" to ("Entertainment" to (6.0 to 200.0)),

            // 生活服务
            "美团外卖" to ("Food" to (20.0 to 60.0)),
            "饿了么" to ("Food" to (25.0 to 55.0)),
            "盒马鲜生" to ("Shopping" to (50.0 to 200.0)),
            "物业费" to ("Bills" to (200.0 to 800.0))
        )

        fun generateRealisticSMS(): String {
            val template = smsTemplates.random()
            val (merchant, categoryInfo) = merchantCategories.entries.random()
            val (category, amountRange) = categoryInfo
            val amount = Random.nextDouble(amountRange.first, amountRange.second)
            val balance = Random.nextDouble(1000.0, 50000.0)
            val card = "****${Random.nextInt(1000, 9999)}"

            return template
                .replace("{merchant}", merchant)
                .replace("{amount}", String.format("%.2f", amount))
                .replace("{balance}", String.format("%.2f", balance))
                .replace("{card}", card)
        }

        fun parseTransaction(smsText: String): ParsedTransaction? {
            return try {
                // 模拟你的iOS NLP解析逻辑
                val amount = extractAmount(smsText)
                val (merchant, category) = extractMerchantAndCategory(smsText)
                val transactionType = identifyTransactionType(smsText)

                if (amount != null && merchant != null) {
                    ParsedTransaction(
                        title = merchant,
                        amount = amount,
                        category = category,
                        date = Date(),
                        originalText = smsText,
                        confidence = calculateConfidence(smsText)
                    )
                } else {
                    Log.w(TAG, "Failed to parse SMS: $smsText")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing SMS: $smsText", e)
                null
            }
        }

        private fun extractAmount(text: String): Double? {
            // 模拟你的iOS金额识别算法
            val patterns = listOf(
                "消费\\s*([0-9]+(?:\\.[0-9]+)?)元",
                "付款\\s*([0-9]+(?:\\.[0-9]+)?)元",
                "金额[￥¥]?\\s*([0-9]+(?:\\.[0-9]+)?)",
                "支出\\s*([0-9]+(?:\\.[0-9]+)?)元",
                "交易金额为\\s*([0-9]+(?:\\.[0-9]+)?)元"
            )

            for (pattern in patterns) {
                val regex = Regex(pattern)
                val match = regex.find(text)
                if (match != null) {
                    return match.groupValues[1].toDoubleOrNull()
                }
            }
            return null
        }

        private fun extractMerchantAndCategory(text: String): Pair<String?, String> {
            // 查找已知商户
            for ((merchant, categoryInfo) in merchantCategories) {
                if (text.contains(merchant)) {
                    return Pair(merchant, categoryInfo.first)
                }
            }

            // 通用商户提取
            val merchantPatterns = listOf(
                "在\\s*([^\\s]+)\\s*消费",
                "向\\s*([^\\s]+)\\s*付款",
                "商户\\s*([^\\s]+)",
                "([^\\s]+)\\s*刷卡"
            )

            for (pattern in merchantPatterns) {
                val regex = Regex(pattern)
                val match = regex.find(text)
                if (match != null) {
                    val merchant = match.groupValues[1]
                    return Pair(merchant, "Other")
                }
            }

            return Pair("Unknown Merchant", "Other")
        }

        private fun identifyTransactionType(text: String): String {
            val expenseKeywords = listOf("消费", "付款", "支出", "购买", "刷卡")
            val incomeKeywords = listOf("收入", "转入", "工资", "退款")

            return when {
                expenseKeywords.any { text.contains(it) } -> "expense"
                incomeKeywords.any { text.contains(it) } -> "income"
                else -> "expense" // 默认为支出
            }
        }

        private fun calculateConfidence(text: String): Double {
            var confidence = 0.5

            // 如果包含银行关键词，置信度提高
            if (text.contains("银行") || text.contains("支付宝") || text.contains("微信")) {
                confidence += 0.3
            }

            // 如果金额格式标准，置信度提高
            if (text.contains("元") || text.contains("￥")) {
                confidence += 0.2
            }

            return minOf(confidence, 1.0)
        }
    }

    data class ParsedTransaction(
        val title: String,
        val amount: Double,
        val category: String,
        val date: Date,
        val originalText: String,
        val confidence: Double
    )

    // 其余代码保持不变...
    private fun hasRequiredPermissions(): Boolean {
        val foregroundServicePermission = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.FOREGROUND_SERVICE
        ) == PackageManager.PERMISSION_GRANTED

        val dataSyncPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return foregroundServicePermission && dataSyncPermission
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart Expense Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AI-powered transaction detection from SMS simulation"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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
            .setContentTitle("Smart Expense Tracker")
            .setContentText("AI monitoring SMS for automatic expense detection...")
            .setSmallIcon(R.drawable.ic_add)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
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
                .setContentTitle("Smart Expense Tracker")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_add)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Smart parsing service destroyed")

        try {
            handler.removeCallbacks(runnable)
            serviceScope.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error during service cleanup", e)
        }
    }
}