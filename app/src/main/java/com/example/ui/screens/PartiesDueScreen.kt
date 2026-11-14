package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Customer
import com.example.data.model.DuePayment
import com.example.data.model.Expense
import com.example.data.model.Supplier
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.formatDateTime
import com.example.ui.i18n.formatTaka
import com.example.ui.i18n.pick
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartiesDueScreen(
    customers: List<Customer>,
    suppliers: List<Supplier>,
    duePayments: List<DuePayment>,
    expenses: List<Expense>,
    initialTab: Int = 0,
    language: AppLanguage,
    onSaveCustomer: (Customer) -> Unit,
    onDeleteCustomer: (String) -> Unit,
    onSaveSupplier: (Supplier) -> Unit,
    onDeleteSupplier: (String) -> Unit,
    onCollectCustomerDue: (Customer, Double, String, String) -> Unit,
    onPaySupplierDue: (Supplier, Double, String, String) -> Unit,
    onSaveExpense: (Expense) -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var showCustomerDialog by remember { mutableStateOf(false) }

    var editingSupplier by remember { mutableStateOf<Supplier?>(null) }
    var showSupplierDialog by remember { mutableStateOf(false) }

    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var showExpenseDialog by remember { mutableStateOf(false) }

    var collectTargetCustomer by remember { mutableStateOf<Customer?>(null) }
    var payTargetSupplier by remember { mutableStateOf<Supplier?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(language.pick("বাকি খাতা", "Due Ledger")) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(language.pick("কাস্টমার", "Customers")) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(language.pick("সরবরাহকারী", "Suppliers")) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text(language.pick("খরচ", "Expenses")) }
                )
            }

            when (selectedTab) {
                0 -> DueLedgerTab(
                    customers = customers,
                    suppliers = suppliers,
                    duePayments = duePayments,
                    language = language,
                    onCollectCustomer = { collectTargetCustomer = it },
                    onPaySupplier = { payTargetSupplier = it }
                )
                1 -> CustomersTab(
                    customers = customers,
                    language = language,
                    onEdit = {
                        editingCustomer = it
                        showCustomerDialog = true
                    },
                    onCollectDue = { collectTargetCustomer = it },
                    onDelete = onDeleteCustomer
                )
                2 -> SuppliersTab(
                    suppliers = suppliers,
                    language = language,
                    onEdit = {
                        editingSupplier = it
                        showSupplierDialog = true
                    },
                    onPayDue = { payTargetSupplier = it },
                    onDelete = onDeleteSupplier
                )
                else -> ExpensesTab(
                    expenses = expenses,
                    language = language,
                    onEdit = {
                        editingExpense = it
                        showExpenseDialog = true
                    },
                    onDelete = onDeleteExpense
                )
            }
        }

        if (selectedTab != 0) {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        1 -> {
                            editingCustomer = null
                            showCustomerDialog = true
                        }
                        2 -> {
                            editingSupplier = null
                            showSupplierDialog = true
                        }
                        3 -> {
                            editingExpense = null
                            showExpenseDialog = true
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .testTag("parties_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add entry")
            }
        }
    }

    if (showCustomerDialog) {
        CustomerEditorDialog(
            initial = editingCustomer,
            language = language,
            onSave = {
                onSaveCustomer(it)
                showCustomerDialog = false
            },
            onDismiss = { showCustomerDialog = false }
        )
    }

    if (showSupplierDialog) {
        SupplierEditorDialog(
            initial = editingSupplier,
            language = language,
            onSave = {
                onSaveSupplier(it)
                showSupplierDialog = false
            },
            onDismiss = { showSupplierDialog = false }
        )
    }

    if (showExpenseDialog) {
        ExpenseEditorDialog(
            initial = editingExpense,
            language = language,
            onSave = {
                onSaveExpense(it)
                showExpenseDialog = false
            },
            onDismiss = { showExpenseDialog = false }
        )
    }

    if (collectTargetCustomer != null) {
        val c = collectTargetCustomer!!
        DuePaymentDialog(
            title = language.pick("কাস্টমার বাকি আদায়: ${c.name}", "Collect Due: ${c.name}"),
            currentDue = c.totalDue,
            language = language,
            onConfirm = { amount, method, note ->
                onCollectCustomerDue(c, amount, method, note)
                collectTargetCustomer = null
            },
            onDismiss = { collectTargetCustomer = null }
        )
    }

    if (payTargetSupplier != null) {
        val s = payTargetSupplier!!
        DuePaymentDialog(
            title = language.pick("সরবরাহকারী বাকি পরিশোধ: ${s.name}", "Pay Supplier Due: ${s.name}"),
            currentDue = s.totalDue,
            language = language,
            onConfirm = { amount, method, note ->
                onPaySupplierDue(s, amount, method, note)
                payTargetSupplier = null
            },
            onDismiss = { payTargetSupplier = null }
        )
    }
}

@Composable
private fun DueLedgerTab(
    customers: List<Customer>,
    suppliers: List<Supplier>,
    duePayments: List<DuePayment>,
    language: AppLanguage,
    onCollectCustomer: (Customer) -> Unit,
    onPaySupplier: (Supplier) -> Unit
) {
    val dueCustomers = remember(customers) { customers.filter { it.totalDue > 0.0 }.sortedByDescending { it.totalDue } }
    val dueSuppliers = remember(suppliers) { suppliers.filter { it.totalDue > 0.0 }.sortedByDescending { it.totalDue } }
    val totalReceivable = remember(customers) { customers.sumOf { it.totalDue } }
    val totalPayable = remember(suppliers) { suppliers.sumOf { it.totalDue } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = StatusDanger.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(language.pick("পাওনা বাকি", "Receivable Due"), style = MaterialTheme.typography.labelMedium)
                        Text(
                            formatTaka(totalReceivable),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.Bold,
                            color = StatusDanger
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(language.pick("দেনা বাকি", "Payable Due"), style = MaterialTheme.typography.labelMedium)
                        Text(
                            formatTaka(totalPayable),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        if (dueCustomers.isNotEmpty()) {
            item {
                Text(
                    language.pick("কাস্টমারদের কাছে পাওনা", "Customer Receivables"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            items(dueCustomers, key = { "dc_${it.id}" }) { c ->
                Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(c.phone.ifBlank { "No phone" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                formatTaka(c.totalDue),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.Bold,
                                color = StatusDanger
                            )
                            Button(onClick = { onCollectCustomer(c) }) {
                                Text(language.pick("আদায়", "Collect"))
                            }
                        }
                    }
                }
            }
        }

        if (dueSuppliers.isNotEmpty()) {
            item {
                Text(
                    language.pick("সরবরাহকারীর দেনা", "Supplier Payables"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            items(dueSuppliers, key = { "ds_${it.id}" }) { s ->
                Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(s.companyName.ifBlank { s.phone }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                formatTaka(s.totalDue),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            OutlinedButton(onClick = { onPaySupplier(s) }) {
                                Text(language.pick("পরিশোধ", "Pay"))
                            }
                        }
                    }
                }
            }
        }

        if (dueCustomers.isEmpty() && dueSuppliers.isEmpty()) {
            item {
                EmptySectionCard(
                    icon = Icons.Filled.AccountBalanceWallet,
                    message = language.pick("কোনো বাকি নেই! সব হিসাব পরিশোধিত।", "No outstanding dues right now.")
                )
            }
        }

        if (duePayments.isNotEmpty()) {
            item {
                Text(
                    language.pick("পরিশোধের ইতিহাস", "Due Payment History"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            items(duePayments.take(20), key = { it.id }) { pay ->
                Card(shape = MaterialTheme.shapes.small, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${pay.partyName} (${pay.partyType})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${formatDateTime(pay.paymentEpochMs)} • ${pay.paymentMethod}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatTaka(pay.amount),
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccess
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomersTab(
    customers: List<Customer>,
    language: AppLanguage,
    onEdit: (Customer) -> Unit,
    onCollectDue: (Customer) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (customers.isEmpty()) {
            item {
                EmptySectionCard(
                    icon = Icons.Filled.People,
                    message = language.pick("কোনো কাস্টমার যোগ করা হয়নি। + বাটনে ট্যাপ করুন।", "No customers yet. Tap + to add a customer.")
                )
            }
        } else {
            items(customers, key = { it.id }) { customer ->
                Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(customer.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (customer.phone.isNotBlank() || customer.address.isNotBlank()) {
                                Text(
                                    listOf(customer.phone, customer.address).filter { it.isNotBlank() }.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "${language.pick("মোট ক্রয়", "Bought")}: ${formatTaka(customer.totalPurchases)}  |  ${language.pick("বাকি", "Due")}: ${formatTaka(customer.totalDue)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                color = if (customer.totalDue > 0) StatusDanger else StatusSuccess
                            )
                        }
                        Row {
                            if (customer.totalDue > 0) {
                                IconButton(onClick = { onCollectDue(customer) }) {
                                    Icon(Icons.Filled.Payments, contentDescription = "Collect due", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            IconButton(onClick = { onEdit(customer) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit customer")
                            }
                            IconButton(onClick = { onDelete(customer.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete customer", tint = StatusDanger)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuppliersTab(
    suppliers: List<Supplier>,
    language: AppLanguage,
    onEdit: (Supplier) -> Unit,
    onPayDue: (Supplier) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (suppliers.isEmpty()) {
            item {
                EmptySectionCard(
                    icon = Icons.Filled.LocalShipping,
                    message = language.pick("কোনো সরবরাহকারী নেই। + বাটনে ট্যাপ করুন।", "No suppliers yet. Tap + to add a supplier.")
                )
            }
        } else {
            items(suppliers, key = { it.id }) { supplier ->
                Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(supplier.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (supplier.companyName.isNotBlank() || supplier.phone.isNotBlank()) {
                                Text(
                                    listOf(supplier.companyName, supplier.phone).filter { it.isNotBlank() }.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "${language.pick("সরবরাহ", "Supplied")}: ${formatTaka(supplier.totalSupplied)}  |  ${language.pick("দেনা", "Due")}: ${formatTaka(supplier.totalDue)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                color = if (supplier.totalDue > 0) StatusDanger else StatusSuccess
                            )
                        }
                        Row {
                            if (supplier.totalDue > 0) {
                                IconButton(onClick = { onPayDue(supplier) }) {
                                    Icon(Icons.Filled.Payments, contentDescription = "Pay supplier due", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            IconButton(onClick = { onEdit(supplier) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit supplier")
                            }
                            IconButton(onClick = { onDelete(supplier.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete supplier", tint = StatusDanger)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpensesTab(
    expenses: List<Expense>,
    language: AppLanguage,
    onEdit: (Expense) -> Unit,
    onDelete: (String) -> Unit
) {
    val totalExpenses = remember(expenses) { expenses.sumOf { it.amount } }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(language.pick("মোট খরচ", "Total Expenses"), style = MaterialTheme.typography.titleSmall)
                    Text(
                        formatTaka(totalExpenses),
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (expenses.isEmpty()) {
            item {
                EmptySectionCard(
                    icon = Icons.Filled.MoneyOff,
                    message = language.pick("কোনো খরচ রেকর্ড নেই। + বাটনে ট্যাপ করে খরচ যোগ করুন।", "No expenses recorded yet. Tap + to add one.")
                )
            }
        } else {
            items(expenses, key = { it.id }) { expense ->
                Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(expense.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${expense.category} • ${formatDateTime(expense.expenseEpochMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                formatTaka(expense.amount),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.Bold,
                                color = StatusDanger
                            )
                            IconButton(onClick = { onEdit(expense) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit expense")
                            }
                            IconButton(onClick = { onDelete(expense.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete expense", tint = StatusDanger)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerEditorDialog(
    initial: Customer?,
    language: AppLanguage,
    onSave: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var phone by remember { mutableStateOf(initial?.phone.orEmpty()) }
    var address by remember { mutableStateOf(initial?.address.orEmpty()) }
    var openingDue by remember { mutableStateOf(initial?.totalDue?.takeIf { it > 0 }?.toString().orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) language.pick("নতুন কাস্টমার", "Add Customer") else language.pick("কাস্টমার সম্পাদনা", "Edit Customer")) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(language.pick("নাম*", "Name*")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(language.pick("মোবাইল নম্বর", "Phone")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(language.pick("ঠিকানা", "Address")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = openingDue, onValueChange = { openingDue = it }, label = { Text(language.pick("আগের বাকি (৳)", "Due Balance (৳)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(language.pick("নোট", "Notes")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            (initial ?: Customer()).copy(
                                name = name.trim(),
                                phone = phone.trim(),
                                address = address.trim(),
                                totalDue = openingDue.toDoubleOrNull() ?: (initial?.totalDue ?: 0.0),
                                notes = notes.trim()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) { Text(language.pick("সংরক্ষণ", "Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(language.pick("বাতিল", "Cancel")) } }
    )
}

@Composable
private fun SupplierEditorDialog(
    initial: Supplier?,
    language: AppLanguage,
    onSave: (Supplier) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var company by remember { mutableStateOf(initial?.companyName.orEmpty()) }
    var phone by remember { mutableStateOf(initial?.phone.orEmpty()) }
    var address by remember { mutableStateOf(initial?.address.orEmpty()) }
    var openingDue by remember { mutableStateOf(initial?.totalDue?.takeIf { it > 0 }?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) language.pick("নতুন সরবরাহকারী", "Add Supplier") else language.pick("সরবরাহকারী সম্পাদনা", "Edit Supplier")) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(language.pick("নাম*", "Contact Name*")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text(language.pick("কোম্পানি", "Company")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(language.pick("মোবাইল", "Phone")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(language.pick("ঠিকানা", "Address")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = openingDue, onValueChange = { openingDue = it }, label = { Text(language.pick("দেনা বাকি (৳)", "Payable Due (৳)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            (initial ?: Supplier()).copy(
                                name = name.trim(),
                                companyName = company.trim(),
                                phone = phone.trim(),
                                address = address.trim(),
                                totalDue = openingDue.toDoubleOrNull() ?: (initial?.totalDue ?: 0.0)
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) { Text(language.pick("সংরক্ষণ", "Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(language.pick("বাতিল", "Cancel")) } }
    )
}

@Composable
private fun ExpenseEditorDialog(
    initial: Expense?,
    language: AppLanguage,
    onSave: (Expense) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category?.ifBlank { "General" } ?: "Rent") }
    var amount by remember { mutableStateOf(initial?.amount?.takeIf { it > 0 }?.toString().orEmpty()) }
    var method by remember { mutableStateOf(initial?.paymentMethod?.ifBlank { "CASH" } ?: "CASH") }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) language.pick("দৈনিক খরচ যোগ", "Add Expense") else language.pick("খরচ সম্পাদনা", "Edit Expense")) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(language.pick("খরচের বিবরণ*", "Expense Title*")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(language.pick("টাকার পরিমাণ (৳)*", "Amount (৳)*")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Rent", "Electricity", "Transport", "Staff", "Snacks", "General")) { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("CASH", "BKASH", "NAGAD", "BANK")) { m ->
                        FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(language.pick("নোট", "Note")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            val amt = amount.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    if (title.isNotBlank() && amt > 0) {
                        onSave(
                            (initial ?: Expense()).copy(
                                title = title.trim(),
                                category = category,
                                amount = amt,
                                paymentMethod = method,
                                expenseEpochMs = initial?.expenseEpochMs?.takeIf { it > 0 } ?: System.currentTimeMillis(),
                                note = note.trim()
                            )
                        )
                    }
                },
                enabled = title.isNotBlank() && amt > 0
            ) { Text(language.pick("সংরক্ষণ", "Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(language.pick("বাতিল", "Cancel")) } }
    )
}

@Composable
private fun DuePaymentDialog(
    title: String,
    currentDue: Double,
    language: AppLanguage,
    onConfirm: (Double, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(currentDue.toInt().toString()) }
    var method by remember { mutableStateOf("CASH") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${language.pick("মোট বাকি", "Current Due")}: ${formatTaka(currentDue)}", fontWeight = FontWeight.Bold, color = StatusDanger)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(language.pick("পরিশোধের পরিমাণ (৳)", "Payment Amount (৳)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("CASH", "BKASH", "NAGAD", "BANK")) { m ->
                        FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(language.pick("নোট", "Note")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amt = amountText.toDoubleOrNull() ?: 0.0
            Button(onClick = { if (amt > 0) onConfirm(amt, method, note) }, enabled = amt > 0) {
                Text(language.pick("নিশ্চিত করুন", "Confirm"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(language.pick("বাতিল", "Cancel")) } }
    )
}
