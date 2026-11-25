package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreBusinessRepository
import com.example.data.ShopLinkRepository
import com.example.data.model.CustomerRecord
import com.example.data.model.DueEntryRecord
import com.example.data.model.GeneratedLinkCode
import com.example.data.model.LinkedDeviceRecord
import com.example.data.model.TransactionRecord
import com.example.data.model.UserProfileRecord
import com.example.ui.components.CategoryShare
import com.example.ui.theme.CategoryColors
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data object Missing : ProfileUiState
    data class Ready(val profile: UserProfileRecord) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

sealed interface UiEvent {
    data class Success(val message: String) : UiEvent
    data class Error(val message: String) : UiEvent
    data class TransactionDeleted(val deleted: TransactionRecord) : UiEvent
}

private sealed interface OwnProfileSnapshot {
    data object Loading : OwnProfileSnapshot
    data class Loaded(val profile: UserProfileRecord?) : OwnProfileSnapshot
    data class Error(val message: String) : OwnProfileSnapshot
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: FirestoreBusinessRepository,
    private val shopLinkRepository: ShopLinkRepository,
    private val currentUserId: String
) : ViewModel() {

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents: Flow<UiEvent> = _uiEvents.receiveAsFlow()

    private val _profileRetryTrigger = MutableStateFlow(0)
    private val isHandlingRevocation = AtomicBoolean(false)

    private fun friendlyFirestoreMessage(error: Throwable, fallback: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, fallback, root)
        if (firestoreError != null) {
            return when (firestoreError.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "Connection issue. Check your internet and try again."
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Permission denied while syncing. Please check your shop access or sign in again."
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "Session expired. Please sign in again."
                else -> firestoreError.localizedMessage ?: fallback
            }
        }
        return root.localizedMessage ?: fallback
    }

    private val ownProfileSnapshot: StateFlow<OwnProfileSnapshot> = _profileRetryTrigger
        .flatMapLatest {
            repository.observeUserProfile(currentUserId)
                .map<UserProfileRecord?, OwnProfileSnapshot> { OwnProfileSnapshot.Loaded(it) }
                .catch { error ->
                    emit(OwnProfileSnapshot.Error(friendlyFirestoreMessage(error, "Unable to load your profile.")))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), OwnProfileSnapshot.Loading)

    val ownProfile: StateFlow<UserProfileRecord?> = ownProfileSnapshot
        .map { (it as? OwnProfileSnapshot.Loaded)?.profile }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val activeLedgerOwnerId: StateFlow<String> = ownProfile
        .map { profile ->
            val linked = profile?.linkedOwnerId?.trim().orEmpty()
            if (linked.isNotBlank() && linked != currentUserId) linked else currentUserId
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), currentUserId)

    val isLinkedToOtherShop: StateFlow<Boolean> = activeLedgerOwnerId
        .map { it != currentUserId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    val profileState: StateFlow<ProfileUiState> = ownProfileSnapshot
        .flatMapLatest { snap ->
            when (snap) {
                is OwnProfileSnapshot.Loading -> flowOf(ProfileUiState.Loading)
                is OwnProfileSnapshot.Error -> flowOf(ProfileUiState.Error(snap.message))
                is OwnProfileSnapshot.Loaded -> {
                    val own = snap.profile
                    val linkedOwner = own?.linkedOwnerId?.trim().orEmpty()
                    if (linkedOwner.isNotBlank() && linkedOwner != currentUserId) {
                        combine(
                            shopLinkRepository.observeLinkedDeviceMembership(linkedOwner, currentUserId),
                            repository.observeUserProfile(linkedOwner)
                                .catch { error ->
                                    val fsError = generateSequence(error) { it.cause }
                                        .filterIsInstance<FirebaseFirestoreException>()
                                        .firstOrNull()
                                    if (fsError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                        emit(null)
                                    } else {
                                        throw error
                                    }
                                }
                        ) { membership, ownerProfile ->
                            if (membership == null || ownerProfile == null) {
                                handleRevokedLink()
                                ProfileUiState.Loading
                            } else {
                                isHandlingRevocation.set(false)
                                ProfileUiState.Ready(ownerProfile)
                            }
                        }.catch { error ->
                            emit(ProfileUiState.Error(friendlyFirestoreMessage(error, "Unable to sync linked shop.")))
                        }
                    } else if (own == null || own.isPlaceholderLinkedProfile) {
                        flowOf(ProfileUiState.Missing)
                    } else {
                        flowOf(ProfileUiState.Ready(own))
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ProfileUiState.Loading)

    private fun handleRevokedLink() {
        if (!isHandlingRevocation.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                _selectedCustomerId.value = null
                shopLinkRepository.clearLocalLinkedOwnerPointer()
                _uiEvents.send(UiEvent.Error("Access to the linked shop was disconnected by the shop owner."))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed clearing revoked linkedOwnerId", e)
            } finally {
                isHandlingRevocation.set(false)
            }
        }
    }

    private val _isSavingProfile = MutableStateFlow(false)
    val isSavingProfile: StateFlow<Boolean> = _isSavingProfile.asStateFlow()

    private val _operationError = MutableStateFlow<String?>(null)
    val operationError: StateFlow<String?> = _operationError.asStateFlow()

    // --- MULTI-DEVICE LINKING STATE ---
    private val _activeLinkCode = MutableStateFlow<GeneratedLinkCode?>(null)
    val activeLinkCode: StateFlow<GeneratedLinkCode?> = _activeLinkCode.asStateFlow()

    private val _isGeneratingLinkCode = MutableStateFlow(false)
    val isGeneratingLinkCode: StateFlow<Boolean> = _isGeneratingLinkCode.asStateFlow()

    private val _isLinkingShop = MutableStateFlow(false)
    val isLinkingShop: StateFlow<Boolean> = _isLinkingShop.asStateFlow()

    private val _linkShopError = MutableStateFlow<String?>(null)
    val linkShopError: StateFlow<String?> = _linkShopError.asStateFlow()

    val linkedDevices: StateFlow<List<LinkedDeviceRecord>> = isLinkedToOtherShop
        .flatMapLatest { isLinked ->
            if (isLinked) {
                flowOf(emptyList())
            } else {
                shopLinkRepository.observeLinkedDevices(currentUserId)
                    .catch { error ->
                        Log.w(TAG, "Failed to observe linked devices", error)
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    // --- ACTIVE LEDGER STREAMS ---
    val allTransactions: StateFlow<List<TransactionRecord>> = activeLedgerOwnerId
        .flatMapLatest { ownerId ->
            repository.observeTransactions(ownerId)
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED && ownerId != currentUserId) {
                        handleRevokedLink()
                    } else {
                        val msg = friendlyFirestoreMessage(error, "Failed to sync transactions.")
                        _operationError.value = msg
                        _uiEvents.trySend(UiEvent.Error(msg))
                    }
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val incomeTransactions: StateFlow<List<TransactionRecord>> = allTransactions
        .map { list -> list.filter { it.type == "INCOME" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val expenseTransactions: StateFlow<List<TransactionRecord>> = allTransactions
        .map { list -> list.filter { it.type == "EXPENSE" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val allCustomers: StateFlow<List<CustomerRecord>> = activeLedgerOwnerId
        .flatMapLatest { ownerId ->
            repository.observeCustomers(ownerId)
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED && ownerId != currentUserId) {
                        handleRevokedLink()
                    } else {
                        val msg = friendlyFirestoreMessage(error, "Failed to sync customers.")
                        _operationError.value = msg
                        _uiEvents.trySend(UiEvent.Error(msg))
                    }
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    // Month Selector for Reports - normalized to Day 1 to avoid month rollover issues
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

    val monthlyTransactions: StateFlow<List<TransactionRecord>> = combine(
        allTransactions,
        _selectedCalendar
    ) { transactions, cal ->
        FirestoreBusinessRepository.filterTransactionsForMonth(
            transactions = transactions,
            year = cal.get(Calendar.YEAR),
            monthZeroIndexed = cal.get(Calendar.MONTH)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val _selectedCustomerId = MutableStateFlow<String?>(null)

    val selectedCustomer: StateFlow<CustomerRecord?> = combine(
        activeLedgerOwnerId,
        _selectedCustomerId
    ) { ownerId, id -> ownerId to id }
        .flatMapLatest { (ownerId, id) ->
            if (!id.isNullOrBlank()) {
                repository.observeCustomerById(ownerId, id)
                    .catch { error ->
                        val msg = friendlyFirestoreMessage(error, "Failed to load customer.")
                        _operationError.value = msg
                        _uiEvents.trySend(UiEvent.Error(msg))
                        emit(null)
                    }
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val customerHistoryEntries: StateFlow<List<DueEntryRecord>> = combine(
        activeLedgerOwnerId,
        _selectedCustomerId
    ) { ownerId, id -> ownerId to id }
        .flatMapLatest { (ownerId, id) ->
            if (!id.isNullOrBlank()) {
                repository.observeEntriesForCustomer(ownerId, id)
                    .catch { error ->
                        val msg = friendlyFirestoreMessage(error, "Failed to load customer history.")
                        _operationError.value = msg
                        _uiEvents.trySend(UiEvent.Error(msg))
                        emit(emptyList())
                    }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    fun clearError() {
        _operationError.value = null
    }

    fun clearLinkShopError() {
        _linkShopError.value = null
    }

    fun retryProfileSync() {
        _operationError.value = null
        _profileRetryTrigger.value += 1
    }

    fun selectCustomerForHistory(customerId: String?) {
        _selectedCustomerId.value = customerId
    }

    fun changeMonth(monthDelta: Int) {
        val current = _selectedCalendar.value.clone() as Calendar
        current.add(Calendar.MONTH, monthDelta)
        current.set(Calendar.DAY_OF_MONTH, 1)
        _selectedCalendar.value = current
    }

    // --- MULTI-DEVICE ACTIONS ---

    fun generateOrRotateLinkCode(shopName: String, ownerEmail: String) {
        if (isLinkedToOtherShop.value) return
        viewModelScope.launch {
            _isGeneratingLinkCode.value = true
            _operationError.value = null
            try {
                val previousHash = _activeLinkCode.value?.tokenHash
                    ?: ownProfile.value?.activeLinkTokenHash
                val generated = shopLinkRepository.generateOrRotateShopLinkCode(
                    shopName = shopName,
                    ownerEmail = ownerEmail,
                    previousTokenHash = previousHash
                )
                _activeLinkCode.value = generated
                _uiEvents.send(UiEvent.Success("New 6-digit link code ready for 15 minutes"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Could not generate link code.")
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            } finally {
                _isGeneratingLinkCode.value = false
            }
        }
    }

    fun linkToExistingShop(
        ownerEmail: String,
        code: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isLinkingShop.value = true
            _linkShopError.value = null
            _operationError.value = null
            try {
                val shopName = shopLinkRepository.linkDeviceByEmailAndCode(
                    ownerEmail = ownerEmail,
                    code = code
                )
                _selectedCustomerId.value = null
                _activeLinkCode.value = null
                _uiEvents.send(UiEvent.Success("Connected to $shopName"))
                onSuccess?.invoke()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Unable to link to that shop profile.")
                _linkShopError.value = msg
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            } finally {
                _isLinkingShop.value = false
            }
        }
    }

    fun unlinkDevice(device: LinkedDeviceRecord) {
        viewModelScope.launch {
            try {
                shopLinkRepository.unlinkDevice(device.deviceUid)
                _uiEvents.send(UiEvent.Success("${device.deviceName} unlinked"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Failed to unlink device.")
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            }
        }
    }

    fun disconnectFromLinkedShop(onDisconnected: (() -> Unit)? = null) {
        val targetOwner = activeLedgerOwnerId.value
        if (targetOwner == currentUserId) return
        viewModelScope.launch {
            try {
                _selectedCustomerId.value = null
                shopLinkRepository.disconnectFromLinkedShop(targetOwner)
                _uiEvents.send(UiEvent.Success("Disconnected from linked shop"))
                onDisconnected?.invoke()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Failed to disconnect from linked shop.")
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            }
        }
    }

    // --- PROFILE & LEDGER CRUD ---

    fun saveProfile(displayName: String, phoneNumber: String, email: String) {
        if (isLinkedToOtherShop.value) return
        viewModelScope.launch {
            _isSavingProfile.value = true
            _operationError.value = null
            try {
                val alreadyExists = ownProfile.value != null
                repository.saveUserProfile(
                    displayName = displayName,
                    phoneNumber = phoneNumber,
                    email = email,
                    isExistingProfile = alreadyExists
                )
                if (alreadyExists) {
                    _uiEvents.send(UiEvent.Success("Profile updated"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Failed to save profile.")
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            } finally {
                _isSavingProfile.value = false
            }
        }
    }

    fun addTransaction(
        type: String,
        category: String,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            try {
                repository.addTransaction(
                    type = type,
                    category = category,
                    amount = amount,
                    note = note,
                    timestamp = timestamp,
                    ledgerOwnerId = activeLedgerOwnerId.value
                )
                val label = if (type == "INCOME") "Income added" else "Expense added"
                _uiEvents.send(UiEvent.Success(label))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Failed to save transaction.")
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            }
        }
    }

    fun updateTransaction(
        transactionId: String,
        type: String,
        category: String,
        amount: Double,
        note: String,
        timestamp: Long
    ) {
        viewModelScope.launch {
            try {
                repository.updateTransaction(
                    transactionId = transactionId,
                    type = type,
                    category = category,
                    amount = amount,
                    note = note,
                    timestamp = timestamp,
                    ledgerOwnerId = activeLedgerOwnerId.value
                )
                _uiEvents.send(UiEvent.Success("Changes saved"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Failed to update transaction.")
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            }
        }
    }

    fun deleteTransaction(transaction: TransactionRecord) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(
                    transactionId = transaction.id,
                    ledgerOwnerId = activeLedgerOwnerId.value
                )
                _uiEvents.send(UiEvent.TransactionDeleted(transaction))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Failed to delete transaction.")
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            }
        }
    }

    fun restoreTransaction(transaction: TransactionRecord) {
        viewModelScope.launch {
            try {
                repository.addTransaction(
                    type = transaction.type,
                    category = transaction.category,
                    amount = transaction.amount,
                    note = transaction.note,
                    timestamp = transaction.timestamp,
                    ledgerOwnerId = activeLedgerOwnerId.value
                )
                _uiEvents.send(UiEvent.Success("Entry restored"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Failed to restore entry.")
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            }
        }
    }

    fun deleteAllTransactions(typeFilter: String? = null) {
        viewModelScope.launch {
            try {
                repository.deleteAllTransactions(
                    typeFilter = typeFilter,
                    ledgerOwnerId = activeLedgerOwnerId.value
                )
                val label = when (typeFilter) {
                    "INCOME" -> "All income entries cleared"
                    "EXPENSE" -> "All expense entries cleared"
                    else -> "All transactions cleared"
                }
                _uiEvents.send(UiEvent.Success(label))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Failed to clear transactions.")
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            }
        }
    }

    fun createCustomer(
        name: String,
        phone: String,
        address: String,
        initialAmount: Double,
        initialNote: String,
        isInitialAdvance: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                repository.createCustomer(
                    name = name,
                    phone = phone,
                    address = address,
                    initialAmount = initialAmount,
                    initialNote = initialNote,
                    isInitialAdvance = isInitialAdvance,
                    ledgerOwnerId = activeLedgerOwnerId.value
                )
                _uiEvents.send(UiEvent.Success("Customer added"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Failed to create customer.")
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            }
        }
    }

    fun updateCustomerDetails(customerId: String, name: String, phone: String, address: String) {
        viewModelScope.launch {
            try {
                repository.updateCustomerDetails(
                    customerId = customerId,
                    name = name,
                    phone = phone,
                    address = address,
                    ledgerOwnerId = activeLedgerOwnerId.value
                )
                _uiEvents.send(UiEvent.Success("Customer updated"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Failed to update customer.")
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            }
        }
    }

    fun addCustomerCredit(
        customerId: String,
        amount: Double,
        itemDescription: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            try {
                repository.addCustomerCredit(
                    customerId = customerId,
                    amount = amount,
                    itemsDescription = itemDescription,
                    timestamp = timestamp,
                    ledgerOwnerId = activeLedgerOwnerId.value
                )
                _uiEvents.send(UiEvent.Success("Credit recorded"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Failed to add credit items.")
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            }
        }
    }

    fun recordCustomerPayment(
        customerId: String,
        amount: Double,
        note: String,
        isAdvance: Boolean = false,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            try {
                repository.recordCustomerPayment(
                    customerId = customerId,
                    amount = amount,
                    paymentNote = note,
                    isAdvance = isAdvance,
                    timestamp = timestamp,
                    ledgerOwnerId = activeLedgerOwnerId.value
                )
                _uiEvents.send(UiEvent.Success(if (isAdvance) "Advance recorded" else "Payment recorded"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Failed to record payment.")
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            }
        }
    }

    fun updateDueEntry(
        customerId: String,
        entryId: String,
        type: String,
        amount: Double,
        itemsDescription: String,
        timestamp: Long
    ) {
        viewModelScope.launch {
            try {
                repository.updateDueEntry(
                    customerId = customerId,
                    entryId = entryId,
                    type = type,
                    amount = amount,
                    itemsDescription = itemsDescription,
                    timestamp = timestamp,
                    ledgerOwnerId = activeLedgerOwnerId.value
                )
                _uiEvents.send(UiEvent.Success("Entry updated"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Failed to update entry.")
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            }
        }
    }

    fun deleteDueEntry(customerId: String, entryId: String) {
        viewModelScope.launch {
            try {
                repository.deleteDueEntry(
                    customerId = customerId,
                    entryId = entryId,
                    ledgerOwnerId = activeLedgerOwnerId.value
                )
                _uiEvents.send(UiEvent.Success("Entry deleted"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Failed to delete entry.")
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            }
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            try {
                if (_selectedCustomerId.value == customerId) {
                    _selectedCustomerId.value = null
                }
                repository.deleteCustomer(
                    customerId = customerId,
                    ledgerOwnerId = activeLedgerOwnerId.value
                )
                _uiEvents.send(UiEvent.Success("Customer removed"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = friendlyFirestoreMessage(e, "Failed to delete customer.")
                _operationError.value = msg
                _uiEvents.send(UiEvent.Error(msg))
            }
        }
    }

    fun computeCategoryShares(expenses: List<TransactionRecord>): List<CategoryShare> {
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
