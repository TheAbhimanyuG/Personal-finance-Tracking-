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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DbTransaction
import com.example.ui.AppViewModel
import com.example.ui.components.*
import com.example.ui.context.appGlobalContext
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeDashboardScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScreen: (String) -> Unit
) {
    val contextState = appGlobalContext
    val txns = contextState.transactions
    val categories = contextState.categories
    val currencyUnit = contextState.currencyUnit

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

    // Filter transaction inflows
    val now = remember { System.currentTimeMillis() }
    val intervalTxns = remember(txns, selectedInterval, customStartDate, customEndDate) {
        txns.filter { txn ->
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

    val filteredIncomes = remember(intervalTxns) {
        intervalTxns.filter { it.type.equals("Income", ignoreCase = true) }
    }

    val rawInflow = remember(filteredIncomes) { filteredIncomes.sumOf { it.amount } }

    val totalIncome = remember(intervalTxns) {
        val totalInFlow = intervalTxns.filter { 
            it.type.equals("Income", ignoreCase = true) && 
            it.sourceType == "SOURCE_INCOME" 
        }.sumOf { it.amount }
        val deductions = intervalTxns.filter { 
            !it.type.equals("Income", ignoreCase = true) && 
            it.sourceType == "SOURCE_INCOME" &&
            !it.type.equals("Budget", ignoreCase = true) &&
            !it.type.equals("Goal", ignoreCase = true)
        }.sumOf { it.amount }
        (totalInFlow - deductions).coerceAtLeast(0.0)
    }

    val cashBalanceVal = remember(txns) {
        val totalInc = txns.filter { 
            it.type.equals("Income", ignoreCase = true) && 
            it.sourceType == "SOURCE_INCOME" 
        }.sumOf { it.amount }
        val deductions = txns.filter { 
            !it.type.equals("Income", ignoreCase = true) && 
            it.sourceType == "SOURCE_INCOME" &&
            !it.type.equals("Budget", ignoreCase = true) &&
            !it.type.equals("Goal", ignoreCase = true)
        }.sumOf { it.amount }
        val rem = (totalInc - deductions).coerceAtLeast(0.0)
        
        val otherInflows = txns.filter { 
            it.sourceType == "SOURCE_OTHER" && 
            it.type.equals("Income", ignoreCase = true) 
        }.sumOf { it.amount }
        val otherOutflows = txns.filter { 
            it.sourceType == "SOURCE_OTHER" && 
            it.type.equals("Expense", ignoreCase = true) 
        }.sumOf { it.amount }
        (rem + otherInflows - otherOutflows).coerceAtLeast(0.0)
    }

    val incomeAllocations = remember(filteredIncomes, txns) {
        val mapping = mutableMapOf<String, Double>()
        // 1. Add raw inflow amounts per category
        filteredIncomes.forEach { txn ->
            mapping[txn.category] = (mapping[txn.category] ?: 0.0) + txn.amount
        }
        
        // 2. Subtract transfers to assets or budgets funded by this income category
        txns.forEach { txn ->
            val titleLower = txn.title.lowercase()
            val isInvestmentTransfer = titleLower.contains("investment deduction") ||
                    (titleLower.contains("invest") && txn.fundsSource.lowercase().contains("income")) ||
                    txn.type.equals("Asset", ignoreCase = true) ||
                    txn.type.equals("Investment", ignoreCase = true)
            val isBudgetGoalTransfer = titleLower.contains("top up transfer") || titleLower.contains("top-up") ||
                    txn.type.equals("Budget/Goal Top-Up", ignoreCase = true) ||
                    ((titleLower.contains("budget") || titleLower.contains("goal")) && txn.fundsSource.lowercase().contains("income") && txn.type.equals("Budget/Goal Top-Up", ignoreCase = true))
            
            if (isInvestmentTransfer || isBudgetGoalTransfer) {
                val cat = txn.category
                if (cat.isNotBlank() && mapping.containsKey(cat)) {
                    mapping[cat] = (mapping[cat] ?: 0.0) - txn.amount
                }
            } else if (txn.fundsSource.startsWith("Income:", ignoreCase = true)) {
                val cat = txn.fundsSource.substringAfter("Income:").trim()
                if (cat.isNotBlank() && mapping.containsKey(cat)) {
                    mapping[cat] = (mapping[cat] ?: 0.0) - txn.amount
                }
            } else if (txn.fundsSource.lowercase().contains("income") && !txn.fundsSource.lowercase().contains("wallet")) {
                val cat = txn.category
                if (cat.isNotBlank() && mapping.containsKey(cat)) {
                    mapping[cat] = (mapping[cat] ?: 0.0) - txn.amount
                }
            }
        }
        
        // Coerce remaining amounts to at least 0.0
        mapping.keys.forEach { k ->
            mapping[k] = (mapping[k] ?: 0.0).coerceAtLeast(0.0)
        }
        mapping
    }

    Scaffold(
        containerColor = if (isDark) BrandBackground else Color(0xFFF4F7FC),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF10B981),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Income")
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
            // Dual Top Tiles Panel (Total Income & Remaining Cash Balance)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tile 1: Total Income (for selected interval)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF0F2D1F) else Color(0xFFE8F5E9)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("income_tile_total")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "TOTAL INCOME",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF10B981) else Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currencyPrefix${String.format(Locale.getDefault(), "%,.0f", rawInflow)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color(0xFF1B5E20)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Inflows in interval",
                                fontSize = 8.5.sp,
                                color = if (isDark) Color.LightGray.copy(0.7f) else Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Tile 2: Remaining Cash Balance (overall cumulative pool)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF172554) else Color(0xFFE0F2FE)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("income_tile_remaining")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "REMAINING CASH",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF60A5FA) else Color(0xFF0369A1)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currencyPrefix${String.format(Locale.getDefault(), "%,.0f", cashBalanceVal)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color(0xFF0C4A6E)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Current cash pool",
                                fontSize = 8.5.sp,
                                color = if (isDark) Color.LightGray.copy(0.7f) else Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
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

            // Interactive 6-Month Historical Inflow Trend Bar Graph
            item {
                IncomeHistoricalBarChart(
                    transactions = txns,
                    isDark = isDark,
                    currencyPrefix = currencyPrefix
                )
            }

            // Interactive detailed Pie Chart based on dynamic database transactions
            item {
                Text(
                    text = "Aesthetic Inflow Allocations", 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                InteractivePieChart(
                    dataMap = incomeAllocations,
                    isDark = isDark
                )
            }

            // Inflow List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Registered Income Entries", 
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F1B6B)
                    )
                    TextButton(
                        onClick = {
                            val csvContent = com.example.util.CsvExporter.formatTransactionsToCsv(filteredIncomes)
                            com.example.util.CsvExporter.exportToCsvFile(context, "income_ledger_export.csv", csvContent)
                        },
                        modifier = Modifier.testTag("export_income_csv_button")
                    ) {
                        Text("Export CSV", fontSize = 12.sp)
                    }
                }
            }

            if (filteredIncomes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No income records found on ledger path", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                items(filteredIncomes, key = { "${it.id}_${it.date}" }) { txn ->
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

            // Monthly Income Audit report card at the absolute bottom
            item {
                MonthlyIncomeAuditReport(
                    transactions = txns,
                    isDark = isDark,
                    currencyPrefix = currencyPrefix
                )
            }
        }
    }

    // Standard Universal Add Dialogue pre-locked to Income type
    UnifiedAddEditTxDialog(
        show = showAddDialog,
        editingTxn = null,
        onDismiss = { showAddDialog = false },
        categories = categories,
        isDark = isDark,
        txTypeLocked = "Income",
        viewModel = viewModel,
        onSave = { title, amount, type, category, sub, payment, originOfMoney, originIncomeCategory, originIncomeSubCategory, originOtherDesc ->
            viewModel.addTransaction(
                title = title,
                amount = amount,
                type = "Income",
                category = category,
                subCategory = sub,
                payment = payment,
                originOfMoney = originOfMoney,
                originIncomeCategory = originIncomeCategory,
                originIncomeSubCategory = originIncomeSubCategory,
                originOtherDescription = originOtherDesc
            )
            Toast.makeText(context, "Income transaction registered successfully!", Toast.LENGTH_SHORT).show()
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
        txTypeLocked = "Income",
        viewModel = viewModel,
        onSave = { title, amount, type, category, sub, payment, _, _, _, _ ->
            val existing = activeEditingTx
            if (existing != null) {
                txToSave = existing.copy(
                    title = title,
                    amount = amount,
                    type = "Income",
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
        message = "Do you want to delete the income entry '${txToDelete?.title}'? This action cannot be undone.",
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
        message = "Do you want to save changes to the income entry '${txToSave?.title}'?",
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

@Composable
fun IncomeHistoricalBarChart(
    transactions: List<DbTransaction>,
    isDark: Boolean,
    currencyPrefix: String
) {
    // Generate beautiful dynamic list of 6 months
    val months = remember {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMM", Locale.getDefault())
        val list = mutableListOf<String>()
        // We need 6 months ending with current month.
        for (i in 5 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i)
            list.add(sdf.format(cal.time))
        }
        list
    }

    // Group dynamically using strict [Total Income] - [All Deductions] of sourceType SOURCE_INCOME
    val monthlyRemainingAmounts = remember(transactions) {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) // 0-based
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Construct 7 buckets. Bucket 6 is current month (Jun), Bucket 0 is 6 months ago (Dec 2025).
        val rawIncomes = DoubleArray(7) { 0.0 }
        val rawDeductions = DoubleArray(7) { 0.0 }

        transactions.forEach { tx ->
            calendar.timeInMillis = tx.date
            val txMonth = calendar.get(Calendar.MONTH)
            val txYear = calendar.get(Calendar.YEAR)
            
            val totalMonthDiff = (currentYear - txYear) * 12 + (currentMonth - txMonth)
            
            if (totalMonthDiff in 0..6) {
                val arrIdx = 6 - totalMonthDiff
                if (tx.type.equals("Income", ignoreCase = true) && tx.sourceType == "SOURCE_INCOME") {
                    rawIncomes[arrIdx] += tx.amount
                } else if (!tx.type.equals("Income", ignoreCase = true) && tx.sourceType == "SOURCE_INCOME" && !tx.type.equals("Budget", ignoreCase = true) && !tx.type.equals("Goal", ignoreCase = true)) {
                    rawDeductions[arrIdx] += tx.amount
                }
            }
        }
        
        // Calculate remaining income for each of the 7 buckets
        (0..6).map { idx ->
            (rawIncomes[idx] - rawDeductions[idx]).coerceAtLeast(0.0)
        }
    }

    // drop the first element (the 6-months-ago month, which was only used for % Change baseline)
    val barAmounts = remember(monthlyRemainingAmounts) {
        monthlyRemainingAmounts.drop(1)
    }

    val total6Month = remember(barAmounts) { barAmounts.sum() }
    var selectedBarIndex by remember { mutableStateOf<Int?>(5) } // Default to latest month

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 6-Month Remaining Income Trends",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color(0xFF0F1B6B)
            )
            Text(
                text = "Tap any bar to inspect specific remaining volume & percentage yield:",
                fontSize = 11.sp,
                color = if (isDark) Color.LightGray.copy(alpha = 0.8f) else Color(0xFF64748B),
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Graph rows
            val maxAmount = remember(barAmounts) { (barAmounts.maxOrNull() ?: 1.0) * 1.15 }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    barAmounts.forEachIndexed { index, amount ->
                        val ratio = (amount / maxAmount).toFloat().coerceIn(0.12f, 1.0f)
                        val isSelected = selectedBarIndex == index
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedBarIndex = if (isSelected) null else index }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height((110.dp * ratio))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (isSelected) Color(0xFF10B981) else Color(0xFF10B981).copy(alpha = 0.45f)
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = months[index],
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) (if (isDark) Color.White else Color(0xFF0F1B6B)) else Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selection report details
            val selectedIndex = selectedBarIndex
            if (selectedIndex != null) {
                val selectedAmount = barAmounts[selectedIndex]
                val priorValue = monthlyRemainingAmounts[selectedIndex] // previous month in array is exactly at selectedIndex
                val pctChange = if (priorValue > 0.0) {
                    ((selectedAmount - priorValue) / priorValue) * 100.0
                } else {
                    0.0
                }
                
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF0F251C) else Color(0xFFE8F5E9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "REMAINING INCOME: ${months[selectedIndex].uppercase()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF10B981) else Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$currencyPrefix${String.format(Locale.getDefault(), "%,.2f", selectedAmount)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color(0xFF1B5E20)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (pctChange >= 0.0) {
                                        if (isDark) Color(0xFF10B981).copy(0.15f) else Color(0xFFE8F5E9)
                                    } else {
                                        if (isDark) Color(0xFFEF4444).copy(0.15f) else Color(0xFFFEE2E2)
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%s%.1f%%", if (pctChange >= 0.0) "+" else "", pctChange),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (pctChange >= 0.0) {
                                    if (isDark) Color(0xFF10B981) else Color(0xFF2E7D32)
                                } else {
                                    if (isDark) Color(0xFFEF4444) else Color(0xFFC53030)
                                }
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Total 6-Month Remaining sum: $currencyPrefix${String.format(Locale.getDefault(), "%,.0f", total6Month)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyIncomeAuditReport(
    transactions: List<DbTransaction>,
    isDark: Boolean,
    currencyPrefix: String
) {
    val incomes = remember(transactions) { transactions.filter { it.type == "Income" } }
    val totalThisMonth = remember(incomes) { incomes.sumOf { it.amount } }
    val avgTransaction = remember(incomes) { if (incomes.isNotEmpty()) totalThisMonth / incomes.size else 0.0 }
    val maxSingleInflow = remember(incomes) { incomes.maxOfOrNull { it.amount } ?: 0.0 }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 Monthly Income Audit Report",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF10B981).copy(0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LIVE DATA",
                        fontSize = 8.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Detailed audit on registered income entries path indicates high reliability of calculated ledger inputs:",
                fontSize = 11.sp,
                color = if (isDark) Color.LightGray else Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF161A26) else Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Average Inflow", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "$currencyPrefix${String.format(Locale.getDefault(), "%,.1f", avgTransaction)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF0F1B6B)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF161A26) else Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Max Single Pay", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "$currencyPrefix${String.format(Locale.getDefault(), "%,.1f", maxSingleInflow)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF0F1B6B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "💡 Audit summary: Your average income entry represents a stable cash flow on the ledger path. Ensure proper capital allocation is maintained directly across Expense goals.",
                fontSize = 10.sp,
                color = Color.Gray,
                lineHeight = 14.sp
            )
        }
    }
}
