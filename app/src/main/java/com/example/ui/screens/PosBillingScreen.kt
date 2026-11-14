package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.formatQty
import com.example.ui.i18n.formatTaka
import com.example.ui.i18n.pick
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.CartItem
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosBillingScreen(
    products: List<Product>,
    customers: List<Customer>,
    cart: List<CartItem>,
    selectedCustomerId: String?,
    language: AppLanguage,
    isBusy: Boolean,
    onAddToCart: (Product) -> Unit,
    onUpdateQty: (String, Double) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onClearCart: () -> Unit,
    onSelectCustomer: (String?) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenAddProduct: () -> Unit,
    onCheckout: (discount: Double, paidAmount: Double, paymentMethod: String, walkInName: String, note: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCartSheet by remember { mutableStateOf(false) }

    val categories = remember(products) {
        listOf("All") + products.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchesCat = selectedCategory == "All" || p.category == selectedCategory
            val q = searchQuery.trim()
            val matchesSearch = q.isBlank() ||
                p.name.contains(q, ignoreCase = true) ||
                p.nameBn.contains(q, ignoreCase = true) ||
                p.barcode.contains(q, ignoreCase = true) ||
                p.qrCode.contains(q, ignoreCase = true) ||
                p.sku.contains(q, ignoreCase = true)
            matchesCat && matchesSearch
        }
    }

    val cartSubtotal = remember(cart) { cart.sumOf { it.lineTotal } }
    val cartCount = remember(cart) { cart.sumOf { it.qty } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Catalog Pane
            Box(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search & Scan bar
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
                            placeholder = {
                                Text(language.pick("পণ্য বা বারকোড খুঁজুন...", "Search product or barcode..."))
                            },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pos_search_input")
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.size(54.dp)
                        ) {
                            IconButton(
                                onClick = onOpenScanner,
                                modifier = Modifier.testTag("pos_scan_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = "Scan barcode or QR",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    // Category chips
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(if (cat == "All") language.pick("সব", "All") else cat) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (filteredProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = language.pick(
                                        "কোনো পণ্য পাওয়া যায়নি। নতুন পণ্য যোগ করুন!",
                                        "No products found. Add products to start billing!"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(onClick = onOpenAddProduct) {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(language.pick("পণ্য যোগ করুন", "Add Product"))
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 6.dp,
                                bottom = if (!isWideScreen && cart.isNotEmpty()) 96.dp else 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredProducts, key = { it.id }) { product ->
                                val inCartQty = cart.firstOrNull { it.product.id == product.id }?.qty ?: 0.0
                                PosProductRow(
                                    product = product,
                                    inCartQty = inCartQty,
                                    language = language,
                                    onAdd = { onAddToCart(product) }
                                )
                            }
                        }
                    }
                }

                // Compact Bottom Cart Summary Bar
                if (!isWideScreen && cart.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.large,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(14.dp)
                            .clickable { showCartSheet = true }
                            .testTag("pos_view_cart_bar")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Column {
                                    Text(
                                        text = "${formatQty(cartCount)} ${language.pick("টি পণ্য", "items")}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Text(
                                        text = language.pick("বিল ও মেমো তৈরি করুন", "Tap to Checkout & Memo"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            Text(
                                text = formatTaka(cartSubtotal),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            // Expanded Screen Side Cart Pane
            if (isWideScreen) {
                Surface(
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    CartCheckoutContent(
                        cart = cart,
                        customers = customers,
                        selectedCustomerId = selectedCustomerId,
                        language = language,
                        isBusy = isBusy,
                        onUpdateQty = onUpdateQty,
                        onRemoveFromCart = onRemoveFromCart,
                        onClearCart = onClearCart,
                        onSelectCustomer = onSelectCustomer,
                        onCheckout = { disc, paid, method, walkIn, note ->
                            onCheckout(disc, paid, method, walkIn, note)
                        }
                    )
                }
            }
        }
    }

    if (showCartSheet) {
        ModalBottomSheet(onDismissRequest = { showCartSheet = false }) {
            CartCheckoutContent(
                cart = cart,
                customers = customers,
                selectedCustomerId = selectedCustomerId,
                language = language,
                isBusy = isBusy,
                onUpdateQty = onUpdateQty,
                onRemoveFromCart = onRemoveFromCart,
                onClearCart = {
                    onClearCart()
                    showCartSheet = false
                },
                onSelectCustomer = onSelectCustomer,
                onCheckout = { disc, paid, method, walkIn, note ->
                    onCheckout(disc, paid, method, walkIn, note)
                    showCartSheet = false
                }
            )
        }
    }
}

@Composable
private fun PosProductRow(
    product: Product,
    inCartQty: Double,
    language: AppLanguage,
    onAdd: () -> Unit
) {
    val isLowStock = product.stockQty <= product.lowStockThreshold
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAdd)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (product.nameBn.isNotBlank() && language != AppLanguage.ENGLISH) {
                        "${product.nameBn} • ${product.name}"
                    } else product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${language.pick("স্টক", "Stock")}: ${formatQty(product.stockQty)} ${product.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (product.stockQty <= 0) StatusDanger else if (isLowStock) MaterialTheme.colorScheme.secondary else StatusSuccess
                    )
                    if (product.barcode.isNotBlank()) {
                        Text(
                            text = "#${product.barcode}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatTaka(product.sellPrice),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = if (inCartQty > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add to bill",
                            tint = if (inCartQty > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        if (inCartQty > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatQty(inCartQty),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartCheckoutContent(
    cart: List<CartItem>,
    customers: List<Customer>,
    selectedCustomerId: String?,
    language: AppLanguage,
    isBusy: Boolean,
    onUpdateQty: (String, Double) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onClearCart: () -> Unit,
    onSelectCustomer: (String?) -> Unit,
    onCheckout: (discount: Double, paidAmount: Double, paymentMethod: String, walkInName: String, note: String) -> Unit
) {
    val subtotal = remember(cart) { cart.sumOf { it.lineTotal } }
    var discountInput by remember { mutableStateOf("") }
    val discount = (discountInput.toDoubleOrNull() ?: 0.0).coerceIn(0.0, subtotal)
    val netTotal = max(0.0, subtotal - discount)

    var paidInput by remember(netTotal) { mutableStateOf(if (netTotal > 0) netTotal.toInt().toString() else "0") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var walkInName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showCustomerMenu by remember { mutableStateOf(false) }

    val selectedCustomer = customers.firstOrNull { it.id == selectedCustomerId }
    val paidAmount = if (paymentMethod == "DUE") 0.0 else (paidInput.toDoubleOrNull() ?: netTotal).coerceIn(0.0, netTotal)
    val dueAmount = max(0.0, netTotal - paidAmount)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.pick("বিল ও ক্যাশ মেমো", "Current Bill & Checkout"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (cart.isNotEmpty()) {
                OutlinedButton(onClick = onClearCart) {
                    Text(language.pick("মুছুন", "Clear"))
                }
            }
        }

        if (cart.isEmpty()) {
            Text(
                text = language.pick("কার্টে কোনো পণ্য নেই।", "Cart is empty. Tap products to add."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
            return@Column
        }

        // Cart lines
        cart.forEach { item ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.product.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${formatTaka(item.product.sellPrice)} × ${formatQty(item.qty)} = ${formatTaka(item.lineTotal)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onUpdateQty(item.product.id, item.qty - 1.0) }) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease qty")
                        }
                        Text(
                            text = formatQty(item.qty),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { onUpdateQty(item.product.id, item.qty + 1.0) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase qty")
                        }
                        IconButton(onClick = { onRemoveFromCart(item.product.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove item", tint = StatusDanger)
                        }
                    }
                }
            }
        }

        // Customer picker
        Box {
            OutlinedButton(
                onClick = { showCustomerMenu = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Person, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedCustomer?.let { "${it.name} (Due ${formatTaka(it.totalDue)})" }
                        ?: language.pick("কাস্টমার: ওয়াক-ইন / নির্বাচন করুন", "Customer: Walk-in (Tap to select)")
                )
            }
            DropdownMenu(
                expanded = showCustomerMenu,
                onDismissRequest = { showCustomerMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(language.pick("ওয়াক-ইন কাস্টমার", "Walk-in Customer")) },
                    onClick = {
                        onSelectCustomer(null)
                        showCustomerMenu = false
                    }
                )
                customers.forEach { c ->
                    DropdownMenuItem(
                        text = { Text("${c.name} (${c.phone})") },
                        onClick = {
                            onSelectCustomer(c.id)
                            showCustomerMenu = false
                        }
                    )
                }
            }
        }

        if (selectedCustomer == null) {
            OutlinedTextField(
                value = walkInName,
                onValueChange = { walkInName = it },
                label = { Text(language.pick("ক্রেতার নাম (ঐচ্ছিক)", "Customer Name (Optional)")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = discountInput,
                onValueChange = { discountInput = it },
                label = { Text(language.pick("ছাড় (৳)", "Discount (৳)")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = if (paymentMethod == "DUE") "0" else paidInput,
                onValueChange = { paidInput = it },
                enabled = paymentMethod != "DUE",
                label = { Text(language.pick("জমা (৳)", "Paid (৳)")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        // Payment Method chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val methods = listOf("CASH" to "Cash", "BKASH" to "bKash", "NAGAD" to "Nagad", "BANK" to "Bank", "DUE" to "Due/বাকি")
            items(methods) { (code, label) ->
                FilterChip(
                    selected = paymentMethod == code,
                    onClick = { paymentMethod = code },
                    label = { Text(label) }
                )
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(language.pick("নোট (ঐচ্ছিক)", "Note (Optional)")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(language.pick("সর্বমোট বিল", "Net Total"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = formatTaka(netTotal),
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (dueAmount > 0.0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(language.pick("বাকি থাকবে", "Due Balance"), color = StatusDanger, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = formatTaka(dueAmount),
                    color = StatusDanger,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily)
                )
            }
        }

        Button(
            onClick = { onCheckout(discount, paidAmount, paymentMethod, walkInName, note) },
            enabled = !isBusy,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("pos_checkout_button")
        ) {
            Icon(Icons.Filled.ShoppingCartCheckout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = language.pick("বিল সম্পন্ন ও ক্যাশ মেমো তৈরি করুন", "Complete Sale & Cash Memo"),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
