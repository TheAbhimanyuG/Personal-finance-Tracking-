package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.components.UnifiedBottomNavBar
import com.example.ui.components.GlobalConfirmationModal

import com.example.ui.AppViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {},
    onNavigateToScreen: (String) -> Unit = {}
) {
    val themeMode by viewModel.selectedTheme.collectAsState()
    val currencyUnit by viewModel.selectedCurrency.collectAsState()
    val languageSelection by viewModel.selectedLanguage.collectAsState()

    val bioActive by viewModel.isBiometricEnabled.collectAsState()
    val alertsActive by viewModel.isTransactionAlertsEnabled.collectAsState()
    val budgetActive by viewModel.isBudgetRemindersEnabled.collectAsState()
    val newsActive by viewModel.isInvestmentNewsEnabled.collectAsState()
    val autoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsState()
    val autoBackupFrequency by viewModel.autoBackupFrequency.collectAsState()

    val user by viewModel.currentUser.collectAsState()
    val usersList by viewModel.availableUsers.collectAsState()
    val userBudgets by viewModel.userBudgets.collectAsState()
    val userGoals by viewModel.userGoals.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Dynamic state controllers
    var expandedCurrency by remember { mutableStateOf(false) }
    var expandedLanguage by remember { mutableStateOf(false) }
    var twoFactorAuthEnabled by remember { mutableStateOf(true) }

    // Dialog flags
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    var showPinChangeDialog by remember { mutableStateOf(false) }
    var showPasswordChangeDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showBackupShareSuccessDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showWipeOptionsDialog by remember { mutableStateOf(false) }
    val clearIncomeSelected = remember { mutableStateOf(false) }
    val clearExpenseSelected = remember { mutableStateOf(false) }
    val clearInvestmentSelected = remember { mutableStateOf(false) }
    val clearBudgetGoalSelected = remember { mutableStateOf(false) }
    val clearAllSelected = remember { mutableStateOf(false) }
    val showClearDataSuccessDialog = remember { mutableStateOf(false) }
    val clearDataDownloadedFileName = remember { mutableStateOf("") }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showSwitchUserDialog by remember { mutableStateOf(false) }

    // New Restore Wizard and Filename Prompt States
    var pendingRestoreText by remember { mutableStateOf("") }
    var parsedBackupResult by remember { mutableStateOf<com.example.ui.DuplicateCheckResult?>(null) }
    var showRestoreWizardDialog by remember { mutableStateOf(false) }
    var showRestoreMergeDuplicateDialog by remember { mutableStateOf(false) }
    var showSafetyBackupPromptDialog by remember { mutableStateOf(false) }

    // Filename Customization states
    var showFileNamePromptDialog by remember { mutableStateOf(false) }
    var customFileNamePrefix by remember { mutableStateOf("") }
    var onFileNameFilenameConfirmed by remember { mutableStateOf<((String) -> Unit)?>(null) }

    fun askAndDownloadBackup(
        onlyActive: Boolean,
        defaultName: String,
        selectedModules: Set<String> = setOf("Income", "Expense", "Investment", "Budget & Goal"),
        selectedBudgetIds: Set<Long> = emptySet(),
        onBackupWritten: (String) -> Unit = {}
    ) {
        customFileNamePrefix = defaultName
        onFileNameFilenameConfirmed = { chosenPrefix ->
            viewModel.generateBackupJson(onlyActive, selectedModules, selectedBudgetIds) { jsonStr ->
                if (jsonStr != null) {
                    try {
                        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                        val cleanPrefix = chosenPrefix.trim().replace("\\s+".toRegex(), "_")
                        val fileName = "${cleanPrefix}_${timestamp}.json"
                        val targetFile = java.io.File(context.getExternalFilesDir(null), fileName)
                        targetFile.writeText(jsonStr)

                        // Download to public Downloads directory
                        val savedPublicly = saveToPublicDownloads(context, fileName, jsonStr)
                        
                        if (savedPublicly) {
                            Toast.makeText(context, "Backup downloaded to local 'Downloads' folder! 📁", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Backup JSON file saved to internal storage! 📁", Toast.LENGTH_LONG).show()
                        }
                        clearDataDownloadedFileName.value = fileName
                        onBackupWritten(jsonStr)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Storage write error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Error yielding backup data", Toast.LENGTH_SHORT).show()
                }
            }
        }
        showFileNamePromptDialog = true
    }


    // Preferences file handling
    val authPrefs = remember(context) {
        context.getSharedPreferences("kinetic_auth_prefs", android.content.Context.MODE_PRIVATE)
    }

    // Dynamic badge evaluations
    val currentUserId = user?.userId ?: "default_user"
    val registrationTime = remember(currentUserId) {
        authPrefs.getLong("registration_timestamp_$currentUserId", 0L)
    }
    val accountAgeMinutes = remember(registrationTime) {
        if (registrationTime == 0L) 0L else (System.currentTimeMillis() - registrationTime) / (60 * 1000)
    }

    val membershipBadge = remember(currentUserId, accountAgeMinutes) {
        when {
            accountAgeMinutes < 1 -> "Bronze Starter 🥉"
            accountAgeMinutes < 5 -> "Silver Associate 🥈"
            accountAgeMinutes < 15 -> "Gold Senior 🥇"
            else -> "Platinum Veteran 💎"
        }
    }

    // Backup & Restore files loading launcher
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val text = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.bufferedReader().use { it.readText() }
                        } ?: ""
                    }
                    if (text.isNotBlank()) {
                        val result = viewModel.parseBackupAndDetectDuplicates(text)
                        if (result != null) {
                            pendingRestoreText = text
                            parsedBackupResult = result
                            showRestoreWizardDialog = true
                        } else {
                            Toast.makeText(context, "Could not parse chosen backup file! 📁", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Backup file is empty!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to restore: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Capture Wipe and Math Solver States
    var wipeDataScope by remember { mutableStateOf("single") } // "single" or "full"
    var enteredWipePassword by remember { mutableStateOf("") }
    var enteredWipeCaptchaAnswer by remember { mutableStateOf("") }
    var mathCaptchaNum1 by remember { mutableStateOf(0) }
    var mathCaptchaNum2 by remember { mutableStateOf(0) }
    var backupPerformedBeforeWipe by remember { mutableStateOf(false) }
    var generatedBackupJsonText by remember { mutableStateOf("") }

    fun refreshMathCaptcha() {
        mathCaptchaNum1 = (5..25).random()
        mathCaptchaNum2 = (5..25).random()
    }

    // Theme responsive properties
    val isDark = themeMode == "Dark" || (themeMode == "System" && androidx.compose.foundation.isSystemInDarkTheme())
    val scaffoldBg = if (isDark) BrandBackground else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF0F1524) else Color.White
    val outlineBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val primaryText = if (isDark) Color.White else Color(0xFF0F172A)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Scaffold(
        containerColor = scaffoldBg,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    // Refined ambient background design grid lines
                    val gridSpacing = 28.dp.toPx()
                    val gridColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.2f) else Color(0xFFE2E8F0).copy(alpha = 0.4f)
                    
                    var xPos = 0f
                    while (xPos < size.width) {
                        drawLine(color = gridColor, start = Offset(xPos, 0f), end = Offset(xPos, size.height), strokeWidth = 1f)
                        xPos += gridSpacing
                    }
                    var yPos = 0f
                    while (yPos < size.height) {
                        drawLine(color = gridColor, start = Offset(0f, yPos), end = Offset(size.width, yPos), strokeWidth = 1f)
                        yPos += gridSpacing
                    }
                },
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. STITCH PROFILE BLOCK
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, outlineBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Centered avatar with double borders
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clickable { showEditProfileDialog = true },
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(2.5.dp, if (isDark) BrandPrimary else Color(0xFF0F1B6B), CircleShape)
                                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (user?.avatarUrl?.isNotBlank() == true) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = user?.avatarUrl),
                                        contentDescription = "User Avatar",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = user?.name?.take(2)?.uppercase() ?: "KT",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 28.sp,
                                        color = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                                    .border(2.dp, cardBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Edit photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = user?.name ?: "Guest Identity",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = primaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = user?.email ?: "guest_ledger@kinetictrust.com",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Horizontally Aligned Badges side by side
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left badge: Streak days
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF3B2414) else Color(0xFFFFF7ED))
                                    .border(1.dp, if (isDark) Color(0xFF78350F) else Color(0xFFFFEDD5), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Whatshot,
                                        contentDescription = "Streak",
                                        tint = Color(0xFFEA580C),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${user?.streak ?: 0} DAY STREAK",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = Color(0xFFC2410C)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Right badge: membership level
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF1E261C) else Color(0xFFF0FDF4))
                                    .border(1.dp, if (isDark) Color(0xFF166534) else Color(0xFFDCFCE7), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stars,
                                        contentDescription = "Status",
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = membershipBadge.uppercase(),
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Edit profile button centered under the badges
                        OutlinedButton(
                            onClick = { onNavigateToScreen("edit_profile") },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryText),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = secondaryText)
                                Text("Edit Profile Panel", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. APPEARANCE SECTION
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "APPEARANCE",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                        letterSpacing = 1.3.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, outlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = if (isDark) BrandPrimary else Color(0xFF2563EB),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Theme Selection",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = primaryText
                                    )
                                    Text(
                                        text = "Choose light or dark look",
                                        fontFamily = InterFontFamily,
                                        fontSize = 11.sp,
                                        color = secondaryText
                                    )
                                }
                            }

                            // Theme Selector Chips
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                listOf("Light", "Dark", "System").forEach { opt ->
                                    val isCur = themeMode == opt
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isCur) (if (isDark) Color(0xFF2563EB) else Color(0xFF0F1B6B))
                                                else Color.Transparent
                                            )
                                            .clickable { viewModel.toggleTheme(opt) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = opt,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isCur) Color.White else secondaryText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. SECURITY SECTION
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SECURITY",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                        letterSpacing = 1.3.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, outlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Biometric row
                            SettingsListItem(
                                icon = Icons.Default.Fingerprint,
                                title = "Biometric Login",
                                description = "Enable fast fingerprint entry",
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                control = {
                                    Switch(
                                        checked = bioActive,
                                        onCheckedChange = { viewModel.toggleBiometric(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                        )
                                    )
                                }
                            )

                            HorizontalDivider(color = outlineBorder, modifier = Modifier.padding(horizontal = 16.dp))

                            // Compact View row
                            SettingsListItem(
                                icon = Icons.Default.AspectRatio,
                                title = "Compact View",
                                description = "Reduces padding and font sizes across panels",
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                control = {
                                    Switch(
                                        checked = isCompactViewActive,
                                        onCheckedChange = { isCompactViewActive = it },
                                        modifier = Modifier.testTag("compact_view_toggle"),
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                        )
                                    )
                                }
                            )

                            HorizontalDivider(color = outlineBorder, modifier = Modifier.padding(horizontal = 16.dp))

                            // Change PIN row
                            val hasPin = remember {
                                val sp = authPrefs.getString("app_security_pin", "") ?: ""
                                sp.isNotEmpty()
                            }
                            SettingsListItem(
                                icon = Icons.Default.LockOpen,
                                title = "Change Lock PIN",
                                description = if (hasPin) "Update your 4-digit code" else "Unconfigured passcode",
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = { showPinChangeDialog = true },
                                control = {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = secondaryText
                                    )
                                }
                            )

                            HorizontalDivider(color = outlineBorder, modifier = Modifier.padding(horizontal = 16.dp))

                            // Change Account Password row
                            SettingsListItem(
                                icon = Icons.Default.VpnKey,
                                title = "Change Password",
                                description = "Requires current password verification",
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = { showPasswordChangeDialog = true },
                                control = {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = secondaryText
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 4. NOTIFICATIONS SECTION
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "NOTIFICATIONS",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                        letterSpacing = 1.3.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, outlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SettingsListItem(
                                icon = Icons.Default.NotificationsActive,
                                title = "Transaction Alerts",
                                description = "Push notifications for buy/sell",
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                control = {
                                    Switch(
                                        checked = alertsActive,
                                        onCheckedChange = { viewModel.toggleTransactionAlerts(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                        )
                                    )
                                }
                            )
                            HorizontalDivider(color = outlineBorder, modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsListItem(
                                icon = Icons.Default.Warning,
                                title = "Budget Reminders",
                                description = "Alert at 80% expenditure speed",
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                control = {
                                    Switch(
                                        checked = budgetActive,
                                        onCheckedChange = { viewModel.toggleBudgetReminders(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                        )
                                    )
                                }
                            )
                            HorizontalDivider(color = outlineBorder, modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsListItem(
                                icon = Icons.Default.TrendingUp,
                                title = "Investment News",
                                description = "Sip suggestions weekly",
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                control = {
                                    Switch(
                                        checked = newsActive,
                                        onCheckedChange = { viewModel.toggleInvestmentNews(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 5. LOCALIZATION SECTION
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "LOCALIZATION",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                        letterSpacing = 1.3.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, outlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Currency picker
                            SettingsListItem(
                                icon = Icons.Default.MonetizationOn,
                                title = "Default Currency",
                                description = currencyUnit,
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = { expandedCurrency = true },
                                control = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (currencyUnit.contains("Rupee")) "INR (₹)" else if (currencyUnit.contains("Euro")) "EUR (€)" else "USD ($)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = secondaryText)
                                    }
                                    DropdownMenu(
                                        expanded = expandedCurrency,
                                        onDismissRequest = { expandedCurrency = false }
                                    ) {
                                        listOf("Rupee (₹)", "Dollar ($)", "Euro (€)").forEach { curr ->
                                            DropdownMenuItem(
                                                text = { Text(curr) },
                                                onClick = {
                                                    viewModel.setCurrency(curr)
                                                    expandedCurrency = false
                                                }
                                            )
                                        }
                                    }
                                }
                            )

                            HorizontalDivider(color = outlineBorder, modifier = Modifier.padding(horizontal = 16.dp))

                            // Language picker
                            SettingsListItem(
                                icon = Icons.Default.Language,
                                title = "Language Selection",
                                description = languageSelection,
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = { expandedLanguage = true },
                                control = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(languageSelection.split(" ").firstOrNull() ?: "English", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = secondaryText)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = secondaryText)
                                    }
                                    DropdownMenu(
                                        expanded = expandedLanguage,
                                        onDismissRequest = { expandedLanguage = false }
                                    ) {
                                        listOf("English (US)", "Hindi (हिन्दी)", "Spanish (Español)").forEach { lang ->
                                            DropdownMenuItem(
                                                text = { Text(lang) },
                                                onClick = {
                                                    viewModel.setLanguage(lang)
                                                    expandedLanguage = false
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // AUTO BACKUP SECTION
            item {
                var expandedBackupFreq by remember { mutableStateOf(false) }

                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text(
                        text = "AUTO BACKUP",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                        letterSpacing = 1.3.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, outlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 1. Auto Backup toggle rule
                            SettingsListItem(
                                icon = Icons.Default.CloudUpload,
                                title = "Automated Backups",
                                description = "Enable automatic night backups at 11:59 PM",
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                control = {
                                    Switch(
                                        checked = autoBackupEnabled,
                                        onCheckedChange = { viewModel.toggleAutoBackup(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = BrandPrimary,
                                            checkedTrackColor = BrandPrimary.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            )

                            if (autoBackupEnabled) {
                                HorizontalDivider(color = outlineBorder, modifier = Modifier.padding(horizontal = 16.dp))

                                // 2. Frequency Select option
                                SettingsListItem(
                                    icon = Icons.Default.Language,
                                    title = "Backup Frequency",
                                    description = "Set intervals for automated triggers",
                                    isDark = isDark,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onClick = { expandedBackupFreq = true },
                                    control = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = autoBackupFrequency,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = secondaryText
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = secondaryText)
                                        }
                                        DropdownMenu(
                                            expanded = expandedBackupFreq,
                                            onDismissRequest = { expandedBackupFreq = false }
                                        ) {
                                            listOf("Daily", "Weekly", "Monthly").forEach { freq ->
                                                DropdownMenuItem(
                                                    text = { Text(freq) },
                                                    onClick = {
                                                        viewModel.setAutoBackupFrequency(freq)
                                                        expandedBackupFreq = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                            }

                            HorizontalDivider(color = outlineBorder, modifier = Modifier.padding(horizontal = 16.dp))

                            // 3. User Override button: "Backup Now" and trigger immediate manual backup
                            SettingsListItem(
                                icon = Icons.Default.CloudDownload,
                                title = "Backup Now",
                                description = "Trigger manual backup of full application DB",
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = { viewModel.triggerBackup() },
                                control = {
                                    IconButton(onClick = { viewModel.triggerBackup() }) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Trigger backup right now",
                                            tint = if (isDark) BrandPrimary else Color(0xFF1E3A8A)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 6. DATA & PRIVACY SECTION (BACKUP, RESTORE, CLEAR DATA)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "DATA & PRIVACY",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                        letterSpacing = 1.3.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, outlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Backup row
                            SettingsListItem(
                                icon = Icons.Default.CloudDownload,
                                title = "Backup Database",
                                description = "Export encrypted state JSON",
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = { showBackupDialog = true },
                                control = {
                                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = secondaryText)
                                }
                            )
                            HorizontalDivider(color = outlineBorder, modifier = Modifier.padding(horizontal = 16.dp))

                            // Restore row
                            SettingsListItem(
                                icon = Icons.Default.CloudUpload,
                                title = "Restore Database",
                                description = "Upload file backup JSON",
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = { showRestoreDialog = true },
                                control = {
                                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = secondaryText)
                                }
                            )
                            HorizontalDivider(color = outlineBorder, modifier = Modifier.padding(horizontal = 16.dp))

                            // Clear Data row
                            SettingsListItem(
                                icon = Icons.Default.DeleteForever,
                                title = "Clear Data (Wipe)",
                                description = "Erase account Ledger records",
                                isDark = isDark,
                                primaryText = Color.Red,
                                secondaryText = Color.Red.copy(alpha = 0.8f),
                                onClick = {
                                    backupPerformedBeforeWipe = false
                                    enteredWipePassword = ""
                                    enteredWipeCaptchaAnswer = ""
                                    refreshMathCaptcha()
                                    showWipeOptionsDialog = true
                                },
                                control = {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                                }
                            )
                            HorizontalDivider(color = outlineBorder, modifier = Modifier.padding(horizontal = 16.dp))

                            // Privacy Policy row
                            SettingsListItem(
                                icon = Icons.Default.Security,
                                title = "Privacy Policy",
                                description = "Read user protection logs",
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = { showPrivacyPolicyDialog = true },
                                control = {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = secondaryText, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }

            // 7. BOTTOM EXITS & SWITCH USER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showSwitchUserDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                            contentColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Switch Profile", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { showLogoutConfirmDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFEE2E2),
                            contentColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Logout Session", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    GlobalConfirmationModal(
                        show = showLogoutConfirmDialog,
                        title = "Are you sure?",
                        message = "Do you want to confirm logging out and ending your active account session?",
                        confirmButtonColor = Color(0xFFEF4444),
                        isDark = isDark,
                        onConfirm = {
                            showLogoutConfirmDialog = false
                            viewModel.logoutUser(onLogout)
                        },
                        onCancel = { showLogoutConfirmDialog = false }
                    )

                }
            }

            // App Identity corporate tag
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F1B6B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Kinetic Trust Version 2.4.0 (Enterprise)",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = secondaryText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // --- DIALOGS REGISTRY ---

    // 1. EDIT PROFILE DIALOGUE
    if (showEditProfileDialog) {
        var profileName by remember { mutableStateOf(user?.name ?: "") }
        var profileEmail by remember { mutableStateOf(user?.email ?: "") }
        var profilePhone by remember { mutableStateOf(user?.contactNo ?: "") }
        var profileGmail by remember { mutableStateOf(user?.gmailId ?: "") }
        var profileAvatar by remember { mutableStateOf(user?.avatarUrl ?: "") }

        val predefinedAvatars = listOf(
            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=150&q=80",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&q=80",
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=150&q=80",
            "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=150&q=80"
        )

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = "Edit Profile Info",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = primaryText
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Select an elegant preset avatar or input custom image URL:",
                        fontSize = 12.sp,
                        color = secondaryText
                    )

                    // Row of presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        predefinedAvatars.forEach { imgUrl ->
                            val isSel = profileAvatar == imgUrl
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.dp,
                                        if (isSel) (if (isDark) BrandPrimary else Color(0xFF0F1B6B)) else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { profileAvatar = imgUrl }
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = imgUrl),
                                    contentDescription = "Avatar preset",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = profileAvatar,
                        onValueChange = { profileAvatar = it },
                        label = { Text("Avatar URL", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                    )

                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Full Name", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                    )

                    OutlinedTextField(
                        value = profileEmail,
                        onValueChange = { profileEmail = it },
                        label = { Text("Email Address", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                    )

                    OutlinedTextField(
                        value = profilePhone,
                        onValueChange = { profilePhone = it },
                        label = { Text("Contact Number", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                    )

                    OutlinedTextField(
                        value = profileGmail,
                        onValueChange = { profileGmail = it },
                        label = { Text("Gmail Linked ID", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(
                            name = profileName,
                            email = profileEmail,
                            isPremium = user?.isPremium ?: true,
                            trustScore = user?.trustScore ?: 800,
                            contactNo = profilePhone,
                            gmailId = profileGmail,
                            avatarUrl = profileAvatar,
                            streak = user?.streak ?: 0,
                            medal = user?.medal ?: "Start"
                        )
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = secondaryText)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = cardBg
        )
    }

    // 1b. CHANGE PASSWORD DIALOG
    if (showPasswordChangeDialog) {
        val userId = user?.userId ?: ""
        var showConfirmSubmitChange by remember { mutableStateOf(false) }
        var showConfirmRemovePassword by remember { mutableStateOf(false) }


        var currentPassInput by remember { mutableStateOf("") }
        var newPassInput by remember { mutableStateOf("") }
        var confirmPassInput by remember { mutableStateOf("") }


        var currentPassVis by remember { mutableStateOf(false) }
        var newPassVis by remember { mutableStateOf(false) }
        var confirmPassVis by remember { mutableStateOf(false) }

        val savedPass = remember(user) { authPrefs.getString("password_${user?.userId ?: ""}", "password") ?: "password" }

        AlertDialog(
            onDismissRequest = { showPasswordChangeDialog = false },
            title = {
                Text(
                    text = "Change Account Password",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = primaryText
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Verify your current credentials and select a strong new security password below.",
                        fontSize = 12.sp,
                        color = secondaryText
                    )

                    OutlinedTextField(
                        value = currentPassInput,
                        onValueChange = { currentPassInput = it },
                        label = { Text("Current Password", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (currentPassVis) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { currentPassVis = !currentPassVis }) {
                                Icon(
                                    imageVector = if (currentPassVis) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle visibility"
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                    )

                    OutlinedTextField(
                        value = newPassInput,
                        onValueChange = { newPassInput = it },
                        label = { Text("New Password", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (newPassVis) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { newPassVis = !newPassVis }) {
                                Icon(
                                    imageVector = if (newPassVis) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle visibility"
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                    )

                    OutlinedTextField(
                        value = confirmPassInput,
                        onValueChange = { confirmPassInput = it },
                        label = { Text("Confirm New Password", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (confirmPassVis) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPassVis = !confirmPassVis }) {
                                Icon(
                                    imageVector = if (confirmPassVis) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle visibility"
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentPassInput != savedPass) {
                            Toast.makeText(context, "Incorrect current account password!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPassInput.trim().isEmpty()) {
                            Toast.makeText(context, "New security password cannot be empty!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPassInput != confirmPassInput) {
                            Toast.makeText(context, "Mismatched new password confirmations!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        showConfirmSubmitChange = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Change Password", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isCurrentPassDisabled = authPrefs.getBoolean("password_disabled_$userId", false)
                    if (!isCurrentPassDisabled) {
                        TextButton(
                            onClick = {
                                if (currentPassInput != savedPass) {
                                    Toast.makeText(context, "Verify current password first!", Toast.LENGTH_SHORT).show()
                                    return@TextButton
                                }
                                showConfirmRemovePassword = true
                            }
                        ) {
                            Text("Remove Password", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(
                            onClick = {
                                authPrefs.edit().putBoolean("password_disabled_$userId", false).apply()
                                Toast.makeText(context, "Password protection required.", Toast.LENGTH_SHORT).show()
                                showPasswordChangeDialog = false
                            }
                        ) {
                            Text("Enable Password", color = if (isDark) BrandPrimary else Color(0xFF0F1B6B), fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { showPasswordChangeDialog = false }) {
                        Text("Cancel", color = secondaryText)
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = cardBg
        )

        GlobalConfirmationModal(
            show = showConfirmSubmitChange,
            title = "Are you sure?",
            message = "Confirm updating your account's primary protection security password?",
            confirmButtonColor = BrandPrimary,
            isDark = isDark,
            onConfirm = {
                showConfirmSubmitChange = false
                val uId = user?.userId ?: ""
                authPrefs.edit().putString("password_$uId", newPassInput).putBoolean("password_disabled_$uId", false).apply()
                Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                showPasswordChangeDialog = false
            },
            onCancel = { showConfirmSubmitChange = false }
        )

        GlobalConfirmationModal(
            show = showConfirmRemovePassword,
            title = "Are you sure?",
            message = "Deactivating security password will enable password-less system entry. Confirm removing security requirements?",
            confirmButtonColor = Color(0xFFEF4444),
            isDark = isDark,
            onConfirm = {
                showConfirmRemovePassword = false
                authPrefs.edit().putBoolean("password_disabled_$userId", true).apply()
                Toast.makeText(context, "Password deactivated! Password-less entry is enabled.", Toast.LENGTH_LONG).show()
                showPasswordChangeDialog = false
            },
            onCancel = { showConfirmRemovePassword = false }
        )
    }

    // 2. CONFIGURE VERIFICATION LOCK PIN DIALOG
    if (showPinChangeDialog) {
        var currentPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }

        val savedPin = remember { authPrefs.getString("app_security_pin", "") ?: "" }
        val hasExistingPin = savedPin.isNotEmpty()

        AlertDialog(
            onDismissRequest = { showPinChangeDialog = false },
            title = {
                Text(
                    text = if (hasExistingPin) "Modify Security PIN" else "Configure Unlock PIN",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = primaryText
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Establish a 4-digit numeric code to protect your ledger reports on system boot.",
                        fontSize = 12.sp,
                        color = secondaryText
                    )

                    if (hasExistingPin) {
                        OutlinedTextField(
                            value = currentPinInput,
                            onValueChange = { if (it.length <= 4) currentPinInput = it },
                            label = { Text("Current PIN (4 digits)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                        )
                    }

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4) newPinInput = it },
                        label = { Text("New PIN (4 digits)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                    )

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { if (it.length <= 4) confirmPinInput = it },
                        label = { Text("Confirm PIN (4 digits)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (hasExistingPin && currentPinInput != savedPin) {
                            Toast.makeText(context, "Incorrect current passcode!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPinInput.length != 4 || confirmPinInput.length != 4) {
                            Toast.makeText(context, "PIN code must be exactly 4 digits!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPinInput != confirmPinInput) {
                            Toast.makeText(context, "Confirm passcodes do not match!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        authPrefs.edit().putString("app_security_pin", newPinInput).apply()
                        Toast.makeText(context, "Lock PIN established successfully! 🔒", Toast.LENGTH_SHORT).show()
                        showPinChangeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save PIN", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                Row {
                    if (hasExistingPin) {
                        TextButton(
                            onClick = {
                                authPrefs.edit().remove("app_security_pin").apply()
                                Toast.makeText(context, "Passcode lock deactivated", Toast.LENGTH_SHORT).show()
                                showPinChangeDialog = false
                            }
                        ) {
                            Text("Remove PIN", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { showPinChangeDialog = false }) {
                        Text("Cancel", color = secondaryText)
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = cardBg
        )
    }

    // 3. BACKUP PROFILE / APPLICATION PROMPT
    if (showBackupDialog) {
        var onlyUserBackup by remember { mutableStateOf(true) }
        
        // Manual backup filter checklist states
        var selectIncome by remember { mutableStateOf(true) }
        var selectExpense by remember { mutableStateOf(true) }
        var selectInvestment by remember { mutableStateOf(true) }
        var selectBudgetGoal by remember { mutableStateOf(true) }

        // Load all budget & goal IDs initially
        val allBudgetAndGoalIds = remember(userBudgets, userGoals) {
            (userBudgets.map { it.id } + userGoals.map { it.id }).toSet()
        }
        var selectedIdsState by remember { mutableStateOf<Set<Long>>(emptySet()) }
        LaunchedEffect(allBudgetAndGoalIds) {
            selectedIdsState = allBudgetAndGoalIds
        }

        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = {
                Text(
                    text = "Select Backup Type",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = primaryText
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Choose whether you wish to back up only your current active profile's ledger, or the entire application database.",
                        fontSize = 13.sp,
                        color = secondaryText
                    )

                    // Selection radio-look card options
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (onlyUserBackup) (if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)) else Color.Transparent)
                                .border(1f.dp, if (onlyUserBackup) (if (isDark) BrandPrimary else Color(0xFF0F1B6B)) else outlineBorder, RoundedCornerShape(12.dp))
                                .clickable { onlyUserBackup = true }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = onlyUserBackup,
                                onClick = { onlyUserBackup = true },
                                colors = RadioButtonDefaults.colors(selectedColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Current Active Profile Only", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryText)
                                Text("Custom manual filters and module checklist", fontSize = 11.sp, color = secondaryText)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (!onlyUserBackup) (if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)) else Color.Transparent)
                                .border(1f.dp, if (!onlyUserBackup) (if (isDark) BrandPrimary else Color(0xFF0F1B6B)) else outlineBorder, RoundedCornerShape(12.dp))
                                .clickable { onlyUserBackup = false }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !onlyUserBackup,
                                onClick = { onlyUserBackup = false },
                                colors = RadioButtonDefaults.colors(selectedColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Full Platform Application Database", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryText)
                                Text("Includes all registered users, settings & catalogs", fontSize = 11.sp, color = secondaryText)
                            }
                        }
                    }

                    // Checklist view if current active profile custom backup is chosen
                    if (onlyUserBackup) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Select Modules to Include:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                            )

                            // Income module filter selection
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectIncome = !selectIncome },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectIncome,
                                    onCheckedChange = { selectIncome = it },
                                    colors = CheckboxDefaults.colors(checkedColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Income Records", fontSize = 13.sp, color = primaryText)
                            }

                            // Expense module filter selection
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectExpense = !selectExpense },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectExpense,
                                    onCheckedChange = { selectExpense = it },
                                    colors = CheckboxDefaults.colors(checkedColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Expense Records", fontSize = 13.sp, color = primaryText)
                            }

                            // Investment module filter selection
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectInvestment = !selectInvestment },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectInvestment,
                                    onCheckedChange = { selectInvestment = it },
                                    colors = CheckboxDefaults.colors(checkedColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Investment Records", fontSize = 13.sp, color = primaryText)
                            }

                            // Budget & Goal module filter selection
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectBudgetGoal = !selectBudgetGoal },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectBudgetGoal,
                                    onCheckedChange = { selectBudgetGoal = it },
                                    colors = CheckboxDefaults.colors(checkedColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Budget & Goal Records", fontSize = 13.sp, color = primaryText)
                            }

                            // If 'Budget & Goal' is checked, dynamically load and display a sub-checklist of specific budget/goal entries
                            if (selectBudgetGoal && (userBudgets.isNotEmpty() || userGoals.isNotEmpty())) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Select Specific Budgets/Goals to include:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = secondaryText
                                    )

                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 140.dp)
                                            .border(1.dp, outlineBorder, RoundedCornerShape(8.dp))
                                            .padding(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(userBudgets) { b ->
                                            val isChecked = selectedIdsState.contains(b.id)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedIdsState = if (isChecked) selectedIdsState - b.id else selectedIdsState + b.id
                                                    }
                                                    .padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = {
                                                        selectedIdsState = if (isChecked) selectedIdsState - b.id else selectedIdsState + b.id
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Budget: ${b.name}", fontSize = 12.sp, color = primaryText)
                                            }
                                        }

                                        items(userGoals) { g ->
                                            val isChecked = selectedIdsState.contains(g.id)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedIdsState = if (isChecked) selectedIdsState - g.id else selectedIdsState + g.id
                                                    }
                                                    .padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = {
                                                        selectedIdsState = if (isChecked) selectedIdsState - g.id else selectedIdsState + g.id
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Goal: ${g.name}", fontSize = 12.sp, color = primaryText)
                                            }
                                        }
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
                        showBackupDialog = false
                        if (!onlyUserBackup) {
                            // Full backup ignores selections to ensure disaster recovery
                            askAndDownloadBackup(onlyActive = false, "kinetic_full_database_backup") { fileJson ->
                                generatedBackupJsonText = fileJson
                                showBackupShareSuccessDialog = true
                            }
                        } else {
                            // Custom Manual Backup
                            val selectedModules = mutableSetOf<String>()
                            if (selectIncome) selectedModules.add("Income")
                            if (selectExpense) selectedModules.add("Expense")
                            if (selectInvestment) selectedModules.add("Investment")
                            if (selectBudgetGoal) selectedModules.add("Budget & Goal")
                            
                            askAndDownloadBackup(
                                onlyActive = true,
                                defaultName = "kinetic_custom_manual_backup",
                                selectedModules = selectedModules,
                                selectedBudgetIds = selectedIdsState
                            ) { fileJson ->
                                generatedBackupJsonText = fileJson
                                showBackupShareSuccessDialog = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Generate & Save", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Cancel", color = secondaryText)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = cardBg
        )
    }

    // 4. BACKUP SUCCESSFUL & SHARE DIALOGUE PANEL
    if (showBackupShareSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showBackupShareSuccessDialog = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Backup Ready! ✅",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = primaryText
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Your secure encrypted backup was generated and recorded to physical storage successfully.",
                        fontSize = 13.sp,
                        color = secondaryText,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "You can share this JSON file via Whatsapp, Gmail, or drive options below to ensure maximum preservation safety.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryText,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            triggerBackupShare(context, "kinetic_recovery_ledger.json", generatedBackupJsonText)
                            showBackupShareSuccessDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Share File (Whatsapp / Gmail)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    TextButton(
                        onClick = { showBackupShareSuccessDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dismiss Panel", fontWeight = FontWeight.Bold, color = secondaryText, textAlign = TextAlign.Center)
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = cardBg
        )
    }

    // 5. RESTORE BACKUP DIALOG FLOW
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = {
                Text(
                    text = "Upload Backup File",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = primaryText
                )
            },
            text = {
                Text(
                    text = "Verify and locate your saved '.json' backup file in local storage directory. This operation restores previous transaction logs, category tables, and security passcodes.",
                    fontSize = 13.sp,
                    color = secondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            restoreFileLauncher.launch("*/*")
                            showRestoreDialog = false
                        } catch (e: Exception) {
                            Toast.makeText(context, "Launcher error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Select Backup File", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel", color = secondaryText)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = cardBg
        )
    }

    // 6. CAPTCHA-VERIFIED MANDATORY BACKUP CLEAR DATA WIPER PROMPT
    if (showWipeOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showWipeOptionsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                    Text(
                        text = "Force Ledger Storage Wipe",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Red
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "IMPORTANT: To prevent accident data losses, a backup is STRICTLY mandatory. We will generate and export safety backup data before proceeding with the deletions.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Data To Clear (Select Scope):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = primaryText
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!clearAllSelected.value) {
                                        clearIncomeSelected.value = !clearIncomeSelected.value
                                    }
                                }
                        ) {
                            Checkbox(
                                checked = clearIncomeSelected.value || clearAllSelected.value,
                                onCheckedChange = { if (!clearAllSelected.value) clearIncomeSelected.value = it },
                                enabled = !clearAllSelected.value,
                                colors = CheckboxDefaults.colors(checkedColor = Color.Red)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear All Income Transactions", fontSize = 13.sp, color = primaryText)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!clearAllSelected.value) {
                                        clearExpenseSelected.value = !clearExpenseSelected.value
                                    }
                                }
                        ) {
                            Checkbox(
                                checked = clearExpenseSelected.value || clearAllSelected.value,
                                onCheckedChange = { if (!clearAllSelected.value) clearExpenseSelected.value = it },
                                enabled = !clearAllSelected.value,
                                colors = CheckboxDefaults.colors(checkedColor = Color.Red)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear All Expense Transactions", fontSize = 13.sp, color = primaryText)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!clearAllSelected.value) {
                                        clearInvestmentSelected.value = !clearInvestmentSelected.value
                                    }
                                }
                        ) {
                            Checkbox(
                                checked = clearInvestmentSelected.value || clearAllSelected.value,
                                onCheckedChange = { if (!clearAllSelected.value) clearInvestmentSelected.value = it },
                                enabled = !clearAllSelected.value,
                                colors = CheckboxDefaults.colors(checkedColor = Color.Red)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Investment Data", fontSize = 13.sp, color = primaryText)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!clearAllSelected.value) {
                                        clearBudgetGoalSelected.value = !clearBudgetGoalSelected.value
                                    }
                                }
                        ) {
                            Checkbox(
                                checked = clearBudgetGoalSelected.value || clearAllSelected.value,
                                onCheckedChange = { if (!clearAllSelected.value) clearBudgetGoalSelected.value = it },
                                enabled = !clearAllSelected.value,
                                colors = CheckboxDefaults.colors(checkedColor = Color.Red)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Budget & Goal Data", fontSize = 13.sp, color = primaryText)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clearAllSelected.value = !clearAllSelected.value
                                }
                        ) {
                            Checkbox(
                                checked = clearAllSelected.value,
                                onCheckedChange = { clearAllSelected.value = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color.Red)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear All Data", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. Clear status of Mandatory Backup
                    Button(
                        onClick = {
                            val defaultPrefix = "kinetic_wipe_safety"
                            askAndDownloadBackup(onlyActive = true, defaultPrefix) { jsonText ->
                                generatedBackupJsonText = jsonText
                                backupPerformedBeforeWipe = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (backupPerformedBeforeWipe) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                            contentColor = if (backupPerformedBeforeWipe) Color(0xFF16A34A) else Color(0xFFD97706)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (backupPerformedBeforeWipe) Icons.Default.CheckCircle else Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (backupPerformedBeforeWipe) "Safety Backup Created! ✅" else "1. Tap to Name & Create Safety Backup *",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (backupPerformedBeforeWipe) {
                        Text(
                            text = "2. Security authorization required to commit deletions:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )

                        // 3. Password Check
                        OutlinedTextField(
                            value = enteredWipePassword,
                            onValueChange = { enteredWipePassword = it },
                            label = { Text("Account Secret Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Red)
                        )

                        // 4. Captcha Solve
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFF1E293D) else Color(0xFFF1F5F9))
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Solve: $mathCaptchaNum1 + $mathCaptchaNum2 =",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                )
                            }

                            OutlinedTextField(
                                value = enteredWipeCaptchaAnswer,
                                onValueChange = { enteredWipeCaptchaAnswer = it },
                                label = { Text("Result") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Red)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!backupPerformedBeforeWipe) {
                            Toast.makeText(context, "Kindly perform the mandatory security backup first!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        val anySel = clearIncomeSelected.value || clearExpenseSelected.value || clearInvestmentSelected.value || clearBudgetGoalSelected.value || clearAllSelected.value
                        if (!anySel) {
                            Toast.makeText(context, "Please select at least one dataset to clear!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Check password matching
                        val activeUserId = user?.userId ?: "default_user"
                        val expectedPass = authPrefs.getString("password_$activeUserId", "password") ?: "password"
                        if (enteredWipePassword.trim() != expectedPass) {
                            Toast.makeText(context, "Incorrect account password authorization!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Check Captcha solution
                        val correctSum = mathCaptchaNum1 + mathCaptchaNum2
                        val answeredSum = enteredWipeCaptchaAnswer.trim().toIntOrNull()
                        if (answeredSum == null || answeredSum != correctSum) {
                            Toast.makeText(context, "Captcha solution is incorrect!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Everything valid! Execute granular database erasure
                        viewModel.executeGranularDatabaseWipe(
                            clearIncome = clearIncomeSelected.value || clearAllSelected.value,
                            clearExpense = clearExpenseSelected.value || clearAllSelected.value,
                            clearInvestment = clearInvestmentSelected.value || clearAllSelected.value,
                            clearBudgetGoal = clearBudgetGoalSelected.value || clearAllSelected.value,
                            clearAll = clearAllSelected.value
                        ) {
                            showWipeOptionsDialog = false
                            showClearDataSuccessDialog.value = true // Show successes share popup
                            if (clearAllSelected.value) {
                                onLogout()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(10.dp),
                    enabled = backupPerformedBeforeWipe && enteredWipePassword.isNotBlank() && enteredWipeCaptchaAnswer.isNotBlank()
                ) {
                    Text("Confirm Erase", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeOptionsDialog = false }) {
                    Text("Cancel", color = secondaryText)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = cardBg
        )
    }

    // 6b. GRANULAR CLEAR DATA DOWNLOAD SUCCESS POPUP
    if (showClearDataSuccessDialog.value) {
        AlertDialog(
            onDismissRequest = { showClearDataSuccessDialog.value = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Backup Downloaded Successful",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = primaryText
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Your granular backup has been successfully generated and saved.",
                        fontSize = 13.sp,
                        color = secondaryText,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Location: Internal Storage > Downloads",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "File Name: ${clearDataDownloadedFileName.value}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryText,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            triggerBackupShare(context, clearDataDownloadedFileName.value, generatedBackupJsonText)
                            showClearDataSuccessDialog.value = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Share File (WhatsApp / Gmail)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    TextButton(
                        onClick = { showClearDataSuccessDialog.value = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dismiss", fontWeight = FontWeight.Bold, color = secondaryText, textAlign = TextAlign.Center)
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = cardBg
        )
    }

    // 7. FILE NAME PROMPT DIALOGUE WITH TIMESTAMP
    if (showFileNamePromptDialog) {
        var prefixInput by remember { mutableStateOf(customFileNamePrefix) }
        AlertDialog(
            onDismissRequest = { showFileNamePromptDialog = false },
            title = {
                Text(
                    text = "Name Backup File",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = primaryText
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Enter a prefix name for your backup. The current timestamp will automatically be appended to guarantee unique files.",
                        fontSize = 12.sp,
                        color = secondaryText
                    )
                    OutlinedTextField(
                        value = prefixInput,
                        onValueChange = { prefixInput = it },
                        placeholder = { Text("kinetic_backup") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                            cursorColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = prefixInput.trim()
                        val finalPrefix = if (input.isEmpty()) "kinetic_backup" else input
                        showFileNamePromptDialog = false
                        onFileNameFilenameConfirmed?.invoke(finalPrefix)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFileNamePromptDialog = false }) {
                    Text("Cancel", color = secondaryText)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = cardBg
        )
    }

    // 8. DATA RESTORE WIZARD (MERGE WITH DUPLICATE RESOLUTIONS OR CLEAN REPLACE FLOWS)
    if (showRestoreWizardDialog && parsedBackupResult != null) {
        val result = parsedBackupResult!!
        var wizardStep by remember { mutableStateOf("choose_mode") } // "choose_mode" or "resolve_duplicates"
        val hasDupes = result.duplicateTransactions.isNotEmpty() || result.duplicateAssets.isNotEmpty()

        if (wizardStep == "choose_mode") {
            var selectedRestoreMode by remember { mutableStateOf("merge") } // "merge" or "overwrite"

            AlertDialog(
                onDismissRequest = { showRestoreWizardDialog = false },
                title = {
                    Text(
                        text = "Data Restore Strategy",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = primaryText
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "How would you like to apply the backup contents to the local database?",
                            fontSize = 13.sp,
                            color = secondaryText
                        )

                        // 1. Merge Mode Selective Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedRestoreMode == "merge") (if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)) else Color.Transparent)
                                .border(1.dp, if (selectedRestoreMode == "merge") (if (isDark) BrandPrimary else Color(0xFF0F1B6B)) else outlineBorder, RoundedCornerShape(12.dp))
                                .clickable { selectedRestoreMode = "merge" }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRestoreMode == "merge",
                                onClick = { selectedRestoreMode = "merge" },
                                colors = RadioButtonDefaults.colors(selectedColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Merge with Existing Data", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryText)
                                Text("Appends incoming entries. If duplicate identity overlap is found, you can filter them.", fontSize = 11.sp, color = secondaryText)
                            }
                        }

                        // 2. Overwrite Mode Selective Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedRestoreMode == "overwrite") (if (isDark) Color(0xFF291E1E) else Color(0xFFFEF2F2)) else Color.Transparent)
                                .border(1.dp, if (selectedRestoreMode == "overwrite") Color.Red else outlineBorder, RoundedCornerShape(12.dp))
                                .clickable { selectedRestoreMode = "overwrite" }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRestoreMode == "overwrite",
                                onClick = { selectedRestoreMode = "overwrite" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color.Red)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Overwrite (Clear & Restore)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryText)
                                Text("Wipes matching live databases entirely before loading values. Safety download of current live logs is provided.", fontSize = 11.sp, color = secondaryText)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedRestoreMode == "merge") {
                                if (hasDupes) {
                                    wizardStep = "resolve_duplicates"
                                } else {
                                    viewModel.executeMergeRestore(result, keepAll = true) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        showRestoreWizardDialog = false
                                    }
                                }
                            } else {
                                showRestoreWizardDialog = false
                                showSafetyBackupPromptDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreWizardDialog = false }) {
                        Text("Cancel", color = secondaryText)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = cardBg
            )
        } else if (wizardStep == "resolve_duplicates") {
            AlertDialog(
                onDismissRequest = { showRestoreWizardDialog = false },
                title = {
                    Text(
                        text = "Duplicate Entries Found ⚠️",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = primaryText
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "We found ${result.duplicateTransactions.size} overlapping transaction entries and ${result.duplicateAssets.size} duplicate asset records. Let us know how to handle these duplicates:",
                            fontSize = 12.sp,
                            color = secondaryText
                        )

                        // Beautiful lists
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .border(1.dp, outlineBorder, RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF0C101A) else Color(0xFFF8FAFC))
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            result.duplicateTransactions.forEach { tx ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tx: ${tx.title}", fontSize = 11.sp, color = primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    val currencySymbol = if (currencyUnit.startsWith("Dollar")) "$" else if (currencyUnit.startsWith("Euro")) "€" else "₹"
                                    Text("${if (tx.type == "Income") "+" else "-"}$currencySymbol${tx.amount}", fontSize = 11.sp, color = if (tx.type == "Income") Color(0xFF10B981) else Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                }
                            }
                            result.duplicateAssets.forEach { asset ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Asset: ${asset.name}", fontSize = 11.sp, color = primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    val currencySymbol = if (currencyUnit.startsWith("Dollar")) "$" else if (currencyUnit.startsWith("Euro")) "€" else "₹"
                                    Text("$currencySymbol${asset.amountInvested}", fontSize = 11.sp, color = if (isDark) BrandPrimary else Color(0xFF0F1B6B), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Vertical choice buttons
                        Button(
                            onClick = {
                                viewModel.executeMergeRestore(result, keepAll = true) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    showRestoreWizardDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Keep All Data", fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                viewModel.executeMergeRestore(result, keepAll = false) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    showRestoreWizardDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove / Skip Overlap Duplicates", fontSize = 12.sp, color = primaryText)
                        }

                        Button(
                            onClick = {
                                showRestoreWizardDialog = false
                                showSafetyBackupPromptDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove Current Data & Restore New", fontSize = 12.sp, color = Color.Red)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showRestoreWizardDialog = false }) {
                        Text("Cancel", color = secondaryText)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = cardBg
            )
        }
    }

    // 9. OVERWRITE RESTORE SAFETY PROMPT
    if (showSafetyBackupPromptDialog && parsedBackupResult != null) {
        val result = parsedBackupResult!!
        AlertDialog(
            onDismissRequest = { showSafetyBackupPromptDialog = false },
            title = {
                Text(
                    text = "Backup Current Data First?",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = primaryText
                )
            },
            text = {
                Text(
                    text = "You are about to wipe live records on this database and overwrite them with this backup. This cannot be undone. Would you like to create and download a backup of your current live data before clearing?",
                    fontSize = 13.sp,
                    color = secondaryText
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showSafetyBackupPromptDialog = false
                            askAndDownloadBackup(onlyActive = result.isSingleProfile, defaultName = "safety_ledger_save") {
                                viewModel.executeOverwriteRestore(result) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Yes, Download Safety Backup First", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showSafetyBackupPromptDialog = false
                            viewModel.executeOverwriteRestore(result) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("No, Erase & Restore Instantly", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = { showSafetyBackupPromptDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel", color = secondaryText, textAlign = TextAlign.Center)
                    }
                }
            },
            dismissButton = {}
        )
    }

    // 8. PRIVACY POLICY DIALOG
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = {
                Text(
                    text = "Privacy Policy & Trust Laws",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = primaryText
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Kinetic Trust operates on full client-side local Sandbox database structures. High protection vectors ensure no unauthorized external indexing has access to transaction diaries recorded in this ledger application.",
                        fontSize = 12.sp,
                        color = secondaryText
                    )
                    Text(
                        text = "Cryptographic logs of the passcode lock secure physical database storage instances on device directories. No logs are exported to external networks.",
                        fontSize = 12.sp,
                        color = secondaryText
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyPolicyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                ) {
                    Text("Acknowledge", color = Color.White)
                }
            }
        )
    }

    // 9. SWITCH USER COMPOSABLE DIALOG
    if (showSwitchUserDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchUserDialog = false },
            title = {
                Text("Switch Active Profile", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primaryText)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Choose one of the active profile sessions in storage to access their workspace:", fontSize = 12.sp, color = secondaryText)
                    
                    if (usersList.isEmpty()) {
                        Text("No other active profiles in storage. Please log out and sign up a new account.", fontStyle = FontStyle.Italic, fontSize = 12.sp, color = Color.Red)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            usersList.forEach { u ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (u.userId == user?.userId) (if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)) else Color.Transparent)
                                        .border(1.dp, if (u.userId == user?.userId) (if (isDark) BrandPrimary else Color(0xFF0F1B6B)) else outlineBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.loginUser(u.userId) {
                                                showSwitchUserDialog = false
                                                Toast.makeText(context, "Switched workspace to ${u.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(u.name.take(2).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(u.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryText)
                                        Text("@${u.userId}", fontSize = 10.sp, color = secondaryText)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSwitchUserDialog = false }) {
                    Text("Close", color = secondaryText)
                }
            }
        )
    }
}

@Composable
fun SettingsListItem(
    icon: ImageVector,
    title: String,
    description: String,
    isDark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    onClick: (() -> Unit)? = null,
    control: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDark) BrandPrimary else Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = primaryText
                )
                Text(
                    text = description,
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    color = secondaryText
                )
            }
        }
        control()
    }
}

private fun saveToPublicDownloads(context: android.content.Context, filename: String, content: String): Boolean {
    return try {
        val resolver = context.contentResolver
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                true
            } else {
                false
            }
        } else {
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            val targetFile = java.io.File(downloadDir, filename)
            targetFile.writeText(content)
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun triggerBackupShare(context: android.content.Context, filename: String, content: String) {
    try {
        val file = java.io.File(context.getExternalFilesDir(null), filename)
        file.writeText(content)
        
        // Share intent
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Kinetic Trust Ledger Backup")
            putExtra(android.content.Intent.EXTRA_TEXT, content)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Recovery Backup"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Sharing failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
    }
}
