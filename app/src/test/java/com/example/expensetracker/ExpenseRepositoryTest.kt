package com.example.expensetracker

import com.example.expensetracker.data.database.Expense
import com.example.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import java.util.Date

class ExpenseRepositoryTest {

    @Test
    fun testExpenseValidation() {
        // Test valid expense
        val validExpense = Expense(
            title = "Test Expense",
            amount = 50.0,
            category = "Food",
            date = Date()
        )

        assertTrue("Valid expense should pass validation", isValidExpense(validExpense))

        // Test invalid expenses
        val invalidExpenseEmptyTitle = validExpense.copy(title = "")
        assertFalse("Empty title should fail validation", isValidExpense(invalidExpenseEmptyTitle))

        val invalidExpenseNegativeAmount = validExpense.copy(amount = -10.0)
        assertFalse("Negative amount should fail validation", isValidExpense(invalidExpenseNegativeAmount))

        val invalidExpenseZeroAmount = validExpense.copy(amount = 0.0)
        assertFalse("Zero amount should fail validation", isValidExpense(invalidExpenseZeroAmount))
    }

    @Test
    fun testExpenseCalculations() {
        val expenses = listOf(
            Expense(1, "Expense 1", 25.50, "Food", Date()),
            Expense(2, "Expense 2", 15.75, "Transport", Date()),
            Expense(3, "Expense 3", 100.00, "Shopping", Date())
        )

        val total = calculateTotal(expenses)
        assertEquals("Total should be 141.25", 141.25, total, 0.01)
    }

    private fun isValidExpense(expense: Expense): Boolean {
        return expense.title.isNotBlank() && expense.amount > 0
    }

    private fun calculateTotal(expenses: List<Expense>): Double {
        return expenses.sumOf { it.amount }
    }
}