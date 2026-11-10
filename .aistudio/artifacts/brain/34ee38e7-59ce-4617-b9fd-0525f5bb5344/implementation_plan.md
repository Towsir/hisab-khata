# Business Ledger & Due Manager (হিসাব খাতা)

A complete business financial management and credit ledger Android application designed for store owners and small businesses. Enables real-time income and expense tracking in Bangladeshi Taka (৳ BDT), a quick-entry customer due balance ledger ("Khata") with instant balance adjustments, automated monthly financial reports with visual cash flow charts, and deep business reasoning powered by Gemini 4 Argon with High Thinking mode.

## User Review & Critical Decisions

> [!IMPORTANT]
> Based on your selections, the application will use **BDT (৳)** as the primary currency, implement a **quick-entry customer due ledger** with instant payment and item additions, generate **visual cash flow charts categorized by expense**, and incorporate **Gemini 4 Argon with High Thinking (`ThinkingLevel.HIGH`)** for complex business financial diagnostics.

- **Confirmed Currency**: Bangladeshi Taka (BDT / ৳) formatted with locale-aware number representations.
- **Confirmed Due Ledger Style**: Quick-entry due balance list where each customer card displays total credit items taken, total paid, and remaining due balance, complete with quick "+ More Items" and "− Record Payment" adjustment dialogs.
- **Confirmed Reporting Format**: Visual category-based cash flow charts, monthly profit & loss breakdown, and cash-in vs cash-out metrics.
- **AI Thinking Engine**: Gemini 4 Argon configured with `ThinkingLevel.HIGH` for deep strategic business analysis, credit risk assessment, and cash flow optimization without token clipping.

---

## 1. Overview & Core Concept

Small business owners and shopkeepers require a rapid, frictionless tool to record daily sales income, operating expenses, and customer credit ("Baki / Khata") balances.

### Key Capabilities:
1. **Cash Flow & Expense Tracking**:
   - Quick logging of Income (Sales, Service, Custom) and Expenses (Inventory, Rent, Utilities, Transport, Wages, Misc).
   - Real-time dashboard showing today's balance, monthly net profit, and total outstanding dues.
2. **Customer Due Ledger (বাকির খাতা)**:
   - Dedicated customer cards with instant balance visibility.
   - One-tap "+ Add Due / Goods Taken" to increase due amount with optional item description.
   - One-tap "− Collect Payment" to reduce outstanding balance with history logging.
   - Status indicators: Fully Paid, Partial Due, and High Due alerts.
3. **Automated Monthly Reports & Cash Flow Charts**:
   - Monthly breakdown selector (e.g., November 2026, October 2026).
   - Visual category distribution donut/bar visualization for expenses.
   - Daily income vs expense trend comparisons.
4. **AI Financial Advisor with High Thinking Mode**:
   - Deep reasoning via `gemini-4-argon` and `ThinkingLevel.HIGH`.
   - Comprehensive audit of spending patterns, margin evaluation, overdue debt recovery tactics, and cash reserves forecasting.

---

## 2. User Experience & Visual Design

### Aesthetic & Styling:
- **Theme Direction**: Professional Modern FinTech / Modern Bengali Business aesthetic. Clean emerald and deep slate tones symbolizing financial growth, clarity, and trust.
- **Color Palette**:
  - Primary: Deep Emerald (`#0A5C36` / `#00875A`)
  - Accent / Profit: Vivid Mint (`#00C853` / `#22C55E`)
  - Due / Alert: Warm Amber / Terracotta (`#D9381E` / `#E65100`)
  - Background: Crisp Slate Neutral (`#F8FAFC` light / `#0F172A` dark)
  - Surface: Elevated pure white / dark slate containers with subtle borders.
- **Typography & Scale**: Bold monetary figures, crisp tabular figures for calculations, clean Bengali / English dual-support labels.

### Key User Flows:
1. **Dashboard Tab**:
   - Financial overview cards: *Total Balance*, *This Month's Income*, *This Month's Expenses*, *Total Pending Dues*.
   - Quick Action floating bar: "+ New Income", "+ New Expense", "+ Add Customer Due".
   - Recent transactions stream with filter chips (All, Income, Expense, Dues).
2. **Customer Due Ledger Tab**:
   - Search bar with instant customer lookup by name or phone.
   - Summary banner: Total Outstanding Dues across all customers.
   - Quick action buttons on each customer card:
     - `+ Add Due`: Dialog to add item amount and description (updates total & due).
     - `✓ Collect Payment`: Dialog to record payment received (updates paid & balance).
     - Full transaction ledger drawer showing date, item description, and balance change history.
3. **Monthly Reports Tab**:
   - Month-Year selector carousel.
   - Visual cash flow breakdown chart: interactive Compose Canvas bar and proportion chart grouping expenses by category (Inventory, Rent, Bills, Supplies, Personal).
   - Income vs Expense comparative metrics with net profit margin percentage.
   - Export/Share ready financial statement summary.
4. **AI Business Advisor Tab (High Thinking)**:
   - "Analyze My Business" trigger button.
   - Thinking indicator indicating deep multi-step financial computation.
   - Structured diagnostic output: Cash Flow Health, Expense Reduction Opportunities, Credit Default Risk Rating, and Actionable Growth Plan.

---

## 3. Key Product Decisions & Trade-Offs

- **Local Persistence via Room (Offline-First)**:
  - *Chosen*: Room Database with reactive Kotlin Coroutine `Flow` queries.
  - *Why*: Shopkeepers frequently operate in locations with intermittent internet. Transactions and customer due updates must be instantaneous, reliable, and 100% offline-capable.
- **Editable Running Balances with Transaction Ledger**:
  - *Chosen*: Double-entry style customer ledger where every credit addition and payment is recorded as an immutable log entry that recalculates the customer's aggregate balance.
  - *Why*: Ensures transparency so the merchant can show the customer exactly when each product was taken and when each partial payment was made, while keeping the UI as a simple quick-entry balance card.
- **High Thinking Gemini 4 Argon**:
  - *Chosen*: `gemini-4-argon` with `ThinkingLevel.HIGH` without `maxOutputTokens`.
  - *Why*: Direct compliance with requirements. Handles complex financial multi-category reasoning, debtor risk calculation, and inventory cycle recommendations.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                       Jetpack Compose UI                    │
│  ┌───────────────┐ ┌────────────────┐ ┌───────────────────┐ │
│  │ DashboardView │ │ DueLedgerView  │ │ ReportsChartView  │ │
│  └───────┬───────┘ └───────┬────────┘ └─────────┬─────────┘ │
│          │                 │                    │           │
│  ┌───────▼─────────────────▼────────────────────▼─────────┐ │
│  │                     BusinessViewModel                  │ │
│  └───────┬──────────────────────────────┬─────────────────┘ │
└──────────┼──────────────────────────────┼───────────────────┘
           │                              │
┌──────────▼───────────────┐   ┌──────────▼─────────────────┐
│     BusinessRepository   │   │   GeminiAdvisorService     │
│  ┌─────────────────────┐ │   │  (gemini-4-argon with      │
│  │ Room Database       │ │   │   ThinkingLevel.HIGH)      │
│  │ - TransactionEntity │ │   └────────────────────────────┘
│  │ - CustomerEntity    │ │
│  │ - DueEntryEntity    │ │
│  └─────────────────────┘ │
└──────────────────────────┘
```

### Data Models:
1. **`TransactionEntity`**:
   - `id: Long`, `type: TransactionType` (INCOME, EXPENSE), `category: String`, `amount: Double`, `note: String`, `date: Long`.
2. **`CustomerEntity`**:
   - `id: Long`, `name: String`, `phone: String`, `totalCredit: Double`, `totalPaid: Double`, `currentDue: Double`, `lastUpdated: Long`.
3. **`DueEntryEntity`**:
   - `id: Long`, `customerId: Long`, `entryType: DueType` (GOODS_TAKEN, PAYMENT_RECEIVED), `amount: Double`, `itemsDescription: String`, `date: Long`.

### AI Integration Plan:
- Utilizes `Firebase.ai.generativeModel("gemini-4-argon", generationConfig = generationConfig { thinkingConfig = thinkingConfig { thinkingLevel = ThinkingLevel.HIGH } })`.
- Transmits anonymized aggregate monthly totals, category shares, and outstanding customer debt aging to produce deep strategic advice.
