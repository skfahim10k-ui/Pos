package com.example.util

import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleLineItem
import com.example.data.model.ShopProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PosBackupManagerTest {

    @Test
    fun exportAndParseBackup_roundTripsAllEntities() {
        val profile = ShopProfile(
            businessName = "Fahim Siam Traders",
            phone = "01711223344",
            preferredLanguage = "both"
        )
        val products = listOf(
            Product(
                id = "p1",
                name = "Miniket Rice 25kg",
                nameBn = "মিনিকেট চাল ২৫ কেজি",
                barcode = "890111222333",
                category = "Grocery",
                unit = "Bag",
                purchasePrice = 1650.0,
                sellPrice = 1820.0,
                stockQty = 12.0,
                lowStockThreshold = 3.0
            )
        )
        val customers = listOf(
            Customer(id = "c1", name = "Karim Store", phone = "01811000000", totalDue = 500.0)
        )
        val sales = listOf(
            Sale(
                id = "s1",
                memoNumber = "FST-001",
                customerId = "c1",
                customerName = "Karim Store",
                items = listOf(
                    SaleLineItem(
                        productId = "p1",
                        productName = "Miniket Rice 25kg",
                        unit = "Bag",
                        qty = 2.0,
                        unitPrice = 1820.0,
                        costPrice = 1650.0,
                        lineTotal = 3640.0
                    )
                ),
                subtotal = 3640.0,
                totalAmount = 3640.0,
                paidAmount = 3140.0,
                dueAmount = 500.0,
                totalCost = 3300.0,
                paymentMethod = "CASH",
                saleEpochMs = 1700000000000L
            )
        )

        val json = PosBackupManager.exportToJson(
            shopProfile = profile,
            products = products,
            customers = customers,
            suppliers = emptyList(),
            sales = sales,
            purchases = emptyList(),
            duePayments = emptyList(),
            expenses = emptyList()
        )
        val parsed = PosBackupManager.parseFromJson(json)
        assertThat(parsed.shopProfile?.businessName).isEqualTo("Fahim Siam Traders")
        assertThat(parsed.products).hasSize(1)
        assertThat(parsed.products.first().nameBn).isEqualTo("মিনিকেট চাল ২৫ কেজি")
        assertThat(parsed.sales.first().memoNumber).isEqualTo("FST-001")
    }

    @Test
    fun parseQrCodePayload_extractsStructuredAndPlainCodes() {
        val structured = PosBackupManager.parseQrCodePayload("FST:890123|Soybean Oil|175")
        assertThat(structured.code).isEqualTo("890123")
        assertThat(structured.name).isEqualTo("Soybean Oil")
        assertThat(structured.price).isEqualTo(175.0)

        val plain = PosBackupManager.parseQrCodePayload("8901234567890")
        assertThat(plain.code).isEqualTo("8901234567890")
        assertThat(plain.name).isNull()
    }
}
