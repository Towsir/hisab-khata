const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const nowTs = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- UNAUTHENTICATED ACCESS TESTS ---

test("Unauthenticated user cannot read or write profiles, transactions, or customers", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("customers").get());
});

// --- USER PROFILE SECURITY & RED TEAM TESTS ---

test("Authenticated user can create and update own profile with phone & email", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(profileRef.set({
    userId: ALICE_UID,
    displayName: "Bismillah Store",
    phoneNumber: "01711000000",
    email: "alice@example.com",
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertSucceeds(profileRef.update({
    phoneNumber: "01811000000",
    updatedAt: nowTs(),
  }));
});

test("Shadow update with ghost field or cross-user profile read is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    displayName: "Bismillah Store",
    phoneNumber: "01711000000",
    email: "alice@example.com",
    isVerified: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    displayName: "Bismillah Store",
    phoneNumber: "01711000000",
    email: "alice@example.com",
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

// --- TRANSACTION CRUD & VALIDATION TESTS ---

test("Owner can create, edit, list, and delete Income and Expense transactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txCol = aliceDb.collection("users").doc(ALICE_UID).collection("transactions");
  const txRef = txCol.doc("tx_1");

  await assertSucceeds(txRef.set({
    userId: ALICE_UID,
    type: "INCOME",
    category: "Cash Sales",
    amount: 2500.5,
    note: "Morning sales",
    timestamp: 1700000000000,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertSucceeds(txRef.update({
    amount: 2750,
    note: "Corrected morning sales",
    updatedAt: nowTs(),
  }));

  await assertSucceeds(txCol.get());
  await assertSucceeds(txRef.delete());
});

test("Invalid transaction amount or cross-user transaction write is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Negative / zero amount rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_bad").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      category: "Rent",
      amount: 0,
      note: "",
      timestamp: 1700000000000,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Bob cannot write into Alice's transactions
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_spoof").set({
      userId: ALICE_UID,
      type: "INCOME",
      category: "Sales",
      amount: 100,
      note: "",
      timestamp: 1700000000000,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

// --- CUSTOMER & DUE ENTRY ATOMIC BATCH + EDITABILITY TESTS ---

test("Owner can batch-create customer + dueEntry, edit dueEntry, and recalculate balances", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const custRef = aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1");
  const entryRef = custRef.collection("dueEntries").doc("entry_1");

  const batch = aliceDb.batch();
  batch.set(custRef, {
    userId: ALICE_UID,
    name: "Rafiqul Islam",
    phone: "01711223344",
    address: "Bazar Road",
    totalCredit: 1200,
    totalPaid: 0,
    currentDue: 1200,
    lastUpdated: 1700000000000,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  });
  batch.set(entryRef, {
    userId: ALICE_UID,
    customerId: "cust_1",
    type: "GOODS_TAKEN",
    amount: 1200,
    itemsDescription: "Rice 25kg",
    timestamp: 1700000000000,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  });

  await assertSucceeds(batch.commit());

  // Edit due entry and update customer totals in a batch
  const editBatch = aliceDb.batch();
  editBatch.update(entryRef, {
    amount: 1500,
    itemsDescription: "Rice 30kg",
    updatedAt: nowTs(),
  });
  editBatch.update(custRef, {
    totalCredit: 1500,
    totalPaid: 0,
    currentDue: 1500,
    lastUpdated: 1700000005000,
    updatedAt: nowTs(),
  });
  await assertSucceeds(editBatch.commit());
});

test("Orphan dueEntry creation without existing customer fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orphanEntryRef = aliceDb
    .collection("users").doc(ALICE_UID)
    .collection("customers").doc("missing_cust")
    .collection("dueEntries").doc("entry_1");

  await assertFails(orphanEntryRef.set({
    userId: ALICE_UID,
    customerId: "missing_cust",
    type: "GOODS_TAKEN",
    amount: 500,
    itemsDescription: "Oil",
    timestamp: 1700000000000,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));
});

test("Owner can record ADVANCE_PAYMENT entry for a customer", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const custRef = aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_adv");
  const entryRef = custRef.collection("dueEntries").doc("entry_adv");

  const batch = aliceDb.batch();
  batch.set(custRef, {
    userId: ALICE_UID,
    name: "Sajib Traders",
    phone: "01911000222",
    address: "Station Road",
    totalCredit: 0,
    totalPaid: 1000,
    currentDue: 0,
    lastUpdated: 1700000000000,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  });
  batch.set(entryRef, {
    userId: ALICE_UID,
    customerId: "cust_adv",
    type: "ADVANCE_PAYMENT",
    amount: 1000,
    itemsDescription: "Advance deposit for rice",
    timestamp: 1700000000000,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  });
  await assertSucceeds(batch.commit());
});

test("Owner can update dueEntry date/timestamp and delete dueEntry with customer balance recalculation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const custRef = aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_date");
  const entryRef = custRef.collection("dueEntries").doc("entry_date");

  const createBatch = aliceDb.batch();
  createBatch.set(custRef, {
    userId: ALICE_UID,
    name: "Mizan Traders",
    phone: "01711334455",
    address: "Mirpur",
    totalCredit: 800,
    totalPaid: 0,
    currentDue: 800,
    lastUpdated: 1700000000000,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  });
  createBatch.set(entryRef, {
    userId: ALICE_UID,
    customerId: "cust_date",
    type: "GOODS_TAKEN",
    amount: 800,
    itemsDescription: "Flour 10kg",
    timestamp: 1700000000000,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  });
  await assertSucceeds(createBatch.commit());

  // Update entry timestamp and amount
  await assertSucceeds(entryRef.update({
    timestamp: 1699900000000,
    amount: 950,
    updatedAt: nowTs(),
  }));

  // Delete entry and recalculate customer totals to zero
  const deleteBatch = aliceDb.batch();
  deleteBatch.delete(entryRef);
  deleteBatch.update(custRef, {
    totalCredit: 0,
    totalPaid: 0,
    currentDue: 0,
    lastUpdated: 1700000010000,
    updatedAt: nowTs(),
  });
  await assertSucceeds(deleteBatch.commit());
});

test("Oversized or negative amounts are rejected by security rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_huge").set({
      userId: ALICE_UID,
      type: "INCOME",
      category: "Daily Sales",
      amount: 1000000001,
      note: "Too big",
      timestamp: 1700000000000,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_neg").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      category: "Shop Rent",
      amount: -50,
      note: "Negative",
      timestamp: 1700000000000,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

// --- MULTI-DEVICE EMAIL + 6-DIGIT LINK CODE & INSTANT REVOCATION TESTS ---

const CHARLIE_UID = "charlie_789";
const VALID_TOKEN_HASH = "a".repeat(64);
const WRONG_TOKEN_HASH = "b".repeat(64);
const EXPIRED_TOKEN_HASH = "c".repeat(64);

test("Shop links cannot be listed/enumerated and reject forged ownerId or invalid expiry", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Owner creates valid 15-minute link token
  await assertSucceeds(
    aliceDb.collection("shopLinks").doc(VALID_TOKEN_HASH).set({
      ownerId: ALICE_UID,
      shopName: "Bismillah Store",
      expiresAt: Date.now() + 15 * 60 * 1000,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Listing /shopLinks is strictly denied for everyone
  await assertFails(bobDb.collection("shopLinks").get());
  await assertFails(aliceDb.collection("shopLinks").get());

  // Forged ownerId is denied
  await assertFails(
    bobDb.collection("shopLinks").doc(WRONG_TOKEN_HASH).set({
      ownerId: ALICE_UID,
      shopName: "Spoof Store",
      expiresAt: Date.now() + 15 * 60 * 1000,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Already-expired link creation is denied
  await assertFails(
    aliceDb.collection("shopLinks").doc(EXPIRED_TOKEN_HASH).set({
      ownerId: ALICE_UID,
      shopName: "Bismillah Store",
      expiresAt: Date.now() - 60 * 1000,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Too-far-in-future expiry (> 1 hour) is denied
  await assertFails(
    aliceDb.collection("shopLinks").doc("d".repeat(64)).set({
      ownerId: ALICE_UID,
      shopName: "Bismillah Store",
      expiresAt: Date.now() + 24 * 60 * 60 * 1000,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Secondary device links with valid token hash, syncs owner ledger, and loses access immediately upon owner unlink", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Alice creates her profile and a valid 15-minute link token
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Bismillah Store",
      phoneNumber: "+8801711000000",
      email: "alice@example.com",
      activeLinkTokenHash: VALID_TOKEN_HASH,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("shopLinks").doc(VALID_TOKEN_HASH).set({
      ownerId: ALICE_UID,
      shopName: "Bismillah Store",
      expiresAt: Date.now() + 15 * 60 * 1000,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Before linking, Bob cannot access Alice's profile or transactions
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());

  // Bob cannot link using a wrong/missing token hash
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("linkedDevices").doc(BOB_UID).set({
      ownerId: ALICE_UID,
      deviceUid: BOB_UID,
      deviceName: "Pixel 8",
      deviceEmail: "bob@example.com",
      tokenHash: WRONG_TOKEN_HASH,
      linkedAt: Date.now(),
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Bob looks up the exact SHA-256 token hash and links his device
  await assertSucceeds(bobDb.collection("shopLinks").doc(VALID_TOKEN_HASH).get());
  await assertSucceeds(
    bobDb.collection("users").doc(ALICE_UID).collection("linkedDevices").doc(BOB_UID).set({
      ownerId: ALICE_UID,
      deviceUid: BOB_UID,
      deviceName: "Pixel 8",
      deviceEmail: "bob@example.com",
      tokenHash: VALID_TOKEN_HASH,
      linkedAt: Date.now(),
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Bob saves linkedOwnerId on his own profile
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Linked Device",
      phoneNumber: "",
      email: "bob@example.com",
      linkedOwnerId: ALICE_UID,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Linked device Bob can read Alice's profile, list/write transactions & customers, but CANNOT edit Alice's profile
  await assertSucceeds(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked Shop",
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_from_bob").set({
      userId: ALICE_UID,
      type: "INCOME",
      category: "Cash Sales",
      amount: 1200,
      note: "Recorded from counter phone",
      timestamp: 1700000000000,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());

  // Unlinked user Charlie still has zero access
  await assertFails(charlieDb.collection("users").doc(ALICE_UID).collection("linkedDevices").get());
  await assertFails(charlieDb.collection("users").doc(ALICE_UID).collection("transactions").get());

  // Alice lists linked devices and unlinks Bob -> instant revocation
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("linkedDevices").get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("linkedDevices").doc(BOB_UID).delete()
  );

  // Bob's access to Alice's profile & transactions is immediately denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_after_unlink").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      category: "Rent",
      amount: 500,
      note: "Should fail",
      timestamp: 1700000000000,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Expired shop link token cannot authorize new devices and linked device can self-disconnect", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Seed an expired shopLink directly via security-rules-disabled admin context
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("shopLinks").doc(EXPIRED_TOKEN_HASH).set({
      ownerId: ALICE_UID,
      shopName: "Bismillah Store",
      expiresAt: Date.now() - 5000,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
  });

  // Bob fails to link with expired token
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("linkedDevices").doc(BOB_UID).set({
      ownerId: ALICE_UID,
      deviceUid: BOB_UID,
      deviceName: "Pixel 8",
      deviceEmail: "bob@example.com",
      tokenHash: EXPIRED_TOKEN_HASH,
      linkedAt: Date.now(),
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Now create valid link, link Bob, and verify Bob can self-disconnect
  await assertSucceeds(
    aliceDb.collection("shopLinks").doc(VALID_TOKEN_HASH).set({
      ownerId: ALICE_UID,
      shopName: "Bismillah Store",
      expiresAt: Date.now() + 15 * 60 * 1000,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(ALICE_UID).collection("linkedDevices").doc(BOB_UID).set({
      ownerId: ALICE_UID,
      deviceUid: BOB_UID,
      deviceName: "Pixel 8",
      deviceEmail: "bob@example.com",
      tokenHash: VALID_TOKEN_HASH,
      linkedAt: Date.now(),
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(ALICE_UID).collection("linkedDevices").doc(BOB_UID).delete()
  );
});


