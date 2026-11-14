package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleLineItem
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.fail
import org.junit.Test

class PosRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndObserveProduct_authenticatedOwner_succeedsAndEmits() = runBlocking {
        val uid = signInTestUser(OWNER_EMAIL)
        val repo = PosRepository(firestore, auth)

        val prodId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveProduct(
                Product(
                    name = "Rupchanda Soybean Oil 5L",
                    nameBn = "রূপচাঁদা সয়াবিন তেল ৫ লি.",
                    barcode = "890123400011",
                    category = "Grocery",
                    unit = "Btl",
                    purchasePrice = 810.0,
                    sellPrice = 880.0,
                    stockQty = 15.0,
                    lowStockThreshold = 3.0
                )
            )
        }
        assertThat(prodId).isNotEmpty()

        val products = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeProducts(uid).first { list -> list.any { it.id == prodId } }
        }
        assertThat(products.first { it.id == prodId }.name).isEqualTo("Rupchanda Soybean Oil 5L")
    }

    @Test
    fun recordSale_updatesStockAndCustomerDueAtomically() = runBlocking {
        val uid = signInTestUser(OWNER_EMAIL)
        val repo = PosRepository(firestore, auth)

        val prodId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveProduct(
                Product(
                    name = "Aarong Milk 1L",
                    category = "Dairy",
                    unit = "Pcs",
                    purchasePrice = 85.0,
                    sellPrice = 100.0,
                    stockQty = 10.0,
                    lowStockThreshold = 2.0
                )
            )
        }
        val custId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveCustomer(Customer(name = "Jamal Uddin", phone = "01700000001"))
        }

        val currentProducts = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeProducts(uid).first { list -> list.any { it.id == prodId } }
        }
        val currentCustomer = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCustomers(uid).first { list -> list.any { it.id == custId } }
        }.first { it.id == custId }

        val saleId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.recordSale(
                sale = Sale(
                    memoNumber = "FST-TEST-001",
                    customerId = custId,
                    customerName = currentCustomer.name,
                    items = listOf(
                        SaleLineItem(
                            productId = prodId,
                            productName = "Aarong Milk 1L",
                            unit = "Pcs",
                            qty = 3.0,
                            unitPrice = 100.0,
                            costPrice = 85.0,
                            lineTotal = 300.0
                        )
                    ),
                    subtotal = 300.0,
                    discount = 0.0,
                    totalAmount = 300.0,
                    paidAmount = 200.0,
                    dueAmount = 100.0,
                    totalCost = 255.0,
                    paymentMethod = "CASH",
                    saleEpochMs = System.currentTimeMillis()
                ),
                currentProducts = currentProducts,
                customer = currentCustomer
            )
        }
        assertThat(saleId).isNotEmpty()

        val updatedProducts = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeProducts(uid).first { list -> list.any { it.id == prodId && it.stockQty == 7.0 } }
        }
        assertThat(updatedProducts.first { it.id == prodId }.stockQty).isEqualTo(7.0)

        val updatedCustomers = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCustomers(uid).first { list -> list.any { it.id == custId && it.totalDue == 100.0 } }
        }
        assertThat(updatedCustomers.first { it.id == custId }.totalDue).isEqualTo(100.0)
    }

    @Test
    fun crossUserAccess_otherUserCannotObserveOwnersProducts() = runBlocking {
        val ownerUid = signInTestUser(OWNER_EMAIL)
        val ownerRepo = PosRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.saveProduct(Product(name = "Private Stock", category = "Misc", unit = "Pcs"))
        }

        signInTestUser(OTHER_EMAIL)
        val otherRepo = PosRepository(firestore, auth)
        val exception = try {
            withTimeout(FLOW_TIMEOUT_MS) {
                otherRepo.observeProducts(ownerUid).first()
            }
            fail("Expected PERMISSION_DENIED exception")
            throw AssertionError("Unreachable")
        } catch (e: Exception) {
            e
        }
        val firestoreException = checkNotNull(exception.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $exception"
        }
        assertThat(firestoreException.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    @Test
    fun unauthenticatedAccess_failsWithPermissionDenied() = runBlocking {
        val ownerUid = signInTestUser(OWNER_EMAIL)
        auth.signOut()
        val repo = PosRepository(firestore, auth)

        val exception = try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeExpenses(ownerUid).first()
            }
            fail("Expected PERMISSION_DENIED exception")
            throw AssertionError("Unreachable")
        } catch (e: Exception) {
            e
        }
        val firestoreException = checkNotNull(exception.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $exception"
        }
        assertThat(firestoreException.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    @Test
    fun saveExpense_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(OWNER_EMAIL)
        val repo = PosRepository(firestore, auth)
        val expId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveExpense(
                Expense(
                    title = "Shop Rent",
                    category = "Rent",
                    amount = 12000.0,
                    paymentMethod = "CASH",
                    expenseEpochMs = System.currentTimeMillis()
                )
            )
        }
        val list = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeExpenses(uid).first { it.any { exp -> exp.id == expId } }
        }
        assertThat(list.first { it.id == expId }.amount).isEqualTo(12000.0)
    }

    private companion object {
        const val OWNER_EMAIL = "fahim_owner@test.com"
        const val OTHER_EMAIL = "intruder@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
