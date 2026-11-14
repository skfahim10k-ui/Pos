package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseLineItem
import com.example.data.model.Supplier
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.formatDateTime
import com.example.ui.i18n.formatQty
import com.example.ui.i18n.formatTaka
import com.example.ui.i18n.pick
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.util.PosBackupManager
import com.example.util.QrBarcodeRenderer
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    products: List<Product>,
    suppliers: List<Supplier>,
    purchases: List<Purchase>,
    initialTab: Int = 0,
    prefillCode: String? = null,
    onConsumePrefillCode: () -> Unit = {},
    language: AppLanguage,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onAdjustStock: (String, Double) -> Unit,
    onRecordPurchase: (Supplier?, String, List<PurchaseLineItem>, Double, String, String) -> Unit,
    onScanCodeForProduct: ((String) -> Unit) -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    var stockFilter by remember { mutableStateOf("ALL") } // ALL, LOW, OUT

    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showProductDialog by remember { mutableStateOf(false) }
    var labelPreviewProduct by remember { mutableStateOf<Product?>(null) }
    var showPurchaseDialog by remember { mutableStateOf(false) }

    // If QR/Barcode scan triggered "add product with prefilled code"
    if (!prefillCode.isNullOrBlank() && !showProductDialog) {
        val parsed = PosBackupManager.parseQrCodePayload(prefillCode)
        editingProduct = Product(
            name = parsed.name.orEmpty(),
            barcode = parsed.code,
            qrCode = prefillCode,
            sellPrice = parsed.price ?: 0.0
        )
        showProductDialog = true
        onConsumePrefillCode()
    }

    val filteredProducts = remember(products, searchQuery, stockFilter, selectedTab) {
        products.filter { p ->
            val q = searchQuery.trim()
            val matchesSearch = q.isBlank() ||
                p.name.contains(q, ignoreCase = true) ||
                p.nameBn.contains(q, ignoreCase = true) ||
                p.barcode.contains(q, ignoreCase = true) ||
                p.category.contains(q, ignoreCase = true)
            val matchesStock = if (selectedTab == 1) {
                when (stockFilter) {
                    "LOW" -> p.stockQty > 0 && p.stockQty <= p.lowStockThreshold
                    "OUT" -> p.stockQty <= 0
                    else -> true
                }
            } else true
            matchesSearch && matchesStock
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(language.pick("পণ্য (${products.size})", "Products (${products.size})")) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(language.pick("স্টক", "Stock")) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(language.pick("ক্রয় (${purchases.size})", "Purchase (${purchases.size})")) }
                )
            }

            if (selectedTab == 0 || selectedTab == 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(language.pick("পণ্য বা বারকোড খুঁজুন...", "Search product or barcode...")) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.size(54.dp)
                    ) {
                        IconButton(
                            onClick = {
                                onScanCodeForProduct { scanned ->
                                    val match = products.firstOrNull {
                                        it.barcode.equals(scanned, true) || it.qrCode.equals(scanned, true)
                                    }
                                    if (match != null) {
                                        searchQuery = match.name
                                    } else {
                                        val parsed = PosBackupManager.parseQrCodePayload(scanned)
                                        editingProduct = Product(
                                            name = parsed.name.orEmpty(),
                                            barcode = parsed.code,
                                            qrCode = scanned,
                                            sellPrice = parsed.price ?: 0.0
                                        )
                                        showProductDialog = true
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = "Scan barcode or QR",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                if (selectedTab == 1) {
                    val totalCostVal = products.sumOf { it.stockQty * it.purchasePrice }
                    val totalRetailVal = products.sumOf { it.stockQty * it.sellPrice }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(language.pick("ক্রয়মূল্যে স্টক", "Cost Valuation"), style = MaterialTheme.typography.labelSmall)
                                Text(
                                    formatTaka(totalCostVal),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(language.pick("বিক্রয়মূল্যে স্টক", "Retail Valuation"), style = MaterialTheme.typography.labelSmall)
                                Text(
                                    formatTaka(totalRetailVal),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf("ALL" to "All", "LOW" to "Low Stock", "OUT" to "Out of Stock")) { (key, label) ->
                            FilterChip(
                                selected = stockFilter == key,
                                onClick = { stockFilter = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredProducts.isEmpty()) {
                        item {
                            EmptySectionCard(
                                icon = Icons.Filled.Inventory2,
                                message = language.pick("কোনো পণ্য পাওয়া যায়নি। + বাটনে ট্যাপ করে পণ্য যোগ করুন।", "No products yet. Tap + to add your first product.")
                            )
                        }
                    } else {
                        items(filteredProducts, key = { it.id }) { product ->
                            if (selectedTab == 0) {
                                ProductManageCard(
                                    product = product,
                                    language = language,
                                    onEdit = {
                                        editingProduct = product
                                        showProductDialog = true
                                    },
                                    onShowQr = { labelPreviewProduct = product },
                                    onDelete = { onDeleteProduct(product.id) }
                                )
                            } else {
                                StockControlCard(
                                    product = product,
                                    language = language,
                                    onAdjust = { newQty -> onAdjustStock(product.id, newQty) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Purchase tab
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (purchases.isEmpty()) {
                        item {
                            EmptySectionCard(
                                icon = Icons.Filled.AddShoppingCart,
                                message = language.pick("কোনো ক্রয় হিসাব নেই। + বাটনে ট্যাপ করে মালামাল ক্রয় যোগ করুন।", "No purchases recorded yet. Tap + to record a supplier purchase.")
                            )
                        }
                    } else {
                        items(purchases, key = { it.id }) { purchase ->
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${purchase.invoiceNumber} • ${purchase.supplierName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = formatTaka(purchase.totalAmount),
                                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = "${formatDateTime(purchase.purchaseEpochMs)} • ${purchase.items.size} items • ${purchase.paymentMethod}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (purchase.dueAmount > 0) {
                                        Text(
                                            text = language.pick("বাকি: ${formatTaka(purchase.dueAmount)}", "Due Payable: ${formatTaka(purchase.dueAmount)}"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusDanger
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (selectedTab == 2) {
                    showPurchaseDialog = true
                } else {
                    editingProduct = null
                    showProductDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("inventory_fab")
        ) {
            Icon(
                imageVector = if (selectedTab == 2) Icons.Filled.AddShoppingCart else Icons.Filled.Add,
                contentDescription = if (selectedTab == 2) "New Purchase" else "Add Product"
            )
        }
    }

    if (showProductDialog) {
        ProductEditorDialog(
            initial = editingProduct,
            language = language,
            onScanCode = onScanCodeForProduct,
            onSave = { prod ->
                onSaveProduct(prod)
                showProductDialog = false
            },
            onDismiss = { showProductDialog = false }
        )
    }

    if (labelPreviewProduct != null) {
        ProductBarcodeQrDialog(
            product = labelPreviewProduct!!,
            language = language,
            onDismiss = { labelPreviewProduct = null }
        )
    }

    if (showPurchaseDialog) {
        PurchaseEntryDialog(
            products = products,
            suppliers = suppliers,
            language = language,
            onRecordPurchase = { supplier, fallbackName, items, paid, method, note ->
                onRecordPurchase(supplier, fallbackName, items, paid, method, note)
                showPurchaseDialog = false
            },
            onDismiss = { showPurchaseDialog = false }
        )
    }
}

@Composable
private fun ProductManageCard(
    product: Product,
    language: AppLanguage,
    onEdit: () -> Unit,
    onShowQr: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (product.nameBn.isNotBlank()) "${product.nameBn} (${product.name})" else product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${product.category} • ${language.pick("স্টক", "Stock")}: ${formatQty(product.stockQty)} ${product.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${language.pick("ক্রয়", "Buy")}: ${formatTaka(product.purchasePrice)}  |  ${language.pick("বিক্রয়", "Sell")}: ${formatTaka(product.sellPrice)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row {
                IconButton(onClick = onShowQr) {
                    Icon(Icons.Filled.QrCode2, contentDescription = "View Barcode & QR")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit product")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete product", tint = StatusDanger)
                }
            }
        }
    }
}

@Composable
private fun StockControlCard(
    product: Product,
    language: AppLanguage,
    onAdjust: (Double) -> Unit
) {
    val statusColor = when {
        product.stockQty <= 0 -> StatusDanger
        product.stockQty <= product.lowStockThreshold -> StatusWarning
        else -> StatusSuccess
    }
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${language.pick("সতর্কতা সীমা", "Low alert")}: ${formatQty(product.lowStockThreshold)} ${product.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { onAdjust(max(0.0, product.stockQty - 1.0)) }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease stock")
                }
                Text(
                    text = "${formatQty(product.stockQty)} ${product.unit}",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                IconButton(onClick = { onAdjust(product.stockQty + 1.0) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase stock")
                }
            }
        }
    }
}

@Composable
private fun ProductEditorDialog(
    initial: Product?,
    language: AppLanguage,
    onScanCode: ((String) -> Unit) -> Unit,
    onSave: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var nameBn by remember { mutableStateOf(initial?.nameBn.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category?.ifBlank { "General" } ?: "General") }
    var unit by remember { mutableStateOf(initial?.unit?.ifBlank { "Pcs" } ?: "Pcs") }
    var barcode by remember { mutableStateOf(initial?.barcode.orEmpty()) }
    var qrCode by remember { mutableStateOf(initial?.qrCode.orEmpty()) }
    var purchasePrice by remember { mutableStateOf(initial?.purchasePrice?.takeIf { it > 0 }?.toString().orEmpty()) }
    var sellPrice by remember { mutableStateOf(initial?.sellPrice?.takeIf { it > 0 }?.toString().orEmpty()) }
    var stockQty by remember { mutableStateOf(initial?.stockQty?.toString() ?: "0") }
    var lowThreshold by remember { mutableStateOf(initial?.lowStockThreshold?.toString() ?: "5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial?.id.isNullOrBlank()) language.pick("নতুন পণ্য যোগ", "Add Product")
                else language.pick("পণ্য সম্পাদনা", "Edit Product")
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(language.pick("পণ্যের নাম (ইংরেজি)*", "Product Name (English)*")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                )
                OutlinedTextField(
                    value = nameBn,
                    onValueChange = { nameBn = it },
                    label = { Text(language.pick("বাংলা নাম (ঐচ্ছিক)", "Bengali Name (Optional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text(language.pick("ক্যাটাগরি", "Category")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text(language.pick("একক (Pcs/Kg)", "Unit")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { purchasePrice = it },
                        label = { Text(language.pick("ক্রয় মূল্য (৳)", "Buy Price")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellPrice,
                        onValueChange = { sellPrice = it },
                        label = { Text(language.pick("বিক্রয় মূল্য (৳)*", "Sell Price*")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_sell_price_input")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockQty,
                        onValueChange = { stockQty = it },
                        label = { Text(language.pick("স্টক পরিমাণ", "Stock Qty")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lowThreshold,
                        onValueChange = { lowThreshold = it },
                        label = { Text(language.pick("লো-স্টক সীমা", "Low Alert")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text(language.pick("বারকোড / SKU", "Barcode / SKU")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            onScanCode { code ->
                                val parsed = PosBackupManager.parseQrCodePayload(code)
                                barcode = parsed.code
                                if (qrCode.isBlank()) qrCode = code
                                if (name.isBlank() && !parsed.name.isNullOrBlank()) name = parsed.name
                            }
                        }
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan barcode into field")
                    }
                }
                OutlinedTextField(
                    value = qrCode,
                    onValueChange = { qrCode = it },
                    label = { Text(language.pick("QR কোড ডেটা (ঐচ্ছিক)", "QR Code Data (Optional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isNotBlank()) {
                        val effectiveBarcode = barcode.trim()
                        val effectiveQr = qrCode.trim().ifBlank {
                            if (effectiveBarcode.isNotBlank()) "FST:$effectiveBarcode|$cleanName|${sellPrice.toDoubleOrNull() ?: 0.0}"
                            else ""
                        }
                        onSave(
                            (initial ?: Product()).copy(
                                name = cleanName,
                                nameBn = nameBn.trim(),
                                category = category.ifBlank { "General" }.trim(),
                                unit = unit.ifBlank { "Pcs" }.trim(),
                                barcode = effectiveBarcode,
                                qrCode = effectiveQr,
                                purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                                sellPrice = sellPrice.toDoubleOrNull() ?: 0.0,
                                stockQty = stockQty.toDoubleOrNull() ?: 0.0,
                                lowStockThreshold = lowThreshold.toDoubleOrNull() ?: 5.0
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text(language.pick("সংরক্ষণ", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(language.pick("বাতিল", "Cancel"))
            }
        }
    )
}

@Composable
private fun ProductBarcodeQrDialog(
    product: Product,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val qrValue = product.qrCode.ifBlank {
        "FST:${product.barcode.ifBlank { product.id }}|${product.name}|${product.sellPrice}"
    }
    val barValue = product.barcode.ifBlank { product.id.take(12).ifBlank { "FST001" } }
    val qrBitmap = remember(qrValue) { QrBarcodeRenderer.generateQrBitmap(qrValue, 360) }
    val barBitmap = remember(barValue) { QrBarcodeRenderer.generateBarcodeBitmap(barValue, 520, 140) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${product.name} • ${formatTaka(product.sellPrice)}") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Product QR Code",
                        modifier = Modifier.size(180.dp)
                    )
                }
                if (barBitmap != null) {
                    Image(
                        bitmap = barBitmap.asImageBitmap(),
                        contentDescription = "Product Barcode",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                    )
                    Text(
                        text = barValue,
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(language.pick("ঠিক আছে", "Done"))
            }
        }
    )
}

@Composable
private fun PurchaseEntryDialog(
    products: List<Product>,
    suppliers: List<Supplier>,
    language: AppLanguage,
    onRecordPurchase: (Supplier?, String, List<PurchaseLineItem>, Double, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSupplier by remember { mutableStateOf<Supplier?>(suppliers.firstOrNull()) }
    var supplierFallback by remember { mutableStateOf("") }
    var showSupplierMenu by remember { mutableStateOf(false) }

    var selectedProduct by remember { mutableStateOf<Product?>(products.firstOrNull()) }
    var showProductMenu by remember { mutableStateOf(false) }
    var qtyText by remember { mutableStateOf("10") }
    var costText by remember { mutableStateOf(products.firstOrNull()?.purchasePrice?.toString().orEmpty()) }
    var items by remember { mutableStateOf<List<PurchaseLineItem>>(emptyList()) }

    val total = items.sumOf { it.lineTotal }
    var paidText by remember(total) { mutableStateOf(if (total > 0) total.toInt().toString() else "0") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.pick("মালামাল ক্রয় এন্ট্রি", "Record Purchase")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    OutlinedButton(onClick = { showSupplierMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedSupplier?.name ?: language.pick("সরবরাহকারী বাছাই করুন", "Select Supplier"))
                    }
                    DropdownMenu(expanded = showSupplierMenu, onDismissRequest = { showSupplierMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(language.pick("অন্যান্য / নগদ সরবরাহকারী", "Other / Walk-in Supplier")) },
                            onClick = {
                                selectedSupplier = null
                                showSupplierMenu = false
                            }
                        )
                        suppliers.forEach { sup ->
                            DropdownMenuItem(
                                text = { Text("${sup.name} (${sup.companyName})") },
                                onClick = {
                                    selectedSupplier = sup
                                    showSupplierMenu = false
                                }
                            )
                        }
                    }
                }
                if (selectedSupplier == null) {
                    OutlinedTextField(
                        value = supplierFallback,
                        onValueChange = { supplierFallback = it },
                        label = { Text(language.pick("সরবরাহকারীর নাম", "Supplier Name")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()

                if (products.isEmpty()) {
                    Text(
                        language.pick("প্রথমে পণ্য তালিকায় পণ্য যোগ করুন।", "Add products in the Products tab first."),
                        color = StatusDanger,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Box {
                        OutlinedButton(onClick = { showProductMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedProduct?.name ?: language.pick("পণ্য বাছাই করুন", "Select Product"))
                        }
                        DropdownMenu(expanded = showProductMenu, onDismissRequest = { showProductMenu = false }) {
                            products.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (Stock ${formatQty(p.stockQty)})") },
                                    onClick = {
                                        selectedProduct = p
                                        costText = p.purchasePrice.toString()
                                        showProductMenu = false
                                    }
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qtyText,
                            onValueChange = { qtyText = it },
                            label = { Text(language.pick("পরিমাণ", "Qty")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = costText,
                            onValueChange = { costText = it },
                            label = { Text(language.pick("ক্রয় দর", "Unit Cost")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val prod = selectedProduct ?: return@OutlinedButton
                            val q = qtyText.toDoubleOrNull() ?: 0.0
                            val c = costText.toDoubleOrNull() ?: prod.purchasePrice
                            if (q > 0) {
                                items = items + PurchaseLineItem(
                                    productId = prod.id,
                                    productName = prod.name,
                                    unit = prod.unit,
                                    qty = q,
                                    costPrice = c,
                                    lineTotal = q * c
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(language.pick("আইটেম যোগ করুন", "Add Item to Bill"))
                    }
                }

                items.forEach { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${line.productName} (${formatQty(line.qty)} ${line.unit})", style = MaterialTheme.typography.bodySmall)
                        Text(formatTaka(line.lineTotal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }

                if (items.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "${language.pick("মোট ক্রয়", "Total Purchase")}: ${formatTaka(total)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = paidText,
                        onValueChange = { paidText = it },
                        label = { Text(language.pick("পরিশোধিত টাকা (৳)", "Paid Amount (৳)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("CASH", "BKASH", "BANK", "DUE")) { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method) }
                            )
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paid = if (paymentMethod == "DUE") 0.0 else (paidText.toDoubleOrNull() ?: total)
                    onRecordPurchase(selectedSupplier, supplierFallback, items, paid, paymentMethod, note)
                },
                enabled = items.isNotEmpty()
            ) {
                Text(language.pick("ক্রয় সংরক্ষণ", "Save Purchase"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(language.pick("বাতিল", "Cancel"))
            }
        }
    )
}

@Composable
fun EmptySectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
