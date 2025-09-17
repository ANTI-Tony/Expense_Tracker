package com.example.expensetracker.ui.addedit

import androidx.lifecycle.ViewModel
import com.example.expensetracker.data.database.Expense
import com.example.expensetracker.data.repository.ExpenseRepository

class AddEditViewModel(private val repository: ExpenseRepository) : ViewModel() {

    suspend fun insertExpense(expense: Expense): Long {
        return repository.insertExpense(expense)
    }

    suspend fun updateExpense(expense: Expense) {
        repository.updateExpense(expense)
    }
}