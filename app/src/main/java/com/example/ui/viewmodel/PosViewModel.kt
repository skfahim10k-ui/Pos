package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
import com.example.data.remote.unwrapFirestoreException
import com.example.data.repository.PosRepository
import com.example.ui.i18n.AppLanguage
import com.example.util.PosBackupManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class CartItem(
    val product: Product,
    val qty: Double = 1.0
) {
    val lineTotal: Double get() = qty * product.sellPrice
    val lineCost: Double get() = qty * product.purchasePrice
}

data class PosDashboardUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val shopProfile: ShopProfile = ShopProfile(),
    val products: List<Product> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val sales: List<Sale> = emptyList(),
    val purchases: List<Purchase> = emptyList(),
    val duePayments: List<DuePayment> = emptyList(),
    val expenses: List<Expense> = emptyList()
)

class PosViewModel(
    private val repository: PosRepository,
    private val currentUserId: String,
    private val ownerName: String,
    private val ownerEmail: String
) : ViewModel() {

    private val _languageOverride = MutableStateFlow<AppLanguage?>(null)
    val languageOverride: StateFlow<AppLanguage?> = _languageOverride.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _selectedCustomerId = MutableStateFlow<String?>(null)
    val selectedCustomerId: StateFlow<String?> = _selectedCustomerId.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _latestSaleForMemo = MutableStateFlow<Sale?>(null)
    val latestSaleForMemo: StateFlow<Sale?> = _latestSaleForMemo.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.ensureShopProfile(ownerName = ownerName, ownerEmail = ownerEmail)
            } catch (e: Exception) {
                Log.w(TAG, "Could not ensure shop profile", e)
            }
        }
    }

    private val coreStreams = combine(
        repository.observeShopProfile(currentUserId),
        repository.observeProducts(currentUserId),
        repository.observeCustomers(currentUserId),
        repository.observeSuppliers(currentUserId)
    ) { profile, products, customers, suppliers ->
        CoreBundle(profile, products, customers, suppliers)
    }

    private val ledgerStreams = combine(
        repository.observeSales(currentUserId),
        repository.observePurchases(currentUserId),
        repository.observeDuePayments(currentUserId),
        repository.observeExpenses(currentUserId)
    ) { sales, purchases, dues, expenses ->
        LedgerBundle(sales, purchases, dues, expenses)
    }

    val uiState: StateFlow<PosDashboardUiState> = combine(coreStreams, ledgerStreams) { core, ledger ->
        PosDashboardUiState(
            isLoading = false,
            errorMessage = null,
            shopProfile = core.profile ?: ShopProfile(
                userId = currentUserId,
                businessName = "Fahim Siam Traders",
                ownerName = ownerName,
                ownerEmail = ownerEmail
            ),
            products = core.products,
            customers = core.customers,
            suppliers = core.suppliers,
            sales = ledger.sales,
            purchases = ledger.purchases,
            duePayments = ledger.duePayments,
            expenses = ledger.expenses
        )
    }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing POS streams", rootError)
            emit(
                PosDashboardUiState(
                    isLoading = false,
                    errorMessage = rootError.message ?: "Failed to load store data"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = PosDashboardUiState(isLoading = true)
        )

    fun activeLanguage(): AppLanguage {
        return _languageOverride.value ?: AppLanguage.fromCode(uiState.value.shopProfile.preferredLanguage)
    }

    fun cycleLanguage() {
        val current = activeLanguage()
        val next = when (current) {
            AppLanguage.BILINGUAL -> AppLanguage.BENGALI
            AppLanguage.BENGALI -> AppLanguage.ENGLISH
            AppLanguage.ENGLISH -> AppLanguage.BILINGUAL
        }
        _languageOverride.value = next
        val profile = uiState.value.shopProfile
        viewModelScope.launch {
            try {
                repository.updateShopProfile(profile.copy(preferredLanguage = next.code))
            } catch (_: Exception) {
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun dismissMemoDialog() {
        _latestSaleForMemo.value = null
    }

    fun openMemoDialog(sale: Sale) {
        _latestSaleForMemo.value = sale
    }

    // --- Cart Operations ---

    fun addToCart(product: Product, qtyToAdd: Double = 1.0) {
        _cart.update { items ->
            val idx = items.indexOfFirst { it.product.id == product.id }
            if (idx >= 0) {
                items.mapIndexed { index, cartItem ->
                    if (index == idx) cartItem.copy(qty = cartItem.qty + qtyToAdd) else cartItem
                }
            } else {
                if (items.size >= 50) {
                    _statusMessage.value = "সর্বোচ্চ ৫০টি পণ্য এক বিলে যোগ করা যাবে / Max 50 items per bill"
                    items
                } else {
                    items + CartItem(product = product, qty = qtyToAdd)
                }
            }
        }
    }

    fun updateCartQty(productId: String, newQty: Double) {
        if (newQty <= 0.0) {
            removeFromCart(productId)
            return
        }
        _cart.update { items ->
            items.map { if (it.product.id == productId) it.copy(qty = newQty) else it }
        }
    }

    fun removeFromCart(productId: String) {
        _cart.update { items -> items.filterNot { it.product.id == productId } }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _selectedCustomerId.value = null
    }

    fun selectCustomer(customerId: String?) {
        _selectedCustomerId.value = customerId
    }

    fun addProductToCartByCode(rawCode: String): Product? {
        val parsed = PosBackupManager.parseQrCodePayload(rawCode)
        val normalized = parsed.code.trim()
        val found = uiState.value.products.firstOrNull { p ->
            p.barcode.equals(normalized, ignoreCase = true) ||
                p.qrCode.equals(normalized, ignoreCase = true) ||
                p.qrCode.equals(rawCode.trim(), ignoreCase = true) ||
                p.sku.equals(normalized, ignoreCase = true)
        }
        if (found != null) {
            addToCart(found, 1.0)
            _statusMessage.value = "যোগ হয়েছে / Added: ${found.name}"
        }
        return found
    }

    // --- Checkout ---

    fun completeSale(
        discount: Double,
        paidAmount: Double,
        paymentMethod: String,
        walkInCustomerName: String,
        note: String
    ) {
        val items = _cart.value
        if (items.isEmpty()) return
        val subtotal = items.sumOf { it.lineTotal }
        val cleanDiscount = discount.coerceIn(0.0, subtotal)
        val total = max(0.0, subtotal - cleanDiscount)
        val cleanPaid = paidAmount.coerceIn(0.0, total)
        val due = max(0.0, total - cleanPaid)
        val totalCost = items.sumOf { it.lineCost }
        val now = System.currentTimeMillis()
        val memoNum = "FST-${SimpleDateFormat("yyMMdd-HHmmss", Locale.US).format(Date(now))}"
        val customer = uiState.value.customers.firstOrNull { it.id == _selectedCustomerId.value }

        val sale = Sale(
            memoNumber = memoNum,
            customerId = customer?.id.orEmpty(),
            customerName = customer?.name ?: walkInCustomerName.ifBlank { "Walk-in Customer" },
            customerPhone = customer?.phone.orEmpty(),
            items = items.map {
                SaleLineItem(
                    productId = it.product.id,
                    productName = it.product.name,
                    productNameBn = it.product.nameBn,
                    unit = it.product.unit,
                    qty = it.qty,
                    unitPrice = it.product.sellPrice,
                    costPrice = it.product.purchasePrice,
                    lineTotal = it.lineTotal
                )
            },
            subtotal = subtotal,
            discount = cleanDiscount,
            totalAmount = total,
            paidAmount = cleanPaid,
            dueAmount = due,
            totalCost = totalCost,
            paymentMethod = paymentMethod,
            saleEpochMs = now,
            note = note
        )

        viewModelScope.launch {
            _isBusy.value = true
            try {
                val saleId = repository.recordSale(sale, uiState.value.products, customer)
                _latestSaleForMemo.value = sale.copy(id = saleId)
                clearCart()
                _statusMessage.value = "বিক্রি সম্পন্ন হয়েছে / Sale recorded ($memoNum)"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Sale failed"
            } finally {
                _isBusy.value = false
            }
        }
    }

    // --- Products & Stock ---

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.saveProduct(product)
                _statusMessage.value = "পণ্য সংরক্ষিত হয়েছে / Product saved"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to save product"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun adjustStock(productId: String, newStockQty: Double) {
        viewModelScope.launch {
            try {
                repository.adjustProductStock(productId, newStockQty)
                _statusMessage.value = "স্টক আপডেট হয়েছে / Stock updated"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to update stock"
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(productId)
                _statusMessage.value = "পণ্য মুছে ফেলা হয়েছে / Product deleted"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Delete failed"
            }
        }
    }

    // --- Purchases ---

    fun recordPurchase(
        supplier: Supplier?,
        supplierNameFallback: String,
        items: List<PurchaseLineItem>,
        paidAmount: Double,
        paymentMethod: String,
        note: String
    ) {
        if (items.isEmpty()) return
        val total = items.sumOf { it.lineTotal }
        val cleanPaid = paidAmount.coerceIn(0.0, total)
        val due = max(0.0, total - cleanPaid)
        val now = System.currentTimeMillis()
        val invoiceNo = "PUR-${SimpleDateFormat("yyMMdd-HHmmss", Locale.US).format(Date(now))}"

        val purchase = Purchase(
            invoiceNumber = invoiceNo,
            supplierId = supplier?.id.orEmpty(),
            supplierName = supplier?.name ?: supplierNameFallback.ifBlank { "General Supplier" },
            items = items,
            totalAmount = total,
            paidAmount = cleanPaid,
            dueAmount = due,
            paymentMethod = paymentMethod,
            purchaseEpochMs = now,
            note = note
        )

        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.recordPurchase(purchase, uiState.value.products, supplier)
                _statusMessage.value = "ক্রয় হিসাব সংরক্ষিত ও স্টক আপডেট হয়েছে / Purchase saved ($invoiceNo)"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Purchase failed"
            } finally {
                _isBusy.value = false
            }
        }
    }

    // --- Customers & Suppliers ---

    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            try {
                repository.saveCustomer(customer)
                _statusMessage.value = "কাস্টমার তথ্য সংরক্ষিত হয়েছে / Customer saved"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to save customer"
            }
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            try {
                repository.deleteCustomer(customerId)
                _statusMessage.value = "কাস্টমার মুছে ফেলা হয়েছে / Customer deleted"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Delete failed"
            }
        }
    }

    fun saveSupplier(supplier: Supplier) {
        viewModelScope.launch {
            try {
                repository.saveSupplier(supplier)
                _statusMessage.value = "সরবরাহকারীর তথ্য সংরক্ষিত হয়েছে / Supplier saved"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to save supplier"
            }
        }
    }

    fun deleteSupplier(supplierId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSupplier(supplierId)
                _statusMessage.value = "সরবরাহকারী মুছে ফেলা হয়েছে / Supplier deleted"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Delete failed"
            }
        }
    }

    // --- Due Payments ---

    fun collectCustomerDue(customer: Customer, amount: Double, method: String, note: String) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.recordCustomerDueCollection(customer, amount, method, note)
                _statusMessage.value = "বাকি আদায় সংরক্ষিত হয়েছে / Due collected"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to record collection"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun paySupplierDue(supplier: Supplier, amount: Double, method: String, note: String) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.recordSupplierDuePayment(supplier, amount, method, note)
                _statusMessage.value = "সরবরাহকারীর বাকি পরিশোধ হয়েছে / Supplier due paid"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to record payment"
            } finally {
                _isBusy.value = false
            }
        }
    }

    // --- Expenses ---

    fun saveExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                repository.saveExpense(expense)
                _statusMessage.value = "খরচ সংরক্ষিত হয়েছে / Expense saved"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to save expense"
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(expenseId)
                _statusMessage.value = "খরচ মুছে ফেলা হয়েছে / Expense deleted"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Delete failed"
            }
        }
    }

    // --- Shop Profile & Backup ---

    fun saveShopProfile(profile: ShopProfile) {
        viewModelScope.launch {
            try {
                repository.updateShopProfile(profile)
                _statusMessage.value = "দোকানের তথ্য আপডেট হয়েছে / Shop profile updated"
            } catch (e: Exception) {
                _statusMessage.value = e.localizedMessage ?: "Failed to update shop profile"
            }
        }
    }

    fun exportBackupJson(): String {
        val s = uiState.value
        return PosBackupManager.exportToJson(
            shopProfile = s.shopProfile,
            products = s.products,
            customers = s.customers,
            suppliers = s.suppliers,
            sales = s.sales,
            purchases = s.purchases,
            duePayments = s.duePayments,
            expenses = s.expenses
        )
    }

    fun restoreBackupJson(rawJson: String) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val snap = PosBackupManager.parseFromJson(rawJson)
                val currentProfile = uiState.value.shopProfile
                val restoredProfile = snap.shopProfile?.let {
                    currentProfile.copy(
                        businessName = it.businessName,
                        ownerName = it.ownerName,
                        ownerEmail = it.ownerEmail,
                        phone = it.phone,
                        address = it.address,
                        preferredLanguage = it.preferredLanguage,
                        receiptFooterBn = it.receiptFooterBn,
                        receiptFooterEn = it.receiptFooterEn
                    )
                }
                repository.restoreBackupBatch(
                    products = PosBackupManager.toDomainProducts(snap.products),
                    customers = PosBackupManager.toDomainCustomers(snap.customers),
                    suppliers = PosBackupManager.toDomainSuppliers(snap.suppliers),
                    sales = PosBackupManager.toDomainSales(snap.sales),
                    purchases = PosBackupManager.toDomainPurchases(snap.purchases),
                    duePayments = PosBackupManager.toDomainDuePayments(snap.duePayments),
                    expenses = PosBackupManager.toDomainExpenses(snap.expenses),
                    shopProfile = restoredProfile
                )
                _statusMessage.value = "ব্যাকআপ পুনরুদ্ধার সম্পন্ন হয়েছে / Backup restored (${snap.products.size} products, ${snap.sales.size} sales)"
            } catch (e: Exception) {
                _statusMessage.value = "ব্যাকআপ ফাইল সঠিক নয় / Invalid backup: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    private data class CoreBundle(
        val profile: ShopProfile?,
        val products: List<Product>,
        val customers: List<Customer>,
        val suppliers: List<Supplier>
    )

    private data class LedgerBundle(
        val sales: List<Sale>,
        val purchases: List<Purchase>,
        val duePayments: List<DuePayment>,
        val expenses: List<Expense>
    )

    companion object {
        private const val TAG = "PosViewModel"

        fun provideFactory(
            currentUserId: String,
            ownerName: String,
            ownerEmail: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                PosViewModel(
                    repository = PosRepository(db),
                    currentUserId = currentUserId,
                    ownerName = ownerName,
                    ownerEmail = ownerEmail
                )
            }
        }
    }
}
