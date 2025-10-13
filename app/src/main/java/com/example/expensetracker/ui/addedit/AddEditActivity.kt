package com.example.expensetracker.ui.addedit

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.expensetracker.R
import com.example.expensetracker.data.database.Expense
import com.example.expensetracker.data.database.ExpenseDatabase
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.ui.main.MainActivity
import com.example.expensetracker.utils.DateUtils
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class AddEditActivity : AppCompatActivity() {

    private lateinit var repository: ExpenseRepository
    private val viewModel: AddEditViewModel by viewModels {
        AddEditViewModelFactory(repository)
    }

    private var selectedDate: Date = DateUtils.getCurrentDate()
    private var editingExpense: Expense? = null
    private var isEditMode = false
    private var hasUnsavedChanges = false

    // UI Components
    private lateinit var etTitle: EditText
    private lateinit var etAmount: EditText
    private lateinit var etDescription: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    companion object {
        private const val TAG = "AddEditActivity"

        // 保存状态的键
        private const val KEY_TITLE = "title"
        private const val KEY_AMOUNT = "amount"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_CATEGORY_POSITION = "category_position"
        private const val KEY_SELECTED_DATE = "selected_date"
        private const val KEY_HAS_UNSAVED_CHANGES = "has_unsaved_changes"
        private const val KEY_IS_EDIT_MODE = "is_edit_mode"
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleBackPress()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")
        setContentView(R.layout.activity_add_edit)

        // Initialize repository
        val database = ExpenseDatabase.getDatabase(applicationContext)
        repository = ExpenseRepository(database.expenseDao())

        initViews()
        setupUI()
        handleIntent()
        restoreInstanceState(savedInstanceState)
        setupClickListeners()
        setupTextWatchers()

        // 注册返回键回调
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart() called")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() called")

        // 确保日期显示是最新的
        updateDateDisplay()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() called")

        // 保存当前的输入状态
        checkForUnsavedChanges()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop() called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called")

        if (!isChangingConfigurations) {
            Log.d(TAG, "Activity being destroyed permanently")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState() called")

        // 保存用户输入的所有数据
        outState.apply {
            putString(KEY_TITLE, etTitle.text.toString())
            putString(KEY_AMOUNT, etAmount.text.toString())
            putString(KEY_DESCRIPTION, etDescription.text.toString())
            putInt(KEY_CATEGORY_POSITION, spinnerCategory.selectedItemPosition)
            putLong(KEY_SELECTED_DATE, selectedDate.time)
            putBoolean(KEY_HAS_UNSAVED_CHANGES, hasUnsavedChanges)
            putBoolean(KEY_IS_EDIT_MODE, isEditMode)
        }

        Log.d(TAG, "Saved state: title=${etTitle.text}, amount=${etAmount.text}")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d(TAG, "onRestoreInstanceState() called")
        // 恢复逻辑在 restoreInstanceState 方法中处理
    }

    private fun restoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let { bundle ->
            Log.d(TAG, "Restoring instance state")

            // 恢复用户输入
            etTitle.setText(bundle.getString(KEY_TITLE, ""))
            etAmount.setText(bundle.getString(KEY_AMOUNT, ""))
            etDescription.setText(bundle.getString(KEY_DESCRIPTION, ""))

            // 恢复分类选择
            val categoryPosition = bundle.getInt(KEY_CATEGORY_POSITION, 0)
            if (categoryPosition < spinnerCategory.adapter.count) {
                spinnerCategory.setSelection(categoryPosition)
            }

            // 恢复选择的日期
            val dateTime = bundle.getLong(KEY_SELECTED_DATE, 0)
            if (dateTime > 0) {
                selectedDate = Date(dateTime)
                updateDateDisplay()
            }

            // 恢复其他状态
            hasUnsavedChanges = bundle.getBoolean(KEY_HAS_UNSAVED_CHANGES, false)
            isEditMode = bundle.getBoolean(KEY_IS_EDIT_MODE, false)

            Log.d(TAG, "Restored state: title=${etTitle.text}, amount=${etAmount.text}")
        }
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etTitle)
        etAmount = findViewById(R.id.etAmount)
        etDescription = findViewById(R.id.etDescription)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun setupUI() {
        // Setup category spinner
        val categories = resources.getStringArray(R.array.expense_categories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Setup toolbar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = if (isEditMode) "Edit Expense" else "Add Expense"
        }

        updateDateDisplay()
    }

    private fun handleIntent() {
        editingExpense = intent.getParcelableExtra(MainActivity.EXTRA_EXPENSE)
        if (editingExpense != null) {
            isEditMode = true
            populateFields(editingExpense!!)
            supportActionBar?.title = "Edit Expense"
            Log.d(TAG, "Edit mode: editing expense with ID ${editingExpense?.id}")
        } else {
            Log.d(TAG, "Add mode: creating new expense")
        }
    }

    private fun populateFields(expense: Expense) {
        etTitle.setText(expense.title)
        etAmount.setText(expense.amount.toString())
        etDescription.setText(expense.description)

        // Set category
        val categories = resources.getStringArray(R.array.expense_categories)
        val categoryIndex = categories.indexOf(expense.category)
        if (categoryIndex >= 0) {
            spinnerCategory.setSelection(categoryIndex)
        }

        selectedDate = expense.date
        updateDateDisplay()

        // 重置未保存更改标志，因为这是从现有数据加载的
        hasUnsavedChanges = false
    }

    private fun setupTextWatchers() {
        // 监听文本变化以标记未保存的更改
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                hasUnsavedChanges = true
                Log.d(TAG, "Text changed, marking as unsaved")
            }
        }

        etTitle.addTextChangedListener(textWatcher)
        etAmount.addTextChangedListener(textWatcher)
        etDescription.addTextChangedListener(textWatcher)

        // 监听分类选择变化
        spinnerCategory.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                hasUnsavedChanges = true
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupClickListeners() {
        btnSave.setOnClickListener {
            saveExpense()
        }

        btnCancel.setOnClickListener {
            handleBackPress()
        }

        tvSelectedDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance().apply {
            time = selectedDate
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                selectedDate = newCalendar.time
                updateDateDisplay()
                hasUnsavedChanges = true
                Log.d(TAG, "Date changed to: $selectedDate")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun updateDateDisplay() {
        tvSelectedDate.text = DateUtils.formatDate(selectedDate)
    }

    private fun checkForUnsavedChanges() {
        // 检查是否有未保存的更改
        if (!hasUnsavedChanges) {
            val currentTitle = etTitle.text.toString().trim()
            val currentAmount = etAmount.text.toString().trim()
            val currentDescription = etDescription.text.toString().trim()

            if (isEditMode && editingExpense != null) {
                // 编辑模式：与原始数据比较
                hasUnsavedChanges = currentTitle != editingExpense!!.title ||
                        currentAmount != editingExpense!!.amount.toString() ||
                        currentDescription != (editingExpense!!.description ?: "")
            } else {
                // 添加模式：检查是否有任何输入
                hasUnsavedChanges = currentTitle.isNotEmpty() ||
                        currentAmount.isNotEmpty() ||
                        currentDescription.isNotEmpty()
            }
        }
    }

    private fun handleBackPress() {
        checkForUnsavedChanges()

        if (hasUnsavedChanges) {
            showUnsavedChangesDialog()
        } else {
            finishActivity()
        }
    }

    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Do you want to save them before leaving?")
            .setPositiveButton("Save") { _, _ ->
                saveExpense()
            }
            .setNegativeButton("Discard") { _, _ ->
                Log.d(TAG, "User discarded unsaved changes")
                hasUnsavedChanges = false
                finishActivity()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun saveExpense() {
        val title = etTitle.text.toString().trim()
        val amountText = etAmount.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()
        val description = etDescription.text.toString().trim()

        // Validation
        if (title.isEmpty()) {
            etTitle.error = "标题不能为空"
            etTitle.requestFocus()
            return
        }

        if (amountText.isEmpty()) {
            etAmount.error = "金额不能为空"
            etAmount.requestFocus()
            return
        }

        val amount = try {
            amountText.toDouble()
        } catch (e: NumberFormatException) {
            etAmount.error = "请输入有效的金额"
            etAmount.requestFocus()
            return
        }

        if (amount <= 0) {
            etAmount.error = "金额必须大于0"
            etAmount.requestFocus()
            return
        }

        val expense = if (isEditMode) {
            editingExpense!!.copy(
                title = title,
                amount = amount,
                category = category,
                date = selectedDate,
                description = description.ifEmpty { null }
            )
        } else {
            Expense(
                title = title,
                amount = amount,
                category = category,
                date = selectedDate,
                description = description.ifEmpty { null }
            )
        }

        // 禁用保存按钮避免重复提交
        btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                if (isEditMode) {
                    viewModel.updateExpense(expense)
                    Log.d(TAG, "Expense updated successfully: ${expense.title}")
                    Toast.makeText(this@AddEditActivity, "Expense updated", Toast.LENGTH_SHORT).show()
                } else {
                    val newId = viewModel.insertExpense(expense)
                    Log.d(TAG, "Expense saved successfully with ID: $newId")
                    Toast.makeText(this@AddEditActivity, "Expense saved", Toast.LENGTH_SHORT).show()
                }

                hasUnsavedChanges = false
                setResult(RESULT_OK)
                finishActivity()

            } catch (e: Exception) {
                Log.e(TAG, "Error saving expense", e)
                Toast.makeText(this@AddEditActivity, "Error saving expense: ${e.message}", Toast.LENGTH_LONG).show()
                btnSave.isEnabled = true // 重新启用保存按钮
            }
        }
    }

    private fun finishActivity() {
        Log.d(TAG, "Finishing activity")
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        Log.d(TAG, "Navigation up pressed")
        handleBackPress()
        return true
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        Log.d(TAG, "onUserLeaveHint() called - User is leaving the app")
        checkForUnsavedChanges()
    }
}