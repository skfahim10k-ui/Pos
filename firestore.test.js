const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate() {
  return new Date(Date.now() - 2000);
}

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

test("Unauthenticated user cannot read or write products", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("products").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Cross-user isolation: Bob cannot read or write Alice's shop or products", async () => {
  const ts = pastDate();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("products").doc("prod1").set({
      userId: ALICE_UID,
      name: "Miniket Rice 25kg",
      category: "Grocery",
      unit: "Bag",
      purchasePrice: 1650,
      sellPrice: 1820,
      stockQty: 20,
      lowStockThreshold: 5,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").doc("prod1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").doc("prod2").set({
    userId: ALICE_UID,
    name: "Injected",
    category: "Grocery",
    unit: "Pcs",
    purchasePrice: 10,
    sellPrice: 20,
    stockQty: 1,
    lowStockThreshold: 1,
    createdAt: ts,
    updatedAt: ts,
  }));
});

test("Alice can create, update, list, and delete her own product", async () => {
  const ts = pastDate();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const prodRef = aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_rice");

  await assertSucceeds(prodRef.set({
    userId: ALICE_UID,
    name: "Miniket Rice 25kg",
    nameBn: "মিনিকেট চাল ২৫ কেজি",
    barcode: "8901234567890",
    category: "Grocery",
    unit: "Bag",
    purchasePrice: 1650,
    sellPrice: 1820,
    stockQty: 25,
    lowStockThreshold: 5,
    createdAt: ts,
    updatedAt: ts,
  }));

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("products").get());

  await assertSucceeds(prodRef.update({
    stockQty: 23,
    updatedAt: new Date(Date.now() - 500),
  }));

  // Shadow update with ghost field must fail
  await assertFails(prodRef.update({
    stockQty: 22,
    ghostField: true,
    updatedAt: new Date(Date.now() - 400),
  }));

  // Negative stock must fail
  await assertFails(prodRef.update({
    stockQty: -1,
    updatedAt: new Date(Date.now() - 300),
  }));

  await assertSucceeds(prodRef.delete());
});

test("Alice can create shop profile, customer, supplier, sale, purchase, due payment, and expense", async () => {
  const ts = pastDate();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(userDoc.set({
    userId: ALICE_UID,
    businessName: "Fahim Siam Traders",
    preferredLanguage: "both",
    createdAt: ts,
    updatedAt: ts,
  }));

  await assertSucceeds(userDoc.collection("customers").doc("cust1").set({
    userId: ALICE_UID,
    name: "Karim Store",
    phone: "01711000000",
    totalPurchases: 5000,
    totalDue: 1200,
    createdAt: ts,
    updatedAt: ts,
  }));

  await assertSucceeds(userDoc.collection("suppliers").doc("sup1").set({
    userId: ALICE_UID,
    name: "Rahman Enterprise",
    companyName: "Rahman Agro",
    totalSupplied: 20000,
    totalDue: 4000,
    createdAt: ts,
    updatedAt: ts,
  }));

  await assertSucceeds(userDoc.collection("sales").doc("sale1").set({
    userId: ALICE_UID,
    memoNumber: "FST-001",
    customerId: "cust1",
    customerName: "Karim Store",
    items: [{ productId: "prod1", productName: "Rice", qty: 2, unitPrice: 1820, costPrice: 1650, lineTotal: 3640 }],
    subtotal: 3640,
    discount: 40,
    totalAmount: 3600,
    paidAmount: 3000,
    dueAmount: 600,
    totalCost: 3300,
    paymentMethod: "CASH",
    saleEpochMs: Date.now(),
    createdAt: ts,
    updatedAt: ts,
  }));

  await assertSucceeds(userDoc.collection("purchases").doc("pur1").set({
    userId: ALICE_UID,
    invoiceNumber: "PUR-001",
    supplierId: "sup1",
    supplierName: "Rahman Enterprise",
    items: [{ productId: "prod1", productName: "Rice", qty: 10, costPrice: 1650, lineTotal: 16500 }],
    totalAmount: 16500,
    paidAmount: 12500,
    dueAmount: 4000,
    paymentMethod: "BANK",
    purchaseEpochMs: Date.now(),
    createdAt: ts,
    updatedAt: ts,
  }));

  await assertSucceeds(userDoc.collection("due_payments").doc("pay1").set({
    userId: ALICE_UID,
    partyType: "CUSTOMER",
    partyId: "cust1",
    partyName: "Karim Store",
    amount: 500,
    paymentMethod: "BKASH",
    paymentEpochMs: Date.now(),
    createdAt: ts,
    updatedAt: ts,
  }));

  await assertSucceeds(userDoc.collection("expenses").doc("exp1").set({
    userId: ALICE_UID,
    title: "Shop Electricity Bill",
    category: "Utilities",
    amount: 1450,
    paymentMethod: "CASH",
    expenseEpochMs: Date.now(),
    createdAt: ts,
    updatedAt: ts,
  }));

  // Immutable createdAt cannot be altered on update
  await assertFails(userDoc.collection("expenses").doc("exp1").update({
    amount: 1500,
    createdAt: new Date(Date.now() - 500),
    updatedAt: new Date(Date.now() - 100),
  }));
});
