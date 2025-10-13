package com.example.expensetracker.ui.chart

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.expensetracker.data.repository.ExpenseRepository
import java.text.SimpleDateFormat
import java.util.*

class ChartViewModel(private val repository: ExpenseRepository) : ViewModel() {

    companion object {
        private const val TAG = "ChartViewModel"
    }

    suspend fun getCategoryTotals(): Map<String, Double> {
        return try {
            val categoryTotals = repository.getCategoryTotals()
            Log.d(TAG, "Category totals loaded: ${categoryTotals.size} categories")
            categoryTotals.forEach {
                Log.d(TAG, "Category: ${it.category}, Total: ${it.total}")
            }
            categoryTotals.associate { it.category to it.total }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading category totals", e)
            emptyMap()
        }
    }

    suspend fun getDailyExpensesLast7Days(): Map<String, Double> {
        return try {
            // 🔥 修复：设置正确的日期范围
            val calendar = Calendar.getInstance()

            // 结束日期：今天 23:59:59
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endDate = calendar.time

            // 开始日期：6天前 00:00:00
            calendar.add(Calendar.DAY_OF_MONTH, -6)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startDate = calendar.time

            Log.d(TAG, "Date range - Start: $startDate, End: $endDate")

            // 🔥 修复：使用同步方法获取所有费用
            val allExpenses = repository.getAllExpensesSync()
            Log.d(TAG, "Total expenses in database: ${allExpenses.size}")

            // 打印所有费用的日期（调试用）
            allExpenses.forEach { expense ->
                Log.d(TAG, "Expense: ${expense.title}, Date: ${expense.date}, Amount: ${expense.amount}")
            }

            // 🔥 修复：使用正确的日期筛选逻辑
            val recentExpenses = allExpenses.filter { expense ->
                !expense.date.before(startDate) && !expense.date.after(endDate)
            }

            Log.d(TAG, "Expenses in last 7 days: ${recentExpenses.size}")

            val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
            val dailyTotals = mutableMapOf<String, Double>()

            // 🔥 修复：初始化最近7天的日期（都设为0）
            for (i in 0..6) {
                val date = Calendar.getInstance().apply {
                    // 从6天前开始
                    add(Calendar.DAY_OF_MONTH, -6 + i)
                }.time
                val dateKey = dateFormat.format(date)
                dailyTotals[dateKey] = 0.0
                Log.d(TAG, "Initialized date: $dateKey = 0.0")
            }

            // 🔥 修复：累加每天的费用
            recentExpenses.forEach { expense ->
                val dateKey = dateFormat.format(expense.date)
                val currentTotal = dailyTotals[dateKey] ?: 0.0
                val newTotal = currentTotal + expense.amount
                dailyTotals[dateKey] = newTotal
                Log.d(TAG, "Adding to $dateKey: ${expense.amount} (new total: $newTotal)")
            }

            // 打印最终结果
            dailyTotals.forEach { (date, amount) ->
                Log.d(TAG, "Final daily total - $date: $$amount")
            }

            // 返回按日期排序的结果
            dailyTotals.toSortedMap()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading daily expenses", e)
            // 返回空的7天数据（而不是完全空的 map）
            val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
            val emptyData = mutableMapOf<String, Double>()
            for (i in 0..6) {
                val date = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -6 + i)
                }.time
                emptyData[dateFormat.format(date)] = 0.0
            }
            emptyData
        }
    }
}