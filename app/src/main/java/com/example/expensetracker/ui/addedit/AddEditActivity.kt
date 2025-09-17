package com.example.expensetracker.ui.addedit

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
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

    // UI Components
    private lateinit var etTitle: EditText
    private lateinit var etAmount: EditText
    private lateinit var etDescription: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit)

        // Initialize repository
        val database = ExpenseDatabase.getDatabase(applicationContext)
        repository = ExpenseRepository(database.expenseDao())

        initViews()
        setupUI()
        handleIntent()
        setupClickListeners()
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
    }

    private fun setupClickListeners() {
        btnSave.setOnClickListener {
            saveExpense()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        tvSelectedDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance().apply {
            time = selectedDate
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                selectedDate = newCalendar.time
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateDisplay() {
        tvSelectedDate.text = DateUtils.formatDate(selectedDate)
    }

    private fun saveExpense() {
        val title = etTitle.text.toString().trim()
        val amountText = etAmount.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()
        val description = etDescription.text.toString().trim()

        // Validation
        if (title.isEmpty()) {
            etTitle.error = "Title is required"
            return
        }

        if (amountText.isEmpty()) {
            etAmount.error = "Amount is required"
            return
        }

        val amount = try {
            amountText.toDouble()
        } catch (e: NumberFormatException) {
            etAmount.error = "Invalid amount"
            return
        }

        if (amount <= 0) {
            etAmount.error = "Amount must be greater than 0"
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

        lifecycleScope.launch {
            try {
                if (isEditMode) {
                    viewModel.updateExpense(expense)
                    Toast.makeText(this@AddEditActivity, "Expense updated", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.insertExpense(expense)
                    Toast.makeText(this@AddEditActivity, "Expense saved", Toast.LENGTH_SHORT).show()
                }
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddEditActivity, "Error saving expense", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}