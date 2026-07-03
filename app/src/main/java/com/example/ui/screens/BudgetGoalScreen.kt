package com.example.ui.screens

import android.widget.Toast

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DbBudget
import com.example.data.model.DbBudgetGoalActivity
import com.example.data.model.DbCategory
import com.example.data.model.DbGoal
import com.example.ui.AppViewModel
import com.example.ui.components.UnifiedBottomNavBar
import com.example.ui.components.UnifiedTopBar
import com.example.ui.components.GlobalConfirmationModal

import com.example.ui.theme.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BudgetGoalDashboardScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScreen: (String) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    LaunchedEffect(currentUser) {
        currentUser?.let {
            viewModel.syncBudgetsAndGoals(it.userId)
        }
    }

    var activeSubScreen by remember { mutableStateOf("dashboard") } // "dashboard", "create_budget", "create_goal"

    var showGlobalConfirmDialog by remember { mutableStateOf(false) }
    var globalConfirmTitle by remember { mutableStateOf("Are you sure?") }
    var globalConfirmMessage by remember { mutableStateOf("") }
    var globalOnConfirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var globalConfirmColor by remember { mutableStateOf(Color(0xFFEF4444)) }
    
    // Dialog / Modal state

    var activeTopUpTarget by remember { mutableStateOf<Pair<Long, Boolean>?>(null) } // parentId, isBudget
    var activeSpendTarget by remember { mutableStateOf<Pair<Long, Boolean>?>(null) } // parentId, isBudget

    val isDark = isDarkThemeActive
    val scaffoldBg = if (isDark) BrandBackground else Color(0xFFF8FAFC)

    androidx.activity.compose.BackHandler(enabled = activeSubScreen != "dashboard") {
        activeSubScreen = "dashboard"
    }

    Scaffold(
        containerColor = scaffoldBg,
        modifier = Modifier.fillMaxSize().testTag("budget_goal_scaffold")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeSubScreen) {
                "dashboard" -> {
                    DashboardView(
                        viewModel = viewModel,
                        onCreateBudgetClick = { activeSubScreen = "create_budget" },
                        onCreateGoalClick = { activeSubScreen = "create_goal" },
                        onTopUpClick = { id, isBudget -> activeTopUpTarget = Pair(id, isBudget) },
                        onSpendClick = { id, isBudget -> activeSpendTarget = Pair(id, isBudget) }
                    )
                }
                "create_budget" -> {
                    CreateBudgetView(
                        viewModel = viewModel,
                        onCompleted = { activeSubScreen = "dashboard" }
                    )
                }
                "create_goal" -> {
                    CreateGoalView(
                        viewModel = viewModel,
                        onCompleted = { activeSubScreen = "dashboard" }
                    )
                }
            }

            // Top Up Modal
            if (activeTopUpTarget != null) {
                val (parentId, isBudget) = activeTopUpTarget!!
                TopUpModal(
                    viewModel = viewModel,
                    parentId = parentId,
                    isBudget = isBudget,
                    onDismiss = { activeTopUpTarget = null }
                )
            }

            // Spend Modal
            if (activeSpendTarget != null) {
                val (parentId, isBudget) = activeSpendTarget!!
                SpendModal(
                    viewModel = viewModel,
                    parentId = parentId,
                    isBudget = isBudget,
                    onDismiss = { activeSpendTarget = null }
                )
            }
        }
    }
}

@Composable
fun DashboardView(
    viewModel: AppViewModel,
    onCreateBudgetClick: () -> Unit,
    onCreateGoalClick: () -> Unit,
    onTopUpClick: (Long, Boolean) -> Unit,
    onSpendClick: (Long, Boolean) -> Unit
) {
    val budgets by viewModel.userBudgets.collectAsState()
    val goals by viewModel.userGoals.collectAsState()
    val isDark = isDarkThemeActive
    val scrollState = rememberScrollState()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var targetDeleteBudget by remember { mutableStateOf<DbBudget?>(null) }
    var targetDeleteGoal by remember { mutableStateOf<DbGoal?>(null) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Create Headers Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Create Budget Button Card
            Card(
                onClick = onCreateBudgetClick,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) BrandSurface else Color.White
                ),
                border = BorderStroke(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0)),
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .testTag("create_budget_option_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFE0F2FE), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = "Create Budget",
                            tint = Color(0xFF0369A1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create Budget",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "e.g. Travel, Food",
                        fontSize = 10.sp,
                        color = if (isDark) Color.LightGray else Color(0xFF64748B)
                    )
                }
            }

            // Create Goal Button Card
            Card(
                onClick = onCreateGoalClick,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) BrandSurface else Color.White
                ),
                border = BorderStroke(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0)),
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .testTag("create_goal_option_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFDCFCE7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = "Create Goal",
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create Goal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "e.g. New Bike, Emergency House",
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isDark) Color.LightGray else Color(0xFF64748B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Active Goals Table/Section
        Text(
            text = "Active Budgets & Goals",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = if (isDark) Color.White else Color(0xFF000839)
        )
        Text(
            text = "Tracking your long-term wealth milestones and thresholds",
            fontSize = 12.sp,
            color = if (isDark) Color.LightGray.copy(alpha = 0.8f) else Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Persistent Summary Metrics Panel (Total Allocated, Total Spent, Remaining)
        val totalAllocated = budgets.sumOf { it.totalAllocated }
        val totalSpent = budgets.sumOf { it.spentAmount }
        val remainingFunds = (totalAllocated - totalSpent).coerceAtLeast(0.0)

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFBFDBFE)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("budget_summary_header")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Allocated",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.LightGray else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format("₹%,.0f", totalAllocated),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF1E3A8A)
                    )
                }
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(if (isDark) Color(0xFF334155) else Color(0xFFBFDBFE))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Spent",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.LightGray else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format("₹%,.0f", totalSpent),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFEF4444)
                    )
                }
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(if (isDark) Color(0xFF334155) else Color(0xFFBFDBFE))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Remaining",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.LightGray else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format("₹%,.0f", remainingFunds),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Render Budgets
        if (budgets.isEmpty() && goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
                    .background(
                        color = if (isDark) BrandSurface.copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Empty planning data",
                        tint = if (isDark) Color.LightGray else Color.Gray,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Active Budgets or Goals",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDark) Color.White else Color(0xFF334155)
                    )
                    Text(
                        text = "Click the creation cards above to define your targets.",
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        color = if (isDark) Color.LightGray else Color(0xFF64748B)
                    )
                }
            }
        } else {
            budgets.forEach { budget ->
                BudgetCard(
                    budget = budget,
                    viewModel = viewModel,
                    onTopUp = { onTopUpClick(budget.id, true) },
                    onSpend = { onSpendClick(budget.id, true) },
                    onDelete = {
                        targetDeleteBudget = budget
                        targetDeleteGoal = null
                        showDeleteConfirmDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            goals.forEach { goal ->
                GoalCard(
                    goal = goal,
                    viewModel = viewModel,
                    onTopUp = { onTopUpClick(goal.id, false) },
                    onSpend = { onSpendClick(goal.id, false) },
                    onDelete = {
                        targetDeleteGoal = goal
                        targetDeleteBudget = null
                        showDeleteConfirmDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Savings Velocity Chart Section
        SavingsVelocityChartSection(viewModel = viewModel)
    }

    GlobalConfirmationModal(
        show = showDeleteConfirmDialog,
        title = "Are you sure?",
        message = if (targetDeleteBudget != null) {
            "Do you want to permanently delete the budget '${targetDeleteBudget?.name}'?"
        } else {
            "Do you want to permanently delete the goal '${targetDeleteGoal?.name}'?"
        },

        confirmButtonColor = Color(0xFFEF4444),
        isDark = isDark,
        onConfirm = {
            if (targetDeleteBudget != null) {
                viewModel.deleteBudget(targetDeleteBudget!!)
                targetDeleteBudget = null
            } else if (targetDeleteGoal != null) {
                viewModel.deleteGoal(targetDeleteGoal!!)
                targetDeleteGoal = null
            }
            showDeleteConfirmDialog = false
        },
        onCancel = {
            targetDeleteBudget = null
            targetDeleteGoal = null
            showDeleteConfirmDialog = false
        }
    )
}


@Composable
fun BudgetCard(
    budget: DbBudget,
    viewModel: AppViewModel,
    onTopUp: () -> Unit,
    onSpend: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isDarkThemeActive
    val context = LocalContext.current
    
    // Real-time stats subscription
    val budgetStats by viewModel.getBudgetStats(budget.id).collectAsState(initial = null)
    val allocated = budgetStats?.totalAllocated ?: (budget.limitAmount + budget.savedAmount)
    val spent = budgetStats?.totalSpent ?: budget.spentAmount
    val remaining = budgetStats?.remainingBudget ?: ((budget.limitAmount + budget.savedAmount) - budget.spentAmount)
    val isOverspent = remaining < 0.0

    // Available = Limit + TopUp - Spend
    val availableAmount = remaining
    val totalLimit = allocated
    val progress = if (totalLimit > 0) (spent / totalLimit).coerceIn(0.0, 1.0) else 0.0
    val progressPctMessage = String.format("%.0f%% Spent", progress * 100)

    val activities by viewModel.getActivitiesForBudgetOrGoal(budget.id, isBudget = true).collectAsState(initial = emptyList())
    var expandedActivities by remember { mutableStateOf(false) }
    var activityFilter by remember { mutableStateOf("All") }

    var activeActivityForDetail by remember { mutableStateOf<DbBudgetGoalActivity?>(null) }
    var activeActivityForModify by remember { mutableStateOf<DbBudgetGoalActivity?>(null) }
    var activeActivityForDelete by remember { mutableStateOf<DbBudgetGoalActivity?>(null) }

    var showModifyConfirmProtocol by remember { mutableStateOf(false) }
    var showDeleteConfirmProtocol by remember { mutableStateOf(false) }

    var pendingModifyTitle by remember { mutableStateOf("") }
    var pendingModifyAmount by remember { mutableStateOf(0.0) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) BrandSurface else Color.White
        ),
        border = BorderStroke(
            width = if (isOverspent) 2.dp else 1.dp,
            color = if (isOverspent) Color.Red else (if (isDark) BrandOutline else Color(0xFFE2E8F0))
        ),
        modifier = Modifier.fillMaxWidth().testTag("budget_card_${budget.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF3B82F6), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = budget.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeBgColor = when {
                        progress >= 0.9 -> Color(0xFFFEE2E2)
                        progress >= 0.7 -> Color(0xFFFEF3C7)
                        else -> Color(0xFFD1FAE5)
                    }
                    val badgeTextColor = when {
                        progress >= 0.9 -> Color(0xFFEF4444)
                        progress >= 0.7 -> Color(0xFFD97706)
                        else -> Color(0xFF059669)
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                color = badgeBgColor,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = progressPctMessage,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = badgeTextColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp).testTag("delete_budget_${budget.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Budget",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Three horizontal summary indicators / chips immediately below the main title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Allocated
                StatChip(
                    title = "Allocated",
                    amount = allocated,
                    color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                    onClick = {
                        expandedActivities = true
                        activityFilter = "Top up"
                    }
                )
                
                // Total Spent
                StatChip(
                    title = "Spent",
                    amount = spent,
                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                    onClick = {
                        expandedActivities = true
                        activityFilter = "Spend"
                    }
                )
                
                // Remaining Budget
                val remainingColor = when {
                    remaining > 0 -> if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                    remaining == 0.0 -> if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
                    else -> if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                }
                val remainingBorderColor = when {
                    remaining > 0 -> Color(0xFF10B981)
                    remaining == 0.0 -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                }
                StatChip(
                    title = "Remaining",
                    amount = remaining,
                    color = remainingColor,
                    borderColor = remainingBorderColor,
                    onClick = {
                        expandedActivities = true
                        activityFilter = "All"
                    }
                )
            }

            // Limit vs Spend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent Amount",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = String.format("₹ %,.2f", spent),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (progress >= 0.9) Color(0xFFEF4444) else (if (isDark) Color.White else Color(0xFF334155))
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Available Budget",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = String.format("₹ %,.2f", availableAmount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (availableAmount <= 0) Color.Red else Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val progressColor = when {
                progress >= 0.9 -> Color(0xFFEF4444) // overspent / high consumption (Red)
                progress >= 0.7 -> Color(0xFFF59E0B) // nearing limit / moderate consumption (Yellow)
                else -> Color(0xFF10B981) // healthy / low consumption (Green)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget Consumption Level",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF334155)
                )
                val consumptionLevelText = when {
                    progress >= 0.9 -> "High Consumption"
                    progress >= 0.7 -> "Moderate Consumption"
                    else -> "Low Consumption"
                }
                Text(
                    text = "$consumptionLevelText (${String.format("%.0f%%", progress * 100)})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { progress.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (budget.limitAmount > 0.0) {
                        String.format("Overall limit: ₹ %,.2f", totalLimit)
                    } else {
                        String.format("Total Funded: ₹ %,.2f", allocated)
                    },
                    fontSize = 9.sp,
                    color = Color.Gray
                )
                Text(
                    text = String.format("Spent: ₹ %,.2f of ₹ %,.2f", spent, totalLimit),
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTopUp,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("top_up_budget_${budget.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) BrandPrimary else Color(0xFF1E2A7E)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Top Up", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onSpend,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("spend_budget_${budget.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Spend", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recent activity header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedActivities = !expandedActivities },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RECENT ACTIVITY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Icon(
                    imageVector = if (expandedActivities) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = expandedActivities) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    val filteredActivities = when (activityFilter) {
                        "Top up" -> activities.filter { it.amount > 0 }
                        "Spend" -> activities.filter { it.amount <= 0 }
                        else -> activities
                    }

                    // Filter and Export row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Small filter pills row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("All", "Top up", "Spend").forEach { filter ->
                                val isSelected = activityFilter == filter
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isSelected) {
                                                if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                            } else {
                                                Color.Transparent
                                            }
                                        )
                                        .clickable { activityFilter = filter }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = when (filter) {
                                            "Top up" -> "Top Ups"
                                            "Spend" -> "Spends"
                                            else -> "All"
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            if (isDark) Color.White else Color(0xFF1E293B)
                                        } else {
                                            Color.Gray
                                        }
                                    )
                                }
                            }
                        }

                        // Export Button
                        TextButton(
                            onClick = {
                                val csvContent = com.example.util.CsvExporter.formatActivitiesToCsv(filteredActivities)
                                com.example.util.CsvExporter.exportToCsvFile(context, "budget_${budget.id}_ledger_export.csv", csvContent)
                            },
                            modifier = Modifier.height(28.dp).testTag("export_budget_csv_button"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Export CSV", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (filteredActivities.isEmpty()) {
                        Text(
                            text = "No matching transactions logged yet.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        filteredActivities.forEach { act ->
                            ActivityRow(
                                activity = act,
                                onDetail = { activeActivityForDetail = act },
                                onModify = { activeActivityForModify = act },
                                onDelete = {
                                    activeActivityForDelete = act
                                    showDeleteConfirmProtocol = true
                                }
                            )
                        }
                    }

                    activeActivityForDetail?.let { act ->
                        BudgetGoalActivityDetailDialog(
                            activity = act,
                            isDark = isDark,
                            onDismiss = { activeActivityForDetail = null }
                        )
                    }

                    activeActivityForModify?.let { act ->
                        BudgetGoalActivityModifyDialog(
                            activity = act,
                            isDark = isDark,
                            onDismiss = { activeActivityForModify = null },
                            onConfirm = { newTitle, newAmount ->
                                pendingModifyTitle = newTitle
                                pendingModifyAmount = newAmount
                                showModifyConfirmProtocol = true
                            }
                        )
                    }

                    GlobalConfirmationModal(
                        show = showModifyConfirmProtocol,
                        title = "Are you sure?",
                        message = "Do you want to modify this transaction's title to '$pendingModifyTitle' and amount to ₹$pendingModifyAmount?",
                        confirmButtonColor = BrandPrimary,
                        isDark = isDark,
                        onConfirm = {
                            activeActivityForModify?.let { act ->
                                viewModel.updateBudgetGoalActivity(act, pendingModifyTitle, pendingModifyAmount)
                            }
                            showModifyConfirmProtocol = false
                            activeActivityForModify = null
                        },
                        onCancel = {
                            showModifyConfirmProtocol = false
                        }
                    )

                    GlobalConfirmationModal(
                        show = showDeleteConfirmProtocol,
                        title = "Are you sure?",
                        message = "Do you want to permanently delete this transaction? This will automatically reverse its effect on your ledger balance.",
                        confirmButtonColor = Color(0xFFEF4444),
                        isDark = isDark,
                        onConfirm = {
                            activeActivityForDelete?.let { act ->
                                viewModel.deleteBudgetGoalActivity(act)
                            }
                            showDeleteConfirmProtocol = false
                            activeActivityForDelete = null
                        },
                        onCancel = {
                            showDeleteConfirmProtocol = false
                            activeActivityForDelete = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.StatChip(
    title: String,
    amount: Double,
    color: Color,
    borderColor: Color? = null,
    onClick: () -> Unit
) {
    val isDark = isDarkThemeActive
    val bg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    Box(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .then(
                if (borderColor != null) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(10.dp))
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title.uppercase(), fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(
                text = String.format("₹%,.0f", amount),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun GoalCard(
    goal: DbGoal,
    viewModel: AppViewModel,
    onTopUp: () -> Unit,
    onSpend: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isDarkThemeActive
    val progress = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).coerceIn(0.0, 1.0) else 0.0
    val progressPctMessage = String.format("%.0f%% Achieved", progress * 100)
    val remainingToSave = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)

    val activities by viewModel.getActivitiesForBudgetOrGoal(goal.id, isBudget = false).collectAsState(initial = emptyList())
    var expandedActivities by remember { mutableStateOf(false) }
    var activityFilter by remember { mutableStateOf("All") }

    var activeActivityForDetail by remember { mutableStateOf<DbBudgetGoalActivity?>(null) }
    var activeActivityForModify by remember { mutableStateOf<DbBudgetGoalActivity?>(null) }
    var activeActivityForDelete by remember { mutableStateOf<DbBudgetGoalActivity?>(null) }

    var showModifyConfirmProtocol by remember { mutableStateOf(false) }
    var showDeleteConfirmProtocol by remember { mutableStateOf(false) }

    var pendingModifyTitle by remember { mutableStateOf("") }
    var pendingModifyAmount by remember { mutableStateOf(0.0) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) BrandSurface else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth().testTag("goal_card_${goal.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = goal.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (goal.priority) {
                                    "HIGH" -> Color(0xFFFEE2E2)
                                    "MEDIUM" -> Color(0xFFFEF3C7)
                                    else -> Color(0xFFF3F4F6)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = goal.priority,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            color = when (goal.priority) {
                                "HIGH" -> Color(0xFFEF4444)
                                "MEDIUM" -> Color(0xFFD97706)
                                else -> Color(0xFF64748B)
                            }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDCFCE7), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = progressPctMessage,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color(0xFF15803D)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp).testTag("delete_goal_${goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Goal",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Saved vs Remaining
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Saved Amount",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = String.format("₹ %,.2f", goal.savedAmount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (isDark) Color.White else Color(0xFF334155)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "To Go Amount",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = String.format("₹ %,.2f", remainingToSave),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (remainingToSave <= 0) Color(0xFF10B981) else Color.Red
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Savings Goal Target Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Goal Target Savings Progress",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF334155)
                )
                Text(
                    text = progressPctMessage,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (progress >= 1.0) Color(0xFF10B981) else if (progress >= 0.7) Color(0xFFF59E0B) else Color(0xFFEF4444)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val progressColor = when {
                progress >= 1.0 -> Color(0xFF10B981) // achieved (Green)
                progress >= 0.7 -> Color(0xFFF59E0B) // nearing target (Yellow)
                else -> Color(0xFFEF4444) // long way to go (Red)
            }
            LinearProgressIndicator(
                progress = { progress.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format("Target: ₹ %,.2f • Target Date: %s", goal.targetAmount, goal.targetDate.ifBlank { "Anytime" }),
                fontSize = 9.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Goal Fund Consumption Level Progress Bar (Spent vs Allocated)
            val goalTotalSpent = activities.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
            val goalTotalAllocated = (goal.savedAmount + goalTotalSpent).coerceAtLeast(0.0)
            val consumptionRatio = if (goalTotalAllocated > 0.0) (goalTotalSpent / goalTotalAllocated).coerceIn(0.0, 1.0) else 0.0

            val consumptionColor = when {
                consumptionRatio >= 0.9 -> Color(0xFFEF4444) // Red
                consumptionRatio >= 0.7 -> Color(0xFFF59E0B) // Yellow
                else -> Color(0xFF10B981) // Green
            }

            val consumptionPctMessage = String.format("%.0f%% Spent of Allocated Fund", consumptionRatio * 100)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Goal Fund Consumption Level",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF334155)
                )
                Text(
                    text = consumptionPctMessage,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = consumptionColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { consumptionRatio.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = consumptionColor,
                trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format("Spent: ₹ %,.2f", goalTotalSpent),
                    fontSize = 9.sp,
                    color = Color.Gray
                )
                Text(
                    text = String.format("Allocated (Saved+Spent): ₹ %,.2f", goalTotalAllocated),
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTopUp,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("top_up_goal_${goal.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) BrandPrimary else Color(0xFF1E2A7E)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Top Up", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onSpend,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("spend_goal_${goal.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Spend", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recent activity header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedActivities = !expandedActivities },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RECENT ACTIVITY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Icon(
                    imageVector = if (expandedActivities) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = expandedActivities) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Small filter pills row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Top up", "Spend").forEach { filter ->
                            val isSelected = activityFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) {
                                            if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .clickable { activityFilter = filter }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = when (filter) {
                                        "Top up" -> "Top Ups"
                                        "Spend" -> "Spends"
                                        else -> "All"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        if (isDark) Color.White else Color(0xFF1E293B)
                                    } else {
                                        Color.Gray
                                    }
                                )
                            }
                        }
                    }

                    val filteredActivities = when (activityFilter) {
                        "Top up" -> activities.filter { it.amount > 0 }
                        "Spend" -> activities.filter { it.amount <= 0 }
                        else -> activities
                    }

                    if (filteredActivities.isEmpty()) {
                        Text(
                            text = "No matching transactions logged yet.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        filteredActivities.forEach { act ->
                            ActivityRow(
                                activity = act,
                                onDetail = { activeActivityForDetail = act },
                                onModify = { activeActivityForModify = act },
                                onDelete = {
                                    activeActivityForDelete = act
                                    showDeleteConfirmProtocol = true
                                }
                            )
                        }
                    }

                    activeActivityForDetail?.let { act ->
                        BudgetGoalActivityDetailDialog(
                            activity = act,
                            isDark = isDark,
                            onDismiss = { activeActivityForDetail = null }
                        )
                    }

                    activeActivityForModify?.let { act ->
                        BudgetGoalActivityModifyDialog(
                            activity = act,
                            isDark = isDark,
                            onDismiss = { activeActivityForModify = null },
                            onConfirm = { newTitle, newAmount ->
                                pendingModifyTitle = newTitle
                                pendingModifyAmount = newAmount
                                showModifyConfirmProtocol = true
                            }
                        )
                    }

                    GlobalConfirmationModal(
                        show = showModifyConfirmProtocol,
                        title = "Are you sure?",
                        message = "Do you want to modify this transaction's title to '$pendingModifyTitle' and amount to ₹$pendingModifyAmount?",
                        confirmButtonColor = BrandPrimary,
                        isDark = isDark,
                        onConfirm = {
                            activeActivityForModify?.let { act ->
                                viewModel.updateBudgetGoalActivity(act, pendingModifyTitle, pendingModifyAmount)
                            }
                            showModifyConfirmProtocol = false
                            activeActivityForModify = null
                        },
                        onCancel = {
                            showModifyConfirmProtocol = false
                        }
                    )

                    GlobalConfirmationModal(
                        show = showDeleteConfirmProtocol,
                        title = "Are you sure?",
                        message = "Do you want to permanently delete this transaction? This will automatically reverse its effect on your ledger balance.",
                        confirmButtonColor = Color(0xFFEF4444),
                        isDark = isDark,
                        onConfirm = {
                            activeActivityForDelete?.let { act ->
                                viewModel.deleteBudgetGoalActivity(act)
                            }
                            showDeleteConfirmProtocol = false
                            activeActivityForDelete = null
                        },
                        onCancel = {
                            showDeleteConfirmProtocol = false
                            activeActivityForDelete = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityRow(
    activity: DbBudgetGoalActivity,
    onDetail: () -> Unit,
    onModify: () -> Unit,
    onDelete: () -> Unit
) {
    val isPositive = activity.amount > 0
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }
    val formattedDate = sdf.format(Date(activity.date))
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = if (isPositive) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = activity.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isDarkThemeActive) Color.White else Color(0xFF334155),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isPositive) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.2.dp)
                    ) {
                        Text(
                            text = if (isPositive) "Top up" else "Spend",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 7.5.sp,
                            color = if (isPositive) Color(0xFF137333) else Color(0xFFC5221F)
                        )
                    }
                }
                Text(
                    text = formattedDate,
                    fontSize = 8.sp,
                    color = Color.Gray
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format("%s ₹ %,.0f", if (isPositive) "+" else "-", Math.abs(activity.amount)),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isPositive) Color(0xFF137333) else Color(0xFFC5221F)
            )
            
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp).testTag("activity_menu_btn_${activity.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Detail", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            onDetail()
                        },
                        modifier = Modifier.testTag("activity_detail_item_${activity.id}")
                    )
                    DropdownMenuItem(
                        text = { Text("Modify", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            onModify()
                        },
                        modifier = Modifier.testTag("activity_modify_item_${activity.id}")
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", fontSize = 11.sp, color = Color.Red) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        modifier = Modifier.testTag("activity_delete_item_${activity.id}")
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetGoalActivityDetailDialog(
    activity: DbBudgetGoalActivity,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val sdf = remember { java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US) }
    val formattedDate = sdf.format(java.util.Date(activity.date))
    val isPositive = activity.amount >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Transaction Details",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth().testTag("detail_dialog_title"),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text("DESCRIPTION / MERCHANT", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(
                        text = activity.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDark) Color.White else Color(0xFF1E293B)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("TYPE", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isPositive) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isPositive) "Top Up (Inflow)" else "Spend (Outflow)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                color = if (isPositive) Color(0xFF137333) else Color(0xFFC5221F)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("AMOUNT", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format("₹ %,.2f", kotlin.math.abs(activity.amount)),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = if (isPositive) Color(0xFF137333) else Color(0xFFC5221F)
                        )
                    }
                }

                Column {
                    Text("DATE & TIME", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(
                        text = formattedDate,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isDark) Color.LightGray else Color.DarkGray
                    )
                }

                Column {
                    Text("LEDGER ID", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(
                        text = "ID: ${activity.id} • ${if (activity.isBudget) "Budget" else "Goal"} Parent ID: ${activity.parentId}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) BrandPrimary else Color(0xFF1E2A7E)
                ),
                modifier = Modifier.fillMaxWidth().testTag("detail_dialog_close")
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun BudgetGoalActivityModifyDialog(
    activity: DbBudgetGoalActivity,
    isDark: Boolean,
    onConfirm: (String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(activity.title) }
    var amountStr by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", kotlin.math.abs(activity.amount))) }
    var errorText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Modify Transaction",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth().testTag("modify_dialog_title"),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "You are editing a ${if (activity.amount >= 0) "Top Up" else "Spend"} transaction. The ledger balances will adjust automatically.",
                    fontSize = 10.sp,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Description") },
                    modifier = Modifier.fillMaxWidth().testTag("modify_title_input")
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth().testTag("modify_amount_input")
                )

                if (errorText.isNotBlank()) {
                    Text(errorText, color = Color.Red, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).testTag("modify_dialog_cancel")
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val parsedAmount = amountStr.toDoubleOrNull()
                        if (title.isBlank()) {
                            errorText = "Title cannot be empty."
                        } else if (parsedAmount == null || parsedAmount <= 0.0) {
                            errorText = "Please enter a valid positive amount."
                        } else {
                            onConfirm(title, parsedAmount)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) BrandPrimary else Color(0xFF1E2A7E)
                    ),
                    modifier = Modifier.weight(1f).testTag("modify_dialog_save")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
fun SavingsVelocityChartSection(viewModel: AppViewModel) {
    val isDark = isDarkThemeActive
    val txns by viewModel.userTransactions.collectAsState()
    
    val monthData = remember(txns) {
        val monthFormatter = java.text.SimpleDateFormat("MMM", java.util.Locale.US)
        val last6Months = (0..5).map { offset ->
            val cal = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.MONTH, -offset)
            }
            val name = monthFormatter.format(cal.time).uppercase()
            val year = cal.get(java.util.Calendar.YEAR)
            val monthInt = cal.get(java.util.Calendar.MONTH)
            Triple(name, year, monthInt)
        }.reversed()

        last6Months.map { (name, year, monthInt) ->
            val currentMonthTxns = txns.filter { t ->
                val tCal = java.util.Calendar.getInstance().apply { timeInMillis = t.date }
                tCal.get(java.util.Calendar.YEAR) == year && tCal.get(java.util.Calendar.MONTH) == monthInt
            }
            val incomeSum = currentMonthTxns.filter { it.type.equals("Income", ignoreCase = true) }.sumOf { it.amount }
            val expenseSum = currentMonthTxns.filter { it.type.equals("Expense", ignoreCase = true) }.sumOf { it.amount }
            
            // Core savings metric = (Income - Expense), if negative we treat as 0
            val savings = (incomeSum - expenseSum).coerceAtLeast(0.0)
            name to savings
        }
    }

    val maxSavings = remember(monthData) { monthData.maxOf { it.second }.coerceAtLeast(100.0) }
    var selectedIndex by remember { mutableStateOf(-1) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) BrandSurface else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Savings Velocity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Monthly savings rate growth rate tracking",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
                
                // Show interactive hint / selection tooltip
                if (selectedIndex != -1) {
                    val sData = monthData[selectedIndex]
                    Text(
                        text = "${sData.first}: ₹${String.format(java.util.Locale.getDefault(), "%,.0f", sData.second)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = BrandPrimary,
                        modifier = Modifier
                            .background(
                                color = BrandPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                } else {
                    Text(
                        text = "Tap bars to inspect",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fixed aligned row with clean weight(1f) columns
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthData.forEachIndexed { index, (name, savings) ->
                        val scale = (savings / maxSavings).toFloat().coerceIn(0.12f, 1.0f)
                        val isSelected = index == selectedIndex
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedIndex = if (isSelected) -1 else index
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .fillMaxHeight(scale)
                                        .clip(RoundedCornerShape(tfr = 6.dp, tfl = 6.dp, bfl = 0.dp, bfr = 0.dp))
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = if (isSelected) {
                                                    listOf(Color(0xFFF472B6), Color(0xFFDB2777)) // Bright pink for active select
                                                } else if (savings == maxSavings && savings > 0.0) {
                                                    listOf(Color(0xFF34D399), Color(0xFF059669)) // Green for highest savings
                                                } else {
                                                    listOf(Color(0xFF93C5FD), Color(0xFF2563EB))
                                                }
                                            )
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isDark) Color.White else Color(0xFF0F172A),
                                            shape = RoundedCornerShape(tfr = 6.dp, tfl = 6.dp, bfl = 0.dp, bfr = 0.dp)
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = name,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BrandPrimary else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

// Visual layout helper for corner clips
private fun RoundedCornerShape(tfr: androidx.compose.ui.unit.Dp, tfl: androidx.compose.ui.unit.Dp, bfl: androidx.compose.ui.unit.Dp, bfr: androidx.compose.ui.unit.Dp) = 
    RoundedCornerShape(topStart = tfl, topEnd = tfr, bottomStart = bfl, bottomEnd = bfr)

@Composable
fun FinancialTipCard() {
    val isDark = isDarkThemeActive
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isDark) Color(0xFF1E264D) else Color(0xFFEFF6FF),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = if (isDark) Color(0xFF2E3D85) else Color(0xFFBFDBFE),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Tips",
                    tint = Color(0xFFEAB308),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Financial Tip",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isDark) Color.White else Color(0xFF1E3A8A)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "You're saving 15% more than last quarter. Redirecting your surplus to the 'Retirement Fund' could shave 2 years off your target date.",
                fontSize = 11.sp,
                color = if (isDark) Color.LightGray else Color(0xFF1E40AF)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = {},
                border = BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6) else Color(0xFF3B82F6)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp).testTag("optimize_strategy_button")
            ) {
                Text(
                    text = "Optimize My Strategy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color(0xFF3B82F6)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBudgetView(
    viewModel: AppViewModel,
    onCompleted: () -> Unit
) {
    var budgetName by remember { mutableStateOf("") }
    val isDark = isDarkThemeActive
    val context = LocalContext.current

    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Set Up a Budget Plan.",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = if (isDark) Color.White else Color(0xFF0F172A)
        )
        Text(
            text = "Set up a budget to categorize, top up, and track your expenditures freely without fixed limits.",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Input 1: Budget Name
        Text(
            text = "BUDGET NAME",
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = budgetName,
            onValueChange = { budgetName = it },
            placeholder = { Text("e.g. Bike Expense Budget", fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_budget_name"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = if (isDark) BrandOutline else Color(0xFFCBD5E1)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                showConfirm = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_create_budget"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) BrandPrimary else Color(0xFF000839)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Create Budget", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }

        GlobalConfirmationModal(
            show = showConfirm,
            title = "Are you sure?",
            message = "Do you want to confirm and register the new budget plan: '$budgetName'?",
            confirmButtonColor = Color(0xFF10B981),
            isDark = isDark,
            onConfirm = {
                showConfirm = false
                viewModel.createBudget(budgetName, 0.0) { success, msg ->
                    if (success) {
                        onCompleted()
                    } else {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onCancel = { showConfirm = false }
        )

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalView(
    viewModel: AppViewModel,
    onCompleted: () -> Unit
) {
    var goalName by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("") }
    var priorityLevel by remember { mutableStateOf("MEDIUM") } // LOW, MEDIUM, HIGH

    val isDark = isDarkThemeActive
    val context = LocalContext.current

    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Plant a financial seed.",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = if (isDark) Color.White else Color(0xFF0F172A)
        )
        Text(
            text = "Define your objective and watch your kinetic progress grow with precision.",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Input 1: Goal Name
        Text(
            text = "GOAL NAME",
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = goalName,
            onValueChange = { goalName = it },
            placeholder = { Text("e.g. Emergency Fund", fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_goal_name"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = if (isDark) BrandOutline else Color(0xFFCBD5E1)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Input 2: Target Goal Amount
        Text(
            text = "TARGET GOAL AMOUNT (₹)",
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = targetAmount,
            onValueChange = { targetAmount = it },
            placeholder = { Text("e.g. 50000", fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_goal_amount"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = if (isDark) BrandOutline else Color(0xFFCBD5E1)
            )
        )
        if (targetAmount.toDoubleOrNull() != null && targetAmount.toDouble() > 0) {
            Text(
                text = "GROWTH READY",
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = Color(0xFF10B981),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input 3: Target Date
        Text(
            text = "TARGET DATE",
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = targetDate,
            onValueChange = { targetDate = it },
            placeholder = { Text("e.g. Dec 2028 or dd-mm-yyyy", fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_goal_date"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = if (isDark) BrandOutline else Color(0xFFCBD5E1)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Priority Level
        Text(
            text = "PRIORITY LEVEL",
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("LOW", "MEDIUM", "HIGH").forEach { level ->
                val isSelected = priorityLevel == level
                val bgC = if (isSelected) (if (isDark) BrandPrimary else Color(0xFF000839)) else (if (isDark) BrandSurface else Color(0xFFF1F5F9))
                val borderC = if (isSelected) Color.Transparent else (if (isDark) BrandOutline else Color(0xFFE2E8F0))
                val textC = if (isSelected) Color.White else (if (isDark) Color.LightGray else Color(0xFF334155))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgC)
                        .border(1.dp, borderC, RoundedCornerShape(8.dp))
                        .clickable { priorityLevel = level }
                        .testTag("priority_btn_$level"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = level, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textC)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Dynamic Estimation Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (isDark) Color(0xFF1E2833) else Color(0xFFDCFCE7).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    1.dp,
                    if (isDark) BrandOutline else Color(0xFFBBF7D0),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DYNAMIC ESTIMATION",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color(0xFF15803D)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Based on your current transaction velocity, you are 12% more likely to reach this goal early if set to '$priorityLevel' priority.",
                    fontSize = 10.sp,
                    color = if (isDark) Color.LightGray else Color(0xFF22C55E)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                showConfirm = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_create_goal"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) BrandPrimary else Color(0xFF000839)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Set Goal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }

        GlobalConfirmationModal(
            show = showConfirm,
            title = "Are you sure?",
            message = "Do you want to confirm and register the new target goal: '$goalName' (target ₹$targetAmount)?",
            confirmButtonColor = Color(0xFF10B981),
            isDark = isDark,
            onConfirm = {
                showConfirm = false
                val amt = targetAmount.toDoubleOrNull() ?: 0.0
                viewModel.createGoal(goalName, amt, targetDate, priorityLevel) { success, msg ->
                    if (success) {
                        onCompleted()
                    } else {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onCancel = { showConfirm = false }
        )


        Spacer(modifier = Modifier.height(16.dp))

        // Auto-Save Plan Row Option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (isDark) BrandSurface else Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.OfflineBolt,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Auto-Save Plan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isDark) Color.White else Color(0xFF334155)
                    )
                    Text(
                        text = "Enable daily transactions round-ups",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopUpModal(
    viewModel: AppViewModel,
    parentId: Long,
    isBudget: Boolean,
    onDismiss: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val isDark = isDarkThemeActive
    val context = LocalContext.current

    // State Vars
    var selectedSource by remember { mutableStateOf("Income") } // "Income", "Other"
    var selectedCategory by remember { mutableStateOf("") }
    var selectedSubcategory by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }

    var showConfirm by remember { mutableStateOf(false) }

    // Dropdowns Expanded state
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var subcategoryMenuExpanded by remember { mutableStateOf(false) }

    // Filtered categories based on Source selection
    val filteredCategories = categories.filter {
        it.type.equals("Income", ignoreCase = true)
    }

    // Reset categories when source changes
    LaunchedEffect(selectedSource) {
        selectedCategory = ""
        selectedSubcategory = ""
    }

    // Get subcategories for selectedCategory
    val currentSubcategories = remember(selectedCategory, categories) {
        categories.firstOrNull { it.name == selectedCategory }?.getSubcategoryItems() ?: emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Deposit Funds",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Step 1: Fund Source Option Cards
                Text(
                    text = "STEP 1: SELECT SOURCE OF FUNDS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Income" to "Salary, bonuses, wages",
                        "Other" to "Gifts, refunds, external"
                    ).forEach { (src, desc) ->
                        val isSelected = selectedSource == src
                        Card(
                            onClick = { selectedSource = src },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)
                                } else {
                                    if (isDark) BrandSurface else Color.White
                                }
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) BrandPrimary else (if (isDark) BrandOutline else Color(0xFFE2E8F0))
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("source_card_$src")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedSource = src },
                                    colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = src,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isDark) Color.White else Color(0xFF334155)
                                    )
                                    Text(
                                        text = desc,
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Step 2: Category Selectors (Conditional)
                if (selectedSource == "Income") {
                    Text(
                        text = "STEP 2: ORIGIN CATEGORY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Dropdown Trigger
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { categoryMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("topup_cat_trigger"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedCategory.isBlank()) "Select Source Category" else selectedCategory,
                                    fontSize = 12.sp,
                                    color = if (isDark) Color.White else Color.Black
                                )
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            if (filteredCategories.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No categories available") },
                                    onClick = { categoryMenuExpanded = false }
                                )
                            } else {
                                filteredCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCategory = cat.name
                                            selectedSubcategory = ""
                                            categoryMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Subcategory Dropdown Trigger
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { subcategoryMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("topup_sub_trigger"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0)),
                            enabled = selectedCategory.isNotBlank()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedSubcategory.isBlank()) "Select Subcategory" else selectedSubcategory,
                                    fontSize = 12.sp,
                                    color = if (isDark) Color.White else Color.Black
                                )
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = subcategoryMenuExpanded,
                            onDismissRequest = { subcategoryMenuExpanded = false }
                        ) {
                            if (currentSubcategories.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("General List") },
                                    onClick = {
                                        selectedSubcategory = "General"
                                        subcategoryMenuExpanded = false
                                    }
                                )
                            } else {
                                currentSubcategories.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.name) },
                                        onClick = {
                                            selectedSubcategory = sub.name
                                            subcategoryMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Amount & Description fields
                Text(
                    text = "DEPOSIT AMOUNT (₹)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    placeholder = { Text("e.g. 500", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("topup_amount_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPrimary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "DESCRIPTION / MEMO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${descriptionText.length}/100",
                        fontSize = 9.sp,
                        color = if (descriptionText.length >= 100) Color.Red else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { if (it.length <= 100) descriptionText = it },
                    placeholder = { Text("Notes (Max 100 chars)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("topup_desc_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPrimary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    showConfirm = true
                },
                modifier = Modifier.testTag("topup_confirm_button"),
                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF000839)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm Deposit", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("topup_cancel_button")) {
                Text("Cancel", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = if (isDark) BrandSurface else Color.White
    )

    GlobalConfirmationModal(
        show = showConfirm,
        title = "Are you sure?",
        message = "Do you want to confirm depositing ₹${amountText} into this ledger?",
        confirmButtonColor = Color(0xFF10B981),
        isDark = isDark,
        onConfirm = {
            showConfirm = false
            val amount = amountText.toDoubleOrNull() ?: 0.0
            viewModel.topUpBudgetOrGoal(
                parentId = parentId,
                isBudget = isBudget,
                amount = amount,
                source = selectedSource,
                category = selectedCategory,
                subCategory = selectedSubcategory,
                description = descriptionText
            ) { success, msg ->
                if (success) {
                    onDismiss()
                } else {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        },
        onCancel = { showConfirm = false }
    )

}

@Composable
fun SpendModal(
    viewModel: AppViewModel,
    parentId: Long,
    isBudget: Boolean,
    onDismiss: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val budgets by viewModel.userBudgets.collectAsState()
    val isDark = isDarkThemeActive
    val context = LocalContext.current

    // State
    var merchantText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedSubcategory by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }

    var showConfirm by remember { mutableStateOf(false) }

    var subcategoryMenuExpanded by remember { mutableStateOf(false) }

    // Fetch parent Budget details to render "Available budget"
    val parentBudget = remember(budgets, parentId) {
        budgets.firstOrNull { it.id == parentId }
    }
    val availableBudget = if (parentBudget != null) {
        (parentBudget.limitAmount + parentBudget.savedAmount) - parentBudget.spentAmount
    } else {
        0.0
    }

    // Load available subcategories of matched budget category
    val budgetCategory = remember(categories, parentBudget) {
        categories.firstOrNull { it.name.equals(parentBudget?.name, ignoreCase = true) }
    }
    val budgetSubcategories = remember(budgetCategory) {
        budgetCategory?.getSubcategoryItems() ?: emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log Spend purchase",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (isBudget) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isDark) Color(0xFF1E264D) else Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("Available Budget: ₹ %,.2f", availableBudget),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isDark) Color.White else Color(0xFF1E40AF)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Input 1: Merchant / Title
                Text(
                    text = "MERCHANT / PURPOSE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    placeholder = { Text("e.g. Airline Ticket, Grocery shop", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("spend_merchant_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPrimary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Input 2: Amount
                Text(
                    text = "SPEND AMOUNT (₹)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    placeholder = { Text("e.g. 1500", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("spend_amount_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPrimary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Dropdown of budget subcategories
                if (isBudget && budgetCategory != null) {
                    var showNewSubInput by remember { mutableStateOf(false) }
                    var newSubName by remember { mutableStateOf("") }

                    Text(
                        text = "BUDGET SUBCATEGORY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    if (!showNewSubInput) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { subcategoryMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("spend_sub_trigger"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isDark) BrandOutline else Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedSubcategory.isBlank()) "Select Subcategory" else selectedSubcategory,
                                        fontSize = 12.sp,
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = subcategoryMenuExpanded,
                                onDismissRequest = { subcategoryMenuExpanded = false }
                            ) {
                                if (budgetSubcategories.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("General") },
                                        onClick = {
                                            selectedSubcategory = "General"
                                            subcategoryMenuExpanded = false
                                        }
                                    )
                                } else {
                                    budgetSubcategories.forEach { sub ->
                                        DropdownMenuItem(
                                            text = { Text(sub.name) },
                                            onClick = {
                                                selectedSubcategory = sub.name
                                                subcategoryMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        TextButton(
                            onClick = { showNewSubInput = true },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text("+ Create Subcategory", fontSize = 11.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newSubName,
                                onValueChange = { newSubName = it },
                                modifier = Modifier.weight(1.5f),
                                placeholder = { Text("New subcategory name", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPrimary)
                            )
                            Button(
                                onClick = {
                                    val nameTrimmed = newSubName.trim()
                                    if (nameTrimmed.isNotBlank()) {
                                        viewModel.addSubcategoryTo(budgetCategory, nameTrimmed)
                                        selectedSubcategory = nameTrimmed
                                        showNewSubInput = false
                                        newSubName = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Add", fontSize = 11.sp)
                            }
                            TextButton(onClick = { showNewSubInput = false }) {
                                Text("Cancel", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Notes (Max 100 character restriction)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "OPTIONAL NOTES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${descriptionText.length}/100",
                        fontSize = 9.sp,
                        color = if (descriptionText.length >= 100) Color.Red else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { if (it.length <= 100) descriptionText = it },
                    placeholder = { Text("Spend details (Max 100 chars)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("spend_desc_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPrimary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    showConfirm = true
                },
                modifier = Modifier.testTag("spend_confirm_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm Purchase", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("spend_cancel_button")) {
                Text("Cancel", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = if (isDark) BrandSurface else Color.White
    )

    GlobalConfirmationModal(
        show = showConfirm,
        title = "Are you sure?",
        message = "Do you want to confirm deducting ₹${amountText} from this ledger?",
        confirmButtonColor = Color(0xFFEF4444),
        isDark = isDark,
        onConfirm = {
            showConfirm = false
            val amount = amountText.toDoubleOrNull() ?: 0.0
            viewModel.spendFromBudgetOrGoal(
                parentId = parentId,
                isBudget = isBudget,
                amount = amount,
                merchant = merchantText,
                date = System.currentTimeMillis(),
                subcategory = selectedSubcategory,
                description = descriptionText
            ) { success, msg ->
                if (success) {
                    onDismiss()
                } else {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        },
        onCancel = { showConfirm = false }
    )

}
