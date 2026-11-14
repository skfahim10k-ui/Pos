package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Customer
import com.example.data.model.DuePayment
import com.example.data.model.Expense
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseLineItem
import com.example.data.model.Sale
import com.example.data.model.SaleLineItem
import com.example.data.model.ShopProfile
import com.example.data.model.Supplier
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.remote.unwrapFirestoreException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.max

class PosRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    // --- Real-time Observers ---

    fun observeShopProfile(userId: String): Flow<ShopProfile?> = flow {
        val docRef = userDoc(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            ShopProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError ?: error, OperationType.GET, docRef.path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeProducts(userId: String): Flow<List<Product>> = observeSubcollection(
        userId = userId,
        collection = "products",
        clazz = Product::class.java
    ) { list -> list.sortedBy { it.name.lowercase() } }

    fun observeCustomers(userId: String): Flow<List<Customer>> = observeSubcollection(
        userId = userId,
        collection = "customers",
        clazz = Customer::class.java
    ) { list -> list.sortedBy { it.name.lowercase() } }

    fun observeSuppliers(userId: String): Flow<List<Supplier>> = observeSubcollection(
        userId = userId,
        collection = "suppliers",
        clazz = Supplier::class.java
    ) { list -> list.sortedBy { it.name.lowercase() } }

    fun observeSales(userId: String): Flow<List<Sale>> = observeSubcollection(
        userId = userId,
        collection = "sales",
        clazz = Sale::class.java
    ) { list -> list.sortedByDescending { it.saleEpochMs } }

    fun observePurchases(userId: String): Flow<List<Purchase>> = observeSubcollection(
        userId = userId,
        collection = "purchases",
        clazz = Purchase::class.java
    ) { list -> list.sortedByDescending { it.purchaseEpochMs } }

    fun observeDuePayments(userId: String): Flow<List<DuePayment>> = observeSubcollection(
        userId = userId,
        collection = "due_payments",
        clazz = DuePayment::class.java
    ) { list -> list.sortedByDescending { it.paymentEpochMs } }

    fun observeExpenses(userId: String): Flow<List<Expense>> = observeSubcollection(
        userId = userId,
        collection = "expenses",
        clazz = Expense::class.java
    ) { list -> list.sortedByDescending { it.expenseEpochMs } }

    private fun <T : Any> observeSubcollection(
        userId: String,
        collection: String,
        clazz: Class<T>,
        sorter: (List<T>) -> List<T>
    ): Flow<List<T>> = flow {
        val colRef = userDoc(userId).collection(collection)
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    sorter(
                        snapshot.toObjects(
                            clazz,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    )
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path, auth)
                    }
                    throw error
                }
        )
    }

    // --- Shop Profile ---

    suspend fun ensureShopProfile(
        ownerName: String = "",
        ownerEmail: String = ""
    ) {
        val uid = requireUserId()
        val docRef = userDoc(uid)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "businessName" to "Fahim Siam Traders",
                    "ownerName" to ownerName.take(100),
                    "ownerEmail" to ownerEmail.take(150),
                    "phone" to "",
                    "address" to "",
                    "preferredLanguage" to "both",
                    "receiptFooterBn" to "ধন্যবাদ, আবার আসবেন!",
                    "receiptFooterEn" to "Thank you for your business!",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateShopProfile(profile: ShopProfile) {
        val uid = requireUserId()
        val docRef = userDoc(uid)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "businessName" to profile.businessName.ifBlank { "Fahim Siam Traders" }.take(100),
                    "ownerName" to profile.ownerName.take(100),
                    "ownerEmail" to profile.ownerEmail.take(150),
                    "phone" to profile.phone.take(30),
                    "address" to profile.address.take(250),
                    "preferredLanguage" to profile.preferredLanguage.ifBlank { "both" },
                    "receiptFooterBn" to profile.receiptFooterBn.take(200),
                    "receiptFooterEn" to profile.receiptFooterEn.take(200),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "businessName" to profile.businessName.ifBlank { "Fahim Siam Traders" }.take(100),
                    "ownerName" to profile.ownerName.take(100),
                    "ownerEmail" to profile.ownerEmail.take(150),
                    "phone" to profile.phone.take(30),
                    "address" to profile.address.take(250),
                    "preferredLanguage" to profile.preferredLanguage.ifBlank { "both" },
                    "receiptFooterBn" to profile.receiptFooterBn.take(200),
                    "receiptFooterEn" to profile.receiptFooterEn.take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    // --- Products ---

    suspend fun saveProduct(product: Product): String {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("products")
        val isNew = product.id.isBlank()
        val docRef = if (isNew) colRef.document() else colRef.document(product.id)
        try {
            if (isNew) {
                val payload = mapOf(
                    "userId" to uid,
                    "name" to product.name.trim().take(120),
                    "nameBn" to product.nameBn.trim().take(120),
                    "sku" to product.sku.trim().take(64),
                    "barcode" to product.barcode.trim().take(100),
                    "qrCode" to product.qrCode.trim().take(200),
                    "category" to product.category.ifBlank { "General" }.trim().take(60),
                    "unit" to product.unit.ifBlank { "Pcs" }.trim().take(20),
                    "purchasePrice" to max(0.0, product.purchasePrice),
                    "sellPrice" to max(0.0, product.sellPrice),
                    "stockQty" to max(0.0, product.stockQty),
                    "lowStockThreshold" to max(0.0, product.lowStockThreshold),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "name" to product.name.trim().take(120),
                    "nameBn" to product.nameBn.trim().take(120),
                    "sku" to product.sku.trim().take(64),
                    "barcode" to product.barcode.trim().take(100),
                    "qrCode" to product.qrCode.trim().take(200),
                    "category" to product.category.ifBlank { "General" }.trim().take(60),
                    "unit" to product.unit.ifBlank { "Pcs" }.trim().take(20),
                    "purchasePrice" to max(0.0, product.purchasePrice),
                    "sellPrice" to max(0.0, product.sellPrice),
                    "stockQty" to max(0.0, product.stockQty),
                    "lowStockThreshold" to max(0.0, product.lowStockThreshold),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun adjustProductStock(productId: String, newStockQty: Double) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("products").document(productId)
        try {
            docRef.update(
                mapOf(
                    "stockQty" to max(0.0, newStockQty),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteProduct(productId: String) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("products").document(productId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    // --- Customers ---

    suspend fun saveCustomer(customer: Customer): String {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("customers")
        val isNew = customer.id.isBlank()
        val docRef = if (isNew) colRef.document() else colRef.document(customer.id)
        try {
            if (isNew) {
                val payload = mapOf(
                    "userId" to uid,
                    "name" to customer.name.trim().take(100),
                    "phone" to customer.phone.trim().take(30),
                    "address" to customer.address.trim().take(200),
                    "totalPurchases" to max(0.0, customer.totalPurchases),
                    "totalDue" to max(0.0, customer.totalDue),
                    "notes" to customer.notes.trim().take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "name" to customer.name.trim().take(100),
                    "phone" to customer.phone.trim().take(30),
                    "address" to customer.address.trim().take(200),
                    "totalPurchases" to max(0.0, customer.totalPurchases),
                    "totalDue" to max(0.0, customer.totalDue),
                    "notes" to customer.notes.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteCustomer(customerId: String) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("customers").document(customerId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    // --- Suppliers ---

    suspend fun saveSupplier(supplier: Supplier): String {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("suppliers")
        val isNew = supplier.id.isBlank()
        val docRef = if (isNew) colRef.document() else colRef.document(supplier.id)
        try {
            if (isNew) {
                val payload = mapOf(
                    "userId" to uid,
                    "name" to supplier.name.trim().take(100),
                    "companyName" to supplier.companyName.trim().take(120),
                    "phone" to supplier.phone.trim().take(30),
                    "address" to supplier.address.trim().take(200),
                    "totalSupplied" to max(0.0, supplier.totalSupplied),
                    "totalDue" to max(0.0, supplier.totalDue),
                    "notes" to supplier.notes.trim().take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "name" to supplier.name.trim().take(100),
                    "companyName" to supplier.companyName.trim().take(120),
                    "phone" to supplier.phone.trim().take(30),
                    "address" to supplier.address.trim().take(200),
                    "totalSupplied" to max(0.0, supplier.totalSupplied),
                    "totalDue" to max(0.0, supplier.totalDue),
                    "notes" to supplier.notes.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteSupplier(supplierId: String) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("suppliers").document(supplierId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    // --- POS Sale Checkout (Atomic Batch: Sale + Stock Decrement + Customer Ledger) ---

    suspend fun recordSale(
        sale: Sale,
        currentProducts: List<Product>,
        customer: Customer?
    ): String {
        val uid = requireUserId()
        require(sale.items.isNotEmpty() && sale.items.size <= 50) { "Sale must contain 1..50 items" }
        val saleRef = userDoc(uid).collection("sales").document()
        try {
            val batch = db.batch()
            val itemMaps = sale.items.map { item ->
                mapOf(
                    "productId" to item.productId,
                    "productName" to item.productName,
                    "productNameBn" to item.productNameBn,
                    "unit" to item.unit,
                    "qty" to item.qty,
                    "unitPrice" to item.unitPrice,
                    "costPrice" to item.costPrice,
                    "lineTotal" to item.lineTotal
                )
            }
            val salePayload = mapOf(
                "userId" to uid,
                "memoNumber" to sale.memoNumber.take(40),
                "customerId" to (customer?.id ?: sale.customerId).take(128),
                "customerName" to (customer?.name ?: sale.customerName.ifBlank { "Walk-in Customer" }).take(100),
                "customerPhone" to (customer?.phone ?: sale.customerPhone).take(30),
                "items" to itemMaps,
                "subtotal" to max(0.0, sale.subtotal),
                "discount" to max(0.0, sale.discount),
                "totalAmount" to max(0.0, sale.totalAmount),
                "paidAmount" to max(0.0, sale.paidAmount),
                "dueAmount" to max(0.0, sale.dueAmount),
                "totalCost" to max(0.0, sale.totalCost),
                "paymentMethod" to sale.paymentMethod,
                "saleEpochMs" to sale.saleEpochMs,
                "note" to sale.note.take(300),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(saleRef, salePayload)

            // Decrement stock for each matched product
            val productMap = currentProducts.associateBy { it.id }
            sale.items.groupBy { it.productId }.forEach { (productId, lines) ->
                val existingProduct = productMap[productId]
                if (existingProduct != null && productId.isNotBlank()) {
                    val soldQty = lines.sumOf { it.qty }
                    val newStock = max(0.0, existingProduct.stockQty - soldQty)
                    val prodRef = userDoc(uid).collection("products").document(productId)
                    batch.update(
                        prodRef,
                        mapOf(
                            "stockQty" to newStock,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            // Update customer purchases and due
            if (customer != null && customer.id.isNotBlank()) {
                val custRef = userDoc(uid).collection("customers").document(customer.id)
                batch.update(
                    custRef,
                    mapOf(
                        "totalPurchases" to max(0.0, customer.totalPurchases + sale.totalAmount),
                        "totalDue" to max(0.0, customer.totalDue + sale.dueAmount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            return saleRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, saleRef.path, auth)
            throw e
        }
    }

    // --- Purchase Recording (Atomic Batch: Purchase + Stock Increment + Supplier Ledger) ---

    suspend fun recordPurchase(
        purchase: Purchase,
        currentProducts: List<Product>,
        supplier: Supplier?
    ): String {
        val uid = requireUserId()
        require(purchase.items.isNotEmpty() && purchase.items.size <= 50) { "Purchase must contain 1..50 items" }
        val purchaseRef = userDoc(uid).collection("purchases").document()
        try {
            val batch = db.batch()
            val itemMaps = purchase.items.map { item ->
                mapOf(
                    "productId" to item.productId,
                    "productName" to item.productName,
                    "unit" to item.unit,
                    "qty" to item.qty,
                    "costPrice" to item.costPrice,
                    "lineTotal" to item.lineTotal
                )
            }
            val payload = mapOf(
                "userId" to uid,
                "invoiceNumber" to purchase.invoiceNumber.take(40),
                "supplierId" to (supplier?.id ?: purchase.supplierId).take(128),
                "supplierName" to (supplier?.name ?: purchase.supplierName.ifBlank { "General Supplier" }).take(100),
                "items" to itemMaps,
                "totalAmount" to max(0.0, purchase.totalAmount),
                "paidAmount" to max(0.0, purchase.paidAmount),
                "dueAmount" to max(0.0, purchase.dueAmount),
                "paymentMethod" to purchase.paymentMethod,
                "purchaseEpochMs" to purchase.purchaseEpochMs,
                "note" to purchase.note.take(300),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(purchaseRef, payload)

            val productMap = currentProducts.associateBy { it.id }
            purchase.items.groupBy { it.productId }.forEach { (productId, lines) ->
                val existingProduct = productMap[productId]
                if (existingProduct != null && productId.isNotBlank()) {
                    val boughtQty = lines.sumOf { it.qty }
                    val latestCost = lines.last().costPrice
                    val newStock = max(0.0, existingProduct.stockQty + boughtQty)
                    val prodRef = userDoc(uid).collection("products").document(productId)
                    batch.update(
                        prodRef,
                        mapOf(
                            "stockQty" to newStock,
                            "purchasePrice" to max(0.0, latestCost),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            if (supplier != null && supplier.id.isNotBlank()) {
                val supRef = userDoc(uid).collection("suppliers").document(supplier.id)
                batch.update(
                    supRef,
                    mapOf(
                        "totalSupplied" to max(0.0, supplier.totalSupplied + purchase.totalAmount),
                        "totalDue" to max(0.0, supplier.totalDue + purchase.dueAmount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            return purchaseRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, purchaseRef.path, auth)
            throw e
        }
    }

    // --- Due Payments (Customer Receivable Collection / Supplier Payable Payment) ---

    suspend fun recordCustomerDueCollection(
        customer: Customer,
        amount: Double,
        paymentMethod: String,
        note: String = ""
    ): String {
        val uid = requireUserId()
        require(amount > 0.0) { "Payment amount must be positive" }
        val paymentRef = userDoc(uid).collection("due_payments").document()
        val custRef = userDoc(uid).collection("customers").document(customer.id)
        try {
            val batch = db.batch()
            batch.set(
                paymentRef,
                mapOf(
                    "userId" to uid,
                    "partyType" to "CUSTOMER",
                    "partyId" to customer.id,
                    "partyName" to customer.name.take(100),
                    "amount" to amount,
                    "paymentMethod" to paymentMethod,
                    "paymentEpochMs" to System.currentTimeMillis(),
                    "note" to note.take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                custRef,
                mapOf(
                    "totalPurchases" to max(0.0, customer.totalPurchases),
                    "totalDue" to max(0.0, customer.totalDue - amount),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            return paymentRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, paymentRef.path, auth)
            throw e
        }
    }

    suspend fun recordSupplierDuePayment(
        supplier: Supplier,
        amount: Double,
        paymentMethod: String,
        note: String = ""
    ): String {
        val uid = requireUserId()
        require(amount > 0.0) { "Payment amount must be positive" }
        val paymentRef = userDoc(uid).collection("due_payments").document()
        val supRef = userDoc(uid).collection("suppliers").document(supplier.id)
        try {
            val batch = db.batch()
            batch.set(
                paymentRef,
                mapOf(
                    "userId" to uid,
                    "partyType" to "SUPPLIER",
                    "partyId" to supplier.id,
                    "partyName" to supplier.name.take(100),
                    "amount" to amount,
                    "paymentMethod" to paymentMethod,
                    "paymentEpochMs" to System.currentTimeMillis(),
                    "note" to note.take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                supRef,
                mapOf(
                    "totalSupplied" to max(0.0, supplier.totalSupplied),
                    "totalDue" to max(0.0, supplier.totalDue - amount),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            return paymentRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, paymentRef.path, auth)
            throw e
        }
    }

    // --- Expenses ---

    suspend fun saveExpense(expense: Expense): String {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("expenses")
        val isNew = expense.id.isBlank()
        val docRef = if (isNew) colRef.document() else colRef.document(expense.id)
        try {
            if (isNew) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to expense.title.trim().take(120),
                        "category" to expense.category.ifBlank { "General" }.trim().take(60),
                        "amount" to max(0.01, expense.amount),
                        "paymentMethod" to expense.paymentMethod,
                        "expenseEpochMs" to expense.expenseEpochMs,
                        "note" to expense.note.trim().take(300),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to expense.title.trim().take(120),
                        "category" to expense.category.ifBlank { "General" }.trim().take(60),
                        "amount" to max(0.01, expense.amount),
                        "paymentMethod" to expense.paymentMethod,
                        "expenseEpochMs" to expense.expenseEpochMs,
                        "note" to expense.note.trim().take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteExpense(expenseId: String) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("expenses").document(expenseId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    // --- Backup Restore Helper ---

    suspend fun restoreBackupBatch(
        products: List<Product>,
        customers: List<Customer>,
        suppliers: List<Supplier>,
        sales: List<Sale>,
        purchases: List<Purchase>,
        duePayments: List<DuePayment>,
        expenses: List<Expense>,
        shopProfile: ShopProfile?
    ) {
        val uid = requireUserId()
        try {
            shopProfile?.let { updateShopProfile(it) }
            val ops = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()

            products.forEach { p ->
                val ref = if (p.id.isNotBlank()) userDoc(uid).collection("products").document(p.id)
                else userDoc(uid).collection("products").document()
                ops += ref to mapOf(
                    "userId" to uid,
                    "name" to p.name.take(120),
                    "nameBn" to p.nameBn.take(120),
                    "sku" to p.sku.take(64),
                    "barcode" to p.barcode.take(100),
                    "qrCode" to p.qrCode.take(200),
                    "category" to p.category.ifBlank { "General" }.take(60),
                    "unit" to p.unit.ifBlank { "Pcs" }.take(20),
                    "purchasePrice" to max(0.0, p.purchasePrice),
                    "sellPrice" to max(0.0, p.sellPrice),
                    "stockQty" to max(0.0, p.stockQty),
                    "lowStockThreshold" to max(0.0, p.lowStockThreshold),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            customers.forEach { c ->
                val ref = if (c.id.isNotBlank()) userDoc(uid).collection("customers").document(c.id)
                else userDoc(uid).collection("customers").document()
                ops += ref to mapOf(
                    "userId" to uid,
                    "name" to c.name.take(100),
                    "phone" to c.phone.take(30),
                    "address" to c.address.take(200),
                    "totalPurchases" to max(0.0, c.totalPurchases),
                    "totalDue" to max(0.0, c.totalDue),
                    "notes" to c.notes.take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            suppliers.forEach { s ->
                val ref = if (s.id.isNotBlank()) userDoc(uid).collection("suppliers").document(s.id)
                else userDoc(uid).collection("suppliers").document()
                ops += ref to mapOf(
                    "userId" to uid,
                    "name" to s.name.take(100),
                    "companyName" to s.companyName.take(120),
                    "phone" to s.phone.take(30),
                    "address" to s.address.take(200),
                    "totalSupplied" to max(0.0, s.totalSupplied),
                    "totalDue" to max(0.0, s.totalDue),
                    "notes" to s.notes.take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            sales.filter { it.items.isNotEmpty() }.forEach { sale ->
                val ref = if (sale.id.isNotBlank()) userDoc(uid).collection("sales").document(sale.id)
                else userDoc(uid).collection("sales").document()
                ops += ref to mapOf(
                    "userId" to uid,
                    "memoNumber" to sale.memoNumber.take(40),
                    "customerId" to sale.customerId.take(128),
                    "customerName" to sale.customerName.ifBlank { "Walk-in Customer" }.take(100),
                    "customerPhone" to sale.customerPhone.take(30),
                    "items" to sale.items.take(50).map { item ->
                        mapOf(
                            "productId" to item.productId,
                            "productName" to item.productName,
                            "productNameBn" to item.productNameBn,
                            "unit" to item.unit,
                            "qty" to item.qty,
                            "unitPrice" to item.unitPrice,
                            "costPrice" to item.costPrice,
                            "lineTotal" to item.lineTotal
                        )
                    },
                    "subtotal" to max(0.0, sale.subtotal),
                    "discount" to max(0.0, sale.discount),
                    "totalAmount" to max(0.0, sale.totalAmount),
                    "paidAmount" to max(0.0, sale.paidAmount),
                    "dueAmount" to max(0.0, sale.dueAmount),
                    "totalCost" to max(0.0, sale.totalCost),
                    "paymentMethod" to sale.paymentMethod,
                    "saleEpochMs" to sale.saleEpochMs,
                    "note" to sale.note.take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            purchases.filter { it.items.isNotEmpty() }.forEach { pur ->
                val ref = if (pur.id.isNotBlank()) userDoc(uid).collection("purchases").document(pur.id)
                else userDoc(uid).collection("purchases").document()
                ops += ref to mapOf(
                    "userId" to uid,
                    "invoiceNumber" to pur.invoiceNumber.take(40),
                    "supplierId" to pur.supplierId.take(128),
                    "supplierName" to pur.supplierName.ifBlank { "General Supplier" }.take(100),
                    "items" to pur.items.take(50).map { item ->
                        mapOf(
                            "productId" to item.productId,
                            "productName" to item.productName,
                            "unit" to item.unit,
                            "qty" to item.qty,
                            "costPrice" to item.costPrice,
                            "lineTotal" to item.lineTotal
                        )
                    },
                    "totalAmount" to max(0.0, pur.totalAmount),
                    "paidAmount" to max(0.0, pur.paidAmount),
                    "dueAmount" to max(0.0, pur.dueAmount),
                    "paymentMethod" to pur.paymentMethod,
                    "purchaseEpochMs" to pur.purchaseEpochMs,
                    "note" to pur.note.take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            duePayments.filter { it.amount > 0.0 }.forEach { dp ->
                val ref = if (dp.id.isNotBlank()) userDoc(uid).collection("due_payments").document(dp.id)
                else userDoc(uid).collection("due_payments").document()
                ops += ref to mapOf(
                    "userId" to uid,
                    "partyType" to dp.partyType,
                    "partyId" to dp.partyId,
                    "partyName" to dp.partyName.take(100),
                    "amount" to dp.amount,
                    "paymentMethod" to dp.paymentMethod,
                    "paymentEpochMs" to dp.paymentEpochMs,
                    "note" to dp.note.take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            expenses.filter { it.amount > 0.0 }.forEach { exp ->
                val ref = if (exp.id.isNotBlank()) userDoc(uid).collection("expenses").document(exp.id)
                else userDoc(uid).collection("expenses").document()
                ops += ref to mapOf(
                    "userId" to uid,
                    "title" to exp.title.take(120),
                    "category" to exp.category.ifBlank { "General" }.take(60),
                    "amount" to exp.amount,
                    "paymentMethod" to exp.paymentMethod,
                    "expenseEpochMs" to exp.expenseEpochMs,
                    "note" to exp.note.take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }

            ops.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { (ref, data) -> batch.set(ref, data) }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDoc(uid).path, auth)
            throw e
        }
    }
}
