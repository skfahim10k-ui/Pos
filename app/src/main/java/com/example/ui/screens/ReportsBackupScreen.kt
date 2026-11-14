package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.model.Sale
import com.example.data.model.ShopProfile
import com.example.ui.auth.signOutUser
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.formatDateTime
import com.example.ui.i18n.formatQty
import com.example.ui.i18n.formatTaka
import com.example.ui.i18n.pick
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.PosDashboardUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsBackupScreen(
    state: PosDashboardUiState,
    initialTab: Int = 0,
    language: AppLanguage,
    onOpenMemo: (Sale) -> Unit,
    onSaveShopProfile: (ShopProfile) -> Unit,
    onExportBackupJson: () -> String,
    onRestoreBackupJson: (String) -> Unit,
    onBackupStatus: (String) -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(language.pick("রিপোর্ট", "Reports")) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(language.pick("ক্যাশ মেমো", "Cash Memos")) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(language.pick("ব্যাকআপ ও সেটিংস", "Backup & Shop")) }
            )
        }

        when (selectedTab) {
            0 -> ReportsTab(state = state, language = language)
            1 -> CashMemosArchiveTab(sales = state.sales, language = language, onOpenMemo = onOpenMemo)
            else -> BackupAndSettingsTab(
                state = state,
                language = language,
                onSaveShopProfile = onSaveShopProfile,
                onExportBackupJson = onExportBackupJson,
                onRestoreBackupJson = onRestoreBackupJson,
                onBackupStatus = onBackupStatus
            )
        }
    }
}

@Composable
private fun ReportsTab(
    state: PosDashboardUiState,
    language: AppLanguage
) {
    var period by remember { mutableStateOf("DAILY") } // DAILY, MONTHLY, YEARLY

    val summary = remember(state.sales, state.expenses, state.purchases, period) {
        val nowCal = Calendar.getInstance()
        val nowYear = nowCal.get(Calendar.YEAR)
        val nowMonth = nowCal.get(Calendar.MONTH)
        val nowDay = nowCal.get(Calendar.DAY_OF_YEAR)

        fun matchesPeriod(epochMs: Long): Boolean {
            if (epochMs <= 0) return false
            val c = Calendar.getInstance().apply { timeInMillis = epochMs }
            return when (period) {
                "DAILY" -> c.get(Calendar.YEAR) == nowYear && c.get(Calendar.DAY_OF_YEAR) == nowDay
                "MONTHLY" -> c.get(Calendar.YEAR) == nowYear && c.get(Calendar.MONTH) == nowMonth
                else -> c.get(Calendar.YEAR) == nowYear
            }
        }

        val periodSales = state.sales.filter { matchesPeriod(it.saleEpochMs) }
        val periodExpenses = state.expenses.filter { matchesPeriod(it.expenseEpochMs) }
        val periodPurchases = state.purchases.filter { matchesPeriod(it.purchaseEpochMs) }

        val revenue = periodSales.sumOf { it.totalAmount }
        val cogs = periodSales.sumOf { it.totalCost }
        val grossProfit = revenue - cogs
        val expTotal = periodExpenses.sumOf { it.amount }
        val netProfit = grossProfit - expTotal
        val collected = periodSales.sumOf { it.paidAmount }
        val dueIssued = periodSales.sumOf { it.dueAmount }
        val purchaseTotal = periodPurchases.sumOf { it.totalAmount }

        val topProducts = periodSales
            .flatMap { it.items }
            .groupBy { it.productName }
            .map { (name, lines) -> Triple(name, lines.sumOf { it.qty }, lines.sumOf { it.lineTotal }) }
            .sortedByDescending { it.third }
            .take(5)

        ReportSummary(
            txCount = periodSales.size,
            revenue = revenue,
            cogs = cogs,
            grossProfit = grossProfit,
            expenses = expTotal,
            netProfit = netProfit,
            collected = collected,
            dueIssued = dueIssued,
            purchaseTotal = purchaseTotal,
            topProducts = topProducts
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "DAILY" to language.pick("দৈনিক", "Daily"),
                    "MONTHLY" to language.pick("মাসিক", "Monthly"),
                    "YEARLY" to language.pick("বার্ষিক", "Yearly")
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = period == key,
                        onClick = { period = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        item {
            Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        language.pick("আয়-ব্যয় বিশ্লেষণ", "Sales vs Expense & Profit"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Custom Canvas Comparison Bar Chart
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val values = listOf(
                            summary.revenue.coerceAtLeast(0.0),
                            summary.purchaseTotal.coerceAtLeast(0.0),
                            summary.expenses.coerceAtLeast(0.0),
                            summary.netProfit.coerceAtLeast(0.0)
                        )
                        val colors = listOf(
                            Color(0xFF0D4A3A),
                            Color(0xFF1E6B7B),
                            Color(0xFFDC2626),
                            Color(0xFFD97706)
                        )
                        val maxVal = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
                        val barWidth = size.width / (values.size * 2f)
                        values.forEachIndexed { idx, v ->
                            val barH = ((v / maxVal) * (size.height - 20f)).toFloat()
                            val x = (idx * 2f + 0.5f) * barWidth
                            drawRoundRect(
                                color = colors[idx],
                                topLeft = Offset(x, size.height - barH),
                                size = Size(barWidth, barH.coerceAtLeast(6f)),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Text(language.pick("বিক্রি", "Sales"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF0D4A3A))
                        Text(language.pick("ক্রয়", "Purchase"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E6B7B))
                        Text(language.pick("খরচ", "Expense"), style = MaterialTheme.typography.labelSmall, color = Color(0xFFDC2626))
                        Text(language.pick("লাভ", "Profit"), style = MaterialTheme.typography.labelSmall, color = Color(0xFFD97706))
                    }
                }
            }
        }

        item {
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportLine(language.pick("মোট মেমো সংখ্যা", "Total Invoices"), "${summary.txCount}")
                    ReportLine(language.pick("মোট বিক্রি", "Total Sales"), formatTaka(summary.revenue), bold = true)
                    ReportLine(language.pick("নগদ/জমা আদায়", "Cash Collected"), formatTaka(summary.collected), color = StatusSuccess)
                    ReportLine(language.pick("নতুন বাকি", "Due Issued"), formatTaka(summary.dueIssued), color = StatusDanger)
                    ReportLine(language.pick("পণ্যের ক্রয়মূল্য (COGS)", "Cost of Goods"), formatTaka(summary.cogs))
                    ReportLine(language.pick("মোট লাভ (Gross Profit)", "Gross Profit"), formatTaka(summary.grossProfit))
                    ReportLine(language.pick("দৈনিক খরচ", "Operating Expenses"), formatTaka(summary.expenses), color = StatusDanger)
                    HorizontalDivider()
                    ReportLine(
                        language.pick("নিট লাভ (Net Profit)", "Net Profit"),
                        formatTaka(summary.netProfit),
                        bold = true,
                        color = if (summary.netProfit >= 0) StatusSuccess else StatusDanger
                    )
                }
            }
        }

        if (summary.topProducts.isNotEmpty()) {
            item {
                Text(
                    language.pick("সেরা বিক্রিত পণ্য", "Top Selling Products"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(summary.topProducts) { (name, qty, total) ->
                Card(shape = MaterialTheme.shapes.small, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$name (${formatQty(qty)})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            formatTaka(total),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private data class ReportSummary(
    val txCount: Int,
    val revenue: Double,
    val cogs: Double,
    val grossProfit: Double,
    val expenses: Double,
    val netProfit: Double,
    val collected: Double,
    val dueIssued: Double,
    val purchaseTotal: Double,
    val topProducts: List<Triple<String, Double, Double>>
)

@Composable
private fun ReportLine(label: String, value: String, bold: Boolean = false, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun CashMemosArchiveTab(
    sales: List<Sale>,
    language: AppLanguage,
    onOpenMemo: (Sale) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (sales.isEmpty()) {
            item {
                EmptySectionCard(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    message = language.pick("কোনো ক্যাশ মেমো তৈরি হয়নি।", "No cash memos generated yet.")
                )
            }
        } else {
            items(sales, key = { it.id }) { sale ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenMemo(sale) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${sale.memoNumber} • ${sale.customerName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "${formatDateTime(sale.saleEpochMs)} • ${sale.items.size} items • ${sale.paymentMethod}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatTaka(sale.totalAmount),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                language.pick("মেমো দেখুন", "View Memo"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupAndSettingsTab(
    state: PosDashboardUiState,
    language: AppLanguage,
    onSaveShopProfile: (ShopProfile) -> Unit,
    onExportBackupJson: () -> String,
    onRestoreBackupJson: (String) -> Unit,
    onBackupStatus: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val jsonText = onExportBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(jsonText.toByteArray(Charsets.UTF_8))
                }
                onBackupStatus("ব্যাকআপ ফাইল সংরক্ষিত হয়েছে / Backup exported successfully")
            } catch (e: Exception) {
                onBackupStatus("Backup export failed: ${e.localizedMessage}")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    onRestoreBackupJson(content)
                }
            } catch (e: Exception) {
                onBackupStatus("Backup restore failed: ${e.localizedMessage}")
            }
        }
    }

    var businessName by remember(state.shopProfile.businessName) { mutableStateOf(state.shopProfile.businessName) }
    var phone by remember(state.shopProfile.phone) { mutableStateOf(state.shopProfile.phone) }
    var address by remember(state.shopProfile.address) { mutableStateOf(state.shopProfile.address) }
    var footerBn by remember(state.shopProfile.receiptFooterBn) { mutableStateOf(state.shopProfile.receiptFooterBn) }
    var footerEn by remember(state.shopProfile.receiptFooterEn) { mutableStateOf(state.shopProfile.receiptFooterEn) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cloud + Local Backup Card
        item {
            Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(language.pick("ডেটা সংরক্ষণ ও ব্যাকআপ", "Data Cloud Sync & Backup"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Surface(
                        color = StatusSuccess.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.CloudDone, contentDescription = null, tint = StatusSuccess)
                            Text(
                                text = language.pick(
                                    "Firebase ক্লাউডে রিয়েল-টাইম সংরক্ষিত (${state.products.size} পণ্য, ${state.sales.size} বিক্রি)",
                                    "Real-time Cloud Synced (${state.products.size} products, ${state.sales.size} sales)"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusSuccess,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                exportLauncher.launch("FahimSiamTraders_Backup_$stamp.json")
                            },
                            modifier = Modifier.weight(1f).testTag("export_backup_button")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(language.pick("এক্সপোর্ট", "Export JSON"))
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f).testTag("restore_backup_button")
                        ) {
                            Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(language.pick("রিস্টোর", "Restore JSON"))
                        }
                    }
                }
            }
        }

        // Shop Profile Settings
        item {
            Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(language.pick("দোকানের তথ্য ও মেমো সেটিংস", "Store & Cash Memo Info"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(value = businessName, onValueChange = { businessName = it }, label = { Text(language.pick("দোকানের নাম", "Business Name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(language.pick("ফোন নম্বর", "Phone")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(language.pick("ঠিকানা", "Address")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = footerBn, onValueChange = { footerBn = it }, label = { Text("মেমো ফুটার (বাংলা)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = footerEn, onValueChange = { footerEn = it }, label = { Text("Receipt Footer (English)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            onSaveShopProfile(
                                state.shopProfile.copy(
                                    businessName = businessName.ifBlank { "Fahim Siam Traders" },
                                    phone = phone,
                                    address = address,
                                    receiptFooterBn = footerBn,
                                    receiptFooterEn = footerEn
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(language.pick("তথ্য সংরক্ষণ করুন", "Save Store Info"))
                    }
                }
            }
        }

        // Account & Sign Out
        item {
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${state.shopProfile.ownerName.ifBlank { "Store Owner" }} • ${state.shopProfile.ownerEmail}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedButton(
                        onClick = {
                            signOutUser(credentialManager, scope)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(language.pick("সাইন আউট করুন", "Sign Out"))
                    }
                }
            }
        }
    }
}
