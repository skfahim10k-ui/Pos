package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Sale
import com.example.data.model.ShopProfile
import com.example.ui.i18n.formatDateTime
import com.example.ui.i18n.formatQty
import com.example.ui.i18n.formatTaka
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.util.CashMemoExporter
import com.example.util.QrBarcodeRenderer

@Composable
fun CashMemoDialog(
    sale: Sale,
    shop: ShopProfile,
    initialMode: String = shop.preferredLanguage.ifBlank { "both" },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var memoMode by remember(initialMode) { mutableStateOf(initialMode) }

    val qrBitmap = remember(sale.memoNumber, sale.totalAmount) {
        QrBarcodeRenderer.generateQrBitmap(
            content = "MEMO:${sale.memoNumber}|${shop.businessName}|TOTAL:${sale.totalAmount}|PAID:${sale.paidAmount}|DUE:${sale.dueAmount}",
            sizePx = 220
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (memoMode) {
                            "bn" -> "ক্যাশ মেমো"
                            "en" -> "Cash Memo"
                            else -> "ক্যাশ মেমো / Cash Memo"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Cash Memo")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("both" to "উভয় / Both", "bn" to "বাংলা", "en" to "English").forEach { (code, label) ->
                        FilterChip(
                            selected = memoMode == code,
                            onClick = { memoMode = code },
                            label = { Text(label) }
                        )
                    }
                }

                // Receipt paper card
                Surface(
                    color = Color(0xFFFCFBF7),
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = shop.businessName.ifBlank { "Fahim Siam Traders" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D4A3A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (shop.phone.isNotBlank() || shop.address.isNotBlank()) {
                            Text(
                                text = listOf(shop.phone, shop.address).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475750),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        HorizontalDivider(color = Color(0xFFD9DED8), modifier = Modifier.padding(vertical = 4.dp))

                        MemoMetaRow(
                            label = when (memoMode) {
                                "bn" -> "মেমো নং"
                                "en" -> "Memo No"
                                else -> "মেমো / Memo"
                            },
                            value = sale.memoNumber,
                            mono = true
                        )
                        MemoMetaRow(
                            label = when (memoMode) {
                                "bn" -> "তারিখ"
                                "en" -> "Date"
                                else -> "তারিখ / Date"
                            },
                            value = formatDateTime(sale.saleEpochMs)
                        )
                        MemoMetaRow(
                            label = when (memoMode) {
                                "bn" -> "ক্রেতা"
                                "en" -> "Customer"
                                else -> "ক্রেতা / Customer"
                            },
                            value = sale.customerName
                        )
                        if (sale.customerPhone.isNotBlank()) {
                            MemoMetaRow(
                                label = when (memoMode) {
                                    "bn" -> "ফোন"
                                    "en" -> "Phone"
                                    else -> "ফোন / Phone"
                                },
                                value = sale.customerPhone
                            )
                        }

                        HorizontalDivider(color = Color(0xFFD9DED8), modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = when (memoMode) {
                                    "bn" -> "বিবরণ"
                                    "en" -> "Item"
                                    else -> "পণ্য / Item"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF141E1A),
                                modifier = Modifier.weight(1.6f)
                            )
                            Text(
                                text = when (memoMode) {
                                    "bn" -> "পরিমাণ"
                                    "en" -> "Qty"
                                    else -> "পরিমাণ"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF141E1A),
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.8f)
                            )
                            Text(
                                text = when (memoMode) {
                                    "bn" -> "মোট"
                                    "en" -> "Total"
                                    else -> "মোট"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF141E1A),
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        sale.items.forEach { item ->
                            val itemTitle = when (memoMode) {
                                "bn" -> item.productNameBn.ifBlank { item.productName }
                                "en" -> item.productName
                                else -> if (item.productNameBn.isNotBlank()) "${item.productNameBn} / ${item.productName}" else item.productName
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.6f)) {
                                    Text(
                                        text = itemTitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF141E1A)
                                    )
                                    Text(
                                        text = "@ ${formatTaka(item.unitPrice)}/${item.unit}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF5A6861)
                                    )
                                }
                                Text(
                                    text = "${formatQty(item.qty)} ${item.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF141E1A),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.8f)
                                )
                                Text(
                                    text = formatTaka(item.lineTotal),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF141E1A),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFD9DED8), modifier = Modifier.padding(vertical = 4.dp))

                        MemoMetaRow(
                            label = when (memoMode) {
                                "bn" -> "উপমোট"
                                "en" -> "Subtotal"
                                else -> "উপমোট / Subtotal"
                            },
                            value = formatTaka(sale.subtotal),
                            mono = true
                        )
                        if (sale.discount > 0.0) {
                            MemoMetaRow(
                                label = when (memoMode) {
                                    "bn" -> "ছাড়"
                                    "en" -> "Discount"
                                    else -> "ছাড় / Discount"
                                },
                                value = "-${formatTaka(sale.discount)}",
                                mono = true
                            )
                        }
                        MemoMetaRow(
                            label = when (memoMode) {
                                "bn" -> "সর্বমোট বিল"
                                "en" -> "Net Total"
                                else -> "সর্বমোট / Net Total"
                            },
                            value = formatTaka(sale.totalAmount),
                            mono = true,
                            bold = true
                        )
                        MemoMetaRow(
                            label = when (memoMode) {
                                "bn" -> "পরিশোধ (${sale.paymentMethod})"
                                "en" -> "Paid (${sale.paymentMethod})"
                                else -> "জমা / Paid (${sale.paymentMethod})"
                            },
                            value = formatTaka(sale.paidAmount),
                            mono = true,
                            valueColor = StatusSuccess
                        )
                        if (sale.dueAmount > 0.0) {
                            MemoMetaRow(
                                label = when (memoMode) {
                                    "bn" -> "বাকি"
                                    "en" -> "Due"
                                    else -> "বাকি / Due"
                                },
                                value = formatTaka(sale.dueAmount),
                                mono = true,
                                bold = true,
                                valueColor = StatusDanger
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (memoMode != "en") {
                                    Text(
                                        text = shop.receiptFooterBn.ifBlank { "ধন্যবাদ, আবার আসবেন!" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF0D4A3A),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (memoMode != "bn") {
                                    Text(
                                        text = shop.receiptFooterEn.ifBlank { "Thank you for your business!" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF475750)
                                    )
                                }
                            }
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Cash Memo QR Code",
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { CashMemoExporter.shareCashMemoText(context, sale, shop, memoMode) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_memo_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                    Button(
                        onClick = { CashMemoExporter.printCashMemo(context, sale, shop, memoMode) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("print_memo_button")
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print / PDF")
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoMetaRow(
    label: String,
    value: String,
    mono: Boolean = false,
    bold: Boolean = false,
    valueColor: Color = Color(0xFF141E1A)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = Color(0xFF475750)
        )
        Text(
            text = value,
            style = if (mono) MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily)
            else MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}
