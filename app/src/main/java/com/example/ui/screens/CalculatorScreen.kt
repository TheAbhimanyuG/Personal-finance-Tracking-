package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.components.UnifiedBottomNavBar
import com.example.ui.components.UnifiedTopBar
import com.example.ui.AppViewModel
import com.example.ui.YearlyProjection
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScreen: (String) -> Unit
) {
    val initialInv by viewModel.initialInvestment.collectAsState()
    val monthlyCont by viewModel.monthlyContribution.collectAsState()
    val interestRate by viewModel.interestRate.collectAsState()
    val yearsPeriod by viewModel.yearsPeriod.collectAsState()
    val isCompounding by viewModel.isCompoundingEnabled.collectAsState()
    val frequency by viewModel.compoundingFrequency.collectAsState()
    val currencyUnit by viewModel.selectedCurrency.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var textInitial by remember { mutableStateOf(initialInv.toInt().toString()) }
    var textMonthly by remember { mutableStateOf(monthlyCont.toInt().toString()) }
    var textRate by remember { mutableStateOf(interestRate.toString()) }
    var textYears by remember { mutableStateOf(yearsPeriod.toString()) }

    var isListExpanded by remember { mutableStateOf(false) }

    // Sync input text fields if model changes on external actions
    LaunchedEffect(initialInv) {
        if (initialInv.toInt().toString() != textInitial && textInitial.toDoubleOrNull() != initialInv) {
            textInitial = initialInv.toInt().toString()
        }
    }
    LaunchedEffect(monthlyCont) {
        if (monthlyCont.toInt().toString() != textMonthly && textMonthly.toDoubleOrNull() != monthlyCont) {
            textMonthly = monthlyCont.toInt().toString()
        }
    }

    // Recompute projection arrays
    val projections = remember(initialInv, monthlyCont, interestRate, yearsPeriod, isCompounding, frequency) {
        viewModel.calculateProjectionYears()
    }

    val finalProjection = projections.lastOrNull()
    val totalInvested = finalProjection?.totalInvested ?: 0.0
    val totalInterest = finalProjection?.totalInterest ?: 0.0
    val futureValue = finalProjection?.futureValue ?: 0.0

    // Gain metric calculations
    val gainPercent = remember(totalInvested, futureValue) {
        if (totalInvested > 0) {
            ((futureValue - totalInvested) / totalInvested * 100).toInt()
        } else {
            0
        }
    }

    val currencyPrefix = remember(currencyUnit) {
        when (currencyUnit) {
            "Dollar ($)" -> "$"
            "Euro (€)" -> "€"
            else -> "₹"
        }
    }

    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.US) }
    val compactFormatter = remember { NumberFormat.getNumberInstance(Locale.US) }
    val context = LocalContext.current

    // Aesthetic color schemes matching design specs
    val isDark = isDarkThemeActive
    val scaffoldBg = if (isDark) BrandBackground else Color(0xFFF4F8FD)
    val cardBg = if (isDark) BrandSurface else Color.White
    val borderOutline = if (isDark) BrandOutline else Color(0xFFE5F0FF)
    val mainTextCol = if (isDark) Color.White else Color(0xFF0F1B6B)
    val subTextCol = if (isDark) StitchOnSurfaceVariant else Color(0xFF4B5563)

    // Setup animated entrance factor for the stacked graph bars
    var chartAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        chartAnimated = true
    }
    val animProgress by animateFloatAsState(
        targetValue = if (chartAnimated) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "StackedGraphEntrance"
    )

    // 3 tabs: 0 = Projection, 1 = Calculator Tool, 2 = Currency Converter
    var selectedTab by remember { mutableStateOf(0) }

    // --- Tab 1: Simple Calculator state ---
    var calcDisplay by remember { mutableStateOf("0") }
    var calcExpression by remember { mutableStateOf("") }
    var lastOperator by remember { mutableStateOf("") }
    var operand1 by remember { mutableStateOf<Double?>(null) }
    var clearOnNext by remember { mutableStateOf(false) }

    fun handleCalcInput(input: String) {
        when {
            input in "0123456789." -> {
                if (calcDisplay == "0" || clearOnNext) {
                    calcDisplay = input
                    clearOnNext = false
                } else {
                    if (input == "." && calcDisplay.contains(".")) {
                        // ignore double dot
                    } else {
                        calcDisplay += input
                    }
                }
            }
            input in listOf("+", "-", "×", "÷") -> {
                operand1 = calcDisplay.toDoubleOrNull()
                lastOperator = input
                clearOnNext = true
                calcExpression = "$calcDisplay $input"
            }
            input == "=" -> {
                val op1 = operand1
                val op2 = calcDisplay.toDoubleOrNull()
                if (op1 != null && op2 != null && lastOperator.isNotEmpty()) {
                    val result = when (lastOperator) {
                        "+" -> op1 + op2
                        "-" -> op1 - op2
                        "×" -> op1 * op2
                        "÷" -> if (op2 != 0.0) op1 / op2 else Double.NaN
                        else -> 0.0
                    }
                    calcDisplay = if (result.isNaN()) "Error" else {
                        if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
                    }
                    calcExpression = ""
                    operand1 = null
                    lastOperator = ""
                    clearOnNext = true
                }
            }
            input == "C" -> {
                calcDisplay = "0"
                calcExpression = ""
                operand1 = null
                lastOperator = ""
                clearOnNext = false
            }
            input == "±" -> {
                val num = calcDisplay.toDoubleOrNull()
                if (num != null) {
                    val negated = -num
                    calcDisplay = if (negated % 1.0 == 0.0) negated.toLong().toString() else negated.toString()
                }
            }
            input == "%" -> {
                val num = calcDisplay.toDoubleOrNull()
                if (num != null) {
                    val percentage = num / 100.0
                    calcDisplay = percentage.toString()
                }
            }
        }
    }

    // --- Tab 2: Currency Converter state ---
    var currencyAmount by remember { mutableStateOf("100") }
    var sourceCurrency by remember { mutableStateOf("USD") }
    var targetCurrency by remember { mutableStateOf("INR") }

    val rates = mapOf(
        "INR" to 1.0,
        "USD" to 83.5,
        "EUR" to 90.2,
        "GBP" to 106.1,
        "JPY" to 0.53,
        "CAD" to 61.2,
        "AUD" to 55.4
    )

    val convertedCurrencyAmountMessage = remember(currencyAmount, sourceCurrency, targetCurrency) {
        val amt = currencyAmount.toDoubleOrNull() ?: 0.0
        val srcInInr = amt * (rates[sourceCurrency] ?: 1.0)
        val targetVal = srcInInr / (rates[targetCurrency] ?: 1.0)
        String.format("%.2f %s = %.2f %s", amt, sourceCurrency, targetVal, targetCurrency)
    }

    Scaffold(
        containerColor = scaffoldBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Elegant tab navigation bar at the top of the interface
            val titles = listOf("Projection", "Calculator", "Converter")
            val icons = listOf(Icons.Default.TrendingUp, Icons.Default.Calculate, Icons.Default.Paid)

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = if (isDark) BrandSurface else Color.White,
                contentColor = Color(0xFF3B82F6),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF3B82F6)
                    )
                }
            ) {
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                        icon = { Icon(icons[index], contentDescription = null, modifier = Modifier.size(16.dp)) },
                        selectedContentColor = Color(0xFF3B82F6),
                        unselectedContentColor = Color.Gray
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (selectedTab == 0) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                    // Custom designer grid mesh texture (blueprint engineering style requested)
                    val gridSpacing = 26.dp.toPx()
                    val gridColor = if (isDark) Color(0xFF1A1F38) else Color(0xFFE3EFFC)
                    
                    var currentX = 0f
                    while (currentX < size.width) {
                        drawLine(
                            color = gridColor,
                            start = Offset(currentX, 0f),
                            end = Offset(currentX, size.height),
                            strokeWidth = 0.6.dp.toPx()
                        )
                        currentX += gridSpacing
                    }

                    var currentY = 0f
                    while (currentY < size.height) {
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, currentY),
                            end = Offset(size.width, currentY),
                            strokeWidth = 0.6.dp.toPx()
                        )
                        currentY += gridSpacing
                    }
                },
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // CARD 1: INVESTMENT DETAILS
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Investment Details",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = mainTextCol
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Initial Investment Input
                        Text(
                            text = "Initial Investment",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = subTextCol
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = textInitial,
                            onValueChange = {
                                textInitial = it
                                val parse = it.toDoubleOrNull() ?: 0.0
                                viewModel.initialInvestment.value = parse
                            },
                            leadingIcon = {
                                Text(
                                    text = "$currencyPrefix ",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = mainTextCol,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("initial_investment_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0F1B6B),
                                unfocusedBorderColor = borderOutline,
                                focusedContainerColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Monthly Contribution Input
                        Text(
                            text = "Monthly Contribution",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = subTextCol
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = textMonthly,
                            onValueChange = {
                                textMonthly = it
                                val parse = it.toDoubleOrNull() ?: 0.0
                                viewModel.monthlyContribution.value = parse
                            },
                            leadingIcon = {
                                Text(
                                    text = "$currencyPrefix ",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = mainTextCol,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("monthly_contribution_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0F1B6B),
                                unfocusedBorderColor = borderOutline,
                                focusedContainerColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Rate and Years inputs side-by-side
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rate (%)",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = subTextCol
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = textRate,
                                    onValueChange = {
                                        textRate = it
                                        val parse = it.toDoubleOrNull() ?: 0.0
                                        viewModel.interestRate.value = parse
                                    },
                                    suffix = {
                                        Text(
                                            text = "%",
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = subTextCol
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("rate_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF0F1B6B),
                                        unfocusedBorderColor = borderOutline,
                                        focusedContainerColor = Color.Transparent
                                    )
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Years",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = subTextCol
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = textYears,
                                    onValueChange = {
                                        textYears = it
                                        val parse = it.toIntOrNull() ?: 1
                                        viewModel.yearsPeriod.value = parse
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "Date icon calendar selection symbol",
                                            tint = subTextCol.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("years_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF0F1B6B),
                                        unfocusedBorderColor = borderOutline,
                                        focusedContainerColor = Color.Transparent
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = borderOutline.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(20.dp))

                        // Compound Interest Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Compound Interest",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = mainTextCol
                                )
                                Text(
                                    text = "Include earned interest in calculations",
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.sp,
                                    color = subTextCol,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Switch(
                                checked = isCompounding,
                                onCheckedChange = { viewModel.isCompoundingEnabled.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Color(0xFF0F1B6B)
                                ),
                                modifier = Modifier.testTag("compound_interest_toggle")
                            )
                        }

                        if (isCompounding) {
                            Spacer(modifier = Modifier.height(24.dp))

                            // COMPOUNDING FREQUENCY SELECTOR
                            Text(
                                text = "COMPOUNDING FREQUENCY",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = subTextCol
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Elegant 2x2 custom grid embedded compounding frequency container
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF1E2640).copy(alpha = 0.4f) else Color(0xFFEBF3FC)
                                ),
                                border = BorderStroke(1.dp, borderOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val frequencies = listOf("Daily", "Weekly", "Monthly", "Yearly")
                                    
                                    // Row 1: Daily, Weekly
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        frequencies.take(2).forEach { freq ->
                                            val isSelected = frequency == freq
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) Color(0xFF0F1B6B) else Color.Transparent)
                                                    .clickable { viewModel.compoundingFrequency.value = freq }
                                                    .padding(vertical = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = freq,
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) Color.White else (if (isDark) Color.White else Color(0xFF4B5563))
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Row 2: Monthly, Yearly
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        frequencies.drop(2).forEach { freq ->
                                            val isSelected = frequency == freq
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) Color(0xFF0F1B6B) else Color.Transparent)
                                                    .clickable { viewModel.compoundingFrequency.value = freq }
                                                    .padding(vertical = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = freq,
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) Color.White else (if (isDark) Color.White else Color(0xFF4B5563))
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

            // CARD 2: GROWTH FORECAST MESSAGE BANNER
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2A7E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Growth Forecasting Insight Icon Symbol",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier
                                .size(22.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Growth Forecast",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Based on your parameters, your portfolio is expected to reach substantial growth due to the power of compounding. Consider increasing contributions by 5% annually for better results.",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.82f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // CARD 3: FUTURE VALUE CARD WITH LEFT NAVY ACCENT
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) BrandSurface else Color(0xFFEBF5FF)
                    ),
                    border = BorderStroke(1.dp, borderOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                // Draw trademark premium left bar vertical highlight matching Stitch spec
                                drawRect(
                                    color = Color(0xFF0F1B6B),
                                    topLeft = Offset(0f, 0f),
                                    size = androidx.compose.ui.geometry.Size(5.dp.toPx(), size.height)
                                )
                            }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FUTURE VALUE",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = subTextCol,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$currencyPrefix${compactFormatter.format(futureValue.toInt())}",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                color = mainTextCol
                            )
                        }
                        
                        // Percentage growth pill tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "~$gainPercent%",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }
                }
            }

            // CARD 4: TOTAL INTEREST GAINED CARD WITH LEFT GREEN ACCENT
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) BrandSurface else Color(0xFFEBF5FF)
                    ),
                    border = BorderStroke(1.dp, borderOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                // Draw trademark premium left bar vertical highlight matching green spec
                                drawRect(
                                    color = Color(0xFF16A34A),
                                    topLeft = Offset(0f, 0f),
                                    size = androidx.compose.ui.geometry.Size(5.dp.toPx(), size.height)
                                )
                            }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TOTAL INTEREST GAINED",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = subTextCol,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$currencyPrefix${compactFormatter.format(totalInterest.toInt())}",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }
                }
            }

            // CARD 5: PROJECTION CHART (BAR STACK CHART)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "Projection Chart",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = mainTextCol
                                )
                                Text(
                                    text = "Growth of Principal vs Interest",
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.sp,
                                    color = subTextCol,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            
                            // Stack chart bullet indicators
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0F1B6B))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "PRINCIPAL",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = subTextCol
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF86EFAC))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "INTEREST",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = subTextCol
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Stack Chart Layout Render (Adjustable based on projections)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .drawBehind {
                                    // Soft static horizontal lines inside the bar chart box context
                                    val gridColor = borderOutline.copy(alpha = 0.5f)
                                    drawLine(gridColor, Offset(0f, 0f), Offset(size.width, 0f), 0.8.dp.toPx())
                                    drawLine(gridColor, Offset(0f, size.height * 0.33f), Offset(size.width, size.height * 0.33f), 0.8.dp.toPx())
                                    drawLine(gridColor, Offset(0f, size.height * 0.66f), Offset(size.width, size.height * 0.66f), 0.8.dp.toPx())
                                    drawLine(borderOutline, Offset(0f, size.height), Offset(size.width, size.height), 1.5.dp.toPx())
                                },
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Downsample projection array size to prevent cramming in wide spans (max 15 bar slots)
                            val samplesCount = 15
                            val indicesToDisplay = remember(projections) {
                                if (projections.isEmpty()) emptyList()
                                else if (projections.size <= samplesCount) projections.indices.toList()
                                else {
                                    val step = projections.size.toFloat() / samplesCount
                                    (0 until samplesCount).map { i -> (i * step).toInt().coerceIn(projections.indices) }
                                }
                            }

                            val maximumTotal = remember(projections) {
                                projections.maxOfOrNull { it.futureValue } ?: 1.0
                            }

                            indicesToDisplay.forEach { idx ->
                                val proj = projections[idx]
                                val totalScale = proj.futureValue / maximumTotal
                                
                                val principalScale = (proj.totalInvested / proj.futureValue) * totalScale
                                val interestScale = (proj.totalInterest / proj.futureValue) * totalScale

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.Bottom,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.Bottom,
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            // Top stacked bar (Mint Green - Interest)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .fillMaxHeight((interestScale * animProgress).toFloat().coerceIn(0f, 1f))
                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                    .background(Color(0xFF86EFAC))
                                            )
                                            // Bottom stacked bar (Navy Blue - Principal)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .fillMaxHeight((principalScale * animProgress).toFloat().coerceIn(0f, 1f))
                                                    .background(Color(0xFF0F1B6B))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CARD 6: YEARLY BREAKDOWN LIST TABLE
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Yearly Breakdown",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = mainTextCol
                            )
                            
                            Text(
                                text = if (isListExpanded) "Show Less" else "View All",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F1B6B),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isListExpanded = !isListExpanded }
                                    .padding(6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Limit display length based on expansion state (e.g., 5 or length)
                        val displayProjections = if (isListExpanded) projections else projections.take(5)

                        displayProjections.forEachIndexed { index, proj ->
                            YearlyBreakdownItemRow(
                                proj = proj,
                                index = index + 1,
                                isDark = isDark,
                                borderOutline = borderOutline,
                                mainTextCol = mainTextCol,
                                subTextCol = subTextCol,
                                currencyPrefix = currencyPrefix,
                                compactFormatter = compactFormatter
                            )
                            
                            if (index < displayProjections.size - 1) {
                                HorizontalDivider(
                                    color = borderOutline.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // SAVE PROJECTION CTA BUTTON
            item {
                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Success! Compound projection details stored to historical archive.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_projection_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1B6B))
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save disk floppy icon",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Projection",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }

            // Margin buffering spacers
            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
                } else if (selectedTab == 1) {
                    SimpleCalculatorView(isDark, cardBg, mainTextCol, borderOutline, calcDisplay, calcExpression, ::handleCalcInput)
                } else {
                    CurrencyConverterView(isDark, cardBg, mainTextCol, borderOutline, currencyAmount, { currencyAmount = it }, sourceCurrency, { sourceCurrency = it }, targetCurrency, { targetCurrency = it }, convertedCurrencyAmountMessage, rates.keys.toList())
                }
            }
        }
    }
}

// Single breakdown row design matching design spec
@Composable
fun YearlyBreakdownItemRow(
    proj: YearlyProjection,
    index: Int,
    isDark: Boolean,
    borderOutline: Color,
    mainTextCol: Color,
    subTextCol: Color,
    currencyPrefix: String,
    compactFormatter: NumberFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular index badge
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF222C42) else Color(0xFFEBF5FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isDark) BrandSecondary else Color(0xFF0F1B6B)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Body Info details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Year $index Summary",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = mainTextCol
            )
            Text(
                text = "Accrued Interest: $currencyPrefix${compactFormatter.format(proj.totalInterest.toInt())}",
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                color = subTextCol
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Values amounts with trend indicator on right
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$currencyPrefix${compactFormatter.format(proj.futureValue.toInt())}",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = mainTextCol
            )
            
            val scalePercentage = if (proj.totalInvested > 0) {
                ((proj.futureValue - proj.totalInvested) / proj.totalInvested * 100).toInt()
            } else {
                0
            }
            Text(
                text = "+$scalePercentage% GAIN",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color(0xFF16A34A)
            )
        }
    }
}

// Bottom Navbar item definition
data class CalculatorBottomNavItem(val name: String, val icon: ImageVector, val activeTag: String)

@Composable
fun CalculatorBottomNavLocal(
    activeScreen: String,
    onNavigate: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = if (isDarkThemeActive) BrandSurfaceContainerLow else Color(0xFFEBF3FC),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                CalculatorBottomNavItem("Dashboard", Icons.Default.Dashboard, "home"),
                CalculatorBottomNavItem("History", Icons.Default.History, "reports"),
                CalculatorBottomNavItem("Invest", Icons.Default.AccountBalance, "calculator"),
                CalculatorBottomNavItem("Settings", Icons.Default.Settings, "settings")
            )

            items.forEach { item ->
                val isActive = item.activeTag == "calculator"
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDarkThemeActive) BrandPrimary.copy(alpha = 0.2f) else Color(0xFF86EFAC))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.name,
                                tint = if (isDarkThemeActive) BrandPrimary else Color(0xFF0F1B6B),
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily,
                                color = if (isDarkThemeActive) BrandPrimary else Color(0xFF0F1B6B)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onNavigate(item.activeTag)
                            }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.name,
                            tint = if (isDarkThemeActive) Color.White.copy(alpha = 0.6f) else Color(0xFF475569),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.name,
                            fontSize = 11.sp,
                            fontFamily = InterFontFamily,
                            color = if (isDarkThemeActive) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleCalculatorView(
    isDark: Boolean,
    cardBg: Color,
    mainTextCol: Color,
    borderOutline: Color,
    displayStr: String,
    expressionStr: String,
    onInput: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, borderOutline),
            modifier = Modifier
                .widthIn(max = 380.dp)
                .wrapContentHeight()
                .shadow(4.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Calculator Display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End
                ) {
                    if (expressionStr.isNotEmpty()) {
                        Text(
                            text = expressionStr,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayStr,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Grid of Buttons
                val buttons = listOf(
                    listOf("C", "±", "%", "÷"),
                    listOf("7", "8", "9", "×"),
                    listOf("4", "5", "6", "−"),
                    listOf("1", "2", "3", "+"),
                    listOf("0", ".", "=")
                )

                buttons.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { btn ->
                            val isOperator = btn in listOf("÷", "×", "−", "+", "=")
                            val isSpecial = btn in listOf("C", "±", "%")
                            val weight = if (btn == "0") 2f else 1f

                            Box(
                                modifier = Modifier
                                    .weight(weight)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        when {
                                            isOperator -> if (btn == "=") Color(0xFF3B82F6) else Color(0xFFF59E0B)
                                            isSpecial -> if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                            else -> if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                                        }
                                    )
                                    .clickable {
                                        // map visual minus to standard operator
                                        val actualOp = if (btn == "−") "-" else btn
                                        onInput(actualOp)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = btn,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isOperator -> Color.White
                                        isSpecial -> if (isDark) Color.LightGray else Color.DarkGray
                                        else -> if (isDark) Color.White else Color(0xFF0F172A)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterView(
    isDark: Boolean,
    cardBg: Color,
    mainTextCol: Color,
    borderOutline: Color,
    amountStr: String,
    onAmountChange: (String) -> Unit,
    sourceCurr: String,
    onSourceChange: (String) -> Unit,
    targetCurr: String,
    onTargetChange: (String) -> Unit,
    resultStr: String,
    currencies: List<String>
) {
    var sourceExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, borderOutline),
            modifier = Modifier
                .widthIn(max = 380.dp)
                .wrapContentHeight()
                .shadow(4.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Currency Converter",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = mainTextCol
                )

                // Input Amount
                Column {
                    Text(
                        text = "AMOUNTS TO CONVERT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = onAmountChange,
                        modifier = Modifier.fillMaxWidth().testTag("converter_amount_field"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = borderOutline
                        )
                    )
                }

                // From Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FROM CURRENCY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = sourceExpanded,
                            onExpandedChange = { sourceExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = sourceCurr,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                                modifier = Modifier.menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = borderOutline
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = sourceExpanded,
                                onDismissRequest = { sourceExpanded = false }
                            ) {
                                currencies.forEach { curr ->
                                    DropdownMenuItem(
                                        text = { Text(curr) },
                                        onClick = {
                                            onSourceChange(curr)
                                            sourceExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TO CURRENCY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = targetExpanded,
                            onExpandedChange = { targetExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = targetCurr,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded) },
                                modifier = Modifier.menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = borderOutline
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = targetExpanded,
                                onDismissRequest = { targetExpanded = false }
                            ) {
                                currencies.forEach { curr ->
                                    DropdownMenuItem(
                                        text = { Text(curr) },
                                        onClick = {
                                            onTargetChange(curr)
                                            targetExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Result Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "CONVERTED RESULT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.LightGray else Color(0xFF3B82F6)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = resultStr,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else Color(0xFF0F1B6B),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
