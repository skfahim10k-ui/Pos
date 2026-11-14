package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Sale
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.formatDateTime
import com.example.ui.i18n.formatQty
import com.example.ui.i18n.formatTaka
import com.example.ui.i18n.pick
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.PosDashboardUiState
import java.util.Calendar

@Composable
fun DashboardScreen(
    state: PosDashboardUiState,
    language: AppLanguage,
    onOpenPos: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenPurchase: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenDues: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenMemo: (Sale) -> Unit
) {
    val metrics = remember(state.sales, state.expenses, state.products, state.customers, state.suppliers) {
        val cal = Calendar.getInstance()
        val todayYear = cal.get(Calendar.YEAR)
        val todayDay = cal.get(Calendar.DAY_OF_YEAR)
        val thisMonth = cal.get(Calendar.MONTH)

        var dailySales = 0.0
        var monthlySales = 0.0
        var dailyProfit = 0.0
        var dailyExpenses = 0.0
        var monthlyExpenses = 0.0

        state.sales.forEach { sale ->
            cal.timeInMillis = sale.saleEpochMs
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val d = cal.get(Calendar.DAY_OF_YEAR)
            if (y == todayYear && m == thisMonth) {
                monthlySales += sale.totalAmount
            }
            if (y == todayYear && d == todayDay) {
                dailySales += sale.totalAmount
                dailyProfit += (sale.totalAmount - sale.totalCost)
            }
        }
        state.expenses.forEach { exp ->
            cal.timeInMillis = exp.expenseEpochMs
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val d = cal.get(Calendar.DAY_OF_YEAR)
            if (y == todayYear && m == thisMonth) monthlyExpenses += exp.amount
            if (y == todayYear && d == todayDay) dailyExpenses += exp.amount
        }

        val totalCustomerDue = state.customers.sumOf { it.totalDue }
        val totalSupplierDue = state.suppliers.sumOf { it.totalDue }
        val lowStock = state.products.filter { it.stockQty <= it.lowStockThreshold }
        DashboardMetrics(
            dailySales = dailySales,
            monthlySales = monthlySales,
            dailyNetProfit = dailyProfit - dailyExpenses,
            dailyExpenses = dailyExpenses,
            monthlyExpenses = monthlyExpenses,
            customerDue = totalCustomerDue,
            supplierDue = totalSupplierDue,
            lowStockCount = lowStock.size,
            stockValue = state.products.sumOf { it.stockQty * it.purchasePrice }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Store Branded Hero Card
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    Color(0xFF146B53)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CloudDone,
                                        contentDescription = null,
                                        tint = Color(0xFF9DF5CC),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = language.pick("ক্লাউড সিঙ্ক সক্রিয়", "Cloud Synced"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.shopProfile.businessName.ifBlank { "Fahim Siam Traders" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = language.pick(
                                    "আজকের বিক্রি: ${formatTaka(metrics.dailySales)}",
                                    "Today's Sales: ${formatTaka(metrics.dailySales)}"
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE68A)
                            )
                        }
                        Image(
                            painter = painterResource(id = R.drawable.img_store_hero),
                            contentDescription = "Store Overview Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }
            }
        }

        // KPI 2x2 Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = language.pick("দৈনিক বিক্রি", "Daily Sales"),
                        value = formatTaka(metrics.dailySales),
                        subtitle = language.pick("নিট লাভ: ${formatTaka(metrics.dailyNetProfit)}", "Net: ${formatTaka(metrics.dailyNetProfit)}"),
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        accent = StatusSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = language.pick("মাসিক বিক্রি", "Monthly Sales"),
                        value = formatTaka(metrics.monthlySales),
                        subtitle = language.pick("খরচ: ${formatTaka(metrics.monthlyExpenses)}", "Exp: ${formatTaka(metrics.monthlyExpenses)}"),
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = language.pick("কাস্টমার বাকি", "Receivable Due"),
                        value = formatTaka(metrics.customerDue),
                        subtitle = language.pick("দেনা: ${formatTaka(metrics.supplierDue)}", "Payable: ${formatTaka(metrics.supplierDue)}"),
                        icon = Icons.Filled.AccountBalanceWallet,
                        accent = if (metrics.customerDue > 0) StatusDanger else StatusSuccess,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenDues)
                    )
                    KpiCard(
                        title = language.pick("মজুদ মূল্য", "Stock Value"),
                        value = formatTaka(metrics.stockValue),
                        subtitle = language.pick("${state.products.size} টি পণ্য • ${metrics.lowStockCount} কম", "${state.products.size} items • ${metrics.lowStockCount} low"),
                        icon = Icons.Filled.Inventory2,
                        accent = if (metrics.lowStockCount > 0) StatusWarning else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Low stock alert banner
        if (metrics.lowStockCount > 0) {
            item {
                val lowNames = state.products
                    .filter { it.stockQty <= it.lowStockThreshold }
                    .take(3)
                    .joinToString(", ") { "${it.name} (${formatQty(it.stockQty)})" }
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = "Low Stock Warning",
                            tint = StatusWarning
                        )
                        Text(
                            text = language.pick(
                                "কম স্টক সতর্কতা (${metrics.lowStockCount}): $lowNames",
                                "Low Stock Alert (${metrics.lowStockCount}): $lowNames"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF78350F),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Quick Actions Grid
        item {
            Text(
                text = language.pick("দ্রুত কাজ", "Quick Actions"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionTile(
                    icon = Icons.Filled.PointOfSale,
                    label = language.pick("নতুন বিক্রি", "New POS"),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    onClick = onOpenPos,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_pos_button")
                )
                QuickActionTile(
                    icon = Icons.Filled.QrCodeScanner,
                    label = language.pick("স্ক্যান করুন", "Scan Code"),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.secondary,
                    onClick = onOpenScanner,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_scan_button")
                )
                QuickActionTile(
                    icon = Icons.Filled.AddShoppingCart,
                    label = language.pick("ক্রয় এন্ট্রি", "Purchase"),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = onOpenPurchase,
                    modifier = Modifier.weight(1f)
                )
                QuickActionTile(
                    icon = Icons.Filled.Payments,
                    label = language.pick("খরচ/ব্যাকআপ", "Exp/Backup"),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onOpenExpenses,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent Cash Memos
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.pick("সাম্প্রতিক ক্যাশ মেমো", "Recent Cash Memos"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onOpenBackup) {
                    Text(language.pick("ব্যাকআপ ও রিপোর্ট", "Reports & Backup"))
                }
            }
        }

        if (state.sales.isEmpty()) {
            item {
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = language.pick(
                                "এখনো কোনো বিক্রি রেকর্ড হয়নি। প্রথম ক্যাশ মেমো তৈরি করতে POS শুরু করুন!",
                                "No sales recorded yet. Open POS Billing to create your first Cash Memo!"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(state.sales.take(6), key = { it.id }) { sale ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenMemo(sale) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${sale.memoNumber} • ${sale.customerName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatDateTime(sale.saleEpochMs)} • ${sale.items.size} items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatTaka(sale.totalAmount),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (sale.dueAmount > 0) {
                                Text(
                                    text = "Due ${formatTaka(sale.dueAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusDanger
                                )
                            } else {
                                Text(
                                    text = "Paid • ${sale.paymentMethod}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusSuccess
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class DashboardMetrics(
    val dailySales: Double,
    val monthlySales: Double,
    val dailyNetProfit: Double,
    val dailyExpenses: Double,
    val monthlyExpenses: Double,
    val customerDue: Double,
    val supplierDue: Double,
    val lowStockCount: Int,
    val stockValue: Double
)

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.14f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .height(82.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
