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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DbTransaction
import com.example.data.model.DbLiability
import com.example.data.model.DbCategory
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.ui.AppViewModel
import com.example.ui.components.*
import com.example.ui.context.appGlobalContext
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDashboardScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScreen: (String) -> Unit
) {
    val contextState = appGlobalContext
    val txns = contextState.transactions
    val categories = contextState.categories
    val liabilities = contextState.liabilities
    val budgets = contextState.budgets
    val currencyUnit = contextState.currencyUnit

    val activeRecurringItems = remember(contextState.recurringItems) {
        contextState.recurringItems.filter { !com.example.util.CommitmentProcessor.isPastDurationEnd(it.durationEndDate) }
    }

    val context = LocalContext.current
    val isDark = isDarkThemeActive

    LaunchedEffect(viewModel) {
        viewModel.triggerCommitmentProcessing()
    }

    val unpaidLiabilities = remember(liabilities) { liabilities.filter { !it.isPaid } }

    var showSettleDialog by remember { mutableStateOf(false) }
    var selectedLiabilityToSettle by remember { mutableStateOf<DbLiability?>(null) }
    var settleSourceOfFunds by remember { mutableStateOf("Cash") }
    var settlePaymentType by remember { mutableStateOf("Full") } // "Full" or "Partial"
    var settlePartialAmount by remember { mutableStateOf("") }

    // Selected Interval: binds directly to the high-premium capsules row
    var selectedInterval by remember { mutableStateOf("Monthly") }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    // Dialog trigger states
    var showAddDialog by remember { mutableStateOf(false) }
    var showRecurringItemAddDialog by remember { mutableStateOf(false) }
    var showRecurringItemEditDialog by remember { mutableStateOf(false) }
    var activeEditingCommitment by remember { mutableStateOf<com.example.data.model.DbRecurringItem?>(null) }
    var showCommitmentDetailsDialog by remember { mutableStateOf(false) }
    var selectedCommitmentForDetails by remember { mutableStateOf<com.example.data.model.DbRecurringItem?>(null) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var commitmentToDelete by remember { mutableStateOf<com.example.data.model.DbRecurringItem?>(null) }
    var showEditConfirmationDialog by remember { mutableStateOf(false) }
    var commitmentToUpdate by remember { mutableStateOf<com.example.data.model.DbRecurringItem?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var activeEditingTx by remember { mutableStateOf<DbTransaction?>(null) }

    var txToDelete by remember { mutableStateOf<DbTransaction?>(null) }
    var txToSave by remember { mutableStateOf<DbTransaction?>(null) }
    var showTxDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showTxEditConfirmDialog by remember { mutableStateOf(false) }

    var showGlobalConfirmDialog by remember { mutableStateOf(false) }
    var globalConfirmTitle by remember { mutableStateOf("Are you sure?") }
    var globalConfirmMessage by remember { mutableStateOf("") }
    var globalOnConfirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var globalConfirmColor by remember { mutableStateOf(Color(0xFFEF4444)) }


    val currencyPrefix = when (currencyUnit) {
        "Dollar ($)" -> "$"
        "Euro (€)" -> "€"
        else -> "₹"
    }

    // Filter transaction outflows based on selected interval capsule
    val now = remember { System.currentTimeMillis() }
    val filteredExpenses = remember(txns, selectedInterval, customStartDate, customEndDate) {
        txns.filter { it.type == "Expense" }
            .filter { txn ->
                val isInvestment = txn.type.equals("Asset", ignoreCase = true) || txn.type.equals("Investment", ignoreCase = true) || txn.category.lowercase().contains("invest") || txn.subCategory.lowercase().contains("invest") || txn.title.lowercase().contains("invest")
                val isBudgetGoal = txn.type.equals("Budget", ignoreCase = true) || txn.type.equals("Goal", ignoreCase = true) || txn.category.lowercase().contains("budget") || txn.category.lowercase().contains("goal") || 
                                   txn.subCategory.lowercase().contains("budget") || txn.subCategory.lowercase().contains("goal") || 
                                   txn.title.lowercase().contains("budget") || txn.title.lowercase().contains("goal")
                !isInvestment && !isBudgetGoal
            }
            .filter { txn ->
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

    val totalExpenses = remember(filteredExpenses) { filteredExpenses.sumOf { it.amount } }

    // Group actual expenses by category for top-grade donut calculations
    val expenseAllocations = remember(filteredExpenses) {
        val mapping = mutableMapOf<String, Double>()
        filteredExpenses.forEach { txn ->
            mapping[txn.category] = (mapping[txn.category] ?: 0.0) + txn.amount
        }
        mapping
    }

    // Dynamic Budget Calculation
    val totalBudgetLimit = remember(budgets) {
        if (budgets.isNotEmpty()) budgets.sumOf { it.limitAmount } else 0.0
    }
    val spentFromBudget = remember(totalExpenses, filteredExpenses) {
        totalExpenses
    }
    val remainingBudget = remember(totalBudgetLimit, spentFromBudget) {
        (totalBudgetLimit - spentFromBudget).coerceAtLeast(0.0)
    }
    val budgetHealthPct = remember(totalBudgetLimit, spentFromBudget) {
        if (totalBudgetLimit > 0) (spentFromBudget / totalBudgetLimit).toFloat().coerceIn(0f, 1f) else 0.0f
    }

    Scaffold(
        containerColor = if (isDark) BrandBackground else Color(0xFFF4F7FC),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFBE185D),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
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
            // Segmented Interval Capsule Selector Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val intervals = listOf("Monthly", "Daily", "Weekly", "Quarterly")
                    intervals.forEach { interval ->
                        val isSelected = selectedInterval == interval
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF0F1B6B) else Color.Transparent)
                                .clickable { selectedInterval = interval }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = interval,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else (if (isDark) Color.LightGray else Color.DarkGray)
                            )
                        }
                    }
                }
            }

            // Total Monthly Spend - High-gradient premium layout
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFFFECEE))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF0F1B6B), Color(0xFF1E293B)),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL MONTHLY SPEND",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White.copy(alpha = 0.65f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$currencyPrefix${String.format(Locale.getDefault(), "%,.0f", spentFromBudget)}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            // Elegant growth percentage chip (↗ 12.4% vs last month)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFF4ADE80),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "12.4% vs last month",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "BUDGET HEALTH",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "${(budgetHealthPct * 100).toInt()}% used",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (budgetHealthPct > 0.85f) Color(0xFFEF4444) else Color(0xFF4ADE80)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        // Styled Neon Progress Indicator
                        LinearProgressIndicator(
                            progress = { budgetHealthPct },
                            color = if (budgetHealthPct > 0.85f) Color(0xFFEF4444) else Color(0xFF4ADE80),
                            trackColor = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "$currencyPrefix${String.format(Locale.getDefault(), "%,.0f", remainingBudget)} remaining of $currencyPrefix${String.format(Locale.getDefault(), "%,.0f", totalBudgetLimit)} budget",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Interactive Donut Allocation Graph
            item {
                DistributionDonutChart(dataMap = expenseAllocations, isDark = isDark)
            }

            // 6-Month Trend Visual Chart
            item {
                SixMonthTrendCard(isDark = isDark)
            }

            // Weekly Payment Calendar Carousel
            item {
                PaymentCalendarSlider(
                    isDark = isDark,
                    recurringItems = activeRecurringItems,
                    viewModel = viewModel,
                    onManageItem = { item ->
                        selectedCommitmentForDetails = item
                        showCommitmentDetailsDialog = true
                    }
                )
            }

            // High Priority Commitments Header & Add button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ High Priority Commitments",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F1B6B)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                // ADD NEW COMMITMENTS CTA (pre-configures "Udhar" tracking)
                Button(
                    onClick = {
                        showRecurringItemAddDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_bill_emi_sip_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1B6B)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Text(
                            text = "ADD NEW BILLS, EMI, SIP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Commitment Monitor and Processor Card
            item {
                val userRecs = activeRecurringItems
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Commitment Monitor & Processor",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                                )
                                Text(
                                    text = "Real-time tracker for bills, EMIs, SIPs, & Udhar",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.triggerCommitmentProcessing() },
                                modifier = Modifier.testTag("run_commitment_processor_icon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync & Process",
                                    tint = Color(0xFF10B981)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (userRecs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No pending commitments configured. Add one above!",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                userRecs.forEach { item ->
                                    val daysLeft = com.example.util.CommitmentProcessor.calculateDaysLeft(item.dueDate)
                                    val isPaid = item.isPaid
                                    val isRed = !isPaid && daysLeft <= 7
                                    
                                    val rowBackground = when {
                                        isPaid -> if (isDark) Color(0xFF064E3B).copy(alpha = 0.25f) else Color(0xFFD1FAE5)
                                        isRed -> if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.25f) else Color(0xFFFEF2F2)
                                        else -> if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                    }
                                    
                                    val borderStrokeColor = when {
                                        isPaid -> if (isDark) Color(0xFF059669).copy(alpha = 0.5f) else Color(0xFF10B981)
                                        isRed -> if (isDark) Color(0xFFB91C1C).copy(alpha = 0.5f) else Color(0xFFEF4444)
                                        else -> if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, borderStrokeColor, RoundedCornerShape(12.dp))
                                            .background(rowBackground)
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        when (item.type.uppercase(Locale.getDefault())) {
                                                            "BILL" -> Color(0xFF3B82F6)
                                                            "EMI" -> Color(0xFFF59E0B)
                                                            "SIP" -> Color(0xFF10B981)
                                                            "UDHAR" -> Color(0xFFEF4444)
                                                            else -> Color(0xFF8B5CF6)
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = item.type.take(1).uppercase(Locale.getDefault()),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = item.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = if (isDark) Color.White else Color.Black
                                                )
                                                val daysText = if (isPaid) {
                                                    "Paid"
                                                } else if (daysLeft < 0) {
                                                    "Overdue by ${-daysLeft} days"
                                                } else if (daysLeft == 0) {
                                                    "Due Today"
                                                } else {
                                                    "Due in $daysLeft days"
                                                }
                                                Text(
                                                    text = "Due: ${item.dueDate} • $daysText • Type: ${item.type}",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                        
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "$currencyPrefix${String.format(Locale.getDefault(), "%,.2f", item.amount)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isDark) Color.White else Color.Black
                                            )
                                            
                                            val badgeBg = when {
                                                isPaid -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                isRed -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                                else -> Color.Gray.copy(alpha = 0.2f)
                                            }
                                            val badgeText = when {
                                                isPaid -> "PAID"
                                                isRed -> "UNPAID COMMITMENT"
                                                else -> "SCHEDULED PENDING"
                                            }
                                            val badgeColor = when {
                                                isPaid -> Color(0xFF10B981)
                                                isRed -> Color(0xFFEF4444)
                                                else -> Color.Gray
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(badgeBg)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = badgeText,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = badgeColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        val dueCount = userRecs.count { com.example.util.CommitmentProcessor.isDue(it.dueDate) }
                        Button(
                            onClick = { viewModel.triggerCommitmentProcessing() },
                            modifier = Modifier.fillMaxWidth().testTag("trigger_commitment_processor_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (dueCount > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Text(
                                text = if (dueCount > 0) "TRANSITION $dueCount DUE COMMITMENT(S) NOW" else "SCAN / SYNC PENDING COMMITMENTS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Outstanding liabilities / Udhar list
            item {
                Text(
                    text = "Upcoming Bills & Outstanding Liabilities (Udhar)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (unpaidLiabilities.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No outstanding liabilities or bills.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        unpaidLiabilities.forEach { liability ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = liability.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isDark) Color.White else Color.Black
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Category: ${liability.category} > ${liability.subCategory}",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFFFECEE))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Remaining: ₹${liability.remainingDue}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBE185D))
                                            }
                                            if (liability.amountPaid > 0.0) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFDCFCE7))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Paid: ₹${liability.amountPaid}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                                }
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            selectedLiabilityToSettle = liability
                                            settlePaymentType = "Full"
                                            settleSourceOfFunds = "Cash"
                                            settlePartialAmount = ""
                                            showSettleDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBE185D)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Settle Bill", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Monthly SIPs Visual Tracker
            item {
                MonthlySIPsCard(
                    isDark = isDark,
                    recurringItems = activeRecurringItems,
                    viewModel = viewModel,
                    onManageItem = { item ->
                        selectedCommitmentForDetails = item
                        showCommitmentDetailsDialog = true
                    },
                    onPayClick = { item ->
                        globalConfirmTitle = "Are you sure?"
                        globalConfirmMessage = "Do you want to mark '${item.title}' as PAID for this period? A corresponding expense transaction will be generated dynamically."
                        globalConfirmColor = Color(0xFF10B981) // safe green
                        globalOnConfirmAction = {
                            viewModel.processSingleCommitment(item)
                        }
                        showGlobalConfirmDialog = true
                    }
                )
            }

            // Subscriptions Panel
            item {
                SubscriptionsCard(
                    isDark = isDark,
                    recurringItems = activeRecurringItems,
                    viewModel = viewModel,
                    onManageItem = { item ->
                        selectedCommitmentForDetails = item
                        showCommitmentDetailsDialog = true
                    },
                    onPayClick = { item ->
                        globalConfirmTitle = "Are you sure?"
                        globalConfirmMessage = "Do you want to mark subscription '${item.title}' as PAID for this period? A corresponding expense transaction will be generated dynamically."
                        globalConfirmColor = Color(0xFF10B981) // safe green
                        globalOnConfirmAction = {
                            viewModel.processSingleCommitment(item)
                        }
                        showGlobalConfirmDialog = true
                    }
                )
            }

            // Utilities Panel (Dynamic & Fresh-Account Clean)
            item {
                UtilityBillsCard(
                    isDark = isDark,
                    recurringItems = activeRecurringItems,
                    viewModel = viewModel,
                    onManageItem = { item ->
                        selectedCommitmentForDetails = item
                        showCommitmentDetailsDialog = true
                    },
                    onPayClick = { item ->
                        globalConfirmTitle = "Are you sure?"
                        globalConfirmMessage = "Do you want to mark utility bill '${item.title}' as PAID for this period? A corresponding expense transaction will be generated dynamically."
                        globalConfirmColor = Color(0xFF10B981) // safe green
                        globalOnConfirmAction = {
                            viewModel.processSingleCommitment(item)
                        }
                        showGlobalConfirmDialog = true
                    }
                )
            }


            // Outflow ledger entries list
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Registered Expense Entries",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F1B6B)
                    )
                    TextButton(
                        onClick = {
                            val csvContent = com.example.util.CsvExporter.formatTransactionsToCsv(filteredExpenses)
                            com.example.util.CsvExporter.exportToCsvFile(context, "expense_ledger_export.csv", csvContent)
                        },
                        modifier = Modifier.testTag("export_expense_csv_button")
                    ) {
                        Text("Export CSV", fontSize = 12.sp)
                    }
                }
            }

            if (filteredExpenses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No expense records found on ledger path", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                items(filteredExpenses, key = { "${it.id}_${it.date}" }) { txn ->
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

    // Universal Add transaction dialog filtered natively to show only the "Expense" transaction group
    UnifiedAddEditTxDialog(
        show = showAddDialog,
        editingTxn = null,
        onDismiss = { showAddDialog = false },
        categories = categories,
        isDark = isDark,
        txTypeLocked = "Expense",
        viewModel = viewModel,
        onSave = { title, amount, type, category, sub, payment, originOfMoney, originIncomeCategory, originIncomeSubCategory, originOtherDesc ->
            viewModel.addTransaction(
                title = title,
                amount = amount,
                type = "Expense", // Locked natively here to preserve category consistency
                category = category,
                subCategory = sub,
                payment = payment,
                originOfMoney = originOfMoney,
                originIncomeCategory = originIncomeCategory,
                originIncomeSubCategory = originIncomeSubCategory,
                originOtherDescription = originOtherDesc,
                fundsSource = ""
            )
            Toast.makeText(context, "Expense transaction saved successfully!", Toast.LENGTH_SHORT).show()
            showAddDialog = false
        },
        onSaveWithFundsSource = { title, amount, type, category, sub, payment, originOfMoney, originIncomeCategory, originIncomeSubCategory, originOtherDesc, fundsSource ->
            viewModel.addTransaction(
                title = title,
                amount = amount,
                type = "Expense", // Locked natively here to preserve category consistency
                category = category,
                subCategory = sub,
                payment = payment,
                originOfMoney = originOfMoney,
                originIncomeCategory = originIncomeCategory,
                originIncomeSubCategory = originIncomeSubCategory,
                originOtherDescription = originOtherDesc,
                fundsSource = fundsSource
            )
            Toast.makeText(context, "Expense transaction saved successfully!", Toast.LENGTH_SHORT).show()
            showAddDialog = false
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
        txTypeLocked = "Expense",
        viewModel = viewModel,
        onSave = { title, amount, type, category, sub, payment, _, _, _, _ ->
            val existing = activeEditingTx
            if (existing != null) {
                txToSave = existing.copy(
                    title = title,
                    amount = amount,
                    type = "Expense",
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
                    text = "Do you want to delete the expense entry '${txToDelete!!.title}'? This action cannot be undone.",
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
                    text = "Do you want to save changes to the expense entry '${txToSave!!.title}'?",
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

    AddRecurringItemDialog(
        show = showRecurringItemAddDialog,
        onDismiss = { showRecurringItemAddDialog = false },
        categories = categories,
        isDark = isDark,
        onSave = { title, amount, dueDate, category, subCategory, paymentMode, type, fundsSource, recurrence, repetition, durationEndDate ->
            globalConfirmTitle = "Are you sure?"
            globalConfirmMessage = "Do you want to confirm and register the new commitment: '$title'?"
            globalConfirmColor = Color(0xFF10B981) // Green for successful safe creation
            globalOnConfirmAction = {
                viewModel.addRecurringItem(
                    title = title,
                    amount = amount,
                    dueDate = dueDate,
                    category = category,
                    subCategory = subCategory,
                    paymentMode = paymentMode,
                    type = type,
                    fundsSource = fundsSource,
                    recurrence = recurrence,
                    repetition = repetition,
                    durationEndDate = durationEndDate
                )
                showRecurringItemAddDialog = false
            }
            showGlobalConfirmDialog = true
        }
    )


    AddRecurringItemDialog(
        show = showRecurringItemEditDialog,
        onDismiss = {
            showRecurringItemEditDialog = false
            activeEditingCommitment = null
        },
        categories = categories,
        isDark = isDark,
        editItem = activeEditingCommitment,
        onSave = { _, _, _, _, _, _, _, _, _, _, _ -> },
        onUpdate = { updatedItem ->
            globalConfirmTitle = "Are you sure?"
            globalConfirmMessage = "Do you want to save changes to the commitment '${updatedItem.title}'?"
            globalConfirmColor = Color(0xFF3B82F6)
            globalOnConfirmAction = {
                viewModel.updateRecurringItem(updatedItem)
                showRecurringItemEditDialog = false
                activeEditingCommitment = null
                Toast.makeText(context, "Commitment updated successfully.", Toast.LENGTH_SHORT).show()
            }
            showGlobalConfirmDialog = true
        }
    )


    if (showCommitmentDetailsDialog && selectedCommitmentForDetails != null) {
        CommitmentDetailsDialog(
            show = showCommitmentDetailsDialog,
            onDismiss = {
                showCommitmentDetailsDialog = false
                selectedCommitmentForDetails = null
            },
            item = selectedCommitmentForDetails!!,
            isDark = isDark,
            onEditClick = {
                activeEditingCommitment = selectedCommitmentForDetails
                showCommitmentDetailsDialog = false
                selectedCommitmentForDetails = null
                showRecurringItemEditDialog = true
            },
            onDeleteClick = {
                val target = selectedCommitmentForDetails
                if (target != null) {
                    showCommitmentDetailsDialog = false
                    selectedCommitmentForDetails = null
                    globalConfirmTitle = "Are you sure?"
                    globalConfirmMessage = "Do you want to permanently delete the commitment '${target.title}'?"
                    globalConfirmColor = Color(0xFFEF4444)
                    globalOnConfirmAction = {
                        viewModel.deleteRecurringItem(target)
                        Toast.makeText(context, "Commitment deleted successfully.", Toast.LENGTH_SHORT).show()
                    }
                    showGlobalConfirmDialog = true
                }
            }
        )
    }

    GlobalConfirmationModal(
        show = showGlobalConfirmDialog,
        title = globalConfirmTitle,
        message = globalConfirmMessage,
        confirmButtonColor = globalConfirmColor,
        isDark = isDark,
        onConfirm = {
            globalOnConfirmAction?.invoke()
            showGlobalConfirmDialog = false
            globalOnConfirmAction = null
        },
        onCancel = {
            showGlobalConfirmDialog = false
            globalOnConfirmAction = null
        }
    )


    // Settle Udhar Bill Dialog Overlay
    if (showSettleDialog && selectedLiabilityToSettle != null) {
        val liability = selectedLiabilityToSettle!!
        AlertDialog(
            onDismissRequest = {
                showSettleDialog = false
                selectedLiabilityToSettle = null
            },
            title = {
                Text("Repay & Settle Outstanding Bill", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isDark) Color.White else Color(0xFF0F1B6B))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Bill Name: ${liability.title}", fontSize = 11.sp, color = Color.Gray)
                    Text("Total Original Bill: ₹${liability.totalAmount}", fontSize = 11.sp, color = Color.Gray)
                    Text("Outstanding Balance Due: ₹${liability.remainingDue}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFFEF4444))

                    var sourceExpanded by remember { mutableStateOf(false) }
                    Text("SELECT REPAYMENT SOURCE FUNDS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White.copy(0.6f) else Color(0xFF64748B))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                .clickable { sourceExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(settleSourceOfFunds, fontSize = 13.sp, color = if (isDark) Color.White else Color.Black)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = sourceExpanded,
                            onDismissRequest = { sourceExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            listOf("Cash", "UPI", "Debit Card", "Netbanking").forEach { src ->
                                DropdownMenuItem(
                                    text = { Text(src, fontSize = 13.sp) },
                                    onClick = {
                                        settleSourceOfFunds = src
                                        sourceExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("SETTLEMENT REPAYMENT TYPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White.copy(0.6f) else Color(0xFF64748B))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val fullSelected = settlePaymentType == "Full"
                        Button(
                            onClick = { settlePaymentType = "Full" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (fullSelected) Color(0xFF10B981) else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                            )
                        ) {
                            Text("Full Repayment", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (fullSelected) Color.White else (if (isDark) Color.LightGray else Color.DarkGray))
                        }

                        val partialSelected = settlePaymentType == "Partial"
                        Button(
                            onClick = { settlePaymentType = "Partial" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (partialSelected) Color(0xFF3B82F6) else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                            )
                        ) {
                            Text("Partial Repay", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (partialSelected) Color.White else (if (isDark) Color.LightGray else Color.DarkGray))
                        }
                    }

                    if (settlePaymentType == "Partial") {
                        OutlinedTextField(
                            value = settlePartialAmount,
                            onValueChange = { settlePartialAmount = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Partial payment amount (₹)") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = if (settlePaymentType == "Full") {
                            liability.remainingDue
                        } else {
                            settlePartialAmount.toDoubleOrNull() ?: 0.0
                        }

                        if (amt <= 0.0) {
                            Toast.makeText(context, "Settle payment must be greater than zero!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (amt > liability.remainingDue) {
                            Toast.makeText(context, "Repayment amount cannot exceed total balance due: ₹${liability.remainingDue}!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        viewModel.settleLiability(
                            liability = liability,
                            sourceOfFunds = settleSourceOfFunds,
                            paymentType = settlePaymentType,
                            partialAmount = amt
                        ) { success, msg ->
                            if (success) {
                                showSettleDialog = false
                                selectedLiabilityToSettle = null
                                settlePartialAmount = ""
                            } else {
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Pay & Clear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSettleDialog = false
                        selectedLiabilityToSettle = null
                    }
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

// ==========================================
// CUSTOM VISUAL MATERIAL COMPONENTS (STITCH)
// ==========================================

@Composable
fun DistributionDonutChart(
    dataMap: Map<String, Double>,
    isDark: Boolean
) {
    if (dataMap.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No distribution data available yet", color = Color.Gray, fontSize = 12.sp)
        }
        return
    }

    val total = dataMap.values.sum()
    if (total <= 0) return

    val topCategory = dataMap.maxByOrNull { it.value }?.key ?: "N/A"

    val colors = listOf(
        Color(0xFF0F1B6B), // Strong Premium Indigo
        Color(0xFF22C55E), // Solid Emerald Green
        Color(0xFFEF4444), // Accent Scarlet
        Color(0xFFFBBF24), // Saturated Yellow
        Color(0xFF8B5CF6), // Royal Violet
        Color(0xFF14B8A6)  // Teal
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Distribution",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isDark) Color.White else Color(0xFF0F1B6B)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Interactive Donut Ring Canvas
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        dataMap.entries.forEachIndexed { index, entry ->
                            val pct = (entry.value / total).toFloat()
                            val sweepAngle = pct * 360f
                            val color = colors[index % colors.size]
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 18.dp.toPx())
                            )
                            startAngle += sweepAngle
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Top", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text(
                            text = topCategory,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F1B6B),
                            maxLines = 1
                        )
                    }
                }

                // Detailed legends section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    dataMap.entries.take(5).forEachIndexed { index, entry ->
                        val color = colors[index % colors.size]
                        val pct = (entry.value / total * 100).toInt()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Text(
                                text = "${entry.key} ($pct%)",
                                fontSize = 11.sp,
                                color = if (isDark) Color.LightGray else Color.Black,
                                modifier = Modifier.width(90.dp)
                            )
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%,.0f", entry.value)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F1B6B),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SixMonthTrendCard(isDark: Boolean) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "6-Month Trend",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val months = listOf("JAN", "FEB", "MAR", "APR", "MAY")
                val values = listOf(45000f, 58000f, 52000f, 84250f, 78000f)
                val maxVal = values.maxOrNull() ?: 100000f

                months.forEachIndexed { idx, m ->
                    val hRatio = values[idx] / maxVal
                    val isApr = (m == "APR") // APR represents active month as highlighted in the stitch reference
                    val barColor = if (isApr) Color(0xFF0F1B6B) else Color(0xFFE2ECFD)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "₹${(values[idx]/1000).toInt()}k",
                            fontSize = 8.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height((80 * hRatio).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = m,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isApr) Color(0xFF0F1B6B) else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentCalendarSlider(
    isDark: Boolean,
    recurringItems: List<com.example.data.model.DbRecurringItem>,
    viewModel: AppViewModel,
    onManageItem: (com.example.data.model.DbRecurringItem) -> Unit
) {
    val currentCal = remember { Calendar.getInstance() }
    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.US) }
    val monthYearStr = remember(currentCal) { monthYearFormat.format(currentCal.time).uppercase() }

    var selectedDate by remember { mutableStateOf<Calendar>(Calendar.getInstance()) }

    // monday of current week
    val weekDays = remember(currentCal) {
        val weekCal = Calendar.getInstance()
        weekCal.firstDayOfWeek = Calendar.MONDAY
        weekCal.set(Calendar.HOUR_OF_DAY, 0)
        weekCal.set(Calendar.MINUTE, 0)
        weekCal.set(Calendar.SECOND, 0)
        weekCal.set(Calendar.MILLISECOND, 0)
        val dayOfWeek = weekCal.get(Calendar.DAY_OF_WEEK)
        val offset = if (dayOfWeek == Calendar.SUNDAY) -6 else (Calendar.MONDAY - dayOfWeek)
        weekCal.add(Calendar.DAY_OF_MONTH, offset)

        val list = mutableListOf<Calendar>()
        for (i in 0 until 7) {
            val temp = Calendar.getInstance()
            temp.timeInMillis = weekCal.timeInMillis
            temp.add(Calendar.DAY_OF_MONTH, i)
            list.add(temp)
        }
        list
    }

    val daysWithCommitments = remember(recurringItems, weekDays) {
        weekDays.map { day ->
            recurringItems.filter { item ->
                val itemCal = com.example.util.CommitmentProcessor.parseDueDate(item.dueDate)
                itemCal != null &&
                        itemCal.get(Calendar.DAY_OF_MONTH) == day.get(Calendar.DAY_OF_MONTH) &&
                        itemCal.get(Calendar.MONTH) == day.get(Calendar.MONTH) &&
                        itemCal.get(Calendar.YEAR) == day.get(Calendar.YEAR)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment Calendar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )
                Text(
                    text = monthYearStr,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.LightGray else Color(0xFF0F1B6B)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                
                days.forEachIndexed { idx, d ->
                    val dayCal = weekDays[idx]
                    val isSelected = selectedDate.get(Calendar.DAY_OF_MONTH) == dayCal.get(Calendar.DAY_OF_MONTH) &&
                            selectedDate.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH) &&
                            selectedDate.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR)

                    val itemsDue = daysWithCommitments[idx]
                    val hasDue = itemsDue.isNotEmpty()

                    val activeBg = if (isSelected) {
                        if (isDark) Color(0xFF10B981) else Color(0xFF0F1B6B)
                    } else {
                        if (isDark) Color(0xFF0F172A) else Color(0xFFF1F6FE)
                    }

                    val textCol = if (isSelected) {
                        Color.White
                    } else {
                        if (isDark) Color.LightGray else Color.DarkGray
                    }

                    val dateCol = if (isSelected) {
                        Color.White
                    } else {
                        if (isDark) Color.White else Color(0xFF0F1B6B)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(activeBg)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) (if (isDark) Color(0xFF10B981) else Color(0xFF0F1B6B)) else (if (isDark) Color(0xFF334155) else Color(0xFFE2ECFD)),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedDate = dayCal }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (hasDue) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFEF4444))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("DUE", fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(text = d, fontSize = 8.sp, color = textCol.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.US, "%02d", dayCal.get(Calendar.DAY_OF_MONTH)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = dateCol
                            )
                            if (hasDue) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4ADE80))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val selectedDateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.US).format(selectedDate.time)
            Text(
                text = "Schedule of $selectedDateStr:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.LightGray else Color(0xFF475569)
            )

            val itemsForSelectedDate = remember(selectedDate, recurringItems) {
                recurringItems.filter { item ->
                    val itemCal = com.example.util.CommitmentProcessor.parseDueDate(item.dueDate)
                    itemCal != null &&
                            itemCal.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH) &&
                            itemCal.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                            itemCal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (itemsForSelectedDate.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No commitments due on this date. Click another day above to scan!",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsForSelectedDate.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color.Black
                                )
                                Text(
                                    text = "Type: ${item.type} • Category: ${item.category}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (item.fundsSource.contains("$")) "$" else "₹"}${String.format(Locale.getDefault(), "%,.2f", item.amount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.processSingleCommitment(item) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text("Pay Now", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    IconButton(
                                        onClick = { onManageItem(item) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Details",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
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

@Composable
fun MonthlySIPsCard(
    isDark: Boolean,
    recurringItems: List<com.example.data.model.DbRecurringItem>,
    viewModel: AppViewModel,
    onManageItem: (com.example.data.model.DbRecurringItem) -> Unit,
    onPayClick: (com.example.data.model.DbRecurringItem) -> Unit
) {
    val context = LocalContext.current
    val sips = remember(recurringItems) {
        recurringItems.filter { it.type.equals("SIP", ignoreCase = true) }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly SIPs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White else Color(0xFF0F1B6B)
                    )
                    Text(
                        text = if (sips.isEmpty()) "0 SIP SCHEDULED" else "${sips.size} SIP SCHEDULED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (sips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active monthly SIPs configured.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sips.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF111827) else Color(0xFFF9FAFB))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDark) Color.White else Color.Black)
                                Text("Due: ${item.dueDate} • Tap Pay to sync", fontSize = 10.sp, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "${if (item.fundsSource.contains("$")) "$" else "₹"}${String.format(Locale.getDefault(), "%,.2f", item.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isDark) Color.White else Color.Black
                                )
                                Button(
                                    onClick = { onPayClick(item) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Pay", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                IconButton(
                                    onClick = { onManageItem(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Details",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionsCard(
    isDark: Boolean,
    recurringItems: List<com.example.data.model.DbRecurringItem>,
    viewModel: AppViewModel,
    onManageItem: (com.example.data.model.DbRecurringItem) -> Unit,
    onPayClick: (com.example.data.model.DbRecurringItem) -> Unit
) {
    val context = LocalContext.current
    val subItems = remember(recurringItems) {
        recurringItems.filter { item ->
            val isUtility = item.category.lowercase().contains("util") ||
                    item.subCategory.lowercase().contains("util") ||
                    item.subCategory.lowercase().contains("electricity") ||
                    item.subCategory.lowercase().contains("internet") ||
                    item.subCategory.lowercase().contains("water") ||
                    item.subCategory.lowercase().contains("gas") ||
                    item.title.lowercase().contains("electricity") ||
                    item.title.lowercase().contains("bescom") ||
                    item.title.lowercase().contains("fiber") ||
                    item.title.lowercase().contains("airtel") ||
                    item.title.lowercase().contains("gas") ||
                    item.title.lowercase().contains("water") ||
                    item.title.lowercase().contains("wifi") ||
                    item.title.lowercase().contains("broadband") ||
                    item.title.lowercase().contains("internet")

            (item.type.equals("Bill", ignoreCase = true) || item.category.equals("Bills & Subscriptions", ignoreCase = true)) && !isUtility
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subscriptions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )
                Text(
                    text = if (subItems.isEmpty()) "0 BILLS ACTIVE" else "${subItems.size} BILLS ACTIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (subItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active subscriptions configured.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    subItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF111827) else Color(0xFFF9FAFB))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(item.title.take(1).uppercase(), fontWeight = FontWeight.Black, color = if (isDark) Color.White else Color(0xFF0F1B6B), fontSize = 14.sp)
                                }
                                Column {
                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDark) Color.White else Color.Black)
                                    Text("Due: ${item.dueDate}", fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "${if (item.fundsSource.contains("$")) "$" else "₹"}${String.format(Locale.getDefault(), "%,.2f", item.amount)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                                )
                                Button(
                                    onClick = { onPayClick(item) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Pay", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                IconButton(
                                    onClick = { onManageItem(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Details",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UtilityBillsCard(
    isDark: Boolean,
    recurringItems: List<com.example.data.model.DbRecurringItem>,
    viewModel: AppViewModel,
    onManageItem: (com.example.data.model.DbRecurringItem) -> Unit,
    onPayClick: (com.example.data.model.DbRecurringItem) -> Unit
) {
    val context = LocalContext.current
    val utilityItems = remember(recurringItems) {
        recurringItems.filter { item ->
            item.category.lowercase().contains("util") ||
            item.subCategory.lowercase().contains("util") ||
            item.subCategory.lowercase().contains("electricity") ||
            item.subCategory.lowercase().contains("internet") ||
            item.subCategory.lowercase().contains("water") ||
            item.subCategory.lowercase().contains("gas") ||
            item.title.lowercase().contains("electricity") ||
            item.title.lowercase().contains("bescom") ||
            item.title.lowercase().contains("fiber") ||
            item.title.lowercase().contains("airtel") ||
            item.title.lowercase().contains("gas") ||
            item.title.lowercase().contains("water") ||
            item.title.lowercase().contains("wifi") ||
            item.title.lowercase().contains("broadband") ||
            item.title.lowercase().contains("internet")
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Utility Bills",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )
                Text(
                    text = if (utilityItems.isEmpty()) "0 UTILITIES ACTIVE" else "${utilityItems.size} UTILITIES ACTIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (utilityItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active utility bills configured.\nCreate a commitment with Category or Subcategory containing 'Utilities', 'Electricity', 'Internet', etc. to track here.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    utilityItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF111827) else Color(0xFFF9FAFB))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDark) Color.White else Color.Black)
                                Text("Due: ${item.dueDate} • Tap Pay to sync", fontSize = 10.sp, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%,.2f", item.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isDark) Color.White else Color.Black
                                )
                                Button(
                                    onClick = { onPayClick(item) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Pay", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                IconButton(
                                    onClick = { onManageItem(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Details",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AddRecurringItemDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    categories: List<DbCategory>,
    isDark: Boolean,
    editItem: com.example.data.model.DbRecurringItem? = null,
    onSave: (
        title: String,
        amount: Double,
        dueDate: String,
        category: String,
        subCategory: String,
        paymentMode: String,
        type: String,
        fundsSource: String,
        recurrence: String,
        repetition: String,
        durationEndDate: String
    ) -> Unit,
    onUpdate: ((item: com.example.data.model.DbRecurringItem) -> Unit)? = null
) {
    if (!show) return

    val context = LocalContext.current

    // States
    var selectedType by remember(editItem) { mutableStateOf(editItem?.type ?: "Bill") } // "Bill", "EMI", "SIP", "UDHAR", "Other"
    
    var selectedCategory by remember(editItem) { mutableStateOf(editItem?.category ?: "Bills & Subscriptions") }
    var showCustomCategoryInput by remember { mutableStateOf(false) }
    var customCategoryName by remember { mutableStateOf("") }
    
    var rTitle by remember(editItem) { mutableStateOf(editItem?.title ?: "") }
    var rAmount by remember(editItem) { mutableStateOf(editItem?.amount?.toString() ?: "") }
    var rDueDate by remember(editItem) { mutableStateOf(editItem?.dueDate ?: "") }
    var rDurationEndDate by remember(editItem) { mutableStateOf(editItem?.durationEndDate ?: "") }
    var rFundsSource by remember(editItem) { mutableStateOf(editItem?.fundsSource ?: "") }
    
    var rRecurrence by remember(editItem) { mutableStateOf(editItem?.recurrence ?: "Monthly") }
    var rRepetitionState by remember(editItem) {
        mutableStateOf(
            if (editItem != null) {
                if (editItem.repetition.contains("times", ignoreCase = true)) "by custom Iterations" else editItem.repetition
            } else "until i Cancel"
        )
    }
    var rRepetitionCount by remember(editItem) {
        mutableStateOf(
            if (editItem != null && editItem.repetition.contains("times", ignoreCase = true)) {
                editItem.repetition.filter { it.isDigit() }.ifBlank { "12" }
            } else "12"
        )
    }
    
    var selectedSubCategory by remember(editItem) { mutableStateOf(editItem?.subCategory ?: "General") }
    var showCustomSubCategoryInput by remember { mutableStateOf(false) }
    var customSubCategoryName by remember { mutableStateOf("") }
    
    var rPaymentMode by remember(editItem) { mutableStateOf(editItem?.paymentMode ?: "Cash") }

    // Helpers
    val expenseCategories = remember(categories) {
        categories.filter { it.type.equals("Expense", ignoreCase = true) }
    }
    
    val availableSubs = remember(selectedCategory, categories) {
        val catObj = categories.firstOrNull { it.name.equals(selectedCategory, ignoreCase = true) }
        val list = catObj?.getSubcategories() ?: emptyList()
        if (list.isEmpty()) listOf("General") else list
    }

    // Auto mapping behavior on type selection
    LaunchedEffect(selectedType) {
        if (selectedType != "Other") {
            selectedCategory = when (selectedType) {
                "Bill" -> "Bills & Subscriptions"
                "EMI" -> "EMIs"
                "SIP" -> "SIPs"
                "UDHAR" -> "Udhar & Payables"
                else -> "Other commitments"
            }
            showCustomCategoryInput = false
        }
    }

    // Auto pre-populate funds source mapping
    LaunchedEffect(rPaymentMode) {
        if (rFundsSource.isBlank() || rFundsSource == "Cash Account" || rFundsSource == "UPI Wallet" || rFundsSource == "Bank Account" || rFundsSource == "Credit Line" || rFundsSource == "Udhar Wallet") {
            rFundsSource = when (rPaymentMode) {
                "Cash" -> "Cash Account"
                "UPI" -> "UPI Wallet"
                "Debit Card", "Netbanking" -> "Bank Account"
                "Credit Card" -> "Credit Line"
                "Udhar" -> "Udhar Wallet"
                else -> "General Funds"
            }
        }
    }

    // Modern Full Width Bottom/Sheet Dialog
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp), // Clearance for platform camera notch
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // --- STITCH HEADER: LOGO, TITLE, BELL & AVATAR ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFEC4899)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrackChanges,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (editItem != null) "Edit Existing" else "Create New",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Commitment",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Color(0xFF0F1B6B)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Notifications enabled for this commitment.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alert Reminders",
                                tint = if (isDark) Color.LightGray else Color(0xFF475569)
                            )
                        }
                        // Initials Avatar block exactly as designed (JS circle)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDBEAFE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "JS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E40AF)
                            )
                        }
                    }
                }

                // --- FOCUS CONTAINER 1: COMMITMENT TYPE CHOICE ---
                // Full screen top choice
                val isBill = selectedType == "Bill"
                val billGradient = Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF3B82F6)))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedType = "Bill" }
                        .border(
                            width = if (isBill) 2.5.dp else 1.dp,
                            brush = if (isBill) billGradient else androidx.compose.ui.graphics.SolidColor(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBill) Color(0xFF3B82F6).copy(alpha = 0.15f) else (if (isDark) Color(15, 23, 42) else Color.White)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = if (isBill) Color(0xFF3B82F6) else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Monthly Bill & Subscription",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBill) Color(0xFF2563EB) else (if (isDark) Color.White else Color.Black)
                        )
                    }
                }

                // Grid 4 under Bill choice (EMI, SIP, Udhar, Other)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // EMI
                    val isEmi = selectedType == "EMI"
                    val emiGradient = Brush.horizontalGradient(listOf(Color(0xFF047857), Color(0xFF10B981)))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedType = "EMI" }
                            .let {
                                if (isEmi) it.background(emiGradient)
                                else it.border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .background(if (isDark) Color(15, 23, 42) else Color.White)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = if (isEmi) Color.White else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "EMI",
                                color = if (isEmi) Color.White else (if (isDark) Color.White else Color.Black),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // SIP
                    val isSip = selectedType == "SIP"
                    val sipGradient = Brush.horizontalGradient(listOf(Color(0xFF1D4ED8), Color(0xFF06B6D4)))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedType = "SIP" }
                            .let {
                                if (isSip) it.background(sipGradient)
                                else it.border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .background(if (isDark) Color(15, 23, 42) else Color.White)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (isSip) Color.White else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "SIP",
                                color = if (isSip) Color.White else (if (isDark) Color.White else Color.Black),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Udhar
                    val isUdhar = selectedType == "UDHAR"
                    val udharGradient = Brush.horizontalGradient(listOf(Color(0xFF6B21A8), Color(0xFFEC4899)))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedType = "UDHAR" }
                            .let {
                                if (isUdhar) it.background(udharGradient)
                                else it.border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .background(if (isDark) Color(15, 23, 42) else Color.White)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Handshake,
                                contentDescription = null,
                                tint = if (isUdhar) Color.White else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Udhar",
                                color = if (isUdhar) Color.White else (if (isDark) Color.White else Color.Black),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Other
                    val isOther = selectedType == "Other"
                    val otherGradient = Brush.horizontalGradient(listOf(Color(0xFF374151), Color(0xFF6B7280)))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedType = "Other" }
                            .let {
                                if (isOther) it.background(otherGradient)
                                else it.border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .background(if (isDark) Color(15, 23, 42) else Color.White)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = null,
                                tint = if (isOther) Color.White else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Other",
                                color = if (isOther) Color.White else (if (isDark) Color.White else Color.Black),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // --- FOCUS CONTAINER 2: TEXT DETAILS INSIDE WHITE BACKGROUND CARD ---
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        
                        // 1. Title Input
                        Column {
                            Text(
                                text = "COMMITMENT DETAIL",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = rTitle,
                                onValueChange = { rTitle = it },
                                modifier = Modifier.fillMaxWidth().testTag("recurring_name_field"),
                                placeholder = { Text("e.g. Monthly Rent Payment", fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                                )
                            )
                        }

                        // 2. Amount Input
                        Column {
                            Text(
                                text = "COMMITMENT CASH VALUE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = rAmount,
                                onValueChange = { rAmount = it },
                                modifier = Modifier.fillMaxWidth().testTag("recurring_amount_field"),
                                placeholder = { Text("0.00", fontSize = 13.sp) },
                                leadingIcon = { Text("₹ ", fontWeight = FontWeight.ExtraBold, color = if (isDark) Color.White else Color.DarkGray) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                                )
                            )
                        }

                        // 3. Due Date Input with functional date picker action
                        Column {
                            Text(
                                text = "DUE DATE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = rDueDate,
                                onValueChange = { rDueDate = it },
                                modifier = Modifier.fillMaxWidth().testTag("recurring_due_date_field"),
                                placeholder = { Text("dd-mm-yyyy", fontSize = 13.sp) },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val mCalendar = Calendar.getInstance()
                                        val mYear = mCalendar.get(Calendar.YEAR)
                                        val mMonth = mCalendar.get(Calendar.MONTH)
                                        val mDay = mCalendar.get(Calendar.DAY_OF_MONTH)
                                        val mDatePicker = android.app.DatePickerDialog(
                                            context,
                                            { _, yr, mo, day ->
                                                rDueDate = String.format("%02d-%02d-%d", day, mo + 1, yr)
                                            }, mYear, mMonth, mDay
                                        )
                                        mDatePicker.show()
                                    }) {
                                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Pick Date")
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                                )
                            )
                        }

                        // 3b. Duration End Date (Contract Period end)
                        Column {
                            Text(
                                text = "CONTRACT END DATE (OPTIONAL)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = rDurationEndDate,
                                onValueChange = { rDurationEndDate = it },
                                modifier = Modifier.fillMaxWidth().testTag("recurring_duration_end_date_field"),
                                placeholder = { Text("dd-mm-yyyy (e.g. 31-12-2026)", fontSize = 13.sp) },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val mCalendar = Calendar.getInstance()
                                        val mYear = mCalendar.get(Calendar.YEAR)
                                        val mMonth = mCalendar.get(Calendar.MONTH)
                                        val mDay = mCalendar.get(Calendar.DAY_OF_MONTH)
                                        val mDatePicker = android.app.DatePickerDialog(
                                            context,
                                            { _, yr, mo, day ->
                                                rDurationEndDate = String.format("%02d-%02d-%d", day, mo + 1, yr)
                                            }, mYear, mMonth, mDay
                                        )
                                        mDatePicker.show()
                                    }) {
                                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Pick End Date")
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                                )
                            )
                        }

                        // 4. Category Choice
                        Column {
                            Text(
                                text = "COMMITMENT DETAIL",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.LightGray else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (!showCustomCategoryInput) {
                                var dropExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                                            .border(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                            .clickable { dropExpanded = true }
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = selectedCategory.ifBlank { "Select Category" }, fontSize = 13.sp)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = dropExpanded,
                                        onDismissRequest = { dropExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.85f).background(if (isDark) Color(0xFF1E293B) else Color.White)
                                    ) {
                                        expenseCategories.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat.name, fontSize = 13.sp) },
                                                onClick = {
                                                    selectedCategory = cat.name
                                                    dropExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                TextButton(
                                    onClick = { showCustomCategoryInput = true },
                                    modifier = Modifier.align(Alignment.Start)
                                ) {
                                    Text("+ Create New Category", fontSize = 11.sp, color = Color(0xFF0F1B6B), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customCategoryName,
                                        onValueChange = { customCategoryName = it },
                                        modifier = Modifier.weight(1.5f),
                                        placeholder = { Text("Custom Category Name", fontSize = 12.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    // Save button
                                    Button(
                                        onClick = {
                                            if (customCategoryName.isNotBlank()) {
                                                selectedCategory = customCategoryName.trim()
                                                showCustomCategoryInput = false
                                                customCategoryName = ""
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Set", fontSize = 11.sp)
                                    }
                                    // Cancel button
                                    TextButton(onClick = { showCustomCategoryInput = false }) {
                                        Text("Cancel", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        // 5. Subcategory Choice
                        Column {
                            Text(
                                text = "COMMITMENT DETAIL",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.LightGray else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (!showCustomSubCategoryInput) {
                                var subDropExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                                            .border(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                            .clickable { subDropExpanded = true }
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = selectedSubCategory.ifBlank { "Select Subcategory" }, fontSize = 13.sp)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = subDropExpanded,
                                        onDismissRequest = { subDropExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.85f).background(if (isDark) Color(0xFF1E293B) else Color.White)
                                    ) {
                                        availableSubs.forEach { sub ->
                                            DropdownMenuItem(
                                                text = { Text(sub, fontSize = 13.sp) },
                                                onClick = {
                                                    selectedSubCategory = sub
                                                    subDropExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                TextButton(
                                    onClick = { showCustomSubCategoryInput = true },
                                    modifier = Modifier.align(Alignment.Start)
                                ) {
                                    Text("+ Create New Subcategory", fontSize = 11.sp, color = Color(0xFF0F1B6B), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customSubCategoryName,
                                        onValueChange = { customSubCategoryName = it },
                                        modifier = Modifier.weight(1.5f),
                                        placeholder = { Text("Custom Subcategory", fontSize = 12.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Button(
                                        onClick = {
                                            if (customSubCategoryName.isNotBlank()) {
                                                selectedSubCategory = customSubCategoryName.trim()
                                                showCustomSubCategoryInput = false
                                                customSubCategoryName = ""
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Set", fontSize = 11.sp)
                                    }
                                    TextButton(onClick = { showCustomSubCategoryInput = false }) {
                                        Text("Cancel", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        // Extra elegant detail field for Funds Source (Automated on selection)
                        Column {
                            Text(
                                text = "DEPOSIT SOURCE / INSTITUTION TAG",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.LightGray else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = rFundsSource,
                                onValueChange = { rFundsSource = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. Salary Acct, Cash Pocket", fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0)
                                )
                            )
                        }
                    }
                }

                // --- CUSTOM COLOURFUL & TEXTURED RECURRENCE & REPETITION CARD ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E1E38) else Color(0xFFFFF1F2)
                    ),
                    border = BorderStroke(1.5.dp, if (isDark) Color(0xFFE11D48) else Color(0xFFFDA4AF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFE11D48).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Recurrence & Repetition Rules",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else Color(0xFF9F1239)
                            )
                        }

                        Divider(color = if (isDark) Color(0xFF3B2F50) else Color(0xFFFEE2E2), thickness = 1.dp)

                        Column {
                            Text(
                                text = "RECURRENCE INTERVAL",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.LightGray else Color(0xFFBE123C)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Monthly", "Quarterly", "Half Yearly", "Yearly").forEach { freq ->
                                    val isSelected = rRecurrence == freq
                                    val colorActive = Color(0xFFE11D48)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { rRecurrence = freq }
                                            .background(if (isSelected) colorActive else (if (isDark) Color(0xFF131524) else Color.White))
                                            .border(
                                                1.dp,
                                                if (isSelected) colorActive else (if (isDark) Color(0xFF334155) else Color(0xFFFDA4AF)),
                                                RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = freq,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else (if (isDark) Color.LightGray else Color(0xFFBE123C))
                                        )
                                    }
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "REPETITION DURATION",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.LightGray else Color(0xFFBE123C)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val stateUntilCancel = rRepetitionState == "until i Cancel"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { rRepetitionState = "until i Cancel" }
                                        .background(if (stateUntilCancel) Color(0xFF10B981) else (if (isDark) Color(0xFF131524) else Color.White))
                                        .border(
                                            1.1.dp,
                                            if (stateUntilCancel) Color(0xFF10B981) else (if (isDark) Color(0xFF334155) else Color(0xFFFDA4AF)),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Until I Cancel",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (stateUntilCancel) Color.White else (if (isDark) Color.LightGray else Color.Gray)
                                    )
                                }

                                val stateSpecificTimes = rRepetitionState == "specific"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { rRepetitionState = "specific" }
                                        .background(if (stateSpecificTimes) Color(0xFF3B82F6) else (if (isDark) Color(0xFF131524) else Color.White))
                                        .border(
                                            1.1.dp,
                                            if (stateSpecificTimes) Color(0xFF3B82F6) else (if (isDark) Color(0xFF334155) else Color(0xFFFDA4AF)),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Specify Repetitions",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (stateSpecificTimes) Color.White else (if (isDark) Color.LightGray else Color.Gray)
                                    )
                                }
                            }

                            if (rRepetitionState == "specific") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Number of times to repeat:",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color.LightGray else Color.DarkGray
                                    )
                                    OutlinedTextField(
                                        value = rRepetitionCount,
                                        onValueChange = { rRepetitionCount = it },
                                        singleLine = true,
                                        modifier = Modifier.width(100.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFFE11D48),
                                            unfocusedBorderColor = Color(0xFFFDA4AF)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // --- FOCUS CONTAINER 3: PREFERRED MODE OF PAYMENT GRID ---
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "COMMITMENT DETAIL",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.LightGray else Color(0xFF64748B),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 3x2 grid modeled exactly like the visual mockup
                        // Row 1: Cash, UPI, Debit Card
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Cash", "UPI", "Debit Card").forEach { mode ->
                                val isSelected = rPaymentMode == mode
                                val itemBg = when (mode) {
                                    "Cash" -> Color(0xFFE8F5E9)
                                    "UPI" -> Color(0xFFE3F2FD)
                                    else -> Color(0xFFEEF2FF)
                                }
                                val itemTextColor = when (mode) {
                                    "Cash" -> Color(0xFF2E7D32)
                                    "UPI" -> Color(0xFF1565C0)
                                    else -> Color(0xFF4338CA)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { rPaymentMode = mode }
                                        .background(if (isSelected) itemBg else (if (isDark) Color(15, 23, 42) else Color(0xFFF8FAFC)))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) itemTextColor else (if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0)),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(
                                            imageVector = when (mode) {
                                                "Cash" -> Icons.Default.Payments
                                                "UPI" -> Icons.Default.QrCodeScanner
                                                else -> Icons.Default.CreditCard
                                            },
                                            contentDescription = null,
                                            tint = if (isSelected) itemTextColor else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = mode.uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) itemTextColor else (if (isDark) Color.LightGray else Color.DarkGray)
                                        )
                                    }
                                }
                            }
                        }

                        // Row 2: Credit Card, Netbanking, Udhar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Credit Card", "Netbanking", "Udhar").forEach { mode ->
                                val isSelected = rPaymentMode == mode
                                val itemBg = when (mode) {
                                    "Credit Card" -> Color(0xFFFFE4E6)
                                    "Netbanking" -> Color(0xFFF1F5F9)
                                    else -> Color(0xFFFDF2F8)
                                }
                                val itemTextColor = when (mode) {
                                    "Credit Card" -> Color(0xFFBE123C)
                                    "Netbanking" -> Color(0xFF475569)
                                    else -> Color(0xFFBE185D)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { rPaymentMode = mode }
                                        .background(if (isSelected) itemBg else (if (isDark) Color(15, 23, 42) else Color(0xFFF8FAFC)))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) itemTextColor else (if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0)),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(
                                            imageVector = when (mode) {
                                                "Credit Card" -> Icons.Default.Favorite
                                                "Netbanking" -> Icons.Default.AccountBalance
                                                else -> Icons.Default.Handshake
                                            },
                                            contentDescription = null,
                                            tint = if (isSelected) itemTextColor else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = mode.uppercase().replace(" CARD", ""),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) itemTextColor else (if (isDark) Color.LightGray else Color.DarkGray)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- FOOTER SAVE & DISMISS STICKY ROW ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(14.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                    }

                    Button(
                        onClick = {
                            if (rTitle.isBlank()) {
                                Toast.makeText(context, "Please enter a valid Name!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val amt = rAmount.toDoubleOrNull() ?: 0.0
                            if (amt <= 0.0) {
                                Toast.makeText(context, "Please enter a valid positive Amount!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (rDueDate.isBlank()) {
                                Toast.makeText(context, "Due date description is required!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val finalRep = if (rRepetitionState == "until i Cancel") "until i Cancel" else "${rRepetitionCount} times"
                            val finalCat = if (showCustomCategoryInput) customCategoryName else selectedCategory
                            val finalSub = if (showCustomSubCategoryInput) customSubCategoryName else selectedSubCategory
                            val finalFunds = rFundsSource.ifBlank { "General Funds" }

                            if (editItem != null && onUpdate != null) {
                                onUpdate(
                                    editItem.copy(
                                        title = rTitle,
                                        amount = amt,
                                        dueDate = rDueDate,
                                        category = finalCat,
                                        subCategory = finalSub,
                                        paymentMode = rPaymentMode,
                                        type = selectedType,
                                        fundsSource = finalFunds,
                                        recurrence = rRecurrence,
                                        repetition = finalRep,
                                        durationEndDate = rDurationEndDate
                                    )
                                )
                            } else {
                                onSave(
                                    rTitle,
                                    amt,
                                    rDueDate,
                                    finalCat,
                                    finalSub,
                                    rPaymentMode,
                                    selectedType,
                                    finalFunds,
                                    rRecurrence,
                                    finalRep,
                                    rDurationEndDate
                                )
                            }
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("recurring_save_confirm_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(
                            text = if (editItem != null) "Update Commitment" else "Save Commitment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommitmentDetailsDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    item: com.example.data.model.DbRecurringItem,
    isDark: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    if (!show) return

    val textCol = if (isDark) Color.White else Color(0xFF0F1B6B)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Commitment Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textCol
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailFieldRow(label = "Title", value = item.title, isDark = isDark)
                DetailFieldRow(label = "Cash Value / Amount", value = "₹${item.amount}", isDark = isDark, highlight = true)
                DetailFieldRow(label = "Commitment Type", value = item.type, isDark = isDark)
                DetailFieldRow(label = "Category", value = item.category, isDark = isDark)
                DetailFieldRow(label = "Subcategory", value = item.subCategory, isDark = isDark)
                DetailFieldRow(label = "Due Date", value = item.dueDate, isDark = isDark)
                DetailFieldRow(label = "Payment Mode", value = item.paymentMode, isDark = isDark)
                DetailFieldRow(label = "Source of Funds", value = if (item.fundsSource.isNotBlank()) item.fundsSource else "General Funds", isDark = isDark)
                DetailFieldRow(label = "Recurrence", value = item.recurrence, isDark = isDark)
                DetailFieldRow(label = "Repetition Limit", value = item.repetition, isDark = isDark)
            }
        },
        confirmButton = {
            Button(
                onClick = onEditClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
        }
    )
}

@Composable
fun DetailFieldRow(label: String, value: String, isDark: Boolean, highlight: Boolean = false) {
    Column {
        Text(
            text = label.uppercase(Locale.ROOT),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            fontSize = if (highlight) 14.sp else 12.sp,
            fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (highlight) (if (isDark) Color(0xFF34D399) else Color(0xFF0F766E)) else (if (isDark) Color.White else Color.Black)
        )
    }
}
