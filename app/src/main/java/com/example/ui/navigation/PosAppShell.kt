package com.example.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.pick
import com.example.ui.i18n.shortPick
import com.example.ui.scanner.BarcodeQrScannerDialog
import com.example.ui.screens.CashMemoDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.PartiesDueScreen
import com.example.ui.screens.PosBillingScreen
import com.example.ui.screens.ReportsBackupScreen
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute

@Serializable
data object PosRoute

@Serializable
data class InventoryRoute(val initialTab: Int = 0, val prefillCode: String? = null)

@Serializable
data class PartiesRoute(val initialTab: Int = 0)

@Serializable
data class ReportsRoute(val initialTab: Int = 0)

private data class NavTabItem(
    val route: Any,
    val labelBn: String,
    val labelEn: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosAppShell(
    currentUserId: String,
    ownerName: String,
    ownerEmail: String
) {
    val vm: PosViewModel = viewModel(
        key = currentUserId,
        factory = PosViewModel.provideFactory(
            currentUserId = currentUserId,
            ownerName = ownerName,
            ownerEmail = ownerEmail
        )
    )

    val state by vm.uiState.collectAsStateWithLifecycle()
    val cart by vm.cart.collectAsStateWithLifecycle()
    val selectedCustomerId by vm.selectedCustomerId.collectAsStateWithLifecycle()
    val statusMessage by vm.statusMessage.collectAsStateWithLifecycle()
    val latestMemoSale by vm.latestSaleForMemo.collectAsStateWithLifecycle()
    val isBusy by vm.isBusy.collectAsStateWithLifecycle()
    val languageOverride by vm.languageOverride.collectAsStateWithLifecycle()

    val language = languageOverride ?: AppLanguage.fromCode(state.shopProfile.preferredLanguage)

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var scannerCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var pendingPrefillCode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.clearStatusMessage()
        }
    }

    val navItems = remember {
        listOf(
            NavTabItem(DashboardRoute, "হোম", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
            NavTabItem(PosRoute, "বিক্রি", "POS", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale, "nav_pos"),
            NavTabItem(InventoryRoute(0), "পণ্য/ক্রয়", "Stock", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "nav_inventory"),
            NavTabItem(PartiesRoute(0), "বাকি/হিসাব", "Dues", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_parties"),
            NavTabItem(ReportsRoute(0), "রিপোর্ট", "Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment, "nav_reports")
        )
    }

    fun navigateTop(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openSmartScannerForPos() {
        scannerCallback = { scanned ->
            val found = vm.addProductToCartByCode(scanned)
            if (found != null) {
                navigateTop(PosRoute)
            } else {
                pendingPrefillCode = scanned
                scope.launch {
                    snackbarHostState.showSnackbar(
                        language.pick(
                            "পণ্য পাওয়া যায়নি — নতুন পণ্য হিসেবে যোগ করুন",
                            "Product not found — opening Add Product with code"
                        )
                    )
                }
                navController.navigate(InventoryRoute(initialTab = 0, prefillCode = scanned))
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Column {
                            Text(
                                text = state.shopProfile.businessName.ifBlank { "Fahim Siam Traders" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = language.pick("বিজনেস ও POS সফটওয়্যার", "Business & POS Suite"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        AssistChip(
                            onClick = { vm.cycleLanguage() },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Translate,
                                    contentDescription = "Switch Language",
                                    modifier = Modifier.padding(0.dp)
                                )
                            },
                            label = { Text(language.badge) },
                            modifier = Modifier.testTag("language_toggle_chip")
                        )
                        IconButton(
                            onClick = { openSmartScannerForPos() },
                            modifier = Modifier.testTag("topbar_scan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = "Scan Barcode or QR Code",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        navItems.forEachIndexed { idx, item ->
                            val selected = when (idx) {
                                0 -> currentDest?.hasRoute<DashboardRoute>() == true
                                1 -> currentDest?.hasRoute<PosRoute>() == true
                                2 -> currentDest?.hasRoute<InventoryRoute>() == true
                                3 -> currentDest?.hasRoute<PartiesRoute>() == true
                                else -> currentDest?.hasRoute<ReportsRoute>() == true
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTop(item.route) },
                                icon = {
                                    val iconVec = if (selected) item.selectedIcon else item.unselectedIcon
                                    if (idx == 1 && cart.isNotEmpty()) {
                                        BadgedBox(badge = { Badge { Text("${cart.size}") } }) {
                                            Icon(iconVec, contentDescription = item.labelEn)
                                        }
                                    } else {
                                        Icon(iconVec, contentDescription = item.labelEn)
                                    }
                                },
                                label = {
                                    Text(
                                        text = language.shortPick(item.labelBn, item.labelEn),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        navItems.forEachIndexed { idx, item ->
                            val selected = when (idx) {
                                0 -> currentDest?.hasRoute<DashboardRoute>() == true
                                1 -> currentDest?.hasRoute<PosRoute>() == true
                                2 -> currentDest?.hasRoute<InventoryRoute>() == true
                                3 -> currentDest?.hasRoute<PartiesRoute>() == true
                                else -> currentDest?.hasRoute<ReportsRoute>() == true
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTop(item.route) },
                                icon = {
                                    Icon(
                                        if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.labelEn
                                    )
                                },
                                label = { Text(language.shortPick(item.labelBn, item.labelEn)) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (state.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(language.pick("দোকানের তথ্য লোড হচ্ছে...", "Loading Fahim Siam Traders POS..."))
                            }
                        }
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = DashboardRoute
                        ) {
                            composable<DashboardRoute> {
                                DashboardScreen(
                                    state = state,
                                    language = language,
                                    onOpenPos = { navigateTop(PosRoute) },
                                    onOpenScanner = { openSmartScannerForPos() },
                                    onOpenPurchase = { navController.navigate(InventoryRoute(initialTab = 2)) },
                                    onOpenExpenses = { navController.navigate(PartiesRoute(initialTab = 3)) },
                                    onOpenDues = { navController.navigate(PartiesRoute(initialTab = 0)) },
                                    onOpenBackup = { navController.navigate(ReportsRoute(initialTab = 2)) },
                                    onOpenMemo = { sale -> vm.openMemoDialog(sale) }
                                )
                            }

                            composable<PosRoute> {
                                PosBillingScreen(
                                    products = state.products,
                                    customers = state.customers,
                                    cart = cart,
                                    selectedCustomerId = selectedCustomerId,
                                    language = language,
                                    isBusy = isBusy,
                                    onAddToCart = { vm.addToCart(it) },
                                    onUpdateQty = { id, qty -> vm.updateCartQty(id, qty) },
                                    onRemoveFromCart = { vm.removeFromCart(it) },
                                    onClearCart = { vm.clearCart() },
                                    onSelectCustomer = { vm.selectCustomer(it) },
                                    onOpenScanner = { openSmartScannerForPos() },
                                    onOpenAddProduct = { navController.navigate(InventoryRoute(initialTab = 0)) },
                                    onCheckout = { disc, paid, method, walkIn, note ->
                                        vm.completeSale(disc, paid, method, walkIn, note)
                                    }
                                )
                            }

                            composable<InventoryRoute> { entry ->
                                val args = entry.toRoute<InventoryRoute>()
                                InventoryScreen(
                                    products = state.products,
                                    suppliers = state.suppliers,
                                    purchases = state.purchases,
                                    initialTab = args.initialTab,
                                    prefillCode = pendingPrefillCode ?: args.prefillCode,
                                    onConsumePrefillCode = { pendingPrefillCode = null },
                                    language = language,
                                    onSaveProduct = { vm.saveProduct(it) },
                                    onDeleteProduct = { vm.deleteProduct(it) },
                                    onAdjustStock = { id, qty -> vm.adjustStock(id, qty) },
                                    onRecordPurchase = { sup, fallback, items, paid, method, note ->
                                        vm.recordPurchase(sup, fallback, items, paid, method, note)
                                    },
                                    onScanCodeForProduct = { callback ->
                                        scannerCallback = callback
                                    }
                                )
                            }

                            composable<PartiesRoute> { entry ->
                                val args = entry.toRoute<PartiesRoute>()
                                PartiesDueScreen(
                                    customers = state.customers,
                                    suppliers = state.suppliers,
                                    duePayments = state.duePayments,
                                    expenses = state.expenses,
                                    initialTab = args.initialTab,
                                    language = language,
                                    onSaveCustomer = { vm.saveCustomer(it) },
                                    onDeleteCustomer = { vm.deleteCustomer(it) },
                                    onSaveSupplier = { vm.saveSupplier(it) },
                                    onDeleteSupplier = { vm.deleteSupplier(it) },
                                    onCollectCustomerDue = { cust, amt, method, note ->
                                        vm.collectCustomerDue(cust, amt, method, note)
                                    },
                                    onPaySupplierDue = { sup, amt, method, note ->
                                        vm.paySupplierDue(sup, amt, method, note)
                                    },
                                    onSaveExpense = { vm.saveExpense(it) },
                                    onDeleteExpense = { vm.deleteExpense(it) }
                                )
                            }

                            composable<ReportsRoute> { entry ->
                                val args = entry.toRoute<ReportsRoute>()
                                ReportsBackupScreen(
                                    state = state,
                                    initialTab = args.initialTab,
                                    language = language,
                                    onOpenMemo = { sale -> vm.openMemoDialog(sale) },
                                    onSaveShopProfile = { vm.saveShopProfile(it) },
                                    onExportBackupJson = { vm.exportBackupJson() },
                                    onRestoreBackupJson = { vm.restoreBackupJson(it) },
                                    onBackupStatus = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (scannerCallback != null) {
        BarcodeQrScannerDialog(
            language = language,
            onCodeScanned = { code ->
                val cb = scannerCallback
                scannerCallback = null
                cb?.invoke(code)
            },
            onDismiss = { scannerCallback = null }
        )
    }

    if (latestMemoSale != null) {
        CashMemoDialog(
            sale = latestMemoSale!!,
            shop = state.shopProfile,
            initialMode = state.shopProfile.preferredLanguage.ifBlank { "both" },
            onDismiss = { vm.dismissMemoDialog() }
        )
    }
}
