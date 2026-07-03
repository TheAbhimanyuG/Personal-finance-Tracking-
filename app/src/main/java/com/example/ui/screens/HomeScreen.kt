package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.components.UnifiedBottomNavBar
import com.example.ui.components.UnifiedTopBar
import com.example.ui.components.UnifiedAddEditTxDialog
import com.example.ui.AppViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag

data class BentoNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val circleColorLight: Color,
    val iconColorLight: Color,
    val circleColorDark: Color,
    val iconColorDark: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToScreen: (String) -> Unit,
    onLogout: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            viewModel.triggerCommitmentProcessing()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var showAddTxDialog by remember { mutableStateOf(false) }

    // Dialog state
    var txTitle by remember { mutableStateOf("") }
    var txAmount by remember { mutableStateOf("") }
    var txType by remember { mutableStateOf("Expense") } // "Income" or "Expense"
    var txCategory by remember { mutableStateOf("Food & Dining") }
    var txSubCategory by remember { mutableStateOf("") }
    var txPaymentMethod by remember { mutableStateOf("Debit Card") }

    val bentoItems = remember {
        listOf(
            BentoNavItem(
                title = "Net Worth Dashboard",
                icon = Icons.Default.ReceiptLong,
                route = "net_worth",
                circleColorLight = Color(0xFFE0F2FE),
                iconColorLight = Color(0xFF0369A1),
                circleColorDark = Color(0xFF1E293B),
                iconColorDark = Color(0xFF38BDF8)
            ),
            BentoNavItem(
                title = "Investment Dashboard",
                icon = Icons.Default.TrendingUp,
                route = "investments",
                circleColorLight = Color(0xFF93C5FD),
                iconColorLight = Color(0xFF1E40AF),
                circleColorDark = Color(0xFF1E3A8A),
                iconColorDark = Color(0xFF93C5FD)
            ),
            BentoNavItem(
                title = "Income Dashboard",
                icon = Icons.Default.Payments,
                route = "income",
                circleColorLight = Color(0xFF86EFAC),
                iconColorLight = Color(0xFF14532D),
                circleColorDark = Color(0xFF0F3E22),
                iconColorDark = Color(0xFF4ADE80)
            ),
            BentoNavItem(
                title = "Expense Dashboard",
                icon = Icons.Default.ShoppingCart,
                route = "expense",
                circleColorLight = Color(0xFFBFDBFE),
                iconColorLight = Color(0xFF1E40AF),
                circleColorDark = Color(0xFF2E4A7D),
                iconColorDark = Color(0xFF93C5FD)
            ),
            BentoNavItem(
                title = "Export",
                icon = Icons.Default.CloudDownload,
                route = "reports",
                circleColorLight = Color(0xFFEFF6FF),
                iconColorLight = Color(0xFF3B82F6),
                circleColorDark = Color(0xFF1E293B),
                iconColorDark = Color(0xFF60A5FA)
            ),
            BentoNavItem(
                title = "Budget & Goal",
                icon = Icons.Default.TrackChanges,
                route = "budget_goals",
                circleColorLight = Color(0xFF10B981),
                iconColorLight = Color.White,
                circleColorDark = Color(0xFF047857),
                iconColorDark = Color.White
            ),
            BentoNavItem(
                title = "Category Management",
                icon = Icons.Default.Category,
                route = "categories",
                circleColorLight = Color(0xFFEFF6FF),
                iconColorLight = Color(0xFF3B82F6),
                circleColorDark = Color(0xFF1E293B),
                iconColorDark = Color(0xFF60A5FA)
            ),
            BentoNavItem(
                title = "Calculators",
                icon = Icons.Default.Calculate,
                route = "calculator",
                circleColorLight = Color(0xFFEFF6FF),
                iconColorLight = Color(0xFF3B82F6),
                circleColorDark = Color(0xFF1E293B),
                iconColorDark = Color(0xFF60A5FA)
            ),
            BentoNavItem(
                title = "Settings",
                icon = Icons.Default.Settings,
                route = "settings",
                circleColorLight = Color(0xFFEFF6FF),
                iconColorLight = Color(0xFF3B82F6),
                circleColorDark = Color(0xFF1E293B),
                iconColorDark = Color(0xFF60A5FA)
            ),
            BentoNavItem(
                title = "Edit Profile",
                icon = Icons.Default.AccountCircle,
                route = "edit_profile",
                circleColorLight = Color(0xFFFEF3C7),
                iconColorLight = Color(0xFFD97706),
                circleColorDark = Color(0xFF2E1C0A),
                iconColorDark = Color(0xFFFBBF24)
            )
        )
    }

    val filteredBentoItems = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            bentoItems
        } else {
            bentoItems.filter {
                it.title.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(BrandBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Greeting Block
                    Box(modifier = Modifier.padding(vertical = 12.dp)) {
                        Column {
                            val firstName = currentUser?.name?.split(" ")?.firstOrNull() ?: "User"
                            Text(
                                text = "Welcome back, $firstName",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                lineHeight = 38.sp,
                                color = if (isDarkThemeActive) BrandPrimary else Color(0xFF001F54),
                                letterSpacing = (-1).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your financial status is secured and up to date.",
                                fontFamily = InterFontFamily,
                                fontSize = 15.sp,
                                color = BrandOnSurfaceVariant
                            )
                        }
                    }



                    // Rounded visual custom search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        placeholder = { 
                            Text(
                                text = "Search features...", 
                                fontFamily = InterFontFamily, 
                                color = if (isDarkThemeActive) BrandOnSurfaceVariant else Color(0xFF475569)
                            ) 
                        },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Default.Search, 
                                contentDescription = null, 
                                tint = if (isDarkThemeActive) BrandPrimary else Color(0xFF475569)
                            ) 
                        },
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BrandOnBackground,
                            unfocusedTextColor = BrandOnBackground,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = if (isDarkThemeActive) BrandSurfaceContainerLow else Color(0xFFEBF3FC),
                            unfocusedContainerColor = if (isDarkThemeActive) BrandSurfaceContainerLow else Color(0xFFEBF3FC)
                        )
                    )

                    if (searchQuery.isBlank()) {
                        // Standard complete dashboard grid matching screen
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CustomBentoCard(
                                modifier = Modifier.weight(1f),
                                item = bentoItems[0],
                                onClick = { onNavigateToScreen(bentoItems[0].route) }
                            )
                            CustomBentoCard(
                                modifier = Modifier.weight(1f),
                                item = bentoItems[1],
                                onClick = { onNavigateToScreen(bentoItems[1].route) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CustomBentoCard(
                                modifier = Modifier.weight(1f),
                                item = bentoItems[2],
                                onClick = { onNavigateToScreen(bentoItems[2].route) }
                            )
                            CustomBentoCard(
                                modifier = Modifier.weight(1f),
                                item = bentoItems[3],
                                onClick = { onNavigateToScreen(bentoItems[3].route) }
                            )
                        }

                        AddTransactionCard(
                            onClick = { showAddTxDialog = true },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CustomBentoCard(
                                modifier = Modifier.weight(1f),
                                item = bentoItems[4],
                                onClick = { onNavigateToScreen(bentoItems[4].route) }
                            )
                            CustomBentoCard(
                                modifier = Modifier.weight(1f),
                                item = bentoItems[5],
                                onClick = { onNavigateToScreen(bentoItems[5].route) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CustomBentoCard(
                                modifier = Modifier.weight(1f),
                                item = bentoItems[6],
                                onClick = { onNavigateToScreen(bentoItems[6].route) }
                            )
                            CustomBentoCard(
                                modifier = Modifier.weight(1f),
                                item = bentoItems[7],
                                onClick = { onNavigateToScreen(bentoItems[7].route) }
                            )
                        }

                        QuickActionRow(
                            title = "Settings",
                            icon = Icons.Default.Settings,
                            rightIcon = Icons.Default.KeyboardArrowRight,
                            containerColor = if (isDarkThemeActive) BrandSurfaceContainer else Color(0xFFEBF5FF),
                            contentColor = if (isDarkThemeActive) BrandPrimary else Color(0xFF0F2D59),
                            onClick = { onNavigateToScreen("settings") },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        QuickActionRow(
                            title = "Edit Profile",
                            icon = Icons.Default.Person,
                            rightIcon = Icons.Default.KeyboardArrowRight,
                            containerColor = if (isDarkThemeActive) BrandSurfaceContainer else Color(0xFFF5F3FF),
                            contentColor = if (isDarkThemeActive) BrandPrimary else Color(0xFF5B21B6),
                            onClick = { onNavigateToScreen("edit_profile") },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        QuickActionRow(
                            title = "Switch User / Logout",
                            icon = Icons.Default.Logout,
                            rightIcon = Icons.Default.Close,
                            containerColor = if (isDarkThemeActive) BrandErrorContainer else Color(0xFFFEE2E2),
                            contentColor = if (isDarkThemeActive) BrandOnErrorContainer else Color(0xFF991B1B),
                            onClick = {
                                viewModel.logoutUser {
                                    onLogout()
                                }
                            },
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                    } else {
                        Text(
                            text = "Matching features (${filteredBentoItems.size}):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandOnBackground,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        if (filteredBentoItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No tools matched your search query.",
                                    color = BrandOnSurfaceVariant,
                                    fontFamily = InterFontFamily
                                )
                            }
                        } else {
                            filteredBentoItems.chunked(2).forEach { chunk ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    chunk.forEach { item ->
                                        CustomBentoCard(
                                            modifier = Modifier.weight(1f),
                                            item = item,
                                            onClick = { onNavigateToScreen(item.route) }
                                        )
                                    }
                                    if (chunk.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Sliding Profile Panel with smooth entering and exiting transitions
                val showProfilePanel by viewModel.showProfilePanel.collectAsState()
                val isDark = isDarkThemeActive
                androidx.compose.animation.AnimatedVisibility(
                    visible = showProfilePanel,
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + fadeIn(),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + fadeOut(),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(320.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = if (isDark) {
                                        listOf(BrandSurface, Color(0xFF131A35))
                                    } else {
                                        listOf(Color(0xFFF8FAFC), Color(0xFFEFF6FF))
                                    }
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDark) BrandOutline else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section: Header with close
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "System Profile",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                                )
                                IconButton(
                                    onClick = { viewModel.setShowProfilePanel(false) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close profile panel",
                                        tint = if (isDark) Color.White else Color(0xFF0F1B6B)
                                    )
                                }
                            }

                            // Section: Profile details
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isDark) Color(0xFF1E294B) else Color(0xFFE2E8F0).copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(BrandPrimary.copy(alpha = 0.2f))
                                        .border(2.dp, BrandPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentUser?.avatarUrl?.isNotBlank() == true) {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = currentUser?.avatarUrl),
                                            contentDescription = "Profile Panel avatar",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = currentUser?.name?.take(2)?.uppercase() ?: "KT",
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp,
                                            color = BrandPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = currentUser?.name ?: "Your Name",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isDark) Color.White else Color(0xFF0D1B2A)
                                )
                                Text(
                                    text = currentUser?.email ?: "user@example.com",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = BrandOnSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Trust Coins",
                                            fontSize = 10.sp,
                                            color = BrandOnSurfaceVariant
                                        )
                                        Text(
                                            text = "${currentUser?.trustScore ?: 120}🪙",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFFFBBF24)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "User ID",
                                            fontSize = 10.sp,
                                            color = BrandOnSurfaceVariant
                                        )
                                        Text(
                                            text = "#${currentUser?.userId?.subSequence(0, 5) ?: "KT001"}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isDark) Color.LightGray else Color.DarkGray
                                        )
                                    }
                                }
                            }

                            Divider(color = if (isDark) BrandOutline else Color(0xFFE2E8F0))

                            // Section Preference Controls
                            Text(
                                text = "Preferences",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.LightGray else Color(0xFF0F1B6B)
                            )

                            // 1. Theme Selection
                            val selectedTheme by viewModel.selectedTheme.collectAsState()
                            Text("Interface Theme", fontSize = 11.sp, color = BrandOnSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Light", "Dark", "System").forEach { theme ->
                                    val isSel = selectedTheme == theme
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { viewModel.toggleTheme(theme) },
                                        label = { Text(theme, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandPrimary.copy(alpha = 0.25f),
                                            selectedLabelColor = BrandPrimary
                                        )
                                    )
                                }
                            }

                            // 2. Currency Selection
                            val selectedCurrency by viewModel.selectedCurrency.collectAsState()
                            Text("Primary Currency", fontSize = 11.sp, color = BrandOnSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("USD", "INR", "EUR").forEach { curr ->
                                    val isSel = selectedCurrency == curr
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { viewModel.setCurrency(curr) },
                                        label = { Text(curr, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandPrimary.copy(alpha = 0.25f),
                                            selectedLabelColor = BrandPrimary
                                        )
                                    )
                                }
                            }

                            // 3. Language Selection
                            val selectedLanguage by viewModel.selectedLanguage.collectAsState()
                            Text("System Language", fontSize = 11.sp, color = BrandOnSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("EN", "HI", "FR").forEach { lang ->
                                    val isSel = selectedLanguage == lang
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { viewModel.setLanguage(lang) },
                                        label = { Text(lang, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandPrimary.copy(alpha = 0.25f),
                                            selectedLabelColor = BrandPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    val categoriesList by viewModel.categories.collectAsState()
    UnifiedAddEditTxDialog(
        show = showAddTxDialog,
        editingTxn = null,
        onDismiss = { showAddTxDialog = false },
        categories = categoriesList,
        isDark = isDarkThemeActive,
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
        }
    )
}

@Composable
fun CustomBentoCard(
    item: BentoNavItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkThemeActive) BrandSurfaceContainer else Color(0xFFEBF5FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .height(130.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isDarkThemeActive) item.circleColorDark else item.circleColorLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = if (isDarkThemeActive) item.iconColorDark else item.iconColorLight,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = item.title,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isDarkThemeActive) Color.White else Color(0xFF0F2D59),
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun AddTransactionCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkThemeActive) BrandSurfaceContainerHighest else Color(0xFF000839)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Add Transaction",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun QuickActionRow(
    title: String,
    icon: ImageVector,
    rightIcon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = contentColor
                )
            }
            Icon(
                imageVector = rightIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun KineticBottomNav(
    activeScreen: String,
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        color = if (isDarkThemeActive) BrandSurfaceContainerLow else Color(0xFFEBF3FC),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                BottomNavItem("Reports", Icons.Default.BarChart, "reports"),
                BottomNavItem("Ledger", Icons.Default.ReceiptLong, "reports"),
                BottomNavItem("Home", Icons.Default.Home, "home"),
                BottomNavItem("Settings", Icons.Default.Settings, "settings"),
                BottomNavItem("Profile", Icons.Default.Person, "drawer")
            )

            items.forEach { item ->
                val isActive = item.activeTag == activeScreen
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDarkThemeActive) BrandPrimary.copy(alpha = 0.2f) else Color(0xFFA7F3D0))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isDarkThemeActive) BrandPrimary else Color(0xFF065F46),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkThemeActive) BrandPrimary else Color(0xFF065F46)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (item.activeTag == "drawer") {
                                    onOpenDrawer()
                                } else {
                                    onNavigate(item.activeTag)
                                }
                            }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isDarkThemeActive) Color.White.copy(alpha = 0.6f) else Color(0xFF475569),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            color = if (isDarkThemeActive) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val activeTag: String
)

@Composable
fun HomeMiniInputField(
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
