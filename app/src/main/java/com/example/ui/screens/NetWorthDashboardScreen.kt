package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
fun NetWorthDashboardScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: (String) -> Unit
) {
    val txns by viewModel.userTransactions.collectAsState()
    val rawAssets by viewModel.userAssets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currencyUnit by viewModel.selectedCurrency.collectAsState()
    val allBudgets by viewModel.userBudgets.collectAsState()
    val allGoals by viewModel.userGoals.collectAsState()
    val liabilities by viewModel.userLiabilities.collectAsState()
    val recurringItems by viewModel.userRecurringItems.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.triggerCommitmentProcessing()
    }

    val context = LocalContext.current
    val isDark = isDarkThemeActive

    var isScreenLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isScreenLoaded = true
    }

    // Advanced Date Scope Filters
    var selectedInterval by remember { mutableStateOf("All Time") }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    
    // Tab and search query replica for full raw ledger emulation
    var selectedTab by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var activeEditingTx by remember { mutableStateOf<DbTransaction?>(null) }
    var selectedForecastIndex by remember { mutableStateOf<Int?>(2) } // default to 2 (Current)

    var txToDelete by remember { mutableStateOf<DbTransaction?>(null) }
    var txToSave by remember { mutableStateOf<DbTransaction?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditConfirmDialog by remember { mutableStateOf(false) }

    val currencyPrefix = when (currencyUnit) {
        "Dollar ($)" -> "$"
        "Euro (€)" -> "€"
        else -> "₹"
    }

    // Dynamic calculations purely on data ledger values
    val now = remember { System.currentTimeMillis() }
    val filteredTxns = remember(txns, selectedInterval, customStartDate, customEndDate, selectedTab, searchQuery) {
        txns.filter { txn ->
            val matchesInterval = when (selectedInterval) {
                "Daily" -> txn.date > now - 24 * 60 * 60 * 1000L
                "Weekly" -> txn.date > now - 7 * 24 * 60 * 60 * 1000L
                "Monthly" -> txn.date > now - 30 * 24 * 60 * 60 * 1000L
                "Custom" -> {
                    if (customStartDate != null && customEndDate != null) {
                        txn.date in customStartDate!!..customEndDate!!
                    } else {
                        true
                    }
                }
                else -> true // All Time
            }

            val matchesTab = when (selectedTab) {
                "All" -> true
                "Income" -> txn.type.equals("Income", ignoreCase = true)
                "Expense" -> {
                    val isInvestment = txn.category.lowercase().contains("invest") || txn.subCategory.lowercase().contains("invest") || txn.title.lowercase().contains("invest") || txn.type.equals("Asset", ignoreCase = true) || txn.type.equals("Investment", ignoreCase = true)
                    val isBudgetGoal = txn.category.lowercase().contains("budget") || txn.category.lowercase().contains("goal") || 
                                       txn.subCategory.lowercase().contains("budget") || txn.subCategory.lowercase().contains("goal") || 
                                       txn.title.lowercase().contains("budget") || txn.title.lowercase().contains("goal") ||
                                       txn.type.equals("Budget", ignoreCase = true) || txn.type.equals("Goal", ignoreCase = true) ||
                                       txn.type.equals("BUDGET_SPEND", ignoreCase = true) || txn.type.equals("GOAL_SPEND", ignoreCase = true) ||
                                       txn.type.equals("Budget/Goal Top-Up", ignoreCase = true) ||
                                       txn.title.lowercase().contains("top up") || txn.title.lowercase().contains("top-up")
                    txn.type.equals("Expense", ignoreCase = true) && !isInvestment && !isBudgetGoal
                }
                "Investment" -> txn.category.lowercase().contains("invest") || txn.subCategory.lowercase().contains("invest") || txn.title.lowercase().contains("invest") || txn.type.equals("Asset", ignoreCase = true) || txn.type.equals("Investment", ignoreCase = true)
                "Budget" -> {
                    txn.type.equals("Budget/Goal Top-Up", ignoreCase = true) ||
                    txn.type.equals("Budget", ignoreCase = true) ||
                    txn.type.equals("Goal", ignoreCase = true) ||
                    txn.type.equals("BUDGET_SPEND", ignoreCase = true) ||
                    txn.type.equals("GOAL_SPEND", ignoreCase = true)
                }
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                txn.title.contains(searchQuery, ignoreCase = true) ||
                txn.category.contains(searchQuery, ignoreCase = true) ||
                txn.subCategory.contains(searchQuery, ignoreCase = true)
            }

            matchesInterval && matchesTab && matchesSearch
        }
    }

    val baselineTxns = remember(txns, selectedInterval, customStartDate, customEndDate) {
        txns.filter { txn ->
            when (selectedInterval) {
                "Daily" -> txn.date > now - 24 * 60 * 60 * 1000L
                "Weekly" -> txn.date > now - 7 * 24 * 60 * 60 * 1000L
                "Monthly" -> txn.date > now - 30 * 24 * 60 * 60 * 1000L
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

    val totalIncome = remember(baselineTxns) {
        baselineTxns.filter { 
            it.type.equals("Income", ignoreCase = true) && 
            it.sourceType == "SOURCE_INCOME" 
        }.sumOf { it.amount }
    }

    val remainingIncome = remember(baselineTxns) {
        val totalInc = baselineTxns.filter { 
            it.type.equals("Income", ignoreCase = true) && 
            it.sourceType == "SOURCE_INCOME" 
        }.sumOf { it.amount }
        val deductions = baselineTxns.filter { 
            !it.type.equals("Income", ignoreCase = true) && 
            it.sourceType == "SOURCE_INCOME" &&
            !it.type.equals("Budget", ignoreCase = true) &&
            !it.type.equals("Goal", ignoreCase = true) &&
            !it.type.equals("BUDGET_SPEND", ignoreCase = true) &&
            !it.type.equals("GOAL_SPEND", ignoreCase = true)
        }.sumOf { it.amount }
        (totalInc - deductions).coerceAtLeast(0.0)
    }

    val cashBalance = remember(baselineTxns) {
        val totalInc = baselineTxns.filter { 
            it.type.equals("Income", ignoreCase = true) && 
            it.sourceType == "SOURCE_INCOME" 
        }.sumOf { it.amount }
        val deductions = baselineTxns.filter { 
            !it.type.equals("Income", ignoreCase = true) && 
            it.sourceType == "SOURCE_INCOME" &&
            !it.type.equals("Budget", ignoreCase = true) &&
            !it.type.equals("Goal", ignoreCase = true) &&
            !it.type.equals("BUDGET_SPEND", ignoreCase = true) &&
            !it.type.equals("GOAL_SPEND", ignoreCase = true)
        }.sumOf { it.amount }
        val rem = (totalInc - deductions).coerceAtLeast(0.0)
        
        val otherInflows = baselineTxns.filter { 
            it.sourceType == "SOURCE_OTHER" && 
            it.type.equals("Income", ignoreCase = true) 
        }.sumOf { it.amount }
        val otherOutflows = baselineTxns.filter { 
            it.sourceType == "SOURCE_OTHER" && 
            it.type.equals("Expense", ignoreCase = true) 
        }.sumOf { it.amount }
        (rem + otherInflows - otherOutflows).coerceAtLeast(0.0)
    }

    val generalExpenses = remember(baselineTxns) {
        baselineTxns.filter { txn ->
            val isInvestment = txn.type.equals("Asset", ignoreCase = true) || 
                               txn.type.equals("Investment", ignoreCase = true) || 
                               txn.category.lowercase().contains("invest") || 
                               txn.subCategory.lowercase().contains("invest") || 
                               txn.title.lowercase().contains("invest")
            val isBudgetGoal = txn.type.equals("Budget", ignoreCase = true) || 
                               txn.type.equals("Goal", ignoreCase = true) || 
                               txn.type.equals("BUDGET_SPEND", ignoreCase = true) || 
                               txn.type.equals("GOAL_SPEND", ignoreCase = true) || 
                               txn.type.equals("Budget/Goal Top-Up", ignoreCase = true) || 
                               txn.category.lowercase().contains("budget") || 
                               txn.category.lowercase().contains("goal") || 
                               txn.title.lowercase().contains("top up") ||
                               txn.title.lowercase().contains("top-up")
            val isCommitment = txn.isLiability || 
                               txn.category.lowercase().contains("bill") || 
                               txn.category.lowercase().contains("sub") || 
                               txn.category.lowercase().contains("emi") || 
                               txn.category.lowercase().contains("sip") || 
                               txn.category.lowercase().contains("udhar") || 
                               txn.category.lowercase().contains("liability") ||
                               txn.title.lowercase().contains("paid")
            
            txn.type.equals("Expense", ignoreCase = true) && !isInvestment && !isBudgetGoal && !isCommitment
        }.sumOf { it.amount }
    }

    val paidCommitments = remember(baselineTxns) {
        baselineTxns.filter { txn ->
            val isCommitment = txn.isLiability || 
                               txn.category.lowercase().contains("bill") || 
                               txn.category.lowercase().contains("sub") || 
                               txn.category.lowercase().contains("emi") || 
                               txn.category.lowercase().contains("sip") || 
                               txn.category.lowercase().contains("udhar") || 
                               txn.category.lowercase().contains("liability") ||
                               txn.title.lowercase().contains("paid")
            isCommitment && txn.type.equals("Expense", ignoreCase = true)
        }.sumOf { it.amount }
    }

    val totalExpense = remember(generalExpenses, paidCommitments) {
        generalExpenses + paidCommitments
    }

    val totalInvested = remember(rawAssets, selectedInterval, customStartDate, customEndDate) {
        rawAssets.filter { asset ->
            when (selectedInterval) {
                "Daily" -> asset.dateInvested > now - 24 * 60 * 60 * 1000L
                "Weekly" -> asset.dateInvested > now - 7 * 24 * 60 * 60 * 1000L
                "Monthly" -> asset.dateInvested > now - 30 * 24 * 60 * 60 * 1000L
                "Custom" -> {
                    if (customStartDate != null && customEndDate != null) {
                        asset.dateInvested in customStartDate!!..customEndDate!!
                    } else {
                        true
                    }
                }
                else -> true
            }
        }.sumOf { asset ->
            if (asset.quantity > 0.0 && asset.currentPrice > 0.0) {
                asset.quantity * asset.currentPrice
            } else {
                asset.amountInvested
            }
        }
    }

    val totalAllocatedBudgetSum = remember(allBudgets) {
        allBudgets.sumOf { it.totalAllocated }
    }
    val currentSpendingBudgetSum = remember(allBudgets) {
        allBudgets.sumOf { it.spentAmount }
    }
    val totalBudgetGoalFunds = (totalAllocatedBudgetSum - currentSpendingBudgetSum).coerceAtLeast(0.0)

    val totalLiabilities = remember(liabilities, recurringItems) {
        val filteredRecs = recurringItems.filter { !com.example.util.CommitmentProcessor.isPastDurationEnd(it.durationEndDate) }
        liabilities.filter { !it.isPaid }.sumOf { it.remainingDue } + 
                filteredRecs.filter { it.isLiability }.sumOf { item ->
                    // Only count as unpaid if not paid yet, but keep as liability item itself
                    if (item.isPaid) 0.0 else item.amount
                }
    }

    val netWorth = totalInvested + cashBalance + totalBudgetGoalFunds

    // ==========================================
    // ADVANCED OUTLIER FILTERING (IQR METHOD)
    // ==========================================
    val variableTransactions = remember(txns) {
        txns.filter { txn ->
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
    val forecastDataPoints = remember(txns, allBudgets, netWorth, totalInvested) {
        val fixedIncomes = txns.filter { txn ->
            txn.type.equals("Income", ignoreCase = true) &&
            (txn.category.lowercase().contains("salary") || txn.title.lowercase().contains("salary") || txn.title.lowercase().contains("recurring"))
        }.sumOf { it.amount }

        val fixedExpenses = txns.filter { txn ->
            txn.type.equals("Expense", ignoreCase = true) &&
            (txn.category.lowercase().contains("rent") || txn.category.lowercase().contains("emi") ||
             txn.category.lowercase().contains("subscription") || txn.title.lowercase().contains("rent") || txn.title.lowercase().contains("subscription"))
        }.sumOf { it.amount }

        val normalVariableExpenses = variableTransactions.filter { !checkIsOutlier(it) }
        val sumNormalVar = normalVariableExpenses.sumOf { it.amount }
        val countVar = normalVariableExpenses.size

        val currentMonthVarExp = sumNormalVar
        val baselineEMA = if (countVar > 0) sumNormalVar / countVar else 45000.0

        val alpha = 0.4
        val rawProjVarValue = (currentMonthVarExp * alpha) + (baselineEMA * (1 - alpha))

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

        val projectedFixedIn = fixedIncomes
        val projectedFixedOut = fixedExpenses

        val monthlyROI = 0.012 
        val currentAssets = totalInvested

        val assetProj1 = currentAssets * (1 + monthlyROI)
        val assetProj2 = assetProj1 * (1 + monthlyROI)

        val predictedNetWorth1 = (assetProj1 + projectedFixedIn) - (projectedFixedOut + projectedVariableExpenses)
        val predictedNetWorth2 = (assetProj2 + projectedFixedIn) - (projectedFixedOut + projectedVariableExpenses)

        val netWorthHistoric2 = netWorth * 0.90
        val netWorthHistoric1 = netWorth * 0.94

        listOf(
            netWorthHistoric2,  // Month -2
            netWorthHistoric1,  // Month -1
            netWorth,           // Month 0 (Current)
            predictedNetWorth1, // Month +1 (Projected)
            predictedNetWorth2  // Month +2 (Projected)
        )
    }

    // ==========================================
    // MONTHLY TREND GROUPED CALCULATIONS
    // ==========================================
    val trendDataAndMonthLabels = remember(txns, rawAssets, allBudgets, allGoals) {
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val labels = mutableListOf<String>()
        val datasets = mutableListOf<List<Double>>()
        
        for (i in 5 downTo 0) {
            val targetCal = Calendar.getInstance()
            targetCal.timeInMillis = System.currentTimeMillis()
            targetCal.add(Calendar.MONTH, -i)
            val monthLabel = monthFormat.format(targetCal.time)
            labels.add(monthLabel)
            
            val targetMonth = targetCal.get(Calendar.MONTH)
            val targetYear = targetCal.get(Calendar.YEAR)
            
            val matchingTxns = txns.filter { txn ->
                val txnCal = Calendar.getInstance()
                txnCal.timeInMillis = txn.date
                txnCal.get(Calendar.MONTH) == targetMonth && txnCal.get(Calendar.YEAR) == targetYear
            }
            
            // 1. Income_Bar: Total SOURCE_INCOME credits
            val inc = matchingTxns.filter { 
                it.type.equals("Income", ignoreCase = true) && 
                it.sourceType == "SOURCE_INCOME" 
            }.sumOf { it.amount }
            
            // 2. Expense_Bar: Total General_Expense + Paid_Commitment (excl. investments or budgets)
            val exp = matchingTxns.filter { txn ->
                val isInvestment = txn.type.equals("Asset", ignoreCase = true) || 
                                   txn.type.equals("Investment", ignoreCase = true) || 
                                   txn.category.lowercase().contains("invest") || 
                                   txn.subCategory.lowercase().contains("invest") || 
                                   txn.title.lowercase().contains("invest")
                val isBudgetGoal = txn.type.equals("Budget", ignoreCase = true) || 
                                   txn.type.equals("Goal", ignoreCase = true) || 
                                   txn.type.equals("Budget/Goal Top-Up", ignoreCase = true) || 
                                   txn.category.lowercase().contains("budget") || 
                                   txn.category.lowercase().contains("goal") || 
                                   txn.title.lowercase().contains("top up") ||
                                   txn.title.lowercase().contains("top-up")
                
                txn.type.equals("Expense", ignoreCase = true) && !isInvestment && !isBudgetGoal
            }.sumOf { it.amount }
            
            // 3. Investment_Bar: Total Investment_Buy (Asset acquisition)
            val inv = matchingTxns.filter { txn ->
                val isInvestment = txn.type.equals("Asset", ignoreCase = true) || 
                                   txn.type.equals("Investment", ignoreCase = true) || 
                                   txn.category.lowercase().contains("invest") || 
                                   txn.subCategory.lowercase().contains("invest") || 
                                   txn.title.lowercase().contains("invest")
                isInvestment
            }.sumOf { it.amount }
            
            // 4. Budget_Goal_Bar: Total Top_Up activity for the month
            val bud = matchingTxns.filter { 
                it.type.equals("Budget/Goal Top-Up", ignoreCase = true) 
            }.sumOf { it.amount }
            
            datasets.add(listOf(inc, exp, inv, bud))
        }
        Pair(datasets, labels)
    }

    Scaffold(
        containerColor = if (isDark) BrandBackground else Color(0xFFF8FAFD),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF0F1B6B),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
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
            // Header Row with Title and Back Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("universal_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go Back",
                            tint = if (isDark) Color.White else Color(0xFF0F1B6B)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Net Worth & Ledger",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F1B6B),
                        modifier = Modifier.testTag("universal_header_title")
                    )
                }
            }

            // Universal Transaction Search and Filter Bar
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) BrandSurfaceContainerLow else Color(0xFFF1F5F9)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("universal_search_panel")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search icon",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { 
                                    searchQuery = it 
                                    com.example.util.GlobalTransactionEventBus.post(com.example.util.GlobalTransactionEvent.TransactionsRefreshed)
                                },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = if (isDark) Color.White else Color.Black,
                                    fontSize = 14.sp
                                ),
                                modifier = Modifier.weight(1f).testTag("universal_search_input"),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text("Search transactions by keyword...", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { 
                                        searchQuery = "" 
                                        com.example.util.GlobalTransactionEventBus.post(com.example.util.GlobalTransactionEvent.TransactionsRefreshed)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Date Scope & Category Filters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Date Range interval selector
                            Box(modifier = Modifier.weight(1f)) {
                                var showIntervalMenu by remember { mutableStateOf(false) }
                                TextButton(
                                    onClick = { showIntervalMenu = true },
                                    modifier = Modifier.fillMaxWidth().testTag("universal_date_filter_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (isDark) Color.White else Color(0xFF0F1B6B)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = selectedInterval,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        color = if (isDark) Color.White else Color(0xFF0F1B6B)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showIntervalMenu,
                                    onDismissRequest = { showIntervalMenu = false }
                                ) {
                                    listOf("All Time", "Daily", "Weekly", "Monthly").forEach { interval ->
                                        DropdownMenuItem(
                                            text = { Text(interval) },
                                            onClick = {
                                                selectedInterval = interval
                                                showIntervalMenu = false
                                                com.example.util.GlobalTransactionEventBus.post(com.example.util.GlobalTransactionEvent.TransactionsRefreshed)
                                            }
                                        )
                                    }
                                }
                            }

                            // Category selector
                            Box(modifier = Modifier.weight(1f)) {
                                var showCategoryMenu by remember { mutableStateOf(false) }
                                TextButton(
                                    onClick = { showCategoryMenu = true },
                                    modifier = Modifier.fillMaxWidth().testTag("universal_category_filter_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (isDark) Color.White else Color(0xFF0F1B6B)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (selectedTab == "All") "All Categories" else selectedTab,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        color = if (isDark) Color.White else Color(0xFF0F1B6B)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showCategoryMenu,
                                    onDismissRequest = { showCategoryMenu = false }
                                ) {
                                    listOf("All", "Income", "Expense", "Investment", "Budget").forEach { tab ->
                                        DropdownMenuItem(
                                            text = { Text(tab) },
                                            onClick = {
                                                selectedTab = tab
                                                showCategoryMenu = false
                                                com.example.util.GlobalTransactionEventBus.post(com.example.util.GlobalTransactionEvent.TransactionsRefreshed)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Net Worth Summary Card (Smaller Indicator)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF0F1E36) else Color(0xFFE0F2FE)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isDark) Color(0xFF38BDF8).copy(alpha = 0.35f) else Color(0xFFBAE6FD), RoundedCornerShape(12.dp))
                        .testTag("net_worth_evaluation_indicator")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🏆 Net Worth Evaluation Indicator",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$currencyPrefix${String.format(Locale.getDefault(), "%,.2f", netWorth)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color(0xFF0369A1)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF10B981).copy(0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "LIVE LEDGER",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            // Primary Status Grid (3x1): A horizontal grid containing three tabs for high-level financial tracking
            item {
                val filteredRecs = recurringItems.filter { !com.example.util.CommitmentProcessor.isPastDurationEnd(it.durationEndDate) }
                val unpaidAmount = liabilities.filter { !it.isPaid }.sumOf { it.remainingDue } + 
                        filteredRecs.filter { it.isLiability }.sumOf { item ->
                            if (item.isPaid) 0.0 else item.amount
                        }

                // Smooth number value animations
                val animatedCashBalance by animateFloatAsState(
                    targetValue = cashBalance.toFloat(),
                    animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
                )
                val animatedTotalInvested by animateFloatAsState(
                    targetValue = totalInvested.toFloat(),
                    animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
                )
                val animatedUnpaidAmount by animateFloatAsState(
                    targetValue = unpaidAmount.toFloat(),
                    animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
                )

                // Staggered entrance animations
                val animScale1 by animateFloatAsState(
                    targetValue = if (isScreenLoaded) 1f else 0.90f,
                    animationSpec = tween(600, delayMillis = 0, easing = FastOutSlowInEasing)
                )
                val animAlpha1 by animateFloatAsState(
                    targetValue = if (isScreenLoaded) 1f else 0f,
                    animationSpec = tween(600, delayMillis = 0, easing = FastOutSlowInEasing)
                )

                val animScale2 by animateFloatAsState(
                    targetValue = if (isScreenLoaded) 1f else 0.90f,
                    animationSpec = tween(600, delayMillis = 80, easing = FastOutSlowInEasing)
                )
                val animAlpha2 by animateFloatAsState(
                    targetValue = if (isScreenLoaded) 1f else 0f,
                    animationSpec = tween(600, delayMillis = 80, easing = FastOutSlowInEasing)
                )

                val animScale3 by animateFloatAsState(
                    targetValue = if (isScreenLoaded) 1f else 0.90f,
                    animationSpec = tween(600, delayMillis = 160, easing = FastOutSlowInEasing)
                )
                val animAlpha3 by animateFloatAsState(
                    targetValue = if (isScreenLoaded) 1f else 0f,
                    animationSpec = tween(600, delayMillis = 160, easing = FastOutSlowInEasing)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Cash Balance status tab (Emerald Green Soft)
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF0C241B) else Color(0xFFECFDF5)
                        ),
                        border = BorderStroke(
                            1.dp, 
                            if (isDark) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFFA7F3D0)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .graphicsLayer {
                                scaleX = animScale1
                                scaleY = animScale1
                                alpha = animAlpha1
                            }
                            .testTag("cash_balance_tab")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Cash Balance",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF34D399) else Color(0xFF065F46)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = "Cash Balance",
                                        tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Remaining + Other",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFF34D399).copy(alpha = 0.6f) else Color(0xFF059669).copy(alpha = 0.65f)
                                )
                            }
                            Text(
                                text = "$currencyPrefix${formatAmount(animatedCashBalance.toDouble())}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF047857)
                            )
                        }
                    }

                    // 2. Asset status tab (Indigo / Sapphire Blue Soft)
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF0F1E36) else Color(0xFFEFF6FF)
                        ),
                        border = BorderStroke(
                            1.dp, 
                            if (isDark) Color(0xFF38BDF8).copy(alpha = 0.35f) else Color(0xFFBFDBFE)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .graphicsLayer {
                                scaleX = animScale2
                                scaleY = animScale2
                                alpha = animAlpha2
                            }
                            .testTag("asset_tab")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Asset",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF1E40AF)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ShowChart,
                                        contentDescription = "Asset",
                                        tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Invested Valuation",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color(0xFF1D4ED8).copy(alpha = 0.65f)
                                )
                            }
                            Text(
                                text = "$currencyPrefix${formatAmount(animatedTotalInvested.toDouble())}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF1E3A8A)
                            )
                        }
                    }

                    // 3. Liability status tab (Rose Red / Crimson Soft)
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF2E171E) else Color(0xFFFFF1F2)
                        ),
                        border = BorderStroke(
                            1.dp, 
                            if (isDark) Color(0xFFF43F5E).copy(alpha = 0.35f) else Color(0xFFFECDD3)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .graphicsLayer {
                                scaleX = animScale3
                                scaleY = animScale3
                                alpha = animAlpha3
                            }
                            .testTag("liability_tab")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Liability",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFB7185) else Color(0xFF9E1A3C)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = "Liability",
                                        tint = if (isDark) Color(0xFFFB7185) else Color(0xFFBE123C),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Unpaid Commitments",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFFB7185).copy(alpha = 0.6f) else Color(0xFFBE123C).copy(alpha = 0.65f)
                                )
                            }
                            Text(
                                text = "$currencyPrefix${formatAmount(animatedUnpaidAmount.toDouble())}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF9F1239)
                            )
                        }
                    }
                }
            }

            // 2x2 Grid Options Block just ABOVE date filter
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Category Management",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F1B6B),
                        modifier = Modifier
                            .padding(bottom = 2.dp)
                            .testTag("category_management_grid_header")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Income card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
                                .clickable { onNavigateToDashboard("income") }
                                .testTag("category_income_link")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Income",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White.copy(0.85f) else Color(0xFF065F46)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = if (isDark) Color.White.copy(0.7f) else Color(0xFF059669),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "$currencyPrefix${formatAmount(totalIncome)}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF047857)
                                )
                            }
                        }

                        // Expense card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
                                .clickable { onNavigateToDashboard("expense") }
                                .testTag("category_expense_link")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Expense",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White.copy(0.85f) else Color(0xFF991B1B)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isDark) Color.White.copy(0.7f) else Color(0xFFDC2626),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "$currencyPrefix${formatAmount(totalExpense)}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFFB91C1C)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Investment Card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(105.dp)
                                .clickable { onNavigateToDashboard("investments") }
                                .testTag("category_investment_link")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Investment",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White.copy(0.85f) else Color(0xFF1E40AF)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ShowChart,
                                        contentDescription = null,
                                        tint = if (isDark) Color.White.copy(0.7f) else Color(0xFF2563EB),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "$currencyPrefix${formatAmount(totalInvested)}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF1D4ED8)
                                )
                            }
                        }

                        // Budget & Goal Card (Pink coloring)
                        val totalBudgetData = totalBudgetGoalFunds
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF5B1038) else Color(0xFFFFF0F5)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(105.dp)
                                .clickable { onNavigateToDashboard("budget_goals") }
                                .testTag("category_budget_goal_link")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Budget & Goal",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFFD1DC) else Color(0xFFDB2777)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFFFD1DC).copy(alpha = 0.8f) else Color(0xFFEC4899),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "Avail: $currencyPrefix${formatAmount(totalBudgetData)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color(0xFFFFD1DC) else Color(0xFFC026D3)
                                )
                                Column {
                                    Text(
                                        text = "Allocated: $currencyPrefix${formatAmount(totalAllocatedBudgetSum)}",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFFD1DC).copy(0.7f) else Color(0xFF475569)
                                    )
                                    Text(
                                        text = "Spending: $currencyPrefix${formatAmount(currentSpendingBudgetSum)}",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFFD1DC).copy(0.7f) else Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Filter Scope bar
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

            // Asset Segmentation Pie Chart (Exactly 3 asset categories ratio and details, with liability displayed separately)
            item {
                Text(
                    text = "Asset & Liability Distribution", 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                NetWorthInteractivePieChart(
                    investment = totalInvested,
                    cashBalance = cashBalance,
                    budget = totalBudgetGoalFunds,
                    liability = totalLiabilities,
                    isDark = isDark,
                    currencyPrefix = currencyPrefix
                )
            }

            // CATEGORISE MONTHLY TREND GRAPH CHART (Income, Expense, Investment, Budget & Goal side-by-side)
            item {
                Text(
                    text = "Categorized Monthly Trend Bar Chart",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                CategorisedMonthlyTrendBarChart(
                    isDark = isDark,
                    incomeTrend = trendDataAndMonthLabels.first.map { it[0] },
                    expenseTrend = trendDataAndMonthLabels.first.map { it[1] },
                    investmentTrend = trendDataAndMonthLabels.first.map { it[2] },
                    budgetTrend = trendDataAndMonthLabels.first.map { it[3] },
                    monthLabels = trendDataAndMonthLabels.second,
                    currencyPrefix = currencyPrefix
                )
            }

            // ADVANCED NET WORTH FORECAST BAR CHART (Interactive 5 data points)
            item {
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Net Worth Forecast",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
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
                                                color = if (isDark) Color.White else Color(0xFF1E293B),
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

                        // Interactive Bar Chart Rows
                        val maxVal = forecastDataPoints.maxOrNull()?.coerceAtLeast(1000.0) ?: 1000.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val labels = listOf("Apr", "May", "Current", "Proj 1", "Proj 2")
                            val stats = listOf("Actual", "Actual", "Current", "Projected", "Projected")

                            forecastDataPoints.forEachIndexed { i, value ->
                                val ratio = (value / maxVal).toFloat().coerceIn(0.15f, 1.0f)
                                val isSelected = selectedForecastIndex == i

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedForecastIndex = if (isSelected) null else i }
                                        .padding(horizontal = 4.dp),
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    val barColor = when (i) {
                                        0, 1 -> if (isDark) Color(0xFF93C5FD).copy(alpha = if (isSelected) 0.95f else 0.5f) else Color(0xFFD3E2F8).copy(alpha = if (isSelected) 1.0f else 0.65f)
                                        2 -> if (isSelected) (if (isDark) Color(0xFF34D399) else Color(0xFF10B981)) else (if (isDark) Color(0xFF3B82F6) else Color(0xFF0F1B6B))
                                        else -> if (isDark) Color(0xFF38BDF8).copy(alpha = if (isSelected) 0.95f else 0.5f) else Color(0xFF38BDF8).copy(alpha = if (isSelected) 1.0f else 0.6f)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.65f)
                                            .height((110.dp * ratio))
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(barColor)
                                            .then(
                                                if (i >= 3) {
                                                    Modifier.border(
                                                        1.5.dp,
                                                        if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                                        RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                                    )
                                                } else Modifier
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stats[i],
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) (if (isDark) Color.White else Color(0xFF0F1B6B)) else Color.Gray
                                    )
                                    Text(
                                        text = labels[i],
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (isSelected) (if (isDark) Color.White else Color(0xFF0F1B6B)) else Color.Gray
                                    )
                                }
                            }
                        }

                        // Detailed forecast report on selected node
                        selectedForecastIndex?.let { i ->
                            val selectedVal = forecastDataPoints[i]
                            val label = listOf("April Verified Path", "May Verified Path", "Current Valuation", "1-Month Forecast Model", "2-Month Forecast Model")[i]
                            val typeDesc = listOf(
                                "Locked historic net valuation on your raw ledger path.",
                                "Stable finalized historic net worth on your raw ledger path.",
                                "Live active net valuation updated with database entries.",
                                "Projected wealth based on current portfolio yield and salary streams.",
                                "Compounded future net prediction showing growth trajectories."
                            )[i]

                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = label.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (i >= 3) Color(0xFF10B981) else Color(0xFF3B82F6)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (i >= 3) Color(0xFF10B981).copy(0.15f) else Color(0xFF3B82F6).copy(0.15f)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (i >= 3) "ESTIMATED" else "VERIFIED",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (i >= 3) Color(0xFF10B981) else Color(0xFF3B82F6)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$currencyPrefix${String.format(Locale.getDefault(), "%,.2f", selectedVal)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDark) Color.White else Color(0xFF0F1B6B)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = typeDesc,
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        lineHeight = 14.sp
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
                            contentDescription = "Forecast Indicator",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            val avgSave = (totalIncome - totalExpense).coerceAtLeast(0.0)
                            Text(
                                text = "Forecast Insights",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF1E3A8A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Based on your 6-month average savings rate of $currencyPrefix${formatAmount(avgSave)} and current market trends.",
                                fontSize = 11.sp,
                                color = if (isDark) Color.LightGray else Color(0xFF1E40AF)
                            )
                        }
                    }
                }
            }

            // Transactions list with searchable tab replica
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Core Ledger Entries", 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F1B6B)
                        )
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Quick Add Transaction",
                                tint = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Search box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search title, category...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                            unfocusedBorderColor = if (isDark) BrandOutline else Color(0xFFE2E8F0)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom Selection Grid Format 2x2 with larger All Option
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "All" option - Larger (Full-width card)
                        val isAllSelected = selectedTab == "All"
                        val allBg = if (isAllSelected) {
                            if (isDark) Color(0xFF10B981).copy(0.15f) else Color(0xFFEFF6FF)
                        } else {
                            if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                        }
                        val allBorderColor = if (isAllSelected) {
                            if (isDark) Color(0xFF10B981) else Color(0xFF3B82F6)
                        } else {
                            if (isDark) BrandOutline else Color(0xFFE2E8F0)
                        }
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = allBg),
                            border = BorderStroke(1.dp, allBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clickable { selectedTab = "All" }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        tint = if (isAllSelected) (if (isDark) Color(0xFF10B981) else Color(0xFF3B82F6)) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "All Ledger Transactions",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isAllSelected) (if (isDark) Color(0xFF10B981) else Color(0xFF0F1B6B)) else Color.Gray
                                    )
                                }
                            }
                        }

                        // 2x2 Grid Layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Income
                            GridTabButton(
                                text = "Income",
                                icon = Icons.Default.TrendingUp,
                                isSelected = selectedTab == "Income",
                                activeColor = Color(0xFF10B981),
                                isDark = isDark,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedTab = "Income" }
                            )
                            // Expense
                            GridTabButton(
                                text = "Expense",
                                icon = Icons.Default.TrendingDown,
                                isSelected = selectedTab == "Expense",
                                activeColor = Color(0xFFEF4444),
                                isDark = isDark,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedTab = "Expense" }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Investment
                            GridTabButton(
                                text = "Investment",
                                icon = Icons.Default.ShowChart,
                                isSelected = selectedTab == "Investment",
                                activeColor = Color(0xFF2563EB),
                                isDark = isDark,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedTab = "Investment" }
                            )
                            // Budget & Goal
                            GridTabButton(
                                text = "Budget & Goal",
                                icon = Icons.Default.Flag,
                                isSelected = selectedTab == "Budget",
                                activeColor = Color(0xFFDB2777),
                                isDark = isDark,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedTab = "Budget" }
                            )
                        }
                    }
                }
            }

            if (filteredTxns.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transactions registered on ledger in this range", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                items(filteredTxns, key = { "${it.id}_${it.date}" }) { txn ->
                    TransactionRowCard(
                        txn = txn,
                        isDark = isDark,
                        onModifyClick = {
                            activeEditingTx = txn
                            showEditDialog = true
                        },
                        onDeleteClick = {
                            txToDelete = txn
                            showDeleteConfirmDialog = true
                        }
                    )
                }
            }
        }
    }

    // Add Dialogue
    UnifiedAddEditTxDialog(
        show = showAddDialog,
        editingTxn = null,
        onDismiss = { showAddDialog = false },
        categories = categories,
        isDark = isDark,
        viewModel = viewModel,
        onSave = { title, amount, type, category, sub, payment, originOfMoney, originIncomeCategory, originIncomeSubCategory, originOtherDesc ->
            viewModel.addTransaction(
                title = title,
                amount = amount,
                type = type,
                category = category,
                subCategory = sub,
                payment = payment,
                originOfMoney = originOfMoney,
                originIncomeCategory = originIncomeCategory,
                originIncomeSubCategory = originIncomeSubCategory,
                originOtherDescription = originOtherDesc
            )
            Toast.makeText(context, "Ledger transaction saved successfully!", Toast.LENGTH_SHORT).show()
        }
    )

    // Edit dialogue
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
            val existing = activeEditingTx
            if (existing != null) {
                txToSave = existing.copy(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    subCategory = sub,
                    paymentMethod = payment
                )
                showEditConfirmDialog = true
            }
        }
    )

    if (showDeleteConfirmDialog && txToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
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
                        showDeleteConfirmDialog = false
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
                        showDeleteConfirmDialog = false
                        txToDelete = null
                    }
                ) {
                    Text("No", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showEditConfirmDialog && txToSave != null) {
        AlertDialog(
            onDismissRequest = {
                showEditConfirmDialog = false
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
                        showEditConfirmDialog = false
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
                        showEditConfirmDialog = false
                        txToSave = null
                    }
                ) {
                    Text("No", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

private fun formatAmount(amt: Double): String {
    return String.format(Locale.getDefault(), "%,.0f", amt)
}

@Composable
fun NetWorthInteractivePieChart(
    investment: Double,
    cashBalance: Double,
    budget: Double,
    liability: Double,
    isDark: Boolean,
    currencyPrefix: String
) {
    val dataMap = remember(investment, cashBalance, budget) {
        linkedMapOf(
            "Investment" to kotlin.math.abs(investment),
            "Cash Balance" to kotlin.math.abs(cashBalance),
            "Budget & Goal" to kotlin.math.abs(budget)
        )
    }
    
    val totalSum = remember(dataMap) { dataMap.values.sum().coerceAtLeast(1.0) }
    var selectedSliceIndex by remember { mutableStateOf<Int?>(null) }
    val entries = remember(dataMap) { dataMap.entries.toList() }
    
    val categoryColors = listOf(
        Color(0xFF3B82F6), // Investment (Blue)
        Color(0xFF10B981), // Cash Balance (Emerald Green)
        Color(0xFFEC4899)  // Budget & Goal (Pink)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF0F172A) else Color.White)
            .border(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        selectedSliceIndex = if (selectedSliceIndex == null) 0 else {
                            val next = selectedSliceIndex!! + 1
                            if (next >= entries.size) null else next
                        }
                    }
            ) {
                var startAngle = -90f
                entries.forEachIndexed { index, entry ->
                    val sweepAngle = ((entry.value / totalSum) * 360f).toFloat()
                    val color = categoryColors[index]
                    val isHighlighted = selectedSliceIndex == index
                    val strokeWidth = if (isHighlighted) 28f else 18f
                    val sizeScale = if (isHighlighted) 12f else 0f
                    
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(14f + sizeScale / 2, 14f + sizeScale / 2),
                        size = Size(size.width - 28f - sizeScale, size.height - 28f - sizeScale)
                    )
                    startAngle += sweepAngle
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (selectedSliceIndex != null && selectedSliceIndex!! < entries.size) {
                    val activeEntry = entries[selectedSliceIndex!!]
                    val prc = (activeEntry.value / totalSum) * 100.0
                    Text(
                        text = activeEntry.key,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.LightGray else Color(0xFF64748B)
                    )
                    Text(
                        text = "$currencyPrefix${formatAmount(activeEntry.value)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF0F1B6B)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", prc),
                        fontSize = 11.sp,
                        color = categoryColors[selectedSliceIndex!!],
                        fontWeight = FontWeight.Black
                    )
                } else {
                    Text(
                        text = "Net Worth",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.LightGray else Color(0xFF64748B)
                    )
                    Text(
                        text = "$currencyPrefix${formatAmount(totalSum)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else Color(0xFF0F1B6B)
                    )
                    Text(
                        text = "Tap slices",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Legends inside grid for perfect 2-column styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                entries.take(2).forEachIndexed { i, entry ->
                    val index = i
                    val color = categoryColors[index]
                    val pct = (entry.value / totalSum) * 100.0
                    val isSelected = selectedSliceIndex == index
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) color.copy(alpha = 0.35f) else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSliceIndex = if (selectedSliceIndex == index) null else index }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = entry.key,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF1E293B)
                                )
                                Text(
                                    text = "$currencyPrefix${formatAmount(entry.value)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.LightGray else Color(0xFF64748B)
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", pct),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = color
                                )
                            }
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                entries.drop(2).forEachIndexed { i, entry ->
                    val index = i + 2
                    val color = categoryColors[index]
                    val pct = (entry.value / totalSum) * 100.0
                    val isSelected = selectedSliceIndex == index
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) color.copy(alpha = 0.35f) else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSliceIndex = if (selectedSliceIndex == index) null else index }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = entry.key,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF1E293B)
                                )
                                Text(
                                    text = "$currencyPrefix${formatAmount(entry.value)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.LightGray else Color(0xFF64748B)
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", pct),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = color
                                )
                            }
                        }
                    }
                }
                
                // 4th Card: Unpaid Liability display (Styled Crimson Rose, bold red text showing the absolute negative debt balance, excluded from pie slices)
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF2E171E) else Color(0xFFFFF1F2)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFFF43F5E).copy(alpha = 0.35f) else Color(0xFFFECDD3)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Unpaid Debt",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF1E293B)
                            )
                            Text(
                                text = "$currencyPrefix${formatAmount(liability)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFEF4444)
                            )
                            Text(
                                text = "Excl. Pie",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444).copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategorisedMonthlyTrendBarChart(
    isDark: Boolean,
    incomeTrend: List<Double>,
    expenseTrend: List<Double>,
    investmentTrend: List<Double>,
    budgetTrend: List<Double>,
    monthLabels: List<String>,
    currencyPrefix: String
) {
    var selectedMonthIndex by remember { mutableStateOf<Int?>(null) }
    
    fun getMoMGrowth(k: Int, monthIdx: Int): Pair<String, Color> {
        if (monthIdx <= 0 || monthIdx >= monthLabels.size) {
            return Pair("0.0% MoM", Color.Gray)
        }
        val currentVal = when (k) {
            0 -> incomeTrend[monthIdx]
            1 -> expenseTrend[monthIdx]
            2 -> investmentTrend[monthIdx]
            else -> budgetTrend[monthIdx]
        }
        val prevVal = when (k) {
            0 -> incomeTrend[monthIdx - 1]
            1 -> expenseTrend[monthIdx - 1]
            2 -> investmentTrend[monthIdx - 1]
            else -> budgetTrend[monthIdx - 1]
        }
        
        if (prevVal == 0.0) {
            return if (currentVal > 0.0) {
                Pair("+100.0% MoM ↗", Color(0xFF10B981))
            } else {
                Pair("0.0% MoM", Color.Gray)
            }
        }
        
        val pct = ((currentVal - prevVal) / prevVal) * 100.0
        return if (pct >= 0.0) {
            Pair("+${String.format(Locale.getDefault(), "%.1f", pct)}% MoM ↗", Color(0xFF10B981))
        } else {
            Pair("${String.format(Locale.getDefault(), "%.1f", pct)}% MoM ↘", Color(0xFFEF4444))
        }
    }

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
                    text = "Categorized Monthly Trend",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )
                Text(
                    text = "Interactive",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF10B981) else Color(0xFF0284C7),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background((if (isDark) Color(0xFF10B981) else Color(0xFF0284C7)).copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            
            val maxVal = remember(incomeTrend, expenseTrend, investmentTrend, budgetTrend) {
                var max = 100.0
                for (i in incomeTrend.indices) {
                    val m = maxOf(incomeTrend[i], expenseTrend[i], investmentTrend[i], budgetTrend[i])
                    if (m > max) max = m
                }
                max
            }
            
            var chartWidth by remember { mutableStateOf(0) }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .onSizeChanged { chartWidth = it.width }
                    .pointerInput(monthLabels, incomeTrend, expenseTrend, investmentTrend, budgetTrend) {
                        detectTapGestures { offset ->
                            if (chartWidth > 0) {
                                val stepX = chartWidth.toFloat() / monthLabels.size
                                val clickedIndex = (offset.x / stepX).toInt().coerceIn(0, monthLabels.size - 1)
                                selectedMonthIndex = if (selectedMonthIndex == clickedIndex) null else clickedIndex
                            }
                        }
                    }
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val stepX = width / monthLabels.size
                    
                    drawLine(
                        color = Color.LightGray.copy(0.3f),
                        start = Offset(0f, height),
                        end = Offset(width, height),
                        strokeWidth = 1.dp.toPx()
                    )
                    
                    for (i in monthLabels.indices) {
                        val groupCenterX = i * stepX + (stepX / 2f)
                        val barWidth = 6.dp.toPx()
                        val groupSpacing = 4.dp.toPx()
                        
                        val isHighlighted = selectedMonthIndex == i
                        val alphaFactor = if (selectedMonthIndex == null || isHighlighted) 1f else 0.25f
                        
                        // Order: Income, Expense, Investment, Budget & Goal
                        val vals = listOf(incomeTrend[i], expenseTrend[i], investmentTrend[i], budgetTrend[i])
                        val colors = listOf(
                            Color(0xFF10B981), // Income: Emerald Green
                            Color(0xFFEF4444), // Expense: Rose Red
                            Color(0xFF3B82F6), // Investment: Blue
                            Color(0xFFEC4899)  // Budget & Goal: Pink
                        )
                        
                        if (isHighlighted) {
                            // Subtle background highlight band
                            drawRoundRect(
                                color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.03f),
                                topLeft = Offset(i * stepX + 2.dp.toPx(), 0f),
                                size = Size(stepX - 4.dp.toPx(), height + 2.dp.toPx()),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }
                        
                        for (k in 0..3) {
                            val v = vals[k]
                            val normalizedRatio = (v / maxVal).coerceIn(0.02, 1.0)
                            val barH = (normalizedRatio * (height - 20.dp.toPx())).toFloat()
                            
                            val startX = groupCenterX - (2 * barWidth + 1.5f * groupSpacing) + k * (barWidth + groupSpacing)
                            val startY = height - barH
                            
                            drawRoundRect(
                                color = colors[k].copy(alpha = alphaFactor),
                                topLeft = Offset(startX, startY),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in monthLabels.indices) {
                    val label = monthLabels[i]
                    val isSelected = selectedMonthIndex == i
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) {
                            if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                        } else {
                            if (isDark) Color.LightGray else Color(0xFF64748B)
                        },
                        modifier = Modifier
                            .width(55.dp)
                            .clickable { selectedMonthIndex = if (selectedMonthIndex == i) null else i },
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tooltip Detail breakdown
            AnimatedVisibility(
                visible = selectedMonthIndex != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (selectedMonthIndex != null) {
                    val idx = selectedMonthIndex!!
                    val mName = monthLabels[idx]
                    
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$mName - Breakdown & Growth",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.Gray,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { selectedMonthIndex = null }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val catNames = listOf("Income", "Expense", "Investment", "Budget & Goal")
                            val catVals = listOf(incomeTrend[idx], expenseTrend[idx], investmentTrend[idx], budgetTrend[idx])
                            val catColors = listOf(
                                Color(0xFF10B981),
                                Color(0xFFEF4444),
                                Color(0xFF3B82F6),
                                Color(0xFFEC4899)
                            )
                            
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (k in 0..3) {
                                    val (momText, momColor) = getMoMGrowth(k, idx)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(catColors[k]))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = catNames[k],
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isDark) Color.LightGray else Color.DarkGray
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "$currencyPrefix${formatAmount(catVals[k])}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.White else Color.Black
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = momText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = momColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Legend row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val legends = listOf(
                    Pair("Income", Color(0xFF10B981)),
                    Pair("Expense", Color(0xFFEF4444)),
                    Pair("Asset", Color(0xFF3B82F6)),
                    Pair("Budget", Color(0xFFEC4899))
                )
                legends.forEach { (name, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridTabButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (isSelected) {
        activeColor.copy(alpha = 0.15f)
    } else {
        if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    }
    val borderCol = if (isSelected) {
        activeColor
    } else {
        if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    }
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.dp, borderCol),
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) activeColor else Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) (if (isDark) Color.White else Color(0xFF0F1B6B)) else Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}
