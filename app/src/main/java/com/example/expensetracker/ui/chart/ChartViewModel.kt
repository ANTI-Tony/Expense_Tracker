package com.example.expensetracker.ui.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChartViewModel(private val repository: ExpenseRepository) : ViewModel() {

    suspend fun getCategoryTotals(): Map<String, Double> {
        return try {
            val categoryTotals = repository.getCategoryTotals()
            categoryTotals.associate { it.category to it.total }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun getDailyExpensesLast7Days(): Map<String, Double> {
        return try {
            val calendar = Calendar.getInstance()
            val endDate = calendar.time
            calendar.add(Calendar.DAY_OF_MONTH, -6)
            val startDate = calendar.time

            // 获取所有费用数据（如果LiveData为空，返回空列表）
            val allExpenses = repository.getAllExpenses().value ?: emptyList()

            // 筛选最近7天的费用
            val recentExpenses = allExpenses.filter { expense ->
                expense.date.after(startDate) && expense.date.before(Date(endDate.time + 24 * 60 * 60 * 1000))
            }

            val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
            val dailyTotals = mutableMapOf<String, Double>()

            // 初始化最近7天的日期，都设为0
            for (i in 0..6) {
                val date = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -6 + i)
                }.time
                dailyTotals[dateFormat.format(date)] = 0.0
            }

            // 累加每天的费用
            recentExpenses.forEach { expense ->
                val dateKey = dateFormat.format(expense.date)
                dailyTotals[dateKey] = (dailyTotals[dateKey] ?: 0.0) + expense.amount
            }

            // 返回按日期排序的结果
            dailyTotals.toSortedMap()
        } catch (e: Exception) {
            // 如果出错，返回示例数据
            val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
            val sampleData = mutableMapOf<String, Double>()
            for (i in 0..6) {
                val date = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -6 + i)
                }.time
                sampleData[dateFormat.format(date)] = 0.0
            }
            sampleData
        }
    }
}