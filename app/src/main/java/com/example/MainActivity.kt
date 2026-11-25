package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.FirestoreBusinessRepository
import com.example.data.ShopLinkRepository
import com.example.data.ThemeMode
import com.example.data.ThemePreferences
import com.example.data.model.CustomerRecord
import com.example.data.model.TransactionRecord
import com.example.data.model.UserProfileRecord
import com.example.ui.MainViewModel
import com.example.ui.ProfileUiState
import com.example.ui.UiEvent
import com.example.ui.components.AddCustomerDialog
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.AdjustDueDialog
import com.example.ui.components.AuroraMood
import com.example.ui.components.CustomerHistoryBottomSheet
import com.example.ui.components.DueAdjustmentMode
import com.example.ui.components.FloatingLiquidGlassDock
import com.example.ui.components.InteractiveAuroraBackground
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.QuickAddFab
import com.example.ui.screens.AccountProfileDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CashbookScreen
import com.example.ui.screens.CashbookSegment
import com.example.ui.screens.DueLedgerScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ledgerColors
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val themePreferences = ThemePreferences(applicationContext)

        setContent {
            val themeMode by themePreferences.themeModeFlow
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val coroutineScope = rememberCoroutineScope()

            DisposableEffect(useDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (useDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (useDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                HishabKhataRoot(
                    isDarkTheme = useDarkTheme,
                    currentThemeMode = themeMode,
                    onThemeModeSelected = { newMode ->
                        coroutineScope.launch {
                            themePreferences.setThemeMode(newMode)
                        }
                    }
                )
            }
        }
    }
}

enum class NavigationTab(val label: String, val subtitle: String, val index: Int) {
    CASHBOOK("Cashbook", "Cashbook", 0),
    DUE_LEDGER("Due", "Due & Advance", 1),
    REPORTS("Reports", "Monthly Overview", 2)
}

@Composable
fun HishabKhataRoot(
    isDarkTheme: Boolean,
    currentThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val authStateFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authStateFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var activeAuroraMood by remember { mutableStateOf(AuroraMood.NEUTRAL) }
    var pendingPhoneNumber by remember { mutableStateOf("") }

    InteractiveAuroraBackground(
        mood = activeAuroraMood,
        isDarkTheme = isDarkTheme
    ) {
        val activeUser = currentUser
        if (activeUser == null) {
            LaunchedEffect(Unit) {
                activeAuroraMood = AuroraMood.NEUTRAL
            }
            AuthScreen(
                onAuthSuccess = {},
                onPhoneNumberCaptured = { pendingPhoneNumber = it }
            )
        } else {
            key(activeUser.uid) {
                val appContext = remember(context) { context.applicationContext }
                val viewModel: MainViewModel = viewModel(
                    key = activeUser.uid,
                    factory = viewModelFactory {
                        initializer {
                            MainViewModel(
                                repository = FirestoreBusinessRepository(appContext),
                                shopLinkRepository = ShopLinkRepository(appContext),
                                currentUserId = activeUser.uid
                            )
                        }
                    }
                )

                val profileState by viewModel.profileState.collectAsStateWithLifecycle()
                val isSavingProfile by viewModel.isSavingProfile.collectAsStateWithLifecycle()
                val isLinkingShop by viewModel.isLinkingShop.collectAsStateWithLifecycle()
                val linkShopError by viewModel.linkShopError.collectAsStateWithLifecycle()
                val operationError by viewModel.operationError.collectAsStateWithLifecycle()

                val handleSignOut: () -> Unit = {
                    pendingPhoneNumber = ""
                    signOutUser(credentialManager = credentialManager, scope = coroutineScope)
                }

                when (val state = profileState) {
                    is ProfileUiState.Loading -> {
                        LaunchedEffect(Unit) {
                            activeAuroraMood = AuroraMood.NEUTRAL
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LiquidGlassCard(cornerRadius = 24.dp) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 26.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = if (isLinkingShop) "Connecting to existing shop..." else "Syncing your ledger...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    is ProfileUiState.Missing -> {
                        LaunchedEffect(Unit) {
                            activeAuroraMood = AuroraMood.NEUTRAL
                        }
                        ProfileSetupScreen(
                            initialName = activeUser.displayName.orEmpty(),
                            initialPhone = pendingPhoneNumber.ifBlank { activeUser.phoneNumber.orEmpty() },
                            initialEmail = activeUser.email.orEmpty(),
                            isSaving = isSavingProfile,
                            errorMessage = operationError,
                            onSaveProfile = { name, phone, email ->
                                viewModel.saveProfile(name, phone, email)
                            },
                            onSignOut = handleSignOut,
                            isLinkingShop = isLinkingShop,
                            linkShopError = linkShopError,
                            onClearLinkShopError = { viewModel.clearLinkShopError() },
                            onLinkExistingShop = { shopEmail, code, onSuccess ->
                                viewModel.linkToExistingShop(shopEmail, code, onSuccess)
                            }
                        )
                    }

                    is ProfileUiState.Error -> {
                        LaunchedEffect(Unit) {
                            activeAuroraMood = AuroraMood.EXPENSE
                        }
                        val ledgerColors = MaterialTheme.ledgerColors
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LiquidGlassCard(
                                cornerRadius = 24.dp,
                                accentTint = ledgerColors.expense,
                                modifier = Modifier
                                    .widthIn(max = 400.dp)
                                    .fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Account Sync Issue",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ledgerColors.expense
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = { viewModel.retryProfileSync() },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("retry_profile_sync_btn")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Retry Sync", fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = handleSignOut,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Text("Sign Out")
                                    }
                                }
                            }
                        }
                    }

                    is ProfileUiState.Ready -> {
                        LaunchedEffect(Unit) {
                            pendingPhoneNumber = ""
                        }
                        MainAppContent(
                            viewModel = viewModel,
                            userProfile = state.profile,
                            fallbackEmail = activeUser.email.orEmpty(),
                            isDarkTheme = isDarkTheme,
                            currentThemeMode = currentThemeMode,
                            onThemeModeSelected = onThemeModeSelected,
                            onMoodChanged = { activeAuroraMood = it },
                            onSignOut = handleSignOut
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    userProfile: UserProfileRecord,
    fallbackEmail: String,
    isDarkTheme: Boolean,
    currentThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onMoodChanged: (AuroraMood) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.CASHBOOK) }
    var cashbookSegment by remember { mutableStateOf(CashbookSegment.INCOME) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(selectedTab, cashbookSegment) {
        onMoodChanged(
            when (selectedTab) {
                NavigationTab.CASHBOOK -> if (cashbookSegment == CashbookSegment.INCOME) {
                    AuroraMood.INCOME
                } else {
                    AuroraMood.EXPENSE
                }
                NavigationTab.DUE_LEDGER -> AuroraMood.DUE
                NavigationTab.REPORTS -> AuroraMood.REPORTS
            }
        )
    }

    // Dialog & Sheet States
    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionDialogType by remember { mutableStateOf("INCOME") }
    var transactionToEdit by remember { mutableStateOf<TransactionRecord?>(null) }

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAccountProfileDialog by remember { mutableStateOf(false) }

    var customerForAdjustment by remember { mutableStateOf<CustomerRecord?>(null) }
    var adjustmentMode by remember { mutableStateOf(DueAdjustmentMode.ADD_ITEMS) }

    // Collected States from ViewModel
    val incomeTransactions by viewModel.incomeTransactions.collectAsStateWithLifecycle()
    val expenseTransactions by viewModel.expenseTransactions.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val selectedCalendar by viewModel.selectedCalendar.collectAsStateWithLifecycle()
    val monthlyTransactions by viewModel.monthlyTransactions.collectAsStateWithLifecycle()
    val selectedCustomerForHistory by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val historyEntries by viewModel.customerHistoryEntries.collectAsStateWithLifecycle()
    val isLinkedToOtherShop by viewModel.isLinkedToOtherShop.collectAsStateWithLifecycle()
    val activeLinkCode by viewModel.activeLinkCode.collectAsStateWithLifecycle()
    val isGeneratingLinkCode by viewModel.isGeneratingLinkCode.collectAsStateWithLifecycle()
    val isLinkingShop by viewModel.isLinkingShop.collectAsStateWithLifecycle()
    val linkShopError by viewModel.linkShopError.collectAsStateWithLifecycle()
    val linkedDevices by viewModel.linkedDevices.collectAsStateWithLifecycle()

    // Collect one-shot UI events (Success, Error, and Undo Delete)
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            snackbarHostState.currentSnackbarData?.dismiss()
            when (event) {
                is UiEvent.Success -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is UiEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }
                is UiEvent.TransactionDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Entry deleted",
                        actionLabel = "Undo",
                        withDismissAction = true,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreTransaction(event.deleted)
                    }
                }
            }
        }
    }

    val categoryShares = remember(monthlyTransactions) {
        val expenses = monthlyTransactions.filter { it.type == "EXPENSE" }
        viewModel.computeCategoryShares(expenses)
    }

    // Handle Back Button cleanly: Expense -> Income -> Exit, or other tabs -> Cashbook
    BackHandler(enabled = selectedTab != NavigationTab.CASHBOOK || cashbookSegment != CashbookSegment.INCOME) {
        if (selectedTab != NavigationTab.CASHBOOK) {
            selectedTab = NavigationTab.CASHBOOK
        } else {
            cashbookSegment = CashbookSegment.INCOME
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 152.dp)
                )
            },
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.16f else 0.10f),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.28f else 0.18f)
                                    ),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.Storefront,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = userProfile.displayName.ifBlank { "Hishab Khata" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val dynamicSubtitle = if (selectedTab == NavigationTab.CASHBOOK) {
                                        "Cashbook · ${cashbookSegment.label}"
                                    } else {
                                        selectedTab.subtitle
                                    }
                                    Text(
                                        text = if (isLinkedToOtherShop) {
                                            "Linked • $dynamicSubtitle"
                                        } else {
                                            dynamicSubtitle
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isLinkedToOtherShop) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        },
                        actions = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkTheme) 0.55f else 0.72f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDarkTheme) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.85f)
                                ),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable { showAccountProfileDialog = true }
                                    .testTag("account_profile_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountCircle,
                                        contentDescription = "Account & Settings",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkTheme) 0.58f else 0.70f),
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.testTag("app_top_bar")
                    )
                    HorizontalDivider(
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.65f),
                        thickness = 1.dp
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState.index > initialState.index) {
                            (slideInHorizontally(
                                animationSpec = tween(260, easing = FastOutSlowInEasing),
                                initialOffsetX = { fullWidth -> fullWidth / 5 }
                            ) + fadeIn(animationSpec = tween(240))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                                    targetOffsetX = { fullWidth -> -fullWidth / 5 }
                                ) + fadeOut(animationSpec = tween(150)))
                        } else {
                            (slideInHorizontally(
                                animationSpec = tween(260, easing = FastOutSlowInEasing),
                                initialOffsetX = { fullWidth -> -fullWidth / 5 }
                            ) + fadeIn(animationSpec = tween(240))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                                    targetOffsetX = { fullWidth -> fullWidth / 5 }
                                ) + fadeOut(animationSpec = tween(150)))
                        }
                    },
                    label = "tabTransition"
                ) { tab ->
                    when (tab) {
                        NavigationTab.CASHBOOK -> {
                            CashbookScreen(
                                selectedSegment = cashbookSegment,
                                onSegmentSelected = { cashbookSegment = it },
                                incomeTransactions = incomeTransactions,
                                expenseTransactions = expenseTransactions,
                                onEditTransaction = { tx ->
                                    transactionToEdit = tx
                                    transactionDialogType = tx.type
                                    showTransactionDialog = true
                                },
                                onDeleteTransaction = { tx ->
                                    viewModel.deleteTransaction(tx)
                                },
                                onDeleteAllForSegment = { segment ->
                                    viewModel.deleteAllTransactions(segment.typeKey)
                                }
                            )
                        }

                        NavigationTab.DUE_LEDGER -> {
                            DueLedgerScreen(
                                customers = customers,
                                onCustomerClicked = { customer ->
                                    viewModel.selectCustomerForHistory(customer.id)
                                },
                                onAddItemsClicked = { customer ->
                                    customerForAdjustment = customer
                                    adjustmentMode = DueAdjustmentMode.ADD_ITEMS
                                },
                                onCollectPaymentClicked = { customer ->
                                    customerForAdjustment = customer
                                    adjustmentMode = DueAdjustmentMode.COLLECT_PAYMENT
                                },
                                onAddNewCustomerClicked = {
                                    showAddCustomerDialog = true
                                }
                            )
                        }

                        NavigationTab.REPORTS -> {
                            ReportsScreen(
                                selectedCalendar = selectedCalendar,
                                transactions = monthlyTransactions,
                                categoryShares = categoryShares,
                                onPreviousMonth = { viewModel.changeMonth(-1) },
                                onNextMonth = { viewModel.changeMonth(1) },
                                shopName = userProfile.displayName.ifBlank { "Hishab Khata" }
                            )
                        }
                    }
                }
            }
        }

        // Unified Context-Aware Quick Add FAB (pinned cleanly above the floating dock)
        QuickAddFab(
            selectedTab = selectedTab,
            cashbookSegment = cashbookSegment,
            onAddIncome = {
                transactionToEdit = null
                transactionDialogType = "INCOME"
                showTransactionDialog = true
            },
            onAddExpense = {
                transactionToEdit = null
                transactionDialogType = "EXPENSE"
                showTransactionDialog = true
            },
            onAddCustomer = {
                showAddCustomerDialog = true
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        // Floating Liquid Glass Bottom Navigation Dock with Morphing Pill Indicator
        FloatingLiquidGlassDock(
            selectedTab = selectedTab,
            cashbookSegment = cashbookSegment,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Create or Edit Income / Expense Dialog (with selectable/editable date)
    if (showTransactionDialog) {
        val editing = transactionToEdit
        AddTransactionDialog(
            initialType = editing?.type ?: transactionDialogType,
            lockedType = editing?.type ?: transactionDialogType,
            existingTransaction = editing,
            onDismiss = {
                showTransactionDialog = false
                transactionToEdit = null
            },
            onSave = { type, category, amount, note, timestamp ->
                if (editing == null) {
                    viewModel.addTransaction(type, category, amount, note, timestamp)
                } else {
                    viewModel.updateTransaction(
                        transactionId = editing.id,
                        type = type,
                        category = category,
                        amount = amount,
                        note = note,
                        timestamp = timestamp
                    )
                }
            }
        )
    }

    // Add Customer Dialog (supports Opening Due OR Advance Deposit)
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onSave = { name, phone, address, initialAmount, initialNote, isInitialAdvance ->
                viewModel.createCustomer(
                    name = name,
                    phone = phone,
                    address = address,
                    initialAmount = initialAmount,
                    initialNote = initialNote,
                    isInitialAdvance = isInitialAdvance
                )
            }
        )
    }

    // Adjust Due Balance Dialog (Credit, Payment, or Advance — strictly Due Ledger only)
    customerForAdjustment?.let { customer ->
        AdjustDueDialog(
            customer = customer,
            mode = adjustmentMode,
            onDismiss = { customerForAdjustment = null },
            onAddCredit = { customerId, amount, itemDescription, timestamp ->
                viewModel.addCustomerCredit(customerId, amount, itemDescription, timestamp)
            },
            onRecordPayment = { customerId, amount, note, isAdvance, timestamp ->
                viewModel.recordCustomerPayment(customerId, amount, note, isAdvance, timestamp)
            }
        )
    }

    // Customer History Bottom Sheet (with Editable & Deletable Due / Advance Entries)
    selectedCustomerForHistory?.let { customer ->
        CustomerHistoryBottomSheet(
            customer = customer,
            entries = historyEntries,
            shopName = userProfile.displayName.ifBlank { "Hishab Khata" },
            onDismiss = { viewModel.selectCustomerForHistory(null) },
            onAddCreditClicked = {
                customerForAdjustment = customer
                adjustmentMode = DueAdjustmentMode.ADD_ITEMS
            },
            onCollectPaymentClicked = {
                customerForAdjustment = customer
                adjustmentMode = DueAdjustmentMode.COLLECT_PAYMENT
            },
            onAdvancePaymentClicked = {
                customerForAdjustment = customer
                adjustmentMode = DueAdjustmentMode.ADVANCE_PAYMENT
            },
            onEditCustomer = { id, name, phone, address ->
                viewModel.updateCustomerDetails(id, name, phone, address)
            },
            onDeleteCustomer = { custId ->
                viewModel.deleteCustomer(custId)
            },
            onEditDueEntry = { entryId, newType, newAmount, newDesc, timestamp ->
                viewModel.updateDueEntry(customer.id, entryId, newType, newAmount, newDesc, timestamp)
            },
            onDeleteDueEntry = { entryId ->
                viewModel.deleteDueEntry(customer.id, entryId)
            }
        )
    }

    // Account Profile, Multi-Device Access, Dark Mode Toggle & Sign Out Dialog
    if (showAccountProfileDialog) {
        AccountProfileDialog(
            profile = userProfile,
            fallbackEmail = fallbackEmail,
            currentThemeMode = currentThemeMode,
            onThemeModeSelected = onThemeModeSelected,
            onDismiss = { showAccountProfileDialog = false },
            onSaveProfile = { name, phone, email ->
                viewModel.saveProfile(name, phone, email)
            },
            onSignOut = onSignOut,
            isLinkedToOtherShop = isLinkedToOtherShop,
            activeLinkCode = activeLinkCode,
            isGeneratingLinkCode = isGeneratingLinkCode,
            isLinkingShop = isLinkingShop,
            linkShopError = linkShopError,
            linkedDevices = linkedDevices,
            onGenerateOrRotateLinkCode = { shopName, ownerEmail ->
                viewModel.generateOrRotateLinkCode(shopName, ownerEmail)
            },
            onLinkExistingShop = { shopEmail, code, onSuccess ->
                viewModel.linkToExistingShop(shopEmail, code, onSuccess)
            },
            onClearLinkShopError = { viewModel.clearLinkShopError() },
            onUnlinkDevice = { device ->
                viewModel.unlinkDevice(device)
            },
            onDisconnectFromLinkedShop = {
                viewModel.disconnectFromLinkedShop()
            }
        )
    }
}
