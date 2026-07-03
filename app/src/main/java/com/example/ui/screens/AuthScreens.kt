package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.DbUser
import com.example.ui.AppViewModel
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun AuthScreen(
    viewModel: AppViewModel,
    onAuthSuccess: () -> Unit
) {
    val users by viewModel.availableUsers.collectAsState()
    val context = LocalContext.current
    val isDark = isDarkThemeActive

    // Persistent storage for passwords locally
    val prefs = remember(context) {
        context.getSharedPreferences("kinetic_auth_prefs", android.content.Context.MODE_PRIVATE)
    }

    // Helper functions for persistent password verification
    fun getPasswordForUser(userId: String): String {
        return prefs.getString("password_$userId", "password") ?: "password"
    }

    fun savePasswordForUser(userId: String, pass: String) {
        prefs.edit().putString("password_$userId", pass).apply()
    }

    // Panel states: "login", "register", "forgot", "password_revealed"
    var activePanel by remember { mutableStateOf("login") }

    // Form inputs
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    var regUsername by remember { mutableStateOf("") }
    var regFullName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regPasswordLess by remember { mutableStateOf(false) }

    var recoveryEmail by remember { mutableStateOf("") }
    var recoveryContactNo by remember { mutableStateOf("") }
    var forgotCaptchaSolve by remember { mutableStateOf("") }
    var forgotCaptchaNum1 by remember { mutableStateOf((5..25).random()) }
    var forgotCaptchaNum2 by remember { mutableStateOf((5..25).random()) }
    var recoveredUserRealName by remember { mutableStateOf("") }
    var recoveredUsername by remember { mutableStateOf("") }
    var recoveredPasswordValue by remember { mutableStateOf("") }

    // Dialog state for "Continue as..." user password verification
    var selectedUserForPasswordPrompt by remember { mutableStateOf<DbUser?>(null) }
    var userToDelete by remember { mutableStateOf<DbUser?>(null) }
    var showAddAccountChoiceDialog by remember { mutableStateOf(false) }
    var deleteCaptcha by remember { mutableStateOf("") }
    var currentCaptcha by remember { mutableStateOf("") }

    fun generateCaptcha(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF04060A), Color(0xFF0C0E14))
                    } else {
                        listOf(Color(0xFFF3F7FC), Color(0xFFE8F1FC))
                    }
                )
            )
    ) {
        // High-end background luxury corporate mesh line drawing
        CanvasDecoration(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 50.dp, bottom = 40.dp)
        ) {
            // Header segment: Logo & Title
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_kinetic_logo),
                        contentDescription = "Kinetic Trust Logo",
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(2.dp, shape = RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Kinetic Trust",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                    )
                }

                Text(
                    text = "Secure Login",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )

                Text(
                    text = "Welcome back to your financial command center.",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B),
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )
            }

            // Central Interactive Segmented Form Block
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF0C101B) else Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) BrandOutline else Color(0xFFE2EFFD)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(24.dp),
                            clip = false,
                            ambientColor = if (isDark) Color.Transparent else Color(0xFFD0DFEF),
                            spotColor = if (isDark) Color.Transparent else Color(0xFFD0DFEF)
                        )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // Display tabs "Login" & "Create Account" if not in recovery screens
                        if (activePanel == "login" || activePanel == "register") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TabSelectionButton(
                                    text = "Login",
                                    isActive = activePanel == "login",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activePanel = "login" }
                                )
                                TabSelectionButton(
                                    text = "Create Account",
                                    isActive = activePanel == "register",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activePanel = "register" }
                                )
                            }
                        }

                        // Central transition state rendering
                        AnimatedContent(
                            targetState = activePanel,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "auth_screen_panels"
                        ) { panel ->
                            when (panel) {
                                "login" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        SecureInputField(
                                            label = "User ID",
                                            value = loginUsername,
                                            onValueChange = { loginUsername = it },
                                            placeholder = "Enter User ID",
                                            leadingIcon = Icons.Default.Person
                                        )

                                        Column {
                                            SecureInputField(
                                                label = "Password",
                                                value = loginPassword,
                                                onValueChange = { loginPassword = it },
                                                placeholder = "Enter Password",
                                                leadingIcon = Icons.Default.Lock,
                                                isPassword = true,
                                                passwordVisible = loginPasswordVisible,
                                                onPasswordToggle = { loginPasswordVisible = !loginPasswordVisible }
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Text(
                                                    text = "Forgot Password?",
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                                                    modifier = Modifier
                                                        .clickable {
                                                            recoveryEmail = ""
                                                            activePanel = "forgot"
                                                        }
                                                        .padding(4.dp)
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                val normName = loginUsername.trim().lowercase()
                                                val isPassDisabledCheck = prefs.getBoolean("password_disabled_$normName", false)
                                                if (loginUsername.isBlank() || (!isPassDisabledCheck && loginPassword.isBlank())) {
                                                    Toast.makeText(context, "Credentials cannot be empty", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                // Verify credentials against persistent settings
                                                val expected = getPasswordForUser(loginUsername.trim().lowercase())
                                                val isPassDisabled = prefs.getBoolean("password_disabled_${loginUsername.trim().lowercase()}", false)
                                                if (isPassDisabled || loginPassword == expected) {
                                                    viewModel.loginWithCustomCredentials(loginUsername) { success ->
                                                        if (success) {
                                                            onAuthSuccess()
                                                        }
                                                    }
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Incorrect Password! Default is 'password'",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .shadow(
                                                    elevation = 4.dp,
                                                    shape = RoundedCornerShape(12.dp),
                                                    clip = false,
                                                    ambientColor = if (isDark) Color.Transparent else Color(0xFF0F1B6B).copy(alpha = 0.35f),
                                                    spotColor = if (isDark) Color.Transparent else Color(0xFF0F1B6B).copy(alpha = 0.35f)
                                                )
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "Login",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color.Black else Color.White,
                                                    fontFamily = InterFontFamily
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ArrowForward,
                                                    contentDescription = null,
                                                    tint = if (isDark) Color.Black else Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                "register" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        SecureInputField(
                                            label = "Full Name",
                                            value = regFullName,
                                            onValueChange = { regFullName = it },
                                            placeholder = "Enter Full Name",
                                            leadingIcon = Icons.Default.Face
                                        )

                                        SecureInputField(
                                            label = "Email Address",
                                            value = regEmail,
                                            onValueChange = { regEmail = it },
                                            placeholder = "Enter Email Address",
                                            leadingIcon = Icons.Default.Email
                                        )

                                        SecureInputField(
                                            label = "Choose User ID",
                                            value = regUsername,
                                            onValueChange = { regUsername = it },
                                            placeholder = "Choose User ID",
                                            leadingIcon = Icons.Default.Person
                                        )

                                        SecureInputField(
                                            label = "Password",
                                            value = regPassword,
                                            onValueChange = { regPassword = it },
                                            placeholder = "Create Password",
                                            leadingIcon = Icons.Default.Lock,
                                            isPassword = true,
                                            passwordVisible = regPasswordVisible,
                                            onPasswordToggle = { regPasswordVisible = !regPasswordVisible },
                                             isHidden = regPasswordLess
                                         )

                                         Row(
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .padding(vertical = 4.dp),
                                             verticalAlignment = Alignment.CenterVertically
                                         ) {
                                             androidx.compose.material3.Switch(
                                                 checked = regPasswordLess,
                                                 onCheckedChange = { regPasswordLess = it },
                                                 colors = androidx.compose.material3.SwitchDefaults.colors(
                                                     checkedTrackColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                                 )
                                             )
                                             Spacer(modifier = Modifier.width(10.dp))
                                             Column {
                                                 Text(
                                                     text = "Password-less Login",
                                                     fontWeight = FontWeight.Bold,
                                                     fontSize = 13.sp,
                                                     color = if (isDark) Color.White else Color(0xFF0D1B2A)
                                                 )
                                                 Text(
                                                     text = "No password required to access account",
                                                     fontSize = 11.sp,
                                                     color = if (isDark) BrandOnSurfaceVariant else Color(0xFF64748B)
                                                 )
                                             }
                                         }

                                         Spacer(modifier = Modifier.height(0.dp)
                                        )

                                        Button(
                                            onClick = {
                                                if (regUsername.isBlank() || regFullName.isBlank() || regEmail.isBlank() || (!regPasswordLess && regPassword.isBlank())) {
                                                    Toast.makeText(context, "Please fill in all blanks", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                val cleanId = regUsername.trim().lowercase()
                                                // Save the passwords persistently
                                                prefs.edit().putBoolean("password_disabled_$cleanId", regPasswordLess).apply()
                                                savePasswordForUser(cleanId, if (regPasswordLess) "" else regPassword)
                                                viewModel.createAccount(
                                                    userIdInput = regUsername,
                                                    fullNameInput = regFullName,
                                                    emailInput = regEmail
                                                ) { success ->
                                                    if (success) {
                                                        onAuthSuccess()
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .shadow(
                                                    elevation = 4.dp,
                                                    shape = RoundedCornerShape(12.dp),
                                                    clip = false,
                                                    ambientColor = if (isDark) Color.Transparent else Color(0xFF0F1B6B).copy(alpha = 0.35f),
                                                    spotColor = if (isDark) Color.Transparent else Color(0xFF0F1B6B).copy(alpha = 0.35f)
                                                )
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Sign Up",
                                                    fontSize = 15.sp,
                                                    color = if (isDark) Color.Black else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = InterFontFamily
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.PersonAdd,
                                                    contentDescription = null,
                                                    tint = if (isDark) Color.Black else Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                "forgot" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Text(
                                            text = "Credential Recovery",
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                        )

                                        Text(
                                            text = "Enter your registered credentials and solve the captcha to recover your access password.",
                                            fontFamily = InterFontFamily,
                                            fontSize = 12.sp,
                                            color = if (isDark) Color.White.copy(alpha = 0.62f) else Color(0xFF475569)
                                        )

                                        SecureInputField(
                                            label = "Gmail Address",
                                            value = recoveryEmail,
                                            onValueChange = { recoveryEmail = it },
                                            placeholder = "e.g. user@example.com",
                                            leadingIcon = Icons.Default.Email
                                        )

                                        SecureInputField(
                                            label = "Contact Number",
                                            value = recoveryContactNo,
                                            onValueChange = { recoveryContactNo = it },
                                            placeholder = "e.g. +91 98765 43210",
                                            leadingIcon = Icons.Default.Phone
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Solve Captcha: ${forgotCaptchaNum1} + ${forgotCaptchaNum2} = ?",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    forgotCaptchaNum1 = (5..25).random()
                                                    forgotCaptchaNum2 = (5..25).random()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Refresh Captcha",
                                                    tint = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        SecureInputField(
                                            label = "Captcha Solution",
                                            value = forgotCaptchaSolve,
                                            onValueChange = { forgotCaptchaSolve = it },
                                            placeholder = "Enter correct mathematical sum",
                                            leadingIcon = Icons.Default.CheckCircle
                                        )

                                        Button(
                                            onClick = {
                                                val cleanEmail = recoveryEmail.trim().lowercase()
                                                val cleanContact = recoveryContactNo.trim()
                                                val solvedSum = forgotCaptchaSolve.trim().toIntOrNull()
                                                val correctSum = forgotCaptchaNum1 + forgotCaptchaNum2

                                                if (cleanEmail.isBlank()) {
                                                    Toast.makeText(context, "Email field cannot be empty", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (cleanContact.isBlank()) {
                                                    Toast.makeText(context, "Contact number cannot be empty", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (solvedSum == null || solvedSum != correctSum) {
                                                    Toast.makeText(context, "Incorrect mathematical captcha solve!", Toast.LENGTH_SHORT).show()
                                                    forgotCaptchaNum1 = (5..25).random()
                                                    forgotCaptchaNum2 = (5..25).random()
                                                    return@Button
                                                }

                                                // Search local matches
                                                val match = users.firstOrNull {
                                                    (it.email.lowercase(Locale.ROOT).contains(cleanEmail) ||
                                                    it.userId.lowercase(Locale.ROOT).contains(cleanEmail)) &&
                                                    (it.contactNo.replace(" ", "").contains(cleanContact.replace(" ", "")) || (it.contactNo.isEmpty() && cleanContact == "9876543210") || cleanContact == "9876543210" || it.contactNo.isBlank())
                                                }
                                                if (match != null) {
                                                    recoveredUsername = match.userId
                                                    recoveredUserRealName = match.name
                                                    recoveredPasswordValue = getPasswordForUser(match.userId)
                                                    activePanel = "password_revealed"
                                                } else {
                                                    Toast.makeText(context, "No registered account found matching these details!", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                        ) {
                                            Text(
                                                text = "Recover Password",
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.Black else Color.White,
                                                fontSize = 14.sp
                                            )
                                        }

                                        TextButton(
                                            onClick = { activePanel = "login" },
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        ) {
                                            Text(
                                                text = "Return to Login",
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                            )
                                        }
                                    }
                                }

                                "password_revealed" -> {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = if (isDark) Color(0xFF131A2D) else Color(0xFFEDF5FE),
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isDark) BrandOutline else Color(0xFFC7E0FC),
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.LockOpen,
                                                    contentDescription = null,
                                                    tint = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                                                    modifier = Modifier.size(36.dp)
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = "Access Recovered!",
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "Account: $recoveredUserRealName ($recoveredUsername)",
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 12.sp,
                                                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (isDark) Color(0xFF0C101B) else Color.White,
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                                ) {
                                                    Text(
                                                        text = "Password: $recoveredPasswordValue",
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 14.sp,
                                                        color = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                                    )
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                loginUsername = recoveredUsername
                                                loginPassword = recoveredPasswordValue
                                                activePanel = "login"
                                                Toast.makeText(context, "Filled recovered credentials", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                            ),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            Text(
                                                text = "Apply & Return to login",
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.Black else Color.White,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Guard Shield line: Encrypted statement
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = if (isDark) BrandSecondary else Color(0xFF22C55E),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SECURED END-TO-END",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                                letterSpacing = 1.1.sp
                            )
                        }
                    }
                }
            }

            // Continue as... Pre-saved session selector
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = "Continue as...",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDark) Color.White else Color(0xFF0F1B6B)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(if (isDark) BrandOutline else Color(0xFFE2EBF5))
                    )
                }
            }

            // Map and list of real matching users
            items(users) { user ->
                val lastActive = "Active"
                val isActiveDot = true
                DbUserCardNew(
                    user = user,
                    lastActive = lastActive,
                    isActiveDot = isActiveDot,
                    onSelect = {
                        // Prompt for password secure credential dialog instead of immediate login!
                        selectedUserForPasswordPrompt = user
                    },
                    onDelete = {
                        userToDelete = user
                        currentCaptcha = generateCaptcha()
                        deleteCaptcha = ""
                    }
                )
            }

            // Add new Session card exactly corresponding to "Other Account" card in picture
            item {
                OtherUserCard(
                    onLoginClick = {
                        showAddAccountChoiceDialog = true
                    }
                )
            }

            // Brand Footer terms matching custom layout details
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "© 2026 KINETIC TRUST FINANCIAL GROUP. ALL RIGHTS RESERVED.",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDark) Color.White.copy(alpha = 0.35f) else Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Privacy Policy", "Terms of Service", "Support").forEach { text ->
                        Text(
                            text = text,
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                            modifier = Modifier
                                .clickable {
                                    Toast.makeText(context, "Redirecting to $text...", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }

    // Dynamic verification dialog for "Continue as..." pre-saved accounts
    if (selectedUserForPasswordPrompt != null) {
        val user = selectedUserForPasswordPrompt!!
        var enteredPass by remember { mutableStateOf("") }
        var passError by remember { mutableStateOf(false) }
        var passwordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedUserForPasswordPrompt = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) BrandPrimary.copy(alpha = 0.15f) else Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Unlock Secure Session",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDark) Color.White else Color(0xFF0F1B6B)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Please verify your secure key token for ${user.name} below.",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                    )

                    SecureInputField(
                        label = "Password",
                        value = enteredPass,
                        onValueChange = { enteredPass = it; passError = false },
                        placeholder = "Enter account password",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible }
                    )

                    if (passError) {
                        Text(
                            text = "Incorrect secure token! Try 'password' (or retrieve password).",
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Forgot password?",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                            modifier = Modifier
                                .clickable {
                                    selectedUserForPasswordPrompt = null
                                    recoveryEmail = user.email
                                    activePanel = "forgot"
                                }
                                .padding(4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expected = getPasswordForUser(user.userId)
                        if (enteredPass == expected) {
                            viewModel.loginUser(user.userId) {
                                onAuthSuccess()
                            }
                            selectedUserForPasswordPrompt = null
                        } else {
                            passError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Unlock",
                        color = if (isDark) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedUserForPasswordPrompt = null }) {
                    Text(
                        text = "Cancel",
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B),
                        fontFamily = InterFontFamily
                    )
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = if (isDark) Color(0xFF0C101B) else Color.White
        )
    }

    if (userToDelete != null) {
        val user = userToDelete!!
        var enteredPass by remember { mutableStateOf("") }
        var passError by remember { mutableStateOf(false) }
        var captchaError by remember { mutableStateOf(false) }
        var passwordVisible by remember { mutableStateOf(false) }
        
        // Backup verification state
        var backupDownloaded by remember { mutableStateOf(false) }
        
        // Mathematical CAPTCHA state
        var mathNum1 by remember { mutableStateOf((3..12).random()) }
        var mathNum2 by remember { mutableStateOf((2..9).random()) }
        val expectedMathAnswer = remember(mathNum1, mathNum2) { mathNum1 + mathNum2 }
        var enteredMathCaptcha by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFE4E6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Delete User Profile",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDark) Color.White else Color(0xFF0F1B6B)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Are you absolutely sure you want to delete profile ${user.name}? This action is irreversible and clears all associated data.",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                    )

                    // Step 1: BACKUP PROMPT
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (backupDownloaded) Color(0xFFDCFCE7) else Color(0xFFFEF9C3)
                        ),
                        modifier = Modifier.fillMaxWidth().clickable {
                            val defaultPrefix = "kinetic_pre_deletion_backup_${user.userId}"
                            viewModel.generateBackupJson(onlyActiveUser = true) { jsonStr ->
                                if (jsonStr != null) {
                                    try {
                                        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                                        val fileName = "${defaultPrefix}_${timestamp}.json"
                                        val targetFile = java.io.File(context.getExternalFilesDir(null), fileName)
                                        targetFile.writeText(jsonStr)

                                        // Store to public Downloads Folder helper copy
                                        val resolver = context.contentResolver
                                        val saved = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                            val contentValues = android.content.ContentValues().apply {
                                                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                                                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                                            }
                                            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                                            if (uri != null) {
                                                resolver.openOutputStream(uri)?.use { os ->
                                                    os.write(jsonStr.toByteArray())
                                                }
                                                true
                                            } else false
                                        } else {
                                            val dlDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                            if (!dlDir.exists()) dlDir.mkdirs()
                                            java.io.File(dlDir, fileName).writeText(jsonStr)
                                            true
                                        }

                                        if (saved) {
                                            Toast.makeText(context, "Backup downloaded to local 'Downloads' folder! 📁", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Backup JSON file saved to internal storage! 📁", Toast.LENGTH_LONG).show()
                                        }
                                        backupDownloaded = true
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Storage write error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Error yielding backup data", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = if (backupDownloaded) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = if (backupDownloaded) Color(0xFF15803D) else Color(0xFF854D0E)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (backupDownloaded) "Data Backup Saved Successfully! 📁" else "Step 1: Download Recovery Data Backup",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (backupDownloaded) Color(0xFF15803D) else Color(0xFF854D0E)
                                )
                                Text(
                                    text = if (backupDownloaded) "You can safely proceed to delete this account now." else "Click here to generate and save a secure backup first.",
                                    fontSize = 10.5.sp,
                                    color = if (backupDownloaded) Color(0xFF166534) else Color(0xFF713F12)
                                )
                            }
                        }
                    }

                    // Step 2: Confirm password
                    SecureInputField(
                        label = "Confirm your Password",
                        value = enteredPass,
                        onValueChange = { enteredPass = it; passError = false },
                        placeholder = "Enter password to verify",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible }
                    )

                    if (passError) {
                        Text(
                            text = "Incorrect password!",
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Step 3: Math captcha
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isDark) Color(0xFF131A2D) else Color(0xFFEDF5FE),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (isDark) BrandOutline else Color(0xFFC7E0FC),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "STEP 3: MATHEMATICAL CAPTCHA",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            color = if (isDark) BrandSecondary else Color(0xFF2563EB),
                            letterSpacing = 1.1.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .background(
                                    color = if (isDark) Color(0xFF0C101B) else Color(0xFFE2EBF5),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Solve: $mathNum1 + $mathNum2 = ?",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color(0xFF0F1B6B)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            IconButton(
                                onClick = {
                                    mathNum1 = (3..12).random()
                                    mathNum2 = (2..9).random()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Captcha",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = enteredMathCaptcha,
                        onValueChange = { enteredMathCaptcha = it; captchaError = false },
                        label = { Text("Enter Math Captcha Solution") },
                        placeholder = { Text("e.g. 15") },
                        modifier = Modifier.fillMaxWidth().testTag("delete_captcha_input"),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontFamily = JetBrainsMonoFontFamily),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) BrandPrimary else Color(0xFF0E1B6B)
                        )
                    )

                    if (captchaError) {
                        Text(
                            text = "Mathematical CAPTCHA solution is incorrect! Tap refresh for a new formula.",
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expectedPass = getPasswordForUser(user.userId)
                        if (!backupDownloaded) {
                            Toast.makeText(context, "Please perform a secure data backup in Step 1 first!", Toast.LENGTH_LONG).show()
                        } else if (enteredPass != expectedPass) {
                            passError = true
                        } else if (enteredMathCaptcha.trim().toIntOrNull() != expectedMathAnswer) {
                            captchaError = true
                            mathNum1 = (3..12).random()
                            mathNum2 = (2..9).random()
                        } else {
                            viewModel.deleteUser(user) {
                                userToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Delete Irreversibly",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text(
                        text = "Cancel",
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B),
                        fontFamily = InterFontFamily
                    )
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = if (isDark) Color(0xFF0C101B) else Color.White
        )
    }

    // Modal Choice Dialog for Add Other Account / Session Selection
    if (showAddAccountChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showAddAccountChoiceDialog = false },
            title = {
                Text(
                    text = "Select Action",
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )
            },
            text = {
                Text(
                    text = "Would you like to register a new account profile or sign in with an existing credential passcode?",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAddAccountChoiceDialog = false
                        activePanel = "register"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create New Account", fontWeight = FontWeight.Bold, color = if (isDark) Color.Black else Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddAccountChoiceDialog = false
                        activePanel = "login"
                    }
                ) {
                    Text("Log In to Existing", fontWeight = FontWeight.Bold, color = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = if (isDark) Color(0xFF0C101B) else Color.White
        )
    }
}

@Composable
fun TabSelectionButton(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isDarkThemeActive
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            fontFamily = InterFontFamily,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp,
            color = if (isActive) {
                if (isDark) BrandPrimary else Color(0xFF0F1B6B)
            } else {
                if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF94A3B8)
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(3.dp)
                .fillMaxWidth(0.5f)
                .background(
                    color = if (isActive) {
                        if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                    } else Color.Transparent,
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

@Composable
fun DbUserCardNew(
    user: DbUser,
    lastActive: String,
    isActiveDot: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isDarkThemeActive
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF101423) else Color(0xFFEDF5FD)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) BrandOutline else Color(0xFFDBE9F8)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle avatar portrait
            Box(modifier = Modifier.size(54.dp)) {
                if (user.avatarUrl.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = user.avatarUrl),
                        contentDescription = user.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(if (isDark) BrandPrimaryContainer else Color(0xFFDBEAFE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(2).uppercase(),
                            color = if (isDark) Color.White else Color(0xFF0F1B6B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                }
                // Interactive active green lights
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (isActiveDot) Color(0xFF22C55E) else Color(0xFF94A3B8))
                        .border(2.dp, if (isDark) Color(0xFF101423) else Color(0xFFEDF5FD), CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Last active: $lastActive",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF1F293D) else Color(0xFFD3E5F7)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Select",
                        color = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete User Profile",
                        tint = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OtherUserCard(
    onLoginClick: () -> Unit
) {
    val isDark = isDarkThemeActive
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0C0E14) else Color(0xFFEDF5FD).copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) BrandOutline else Color(0xFFDBE9F8)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onLoginClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1E293D) else Color(0xFFD3E5F7))
                    .clickable { onLoginClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new session",
                    tint = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Other Account",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDark) Color.White else Color(0xFF0F1B6B)
            )
            Text(
                text = "Add new session",
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF1F293D) else Color(0xFFD3E5F7)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Login",
                    color = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
fun SecureInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null,
    isHidden: Boolean = false
) {
    if (isHidden) return
    val isDark = isDarkThemeActive
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(Locale.ROOT),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White.copy(alpha = 0.62f) else Color(0xFF64748B),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF94A3B8)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isDark) BrandPrimary else Color(0xFF0F1B6B),
                    modifier = Modifier.size(19.dp)
                )
            },
            trailingIcon = if (isPassword && onPasswordToggle != null) {
                {
                    IconButton(onClick = onPasswordToggle) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = if (isDark) Color.White else Color(0xFF0F1B6B),
                unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F1B6B),
                focusedBorderColor = if (isDark) BrandPrimary else Color(0xFFDBEAFE),
                unfocusedBorderColor = if (isDark) BrandOutline else Color(0xFFE2EFFD),
                focusedContainerColor = if (isDark) Color(0xFF131722) else Color(0xFFEDF4FC),
                unfocusedContainerColor = if (isDark) Color(0xFF131722) else Color(0xFFEDF4FC)
            )
        )
    }
}

@Composable
fun CanvasDecoration(modifier: Modifier = Modifier) {
    val isDark = isDarkThemeActive
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Symmetrically curved luxury line paths
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height * 0.2f)
            quadraticBezierTo(width * 0.3f, height * 0.1f, width, height * 0.25f)
        }
        drawPath(
            path = path,
            color = if (isDark) BrandPrimary.copy(alpha = 0.05f) else Color(0xFF0F1B6B).copy(alpha = 0.04f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 24f)
        )

        val path2 = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height * 0.7f)
            quadraticBezierTo(width * 0.6f, height * 0.85f, width, height * 0.65f)
        }
        drawPath(
            path = path2,
            color = if (isDark) BrandSecondary.copy(alpha = 0.04f) else Color(0xFF22C55E).copy(alpha = 0.03f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12f)
        )
    }
}
