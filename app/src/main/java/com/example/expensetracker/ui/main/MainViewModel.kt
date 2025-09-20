package com.example.expensetracker.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.Expense
import com.example.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val allExpenses: LiveData<List<Expense>> = repository.getAllExpenses()

    suspend fun getTotalAmount(): Double = repository.getTotalAmount()

    suspend fun getExpenseCount(): Int {
        return allExpenses.value?.size ?: 0
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun insertExpense(expense: Expense) {
        viewModelScope.launch {
            repository.insertExpense(expense)
        }
    }

    fun deleteAllExpenses() {
        viewModelScope.launch {
            allExpenses.value?.forEach { expense ->
                repository.deleteExpense(expense)
            }
        }
    }

    suspend fun getExpensesByCategory(category: String): List<Expense> {
        return allExpenses.value?.filter { it.category == category } ?: emptyList()
    }

    suspend fun getRecentExpenses(limit: Int = 5): List<Expense> {
        return allExpenses.value?.take(limit) ?: emptyList()
    }
}