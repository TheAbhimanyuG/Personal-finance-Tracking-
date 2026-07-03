package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.AppViewModel
import com.example.ui.theme.*

// Data structure representing a unified bottom navigation item
data class UnifiedBottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val tag: String
)

@Composable
fun UnifiedTopBar(
    viewModel: AppViewModel,
    panelTitle: String = "Dashboard",
    onMenuClick: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isDark = isDarkThemeActive
    val showProfile by viewModel.showProfilePanel.collectAsState()

    // Colors
    val cardBg = if (isDark) BrandSurface else Color.White
    val borderOutline = if (isDark) BrandOutline else Color(0xFFE2EFFD)
    val mainTextCol = if (isDark) Color.White else Color(0xFF0F1B6B)
    val accentColor = if (isDark) BrandPrimary else Color(0xFF1B2A7E)

    val userName = currentUser?.name?.split(" ")?.firstOrNull() ?: "User"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg)
            .statusBarsPadding()
            .height(50.dp)
            .drawBehind {
                // Smooth separator trace line
                drawLine(
                    color = borderOutline,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Actions on left
        // 1. Menu Toggle Button
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(36.dp).testTag("unified_menu_button")
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open navigation drawer",
                tint = mainTextCol,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Title text
        Text(
            text = panelTitle,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = mainTextCol,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // User badge
        Box(
            modifier = Modifier
                .background(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = userName,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = accentColor
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Theme Quick Toggle
        IconButton(
            onClick = {
                viewModel.toggleTheme(if (isDark) "Light" else "Dark")
            },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                .testTag("theme_quick_toggle")
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.NightsStay,
                contentDescription = "Toggle color theme",
                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF0F1B6B),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Profile pic icon button
        IconButton(
            onClick = { viewModel.setShowProfilePanel(!showProfile) },
            modifier = Modifier.size(36.dp).testTag("unified_profile_button")
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.2.dp, if (showProfile) BrandPrimary else borderOutline, CircleShape)
                    .background(if (isDark) Color(0xFF1E2640) else Color(0xFFEBF3FC)),
                contentAlignment = Alignment.Center
            ) {
                if (currentUser?.avatarUrl?.isNotBlank() == true) {
                    androidx.compose.foundation.Image(
                        painter = rememberAsyncImagePainter(model = currentUser?.avatarUrl),
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = currentUser?.name?.take(2)?.uppercase() ?: "KT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
                    )
                }
            }
        }
    }
}

@Composable
fun UnifiedBottomNavBar(
    activeRoute: String,
    onNavigate: (String) -> Unit,
    viewModel: AppViewModel? = null
) {
    val isDark = isDarkThemeActive
    val showProfile by if (viewModel != null) viewModel.showProfilePanel.collectAsState() else remember { mutableStateOf(false) }

    if (showProfile) {
        // Hide completely when profile panel is active to prevent congestion
        return
    }

    val barColor = if (isDark) BrandSurface else Color(0xFFF1F6FE)
    val borderOutline = if (isDark) BrandOutline else Color(0xFFE0ECFD)
    val selectedIconTint = if (isDark) BrandPrimary else Color(0xFF0F1B6B)
    val unselectedIconTint = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF64748B)

    // Items list: Exactly 5 principal options as specified
    val navItems = remember {
        listOf(
            UnifiedBottomNavItem("Planning", Icons.Default.TrackChanges, "budget_goals", "planning"),
            UnifiedBottomNavItem("Ledger", Icons.Default.ReceiptLong, "net_worth", "ledger"),
            UnifiedBottomNavItem("Home", Icons.Default.Home, "home", "home"),
            UnifiedBottomNavItem("Add Txn", Icons.Default.AddCircle, "add_txn", "add_txn"),
            UnifiedBottomNavItem("Settings", Icons.Default.Settings, "settings", "settings")
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .drawBehind {
                    // Textured blueprint style background tracing grid lines for the bottom menu
                    val drawW = size.width
                    val drawH = size.height
                    
                    // Solid rounded background card draw
                    val bgPath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                rect = androidx.compose.ui.geometry.Rect(0f, 0f, drawW, drawH),
                                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                            )
                        )
                    }
                    drawPath(
                        path = bgPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(barColor, barColor.copy(alpha = 0.96f))
                        )
                    )

                    // Textured grid overlay lines
                    val lineSpacing = 16.dp.toPx()
                    val gridColor = if (isDark) Color(0xFF1E2849).copy(alpha = 0.12f) else Color(0xFFD3E2F8).copy(alpha = 0.18f)
                    
                    var gridX = 0f
                    while (gridX < drawW) {
                        drawLine(gridColor, Offset(gridX, 0f), Offset(gridX, drawH), 0.5.dp.toPx())
                        gridX += lineSpacing
                    }
                    var gridY = 0f
                    while (gridY < drawH) {
                        drawLine(gridColor, Offset(0f, gridY), Offset(drawW, gridY), 0.5.dp.toPx())
                        gridY += lineSpacing
                    }

                    // Top Accent highlight line with gradient
                    val accentBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent, 
                            if (isDark) BrandPrimary.copy(alpha = 0.5f) else Color(0xFF0F1B6B).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                    drawLine(
                        brush = accentBrush,
                        start = Offset(0f, 0f),
                        end = Offset(drawW, 0f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
                .border(
                    width = 1.dp,
                    color = borderOutline,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    val isSelected = activeRoute == item.route
                    val isHome = item.route == "home"

                    if (isHome) {
                        // Centered raised 3D Home button with primary accent gradient & elevation
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .offset(y = (-10).dp)
                                .shadow(6.dp, CircleShape)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = if (isSelected) {
                                            listOf(BrandPrimary, Color(0xFFC084FC))
                                        } else {
                                            listOf(Color(0xFF3B82F6), BrandPrimary)
                                        }
                                    )
                                )
                                .clickable {
                                    onNavigate("home")
                                }
                                .testTag("nav_btn_home"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home centered button",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        // Standard tab
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)
                        )
                        val selectedColor = if (isDark) BrandPrimary.copy(alpha = 0.15f) else Color(0xFF86EFAC).copy(alpha = 0.3f)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 2.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        onNavigate(item.route)
                                    }
                                )
                                .testTag("nav_btn_${item.tag}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 38.dp, height = 24.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) selectedColor else Color.Transparent
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = if (isSelected) selectedIconTint else unselectedIconTint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = item.title,
                                    fontFamily = InterFontFamily,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 8.5.sp,
                                    color = if (isSelected) selectedIconTint else unselectedIconTint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
