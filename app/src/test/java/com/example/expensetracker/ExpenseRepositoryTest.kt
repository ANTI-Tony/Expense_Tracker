package com.example.expensetracker

import com.example.expensetracker.data.database.Expense
import com.example.expensetracker.utils.DateUtils
import org.junit.Test
import org.junit.Assert.*
import java.util.Date
import java.util.Calendar
import kotlin.random.Random

/**
 * 费用追踪器的全面单元测试
 * 测试数据验证、计算逻辑、日期处理等核心功能
 */
class ExpenseTrackerTest {

    @Test
    fun testExpenseValidation() {
        // 测试有效费用
        val validExpense = Expense(
            title = "Test Expense",
            amount = 50.0,
            category = "Food",
            date = Date()
        )

        assertTrue("Valid expense should pass validation", isValidExpense(validExpense))

        // 测试无效费用 - 空标题
        val invalidExpenseEmptyTitle = validExpense.copy(title = "")
        assertFalse("Empty title should fail validation", isValidExpense(invalidExpenseEmptyTitle))

        val invalidExpenseWhitespaceTitle = validExpense.copy(title = "   ")
        assertFalse("Whitespace-only title should fail validation", isValidExpense(invalidExpenseWhitespaceTitle))

        // 测试无效费用 - 负金额
        val invalidExpenseNegativeAmount = validExpense.copy(amount = -10.0)
        assertFalse("Negative amount should fail validation", isValidExpense(invalidExpenseNegativeAmount))

        // 测试无效费用 - 零金额
        val invalidExpenseZeroAmount = validExpense.copy(amount = 0.0)
        assertFalse("Zero amount should fail validation", isValidExpense(invalidExpenseZeroAmount))

        // 测试边界值
        val minimumValidExpense = validExpense.copy(amount = 0.01)
        assertTrue("Minimum valid amount should pass", isValidExpense(minimumValidExpense))
    }

    @Test
    fun testExpenseCalculations() {
        val expenses = listOf(
            Expense(1, "Expense 1", 25.50, "Food", Date()),
            Expense(2, "Expense 2", 15.75, "Transportation", Date()),
            Expense(3, "Expense 3", 100.00, "Shopping", Date())
        )

        val total = calculateTotal(expenses)
        assertEquals("Total should be 141.25", 141.25, total, 0.01)

        // 测试空列表
        val emptyTotal = calculateTotal(emptyList())
        assertEquals("Empty list total should be 0", 0.0, emptyTotal, 0.01)

        // 测试单个费用
        val singleExpense = listOf(Expense(1, "Single", 42.99, "Test", Date()))
        val singleTotal = calculateTotal(singleExpense)
        assertEquals("Single expense total", 42.99, singleTotal, 0.01)
    }

    @Test
    fun testCategoryGrouping() {
        val expenses = listOf(
            Expense(1, "Coffee", 4.50, "Food", Date()),
            Expense(2, "Lunch", 12.99, "Food", Date()),
            Expense(3, "Bus", 2.50, "Transportation", Date()),
            Expense(4, "Dinner", 18.75, "Food", Date()),
            Expense(5, "Taxi", 15.00, "Transportation", Date())
        )

        val categoryTotals = groupByCategory(expenses)

        assertEquals("Food category total", 36.24, categoryTotals["Food"] ?: 0.0, 0.01)
        assertEquals("Transportation category total", 17.50, categoryTotals["Transportation"] ?: 0.0, 0.01)
        assertEquals("Should have 2 categories", 2, categoryTotals.size)
    }

    @Test
    fun testDateFiltering() {
        val calendar = Calendar.getInstance()
        val today = calendar.time

        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val yesterday = calendar.time

        calendar.add(Calendar.DAY_OF_MONTH, -1) // 2 days ago
        val twoDaysAgo = calendar.time

        val expenses = listOf(
            Expense(1, "Today", 10.0, "Food", today),
            Expense(2, "Yesterday", 20.0, "Food", yesterday),
            Expense(3, "Two days ago", 30.0, "Food", twoDaysAgo)
        )

        // 测试最近2天的费用
        val recentExpenses = filterExpensesByDays(expenses, 2)
        assertEquals("Should have 2 recent expenses", 2, recentExpenses.size)

        // 测试所有费用
        val allExpenses = filterExpensesByDays(expenses, 7)
        assertEquals("Should have all 3 expenses", 3, allExpenses.size)
    }

    @Test
    fun testDateUtils() {
        val testDate = Date()

        // 测试日期格式化
        val formattedDate = DateUtils.formatDate(testDate)
        assertNotNull("Formatted date should not be null", formattedDate)
        assertTrue("Formatted date should not be empty", formattedDate.isNotEmpty())

        // 测试时间格式化
        val formattedTime = DateUtils.formatTime(testDate)
        assertNotNull("Formatted time should not be null", formattedTime)
        assertTrue("Formatted time should contain :", formattedTime.contains(":"))
    }

    @Test
    fun testExpenseStatistics() {
        val expenses = listOf(
            Expense(1, "Small", 5.0, "Food", Date()),
            Expense(2, "Medium", 25.0, "Transportation", Date()),
            Expense(3, "Large", 100.0, "Shopping", Date()),
            Expense(4, "Medium", 30.0, "Food", Date())
        )

        val stats = calculateStatistics(expenses)

        assertEquals("Average should be 40.0", 40.0, stats.average, 0.01)
        assertEquals("Maximum should be 100.0", 100.0, stats.maximum, 0.01)
        assertEquals("Minimum should be 5.0", 5.0, stats.minimum, 0.01)
        assertEquals("Count should be 4", 4, stats.count)
        assertEquals("Total should be 160.0", 160.0, stats.total, 0.01)
    }

    @Test
    fun testDataIntegrity() {
        // 测试费用ID的唯一性
        val expenses = mutableListOf<Expense>()
        val usedIds = mutableSetOf<Long>()

        repeat(100) {
            val expense = createRandomExpense()
            assertFalse("Expense ID should be unique", usedIds.contains(expense.id))
            usedIds.add(expense.id)
            expenses.add(expense)
        }

        assertEquals("Should have 100 unique expenses", 100, expenses.size)
    }

    // 辅助方法
    private fun isValidExpense(expense: Expense): Boolean {
        return expense.title.isNotBlank() &&
                expense.amount > 0 &&
                expense.category.isNotBlank()
    }

    private fun calculateTotal(expenses: List<Expense>): Double {
        return expenses.sumOf { it.amount }
    }

    private fun groupByCategory(expenses: List<Expense>): Map<String, Double> {
        return expenses.groupBy { it.category }
            .mapValues { (_, expenseList) ->
                expenseList.sumOf { it.amount }
            }
    }

    private fun filterExpensesByDays(expenses: List<Expense>, days: Int): List<Expense> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -days)
        val cutoffDate = calendar.time

        return expenses.filter { expense ->
            expense.date.after(cutoffDate)
        }
    }

    private fun calculateStatistics(expenses: List<Expense>): ExpenseStatistics {
        if (expenses.isEmpty()) {
            return ExpenseStatistics(0.0, 0.0, 0.0, 0, 0.0)
        }

        val amounts = expenses.map { it.amount }
        return ExpenseStatistics(
            average = amounts.average(),
            maximum = amounts.maxOrNull() ?: 0.0,
            minimum = amounts.minOrNull() ?: 0.0,
            count = expenses.size,
            total = amounts.sum()
        )
    }

    private fun createRandomExpense(): Expense {
        val categories = listOf("Food", "Transportation", "Shopping", "Entertainment", "Bills")
        return Expense(
            id = Random.nextLong(1, 1000000),
            title = "Random Expense ${Random.nextInt(1000)}",
            amount = Random.nextDouble(1.0, 100.0),
            category = categories.random(),
            date = Date()
        )
    }

    data class ExpenseStatistics(
        val average: Double,
        val maximum: Double,
        val minimum: Double,
        val count: Int,
        val total: Double
    )
}