package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.AiAdviceState
import com.example.ai.AiAdvisorService
import com.example.data.BusinessRepository
import com.example.data.entity.CustomerEntity
import com.example.data.entity.DueEntryEntity
import com.example.data.entity.TransactionEntity
import com.example.ui.components.CategoryShare
import com.example.ui.theme.CategoryColors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(
    private val repository: BusinessRepository,
    private val aiAdvisorService: AiAdvisorService = AiAdvisorService()
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            repository.deleteAllTransactions()
        }
    }

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month Selector for Reports - Always normalize to Day 1 to avoid month rollover bugs
    private val _selectedCalendar = MutableStateFlow(
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    )
    val selectedCalendar: StateFlow<Calendar> = _selectedCalendar.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyTransactions: StateFlow<List<TransactionEntity>> = _selectedCalendar
        .flatMapLatest { cal ->
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            repository.getTransactionsForMonth(year, month)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Fully reactive selected customer for history bottom sheet
    private val _selectedCustomerId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomerId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getCustomerById(id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val customerHistoryEntries: StateFlow<List<DueEntryEntity>> = _selectedCustomerId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getEntriesForCustomer(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Advisor State
    private val _aiAdviceState = MutableStateFlow<AiAdviceState>(AiAdviceState.Idle)
    val aiAdviceState: StateFlow<AiAdviceState> = _aiAdviceState.asStateFlow()

    fun selectCustomerForHistory(customerId: Long?) {
        _selectedCustomerId.value = customerId
    }

    fun changeMonth(monthDelta: Int) {
        val current = _selectedCalendar.value.clone() as Calendar
        current.add(Calendar.MONTH, monthDelta)
        current.set(Calendar.DAY_OF_MONTH, 1)
        _selectedCalendar.value = current
    }

    fun addTransaction(type: String, category: String, amount: Double, note: String) {
        viewModelScope.launch {
            repository.addTransaction(type, category, amount, note)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun deleteAllTransactions() {
        viewModelScope.launch {
            repository.deleteAllTransactions()
        }
    }

    fun createCustomer(name: String, phone: String, address: String, initialDue: Double, initialNote: String) {
        viewModelScope.launch {
            repository.createCustomer(name, phone, address, initialDue, initialNote)
        }
    }

    fun updateCustomerDetails(customerId: Long, name: String, phone: String, address: String) {
        viewModelScope.launch {
            repository.updateCustomerDetails(customerId, name, phone, address)
        }
    }

    fun addCustomerCredit(customerId: Long, amount: Double, itemDescription: String) {
        viewModelScope.launch {
            repository.addCustomerCredit(customerId, amount, itemDescription)
        }
    }

    fun recordCustomerPayment(customerId: Long, amount: Double, note: String, addToIncome: Boolean) {
        viewModelScope.launch {
            repository.recordCustomerPayment(customerId, amount, note, addToIncome)
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            if (_selectedCustomerId.value == customer.id) {
                _selectedCustomerId.value = null
            }
            repository.deleteCustomer(customer)
        }
    }

    // AI Financial Advisor Request (gemini-4-argon with ThinkingLevel.HIGH)
    fun requestFinancialAudit(customPrompt: String? = null) {
        viewModelScope.launch {
            _aiAdviceState.value = AiAdviceState.Loading
            try {
                val transactions = allTransactions.value
                val customers = allCustomers.value
                val advice = aiAdvisorService.generateBusinessAudit(transactions, customers, customPrompt)
                _aiAdviceState.value = AiAdviceState.Success(advice)
            } catch (e: Exception) {
                _aiAdviceState.value = AiAdviceState.Error(
                    e.localizedMessage ?: "Failed to generate AI financial analysis."
                )
            }
        }
    }

    fun clearAiAdvice() {
        _aiAdviceState.value = AiAdviceState.Idle
    }

    // Helper to compute category breakdown for a given transaction list
    fun computeCategoryShares(expenses: List<TransactionEntity>): List<CategoryShare> {
        val validExpenses = expenses.filter { it.amount > 0 }
        val totalExpense = validExpenses.sumOf { it.amount }
        if (totalExpense <= 0.0) return emptyList()

        val grouped = validExpenses.groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }

        return grouped.mapIndexed { index, (category, amount) ->
            val percentage = ((amount / totalExpense) * 100).toFloat()
            val color = CategoryColors[index % CategoryColors.size]
            CategoryShare(category, amount, percentage, color)
        }
    }
}

class MainViewModelFactory(private val repository: BusinessRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
