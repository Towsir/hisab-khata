package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.BusinessRepository
import com.example.data.entity.CustomerEntity
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.components.AddCustomerDialog
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.AdjustDueDialog
import com.example.ui.components.CustomerHistoryBottomSheet
import com.example.ui.components.DueAdjustmentMode
import com.example.ui.screens.AiAdvisorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DueLedgerScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = BusinessRepository(database)
        MainViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Mandatory App Check configuration with intent
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

enum class NavigationTab(val label: String, val subtitle: String, val index: Int) {
    DASHBOARD("Overview", "Business Accounts & Cash Flow", 0),
    DUE_LEDGER("Due Ledger", "Customer Credit & Khata", 1),
    REPORTS("Reports", "Monthly Statements & Charts", 2),
    AI_ADVISOR("AI Advisor", "Gemini 4 Argon High Thinking", 3)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }

    // Dialog & Sheet States
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var transactionDialogType by remember { mutableStateOf("INCOME") }

    var showAddCustomerDialog by remember { mutableStateOf(false) }

    var customerForAdjustment by remember { mutableStateOf<CustomerEntity?>(null) }
    var adjustmentMode by remember { mutableStateOf(DueAdjustmentMode.ADD_ITEMS) }

    // Collected States from ViewModel
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val selectedCalendar by viewModel.selectedCalendar.collectAsStateWithLifecycle()
    val monthlyTransactions by viewModel.monthlyTransactions.collectAsStateWithLifecycle()
    val selectedCustomerForHistory by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val historyEntries by viewModel.customerHistoryEntries.collectAsStateWithLifecycle()
    val aiAdviceState by viewModel.aiAdviceState.collectAsStateWithLifecycle()

    val categoryShares = remember(monthlyTransactions) {
        val expenses = monthlyTransactions.filter { it.type == "EXPENSE" }
        viewModel.computeCategoryShares(expenses)
    }

    // Handle Back Button when on secondary tab
    BackHandler(enabled = selectedTab != NavigationTab.DASHBOARD) {
        selectedTab = NavigationTab.DASHBOARD
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Storefront,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Hishab Khata (হিসাব খাতা)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = selectedTab.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("app_top_bar")
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("app_bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = selectedTab == NavigationTab.DASHBOARD,
                        onClick = { selectedTab = NavigationTab.DASHBOARD },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == NavigationTab.DASHBOARD) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Overview"
                            )
                        },
                        label = { Text("Overview", fontSize = 11.sp, fontWeight = if (selectedTab == NavigationTab.DASHBOARD) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_overview")
                    )

                    NavigationBarItem(
                        selected = selectedTab == NavigationTab.DUE_LEDGER,
                        onClick = { selectedTab = NavigationTab.DUE_LEDGER },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == NavigationTab.DUE_LEDGER) Icons.Filled.Book else Icons.Outlined.Book,
                                contentDescription = "Due Ledger"
                            )
                        },
                        label = { Text("Due Ledger", fontSize = 11.sp, fontWeight = if (selectedTab == NavigationTab.DUE_LEDGER) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_due_ledger")
                    )

                    NavigationBarItem(
                        selected = selectedTab == NavigationTab.REPORTS,
                        onClick = { selectedTab = NavigationTab.REPORTS },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == NavigationTab.REPORTS) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                                contentDescription = "Reports"
                            )
                        },
                        label = { Text("Reports", fontSize = 11.sp, fontWeight = if (selectedTab == NavigationTab.REPORTS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_reports")
                    )

                    NavigationBarItem(
                        selected = selectedTab == NavigationTab.AI_ADVISOR,
                        onClick = { selectedTab = NavigationTab.AI_ADVISOR },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == NavigationTab.AI_ADVISOR) Icons.Filled.Psychology else Icons.Outlined.Psychology,
                                contentDescription = "AI Advisor"
                            )
                        },
                        label = { Text("AI Advisor", fontSize = 11.sp, fontWeight = if (selectedTab == NavigationTab.AI_ADVISOR) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_ai_advisor")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Smooth Animated Tab Transition
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState.index > initialState.index) {
                        (slideInHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> fullWidth / 4 }
                        ) + fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                    targetOffsetX = { fullWidth -> -fullWidth / 4 }
                                ) + fadeOut(animationSpec = tween(200)))
                    } else {
                        (slideInHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> -fullWidth / 4 }
                        ) + fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                    targetOffsetX = { fullWidth -> fullWidth / 4 }
                                ) + fadeOut(animationSpec = tween(200)))
                    }
                },
                label = "tabTransition"
            ) { tab ->
                when (tab) {
                    NavigationTab.DASHBOARD -> {
                        DashboardScreen(
                            transactions = transactions,
                            customers = customers,
                            onAddIncomeClicked = {
                                transactionDialogType = "INCOME"
                                showAddTransactionDialog = true
                            },
                            onAddExpenseClicked = {
                                transactionDialogType = "EXPENSE"
                                showAddTransactionDialog = true
                            },
                            onAddCustomerClicked = {
                                showAddCustomerDialog = true
                            },
                            onDeleteTransaction = { tx ->
                                viewModel.deleteTransaction(tx)
                            },
                            onDeleteAllTransactions = {
                                viewModel.deleteAllTransactions()
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
                            onNextMonth = { viewModel.changeMonth(1) }
                        )
                    }

                    NavigationTab.AI_ADVISOR -> {
                        AiAdvisorScreen(
                            state = aiAdviceState,
                            onRequestAudit = { prompt ->
                                viewModel.requestFinancialAudit(prompt)
                            },
                            onClearAdvice = {
                                viewModel.clearAiAdvice()
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Income / Expense Dialog
    if (showAddTransactionDialog) {
        AddTransactionDialog(
            initialType = transactionDialogType,
            onDismiss = { showAddTransactionDialog = false },
            onSave = { type, category, amount, note ->
                viewModel.addTransaction(type, category, amount, note)
            }
        )
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onSave = { name, phone, address, initialDue, initialNote ->
                viewModel.createCustomer(name, phone, address, initialDue, initialNote)
            }
        )
    }

    // Adjust Due Balance Dialog (Add Items or Collect Payment)
    customerForAdjustment?.let { customer ->
        AdjustDueDialog(
            customer = customer,
            mode = adjustmentMode,
            onDismiss = { customerForAdjustment = null },
            onAddCredit = { customerId, amount, itemDescription ->
                viewModel.addCustomerCredit(customerId, amount, itemDescription)
            },
            onRecordPayment = { customerId, amount, note, addToIncome ->
                viewModel.recordCustomerPayment(customerId, amount, note, addToIncome)
            }
        )
    }

    // Customer History Bottom Sheet
    selectedCustomerForHistory?.let { customer ->
        CustomerHistoryBottomSheet(
            customer = customer,
            entries = historyEntries,
            onDismiss = { viewModel.selectCustomerForHistory(null) },
            onAddCreditClicked = {
                customerForAdjustment = customer
                adjustmentMode = DueAdjustmentMode.ADD_ITEMS
            },
            onCollectPaymentClicked = {
                customerForAdjustment = customer
                adjustmentMode = DueAdjustmentMode.COLLECT_PAYMENT
            },
            onEditCustomer = { id, name, phone, address ->
                viewModel.updateCustomerDetails(id, name, phone, address)
            },
            onDeleteCustomer = { cust ->
                viewModel.deleteCustomer(cust)
            }
        )
    }
}
