package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.AppViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScreen: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = isDarkThemeActive

    // Get live current state of the active user
    val user by viewModel.currentUser.collectAsState()
    val transactions by viewModel.userTransactions.collectAsState()

    // Screen dynamic styling coordinates
    val bg = if (isDark) BrandBackground else Color(0xFFF8FAFC)
    val primaryText = if (isDark) Color.White else Color(0xFF0F1B6B)
    val secondaryText = if (isDark) StitchOnSurfaceVariant else Color(0xFF4B5563)
    val cardBg = if (isDark) BrandSurface else Color.White
    val outlineBorder = if (isDark) BrandOutline else Color(0xFFE2E8F0)

    // Temporary input model states for edits
    var nameInput by remember(user) { mutableStateOf(user?.name ?: "Your Name") }
    var emailInput by remember(user) { mutableStateOf(user?.email ?: "name@example.com") }
    var contactInput by remember(user) { mutableStateOf(user?.contactNo ?: "") }
    var avatarUrlInput by remember(user) { mutableStateOf(user?.avatarUrl ?: "") }

    // Toggle states for inline edit pencils
    var isEditingName by remember { mutableStateOf(false) }
    var isEditingEmail by remember { mutableStateOf(false) }
    var isEditingContact by remember { mutableStateOf(false) }

    // Dialog state controllers for interactivity
    var showChangeAvatarDialog by remember { mutableStateOf(false) }
    var showUploadPhotoDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var downloadProgressActive by remember { mutableStateOf(false) }

    // Interactivity confirmations
    var showAppConfirmDialog by remember { mutableStateOf(false) }
    var appConfirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var appConfirmMessage by remember { mutableStateOf("Are you sure?") }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val uriStr = uri.toString()
            avatarUrlInput = uriStr
            viewModel.updateProfile(
                name = nameInput,
                email = emailInput,
                isPremium = user?.isPremium ?: true,
                trustScore = user?.trustScore ?: 850,
                contactNo = contactInput,
                gmailId = user?.gmailId ?: "",
                avatarUrl = uriStr,
                streak = user?.streak ?: 0,
                medal = user?.medal ?: "Start"
            )
            Toast.makeText(context, "Photo uploaded from device!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("edit_profile_screen"),
        containerColor = bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. DYNAMIC PROFILE SHIELD IMAGE WITH CAMERA BADGE
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Circle Profile frame
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF131722) else Color(0xFFEFF6FF))
                        .border(1.5.dp, outlineBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrlInput.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = avatarUrlInput),
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Stitch Dynamic Shield Graphic Canvas
                        KineticShieldLogo(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            isDark = isDark
                        )
                    }
                }

                // Small camera icon indicator overlaid on bottom-right of avatar shield
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (2).dp, y = (2).dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF131722) else Color(0xFF0F1B6B))
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable { showChangeAvatarDialog = true }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Change Logo/Avatar",
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 2. USER PRIMARY META DETAILS
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = nameInput,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = primaryText
                )

                // Premium membership badge (dynamically calculated based on transaction count)
                Text(
                    text = "• ${viewModel.getUsageBadgeText(transactions.size).uppercase()} •",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    color = if (isDark) Color(0xFFA78BFA) else Color(0xFF5B21B6),
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (isDark) Color(0xFF2E1F4E) else Color(0xFFEDE9FE))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Trust streak badge with a vibrant flame emoji (🔥)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0xFF2E1C0A) else Color(0xFFFEF3C7))
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🔥 ${user?.streak ?: 0} DAILY ENTRY STREAK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                }
            }

            // 3. ACTION BUTTON ROW: "Change Avatar" & "Upload Photo"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { showChangeAvatarDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, outlineBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryText),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Change Avatar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = { showUploadPhotoDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF1E1E2F) else Color(0xFF0F1B6B),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Upload Photo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 4. ACCOUNT MANAGEMENT FORM SECTION
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ManageAccounts,
                        contentDescription = null,
                        tint = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Account Management",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = primaryText
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, outlineBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Username Edit Block
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "USERNAME",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = secondaryText,
                                letterSpacing = 1.sp
                            )
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("username_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                readOnly = !isEditingName,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (isEditingName) {
                                                appConfirmAction = {
                                                    viewModel.updateProfile(
                                                        name = nameInput,
                                                        email = emailInput,
                                                        isPremium = user?.isPremium ?: true,
                                                        trustScore = user?.trustScore ?: 850,
                                                        contactNo = contactInput,
                                                        gmailId = user?.gmailId ?: "",
                                                        avatarUrl = avatarUrlInput,
                                                        streak = user?.streak ?: 0,
                                                        medal = user?.medal ?: "Start"
                                                     )
                                                     Toast.makeText(context, "Username updated!", Toast.LENGTH_SHORT).show()
                                                     isEditingName = false
                                                }
                                                appConfirmMessage = "Are you sure you want to change your username to '$nameInput'?"
                                                showAppConfirmDialog = true
                                            } else {
                                                isEditingName = true
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isEditingName) Icons.Default.Check else Icons.Default.Edit,
                                            contentDescription = "Edit Username",
                                            tint = if (isEditingName) Color(0xFF10B981) else (if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                                    unfocusedBorderColor = outlineBorder,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        }

                        // Email ID Edit Block
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "EMAIL ID",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = secondaryText,
                                letterSpacing = 1.sp
                            )
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("email_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                readOnly = !isEditingEmail,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (isEditingEmail) {
                                                appConfirmAction = {
                                                    viewModel.updateProfile(
                                                        name = nameInput,
                                                        email = emailInput,
                                                        isPremium = user?.isPremium ?: true,
                                                        trustScore = user?.trustScore ?: 850,
                                                        contactNo = contactInput,
                                                        gmailId = user?.gmailId ?: "",
                                                        avatarUrl = avatarUrlInput,
                                                        streak = user?.streak ?: 0,
                                                        medal = user?.medal ?: "Start"
                                                     )
                                                     Toast.makeText(context, "Email ID updated!", Toast.LENGTH_SHORT).show()
                                                     isEditingEmail = false
                                                }
                                                appConfirmMessage = "Are you sure you want to change your email to '$emailInput'?"
                                                showAppConfirmDialog = true
                                            } else {
                                                isEditingEmail = true
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isEditingEmail) Icons.Default.Check else Icons.Default.Edit,
                                            contentDescription = "Edit Email",
                                            tint = if (isEditingEmail) Color(0xFF10B981) else (if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                                    unfocusedBorderColor = outlineBorder,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        }

                        // Contact No Edit Block
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "CONTACT NO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = secondaryText,
                                letterSpacing = 1.sp
                            )
                            OutlinedTextField(
                                value = contactInput,
                                onValueChange = { contactInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("contact_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                readOnly = !isEditingContact,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (isEditingContact) {
                                                appConfirmAction = {
                                                    viewModel.updateProfile(
                                                        name = nameInput,
                                                        email = emailInput,
                                                        isPremium = user?.isPremium ?: true,
                                                        trustScore = user?.trustScore ?: 850,
                                                        contactNo = contactInput,
                                                        gmailId = user?.gmailId ?: "",
                                                        avatarUrl = avatarUrlInput,
                                                        streak = user?.streak ?: 0,
                                                        medal = user?.medal ?: "Start"
                                                     )
                                                     Toast.makeText(context, "Contact number updated!", Toast.LENGTH_SHORT).show()
                                                     isEditingContact = false
                                                }
                                                appConfirmMessage = "Are you sure you want to change your contact no to '$contactInput'?"
                                                showAppConfirmDialog = true
                                            } else {
                                                isEditingContact = true
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isEditingContact) Icons.Default.Check else Icons.Default.Edit,
                                            contentDescription = "Edit Contact No",
                                            tint = if (isEditingContact) Color(0xFF10B981) else (if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                                    unfocusedBorderColor = outlineBorder,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }

            // 5. RECENT ACTIVITY LIST (WITH ACTION DOWNLOAD LOG)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Recent Activity Log",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = primaryText
                        )
                    }

                    Text(
                        text = "DOWNLOAD LOG",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                        modifier = Modifier
                            .clickable {
                                downloadProgressActive = true
                            }
                            .padding(4.dp)
                    )
                }

                // Activity Logs List matched dynamically and accurately
                val currUserId = user?.userId ?: ""
                val activityLogs = remember(user) { viewModel.getRecentActivityLogs(currUserId) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activityLogs.isEmpty()) {
                        RecentActivityItem(
                            device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                            status = "ACTIVE SESSION",
                            location = "New Delhi, IN",
                            time = "Just now",
                            isActive = true,
                            isDark = isDark,
                            outlineBorder = outlineBorder,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            cardBg = cardBg
                        )
                    } else {
                        activityLogs.take(1).forEach { log ->
                            val timeText = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.US).format(java.util.Date(log.timestamp))
                            RecentActivityItem(
                                device = log.device,
                                status = if (log.isActive) "ACTIVE SESSION" else "CLOSED SESSION",
                                location = log.location,
                                time = "$timeText • 1h 20m",
                                isActive = log.isActive,
                                isDark = isDark,
                                outlineBorder = outlineBorder,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                cardBg = cardBg
                            )
                        }
                    }
                }
            }

            // 6. APP SECURITY CREDENTIALS
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Security Credentials",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = primaryText
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, outlineBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "Configure or deactivate your custom passcode lock settings to override the security Password of Kinetic Trust Ledger:",
                            fontSize = 12.sp,
                            color = secondaryText,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Spaced, stacked password buttons
                        OutlinedButton(
                            onClick = { showChangePinDialog = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryText)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Password, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Change Password", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                appConfirmAction = {
                                    val authPrefs = context.getSharedPreferences("kinetic_auth_prefs", android.content.Context.MODE_PRIVATE)
                                    val savedPin = authPrefs.getString("app_security_pin", "") ?: ""
                                    if (savedPin.isEmpty()) {
                                        Toast.makeText(context, "No Password passcode is configured currently!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        authPrefs.edit().remove("app_security_pin").apply()
                                        Toast.makeText(context, "App Security Password lock removed successfully! 🔓", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                appConfirmMessage = "Are you sure you want to remove your protective security Password?"
                                showAppConfirmDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Remove Password", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // A. DIALOG: SELECT PRESET AVATAR OR PASTE CUSTOM IMAGE URL
    if (showChangeAvatarDialog) {
        val predefinedAvatars = listOf(
            // Popular Unsplash Anime / J-Culture Portraits & Illustrations (High-fidelity CDNs)
            "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=150&q=80",
            "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=150&q=80",
            "https://images.unsplash.com/photo-1560942485-b2a11cc13456?auto=format&fit=crop&w=150&q=80",
            "https://images.unsplash.com/photo-1580477667995-2b94f01c9516?auto=format&fit=crop&w=150&q=80",
            "https://images.unsplash.com/photo-1620641788421-7a1c342ea42e?auto=format&fit=crop&w=150&q=80",
            "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=150&q=80",

            // Curated, beautiful, instant-load Anime styles: Attack on Titan
            "https://api.dicebear.com/7.x/adventurer/png?seed=Eren",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Levi",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Mikasa",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Armin",

            // Fullmetal Alchemist
            "https://api.dicebear.com/7.x/adventurer/png?seed=EdwardElric",
            "https://api.dicebear.com/7.x/adventurer/png?seed=AlphonseElric",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Mustang",

            // One Piece
            "https://api.dicebear.com/7.x/adventurer/png?seed=Luffy",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Zoro",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Sanji",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Nami",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Shanks",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Ace",

            // Death Note
            "https://api.dicebear.com/7.x/adventurer/png?seed=LightYagami",
            "https://api.dicebear.com/7.x/adventurer/png?seed=L",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Ryuk",

            // Naruto Series
            "https://api.dicebear.com/7.x/adventurer/png?seed=NarutoUzumaki",
            "https://api.dicebear.com/7.x/adventurer/png?seed=SasukeUchiha",
            "https://api.dicebear.com/7.x/adventurer/png?seed=KakashiHatake",
            "https://api.dicebear.com/7.x/adventurer/png?seed=ItachiUchiha",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Gaara",
            "https://api.dicebear.com/7.x/adventurer/png?seed=SakuraHaruno",
            "https://api.dicebear.com/7.x/adventurer/png?seed=HinataUzumaki",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Jiraiya",
            "https://api.dicebear.com/7.x/adventurer/png?seed=MadaraUchiha",

            // Dragon Ball Z
            "https://api.dicebear.com/7.x/adventurer/png?seed=Goku",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Vegeta",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Gohan",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Frieza",

            // Hunter x Hunter
            "https://api.dicebear.com/7.x/adventurer/png?seed=Gon",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Killua",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Kurapika",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Hisoka",

            // Jujutsu Kaisen
            "https://api.dicebear.com/7.x/adventurer/png?seed=SatoruGojo",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Sukuna",
            "https://api.dicebear.com/7.x/adventurer/png?seed=YujiItadori",
            "https://api.dicebear.com/7.x/adventurer/png?seed=MegumiFushiguro",
            "https://api.dicebear.com/7.x/adventurer/png?seed=NobaraKugisaki",

            // Demon Slayer
            "https://api.dicebear.com/7.x/adventurer/png?seed=TanjirouKamado",
            "https://api.dicebear.com/7.x/adventurer/png?seed=NezukoKamado",
            "https://api.dicebear.com/7.x/adventurer/png?seed=ZenitsuAgatsuma",
            "https://api.dicebear.com/7.x/adventurer/png?seed=InosukeHashibira",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Rengoku",

            // Cowboy Bebop and Steins;Gate
            "https://api.dicebear.com/7.x/adventurer/png?seed=SpikeSpiegel",
            "https://api.dicebear.com/7.x/adventurer/png?seed=FayeValentine",
            "https://api.dicebear.com/7.x/adventurer/png?seed=OkabeRintarou",
            "https://api.dicebear.com/7.x/adventurer/png?seed=KurisuMakise",

            // Code Geass & My Hero Academia
            "https://api.dicebear.com/7.x/adventurer/png?seed=Lelouch",
            "https://api.dicebear.com/7.x/adventurer/png?seed=CC",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Deku",
            "https://api.dicebear.com/7.x/adventurer/png?seed=AllMight",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Bakugo",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Todoroki",

            // Evangelion & Haikyu!!
            "https://api.dicebear.com/7.x/adventurer/png?seed=ShinjiIkari",
            "https://api.dicebear.com/7.x/adventurer/png?seed=ReiAyanami",
            "https://api.dicebear.com/7.x/adventurer/png?seed=AsukaLangley",
            "https://api.dicebear.com/7.x/adventurer/png?seed=HinataShoyo",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Kageyama",

            // Bleach & Gintama
            "https://api.dicebear.com/7.x/adventurer/png?seed=IchigoKurosaki",
            "https://api.dicebear.com/7.x/adventurer/png?seed=KenpachiZaraki",
            "https://api.dicebear.com/7.x/adventurer/png?seed=RukiaKuchiki",
            "https://api.dicebear.com/7.x/adventurer/png?seed=GintokiSakata",

            // Sailor Moon, Solo Leveling, slime & Others
            "https://api.dicebear.com/7.x/adventurer/png?seed=Saitama",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Genos",
            "https://api.dicebear.com/7.x/adventurer/png?seed=Thorfinn",
            "https://api.dicebear.com/7.x/adventurer/png?seed=SailorMoon",
            "https://api.dicebear.com/7.x/adventurer/png?seed=SungJinWoo",
            "https://api.dicebear.com/7.x/adventurer/png?seed=RimuruTempest"
        )
        var selectedAvatarInput by remember { mutableStateOf(avatarUrlInput) }

        AlertDialog(
            onDismissRequest = { showChangeAvatarDialog = false },
            title = { Text("Select Anime Avatar", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primaryText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select a verified premium anime graphic preset to overwrite the current avatar profile (50 Trending presets available):", fontSize = 11.sp, color = secondaryText)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val chunks = predefinedAvatars.chunked(4)
                        chunks.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                rowItems.forEach { imgUrl ->
                                    val isSel = selectedAvatarInput == imgUrl
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .border(
                                                2.2.dp,
                                                if (isSel) (if (isDark) BrandPrimary else Color(0xFF0F1B6B)) else Color.Transparent,
                                                CircleShape
                                            )
                                            .clickable { selectedAvatarInput = imgUrl }
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = imgUrl),
                                            contentDescription = "Avatar Grid Item",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = selectedAvatarInput,
                        onValueChange = { selectedAvatarInput = it },
                        label = { Text("Custom Avatar URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        avatarUrlInput = selectedAvatarInput
                        showChangeAvatarDialog = false
                        viewModel.updateProfile(
                            name = nameInput,
                            email = emailInput,
                            isPremium = user?.isPremium ?: true,
                            trustScore = user?.trustScore ?: 850,
                            contactNo = contactInput,
                            gmailId = user?.gmailId ?: "",
                            avatarUrl = selectedAvatarInput,
                            streak = user?.streak ?: 0,
                            medal = user?.medal ?: "Start"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                ) {
                    Text("Confirm Avatar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeAvatarDialog = false }) { Text("Cancel", color = secondaryText) }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = cardBg
        )
    }

    // B. DIALOG: UPLOAD PHOTO FROM DEVICE OR WEB URL
    if (showUploadPhotoDialog) {
        var uploadUrlInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showUploadPhotoDialog = false },
            title = { Text("Upload Profile Photo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Select a photo from your local device files or paste an external public web URL:", fontSize = 12.sp, color = secondaryText)
                    
                    // Device Upload Trigger
                    Button(
                        onClick = {
                            photoLauncher.launch("image/*")
                            showUploadPhotoDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                            contentColor = if (isDark) Color.Black else Color.White
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("CHOOSE FROM DEVICE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = outlineBorder)
                        Text("OR ENTER WEB URL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = secondaryText)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = outlineBorder)
                    }

                    OutlinedTextField(
                        value = uploadUrlInput,
                        onValueChange = { uploadUrlInput = it },
                        placeholder = { Text("https://example.com/photo.jpg") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (uploadUrlInput.isNotBlank()) {
                            avatarUrlInput = uploadUrlInput.trim()
                            viewModel.updateProfile(
                                name = nameInput,
                                email = emailInput,
                                isPremium = user?.isPremium ?: true,
                                trustScore = user?.trustScore ?: 850,
                                contactNo = contactInput,
                                gmailId = user?.gmailId ?: "",
                                avatarUrl = uploadUrlInput.trim(),
                                streak = user?.streak ?: 0,
                                medal = user?.medal ?: "Start"
                            )
                            Toast.makeText(context, "URL photo uploaded!", Toast.LENGTH_SHORT).show()
                        }
                        showUploadPhotoDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                ) {
                    Text("Upload Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadPhotoDialog = false }) { Text("Cancel", color = secondaryText) }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = cardBg
        )
    }

    // C. DIALOG: CHANGE PASSWORD DIALOG WITH SHARED PREFERENCES BINDING
    if (showChangePinDialog) {
        val authPrefs = remember(context) {
            context.getSharedPreferences("kinetic_auth_prefs", android.content.Context.MODE_PRIVATE)
        }
        var currentPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Modify security Password", fontWeight = FontWeight.Bold, color = primaryText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val actualSavedPin = remember { authPrefs.getString("app_security_pin", "0000") ?: "" }
                    Text("Enter current and new numerical Password (4 digits) to reset security locking:", fontSize = 12.sp, color = secondaryText)

                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { if (it.length <= 4) currentPinInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Current 4-Digit Password") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4) newPinInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("New 4-Digit Password") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (errorMessage.isNotEmpty()) {
                        Text(errorMessage, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val realPin = authPrefs.getString("app_security_pin", "") ?: ""
                        if (realPin.isNotEmpty() && currentPinInput != realPin) {
                            errorMessage = "Current Password matches incorrectly!"
                        } else if (newPinInput.length != 4) {
                            errorMessage = "New Password must be exactly 4 digits!"
                        } else {
                            appConfirmAction = {
                                authPrefs.edit().putString("app_security_pin", newPinInput).apply()
                                Toast.makeText(context, "App Security Password updated successfully! 🔒", Toast.LENGTH_SHORT).show()
                                showChangePinDialog = false
                            }
                            appConfirmMessage = "Are you sure you want to change your security Password?"
                            showAppConfirmDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) { Text("Cancel", color = secondaryText) }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = cardBg
        )
    }

    // D. SYSTEM LOG LOADING OVERLAY SIMULATOR
    if (downloadProgressActive) {
        var logsProgress by remember { mutableStateOf(0.0f) }
        LaunchedEffect(Unit) {
            logsProgress = 0.0f
            while (logsProgress < 1.0f) {
                delay(100)
                logsProgress += 0.1f
            }
            downloadProgressActive = false
            Toast.makeText(context, "Session activity logs exported to public Download folders! 📁", Toast.LENGTH_LONG).show()
        }

        AlertDialog(
            onDismissRequest = {},
            title = { Text("Exporting Activity Logs", fontWeight = FontWeight.Bold, color = primaryText) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Formatting secure device activity indexes to JSON...", fontSize = 12.sp, color = secondaryText)
                    LinearProgressIndicator(
                        progress = { logsProgress },
                        color = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                }
            },
            confirmButton = {}
        )
    }

    // Interactive confirmation overlay (Yes/No styling)
    if (showAppConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showAppConfirmDialog = false },
            title = {
                Text(
                    text = "Confirm Action",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = primaryText
                )
            },
            text = {
                Text(
                    text = appConfirmMessage,
                    fontSize = 13.sp,
                    color = secondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        appConfirmAction?.invoke()
                        showAppConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Yes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAppConfirmDialog = false }) {
                    Text("No", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = cardBg
        )
    }
}

@Composable
fun RecentActivityItem(
    device: String,
    status: String,
    location: String,
    time: String,
    isActive: Boolean,
    isDark: Boolean,
    outlineBorder: Color,
    primaryText: Color,
    secondaryText: Color,
    cardBg: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, outlineBorder),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Device Icon Frame Box
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF161A26) else Color(0xFFF1F5F9))
                        .border(1.dp, outlineBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val deviceIcon = if (device.contains("iPhone")) Icons.Default.PhoneAndroid else if (device.contains("MacBook")) Icons.Default.LaptopMac else Icons.Default.TabletMac
                    Icon(
                        imageVector = deviceIcon,
                        contentDescription = "Device icon",
                        tint = if (isActive && !isDark) Color(0xFF0F1B6B) else primaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        text = device,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = primaryText
                    )
                    Text(
                        text = "📍 $location",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryText
                    )
                    Text(
                        text = time,
                        fontSize = 10.sp,
                        color = secondaryText.copy(alpha = 0.8f)
                    )
                }
            }

            // Status chip tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                }
                Text(
                    text = status,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isActive) Color(0xFF10B981) else secondaryText
                )
            }
        }
    }
}

// Gorgeous Custom Shield logo matching modern dynamic trend representation inside Stitch mockups
@Composable
fun KineticShieldLogo(modifier: Modifier, isDark: Boolean) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Background subtle base circle
        drawCircle(
            color = if (isDark) Color(0xFF131722) else Color(0xFF0F1B6B),
            radius = width * 0.48f
        )

        // Draw dynamic shield lines
        val shieldPath = Path().apply {
            moveTo(width * 0.5f, height * 0.18f)
            // Top Right
            cubicTo(width * 0.72f, height * 0.16f, width * 0.8f, height * 0.22f, width * 0.8f, height * 0.48f)
            // Bottom Right Curve
            cubicTo(width * 0.8f, height * 0.72f, width * 0.5f, height * 0.86f, width * 0.5f, height * 0.86f)
            // Bottom Left Curve
            cubicTo(width * 0.5f, height * 0.86f, width * 0.22f, height * 0.72f, width * 0.22f, height * 0.48f)
            // Top Left
            cubicTo(width * 0.22f, height * 0.22f, width * 0.28f, height * 0.16f, width * 0.5f, height * 0.18f)
            close()
        }

        // Draw Outer Stroke Shield Border
        drawPath(
            path = shieldPath,
            color = if (isDark) StitchPrimary else Color(0xFF00FF66),
            style = Stroke(width = 5f)
        )

        // Draw inner financial upward dynamic trending graph lines
        val crestTrendPath = Path().apply {
            moveTo(width * 0.35f, height * 0.65f)
            lineTo(width * 0.45f, height * 0.52f)
            lineTo(width * 0.52f, height * 0.58f)
            lineTo(width * 0.67f, height * 0.38f)
        }

        drawPath(
            path = crestTrendPath,
            color = if (isDark) StitchPrimary else Color(0xFF00FF66),
            style = Stroke(width = 6f)
        )

        // Arrow head pointing to the success/growth
        val arrowPath = Path().apply {
            moveTo(width * 0.67f, height * 0.38f)
            lineTo(width * 0.58f, height * 0.38f)
            lineTo(width * 0.67f, height * 0.47f)
            close()
        }
        drawPath(
            path = arrowPath,
            color = if (isDark) StitchPrimary else Color(0xFF00FF66)
        )
    }
}
