package com.example.expensetracker.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetracker.R
import com.example.expensetracker.data.database.Expense
import com.example.expensetracker.data.database.ExpenseDatabase
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.service.ExpenseService
import com.example.expensetracker.ui.addedit.AddEditActivity
import com.example.expensetracker.ui.chart.ChartActivity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var repository: ExpenseRepository
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(repository)
    }

    companion object {
        const val REQUEST_ADD_EDIT = 1001
        const val EXTRA_EXPENSE = "extra_expense"
        private const val NOTIFICATION_PERMISSION_REQUEST = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize repository
        val database = ExpenseDatabase.getDatabase(applicationContext)
        repository = ExpenseRepository(database.expenseDao())

        setupRecyclerView()
        setupClickListeners()
        observeData()

        // Request notification permission for Android 13+
        requestNotificationPermission()

        // Start background service
        startService(Intent(this, ExpenseService::class.java))
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(
            onItemClick = { expense -> openEditExpense(expense) },
            onItemLongClick = { expense -> showDeleteDialog(expense) }
        )

        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        recyclerView.apply {
            adapter = expenseAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupClickListeners() {
        val fabAdd = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAdd)
        fabAdd.setOnClickListener {
            openAddExpense()
        }

        val btnChart = findViewById<android.widget.Button>(R.id.btnChart)
        btnChart.setOnClickListener {
            startActivity(Intent(this, ChartActivity::class.java))
        }
    }

    private fun observeData() {
        viewModel.allExpenses.observe(this) { expenses ->
            expenseAdapter.submitList(expenses)
        }

        lifecycleScope.launch {
            val totalAmount = viewModel.getTotalAmount()
            val tvTotalAmount = findViewById<android.widget.TextView>(R.id.tvTotalAmount)
            tvTotalAmount.text = "Total: ${
                NumberFormat.getCurrencyInstance(Locale.getDefault()).format(totalAmount)
            }"
        }
    }

    private fun openAddExpense() {
        val intent = Intent(this, AddEditActivity::class.java)
        startActivityForResult(intent, REQUEST_ADD_EDIT)
    }

    private fun openEditExpense(expense: Expense) {
        val intent = Intent(this, AddEditActivity::class.java).apply {
            putExtra(EXTRA_EXPENSE, expense)
        }
        startActivityForResult(intent, REQUEST_ADD_EDIT)
    }

    private fun showDeleteDialog(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete '${expense.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteExpense(expense)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_EDIT && resultCode == RESULT_OK) {
            // Data will be automatically updated through LiveData
            lifecycleScope.launch {
                val totalAmount = viewModel.getTotalAmount()
                val tvTotalAmount = findViewById<android.widget.TextView>(R.id.tvTotalAmount)
                tvTotalAmount.text = "Total: ${
                    NumberFormat.getCurrencyInstance(Locale.getDefault()).format(totalAmount)
                }"
            }
        }
    }
}