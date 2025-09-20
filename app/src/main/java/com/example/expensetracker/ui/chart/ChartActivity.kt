package com.example.expensetracker.ui.chart

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.expensetracker.R
import com.example.expensetracker.data.database.ExpenseDatabase
import com.example.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ChartActivity : AppCompatActivity() {

    private lateinit var repository: ExpenseRepository
    private val viewModel: ChartViewModel by viewModels {
        ChartViewModelFactory(repository)
    }

    private lateinit var categoryContainer: LinearLayout
    private lateinit var dailyContainer: LinearLayout

    // 数据缓存
    private var categoryData: Map<String, Double>? = null
    private var dailyData: Map<String, Double>? = null
    private var isDataLoaded = false

    companion object {
        private const val TAG = "ChartActivity"

        // 保存状态的键
        private const val KEY_CATEGORY_DATA = "category_data"
        private const val KEY_DAILY_DATA = "daily_data"
        private const val KEY_IS_DATA_LOADED = "is_data_loaded"
        private const val KEY_SCROLL_POSITION = "scroll_position"
    }

    private var scrollPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")
        setContentView(R.layout.activity_chart)

        // Initialize repository
        val database = ExpenseDatabase.getDatabase(applicationContext)
        repository = ExpenseRepository(database.expenseDao())

        initViews()
        setupToolbar()
        restoreInstanceState(savedInstanceState)

        // 只有在没有缓存数据时才加载
        if (!isDataLoaded) {
            loadChartData()
        } else {
            displayCachedData()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart() called")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() called")

        // 恢复滚动位置
        if (scrollPosition > 0) {
            val scrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView)
            scrollView?.post {
                scrollView.scrollTo(0, scrollPosition)
            }
        }

        // 检查数据是否需要刷新（例如从其他Activity返回后）
        refreshDataIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() called")

        // 保存滚动位置
        val scrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView)
        scrollPosition = scrollView?.scrollY ?: 0
        Log.d(TAG, "Saved scroll position: $scrollPosition")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop() called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called")

        if (!isChangingConfigurations) {
            Log.d(TAG, "Activity being destroyed permanently")
            // 清理资源
            categoryData = null
            dailyData = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState() called")

        outState.apply {
            // 保存数据缓存
            categoryData?.let { data ->
                val categoryArray = data.map { "${it.key}|${it.value}" }.toTypedArray()
                putStringArray(KEY_CATEGORY_DATA, categoryArray)
            }

            dailyData?.let { data ->
                val dailyArray = data.map { "${it.key}|${it.value}" }.toTypedArray()
                putStringArray(KEY_DAILY_DATA, dailyArray)
            }

            putBoolean(KEY_IS_DATA_LOADED, isDataLoaded)
            putInt(KEY_SCROLL_POSITION, scrollPosition)
        }

        Log.d(TAG, "Saved chart data and scroll position")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d(TAG, "onRestoreInstanceState() called")
        // 恢复逻辑在 restoreInstanceState 方法中处理
    }

    private fun restoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let { bundle ->
            Log.d(TAG, "Restoring instance state")

            // 恢复分类数据
            bundle.getStringArray(KEY_CATEGORY_DATA)?.let { array ->
                categoryData = array.associate {
                    val parts = it.split("|")
                    parts[0] to parts[1].toDouble()
                }
            }

            // 恢复日常数据
            bundle.getStringArray(KEY_DAILY_DATA)?.let { array ->
                dailyData = array.associate {
                    val parts = it.split("|")
                    parts[0] to parts[1].toDouble()
                }
            }

            isDataLoaded = bundle.getBoolean(KEY_IS_DATA_LOADED, false)
            scrollPosition = bundle.getInt(KEY_SCROLL_POSITION, 0)

            Log.d(TAG, "Restored data - categories: ${categoryData?.size}, daily: ${dailyData?.size}")
        }
    }

    private fun initViews() {
        categoryContainer = findViewById(R.id.categoryContainer)
        dailyContainer = findViewById(R.id.dailyContainer)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            Log.d(TAG, "Navigation back pressed")
            finish()
        }
    }

    private fun loadChartData() {
        Log.d(TAG, "Loading chart data...")
        showLoadingState()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    val categoryTotals = viewModel.getCategoryTotals()
                    val dailyExpenses = viewModel.getDailyExpensesLast7Days()

                    // 缓存数据
                    categoryData = categoryTotals
                    dailyData = dailyExpenses
                    isDataLoaded = true

                    Log.d(TAG, "Data loaded - categories: ${categoryTotals.size}, daily: ${dailyExpenses.size}")

                    displayCategoryData(categoryTotals)
                    displayDailyData(dailyExpenses)
                    showContentState()

                } catch (e: Exception) {
                    Log.e(TAG, "Error loading chart data", e)
                    showErrorState()
                }
            }
        }
    }

    private fun displayCachedData() {
        Log.d(TAG, "Displaying cached data")
        categoryData?.let { displayCategoryData(it) }
        dailyData?.let { displayDailyData(it) }
        showContentState()
    }

    private fun refreshDataIfNeeded() {
        // 检查数据是否需要刷新
        // 可以基于时间戳或其他条件来决定
        Log.d(TAG, "Checking if data refresh is needed")

        // 如果数据超过5分钟，重新加载
        // 这里简化为总是保持现有数据，除非用户主动刷新
    }

    private fun showLoadingState() {
        categoryContainer.removeAllViews()
        dailyContainer.removeAllViews()

        // 创建新的加载视图而不是重用
        val loadingView = createLoadingView()
        categoryContainer.addView(loadingView)
        categoryContainer.visibility = View.VISIBLE
        dailyContainer.visibility = View.GONE
    }

    private fun showErrorState() {
        categoryContainer.removeAllViews()
        dailyContainer.removeAllViews()

        // 创建新的错误视图而不是重用
        val errorView = createErrorView()
        categoryContainer.addView(errorView)
        categoryContainer.visibility = View.VISIBLE
        dailyContainer.visibility = View.GONE
    }

    private fun showContentState() {
        categoryContainer.visibility = View.VISIBLE
        dailyContainer.visibility = View.VISIBLE
    }

    private fun createLoadingView(): View {
        val textView = TextView(this).apply {
            text = "Loading charts..."
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }
        return textView
    }

    private fun createErrorView(): View {
        val textView = TextView(this).apply {
            text = "Error loading chart data. Tap to retry."
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            setOnClickListener {
                loadChartData()
            }
        }
        return textView
    }

    private fun displayCategoryData(categoryTotals: Map<String, Double>) {
        categoryContainer.removeAllViews()

        if (categoryTotals.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No expense data available"
                textSize = 16f
                setPadding(16, 16, 16, 16)
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                gravity = android.view.Gravity.CENTER
            }
            categoryContainer.addView(emptyView)
            return
        }

        val maxAmount = categoryTotals.values.maxOrNull() ?: 0.0
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

        categoryTotals.forEach { (category, amount) ->
            val itemView = layoutInflater.inflate(R.layout.item_chart_bar, categoryContainer, false)

            val tvCategory = itemView.findViewById<TextView>(R.id.tvLabel)
            val tvAmount = itemView.findViewById<TextView>(R.id.tvValue)
            val progressBar = itemView.findViewById<android.widget.ProgressBar>(R.id.progressBar)

            tvCategory.text = category
            tvAmount.text = currencyFormat.format(amount)
            progressBar.max = 100
            progressBar.progress = if (maxAmount > 0) ((amount / maxAmount) * 100).toInt() else 0

            categoryContainer.addView(itemView)
        }

        Log.d(TAG, "Displayed ${categoryTotals.size} category items")
    }

    private fun displayDailyData(dailyExpenses: Map<String, Double>) {
        dailyContainer.removeAllViews()

        if (dailyExpenses.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No daily expense data available"
                textSize = 16f
                setPadding(16, 16, 16, 16)
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                gravity = android.view.Gravity.CENTER
            }
            dailyContainer.addView(emptyView)
            return
        }

        val maxAmount = dailyExpenses.values.maxOrNull() ?: 0.0
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

        dailyExpenses.forEach { (date, amount) ->
            val itemView = layoutInflater.inflate(R.layout.item_chart_bar, dailyContainer, false)

            val tvDate = itemView.findViewById<TextView>(R.id.tvLabel)
            val tvAmount = itemView.findViewById<TextView>(R.id.tvValue)
            val progressBar = itemView.findViewById<android.widget.ProgressBar>(R.id.progressBar)

            tvDate.text = date
            tvAmount.text = currencyFormat.format(amount)
            progressBar.max = 100
            progressBar.progress = if (maxAmount > 0) ((amount / maxAmount) * 100).toInt() else 0

            dailyContainer.addView(itemView)
        }

        Log.d(TAG, "Displayed ${dailyExpenses.size} daily items")
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed() called")
        super.onBackPressed()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        Log.d(TAG, "onUserLeaveHint() called - User is leaving the app")
    }
}