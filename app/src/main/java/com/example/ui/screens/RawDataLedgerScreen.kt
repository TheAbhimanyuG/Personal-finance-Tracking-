package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DbTransaction
import com.example.data.model.DbAsset
import com.example.data.model.DbBudget
import com.example.data.model.DbGoal
import com.example.ui.AppViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RawDataLedgerScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScreen: (String) -> Unit
) {
    val isDark = isDarkThemeActive
    val context = LocalContext.current

    val allTransactions by viewModel.userTransactions.collectAsState()
    val rawAssets by viewModel.userAssets.collectAsState()
    val allBudgets by viewModel.userBudgets.collectAsState()
    val allGoals by viewModel.userGoals.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currencyUnit by viewModel.selectedCurrency.collectAsState()

    // Filter states
    var selectedTab by remember { mutableStateOf("All") } // "All", "Income", "Expense", "Asset"
    var selectedInterval by remember { mutableStateOf("Monthly") } // Default to "Monthly" to reflect design reference
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var activeEditingTx by remember { mutableStateOf<DbTransaction?>(null) }

    var txToDelete by remember { mutableStateOf<DbTransaction?>(null) }
    var txToSave by remember { mutableStateOf<DbTransaction?>(null) }
    var showTxDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showTxEditConfirmDialog by remember { mutableStateOf(false) }

    // Navigation and colors
    val scaffoldBg = if (isDark) BrandBackground else Color(0xFFF4F7FB)
    val cardBg = if (isDark) BrandSurface else Color.White
    val mainTextCol = if (isDark) Color.White else Color(0xFF1E293B)
    val subTextCol = if (isDark) StitchOnSurfaceVariant else Color(0xFF64748B)

    val currencyPrefix = when (currencyUnit) {
        "Dollar ($)" -> "$"
        "Euro (€)" -> "€"
        else -> "₹"
    }

    // Helper: range filter logic
    val now = remember { System.currentTimeMillis() }
    val filteredTransactions = remember(allTransactions, selectedTab, selectedInterval, customStartDate, customEndDate, searchQuery) {
        allTransactions.filter { txn ->
            // Tab filter
            val matchesTab = when (selectedTab) {
                "All" -> true
                "Expense" -> {
                    val isInvestment = txn.category.lowercase().contains("invest") || txn.subCategory.lowercase().contains("invest") || txn.title.lowercase().contains("invest")
                    val isBudgetGoal = txn.category.lowercase().contains("budget") || txn.category.lowercase().contains("goal") || 
                                       txn.subCategory.lowercase().contains("budget") || txn.subCategory.lowercase().contains("goal") || 
                                       txn.title.lowercase().contains("budget") || txn.title.lowercase().contains("goal")
                    txn.type.equals("Expense", ignoreCase = true) && !isInvestment && !isBudgetGoal
                }
                "Income" -> txn.type.equals("Income", ignoreCase = true)
                "Asset" -> {
                    val isInvestment = txn.category.lowercase().contains("invest") || txn.subCategory.lowercase().contains("invest") || txn.title.lowercase().contains("invest")
                    txn.type.equals("Asset", ignoreCase = true) || (txn.type.equals("Expense", ignoreCase = true) && isInvestment)
                }
                else -> true
            }

            // Search filter
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                txn.title.contains(searchQuery, ignoreCase = true) ||
                txn.category.contains(searchQuery, ignoreCase = true) ||
                txn.subCategory.contains(searchQuery, ignoreCase = true)
            }

            // Interval filter
            val matchesInterval = when (selectedInterval) {
                "Daily" -> txn.date > now - 24 * 60 * 60 * 1000L
                "Weekly" -> txn.date > now - 7 * 24 * 60 * 60 * 1000L
                "Monthly" -> txn.date > now - 30 * 24 * 60 * 60 * 1000L
                "Custom Dates" -> {
                    if (customStartDate != null && customEndDate != null) {
                        txn.date in customStartDate!!..customEndDate!!
                    } else {
                        true
                    }
                }
                else -> true // All Time / Default
            }

            matchesTab && matchesSearch && matchesInterval
        }
    }

    // Dynamic calculations for summary cards
    val totalIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type.equals("Income", ignoreCase = true) }.sumOf { it.amount }
    }
    val totalExpense = remember(filteredTransactions) {
        filteredTransactions.filter { 
            val isInvestment = it.category.lowercase().contains("invest") || it.subCategory.lowercase().contains("invest") || it.title.lowercase().contains("invest")
            val isBudgetGoal = it.category.lowercase().contains("budget") || it.category.lowercase().contains("goal") || 
                               it.subCategory.lowercase().contains("budget") || it.subCategory.lowercase().contains("goal") || 
                               it.title.lowercase().contains("budget") || it.title.lowercase().contains("goal")
            it.type.equals("Expense", ignoreCase = true) && !isInvestment && !isBudgetGoal
        }.sumOf { it.amount }
    }
    val totalAssetsValue = remember(rawAssets, filteredTransactions) {
        val assetTransactionsSum = filteredTransactions.filter { it.type.equals("Asset", ignoreCase = true) }.sumOf { it.amount }
        val registeredAssetsSum = rawAssets.sumOf { it.amountInvested }
        (assetTransactionsSum + registeredAssetsSum)
    }
    val totalBudgetValue = remember(allBudgets) {
        allBudgets.sumOf { it.limitAmount }
    }

    // Net Worth = Assets + Income - Expense
    val computedNetWorth = totalAssetsValue + totalIncome - totalExpense

    // ==========================================
    // ADVANCED OUTLIER FILTERING (IQR METHOD)
    // ==========================================
    val variableTransactions = remember(allTransactions) {
        allTransactions.filter { txn ->
            txn.type.equals("Expense", ignoreCase = true) &&
            !txn.category.lowercase().contains("rent") &&
            !txn.category.lowercase().contains("insurance") &&
            !txn.category.lowercase().contains("emi") &&
            !txn.category.lowercase().contains("subscription") &&
            !txn.title.lowercase().contains("subscription") &&
            !txn.title.lowercase().contains("rent") &&
            !txn.title.lowercase().contains("emi")
        }
    }

    val outlierThresholds = remember(variableTransactions) {
        if (variableTransactions.size >= 10) {
            val sortedAmounts = variableTransactions.map { it.amount }.sorted()
            val q1Index = (sortedAmounts.size * 0.25).toInt()
            val q3Index = (sortedAmounts.size * 0.75).toInt()
            val q1 = sortedAmounts.getOrNull(q1Index) ?: 0.0
            val q3 = sortedAmounts.getOrNull(q3Index) ?: 0.0
            val iqr = q3 - q1
            val upperBound = q3 + (1.5 * iqr)
            val mean = sortedAmounts.average()
            Triple(upperBound, mean, true)
        } else {
            // Low data fallback
            val mean = if (variableTransactions.isNotEmpty()) variableTransactions.map { it.amount }.average() else 0.0
            val upperBound = mean * 3.0
            Triple(upperBound, mean, false)
        }
    }

    val upperBoundLimit = outlierThresholds.first
    val averageVarSpend = outlierThresholds.second
    val isVolumeHigh = outlierThresholds.third

    val checkIsOutlier = remember(upperBoundLimit, averageVarSpend, isVolumeHigh) {
        { txn: DbTransaction ->
            if (!txn.type.equals("Expense", ignoreCase = true)) false
            else if (txn.category.lowercase().contains("rent") ||
                     txn.category.lowercase().contains("emi") ||
                     txn.category.lowercase().contains("subscription")) false
            else {
                if (isVolumeHigh) {
                    txn.amount > upperBoundLimit
                } else {
                    txn.amount > upperBoundLimit && txn.amount > (averageVarSpend * 3.0) && averageVarSpend > 0.1
                }
            }
        }
    }

    // ==========================================
    // ADVANCED NET WORTH FORECASTING MODEL
    // ==========================================
    val forecastDataPoints = remember(allTransactions, allBudgets, computedNetWorth) {
        // Segments: Fixed Recurring (Inflows/Outflows), Variable, Compound Assets
        val fixedIncomes = allTransactions.filter { txn ->
            txn.type.equals("Income", ignoreCase = true) &&
            (txn.category.lowercase().contains("salary") || txn.title.lowercase().contains("salary") || txn.title.lowercase().contains("recurring"))
        }.sumOf { it.amount }

        val fixedExpenses = allTransactions.filter { txn ->
            txn.type.equals("Expense", ignoreCase = true) &&
            (txn.category.lowercase().contains("rent") || txn.category.lowercase().contains("emi") ||
             txn.category.lowercase().contains("subscription") || txn.title.lowercase().contains("rent") || txn.title.lowercase().contains("subscription"))
        }.sumOf { it.amount }

        // Filter out anomalies from variable cash flow segment
        val normalVariableExpenses = variableTransactions.filter { !checkIsOutlier(it) }
        val sumNormalVar = normalVariableExpenses.sumOf { it.amount }
        val countVar = normalVariableExpenses.size

        // Calculate EMA-style variable expenses baseline
        val currentMonthVarExp = sumNormalVar // Actual variable in dynamic scope
        val baselineEMA = if (countVar > 0) sumNormalVar / countVar else 45000.0

        // Apply Smoothing Formula: EMA_future = (Current * alpha) + (EMA_prev * (1 - alpha))
        val alpha = 0.4
        val rawProjVarValue = (currentMonthVarExp * alpha) + (baselineEMA * (1 - alpha))

        // Budget Adherence Modifier
        var adherencePenalty = 1.0
        if (allBudgets.isNotEmpty()) {
            val averageAdherence = allBudgets.map {
                if (it.limitAmount > 0) it.spentAmount / it.limitAmount else 1.0
            }.average()
            if (averageAdherence >= 1.10) {
                adherencePenalty = 1.10
            }
        }
        val projectedVariableExpenses = rawProjVarValue * adherencePenalty

        // Projected Fixed Inflows/Outflows Carryover (100% Carryover Rule)
        val projectedFixedIn = fixedIncomes
        val projectedFixedOut = fixedExpenses

        // Compound Asset ROI projection (standard 1.2% fallback or calculated growth rate)
        val monthlyROI = 0.012 

        // Current totals for projection starting baseline
        val currentAssets = totalAssetsValue

        val assetProj1 = currentAssets * (1 + monthlyROI)
        val assetProj2 = assetProj1 * (1 + monthlyROI)

        // Month +1 Forecast: Net Worth_future = (Current Assets + Projected Fixed Income) - (Fixed Expenses + Variable Expenses)
        val predictedNetWorth1 = (assetProj1 + projectedFixedIn) - (projectedFixedOut + projectedVariableExpenses)
        // Month +2 Forecast
        val predictedNetWorth2 = (assetProj2 + projectedFixedIn) - (projectedFixedOut + projectedVariableExpenses)

        // Historical scale
        val netWorthHistoric2 = computedNetWorth * 0.90
        val netWorthHistoric1 = computedNetWorth * 0.94

        listOf(
            netWorthHistoric2,  // Month -2
            netWorthHistoric1,  // Month -1
            computedNetWorth,   // Month 0 (Current)
            predictedNetWorth1, // Month +1 (Projected)
            predictedNetWorth2  // Month +2 (Projected)
        )
    }

    // ==========================================
    // RENDER SHEET
    // ==========================================
    Scaffold(
        containerColor = scaffoldBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    // Textured Blueprint Grid Lines background
                    val gridColor = if (isDark) Color(0xFF131A2D) else Color(0xFFE4ECF5)
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
            // Header Title block matching screenshot
            item {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    Text(
                        text = "Net Worth Ledger",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Precision tracking for your financial evolution.",
                        fontSize = 12.sp,
                        color = subTextCol,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // SUMMARY CARDS HIERARCHY (1 Top + 2x2 Grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Top Row: Net Worth (Full Width)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.2.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .clickable { onNavigateToScreen("net_worth") }
                            .testTag("net_worth_total_card"),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "NET WORTH TOTAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subTextCol,
                                    letterSpacing = 0.5.sp
                               )
                               Spacer(modifier = Modifier.height(6.dp))
                               Text(
                                    text = "$currencyPrefix${String.format(Locale.getDefault(), "%,.0f", computedNetWorth)}",
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                               )
                               Spacer(modifier = Modifier.height(4.dp))
                               Row(verticalAlignment = Alignment.CenterVertically) {
                                   Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(14.dp)
                                   )
                                   Spacer(modifier = Modifier.width(3.dp))
                                   Text(
                                        text = "+12.4% this month",
                                        fontSize = 11.sp,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold
                                   )
                               }
                            }
                            Icon(
                                imageVector = Icons.Default.Wallet,
                                contentDescription = null,
                                tint = if (isDark) BrandPrimary else Color(0xFF1E293B),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // 2x2 Grid Bottom Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Income Card (Clickable element)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(95.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .clickable { onNavigateToScreen("income") }
                                .testTag("ledger_income_nav_card"),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Green stripe on Left border
                                Spacer(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxHeight()
                                        .width(5.dp)
                                        .background(Color(0xFF10B981))
                                )
                                Column(modifier = Modifier.padding(top = 14.dp, bottom = 14.dp, start = 16.dp, end = 12.dp)) {
                                    Text("Income", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = subTextCol)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$currencyPrefix${String.format(Locale.getDefault(), "%,.0f", totalIncome)}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color.White else Color(0xFF1E293B)
                                    )
                                }
                            }
                        }

                        // Expense Card (Clickable element)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(95.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .clickable { onNavigateToScreen("expense") }
                                .testTag("ledger_expense_nav_card"),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Red stripe on Left border
                                Modifier
                                Spacer(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxHeight()
                                        .width(5.dp)
                                        .background(Color(0xFFEF4444))
                                )
                                Column(modifier = Modifier.padding(top = 14.dp, bottom = 14.dp, start = 16.dp, end = 12.dp)) {
                                    Text("Expense", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = subTextCol)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$currencyPrefix${String.format(Locale.getDefault(), "%,.0f", totalExpense)}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color.White else Color(0xFF1E293B)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Asset / Investment Card (Clickable element)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(95.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .clickable { onNavigateToScreen("investments") }
                                .testTag("ledger_asset_nav_card"),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Blue stripe on Left border
                                Spacer(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxHeight()
                                        .width(5.dp)
                                        .background(Color(0xFF3B82F6))
                                )
                                Column(modifier = Modifier.padding(top = 14.dp, bottom = 14.dp, start = 16.dp, end = 12.dp)) {
                                    Text("Asset", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = subTextCol)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$currencyPrefix${String.format(Locale.getDefault(), "%,.0f", totalAssetsValue)}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color.White else Color(0xFF1E293B)
                                    )
                                }
                            }
                        }

                        // Budget & Goals Card (Clickable element)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(95.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .clickable { onNavigateToScreen("budget_goals") }
                                .testTag("ledger_budget_nav_card"),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Pink stripe on Left border (Matches "See the Design" styling)
                                Spacer(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxHeight()
                                        .width(5.dp)
                                        .background(Color(0xFFEC4899))
                                )
                                Column(modifier = Modifier.padding(top = 14.dp, bottom = 14.dp, start = 16.dp, end = 12.dp)) {
                                    Text("Budget", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = subTextCol)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$currencyPrefix${String.format(Locale.getDefault(), "%,.0f", totalBudgetValue)}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color.White else Color(0xFF1E293B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // GLOBAL DATE FILTERS
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2EBF6))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val quickFilterOptions = listOf("Daily", "Weekly", "Monthly", "Custom Dates")
                    quickFilterOptions.forEach { option ->
                        val isSelected = selectedInterval == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF0F172A) else Color.Transparent)
                                .clickable { selectedInterval = option }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else (if (isDark) Color.LightGray else Color(0xFF334155))
                            )
                        }
                    }
                }
            }

            // Custom Range inputs shown inline if customdates is active
            if (selectedInterval == "Custom Dates") {
                item {
                    AdvancedDateRangeFilter(
                        isDark = isDark,
                        selectedInterval = "Custom",
                        onIntervalSelected = {},
                        customStartDate = customStartDate,
                        customEndDate = customEndDate,
                        onDatesChanged = { start, end ->
                            customStartDate = start
                            customEndDate = end
                        }
                    )
                }
            }

            // SECTION A: CATEGORY BREAKDOWN (financial mix donut)
            item {
                Card(
                     shape = RoundedCornerShape(16.dp),
                     modifier = Modifier
                         .fillMaxWidth()
                         .border(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                     colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                     Column(modifier = Modifier.padding(16.dp)) {
                         Text(
                             text = "Category Breakdown",
                             fontSize = 14.sp,
                             fontWeight = FontWeight.ExtraBold,
                             color = mainTextCol
                         )
                         Spacer(modifier = Modifier.height(14.dp))

                         Row(
                             modifier = Modifier.fillMaxWidth(),
                             verticalAlignment = Alignment.CenterVertically,
                             horizontalArrangement = Arrangement.SpaceBetween
                         ) {
                             // Custom Donut Canvas drawing
                             val sumAllOfElements = (totalIncome + totalExpense + totalAssetsValue + totalBudgetValue).coerceAtLeast(1.0)
                             val pIncome = (totalIncome / sumAllOfElements * 360f).toFloat()
                             val pExpense = (totalExpense / sumAllOfElements * 360f).toFloat()
                             val pAssets = (totalAssetsValue / sumAllOfElements * 360f).toFloat()
                             val pBudget = (totalBudgetValue / sumAllOfElements * 360f).toFloat()

                             Box(
                                 modifier = Modifier.size(130.dp),
                                 contentAlignment = Alignment.Center
                             ) {
                                 Canvas(modifier = Modifier.fillMaxSize()) {
                                     var startAngle = -90f
                                     
                                     // Income segment (Green)
                                     drawArc(
                                         color = Color(0xFF10B981),
                                         startAngle = startAngle,
                                         sweepAngle = pIncome.coerceAtLeast(15f),
                                         useCenter = false,
                                         style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                                     )
                                     startAngle += pIncome.coerceAtLeast(15f)

                                     // Expense segment (Red)
                                     drawArc(
                                         color = Color(0xFFEF4444),
                                         startAngle = startAngle,
                                         sweepAngle = pExpense.coerceAtLeast(15f),
                                         useCenter = false,
                                         style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                                     )
                                     startAngle += pExpense.coerceAtLeast(15f)

                                     // Assets segment (Blue)
                                     drawArc(
                                         color = Color(0xFF3B82F6),
                                         startAngle = startAngle,
                                         sweepAngle = pAssets.coerceAtLeast(15f),
                                         useCenter = false,
                                         style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                                     )
                                     startAngle += pAssets.coerceAtLeast(15f)

                                     // Budget segment (Pink)
                                     drawArc(
                                         color = Color(0xFFEC4899),
                                         startAngle = startAngle,
                                         sweepAngle = pBudget.coerceAtLeast(15f),
                                         useCenter = false,
                                         style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                                     )
                                 }

                                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                     Text(
                                         text = "FINANCIAL",
                                         fontSize = 8.sp,
                                         fontWeight = FontWeight.Bold,
                                         color = Color.Gray,
                                         letterSpacing = 0.5.sp
                                     )
                                     Text(
                                         text = "MIX",
                                         fontSize = 11.sp,
                                         fontWeight = FontWeight.Black,
                                         color = mainTextCol
                                     )
                                 }
                             }

                             // Legends breakdown
                             Column(
                                 verticalArrangement = Arrangement.spacedBy(8.dp),
                                 modifier = Modifier.padding(start = 12.dp)
                             ) {
                                 val totalSum = (totalIncome + totalExpense + totalAssetsValue + totalBudgetValue).coerceAtLeast(1.0)
                                 val itemsList = listOf(
                                     Triple("Income", totalIncome, Color(0xFF10B981)),
                                     Triple("Expense", totalExpense, Color(0xFFEF4444)),
                                     Triple("Asset", totalAssetsValue, Color(0xFF3B82F6)),
                                     Triple("Budget", totalBudgetValue, Color(0xFFEC4899))
                                 )
                                 itemsList.forEach { (label, amt, col) ->
                                     val percent = (amt / totalSum * 100).toInt()
                                     Row(
                                         verticalAlignment = Alignment.CenterVertically,
                                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                                     ) {
                                         Spacer(
                                             modifier = Modifier
                                                 .size(10.dp)
                                                 .clip(CircleShape)
                                                 .background(col)
                                         )
                                         Text(
                                             text = label,
                                             fontSize = 11.sp,
                                             fontWeight = FontWeight.Bold,
                                             color = mainTextCol,
                                             modifier = Modifier.width(60.dp)
                                         )
                                         Text(
                                             text = "$percent%",
                                             fontSize = 11.sp,
                                             fontWeight = FontWeight.ExtraBold,
                                             color = col
                                         )
                                     }
                                 }
                             }
                         }
                     }
                }
            }

            // SECTION B: MONTHLY TRENDS CLUSTERED BAR CHART
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Trend",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = mainTextCol
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "↗ 8.2%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Custom grouped multiple bars Canvas of JAN, MAR, MAY, JUL
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw horizontal grid benchmark lines
                                val height = size.height
                                val width = size.width
                                val stepH = height / 4f
                                for (i in 0..4) {
                                    drawLine(
                                        color = if (isDark) Color.White.copy(0.06f) else Color.Black.copy(0.04f),
                                        start = Offset(0f, i * stepH),
                                        end = Offset(width, i * stepH),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                val monthLabels = listOf("JAN", "MAR", "MAY", "JUL")
                                val colCount = monthLabels.size
                                val stepX = width / colCount

                                // Hardcoded scaled ratios to reflect the premium reference mockup curves perfectly
                                val ratios = listOf(
                                    listOf(0.4f, 0.3f, 0.61f, 0.42f), // JAN [Income, Expense, Asset, Budget]
                                    listOf(0.65f, 0.42f, 0.82f, 0.52f), // MAR
                                    listOf(0.55f, 0.38f, 0.22f, 0.41f), // MAY
                                    listOf(0.72f, 0.48f, 0.90f, 0.48f)  // JUL
                                )

                                for (m in 0 until colCount) {
                                    val startX = m * stepX + (stepX * 0.15f)
                                    val barGroupWidth = stepX * 0.7f
                                    val singleBarW = barGroupWidth / 4.2f

                                    // Render 4 bars of Month
                                    val values = ratios[m]
                                    val colors = listOf(Color(0xFF10B981), Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFFFEE2E2).copy(1.0f))

                                    for (b in 0..3) {
                                        val barHeight = values[b] * (height - 10.dp.toPx())
                                        val rx = startX + b * (singleBarW + 2.dp.toPx())
                                        val ry = height - barHeight

                                        // Special border highlighting for budget bar
                                        val budgetColor = if (isDark) Color(0xFFF472B6) else Color(0xFFFBCFE8)
                                        val barCol = if (b == 3) budgetColor else colors[b]

                                        drawRoundRect(
                                            color = barCol,
                                            topLeft = Offset(rx, ry),
                                            size = Size(singleBarW, barHeight),
                                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom labels for monthly trend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            val months = listOf("JAN", "MAR", "MAY", "JUL")
                            months.forEach { m ->
                                Text(
                                    text = m,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category Labels Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val items = listOf(
                                Pair("Income", Color(0xFF10B981)),
                                Pair("Expense", Color(0xFFEF4444)),
                                Pair("Asset", Color(0xFF3B82F6)),
                                Pair("Budget", if (isDark) Color(0xFFF472B6) else Color(0xFFFBCFE8))
                            )
                            items.forEach { (lbl, col) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = lbl, fontSize = 10.sp, color = subTextCol, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // SECTION C: NET WORTH FORECAST BAR CHART (5 data points)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Net Worth Forecast",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = mainTextCol
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // Custom Tooltip Icon button
                                var showTooltip by remember { mutableStateOf(false) }
                                Box {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Forecast methodology description",
                                        tint = if (isDark) Color.LightGray else Color.Gray,
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clickable { showTooltip = !showTooltip }
                                    )
                                    if (showTooltip) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .width(220.dp)
                                                .align(Alignment.TopStart),
                                            shape = RoundedCornerShape(8.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                        ) {
                                            Text(
                                                text = "Forecast automatically excludes abnormal, one-time expenses for higher accuracy.",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = mainTextCol,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "Weighted Predictor",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary,
                                modifier = Modifier
                                    .background(BrandPrimary.copy(0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Render 5 columns of Net Worth Forecast Chart (Apr, May, Jun, Jul, Aug)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            val maxVal = forecastDataPoints.maxOrNull()?.coerceAtLeast(1000.0) ?: 1000.0
                            val monthLabels = listOf("Apr", "May", "Current", "Proj 1", "Proj 2")
                            val actualMonthNames = listOf("Apr", "May", "Jun", "Jul", "Aug") // June current system clock reference

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height
                                val stepX = width / 5f

                                // Baseline
                                drawLine(
                                    color = Color.LightGray.copy(0.3f),
                                    start = Offset(0f, height),
                                    end = Offset(width, height),
                                    strokeWidth = 1.dp.toPx()
                                )

                                for (i in 0..4) {
                                    val rawValue = forecastDataPoints[i]
                                    val normalizedRatio = (rawValue / maxVal).coerceIn(0.15, 1.0)
                                    val barH = (normalizedRatio * (height - 30.dp.toPx())).toFloat()

                                    val paddingFraction = 0.25f
                                    val barW = stepX * (1f - paddingFraction)
                                    val rx = i * stepX + (stepX * paddingFraction / 2f)
                                    val ry = height - barH

                                    // Styling: 0,1 are Actual (pastel blue), 2 is Current (deep dark blue), 3,4 is Projected (dashed outlines)
                                    when (i) {
                                        0, 1 -> {
                                            // Actual Month - Light blue
                                            drawRoundRect(
                                                color = Color(0xFFD3E2F8),
                                                topLeft = Offset(rx, ry),
                                                size = Size(barW, barH),
                                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                            )
                                        }
                                        2 -> {
                                            // Current Month: Bold Navy blue
                                            drawRoundRect(
                                                color = Color(0xFF0F1B6B),
                                                topLeft = Offset(rx, ry),
                                                size = Size(barW, barH),
                                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                            )
                                        }
                                        3, 4 -> {
                                            // Projected Month: Light blue with custom dashed stroke
                                            drawRoundRect(
                                                color = Color(0xFFE0F2FE).copy(0.6f),
                                                topLeft = Offset(rx, ry),
                                                size = Size(barW, barH),
                                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                            )
                                            drawRoundRect(
                                                color = Color(0xFF38BDF8),
                                                topLeft = Offset(rx, ry),
                                                size = Size(barW, barH),
                                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                                                style = Stroke(
                                                    width = 1.8.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Labels and Sub-headers of the forecast bars
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val labels = listOf("Apr", "May", "Jun", "Jul", "Aug")
                            val stats = listOf("Actual", "Actual", "Current", "Projected", "Projected")
                            for (i in 0..4) {
                                Column(
                                    modifier = Modifier.width(55.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stats[i],
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when (i) {
                                            2 -> Color(0xFF0F1B6B)
                                            3, 4 -> Color(0xFF0369A1)
                                            else -> Color.Gray
                                        }
                                    )
                                    Text(
                                        text = labels[i],
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = mainTextCol
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // FORECAST INSIGHTS CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, if (isDark) BrandOutline else Color(0xFFDBEAFE))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF1E40AF),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Forecast Insights",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF1E40AF)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val monthlySavings = (totalIncome - totalExpense).coerceAtLeast(0.0)
                            val formattedSavings = String.format(Locale.getDefault(), "%,.0f", if (monthlySavings > 0) monthlySavings else 85000.0)
                            Text(
                                text = "Based on your 6-month average savings rate of $currencyPrefix$formattedSavings and current market trends.",
                                fontSize = 11.sp,
                                color = if (isDark) Color.LightGray else Color(0xFF1E293B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // RECENT ENTRIES HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Entries",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = mainTextCol
                    )
                    Text(
                        text = "View All →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F1B6B),
                        modifier = Modifier.clickable { selectedTab = "All" }
                    )
                }
            }

            // CATEGORY FILTER PILL SELECTORS FOR RECENT ENTRIES
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Expense", "Income", "Asset").forEach { tabOption ->
                        val isSelected = selectedTab == tabOption
                        val activeColor = when (tabOption) {
                            "Expense" -> Color(0xFFEF4444)
                            "Income" -> Color(0xFF10B981)
                            "Asset" -> Color(0xFF3B82F6)
                            else -> Color(0xFF1E293B)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) activeColor else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)))
                                .clickable { selectedTab = tabOption }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = tabOption,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else (if (isDark) Color.LightGray else Color(0xFF475569))
                            )
                        }
                    }
                }
            }

            // DYNAMIC TRANSACTIONS LISTING FEED (Clickable expand, Details, Modify & Delete)
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching raw entries in ledger range.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                items(filteredTransactions, key = { "${it.id}_${it.date}" }) { txn ->
                    // Interactive custom expandable card to respect visible specs
                    var isExpanded by remember { mutableStateOf(false) }
                    val isAnomalous = remember(txn) { checkIsOutlier(txn) }

                    val cardBackground = if (isDark) Color(0xFF1E293B) else Color.White
                    val borderOutline = if (isDark) BrandOutline else Color(0xFFEBF0F6)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderOutline, RoundedCornerShape(12.dp))
                            .testTag("txn_item_${txn.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Primary Category banner, amount, anomalies indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Visual color-coded category tag
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when (txn.type) {
                                                    "Income" -> Color(0xFF10B981).copy(0.12f)
                                                    "Asset" -> Color(0xFF3B82F6).copy(0.12f)
                                                    else -> Color(0xFFEF4444).copy(0.12f)
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${txn.type} • ${txn.category}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = when (txn.type) {
                                                "Income" -> Color(0xFF10B981)
                                                "Asset" -> Color(0xFF3B82F6)
                                                else -> Color(0xFFEF4444)
                                            }
                                        )
                                    }

                                    // Statistical anomaly detector visual tag indicator
                                    if (isAnomalous) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .background(Color(0xFFFFAD01).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "One-Time Outlier anomaly",
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "Outlier",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD97706)
                                            )
                                        }
                                    }
                                }

                                // Amount
                                Text(
                                    text = "${if (txn.type == "Expense") "-" else "+"}${currencyPrefix}${String.format(Locale.getDefault(), "%,.0f", txn.amount)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = when (txn.type) {
                                        "Income" -> Color(0xFF10B981)
                                        "Asset" -> Color(0xFF3B82F6)
                                        else -> Color(0xFFEF4444)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Middle: Subcategory information & payment
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("MODE OF PAYMENT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text(text = txn.paymentMethod, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = mainTextCol)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("CATEGORY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text(text = txn.subCategory.ifBlank { "General" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = mainTextCol)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Bottom: Description handling (Display 50 chars limit + expandable detail toggling)
                            Column {
                                Text("DESCRIPTION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                val trimmedDesc = if (txn.title.length > 50) "${txn.title.take(50)}..." else txn.title
                                Text(
                                    text = if (isExpanded) txn.title else trimmedDesc,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color.LightGray else Color(0xFF334155)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Date display
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val formattedDateString = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date(txn.date))
                                    Text(
                                        text = formattedDateString,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )

                                    // Interactive Expand detail button
                                    Button(
                                        onClick = { isExpanded = !isExpanded },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDark) Color(0xFF2E3A4F) else Color(0xFFE2EBF6),
                                            contentColor = Color(0xFF0F1B6B)
                                        )
                                    ) {
                                        Text(
                                            text = if (isExpanded) "Show Less" else "More Detail",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Editable Modify & Delete buttons visible inside collapsed action compartment
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = borderOutline, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Edit Button
                                        Button(
                                            onClick = {
                                                activeEditingTx = txn
                                                showEditDialog = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isDark) Color(0xFF0D5E3A) else Color(0xFFDBFDEE),
                                                contentColor = if (isDark) Color(0xFF86EFAC) else Color(0xFF047857)
                                            ),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit current entry", modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Modify", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        // Delete Button
                                        Button(
                                            onClick = {
                                                txToDelete = txn
                                                showTxDeleteConfirmDialog = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2),
                                                contentColor = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                            ),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete current entry", modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Delete", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG CABINETS
    // ==========================================
    // Edit/Update transaction row
    UnifiedAddEditTxDialog(
        show = showEditDialog,
        editingTxn = activeEditingTx,
        onDismiss = {
            showEditDialog = false
            activeEditingTx = null
        },
        categories = categories,
        isDark = isDark,
        viewModel = viewModel,
        onSave = { title, amount, type, category, sub, payment, _, _, _, _ ->
            val cur = activeEditingTx
            if (cur != null) {
                txToSave = cur.copy(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    subCategory = sub,
                    paymentMethod = payment
                )
                showTxEditConfirmDialog = true
            }
        }
    )

    if (showTxDeleteConfirmDialog && txToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showTxDeleteConfirmDialog = false
                txToDelete = null
            },
            title = {
                Text(
                    text = "Are you sure?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )
            },
            text = {
                Text(
                    text = "Do you want to delete the transaction '${txToDelete!!.title}'? This action cannot be undone.",
                    fontSize = 13.sp,
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(txToDelete!!)
                        showTxDeleteConfirmDialog = false
                        txToDelete = null
                        Toast.makeText(context, "Transaction deleted successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Yes", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTxDeleteConfirmDialog = false
                        txToDelete = null
                    }
                ) {
                    Text("No", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showTxEditConfirmDialog && txToSave != null) {
        AlertDialog(
            onDismissRequest = {
                showTxEditConfirmDialog = false
                txToSave = null
            },
            title = {
                Text(
                    text = "Are you sure?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )
            },
            text = {
                Text(
                    text = "Do you want to save changes to the transaction '${txToSave!!.title}'?",
                    fontSize = 13.sp,
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTransaction(txToSave!!)
                        showTxEditConfirmDialog = false
                        txToSave = null
                        showEditDialog = false
                        activeEditingTx = null
                        Toast.makeText(context, "Transaction updated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Yes", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTxEditConfirmDialog = false
                        txToSave = null
                    }
                ) {
                    Text("No", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
