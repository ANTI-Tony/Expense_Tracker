package com.example.expensetracker.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var repository: ExpenseRepository
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(repository)
    }

    companion object {
        private const val TAG = "MainActivity"
        const val REQUEST_ADD_EDIT = 1001
        const val EXTRA_EXPENSE = "extra_expense"
        private const val NOTIFICATION_PERMISSION_REQUEST = 1002

        // 保存状态的键
        private const val KEY_SCROLL_POSITION = "scroll_position"
        private const val KEY_TOTAL_AMOUNT = "total_amount"
    }

    // 保存滚动位置
    private var scrollPosition = 0
    private var isActivityVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")
        setContentView(R.layout.activity_main)

        // 恢复保存的状态
        savedInstanceState?.let {
            scrollPosition = it.getInt(KEY_SCROLL_POSITION, 0)
            Log.d(TAG, "Restored scroll position: $scrollPosition")
        }

        // Initialize repository
        val database = ExpenseDatabase.getDatabase(applicationContext)
        repository = ExpenseRepository(database.expenseDao())

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        setupSwipeToDelete()
        observeDataWithLifecycle()

        // Request notification permission for Android 13+
        requestNotificationPermission()

        // Start background service
        startService(Intent(this, ExpenseService::class.java))

        showWelcomeMessage()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart() called")

        // 启动后台服务（仅在Activity可见时）
        startService(Intent(this, ExpenseService::class.java))

        // 刷新数据以防在后台时有变化
        updateTotalAmount()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() called")
        isActivityVisible = true

        // 恢复滚动位置
        if (scrollPosition > 0) {
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
            recyclerView.scrollToPosition(scrollPosition)
        }

        // 检查并更新数据
        refreshDataIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() called")
        isActivityVisible = false

        // 保存当前滚动位置
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
        scrollPosition = layoutManager?.findFirstVisibleItemPosition() ?: 0
        Log.d(TAG, "Saved scroll position: $scrollPosition")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop() called")

        // 可以在这里停止一些不必要的操作
        // 但保持Service运行用于后台检测
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called")

        // 如果是真正的销毁（不是配置变化），清理资源
        if (!isChangingConfigurations) {
            Log.d(TAG, "Activity being destroyed permanently")
            // 这里可以做一些清理工作，但Service让它继续运行
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState() called")

        // 保存关键状态
        outState.putInt(KEY_SCROLL_POSITION, scrollPosition)

        // 保存当前总金额（如果已加载）
        lifecycleScope.launch {
            val totalAmount = viewModel.getTotalAmount()
            outState.putDouble(KEY_TOTAL_AMOUNT, totalAmount)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d(TAG, "onRestoreInstanceState() called")

        // 恢复状态已在onCreate中处理
        // 这里可以做一些额外的恢复工作
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart() called")

        // Activity从停止状态重新启动
        // 检查是否需要刷新数据
        refreshDataIfNeeded()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Expense Tracker"
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(
            onItemClick = { expense -> openEditExpense(expense) },
            onItemLongClick = { expense -> showDeleteDialog(expense) }
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.apply {
            adapter = expenseAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(
                context, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
            ))
        }
    }

    private fun setupSwipeToDelete() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val expense = expenseAdapter.currentList[position]

                lifecycleScope.launch {
                    viewModel.deleteExpense(expense)
                }

                Snackbar.make(
                    findViewById(R.id.main_layout),
                    "Expense '${expense.title}' deleted",
                    Snackbar.LENGTH_LONG
                ).setAction("UNDO") {
                    lifecycleScope.launch {
                        viewModel.insertExpense(expense)
                    }
                }.show()
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)
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

    private fun observeDataWithLifecycle() {
        // 使用 repeatOnLifecycle 确保只在 STARTED 状态时观察数据
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allExpenses.observe(this@MainActivity) { expenses ->
                    Log.d(TAG, "Received ${expenses.size} expenses")
                    expenseAdapter.submitList(expenses)
                    updateEmptyState(expenses.isEmpty())
                    updateTotalAmount()
                }
            }
        }
    }

    private fun refreshDataIfNeeded() {
        // 检查数据是否需要刷新
        Log.d(TAG, "Checking if data refresh is needed")
        updateTotalAmount()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val emptyStateView = findViewById<View>(R.id.emptyStateView)

        if (isEmpty) {
            recyclerView.visibility = View.GONE
            emptyStateView?.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateView?.visibility = View.GONE
        }
    }

    private fun updateTotalAmount() {
        lifecycleScope.launch {
            try {
                val totalAmount = viewModel.getTotalAmount()
                val tvTotalAmount = findViewById<android.widget.TextView>(R.id.tvTotalAmount)
                tvTotalAmount.text = "Total: ${
                    NumberFormat.getCurrencyInstance(Locale.getDefault()).format(totalAmount)
                }"
                Log.d(TAG, "Updated total amount: $totalAmount")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating total amount", e)
            }
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
        // 检查Activity是否仍在前台
        if (!isActivityVisible) {
            Log.w(TAG, "Attempted to show dialog when activity not visible")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete '${expense.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteExpense(expense)
                    if (isActivityVisible) {
                        Toast.makeText(this@MainActivity, "Expense deleted", Toast.LENGTH_SHORT).show()
                    }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Notification permission granted")
                    Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "Notification permission denied")
                    Toast.makeText(this, "Notifications disabled - some features may be limited", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showWelcomeMessage() {
        lifecycleScope.launch {
            val expenseCount = viewModel.getExpenseCount()
            if (expenseCount == 0 && isActivityVisible) {
                Toast.makeText(
                    this@MainActivity,
                    "Welcome! Add your first expense by tapping the + button",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete_all -> {
                if (isActivityVisible) {
                    showDeleteAllDialog()
                }
                true
            }
            R.id.action_about -> {
                if (isActivityVisible) {
                    showAboutDialog()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteAllDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete All Expenses")
            .setMessage("Are you sure you want to delete ALL expenses? This action cannot be undone.")
            .setPositiveButton("Delete All") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteAllExpenses()
                    if (isActivityVisible) {
                        Toast.makeText(this@MainActivity, "All expenses deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About Expense Tracker")
            .setMessage("Expense Tracker v1.0\n\nA simple and efficient way to track your daily expenses.\n\nFeatures:\n• Add/Edit/Delete expenses\n• Categorize spending\n• Visual charts\n• Automatic transaction detection\n\nDeveloped as an Android learning project.")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        if (requestCode == REQUEST_ADD_EDIT && resultCode == RESULT_OK) {
            // 数据会通过LiveData自动更新
            Log.d(TAG, "Expense operation completed successfully")
            updateTotalAmount()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        Log.d(TAG, "onUserLeaveHint() called - User is leaving the app")
        // 用户按下Home键或切换到其他应用
        // 可以在这里做一些状态保存
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed() called")
        // 可以在这里处理返回键逻辑
        super.onBackPressed()
    }
}