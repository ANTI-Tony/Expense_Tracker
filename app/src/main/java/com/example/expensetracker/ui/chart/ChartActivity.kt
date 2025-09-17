package com.example.expensetracker.ui.chart

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

        // Initialize repository
        val database = ExpenseDatabase.getDatabase(applicationContext)
        repository = ExpenseRepository(database.expenseDao())

        initViews()
        setupToolbar()
        loadChartData()
    }

    private fun initViews() {
        categoryContainer = findViewById(R.id.categoryContainer)
        dailyContainer = findViewById(R.id.dailyContainer)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadChartData() {
        lifecycleScope.launch {
            val categoryTotals = viewModel.getCategoryTotals()
            val dailyExpenses = viewModel.getDailyExpensesLast7Days()

            displayCategoryData(categoryTotals)
            displayDailyData(dailyExpenses)
        }
    }

    private fun displayCategoryData(categoryTotals: Map<String, Double>) {
        categoryContainer.removeAllViews()

        if (categoryTotals.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No expense data available"
                textSize = 16f
                setPadding(16, 16, 16, 16)
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
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
    }

    private fun displayDailyData(dailyExpenses: Map<String, Double>) {
        dailyContainer.removeAllViews()

        if (dailyExpenses.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No daily expense data available"
                textSize = 16f
                setPadding(16, 16, 16, 16)
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
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
    }
}