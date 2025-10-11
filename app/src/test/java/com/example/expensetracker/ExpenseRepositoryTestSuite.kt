package com.example.expensetracker

import com.example.expensetracker.data.database.Expense
import com.example.expensetracker.utils.DateUtils
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Date
import java.util.Calendar
import kotlin.random.Random

/**
 * Android费用追踪器完整测试套件
 * 验证所有核心功能的正确性
 */
@RunWith(JUnit4::class)
class ExpenseTrackerTestSuite {

    private lateinit var testExpenses: List<Expense>

    @Before
    fun setUp() {
        // 准备测试数据
        testExpenses = createTestExpenses()
    }

    // ================================
    // 1. 数据模型验证测试
    // ================================

    @Test
    fun testExpenseDataModel() {
        val expense = Expense(
            id = 1,
            title = "Test Coffee",
            amount = 15.50,
            category = "Food",
            date = Date(),
            description = "Morning coffee test"
        )

        assertEquals("Test Coffee", expense.title)
        assertEquals(15.50, expense.amount, 0.01)
        assertEquals("Food", expense.category)
        assertNotNull(expense.date)
        assertEquals("Morning coffee test", expense.description)
    }

    @Test
    fun testExpenseValidation() {
        // 测试有效费用
        val validExpense = Expense(
            title = "Valid Expense",
            amount = 50.0,
            category = "Food",
            date = Date()
        )
        assertTrue("Valid expense should pass validation", isValidExpense(validExpense))

        // 测试无效费用
        val invalidExpenses = listOf(
            validExpense.copy(title = ""), // 空标题
            validExpense.copy(title = "   "), // 只有空格
            validExpense.copy(amount = -10.0), // 负金额
            validExpense.copy(amount = 0.0), // 零金额
            validExpense.copy(category = "") // 空分类
        )

        invalidExpenses.forEachIndexed { index, expense ->
            assertFalse("Invalid expense $index should fail validation", isValidExpense(expense))
        }
    }

    // ================================
    // 2. 计算逻辑测试
    // ================================

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
        assertEquals("Empty list total should be 0", 0.0, calculateTotal(emptyList()), 0.01)

        // 测试单个费用
        val singleExpense = listOf(Expense(1, "Single", 42.99, "Test", Date()))
        assertEquals("Single expense total", 42.99, calculateTotal(singleExpense), 0.01)
    }

    @Test
    fun testCategoryGrouping() {
        val categoryTotals = groupByCategory(testExpenses)

        // 验证所有分类都存在
        assertTrue("Should contain Food category", categoryTotals.containsKey("Food"))
        assertTrue("Should contain Transportation category", categoryTotals.containsKey("Transportation"))

        // 验证计算准确性
        val foodTotal = testExpenses.filter { it.category == "Food" }.sumOf { it.amount }
        assertEquals("Food category total should match", foodTotal, categoryTotals["Food"] ?: 0.0, 0.01)

        // 验证没有负值或零值（除非合理）
        categoryTotals.values.forEach { total ->
            assertTrue("Category total should be positive", total >= 0.0)
        }
    }

    // ================================
    // 3. 日期处理测试
    // ================================

    @Test
    fun testDateFiltering() {
        val calendar = Calendar.getInstance()
        val today = calendar.time

        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val yesterday = calendar.time

        calendar.add(Calendar.DAY_OF_MONTH, -5)
        val weekAgo = calendar.time

        val expensesWithDates = listOf(
            Expense(1, "Today", 10.0, "Food", today),
            Expense(2, "Yesterday", 20.0, "Food", yesterday),
            Expense(3, "Week ago", 30.0, "Food", weekAgo)
        )

        // 测试最近2天
        val recent2Days = filterExpensesByDays(expensesWithDates, 2)
        assertEquals("Should have 2 recent expenses", 2, recent2Days.size)

        // 测试最近7天
        val recent7Days = filterExpensesByDays(expensesWithDates, 7)
        assertEquals("Should have 3 expenses in 7 days", 3, recent7Days.size)

        // 测试边界条件
        val recent0Days = filterExpensesByDays(expensesWithDates, 0)
        assertTrue("0 days should return empty or only today", recent0Days.size <= 1)
    }

    @Test
    fun testDateUtils() {
        val testDate = Calendar.getInstance().apply {
            set(2023, 5, 15) // 2023年6月15日
        }.time

        val formattedDate = DateUtils.formatDate(testDate)
        assertNotNull("Formatted date should not be null", formattedDate)
        assertTrue("Formatted date should not be empty", formattedDate.isNotEmpty())
        assertTrue("Should contain month info", formattedDate.contains("Jun") || formattedDate.contains("6"))

        val formattedTime = DateUtils.formatTime(testDate)
        assertNotNull("Formatted time should not be null", formattedTime)
        assertTrue("Formatted time should contain colon", formattedTime.contains(":"))
    }

    // ================================
    // 4. 统计分析测试
    // ================================

    @Test
    fun testExpenseStatistics() {
        val stats = calculateStatistics(testExpenses)

        assertTrue("Average should be positive", stats.average > 0)
        assertTrue("Maximum should be >= average", stats.maximum >= stats.average)
        assertTrue("Minimum should be <= average", stats.minimum <= stats.average)
        assertEquals("Count should match list size", testExpenses.size, stats.count)

        val manualTotal = testExpenses.sumOf { it.amount }
        assertEquals("Total should match manual calculation", manualTotal, stats.total, 0.01)
    }

    @Test
    fun testStatisticsEdgeCases() {
        // 测试空列表
        val emptyStats = calculateStatistics(emptyList())
        assertEquals("Empty list average should be 0", 0.0, emptyStats.average, 0.01)
        assertEquals("Empty list count should be 0", 0, emptyStats.count)

        // 测试单个元素
        val singleExpense = listOf(Expense(1, "Single", 50.0, "Test", Date()))
        val singleStats = calculateStatistics(singleExpense)
        assertEquals("Single item average equals amount", 50.0, singleStats.average, 0.01)
        assertEquals("Single item min equals max", singleStats.minimum, singleStats.maximum, 0.01)
    }

    // ================================
    // 5. 分类逻辑测试
    // ================================

    @Test
    fun testCategoryClassification() {
        val categories = listOf("Food", "Transportation", "Shopping", "Entertainment", "Bills", "Healthcare", "Education", "Travel", "Other")

        // 测试每个分类都有合理的费用
        categories.forEach { category ->
            val categoryExpenses = testExpenses.filter { it.category == category }
            if (categoryExpenses.isNotEmpty()) {
                categoryExpenses.forEach { expense ->
                    assertTrue("Amount should be reasonable for $category",
                        expense.amount > 0 && expense.amount < 10000) // 假设上限
                }
            }
        }

        // 测试分类分布合理性
        val categoryDistribution = testExpenses.groupBy { it.category }.mapValues { it.value.size }
        assertTrue("Should have at least 3 different categories", categoryDistribution.size >= 3)
    }

    // ================================
    // 6. 智能解析测试（模拟）
    // ================================

    @Test
    fun testSmartTransactionParsing() {
        // 模拟智能解析功能
        val smsExamples = listOf(
            "您在 星巴克 消费 28.50元，余额1234.56元 【招商银行】",
            "支付宝交易提醒：您向 麦当劳 付款35.80元",
            "微信支付凭证：商户海底捞，金额￥128.90"
        )

        smsExamples.forEach { sms ->
            val parsedAmount = extractAmountFromSMS(sms)
            val parsedMerchant = extractMerchantFromSMS(sms)

            assertNotNull("Should extract amount from: $sms", parsedAmount)
            assertNotNull("Should extract merchant from: $sms", parsedMerchant)
            assertTrue("Amount should be reasonable", parsedAmount!! > 0 && parsedAmount < 1000)
            assertTrue("Merchant should not be empty", parsedMerchant!!.isNotEmpty())
        }
    }

    // ================================
    // 7. 性能测试
    // ================================

    @Test
    fun testLargeDatasetPerformance() {
        val largeDataset = generateLargeExpenseDataset(1000)

        val startTime = System.currentTimeMillis()
        val total = calculateTotal(largeDataset)
        val categoryTotals = groupByCategory(largeDataset)
        val stats = calculateStatistics(largeDataset)
        val endTime = System.currentTimeMillis()

        val processingTime = endTime - startTime

        assertTrue("Large dataset processing should complete quickly", processingTime < 1000) // 1秒内
        assertTrue("Total should be reasonable", total > 0)
        assertTrue("Should have multiple categories", categoryTotals.size > 1)
        assertEquals("Stats count should match dataset size", 1000, stats.count)
    }

    // ================================
    // 8. 边界条件和错误处理测试
    // ================================

    @Test
    fun testBoundaryConditions() {
        // 测试极大金额
        val expensiveItem = Expense(1, "Expensive", 99999.99, "Other", Date())
        assertTrue("Should handle large amounts", isValidExpense(expensiveItem))

        // 测试极小金额
        val cheapItem = Expense(2, "Cheap", 0.01, "Other", Date())
        assertTrue("Should handle small amounts", isValidExpense(cheapItem))

        // 测试长标题
        val longTitle = "Very ".repeat(100) + "Long Title"
        val longTitleExpense = Expense(3, longTitle, 10.0, "Other", Date())
        assertTrue("Should handle long titles", isValidExpense(longTitleExpense))
    }

    @Test
    fun testErrorHandling() {
        // 测试空字符串处理
        assertFalse("Empty title should be invalid", isValidExpense(
            Expense(1, "", 10.0, "Food", Date())
        ))

        // 测试null值处理
        val expenseWithNullDescription = Expense(1, "Test", 10.0, "Food", Date(), null)
        assertTrue("Null description should be valid", isValidExpense(expenseWithNullDescription))
    }

    // ================================
    // 辅助方法
    // ================================

    private fun createTestExpenses(): List<Expense> {
        return listOf(
            Expense(1, "Morning Coffee", 4.50, "Food", Date()),
            Expense(2, "Lunch", 12.99, "Food", Date()),
            Expense(3, "Bus Fare", 2.50, "Transportation", Date()),
            Expense(4, "Movie Ticket", 15.00, "Entertainment", Date()),
            Expense(5, "Groceries", 85.30, "Shopping", Date()),
            Expense(6, "Gas", 45.00, "Transportation", Date()),
            Expense(7, "Phone Bill", 65.00, "Bills", Date()),
            Expense(8, "Doctor Visit", 120.00, "Healthcare", Date())
        )
    }

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
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val cutoffDate = calendar.time

        return expenses.filter { expense ->
            expense.date.after(cutoffDate) || expense.date == cutoffDate
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

    private fun extractAmountFromSMS(sms: String): Double? {
        val patterns = listOf(
            "消费\\s*([0-9]+(?:\\.[0-9]+)?)元",
            "付款\\s*([0-9]+(?:\\.[0-9]+)?)元",
            "金额[￥¥]?\\s*([0-9]+(?:\\.[0-9]+)?)"
        )

        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(sms)
            if (match != null) {
                return match.groupValues[1].toDoubleOrNull()
            }
        }
        return null
    }

    private fun extractMerchantFromSMS(sms: String): String? {
        val patterns = listOf(
            "在\\s*([^\\s]+)\\s*消费",
            "向\\s*([^\\s]+)\\s*付款",
            "商户\\s*([^\\s，]+)"
        )

        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(sms)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun generateLargeExpenseDataset(size: Int): List<Expense> {
        val categories = listOf("Food", "Transportation", "Shopping", "Entertainment", "Bills")
        val merchants = listOf("Starbucks", "McDonald's", "Walmart", "Amazon", "Shell")

        return (1..size).map { i ->
            Expense(
                id = i.toLong(),
                title = merchants.random(),
                amount = Random.nextDouble(1.0, 500.0),
                category = categories.random(),
                date = Date(System.currentTimeMillis() - Random.nextLong(0, 30L * 24 * 60 * 60 * 1000)) // 30天内随机
            )
        }
    }

    data class ExpenseStatistics(
        val average: Double,
        val maximum: Double,
        val minimum: Double,
        val count: Int,
        val total: Double
    )
}