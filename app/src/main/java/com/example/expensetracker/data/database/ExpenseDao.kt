package com.example.expensetracker.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.Date

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): LiveData<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpensesSync(): List<Expense>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: Date, endDate: Date): LiveData<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getExpensesByDateRangeSync(startDate: Date, endDate: Date): List<Expense>

    @Query("SELECT category, SUM(amount) as total FROM expenses GROUP BY category")
    suspend fun getCategoryTotals(): List<CategoryTotal>

    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun getTotalAmount(): Double?
}

data class CategoryTotal(
    val category: String,
    val total: Double
)