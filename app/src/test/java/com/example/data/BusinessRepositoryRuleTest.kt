package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BusinessRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun profileAndTransactions_authenticatedOwner_canCreateEditAndObserve() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = FirestoreBusinessRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveUserProfile(
                displayName = "Bismillah Traders",
                phoneNumber = "01711000111",
                email = ALICE_EMAIL
            )
        }

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(aliceUid).first { it != null }
        }
        assertNotNull(profile)
        assertEquals("01711000111", profile?.phoneNumber)

        val txId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addTransaction("INCOME", "Cash Sales", 1500.0, "Initial sale")
        }
        assertTrue(txId.isNotBlank())

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateTransaction(
                transactionId = txId,
                type = "INCOME",
                category = "Wholesale",
                amount = 2200.0,
                note = "Edited sale amount",
                timestamp = System.currentTimeMillis()
            )
        }

        val txList = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeTransactions(aliceUid).first { list ->
                list.any { it.id == txId && it.amount == 2200.0 }
            }
        }
        val edited = txList.first { it.id == txId }
        assertEquals("Wholesale", edited.category)
        assertEquals(2200.0, edited.amount, 0.001)
    }

    @Test
    fun customerDueEntries_editAndDelete_recalculatesBalancesWithoutCreatingIncome() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = FirestoreBusinessRepository(firestore, auth)

        val custId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createCustomer(
                name = "Karim Store",
                phone = "01811223344",
                address = "Market Road",
                initialAmount = 1000.0,
                initialNote = "Sugar 10kg"
            )
        }

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.recordCustomerPayment(custId, 400.0, "Partial cash")
        }

        // Verify due collection did NOT create an Income transaction
        val txs = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeTransactions(aliceUid).first()
        }
        assertTrue(txs.none { it.note.contains("Karim Store") })

        val entries = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeEntriesForCustomer(aliceUid, custId).first { it.size >= 2 }
        }
        val paymentEntry = entries.first { it.type == "PAYMENT_RECEIVED" }

        // Edit payment entry from 400 -> 600 and confirm customer balance recalculates automatically
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateDueEntry(
                customerId = custId,
                entryId = paymentEntry.id,
                type = "PAYMENT_RECEIVED",
                amount = 600.0,
                itemsDescription = "Corrected cash payment",
                timestamp = paymentEntry.timestamp
            )
        }

        val updatedCust = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCustomerById(aliceUid, custId).first { it != null && it.totalPaid == 600.0 }
        }
        assertEquals(1000.0, updatedCust!!.totalCredit, 0.001)
        assertEquals(600.0, updatedCust.totalPaid, 0.001)
        assertEquals(400.0, updatedCust.currentDue, 0.001)
        assertEquals(0.0, updatedCust.advanceBalance, 0.001)
    }

    @Test
    fun customerAdvancePayment_offsetsCreditAndTracksAdvanceBalance() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = FirestoreBusinessRepository(firestore, auth)

        val custId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createCustomer(
                name = "Sajib Enterprise",
                phone = "01911000222",
                address = "Station Road",
                initialAmount = 1000.0,
                initialNote = "Booking advance",
                isInitialAdvance = true
            )
        }

        val custWithAdvance = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCustomerById(aliceUid, custId).first { it != null }
        }
        assertEquals(0.0, custWithAdvance!!.currentDue, 0.001)
        assertEquals(1000.0, custWithAdvance.advanceBalance, 0.001)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addCustomerCredit(custId, 600.0, "Flour bags")
        }

        val afterGoods = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCustomerById(aliceUid, custId).first { it != null && it.totalCredit == 600.0 }
        }
        assertEquals(0.0, afterGoods!!.currentDue, 0.001)
        assertEquals(400.0, afterGoods.advanceBalance, 0.001)
    }

    @Test
    fun crossUserAccess_bobCannotReadAliceTransactions() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreBusinessRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addTransaction("EXPENSE", "Rent", 5000.0, "Shop rent")
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreBusinessRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeTransactions(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when Bob reads Alice's transactions")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun unauthenticatedUser_cannotObserveTransactions() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repo = FirestoreBusinessRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeTransactions(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun multiDeviceShopLink_rotateLinkSyncAndUnlink() = runBlocking {
        // 1. Alice creates her shop profile and generates a rotating 6-digit link code
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val businessRepo = FirestoreBusinessRepository(firestore, auth)
        val linkRepo = ShopLinkRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            businessRepo.saveUserProfile(
                displayName = "Bismillah Traders",
                phoneNumber = "+8801711000111",
                email = ALICE_EMAIL
            )
        }

        val firstCode = withTimeout(DEFAULT_TIMEOUT_MS) {
            linkRepo.generateOrRotateShopLinkCode(
                shopName = "Bismillah Traders",
                ownerEmail = ALICE_EMAIL
            )
        }
        val rotatedCode = withTimeout(DEFAULT_TIMEOUT_MS) {
            linkRepo.generateOrRotateShopLinkCode(
                shopName = "Bismillah Traders",
                ownerEmail = ALICE_EMAIL,
                previousTokenHash = firstCode.tokenHash
            )
        }

        // 2. Bob signs in on a second device and verifies old rotated code is rejected
        val bobUid = signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                linkRepo.linkDeviceByEmailAndCode(ALICE_EMAIL, firstCode.code, "Counter Phone")
            }
            fail("Expected old rotated code to be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("No active shop"))
        }

        // 3. Bob links with the active rotated code and records a sale on Alice's ledger
        val linkedShopName = withTimeout(DEFAULT_TIMEOUT_MS) {
            linkRepo.linkDeviceByEmailAndCode(ALICE_EMAIL, rotatedCode.code, "Counter Phone")
        }
        assertEquals("Bismillah Traders", linkedShopName)

        val bobTxId = withTimeout(DEFAULT_TIMEOUT_MS) {
            businessRepo.addTransaction(
                type = "INCOME",
                category = "Daily Sales",
                amount = 850.0,
                note = "Recorded on linked counter phone",
                ledgerOwnerId = aliceUid
            )
        }
        assertTrue(bobTxId.isNotBlank())

        val syncedTxs = withTimeout(FLOW_TIMEOUT_MS) {
            businessRepo.observeTransactions(aliceUid).first { list ->
                list.any { it.id == bobTxId && it.amount == 850.0 }
            }
        }
        assertTrue(syncedTxs.any { it.id == bobTxId })

        // 4. Alice sees Bob in linkedDevices and unlinks him
        signInTestUser(ALICE_EMAIL)
        val linkedDevices = withTimeout(FLOW_TIMEOUT_MS) {
            linkRepo.observeLinkedDevices(aliceUid).first { it.any { dev -> dev.deviceUid == bobUid } }
        }
        assertEquals("Counter Phone", linkedDevices.first { it.deviceUid == bobUid }.deviceName)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            linkRepo.unlinkDevice(bobUid)
        }

        // 5. Bob immediately loses access to Alice's ledger
        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                businessRepo.observeTransactions(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED immediately after owner unlinked Bob")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_ledger@test.com"
        const val BOB_EMAIL = "bob_ledger@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
