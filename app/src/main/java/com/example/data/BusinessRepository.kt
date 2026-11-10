package com.example.data

import com.example.data.entity.CustomerEntity
import com.example.data.entity.DueEntryEntity
import com.example.data.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

class BusinessRepository(private val database: AppDatabase) {

    private val transactionDao = database.transactionDao()
    private val customerDao = database.customerDao()
    private val dueEntryDao = database.dueEntryDao()

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    fun getCustomerById(customerId: Long): Flow<CustomerEntity?> {
        return customerDao.getCustomerById(customerId)
    }

    fun getEntriesForCustomer(customerId: Long): Flow<List<DueEntryEntity>> {
        return dueEntryDao.getEntriesForCustomer(customerId)
    }

    fun getTransactionsForMonth(year: Int, monthZeroIndexed: Int): Flow<List<TransactionEntity>> {
        val calendar = Calendar.getInstance()
        calendar.clear()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, monthZeroIndexed)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, maxDay)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endTime = calendar.timeInMillis

        return transactionDao.getTransactionsBetween(startTime, endTime)
    }

    suspend fun addTransaction(
        type: String,
        category: String,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val entity = TransactionEntity(
            type = type,
            category = category,
            amount = amount,
            note = note,
            timestamp = timestamp
        )
        transactionDao.insertTransaction(entity)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteAllTransactions() = withContext(Dispatchers.IO) {
        transactionDao.deleteAllTransactions()
    }

    suspend fun createCustomer(
        name: String,
        phone: String,
        address: String,
        initialDue: Double = 0.0,
        initialNote: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val customer = CustomerEntity(
            name = name,
            phone = phone,
            address = address,
            totalCredit = initialDue,
            totalPaid = 0.0,
            currentDue = initialDue,
            lastUpdated = now
        )
        val customerId = customerDao.insertCustomer(customer)
        if (initialDue > 0) {
            val entry = DueEntryEntity(
                customerId = customerId,
                type = "GOODS_TAKEN",
                amount = initialDue,
                itemsDescription = if (initialNote.isNotBlank()) initialNote else "Opening Due Balance",
                timestamp = now
            )
            dueEntryDao.insertDueEntry(entry)
        }
        customerId
    }

    suspend fun addCustomerCredit(
        customerId: Long,
        amount: Double,
        itemsDescription: String
    ) = withContext(Dispatchers.IO) {
        val customer = customerDao.getCustomerByIdDirect(customerId) ?: return@withContext
        val now = System.currentTimeMillis()

        val newTotalCredit = customer.totalCredit + amount
        val newCurrentDue = customer.currentDue + amount

        val updatedCustomer = customer.copy(
            totalCredit = newTotalCredit,
            currentDue = newCurrentDue,
            lastUpdated = now
        )
        customerDao.updateCustomer(updatedCustomer)

        val entry = DueEntryEntity(
            customerId = customerId,
            type = "GOODS_TAKEN",
            amount = amount,
            itemsDescription = itemsDescription,
            timestamp = now
        )
        dueEntryDao.insertDueEntry(entry)
    }

    suspend fun recordCustomerPayment(
        customerId: Long,
        amount: Double,
        paymentNote: String,
        addToBusinessIncome: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val customer = customerDao.getCustomerByIdDirect(customerId) ?: return@withContext
        val now = System.currentTimeMillis()

        val newTotalPaid = customer.totalPaid + amount
        val newCurrentDue = maxOf(0.0, customer.currentDue - amount)

        val updatedCustomer = customer.copy(
            totalPaid = newTotalPaid,
            currentDue = newCurrentDue,
            lastUpdated = now
        )
        customerDao.updateCustomer(updatedCustomer)

        val entry = DueEntryEntity(
            customerId = customerId,
            type = "PAYMENT_RECEIVED",
            amount = amount,
            itemsDescription = if (paymentNote.isNotBlank()) paymentNote else "Cash Payment",
            timestamp = now
        )
        dueEntryDao.insertDueEntry(entry)

        if (addToBusinessIncome) {
            val incomeTx = TransactionEntity(
                type = "INCOME",
                category = "Due Collection",
                amount = amount,
                note = "Due collection from ${customer.name}" + if (paymentNote.isNotBlank()) " ($paymentNote)" else "",
                timestamp = now
            )
            transactionDao.insertTransaction(incomeTx)
        }
    }

    suspend fun updateCustomerDetails(
        customerId: Long,
        name: String,
        phone: String,
        address: String
    ) = withContext(Dispatchers.IO) {
        val customer = customerDao.getCustomerByIdDirect(customerId) ?: return@withContext
        val updated = customer.copy(
            name = name,
            phone = phone,
            address = address,
            lastUpdated = System.currentTimeMillis()
        )
        customerDao.updateCustomer(updated)
    }

    suspend fun deleteCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        dueEntryDao.deleteEntriesForCustomer(customer.id)
        customerDao.deleteCustomer(customer)
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val txCount = transactionDao.getCount()
        val customerCount = customerDao.getCount()

        if (txCount == 0 && customerCount == 0) {
            val now = System.currentTimeMillis()
            val day = 86_400_000L

            // Seed Customers
            val c1 = CustomerEntity(
                name = "Md. Rafiqul Islam",
                phone = "01711-234567",
                address = "Shop #14, Bazar Road",
                totalCredit = 6500.0,
                totalPaid = 3000.0,
                currentDue = 3500.0,
                lastUpdated = now - day * 1
            )
            val id1 = customerDao.insertCustomer(c1)
            dueEntryDao.insertAll(
                listOf(
                    DueEntryEntity(customerId = id1, type = "GOODS_TAKEN", amount = 4500.0, itemsDescription = "50kg Miniket Rice & 5L Soybean Oil", timestamp = now - day * 5),
                    DueEntryEntity(customerId = id1, type = "PAYMENT_RECEIVED", amount = 3000.0, itemsDescription = "bKash Partial Payment", timestamp = now - day * 3),
                    DueEntryEntity(customerId = id1, type = "GOODS_TAKEN", amount = 2000.0, itemsDescription = "Flour, Sugar 10kg & Spices", timestamp = now - day * 1)
                )
            )

            val c2 = CustomerEntity(
                name = "Kamrul Hasan",
                phone = "01822-987654",
                address = "Station Road, Ward 3",
                totalCredit = 8200.0,
                totalPaid = 2200.0,
                currentDue = 6000.0,
                lastUpdated = now - day * 2
            )
            val id2 = customerDao.insertCustomer(c2)
            dueEntryDao.insertAll(
                listOf(
                    DueEntryEntity(customerId = id2, type = "GOODS_TAKEN", amount = 8200.0, itemsDescription = "Grocery wholesale bundle box", timestamp = now - day * 7),
                    DueEntryEntity(customerId = id2, type = "PAYMENT_RECEIVED", amount = 2200.0, itemsDescription = "Cash payment on delivery", timestamp = now - day * 2)
                )
            )

            val c3 = CustomerEntity(
                name = "Sumon Mia",
                phone = "01933-556677",
                address = "College Gate, East Para",
                totalCredit = 4000.0,
                totalPaid = 4000.0,
                currentDue = 0.0,
                lastUpdated = now - day * 3
            )
            val id3 = customerDao.insertCustomer(c3)
            dueEntryDao.insertAll(
                listOf(
                    DueEntryEntity(customerId = id3, type = "GOODS_TAKEN", amount = 4000.0, itemsDescription = "Tea bags & milk powder cartons", timestamp = now - day * 8),
                    DueEntryEntity(customerId = id3, type = "PAYMENT_RECEIVED", amount = 4000.0, itemsDescription = "Full payment via Cash", timestamp = now - day * 3)
                )
            )

            val c4 = CustomerEntity(
                name = "Abdur Rahim Store",
                phone = "01555-443322",
                address = "New Market, Block C",
                totalCredit = 14500.0,
                totalPaid = 5000.0,
                currentDue = 9500.0,
                lastUpdated = now
            )
            val id4 = customerDao.insertCustomer(c4)
            dueEntryDao.insertAll(
                listOf(
                    DueEntryEntity(customerId = id4, type = "GOODS_TAKEN", amount = 14500.0, itemsDescription = "Wholesale bulk pack (Dal, Oil, Spices)", timestamp = now - day * 4),
                    DueEntryEntity(customerId = id4, type = "PAYMENT_RECEIVED", amount = 5000.0, itemsDescription = "Bank Transfer Partial", timestamp = now - day * 1)
                )
            )

            // Clear all transactions per user request
            transactionDao.deleteAllTransactions()
        } else {
            // Also ensure any existing transactions are cleared per user request
            transactionDao.deleteAllTransactions()
        }
    }
}
