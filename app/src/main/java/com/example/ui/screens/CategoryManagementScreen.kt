package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.DbCategory
import com.example.data.model.SubcategoryItem
import com.example.ui.components.UnifiedTopBar
import com.example.ui.components.UnifiedBottomNavBar
import com.example.ui.AppViewModel
import com.example.ui.theme.*

val defaultIconsList = listOf(
    "AccountBalance", "Payments", "Home", "DirectionsCar", "Restaurant",
    "ShoppingCart", "LocalMall", "MedicalServices", "School", "Movie",
    "Flight", "Bolt", "WaterDrop", "Call", "Wifi",
    "Spa", "FitnessCenter", "CardMembership", "Redeem", "Work",
    "Build", "Category"
)

fun getIconByName(iconName: String): ImageVector {
    return when (iconName) {
        "AccountBalance" -> Icons.Default.AccountBalance
        "Payments" -> Icons.Default.Payments
        "Home" -> Icons.Default.Home
        "DirectionsCar" -> Icons.Default.DirectionsCar
        "Restaurant" -> Icons.Default.Restaurant
        "ShoppingCart" -> Icons.Default.ShoppingCart
        "LocalMall" -> Icons.Default.LocalMall
        "MedicalServices" -> Icons.Default.MedicalServices
        "School" -> Icons.Default.School
        "Movie" -> Icons.Default.Movie
        "Flight" -> Icons.Default.Flight
        "Bolt" -> Icons.Default.Bolt
        "WaterDrop" -> Icons.Default.WaterDrop
        "Call" -> Icons.Default.Call
        "Wifi" -> Icons.Default.Wifi
        "Spa" -> Icons.Default.Spa
        "FitnessCenter" -> Icons.Default.FitnessCenter
        "CardMembership" -> Icons.Default.CardMembership
        "Redeem" -> Icons.Default.Redeem
        "Work" -> Icons.Default.Work
        "Build" -> Icons.Default.Build
        else -> Icons.Default.Category
    }
}

@Composable
fun getCategoryIconAndColors(categoryName: String, categoryIconName: String = ""): Triple<ImageVector, Color, Color> {
    val isDark = isDarkThemeActive
    val icon = if (categoryIconName.isNotBlank() && categoryIconName != "Category") {
        getIconByName(categoryIconName)
    } else {
        when (categoryName.lowercase().trim()) {
            "salary & wages" -> Icons.Default.Work
            "investments" -> Icons.Default.AccountBalance
            "housing" -> Icons.Default.Home
            "transportation" -> Icons.Default.DirectionsCar
            "food & dining" -> Icons.Default.Restaurant
            else -> Icons.Default.Category
        }
    }

    val colors = when (categoryName.lowercase().trim()) {
        "salary & wages" -> Pair(if (isDark) Color(0xFF0F3E22) else Color(0xFFDCFCE7), if (isDark) Color(0xFF4ADE80) else Color(0xFF10B981))
        "investments" -> Pair(if (isDark) Color(0xFF1E293B) else Color(0xFFDBEAFE), if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB))
        "housing" -> Pair(if (isDark) Color(0xFF4C1D15) else Color(0xFFFEE2E2), if (isDark) Color(0xFFFCA5A5) else Color(0xFFEF4444))
        "transportation" -> Pair(if (isDark) Color(0xFF451A03) else Color(0xFFFFEDD5), if (isDark) Color(0xFFFCE89C) else Color(0xFFD97706))
        "food & dining" -> Pair(if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE), if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
        else -> Pair(if (isDark) BrandSurfaceContainerLow else Color(0xFFF1F5F9), if (isDark) BrandPrimary else Color(0xFF64748B))
    }

    return Triple(icon, colors.first, colors.second)
}

@Composable
fun CategoryIconView(
    iconName: String,
    customImageUri: String,
    badgeColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(badgeColor),
        contentAlignment = Alignment.Center
    ) {
        if (customImageUri.isNotBlank()) {
            val painter = rememberAsyncImagePainter(model = customImageUri)
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = getIconByName(iconName.ifBlank { "Category" }),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScreen: (String) -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val transactions by viewModel.userTransactions.collectAsState()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showEditCategoryDialog by remember { mutableStateOf<DbCategory?>(null) }
    var categoryToDelete by remember { mutableStateOf<DbCategory?>(null) }
    
    var showAddSubcategoryDialog by remember { mutableStateOf<DbCategory?>(null) }
    var showEditSubcategoryDialog by remember { mutableStateOf<Pair<DbCategory, SubcategoryItem>?>(null) }
    var subcategoryToDelete by remember { mutableStateOf<Pair<DbCategory, SubcategoryItem>?>(null) }

    // Forms states
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryType by remember { mutableStateOf("Expense") } // "Income", "Expense", "Asset & Investment"
    var newCategoryDesc by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("Category") }
    var newCategoryUri by remember { mutableStateOf("") }

    var newSubcategoryName by remember { mutableStateOf("") }
    var newSubcategoryDesc by remember { mutableStateOf("") }
    var newSubcategoryIcon by remember { mutableStateOf("Category") }
    var newSubcategoryUri by remember { mutableStateOf("") }

    val screenBg = if (isDarkThemeActive) BrandBackground else Color(0xFFF3F8FC)

    Scaffold(
        containerColor = screenBg
    ) { innerPadding ->
        
        val incomeCategories = remember(categories) {
            categories.filter { it.type == "Income" }
        }
        val expenseCategories = remember(categories) {
            categories.filter { it.type == "Expense" }
        }
        val assetInvestmentCategories = remember(categories) {
            categories.filter { it.type == "Asset & Investment" }
        }
        val budgetCategories = remember(categories) {
            categories.filter { it.type == "Budget" }
        }

        var showMoveTxDialog by remember { mutableStateOf(false) }
        var selectedGroup by remember { mutableStateOf("Expense") } // "Income", "Expense", "Investment", "Budget"

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---- PART 1: 2x2 GRID FORMAT SELECTORS ----
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val groups = listOf(
                        Triple("Income", Icons.Default.Payments, Color(0xFF10B981)),
                        Triple("Expense", Icons.Default.ShoppingCart, Color(0xFFEF4444)),
                        Triple("Investment", Icons.Default.TrendingUp, Color(0xFF2563EB)),
                        Triple("Budget", Icons.Default.TrackChanges, Color(0xFFEC4899))
                    )
                    
                    // Row 1: Income & Expense
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groups.take(2).forEach { (name, icon, color) ->
                            val isSelected = selectedGroup == name
                            val colCount = when (name) {
                                "Income" -> incomeCategories.size
                                "Expense" -> expenseCategories.size
                                else -> 0
                            }
                            val isDark = isDarkThemeActive
                            val bgCol = if (isSelected) color.copy(alpha = 0.15f) else (if (isDark) BrandSurface else Color.White)
                            val textCol = if (isSelected) color else (if (isDark) Color.White else Color(0xFF0F1B6B))
                            val borderCol = if (isSelected) color else (if (isDark) BrandOutline else Color(0xFFE2E8F0))
                            
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .clickable { selectedGroup = name },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = bgCol),
                                border = BorderStroke(1.2.dp, borderCol)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(color.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                                    }
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text(
                                            text = name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textCol
                                        )
                                        Text(
                                            text = "$colCount Categories",
                                            fontSize = 11.sp,
                                            color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Row 2: Investment & Budget
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groups.drop(2).forEach { (name, icon, color) ->
                            val isSelected = selectedGroup == name
                            val colCount = when (name) {
                                "Investment" -> assetInvestmentCategories.size
                                "Budget" -> budgetCategories.size
                                else -> 0
                            }
                            val isDark = isDarkThemeActive
                            val bgCol = if (isSelected) color.copy(alpha = 0.15f) else (if (isDark) BrandSurface else Color.White)
                            val textCol = if (isSelected) color else (if (isDark) Color.White else Color(0xFF0F1B6B))
                            val borderCol = if (isSelected) color else (if (isDark) BrandOutline else Color(0xFFE2E8F0))
                            
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .clickable { selectedGroup = name },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = bgCol),
                                border = BorderStroke(1.2.dp, borderCol)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(color.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                                    }
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text(
                                            text = name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textCol
                                        )
                                        Text(
                                            text = "$colCount Categories",
                                            fontSize = 11.sp,
                                            color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---- PART 2: MOVE TRANSACTION / RE-CATEGORIZE CARD ----
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMoveTxDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkThemeActive) BrandSurface else Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isDarkThemeActive) BrandOutline else Color(0xFFE2E8F0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDBEAFE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompareArrows,
                                contentDescription = "Move icon",
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Move or Re-categorize Transactions",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkThemeActive) Color.White else Color.Black
                            )
                            Text(
                                text = "Safely transfer entries from one Category / Subcategory to another",
                                fontSize = 10.5.sp,
                                color = Color.Gray
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Go",
                            modifier = Modifier.size(18.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }

            // ---- PART 3: DYNAMIC GROUP CATEGORIES & SUBCATEGORIES LIST ----
            if (selectedGroup == "Income") {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Income Categories",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (isDarkThemeActive) Color.White else Color(0xFF0F2D59)
                            )
                        }
                        
                        Button(
                            onClick = {
                                newCategoryType = "Income"
                                newCategoryName = ""
                                newCategoryDesc = ""
                                newCategoryIcon = "Category"
                                newCategoryUri = ""
                                showAddCategoryDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkThemeActive) BrandPrimary else Color(0xFF000839),
                                contentColor = if (isDarkThemeActive) Color.Black else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Category",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (incomeCategories.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No income categories created.",
                                color = if (isDarkThemeActive) BrandOnSurfaceVariant else Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(incomeCategories) { category ->
                        CategoryTaxonomyCard(
                            category = category,
                            viewModel = viewModel,
                            onAddSubcategory = {
                                newSubcategoryName = ""
                                newSubcategoryDesc = ""
                                newSubcategoryIcon = "Category"
                                newSubcategoryUri = ""
                                showAddSubcategoryDialog = category
                            },
                            onEditCategory = {
                                showEditCategoryDialog = category
                            },
                            onEditSubcategory = { sub ->
                                showEditSubcategoryDialog = Pair(category, sub)
                            },
                            onDeleteCategory = {
                                categoryToDelete = category
                            },
                            onDeleteSubcategory = { sub ->
                                subcategoryToDelete = Pair(category, sub)
                            }
                        )
                    }
                }
            } else if (selectedGroup == "Expense") {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Expense Categories",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (isDarkThemeActive) Color.White else Color(0xFF0F2D59)
                            )
                        }
                        
                        Button(
                            onClick = {
                                newCategoryType = "Expense"
                                newCategoryName = ""
                                newCategoryDesc = ""
                                newCategoryIcon = "Category"
                                newCategoryUri = ""
                                showAddCategoryDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkThemeActive) BrandPrimary else Color(0xFF000839),
                                contentColor = if (isDarkThemeActive) Color.Black else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Category",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (expenseCategories.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No expense categories created.",
                                color = if (isDarkThemeActive) BrandOnSurfaceVariant else Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(expenseCategories) { category ->
                        CategoryTaxonomyCard(
                            category = category,
                            viewModel = viewModel,
                            onAddSubcategory = {
                                newSubcategoryName = ""
                                newSubcategoryDesc = ""
                                newSubcategoryIcon = "Category"
                                newSubcategoryUri = ""
                                showAddSubcategoryDialog = category
                            },
                            onEditCategory = {
                                showEditCategoryDialog = category
                            },
                            onEditSubcategory = { sub ->
                                showEditSubcategoryDialog = Pair(category, sub)
                            },
                            onDeleteCategory = {
                                categoryToDelete = category
                            },
                            onDeleteSubcategory = { sub ->
                                subcategoryToDelete = Pair(category, sub)
                            }
                        )
                    }
                }
            } else if (selectedGroup == "Investment") {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Investment Categories",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (isDarkThemeActive) Color.White else Color(0xFF0F2D59)
                            )
                        }
                        
                        Button(
                            onClick = {
                                newCategoryType = "Asset & Investment"
                                newCategoryName = ""
                                newCategoryDesc = ""
                                newCategoryIcon = "Category"
                                newCategoryUri = ""
                                showAddCategoryDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkThemeActive) BrandPrimary else Color(0xFF000839),
                                contentColor = if (isDarkThemeActive) Color.Black else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Category",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (assetInvestmentCategories.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No Asset & Investment categories created.",
                                color = if (isDarkThemeActive) BrandOnSurfaceVariant else Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(assetInvestmentCategories) { category ->
                        CategoryTaxonomyCard(
                            category = category,
                            viewModel = viewModel,
                            onAddSubcategory = {
                                newSubcategoryName = ""
                                newSubcategoryDesc = ""
                                newSubcategoryIcon = "Category"
                                newSubcategoryUri = ""
                                showAddSubcategoryDialog = category
                            },
                            onEditCategory = {
                                showEditCategoryDialog = category
                            },
                            onEditSubcategory = { sub ->
                                showEditSubcategoryDialog = Pair(category, sub)
                            },
                            onDeleteCategory = {
                                categoryToDelete = category
                            },
                            onDeleteSubcategory = { sub ->
                                subcategoryToDelete = Pair(category, sub)
                            }
                        )
                    }
                }
            } else if (selectedGroup == "Budget") {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrackChanges,
                                contentDescription = null,
                                tint = if (isDarkThemeActive) BrandPrimary else Color(0xFFC084FC),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Budget Categories",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (isDarkThemeActive) Color.White else Color(0xFF0F2D59)
                            )
                        }
                    }
                }

                if (budgetCategories.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active Budgets linked as custom categories.",
                                color = if (isDarkThemeActive) BrandOnSurfaceVariant else Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(budgetCategories) { category ->
                        CategoryTaxonomyCard(
                            category = category,
                            viewModel = viewModel,
                            onAddSubcategory = {
                                newSubcategoryName = ""
                                newSubcategoryDesc = ""
                                newSubcategoryIcon = "Category"
                                newSubcategoryUri = ""
                                showAddSubcategoryDialog = category
                            },
                            onEditCategory = {
                                showEditCategoryDialog = category
                            },
                            onEditSubcategory = { sub ->
                                showEditSubcategoryDialog = Pair(category, sub)
                            },
                            onDeleteCategory = {
                                categoryToDelete = category
                            },
                            onDeleteSubcategory = { sub ->
                                subcategoryToDelete = Pair(category, sub)
                            }
                        )
                    }
                }
            }
        }

        // ---- MOVING/RE-CATEGORIZE TRANSACTIONS DIALOG ----
        if (showMoveTxDialog) {
            var sourceCatSelected by remember { mutableStateOf("") }
            var sourceSubSelected by remember { mutableStateOf("All Subcategories") }
            var targetCatSelected by remember { mutableStateOf("") }
            var targetSubSelected by remember { mutableStateOf("General") }

            var srcCatExpanded by remember { mutableStateOf(false) }
            var srcSubExpanded by remember { mutableStateOf(false) }
            var tgtCatExpanded by remember { mutableStateOf(false) }
            var tgtSubExpanded by remember { mutableStateOf(false) }

            val context = androidx.compose.ui.platform.LocalContext.current

            val sourceCategoryObj = categories.find { it.name.equals(sourceCatSelected, ignoreCase = true) }
            val sourceSubcategories = remember(sourceCategoryObj) {
                val list = mutableListOf("All Subcategories", "General")
                if (sourceCategoryObj != null) {
                    list.addAll(sourceCategoryObj.getSubcategoryItems().map { it.name })
                }
                list.distinct()
            }

            val targetCategoryObj = categories.find { it.name.equals(targetCatSelected, ignoreCase = true) }
            val targetSubcategories = remember(targetCategoryObj) {
                val list = mutableListOf("General")
                if (targetCategoryObj != null) {
                    list.addAll(targetCategoryObj.getSubcategoryItems().map { it.name })
                }
                list.distinct()
            }

            AlertDialog(
                onDismissRequest = { showMoveTxDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CompareArrows, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Move Ledger Transactions",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isDarkThemeActive) Color.White else Color(0xFF0F2D59)
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Re-categorize and safely transfer all transactions from a Source Category/Subcategory to an existing Target Category/Subcategory.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        // FROM (SOURCE) CATEGORY
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("From (Source) Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDarkThemeActive) Color.White else Color.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDarkThemeActive) Color(0xFF1E293B) else Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isDarkThemeActive) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                    .clickable { srcCatExpanded = true }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (sourceCatSelected.isBlank()) "Select Category" else sourceCatSelected,
                                        color = if (sourceCatSelected.isBlank()) Color.Gray else (if (isDarkThemeActive) Color.White else Color.Black),
                                        fontSize = 13.sp
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                                }
                                DropdownMenu(
                                    expanded = srcCatExpanded,
                                    onDismissRequest = { srcCatExpanded = false }
                                ) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text("${cat.name} (${cat.type})", fontSize = 12.sp) },
                                            onClick = {
                                                sourceCatSelected = cat.name
                                                sourceSubSelected = "All Subcategories"
                                                srcCatExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // FROM (SOURCE) SUBCATEGORY
                        if (sourceCatSelected.isNotBlank()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("From (Source) Subcategory", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDarkThemeActive) Color.White else Color.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isDarkThemeActive) Color(0xFF1E293B) else Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                        .border(1.dp, if (isDarkThemeActive) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                        .clickable { srcSubExpanded = true }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sourceSubSelected,
                                            color = if (isDarkThemeActive) Color.White else Color.Black,
                                            fontSize = 13.sp
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                                    }
                                    DropdownMenu(
                                        expanded = srcSubExpanded,
                                        onDismissRequest = { srcSubExpanded = false }
                                    ) {
                                        sourceSubcategories.forEach { sub ->
                                            DropdownMenuItem(
                                                text = { Text(sub, fontSize = 12.sp) },
                                                onClick = {
                                                    sourceSubSelected = sub
                                                    srcSubExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // TO (TARGET) CATEGORY
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("To (Target) Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDarkThemeActive) Color.White else Color.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDarkThemeActive) Color(0xFF1E293B) else Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isDarkThemeActive) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                    .clickable { tgtCatExpanded = true }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (targetCatSelected.isBlank()) "Select Category" else targetCatSelected,
                                        color = if (targetCatSelected.isBlank()) Color.Gray else (if (isDarkThemeActive) Color.White else Color.Black),
                                        fontSize = 13.sp
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                                }
                                DropdownMenu(
                                    expanded = tgtCatExpanded,
                                    onDismissRequest = { tgtCatExpanded = false }
                                ) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text("${cat.name} (${cat.type})", fontSize = 12.sp) },
                                            onClick = {
                                                targetCatSelected = cat.name
                                                targetSubSelected = "General"
                                                tgtCatExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // TO (TARGET) SUBCATEGORY
                        if (targetCatSelected.isNotBlank()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("To (Target) Subcategory", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDarkThemeActive) Color.White else Color.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isDarkThemeActive) Color(0xFF1E293B) else Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                        .border(1.dp, if (isDarkThemeActive) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                        .clickable { tgtSubExpanded = true }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = targetSubSelected,
                                            color = if (isDarkThemeActive) Color.White else Color.Black,
                                            fontSize = 13.sp
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                                    }
                                    DropdownMenu(
                                        expanded = tgtSubExpanded,
                                        onDismissRequest = { tgtSubExpanded = false }
                                    ) {
                                        targetSubcategories.forEach { sub ->
                                            DropdownMenuItem(
                                                text = { Text(sub, fontSize = 12.sp) },
                                                onClick = {
                                                    targetSubSelected = sub
                                                    tgtSubExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (sourceCatSelected.isBlank() || targetCatSelected.isBlank()) {
                                android.widget.Toast.makeText(context, "Please select both source and target categories.", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val realSourceSub = if (sourceSubSelected == "All Subcategories") null else sourceSubSelected
                            viewModel.recategorizeTransactions(
                                sourceCategory = sourceCatSelected,
                                sourceSubcategory = realSourceSub,
                                targetCategory = targetCatSelected,
                                targetSubcategory = targetSubSelected
                            ) { success, msg ->
                                if (success) {
                                    android.widget.Toast.makeText(context, "Moved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                    showMoveTxDialog = false
                                } else {
                                    android.widget.Toast.makeText(context, "Error: $msg", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = if (isDarkThemeActive) Color.Black else Color.White)
                    ) {
                        Text("Move & Re-categorize", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMoveTxDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }

    // SECURE DELETE SUBCATEGORY VERIFICATION DIALOG
    if (subcategoryToDelete != null) {
        val (parentCat, targetSub) = subcategoryToDelete!!
        val context = androidx.compose.ui.platform.LocalContext.current
        val authPrefs = remember {
            context.getSharedPreferences("kinetic_auth_prefs", android.content.Context.MODE_PRIVATE)
        }
        val expectedPass = remember(currentUser) {
            authPrefs.getString("password_${currentUser?.userId ?: ""}", "password") ?: "password"
        }

        // CAPTCHA Setup
        var num1 by remember { mutableStateOf((2..9).random()) }
        var num2 by remember { mutableStateOf((2..9).random()) }
        val expectedAnswer = remember(num1, num2) { num1 + num2 }
        
        var enteredPassword by remember { mutableStateOf("") }
        var enteredCaptcha by remember { mutableStateOf("") }
        var passVisibility by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { subcategoryToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = BrandError, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Subcategory Deletion Clearances",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDarkThemeActive) Color.White else Color(0xFF0F2D59)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "You are deleting the subcategory '${targetSub.name}' from target category '${parentCat.name}'. This action is destructive. Enter credentials to confirm:",
                        fontSize = 13.sp,
                        color = if (isDarkThemeActive) BrandOnSurfaceVariant else Color(0xFF475569)
                    )

                    // Password input field
                    OutlinedTextField(
                        value = enteredPassword,
                        onValueChange = { enteredPassword = it },
                        label = { Text("Account Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passVisibility) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passVisibility = !passVisibility }) {
                                Icon(
                                    imageVector = if (passVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password view"
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandError
                        )
                    )

                    // CAPTCHA display & input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isDarkThemeActive) Color(0xFF1E293B) else Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                .border(1.dp, if (isDarkThemeActive) Color(0xFF334155) else Color(0xFFBFDBFE), RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Solve: $num1 + $num2 = ?",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDarkThemeActive) BrandPrimary else Color(0xFF1E40AF)
                            )
                        }

                        IconButton(onClick = {
                            num1 = (2..9).random()
                            num2 = (2..9).random()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate math query")
                        }
                    }

                    OutlinedTextField(
                        value = enteredCaptcha,
                        onValueChange = { enteredCaptcha = it },
                        label = { Text("Enter Captcha Answer") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandError
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hasAssociated = transactions.any {
                            it.category.equals(parentCat.name, ignoreCase = true) &&
                            it.subCategory.equals(targetSub.name, ignoreCase = true)
                        }
                        if (hasAssociated) {
                            android.widget.Toast.makeText(context, "Cannot delete subcategory because it has associated transactions. Please re-categorize or delete those transactions first.", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (enteredPassword.trim() != expectedPass) {
                            android.widget.Toast.makeText(context, "Incorrect Password verification!", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (enteredCaptcha.trim().toIntOrNull() != expectedAnswer) {
                            android.widget.Toast.makeText(context, "Incorrect mathematical Captcha answer!", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val currentItems = parentCat.getSubcategoryItems().toMutableList()
                        currentItems.removeAll { it.name == targetSub.name }
                        val updated = parentCat.copy(
                            subcategoriesJson = DbCategory.serializeSubcategories(currentItems)
                        )
                        viewModel.updateCategory(updated)
                        android.widget.Toast.makeText(context, "Subcategory deleted successfully.", android.widget.Toast.LENGTH_SHORT).show()
                        subcategoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandError),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Verify & Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { subcategoryToDelete = null }) {
                    Text("Cancel", color = if (isDarkThemeActive) Color.LightGray else Color.Gray)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = if (isDarkThemeActive) BrandSurface else Color.White
        )
    }

    // SECURE DELETE CATEGORY VERIFICATION DIALOG
    if (categoryToDelete != null) {
        val targetCat = categoryToDelete!!
        val context = androidx.compose.ui.platform.LocalContext.current
        val authPrefs = remember {
            context.getSharedPreferences("kinetic_auth_prefs", android.content.Context.MODE_PRIVATE)
        }
        val expectedPass = remember(currentUser) {
            authPrefs.getString("password_${currentUser?.userId ?: ""}", "password") ?: "password"
        }

        // CAPTCHA Setup
        var num1 by remember { mutableStateOf((2..9).random()) }
        var num2 by remember { mutableStateOf((2..9).random()) }
        val expectedAnswer = remember(num1, num2) { num1 + num2 }
        
        var enteredPassword by remember { mutableStateOf("") }
        var enteredCaptcha by remember { mutableStateOf("") }
        var passVisibility by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = BrandError, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Category Deletion Clearances",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDarkThemeActive) Color.White else Color(0xFF0F2D59)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "You are deleting the category '${targetCat.name}'. This action is destructive and might affect transactions linked to this category. Enter credentials to confirm:",
                        fontSize = 13.sp,
                        color = if (isDarkThemeActive) BrandOnSurfaceVariant else Color(0xFF475569)
                    )

                    // Password input field
                    OutlinedTextField(
                        value = enteredPassword,
                        onValueChange = { enteredPassword = it },
                        label = { Text("Account Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passVisibility) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passVisibility = !passVisibility }) {
                                Icon(
                                    imageVector = if (passVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password view"
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandError
                        )
                    )

                    // CAPTCHA display & input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isDarkThemeActive) Color(0xFF1E293B) else Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                .border(1.dp, if (isDarkThemeActive) Color(0xFF334155) else Color(0xFFBFDBFE), RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Solve: $num1 + $num2 = ?",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDarkThemeActive) BrandPrimary else Color(0xFF1E40AF)
                            )
                        }

                        IconButton(onClick = {
                            num1 = (2..9).random()
                            num2 = (2..9).random()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate math query")
                        }
                    }

                    OutlinedTextField(
                        value = enteredCaptcha,
                        onValueChange = { enteredCaptcha = it },
                        label = { Text("Enter Captcha Answer") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandError
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hasAssociated = transactions.any {
                            it.category.equals(targetCat.name, ignoreCase = true)
                        }
                        if (hasAssociated) {
                            android.widget.Toast.makeText(context, "Cannot delete category because it has associated transactions. Please re-categorize or delete those transactions first.", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (enteredPassword.trim() != expectedPass) {
                            android.widget.Toast.makeText(context, "Incorrect Password verification!", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (enteredCaptcha.trim().toIntOrNull() != expectedAnswer) {
                            android.widget.Toast.makeText(context, "Incorrect mathematical Captcha answer!", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.deleteCategory(targetCat)
                        android.widget.Toast.makeText(context, "Category deleted successfully.", android.widget.Toast.LENGTH_SHORT).show()
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandError),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Verify & Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel", color = if (isDarkThemeActive) Color.LightGray else Color.Gray)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = if (isDarkThemeActive) BrandSurface else Color.White
        )
    }

    // CREATE CATEGORY DIALOG
    if (showAddCategoryDialog) {
        val imageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                newCategoryUri = uri.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            val lower = newCategoryName.trim().lowercase()
                            if (lower == "budget & goal" || lower == "budget") {
                                android.widget.Toast.makeText(viewModel.getApplication(), "Budget & Goal is a system-defined special category and cannot be manually created.", android.widget.Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            viewModel.addNewCategory(
                                name = newCategoryName.trim(),
                                type = newCategoryType,
                                description = newCategoryDesc.trim(),
                                iconName = newCategoryIcon,
                                customImageUri = newCategoryUri
                            )
                            showAddCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor = if (isDarkThemeActive) Color.Black else Color.White
                    )
                ) {
                    Text("Add Group", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel", color = BrandOnSurfaceVariant)
                }
            },
            title = { 
                Text(
                    text = "Create Major Category Group", 
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CategoryMiniInputField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = "Group Name (e.g. Subscriptions)"
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = newCategoryType == "Expense",
                            onClick = { newCategoryType = "Expense" },
                            label = { Text("Expense", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = newCategoryType == "Income",
                            onClick = { newCategoryType = "Income" },
                            label = { Text("Income", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = newCategoryType == "Asset & Investment",
                            onClick = { newCategoryType = "Asset & Investment" },
                            label = { Text("Asset & Inv", fontSize = 11.sp) }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        CategoryMiniInputField(
                            value = newCategoryDesc,
                            onValueChange = { if (it.length <= 100) newCategoryDesc = it },
                            placeholder = "Brief detail/description (under 100 chars)"
                        )
                        Text(
                            text = "${newCategoryDesc.length}/100 characters",
                            fontSize = 11.sp,
                            color = BrandOnSurfaceVariant,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    // Grid Preset Picker
                    PresetIconsGridPicker(
                        selectedIconName = newCategoryIcon,
                        onSelectIconName = { newCategoryIcon = it }
                    )

                    // Image selection (Uri)
                    CustomImagePickerRow(
                        imageUri = newCategoryUri,
                        onSelectImage = { imageLauncher.launch("image/*") },
                        onClearImage = { newCategoryUri = "" }
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // EDIT CATEGORY DIALOG (Modify Major Category)
    if (showEditCategoryDialog != null) {
        val editingCat = showEditCategoryDialog!!
        
        var editName by remember { mutableStateOf(editingCat.name) }
        var editType by remember { mutableStateOf(editingCat.type) }
        var editDesc by remember { mutableStateOf(editingCat.description) }
        var editIcon by remember { mutableStateOf(editingCat.iconName) }
        var editUri by remember { mutableStateOf(editingCat.customImageUri) }

        val imageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                editUri = uri.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showEditCategoryDialog = null },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            val lower = editName.trim().lowercase()
                            if (lower == "budget & goal" || lower == "budget") {
                                android.widget.Toast.makeText(viewModel.getApplication(), "Budget & Goal is a system-defined special category status and cannot be reassigned.", android.widget.Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            val updated = editingCat.copy(
                                name = editName.trim(),
                                type = editType,
                                description = editDesc.trim(),
                                iconName = editIcon,
                                customImageUri = editUri
                            )
                            viewModel.updateCategory(updated)
                            showEditCategoryDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor = if (isDarkThemeActive) Color.Black else Color.White
                    )
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCategoryDialog = null }) {
                    Text("Cancel", color = BrandOnSurfaceVariant)
                }
            },
            title = { 
                Text(
                    text = "Edit Category Group", 
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CategoryMiniInputField(
                        value = editName,
                        onValueChange = { editName = it },
                        placeholder = "Group Name"
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = editType == "Expense",
                            onClick = { editType = "Expense" },
                            label = { Text("Expense Type") }
                        )
                        FilterChip(
                            selected = editType == "Income",
                            onClick = { editType = "Income" },
                            label = { Text("Income Type") }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        CategoryMiniInputField(
                            value = editDesc,
                            onValueChange = { if (it.length <= 100) editDesc = it },
                            placeholder = "Brief detail/description (under 100 chars)"
                        )
                        Text(
                            text = "${editDesc.length}/100 characters",
                            fontSize = 11.sp,
                            color = BrandOnSurfaceVariant,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    PresetIconsGridPicker(
                        selectedIconName = editIcon,
                        onSelectIconName = { editIcon = it }
                    )

                    CustomImagePickerRow(
                        imageUri = editUri,
                        onSelectImage = { imageLauncher.launch("image/*") },
                        onClearImage = { editUri = "" }
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ADD SUBCATEGORY DIALOG
    if (showAddSubcategoryDialog != null) {
        val targetCategory = showAddSubcategoryDialog!!
        
        val imageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                newSubcategoryUri = uri.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showAddSubcategoryDialog = null },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubcategoryName.isNotBlank()) {
                            val currentItems = targetCategory.getSubcategoryItems().toMutableList()
                            if (currentItems.none { it.name.lowercase() == newSubcategoryName.trim().lowercase() }) {
                                currentItems.add(
                                    SubcategoryItem(
                                        name = newSubcategoryName.trim(),
                                        description = newSubcategoryDesc.trim(),
                                        iconName = newSubcategoryIcon,
                                        customImageUri = newSubcategoryUri
                                    )
                                )
                                val updated = targetCategory.copy(
                                    subcategoriesJson = DbCategory.serializeSubcategories(currentItems)
                                )
                                viewModel.updateCategory(updated)
                            }
                            showAddSubcategoryDialog = null
                            newSubcategoryName = ""
                            newSubcategoryDesc = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor = if (isDarkThemeActive) Color.Black else Color.White
                    )
                ) {
                    Text("Append Subcategory", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubcategoryDialog = null }) {
                    Text("Cancel", color = BrandOnSurfaceVariant)
                }
            },
            title = { 
                Text(
                    text = "Save Subcategory to ${targetCategory.name}", 
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CategoryMiniInputField(
                        value = newSubcategoryName,
                        onValueChange = { newSubcategoryName = it },
                        placeholder = "Subcategory Name (e.g. Restaurants)"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        CategoryMiniInputField(
                            value = newSubcategoryDesc,
                            onValueChange = { if (it.length <= 100) newSubcategoryDesc = it },
                            placeholder = "Brief detail/description (under 100 chars)"
                        )
                        Text(
                            text = "${newSubcategoryDesc.length}/100 characters",
                            fontSize = 11.sp,
                            color = BrandOnSurfaceVariant,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    PresetIconsGridPicker(
                        selectedIconName = newSubcategoryIcon,
                        onSelectIconName = { newSubcategoryIcon = it }
                    )

                    CustomImagePickerRow(
                        imageUri = newSubcategoryUri,
                        onSelectImage = { imageLauncher.launch("image/*") },
                        onClearImage = { newSubcategoryUri = "" }
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // EDIT SUBCATEGORY DIALOG
    if (showEditSubcategoryDialog != null) {
        val (parentCategory, subcatItem) = showEditSubcategoryDialog!!

        var editSubName by remember { mutableStateOf(subcatItem.name) }
        var editSubDesc by remember { mutableStateOf(subcatItem.description) }
        var editSubIcon by remember { mutableStateOf(subcatItem.iconName) }
        var editSubUri by remember { mutableStateOf(subcatItem.customImageUri) }

        val imageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                editSubUri = uri.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showEditSubcategoryDialog = null },
            confirmButton = {
                Button(
                    onClick = {
                        if (editSubName.isNotBlank()) {
                            val currentItems = parentCategory.getSubcategoryItems().toMutableList()
                            val idx = currentItems.indexOfFirst { it.name == subcatItem.name }
                            if (idx != -1) {
                                currentItems[idx] = SubcategoryItem(
                                    name = editSubName.trim(),
                                    description = editSubDesc.trim(),
                                    iconName = editSubIcon,
                                    customImageUri = editSubUri
                                )
                                val updated = parentCategory.copy(
                                    subcategoriesJson = DbCategory.serializeSubcategories(currentItems)
                                )
                                viewModel.updateCategory(updated)
                            }
                            showEditSubcategoryDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor = if (isDarkThemeActive) Color.Black else Color.White
                    )
                ) {
                    Text("Apply Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditSubcategoryDialog = null }) {
                    Text("Cancel", color = BrandOnSurfaceVariant)
                }
            },
            title = { 
                Text(
                    text = "Edit Subcategory in ${parentCategory.name}", 
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CategoryMiniInputField(
                        value = editSubName,
                        onValueChange = { editSubName = it },
                        placeholder = "Subcategory Name"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        CategoryMiniInputField(
                            value = editSubDesc,
                            onValueChange = { if (it.length <= 100) editSubDesc = it },
                            placeholder = "Brief detail/description (under 100 chars)"
                        )
                        Text(
                            text = "${editSubDesc.length}/100 characters",
                            fontSize = 11.sp,
                            color = BrandOnSurfaceVariant,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    PresetIconsGridPicker(
                        selectedIconName = editSubIcon,
                        onSelectIconName = { editSubIcon = it }
                    )

                    CustomImagePickerRow(
                        imageUri = editSubUri,
                        onSelectImage = { imageLauncher.launch("image/*") },
                        onClearImage = { editSubUri = "" }
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun CategoryTaxonomyCard(
    category: DbCategory,
    viewModel: AppViewModel,
    onAddSubcategory: () -> Unit,
    onEditCategory: () -> Unit,
    onEditSubcategory: (SubcategoryItem) -> Unit,
    onDeleteCategory: () -> Unit,
    onDeleteSubcategory: (SubcategoryItem) -> Unit
) {
    val (icon, badgeColor, iconColor) = getCategoryIconAndColors(categoryName = category.name, categoryIconName = category.iconName)
    val userBudgets by viewModel.userBudgets.collectAsState()
    val isBudgetSpecialCategory = category.name.equals("Budget & Goal", ignoreCase = true) || 
                                  category.name.equals("Budget", ignoreCase = true) || 
                                  category.type.equals("Budget", ignoreCase = true) ||
                                  userBudgets.any { it.name.equals(category.name, ignoreCase = true) }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkThemeActive) BrandSurface else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isDarkThemeActive) BrandOutline else Color(0xFFEFF6FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkThemeActive) 0.dp else 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    CategoryIconView(
                        iconName = category.iconName,
                        customImageUri = category.customImageUri,
                        badgeColor = if (isBudgetSpecialCategory) {
                            if (isDarkThemeActive) Color(0xFF4C1D3A) else Color(0xFFFCE7F3)
                        } else {
                            badgeColor
                        },
                        iconColor = if (isBudgetSpecialCategory) {
                            if (isDarkThemeActive) Color(0xFFF472B6) else Color(0xFFDB2777)
                        } else {
                            iconColor
                        },
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = category.name,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            color = if (isDarkThemeActive) Color.White else Color(0xFF0F2D59),
                            fontSize = 17.sp
                        )
                        if (category.description.isNotBlank()) {
                            Text(
                                text = category.description,
                                fontSize = 11.5.sp,
                                fontFamily = InterFontFamily,
                                color = if (isDarkThemeActive) BrandOnSurfaceVariant else Color(0xFF64748B)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = if (isDarkThemeActive) Color.White else Color(0xFF64748B)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Subcategory") },
                            onClick = {
                                showMenu = false
                                onAddSubcategory()
                            },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = BrandPrimary) }
                        )
                         DropdownMenuItem(
                            text = { Text("Edit Category", color = if (isBudgetSpecialCategory) Color.Gray else (if (isDarkThemeActive) Color.White else Color.Black)) },
                            onClick = {
                                showMenu = false
                                if (isBudgetSpecialCategory) {
                                    android.widget.Toast.makeText(viewModel.getApplication(), "This is a Special Budget Category. You must delete the budget first.", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    onEditCategory()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = if (isBudgetSpecialCategory) Color.Gray else (if (isDarkThemeActive) BrandPrimary else Color(0xFF1D4ED8))) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Category", color = if (isBudgetSpecialCategory) Color.Gray else BrandError) },
                            onClick = {
                                showMenu = false
                                if (isBudgetSpecialCategory) {
                                    android.widget.Toast.makeText(viewModel.getApplication(), "This is a Special Budget Category. You must delete the budget first.", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    onDeleteCategory()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = if (isBudgetSpecialCategory) Color.Gray else BrandError) }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate Category") },
                            onClick = {
                                showMenu = false
                                viewModel.duplicateCategory(category)
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = if (isDarkThemeActive) BrandPrimary else Color(0xFF64748B)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subcategories Column with Left vertical line
            val subItems = category.getSubcategoryItems()
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Spacer(modifier = Modifier.width(21.dp))
                
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(if (isDarkThemeActive) Color(0xFF334155) else Color(0xFFE2EDF8))
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (subItems.isEmpty()) {
                        Text(
                            text = "No subcategory items logged.",
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            color = if (isDarkThemeActive) BrandOnSurfaceVariant else Color(0xFF94A3B8)
                        )
                    } else {
                        subItems.forEach { subItem ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val subBadge = if (isDarkThemeActive) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                    val subIconColor = if (isDarkThemeActive) BrandPrimary else Color(0xFF475569)
                                    CategoryIconView(
                                        iconName = subItem.iconName,
                                        customImageUri = subItem.customImageUri,
                                        badgeColor = subBadge,
                                        iconColor = subIconColor,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = subItem.name,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDarkThemeActive) Color.White else Color(0xFF334155),
                                            fontSize = 14.sp
                                        )
                                        if (subItem.description.isNotBlank()) {
                                            Text(
                                                text = subItem.description,
                                                fontFamily = InterFontFamily,
                                                color = if (isDarkThemeActive) BrandOnSurfaceVariant else Color(0xFF64748B),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onEditSubcategory(subItem) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Subcategory",
                                            tint = if (isDarkThemeActive) BrandPrimary else Color(0xFF2563EB),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            onDeleteSubcategory(subItem)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Subcategory",
                                            tint = BrandError,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add Subcategory Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onAddSubcategory)
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Text(
                            text = "+ Add Subcategory",
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkThemeActive) BrandPrimary else Color(0xFF2563EB)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PresetIconsGridPicker(
    selectedIconName: String,
    onSelectIconName: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Select Preset Vector Icon",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkThemeActive) Color.White else Color(0xFF0F2D59)
        )
        
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(defaultIconsList.take(11)) { icon ->
                val isActive = selectedIconName == icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) {
                                if (isDarkThemeActive) BrandPrimary else Color(0xFFE2E8F0)
                              } else {
                                if (isDarkThemeActive) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                              }
                        )
                        .clickable { onSelectIconName(icon) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconByName(icon),
                        contentDescription = icon,
                        tint = if (isActive) {
                            if (isDarkThemeActive) Color.Black else Color(0xFF2563EB)
                        } else {
                            if (isDarkThemeActive) Color.White else Color(0xFF64748B)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(defaultIconsList.drop(11)) { icon ->
                val isActive = selectedIconName == icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) {
                                if (isDarkThemeActive) BrandPrimary else Color(0xFFE2E8F0)
                              } else {
                                if (isDarkThemeActive) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                              }
                        )
                        .clickable { onSelectIconName(icon) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconByName(icon),
                        contentDescription = icon,
                        tint = if (isActive) {
                            if (isDarkThemeActive) Color.Black else Color(0xFF2563EB)
                        } else {
                            if (isDarkThemeActive) Color.White else Color(0xFF64748B)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomImagePickerRow(
    imageUri: String,
    onSelectImage: () -> Unit,
    onClearImage: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Button(
            onClick = onSelectImage,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDarkThemeActive) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                contentColor = if (isDarkThemeActive) Color.White else Color.Black
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Upload Custom Icon Image", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        
        if (imageUri.isNotBlank()) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                val painter = rememberAsyncImagePainter(model = imageUri)
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            IconButton(onClick = onClearImage) {
                Icon(Icons.Default.Close, contentDescription = "Clear", tint = BrandError, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun CategoryMiniInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = placeholder, fontSize = 14.sp) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = if (isDarkThemeActive) Color.White else Color(0xFF0F2D59),
            unfocusedTextColor = if (isDarkThemeActive) Color.White else Color(0xFF0F2D59),
            focusedBorderColor = BrandPrimary,
            unfocusedBorderColor = if (isDarkThemeActive) BrandOutline else Color(0xFFEFF6FF)
        )
    )
}
