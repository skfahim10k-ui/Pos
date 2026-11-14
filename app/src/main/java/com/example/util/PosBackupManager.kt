package com.example.util

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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupShopProfile(
    val businessName: String = "Fahim Siam Traders",
    val ownerName: String = "",
    val ownerEmail: String = "",
    val phone: String = "",
    val address: String = "",
    val preferredLanguage: String = "both",
    val receiptFooterBn: String = "ধন্যবাদ, আবার আসবেন!",
    val receiptFooterEn: String = "Thank you for your business!"
)

@Serializable
data class BackupProduct(
    val id: String = "",
    val name: String = "",
    val nameBn: String = "",
    val sku: String = "",
    val barcode: String = "",
    val qrCode: String = "",
    val category: String = "General",
    val unit: String = "Pcs",
    val purchasePrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val stockQty: Double = 0.0,
    val lowStockThreshold: Double = 5.0
)

@Serializable
data class BackupCustomer(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val totalPurchases: Double = 0.0,
    val totalDue: Double = 0.0,
    val notes: String = ""
)

@Serializable
data class BackupSupplier(
    val id: String = "",
    val name: String = "",
    val companyName: String = "",
    val phone: String = "",
    val address: String = "",
    val totalSupplied: Double = 0.0,
    val totalDue: Double = 0.0,
    val notes: String = ""
)

@Serializable
data class BackupSaleItem(
    val productId: String = "",
    val productName: String = "",
    val productNameBn: String = "",
    val unit: String = "Pcs",
    val qty: Double = 0.0,
    val unitPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val lineTotal: Double = 0.0
)

@Serializable
data class BackupSale(
    val id: String = "",
    val memoNumber: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val items: List<BackupSaleItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val paymentMethod: String = "CASH",
    val saleEpochMs: Long = 0L,
    val note: String = ""
)

@Serializable
data class BackupPurchaseItem(
    val productId: String = "",
    val productName: String = "",
    val unit: String = "Pcs",
    val qty: Double = 0.0,
    val costPrice: Double = 0.0,
    val lineTotal: Double = 0.0
)

@Serializable
data class BackupPurchase(
    val id: String = "",
    val invoiceNumber: String = "",
    val supplierId: String = "",
    val supplierName: String = "",
    val items: List<BackupPurchaseItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val purchaseEpochMs: Long = 0L,
    val note: String = ""
)

@Serializable
data class BackupDuePayment(
    val id: String = "",
    val partyType: String = "CUSTOMER",
    val partyId: String = "",
    val partyName: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val paymentEpochMs: Long = 0L,
    val note: String = ""
)

@Serializable
data class BackupExpense(
    val id: String = "",
    val title: String = "",
    val category: String = "General",
    val amount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val expenseEpochMs: Long = 0L,
    val note: String = ""
)

@Serializable
data class PosBackupSnapshot(
    val version: Int = 1,
    val exportedAtEpochMs: Long = 0L,
    val shopProfile: BackupShopProfile? = null,
    val products: List<BackupProduct> = emptyList(),
    val customers: List<BackupCustomer> = emptyList(),
    val suppliers: List<BackupSupplier> = emptyList(),
    val sales: List<BackupSale> = emptyList(),
    val purchases: List<BackupPurchase> = emptyList(),
    val duePayments: List<BackupDuePayment> = emptyList(),
    val expenses: List<BackupExpense> = emptyList()
)

data class ParsedQrProduct(
    val code: String,
    val name: String? = null,
    val price: Double? = null
)

object PosBackupManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun exportToJson(
        shopProfile: ShopProfile?,
        products: List<Product>,
        customers: List<Customer>,
        suppliers: List<Supplier>,
        sales: List<Sale>,
        purchases: List<Purchase>,
        duePayments: List<DuePayment>,
        expenses: List<Expense>
    ): String {
        val snapshot = PosBackupSnapshot(
            version = 1,
            exportedAtEpochMs = System.currentTimeMillis(),
            shopProfile = shopProfile?.let {
                BackupShopProfile(
                    businessName = it.businessName,
                    ownerName = it.ownerName,
                    ownerEmail = it.ownerEmail,
                    phone = it.phone,
                    address = it.address,
                    preferredLanguage = it.preferredLanguage,
                    receiptFooterBn = it.receiptFooterBn,
                    receiptFooterEn = it.receiptFooterEn
                )
            },
            products = products.map {
                BackupProduct(
                    id = it.id,
                    name = it.name,
                    nameBn = it.nameBn,
                    sku = it.sku,
                    barcode = it.barcode,
                    qrCode = it.qrCode,
                    category = it.category,
                    unit = it.unit,
                    purchasePrice = it.purchasePrice,
                    sellPrice = it.sellPrice,
                    stockQty = it.stockQty,
                    lowStockThreshold = it.lowStockThreshold
                )
            },
            customers = customers.map {
                BackupCustomer(
                    id = it.id,
                    name = it.name,
                    phone = it.phone,
                    address = it.address,
                    totalPurchases = it.totalPurchases,
                    totalDue = it.totalDue,
                    notes = it.notes
                )
            },
            suppliers = suppliers.map {
                BackupSupplier(
                    id = it.id,
                    name = it.name,
                    companyName = it.companyName,
                    phone = it.phone,
                    address = it.address,
                    totalSupplied = it.totalSupplied,
                    totalDue = it.totalDue,
                    notes = it.notes
                )
            },
            sales = sales.map { s ->
                BackupSale(
                    id = s.id,
                    memoNumber = s.memoNumber,
                    customerId = s.customerId,
                    customerName = s.customerName,
                    customerPhone = s.customerPhone,
                    items = s.items.map {
                        BackupSaleItem(
                            productId = it.productId,
                            productName = it.productName,
                            productNameBn = it.productNameBn,
                            unit = it.unit,
                            qty = it.qty,
                            unitPrice = it.unitPrice,
                            costPrice = it.costPrice,
                            lineTotal = it.lineTotal
                        )
                    },
                    subtotal = s.subtotal,
                    discount = s.discount,
                    totalAmount = s.totalAmount,
                    paidAmount = s.paidAmount,
                    dueAmount = s.dueAmount,
                    totalCost = s.totalCost,
                    paymentMethod = s.paymentMethod,
                    saleEpochMs = s.saleEpochMs,
                    note = s.note
                )
            },
            purchases = purchases.map { p ->
                BackupPurchase(
                    id = p.id,
                    invoiceNumber = p.invoiceNumber,
                    supplierId = p.supplierId,
                    supplierName = p.supplierName,
                    items = p.items.map {
                        BackupPurchaseItem(
                            productId = it.productId,
                            productName = it.productName,
                            unit = it.unit,
                            qty = it.qty,
                            costPrice = it.costPrice,
                            lineTotal = it.lineTotal
                        )
                    },
                    totalAmount = p.totalAmount,
                    paidAmount = p.paidAmount,
                    dueAmount = p.dueAmount,
                    paymentMethod = p.paymentMethod,
                    purchaseEpochMs = p.purchaseEpochMs,
                    note = p.note
                )
            },
            duePayments = duePayments.map {
                BackupDuePayment(
                    id = it.id,
                    partyType = it.partyType,
                    partyId = it.partyId,
                    partyName = it.partyName,
                    amount = it.amount,
                    paymentMethod = it.paymentMethod,
                    paymentEpochMs = it.paymentEpochMs,
                    note = it.note
                )
            },
            expenses = expenses.map {
                BackupExpense(
                    id = it.id,
                    title = it.title,
                    category = it.category,
                    amount = it.amount,
                    paymentMethod = it.paymentMethod,
                    expenseEpochMs = it.expenseEpochMs,
                    note = it.note
                )
            }
        )
        return json.encodeToString(snapshot)
    }

    fun parseFromJson(rawJson: String): PosBackupSnapshot {
        return json.decodeFromString(rawJson)
    }

    fun toDomainProducts(list: List<BackupProduct>) = list.map {
        Product(
            id = it.id,
            name = it.name,
            nameBn = it.nameBn,
            sku = it.sku,
            barcode = it.barcode,
            qrCode = it.qrCode,
            category = it.category,
            unit = it.unit,
            purchasePrice = it.purchasePrice,
            sellPrice = it.sellPrice,
            stockQty = it.stockQty,
            lowStockThreshold = it.lowStockThreshold
        )
    }

    fun toDomainCustomers(list: List<BackupCustomer>) = list.map {
        Customer(
            id = it.id,
            name = it.name,
            phone = it.phone,
            address = it.address,
            totalPurchases = it.totalPurchases,
            totalDue = it.totalDue,
            notes = it.notes
        )
    }

    fun toDomainSuppliers(list: List<BackupSupplier>) = list.map {
        Supplier(
            id = it.id,
            name = it.name,
            companyName = it.companyName,
            phone = it.phone,
            address = it.address,
            totalSupplied = it.totalSupplied,
            totalDue = it.totalDue,
            notes = it.notes
        )
    }

    fun toDomainSales(list: List<BackupSale>) = list.map { s ->
        Sale(
            id = s.id,
            memoNumber = s.memoNumber,
            customerId = s.customerId,
            customerName = s.customerName,
            customerPhone = s.customerPhone,
            items = s.items.map {
                SaleLineItem(
                    productId = it.productId,
                    productName = it.productName,
                    productNameBn = it.productNameBn,
                    unit = it.unit,
                    qty = it.qty,
                    unitPrice = it.unitPrice,
                    costPrice = it.costPrice,
                    lineTotal = it.lineTotal
                )
            },
            subtotal = s.subtotal,
            discount = s.discount,
            totalAmount = s.totalAmount,
            paidAmount = s.paidAmount,
            dueAmount = s.dueAmount,
            totalCost = s.totalCost,
            paymentMethod = s.paymentMethod,
            saleEpochMs = s.saleEpochMs,
            note = s.note
        )
    }

    fun toDomainPurchases(list: List<BackupPurchase>) = list.map { p ->
        Purchase(
            id = p.id,
            invoiceNumber = p.invoiceNumber,
            supplierId = p.supplierId,
            supplierName = p.supplierName,
            items = p.items.map {
                PurchaseLineItem(
                    productId = it.productId,
                    productName = it.productName,
                    unit = it.unit,
                    qty = it.qty,
                    costPrice = it.costPrice,
                    lineTotal = it.lineTotal
                )
            },
            totalAmount = p.totalAmount,
            paidAmount = p.paidAmount,
            dueAmount = p.dueAmount,
            paymentMethod = p.paymentMethod,
            purchaseEpochMs = p.purchaseEpochMs,
            note = p.note
        )
    }

    fun toDomainDuePayments(list: List<BackupDuePayment>) = list.map {
        DuePayment(
            id = it.id,
            partyType = it.partyType,
            partyId = it.partyId,
            partyName = it.partyName,
            amount = it.amount,
            paymentMethod = it.paymentMethod,
            paymentEpochMs = it.paymentEpochMs,
            note = it.note
        )
    }

    fun toDomainExpenses(list: List<BackupExpense>) = list.map {
        Expense(
            id = it.id,
            title = it.title,
            category = it.category,
            amount = it.amount,
            paymentMethod = it.paymentMethod,
            expenseEpochMs = it.expenseEpochMs,
            note = it.note
        )
    }

    fun parseQrCodePayload(raw: String): ParsedQrProduct {
        val trimmed = raw.trim()
        if (trimmed.startsWith("FST:", ignoreCase = true)) {
            val parts = trimmed.removePrefix("FST:").removePrefix("fst:").split("|")
            return ParsedQrProduct(
                code = parts.getOrNull(0)?.trim().orEmpty().ifBlank { trimmed },
                name = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() },
                price = parts.getOrNull(2)?.trim()?.toDoubleOrNull()
            )
        }
        return ParsedQrProduct(code = trimmed)
    }
}
