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

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }
}