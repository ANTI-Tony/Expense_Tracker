package com.example.expensetracker.data.repository

import androidx.lifecycle.LiveData
import com.example.expensetracker.data.database.CategoryTotal
import com.example.expensetracker.data.database.Expense
import com.example.expensetracker.data.database.ExpenseDao
import java.util.Date

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    fun getAllExpenses(): LiveData<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun getAllExpensesSync(): List<Expense> = expenseDao.getAllExpensesSync()

    suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)

    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    fun getExpensesByDateRange(startDate: Date, endDate: Date): LiveData<List<Expense>> =
        expenseDao.getExpensesByDateRange(startDate, endDate)

    suspend fun getExpensesByDateRangeSync(startDate: Date, endDate: Date): List<Expense> =
        expenseDao.getExpensesByDateRangeSync(startDate, endDate)

    suspend fun getCategoryTotals(): List<CategoryTotal> = expenseDao.getCategoryTotals()

    suspend fun getTotalAmount(): Double = expenseDao.getTotalAmount() ?: 0.0
}