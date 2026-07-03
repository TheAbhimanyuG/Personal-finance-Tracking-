package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.AppViewModel
import com.example.ui.context.AppGlobalStateProvider
import com.example.ui.screens.*
import com.example.ui.theme.KineticTrustTheme
import kotlinx.coroutines.launch
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.BrandOnBackground
import com.example.ui.theme.BrandOnSurfaceVariant
import com.example.ui.components.UnifiedTopBar
import com.example.ui.components.UnifiedBottomNavBar
import com.example.ui.components.UnifiedAddEditTxDialog

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.isDarkThemeActive
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.BrandOutline
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.StitchOnSurfaceVariant
import com.example.ui.theme.InterFontFamily

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

            val unifiedNavigateToScreen = { route: String ->
                if (route == "home") {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate(route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }

            // Toast system output listener binding
            androidx.compose.runtime.LaunchedEffect(viewModel.toastMessage) {
                viewModel.toastMessage.collect { message ->
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            }

            // Global ViewModel-driven navigation routing binding
            androidx.compose.runtime.LaunchedEffect(viewModel.navigationRequest) {
                viewModel.navigationRequest.collect { route ->
                    if (route == "back") {
                        navController.popBackStack()
                    } else {
                        unifiedNavigateToScreen(route)
                    }
                }
            }

            val themeMode by viewModel.selectedTheme.collectAsState()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> systemDark
            }

            val authPrefs = remember {
                getSharedPreferences("kinetic_auth_prefs", android.content.Context.MODE_PRIVATE)
            }
            val hasPinOnStart = remember {
                val savedPin = authPrefs.getString("app_security_pin", "") ?: ""
                savedPin.isNotEmpty() && savedPin.length == 4
            }

            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = currentBackStackEntry?.destination?.route
            val showStaticNavigation = currentRoute != null && currentRoute != "login" && currentRoute != "pin_lock"

            val panelTitle = when (currentRoute) {
                "home" -> "Overview Dashboard"
                "net_worth" -> "Net Worth Dashboard"
                "budget_goals" -> "Budget & Goal"
                "investments" -> "Investment Dashboard"
                "income" -> "Income Dashboard"
                "expense" -> "Expense Dashboard"
                "categories" -> "Category Management"
                "ledger" -> "Raw Data Ledger"
                "reports" -> "Transaction Reports"
                "calculator" -> "Financial Calculator"
                "settings" -> "Settings & Control"
                "edit_profile" -> "Edit Profile"
                else -> "Dashboard"
            }

            KineticTrustTheme(darkTheme = useDarkTheme) {
                AppGlobalStateProvider(viewModel = viewModel) {
                    if (showStaticNavigation) {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = BrandSurface,
                                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                                modifier = Modifier.width(320.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(vertical = 16.dp)
                                ) {
                                    // 1. First option inside drawer: Small Previous Icon to close/back
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { scope.launch { drawerState.close() } },
                                            modifier = Modifier.testTag("drawer_back_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowBack,
                                                contentDescription = "Close Menu Drawer / Return",
                                                tint = if (useDarkTheme) Color.White else Color(0xFF0F1B6B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Back to Panel",
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (useDarkTheme) Color.White else Color(0xFF0F1B6B)
                                        )
                                    }

                                    HorizontalDivider(
                                        color = if (useDarkTheme) BrandOutline else Color(0xFFE2E8F0),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )

                                    // Nav items list
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        NavigationDrawerItem(
                                            label = { Text("Home Navigation") },
                                            selected = currentRoute == "home",
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                unifiedNavigateToScreen("home")
                                            },
                                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("Budget & Goal") },
                                            selected = currentRoute == "budget_goals",
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                unifiedNavigateToScreen("budget_goals")
                                            },
                                            icon = { Icon(Icons.Default.TrackChanges, contentDescription = null) },
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("Net Worth Dashboard") },
                                            selected = currentRoute == "net_worth",
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                unifiedNavigateToScreen("net_worth")
                                            },
                                            icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("Raw Data Ledger") },
                                            selected = currentRoute == "net_worth",
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                unifiedNavigateToScreen("net_worth")
                                            },
                                            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("Taxonomy Categories") },
                                            selected = currentRoute == "categories",
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                unifiedNavigateToScreen("categories")
                                            },
                                            icon = { Icon(Icons.Default.Category, contentDescription = null) },
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("Transaction Reports") },
                                            selected = currentRoute == "reports",
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                unifiedNavigateToScreen("reports")
                                            },
                                            icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("Financial Calculator") },
                                            selected = currentRoute == "calculator",
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                unifiedNavigateToScreen("calculator")
                                            },
                                            icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("Account Preferences") },
                                            selected = currentRoute == "settings",
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                unifiedNavigateToScreen("settings")
                                            },
                                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                drawerState.close()
                                                viewModel.logoutUser {
                                                    navController.navigate("login") {
                                                        popUpTo("home") { inclusive = true }
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .height(48.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Logout Session", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    ) {
                        AppScaffoldContent(
                            navController = navController,
                            showStaticNavigation = true,
                            panelTitle = panelTitle,
                            currentRoute = currentRoute,
                            drawerState = drawerState,
                            scope = scope,
                            viewModel = viewModel,
                            useDarkTheme = useDarkTheme,
                            hasPinOnStart = hasPinOnStart,
                            unifiedNavigateToScreen = unifiedNavigateToScreen,
                            authPrefs = authPrefs
                        )
                    }
                } else {
                    AppScaffoldContent(
                        navController = navController,
                        showStaticNavigation = false,
                        panelTitle = "Dashboard",
                        currentRoute = currentRoute,
                        drawerState = drawerState,
                        scope = scope,
                        viewModel = viewModel,
                        useDarkTheme = useDarkTheme,
                        hasPinOnStart = hasPinOnStart,
                        unifiedNavigateToScreen = unifiedNavigateToScreen,
                        authPrefs = authPrefs
                    )
                }
            }
        }
    }
}
}

@Composable
fun AppScaffoldContent(
    navController: androidx.navigation.NavHostController,
    showStaticNavigation: Boolean,
    panelTitle: String,
    currentRoute: String?,
    drawerState: DrawerState,
    scope: kotlinx.coroutines.CoroutineScope,
    viewModel: AppViewModel,
    useDarkTheme: Boolean,
    hasPinOnStart: Boolean,
    unifiedNavigateToScreen: (String) -> Unit,
    authPrefs: android.content.SharedPreferences
) {
    var showAddTxDialog by remember { mutableStateOf(false) }
    val categoriesList by viewModel.categories.collectAsState()
    val showProfilePanel by viewModel.showProfilePanel.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            if (showStaticNavigation) {
                UnifiedTopBar(
                    viewModel = viewModel,
                    panelTitle = panelTitle,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
        },
        bottomBar = {
            if (showStaticNavigation) {
                val activeBarRoute = when (currentRoute) {
                    "home" -> "home"
                    "settings" -> "settings"
                    "budget_goals" -> "budget_goals"
                    "net_worth", "ledger" -> "ledger"
                    else -> ""
                }
                UnifiedBottomNavBar(
                    activeRoute = activeBarRoute,
                    onNavigate = { route ->
                        if (route == "add_txn") {
                            showAddTxDialog = true
                        } else {
                            unifiedNavigateToScreen(route)
                        }
                    },
                    viewModel = viewModel
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandBackground)
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = if (hasPinOnStart) "pin_lock" else "login",
                enterTransition = {
                    fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.95f, animationSpec = tween(300, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing)) +
                    scaleOut(targetScale = 1.05f, animationSpec = tween(250, easing = FastOutSlowInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 1.05f, animationSpec = tween(300, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing)) +
                    scaleOut(targetScale = 0.95f, animationSpec = tween(250, easing = FastOutSlowInEasing))
                }
            ) {
                composable("pin_lock") {
                    val savedPin = remember { authPrefs.getString("app_security_pin", "") ?: "" }
                    PinLockScreen(
                        correctPin = savedPin,
                        authPrefs = authPrefs,
                        onSuccess = {
                            navController.navigate("home") {
                                popUpTo("pin_lock") { inclusive = true }
                            }
                        }
                    )
                }
                composable("login") {
                    AuthScreen(
                        viewModel = viewModel,
                        onAuthSuccess = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }
                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToScreen = unifiedNavigateToScreen,
                        onLogout = {
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }
                composable("net_worth") {
                    NetWorthDashboardScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToDashboard = unifiedNavigateToScreen
                    )
                }
                composable("budget_goals") {
                    BudgetGoalDashboardScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToScreen = unifiedNavigateToScreen
                    )
                }
                composable("investments") {
                    InvestmentDashboardScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToScreen = unifiedNavigateToScreen
                    )
                }
                composable("income") {
                    IncomeDashboardScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToScreen = unifiedNavigateToScreen
                    )
                }
                composable("expense") {
                    ExpenseDashboardScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToScreen = unifiedNavigateToScreen
                    )
                }
                composable("categories") {
                    CategoryManagementScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToScreen = unifiedNavigateToScreen
                    )
                }
                composable("ledger") {
                    RawDataLedgerScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToScreen = unifiedNavigateToScreen
                    )
                }
                composable("reports") {
                    TransactionExportScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToScreen = unifiedNavigateToScreen
                    )
                }
                composable("calculator") {
                    CalculatorScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToScreen = unifiedNavigateToScreen
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onLogout = {
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        onNavigateToScreen = unifiedNavigateToScreen
                    )
                }
                composable("edit_profile") {
                    EditProfileScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToScreen = unifiedNavigateToScreen
                    )
                }
            }

            // Universal/Static Sliding Profile Panel
            val isDark = useDarkTheme
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
                                text = currentUser?.name ?: "Guest Profile",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color(0xFF0D1B2A)
                            )
                            Text(
                                text = currentUser?.email ?: "guest@kinetictrust.com",
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
                                        text = "#${currentUser?.userId?.take(5) ?: "KT001"}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDark) Color.LightGray else Color.DarkGray
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = if (isDark) BrandOutline else Color(0xFFE2E8F0))

                        Text(
                            text = "Preferences",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDark) Color.LightGray else Color(0xFF0F1B6B)
                        )

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

            UnifiedAddEditTxDialog(
                show = showAddTxDialog,
                editingTxn = null,
                onDismiss = { showAddTxDialog = false },
                categories = categoriesList,
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
                }
            )
        }
    }
}

@Composable
fun PinLockScreen(
    correctPin: String,
    authPrefs: android.content.SharedPreferences,
    onSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorVisible by remember { mutableStateOf(false) }
    var showForgotPinDialog by remember { mutableStateOf(false) }

    val isDark = isDarkThemeActive
    val bg = if (isDark) BrandBackground else Color(0xFFF4F8FD)
    val textC = if (isDark) Color.White else Color(0xFF0F1B6B)
    val secondaryTextC = if (isDark) StitchOnSurfaceVariant else Color(0xFF4B5563)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Identity Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Secure Access Lock",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = textC
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please enter your 4-digit PIN to authenticate",
                fontFamily = InterFontFamily,
                fontSize = 14.sp,
                color = secondaryTextC,
                textAlign = TextAlign.Center
            )
        }

        // Indicator dots for password input
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..3).forEach { index ->
                    val isActive = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) (if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                                else (if (isDark) Color(0xFF1F2937) else Color(0xFFD1D5DB))
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDark) BrandOutline else Color(0xFF9CA3AF),
                                shape = CircleShape
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (errorVisible) {
                Text(
                    text = "Incorrect PIN code. Try again.",
                    color = Color.Red,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp)) // Maintain layout height
            }
        }

        // Custom numeric keyboard keypad grid (1-9, 0, backspace)
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val keypad = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "Back")
            )

            keypad.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { buttonLabel ->
                        if (buttonLabel.isEmpty()) {
                            Spacer(modifier = Modifier.size(60.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark) Color(0xFF111827) else Color(0xFFE5E7EB)
                                    )
                                    .clickable {
                                        errorVisible = false
                                        if (buttonLabel == "Back") {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        } else {
                                            if (enteredPin.length < 4) {
                                                enteredPin += buttonLabel
                                                if (enteredPin.length == 4) {
                                                    if (enteredPin == correctPin) {
                                                        onSuccess()
                                                    } else {
                                                        errorVisible = true
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (buttonLabel == "Back") {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = textC,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = buttonLabel,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = textC
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Forgot PIN Button
        TextButton(
            onClick = { showForgotPinDialog = true },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = "Forgot PIN?",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
            )
        }
    }

    if (showForgotPinDialog) {
        var userIdInput by remember { mutableStateOf("") }
        var emailInput by remember { mutableStateOf("") }
        var contactNoInput by remember { mutableStateOf("") }
        val context = androidx.compose.ui.platform.LocalContext.current
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { showForgotPinDialog = false },
            title = {
                Text(
                    text = "Unlock & Reset PIN",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textC
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "To bypass and deactivate the security lock PIN, enter your identity clearance credentials below:",
                        fontSize = 12.sp,
                        color = secondaryTextC
                    )
                    OutlinedTextField(
                        value = userIdInput,
                        onValueChange = { userIdInput = it },
                        label = { Text("User ID (e.g. user_id)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                        )
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Registered Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                        )
                    )
                    OutlinedTextField(
                        value = contactNoInput,
                        onValueChange = { contactNoInput = it },
                        label = { Text("Contact Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val database = com.example.data.db.AppDatabase.getInstance(context)
                            val dbUser = database.financialDao.getUserById(userIdInput.trim().lowercase())
                            if (dbUser != null &&
                                dbUser.email.trim().lowercase() == emailInput.trim().lowercase() &&
                                dbUser.contactNo.trim() == contactNoInput.trim()
                            ) {
                                authPrefs.edit().remove("app_security_pin").apply()
                                Toast.makeText(context, "PIN code de-registered successfully!", Toast.LENGTH_LONG).show()
                                showForgotPinDialog = false
                                onSuccess()
                            } else {
                                Toast.makeText(context, "Incorrect identity details provided!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Unlock & Reset Mode", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPinDialog = false }) {
                    Text("Dismiss", color = if (isDark) Color.LightGray else Color.Gray)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = if (isDark) Color(0xFF0C101B) else Color.White
        )
    }
}
