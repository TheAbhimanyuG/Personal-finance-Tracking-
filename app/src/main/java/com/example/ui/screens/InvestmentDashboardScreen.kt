package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DbTransaction
import com.example.ui.AppViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentDashboardScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScreen: (String) -> Unit
) {
    val txns by viewModel.userTransactions.collectAsState()
    val rawAssets by viewModel.userAssets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currencyUnit by viewModel.selectedCurrency.collectAsState()

    val context = LocalContext.current
    val isDark = isDarkThemeActive

    // Advanced Range state
    var selectedInterval by remember { mutableStateOf("All Time") }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    // Dialog trigger states
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var activeEditingTx by remember { mutableStateOf<DbTransaction?>(null) }

    var txToDelete by remember { mutableStateOf<DbTransaction?>(null) }
    var txToSave by remember { mutableStateOf<DbTransaction?>(null) }
    var showTxDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showTxEditConfirmDialog by remember { mutableStateOf(false) }

    val currencyPrefix = when (currencyUnit) {
        "Dollar ($)" -> "$"
        "Euro (€)" -> "€"
        else -> "₹"
    }

    // Filter transaction assets
    val now = remember { System.currentTimeMillis() }
    val filteredAssetTxns = remember(txns, selectedInterval, customStartDate, customEndDate) {
        txns.filter { it.type.equals("Asset", ignoreCase = true) || it.type.equals("Investment", ignoreCase = true) }.filter { txn ->
            when (selectedInterval) {
                "Daily" -> txn.date > now - 24 * 60 * 60 * 1000L
                "Weekly" -> txn.date > now - 7 * 24 * 60 * 60 * 1000L
                "Monthly" -> txn.date > now - 30 * 24 * 60 * 60 * 1000L
                "Quarterly" -> txn.date > now - 90 * 24 * 60 * 60 * 1000L
                "Custom" -> {
                    if (customStartDate != null && customEndDate != null) {
                        txn.date in customStartDate!!..customEndDate!!
                    } else {
                        true
                    }
                }
                else -> true
            }
        }
    }

    // Filter database assets by interval dynamically to match filters
    val filteredAssets = remember(rawAssets, selectedInterval, customStartDate, customEndDate) {
        rawAssets.filter { asset ->
            when (selectedInterval) {
                "Daily" -> asset.dateInvested > now - 24 * 60 * 60 * 1000L
                "Weekly" -> asset.dateInvested > now - 7 * 24 * 60 * 60 * 1000L
                "Monthly" -> asset.dateInvested > now - 30 * 24 * 60 * 60 * 1000L
                "Quarterly" -> asset.dateInvested > now - 90 * 24 * 60 * 60 * 1000L
                "Custom" -> {
                    if (customStartDate != null && customEndDate != null) {
                        asset.dateInvested in customStartDate!!..customEndDate!!
                    } else {
                        true
                    }
                }
                else -> true
            }
        }
    }

    val totalInvestmentVal = remember(filteredAssets) {
        filteredAssets.sumOf { asset ->
            if (asset.quantity > 0.0 && asset.currentPrice > 0.0) {
                asset.quantity * asset.currentPrice
            } else {
                asset.amountInvested
            }
        }
    }

    val investmentAllocations = remember(filteredAssets) {
        val mapping = mutableMapOf<String, Double>()
        filteredAssets.forEach { asset ->
            val catKey = asset.type.ifBlank { "Uncategorized Assets" }
            val value = if (asset.quantity > 0.0 && asset.currentPrice > 0.0) {
                asset.quantity * asset.currentPrice
            } else {
                asset.amountInvested
            }
            mapping[catKey] = (mapping[catKey] ?: 0.0) + value
        }
        mapping
    }

    Scaffold(
        containerColor = if (isDark) BrandBackground else Color(0xFFF4F7FC),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF3B82F6),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Asset")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    val gridColor = if (isDark) Color(0xFF131A2D) else Color(0xFFF1F5F9)
                    val spacingPx = 24.dp.toPx()
                    var x = 0f
                    while (x < size.width) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 0.5.dp.toPx())
                        x += spacingPx
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 0.5.dp.toPx())
                        y += spacingPx
                    }
                },
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Net Investment Total Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF0F1E36) else Color(0xFFE3F2FD)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOTAL COMMITTED CAPITAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF3B82F6) else Color(0xFF1565C0)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currencyPrefix${String.format(Locale.getDefault(), "%,.2f", totalInvestmentVal)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else Color(0xFF0D47A1)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Calculated dynamically from live active ledger assets",
                            fontSize = 10.sp,
                            color = if (isDark) Color.LightGray.copy(0.7f) else Color.Gray
                        )
                    }
                }
            }

            // Advanced scope filter
            item {
                AdvancedDateRangeFilter(
                    isDark = isDark,
                    selectedInterval = selectedInterval,
                    onIntervalSelected = { selectedInterval = it },
                    customStartDate = customStartDate,
                    customEndDate = customEndDate,
                    onDatesChanged = { start, end ->
                        customStartDate = start
                        customEndDate = end
                    }
                )
            }

            // Interactive detailed Pie Chart based on dynamic database transactions
            item {
                Text(
                    text = "Aesthetic Asset Segmentations", 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                InteractivePieChart(
                    dataMap = investmentAllocations,
                    isDark = isDark
                )
            }

            // Outflow List Header
            item {
                Text(
                    text = "Registered Investment Entries", 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )
            }

            if (filteredAssetTxns.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No investment records found on ledger path", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                items(filteredAssetTxns, key = { "${it.id}_${it.date}" }) { txn ->
                    TransactionRowCard(
                        txn = txn,
                        isDark = isDark,
                        onModifyClick = {
                            activeEditingTx = txn
                            showEditDialog = true
                        },
                        onDeleteClick = {
                            txToDelete = txn
                            showTxDeleteConfirmDialog = true
                        }
                    )
                }
            }
        }
    }

    // Standard Universal Add Dialogue pre-locked to Asset type
    UnifiedAddEditTxDialog(
        show = showAddDialog,
        editingTxn = null,
        onDismiss = { showAddDialog = false },
        categories = categories,
        isDark = isDark,
        txTypeLocked = "Asset",
        viewModel = viewModel,
        onSave = { title, amount, type, category, sub, payment, originOfMoney, originIncomeCategory, originIncomeSubCategory, originOtherDesc ->
            viewModel.addTransaction(
                title = title,
                amount = amount,
                type = "Investment",
                category = category,
                subCategory = sub,
                payment = payment,
                originOfMoney = originOfMoney,
                originIncomeCategory = originIncomeCategory,
                originIncomeSubCategory = originIncomeSubCategory,
                originOtherDescription = originOtherDesc
            )
            Toast.makeText(context, "Asset transaction saved successfully!", Toast.LENGTH_SHORT).show()
        }
    )

    // Standard Universal Edit/Update dialog
    UnifiedAddEditTxDialog(
        show = showEditDialog,
        editingTxn = activeEditingTx,
        onDismiss = {
            showEditDialog = false
            activeEditingTx = null
        },
        categories = categories,
        isDark = isDark,
        txTypeLocked = "Asset",
        viewModel = viewModel,
        onSave = { title, amount, type, category, sub, payment, _, _, _, _ ->
            val existing = activeEditingTx
            if (existing != null) {
                txToSave = existing.copy(
                    title = title,
                    amount = amount,
                    type = "Investment",
                    category = category,
                    subCategory = sub,
                    paymentMethod = payment
                )
                showTxEditConfirmDialog = true
            }
        }
    )

    GlobalConfirmationModal(
        show = showTxDeleteConfirmDialog && txToDelete != null,
        title = "Are you sure?",
        message = "Do you want to delete the asset entry '${txToDelete?.title}'? This action cannot be undone.",
        confirmButtonColor = Color(0xFFEF4444),
        isDark = isDark,
        onConfirm = {
            if (txToDelete != null) {
                viewModel.deleteTransaction(txToDelete!!)
                showTxDeleteConfirmDialog = false
                txToDelete = null
                Toast.makeText(context, "Transaction deleted successfully!", Toast.LENGTH_SHORT).show()
            }
        },
        onCancel = {
            showTxDeleteConfirmDialog = false
            txToDelete = null
        }
    )


    GlobalConfirmationModal(
        show = showTxEditConfirmDialog && txToSave != null,
        title = "Are you sure?",
        message = "Do you want to save changes to the asset entry '${txToSave?.title}'?",
        confirmButtonColor = Color(0xFF3B82F6),
        isDark = isDark,
        onConfirm = {
            if (txToSave != null) {
                viewModel.updateTransaction(txToSave!!)
                showTxEditConfirmDialog = false
                txToSave = null
                showEditDialog = false
                activeEditingTx = null
                Toast.makeText(context, "Transaction updated successfully!", Toast.LENGTH_SHORT).show()
            }
        },
        onCancel = {
            showTxEditConfirmDialog = false
            txToSave = null
        }
    )

}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
