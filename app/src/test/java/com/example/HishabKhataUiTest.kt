package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.example.data.ThemeMode
import com.example.data.model.CustomerRecord
import com.example.data.model.GeneratedLinkCode
import com.example.data.model.LinkedDeviceRecord
import com.example.data.model.TransactionRecord
import com.example.data.model.UserProfileRecord
import com.example.ui.components.AddTransactionDialogContent
import com.example.ui.components.AdjustDueDialogContent
import com.example.ui.components.ConnectExistingShopDialogContent
import com.example.ui.components.DueAdjustmentMode
import com.example.ui.components.EmailLoginDialogContent
import com.example.ui.components.FloatingLiquidGlassDock
import com.example.ui.components.MultiDeviceAccessSection
import com.example.ui.components.PhoneAuthDialogContent
import com.example.ui.components.PhoneAuthMode
import com.example.ui.components.QuickAddFab
import com.example.ui.screens.AccountProfileDialogContent
import com.example.ui.screens.AuthScreenContent
import com.example.ui.screens.CashbookScreen
import com.example.ui.screens.CashbookSegment
import com.example.ui.screens.DueLedgerScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.TransactionLedgerScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class HishabKhataUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testFloatingLiquidGlassDockSwitchesTabs() {
        var selected = NavigationTab.CASHBOOK
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                FloatingLiquidGlassDock(
                    selectedTab = selected,
                    onTabSelected = { selected = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_due_ledger").performClick()
        composeTestRule.waitForIdle()
        assertEquals(NavigationTab.DUE_LEDGER, selected)

        composeTestRule.onNodeWithTag("nav_reports").performClick()
        composeTestRule.waitForIdle()
        assertEquals(NavigationTab.REPORTS, selected)

        composeTestRule.onNodeWithTag("nav_cashbook").performClick()
        composeTestRule.waitForIdle()
        assertEquals(NavigationTab.CASHBOOK, selected)
    }

    @Test
    fun testCashbookScreenKeepsIncomeAndExpenseStrictlySeparate() {
        val now = System.currentTimeMillis()
        val incomeList = listOf(
            TransactionRecord(id = "inc_1", type = "INCOME", category = "Daily Sales", amount = 2500.0, note = "Counter", timestamp = now)
        )
        val expenseList = listOf(
            TransactionRecord(id = "exp_1", type = "EXPENSE", category = "Shop Rent", amount = 900.0, note = "Advance", timestamp = now)
        )
        var activeSegment by mutableStateOf(CashbookSegment.INCOME)

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                CashbookScreen(
                    selectedSegment = activeSegment,
                    onSegmentSelected = { activeSegment = it },
                    incomeTransactions = incomeList,
                    expenseTransactions = expenseList,
                    onEditTransaction = {},
                    onDeleteTransaction = {},
                    onDeleteAllForSegment = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Single-glance hero card shows both summaries, while active feed shows ONLY Income
        composeTestRule.onNodeWithTag("cashbook_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("transaction_card_inc_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("transaction_card_exp_1").assertDoesNotExist()

        // Switching to Expense displays ONLY Expense entries
        composeTestRule.onNodeWithTag("cashbook_tab_expense").performClick()
        composeTestRule.waitForIdle()
        assertEquals(CashbookSegment.EXPENSE, activeSegment)
        composeTestRule.onNodeWithTag("transaction_card_exp_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("transaction_card_inc_1").assertDoesNotExist()

        // Tapping the Income mini-tile jumps straight back to Income
        composeTestRule.onNodeWithTag("cashbook_summary_income").performClick()
        composeTestRule.waitForIdle()
        assertEquals(CashbookSegment.INCOME, activeSegment)
        composeTestRule.onNodeWithTag("transaction_card_inc_1").assertIsDisplayed()
    }

    @Test
    fun testTransactionLedgerSearchZeroMatchAndResetFilters() {
        val sampleTransactions = listOf(
            TransactionRecord(
                id = "tx1",
                type = "INCOME",
                category = "Daily Sales",
                amount = 1500.0,
                note = "Morning counter",
                timestamp = System.currentTimeMillis()
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                TransactionLedgerScreen(
                    ledgerType = "INCOME",
                    transactions = sampleTransactions,
                    onAddClicked = {},
                    onEditTransaction = {},
                    onDeleteTransaction = {},
                    onDeleteAll = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("transaction_card_tx1").assertIsDisplayed()

        // Search input starts collapsed to keep ledger tidy
        composeTestRule.onNodeWithTag("income_search_input").assertDoesNotExist()
        composeTestRule.onNodeWithTag("toggle_tx_search_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("income_search_input").performTextInput("zzzz_nomatch")
        composeTestRule.waitForIdle()

        // Collapsing while filtered surfaces the active filter banner
        composeTestRule.onNodeWithTag("toggle_tx_search_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("active_tx_filter_banner").assertIsDisplayed()

        composeTestRule.onNodeWithText("No matching entries").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reset_tx_filters_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("transaction_card_tx1").assertIsDisplayed()
    }

    @Test
    fun testAddTransactionDialogQuickPresetsAndValidation() {
        var savedAmount = 0.0
        var savedType = ""

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                AddTransactionDialogContent(
                    initialType = "INCOME",
                    lockedType = "INCOME",
                    onDismiss = {},
                    onSave = { type, _, amount, _, _ ->
                        savedType = type
                        savedAmount = amount
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Clicking save with empty amount shows validation error
        composeTestRule.onNodeWithTag("save_transaction_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Please enter an amount greater than 0").assertIsDisplayed()

        // Tap +100 and +500 quick presets -> 600
        composeTestRule.onNodeWithTag("quick_amount_100").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("quick_amount_500").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("save_transaction_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("INCOME", savedType)
        assertEquals(600.0, savedAmount, 0.001)
    }

    @Test
    fun testDueLedgerFiltersAndZeroMatchReset() {
        val customers = listOf(
            CustomerRecord(
                id = "c1",
                name = "Karim Store",
                phone = "01711111111",
                totalCredit = 2000.0,
                totalPaid = 500.0,
                currentDue = 1500.0
            ),
            CustomerRecord(
                id = "c2",
                name = "Rahim Traders",
                phone = "01822222222",
                totalCredit = 500.0,
                totalPaid = 1200.0,
                currentDue = 0.0
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                DueLedgerScreen(
                    customers = customers,
                    onCustomerClicked = {},
                    onAddItemsClicked = {},
                    onCollectPaymentClicked = {},
                    onAddNewCustomerClicked = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Search & filter tray starts collapsed
        composeTestRule.onNodeWithTag("customer_search_input").assertDoesNotExist()
        composeTestRule.onNodeWithTag("toggle_due_search_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("filter_advance_cust_chip").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("customer_card_c2").assertIsDisplayed()

        composeTestRule.onNodeWithTag("customer_search_input").performTextInput("NoSuchShop")
        composeTestRule.waitForIdle()

        // Collapsing while filtered shows active filter banner
        composeTestRule.onNodeWithTag("toggle_due_search_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("active_due_filter_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("No matching customers").assertIsDisplayed()

        composeTestRule.onNodeWithTag("reset_due_filters_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("customer_card_c1").assertIsDisplayed()
    }

    @Test
    fun testAdjustDueDialogFullDuePresetAndTimestampCallback() {
        val customer = CustomerRecord(
            id = "c_pay",
            name = "Jamal Enterprise",
            totalCredit = 2500.0,
            totalPaid = 1000.0,
            currentDue = 1500.0
        )
        var paidCustomerId = ""
        var paidAmount = 0.0
        var paidTimestamp = 0L

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                AdjustDueDialogContent(
                    customer = customer,
                    mode = DueAdjustmentMode.COLLECT_PAYMENT,
                    onDismiss = {},
                    onAddCredit = { _, _, _, _ -> },
                    onRecordPayment = { id, amount, _, _, ts ->
                        paidCustomerId = id
                        paidAmount = amount
                        paidTimestamp = ts
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Tap one-tap Full Due chip -> populates 1500 and projects Settled (৳0)
        composeTestRule.onNodeWithTag("preset_full_due_chip").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("projected_balance_value").assertTextContains("৳0")
        composeTestRule.onNodeWithTag("adjust_due_date_picker_btn").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("confirm_adjust_due_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("c_pay", paidCustomerId)
        assertEquals(1500.0, paidAmount, 0.001)
        assertTrue(paidTimestamp > 0L)
    }

    @Test
    fun testReportsScreenNegativeNetProfitAndMonthNavigation() {
        val cal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 15)
        }
        val transactions = listOf(
            TransactionRecord(id = "t1", type = "INCOME", category = "Daily Sales", amount = 500.0, timestamp = cal.timeInMillis),
            TransactionRecord(id = "t2", type = "EXPENSE", category = "Shop Rent", amount = 1000.0, timestamp = cal.timeInMillis)
        )
        var prevClicks = 0
        var nextClicks = 0

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                ReportsScreen(
                    selectedCalendar = cal,
                    transactions = transactions,
                    categoryShares = emptyList(),
                    onPreviousMonth = { prevClicks++ },
                    onNextMonth = { nextClicks++ }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verifies negative BDT formatting uses "−৳500" (never "৳-500")
        composeTestRule.onNodeWithText("−৳500").assertIsDisplayed()

        composeTestRule.onNodeWithTag("prev_month_btn").performClick()
        composeTestRule.onNodeWithTag("next_month_btn").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, prevClicks)
        assertEquals(1, nextClicks)
    }

    @Test
    fun testProfileSetupValidatesBangladeshiPhoneNumber() {
        var savedName = ""
        var savedPhone = ""

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ProfileSetupScreen(
                    initialName = "Bismillah Store",
                    initialPhone = "",
                    initialEmail = "owner@shop.com",
                    isSaving = false,
                    errorMessage = null,
                    onSaveProfile = { name, phone, _ ->
                        savedName = name
                        savedPhone = phone
                    },
                    onSignOut = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Invalid BD mobile prefix
        composeTestRule.onNodeWithTag("profile_phone_input").performTextInput("01112345678")
        composeTestRule.onNodeWithTag("save_profile_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Enter a valid Bangladeshi mobile (e.g. 01712345678)").assertIsDisplayed()
        assertEquals("", savedPhone)

        // Valid BD mobile normalizes to +8801712345678
        composeTestRule.onNodeWithTag("profile_phone_input").performTextReplacement("01712345678")
        composeTestRule.onNodeWithTag("save_profile_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("Bismillah Store", savedName)
        assertEquals("+8801712345678", savedPhone)
    }

    @Test
    fun testEmailLoginDialogValidatesAndSubmitsEmailAndSixDigitCode() {
        var linkedEmail = ""
        var linkedCode = ""
        var googleEmailClicked = false

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                EmailLoginDialogContent(
                    isLoading = false,
                    onDismiss = {},
                    onContinueWithGoogleEmail = { googleEmailClicked = true },
                    onContinueWithEmailAndCode = { email, code ->
                        linkedEmail = email
                        linkedCode = code
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Invalid email shows validation error
        composeTestRule.onNodeWithTag("email_link_email_input").performTextInput("not-an-email")
        composeTestRule.onNodeWithTag("email_link_submit_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Please enter a valid existing shop email address.").assertIsDisplayed()

        // Valid email + 6-digit code invokes callback normalized
        composeTestRule.onNodeWithTag("email_link_email_input").performTextReplacement("Owner@Shop.COM")
        composeTestRule.onNodeWithTag("email_link_code_input").performTextInput("482915")
        composeTestRule.onNodeWithTag("email_link_submit_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("owner@shop.com", linkedEmail)
        assertEquals("482915", linkedCode)

        // Switching to Google Email tab triggers Google Email sign-in callback
        composeTestRule.onNodeWithTag("email_login_tab_google_email").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("email_login_google_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(googleEmailClicked)
    }

    @Test
    fun testConnectExistingShopDialogAndProfileSetupEntry() {
        var connectedEmail = ""
        var connectedCode = ""

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ConnectExistingShopDialogContent(
                    isLinking = false,
                    errorMessage = null,
                    onDismiss = {},
                    onSubmit = { email, code ->
                        connectedEmail = email
                        connectedCode = code
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("connect_existing_shop_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("connect_shop_email_input").performTextInput("owner@bismillah.com")
        composeTestRule.onNodeWithTag("connect_shop_code_input").performTextInput("739201")
        composeTestRule.onNodeWithTag("connect_shop_submit_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("owner@bismillah.com", connectedEmail)
        assertEquals("739201", connectedCode)
    }

    @Test
    fun testProfileSetupDisplaysExistingEmailLinkShortcut() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ProfileSetupScreen(
                    initialName = "",
                    initialPhone = "",
                    initialEmail = "cashier@shop.com",
                    isSaving = false,
                    errorMessage = null,
                    onSaveProfile = { _, _, _ -> },
                    onSignOut = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_setup_link_email_btn").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testMultiDeviceAccessSectionDisplaysCodeAndUnlinksDevice() {
        var rotatedCount = 0
        var unlinkedUid = ""
        val activeCode = GeneratedLinkCode(
            code = "482915",
            email = "owner@bismillah.com",
            tokenHash = "a".repeat(64),
            expiresAt = System.currentTimeMillis() + 10 * 60 * 1000L
        )
        val device = LinkedDeviceRecord(
            id = "dev_2",
            ownerId = "owner_1",
            deviceUid = "dev_2",
            deviceName = "Samsung Galaxy A55",
            deviceEmail = "cashier@bismillah.com",
            tokenHash = "a".repeat(64),
            linkedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                MultiDeviceAccessSection(
                    shopName = "Bismillah Store",
                    ownerEmail = "owner@bismillah.com",
                    isLinkedToOtherShop = false,
                    activeLinkCode = activeCode,
                    isGeneratingCode = false,
                    linkedDevices = listOf(device),
                    onGenerateOrRotateCode = { rotatedCount++ },
                    onUnlinkDevice = { unlinkedUid = it.deviceUid },
                    onOpenConnectShopDialog = {},
                    onDisconnectFromLinkedShop = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("active_link_code_value").assertTextContains("482 915")
        composeTestRule.onNodeWithTag("rotate_link_code_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, rotatedCount)

        composeTestRule.onNodeWithTag("unlink_device_btn_dev_2").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("confirm_unlink_device_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals("dev_2", unlinkedUid)
    }

    @Test
    fun testQuickAddFabAdaptsPerTabAndOpensReportsQuickMenu() {
        var currentTab by mutableStateOf(NavigationTab.CASHBOOK)
        var currentSegment by mutableStateOf(CashbookSegment.INCOME)
        var incomeAdds = 0
        var expenseAdds = 0
        var customerAdds = 0

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                QuickAddFab(
                    selectedTab = currentTab,
                    cashbookSegment = currentSegment,
                    onAddIncome = { incomeAdds++ },
                    onAddExpense = { expenseAdds++ },
                    onAddCustomer = { customerAdds++ }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Cashbook (Income) triggers onAddIncome
        composeTestRule.onNodeWithTag("quick_add_fab").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, incomeAdds)

        // Cashbook (Expense) triggers onAddExpense
        currentSegment = CashbookSegment.EXPENSE
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quick_add_fab").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, expenseAdds)

        // Due Ledger tab triggers onAddCustomer
        currentTab = NavigationTab.DUE_LEDGER
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quick_add_fab").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, customerAdds)

        // Reports tab opens Quick Add menu with all 3 actions
        currentTab = NavigationTab.REPORTS
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quick_add_fab").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quick_add_income_item").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(2, incomeAdds)
    }

    @Test
    fun testAccountProfileDialogSwitchesBetweenShopAndDevicesTabs() {
        val profile = UserProfileRecord(
            id = "u1",
            userId = "u1",
            displayName = "Bismillah Store",
            phoneNumber = "+8801712345678",
            email = "owner@bismillah.com"
        )
        var selectedTheme = ThemeMode.SYSTEM

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                AccountProfileDialogContent(
                    profile = profile,
                    fallbackEmail = "owner@bismillah.com",
                    currentThemeMode = selectedTheme,
                    onThemeModeSelected = { selectedTheme = it },
                    onDismiss = {},
                    onSaveProfile = { _, _, _ -> },
                    onSignOut = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Default tab shows Shop & Theme controls
        composeTestRule.onNodeWithTag("theme_mode_dark").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(ThemeMode.DARK, selectedTheme)
        composeTestRule.onNodeWithTag("account_name_input").assertIsDisplayed()

        // Switch to Devices & Sync tab
        composeTestRule.onNodeWithTag("settings_tab_devices").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("multi_device_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("account_name_input").assertDoesNotExist()
    }

    @Test
    fun testAuthScreenContentDisplaysMinimalCardAndAllThreeActions() {
        var phoneClicks = 0
        var googleClicks = 0
        var createPhoneClicks = 0
        var errorText by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                AuthScreenContent(
                    isLoading = false,
                    isCheckingSession = false,
                    infoMessage = null,
                    errorMessage = errorText,
                    onPhoneLoginClicked = { phoneClicks++ },
                    onGoogleLoginClicked = { googleClicks++ },
                    onCreateAccountWithPhoneClicked = { createPhoneClicks++ }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Logo title and all three sign-in actions are displayed cleanly (no email login button)
        composeTestRule.onNodeWithText("Hishab Khata").assertIsDisplayed()
        composeTestRule.onNodeWithTag("phone_login_button").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("create_account_phone_button").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("login_with_email_button").assertDoesNotExist()
        composeTestRule.waitForIdle()

        assertEquals(1, phoneClicks)
        assertEquals(1, googleClicks)
        assertEquals(1, createPhoneClicks)

        // Zero extra info text in resting state
        composeTestRule.onNodeWithText("Welcome").assertDoesNotExist()
        composeTestRule.onNodeWithTag("auth_error_banner").assertDoesNotExist()

        // Error banner appears only on demand
        errorText = "Unable to sign in right now"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("auth_error_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unable to sign in right now").assertIsDisplayed()
    }

    @Test
    fun testPhoneAuthDialogContentShowsFormattedPreviewAndTriggersContinue() {
        var phoneInput by mutableStateOf("")
        var errorMessage by mutableStateOf<String?>(null)
        var continueClicks = 0

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                PhoneAuthDialogContent(
                    mode = PhoneAuthMode.CREATE_ACCOUNT,
                    phoneInput = phoneInput,
                    isBusy = false,
                    errorMessage = errorMessage,
                    onPhoneInputChange = {
                        phoneInput = it
                        errorMessage = null
                    },
                    onContinueClicked = {
                        if (phoneInput.isBlank()) {
                            errorMessage = "Enter a valid Bangladeshi mobile number (e.g. 01712-345678)."
                        } else {
                            continueClicks++
                        }
                    },
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Invalid empty submission surfaces error banner
        composeTestRule.onNodeWithTag("phone_auth_continue_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("phone_auth_error_banner").assertIsDisplayed()
        assertEquals(0, continueClicks)

        // Entering a valid Bangladeshi mobile reveals smart +880 preview and allows continuing
        composeTestRule.onNodeWithTag("phone_auth_number_input").performTextInput("01712345678")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("phone_auth_formatted_preview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("phone_auth_continue_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, continueClicks)
    }
}
