package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ShopProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessName: String = "Fahim Siam Traders",
    val ownerName: String = "",
    val ownerEmail: String = "",
    val phone: String = "",
    val address: String = "",
    val preferredLanguage: String = "both", // "bn", "en", "both"
    val receiptFooterBn: String = "ধন্যবাদ, আবার আসবেন!",
    val receiptFooterEn: String = "Thank you for your business!",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val userId: String = "",
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
    val lowStockThreshold: Double = 5.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val totalPurchases: Double = 0.0,
    val totalDue: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Supplier(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val companyName: String = "",
    val phone: String = "",
    val address: String = "",
    val totalSupplied: Double = 0.0,
    val totalDue: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SaleLineItem(
    val productId: String = "",
    val productName: String = "",
    val productNameBn: String = "",
    val unit: String = "Pcs",
    val qty: Double = 0.0,
    val unitPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val lineTotal: Double = 0.0
)

@IgnoreExtraProperties
data class Sale(
    @DocumentId val id: String = "",
    val userId: String = "",
    val memoNumber: String = "",
    val customerId: String = "",
    val customerName: String = "Walk-in Customer",
    val customerPhone: String = "",
    val items: List<SaleLineItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val paymentMethod: String = "CASH",
    val saleEpochMs: Long = 0L,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PurchaseLineItem(
    val productId: String = "",
    val productName: String = "",
    val unit: String = "Pcs",
    val qty: Double = 0.0,
    val costPrice: Double = 0.0,
    val lineTotal: Double = 0.0
)

@IgnoreExtraProperties
data class Purchase(
    @DocumentId val id: String = "",
    val userId: String = "",
    val invoiceNumber: String = "",
    val supplierId: String = "",
    val supplierName: String = "",
    val items: List<PurchaseLineItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val purchaseEpochMs: Long = 0L,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DuePayment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val partyType: String = "CUSTOMER", // "CUSTOMER" or "SUPPLIER"
    val partyId: String = "",
    val partyName: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val paymentEpochMs: Long = 0L,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "General",
    val amount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val expenseEpochMs: Long = 0L,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
