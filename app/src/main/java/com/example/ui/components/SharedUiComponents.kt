package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DbCategory
import com.example.data.model.DbTransaction
import com.example.data.model.DbBudget
import com.example.data.model.DbLiability
import com.example.data.model.DbRecurringItem
import com.example.ui.AppViewModel
import com.example.ui.theme.BrandOutline
import com.example.ui.theme.BrandSurfaceContainer
import com.example.ui.theme.BrandSurfaceContainerLow
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSurface
import java.text.SimpleDateFormat
import java.util.*

// Dynamic Color Palette for Chart
val ChartColorPalette = listOf(
    Color(0xFF3B82F6), // Blue
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFFEF4444), // Red
    Color(0xFF8B5CF6), // Violet
    Color(0xFFEC4899), // Pink
    Color(0xFF06B6D4), // Cyan
    Color(0xFF14B8A6), // Teal
    Color(0xFF84CC16)  // Lime
)

/**
 * Custom modern interactive Donut/Pie Chart with detailed tooltips and responsive highlight on slice tap
 */
@Composable
fun InteractivePieChart(
    dataMap: Map<String, Double>,
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    if (dataMap.isEmpty() || dataMap.values.sum() == 0.0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(if (isDark) Color(0xFF131825) else Color(0xFFF1F5F9), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.DonutLarge, 
                    contentDescription = null, 
                    tint = if (isDark) Color.White.copy(0.4f) else Color.LightGray,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No metrics found in range", 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color.White.copy(0.5f) else Color.Gray
                )
            }
        }
        return
    }

    val totalSum = remember(dataMap) { dataMap.values.sum() }
    var selectedSliceIndex by remember { mutableStateOf<Int?>(null) }
    
    val entries = remember(dataMap) { dataMap.entries.toList() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF0F172A) else Color.White)
            .border(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Multi-level drawing area
        Box(
            modifier = Modifier
                .size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        // Cycles through highlight selection on tap
                        selectedSliceIndex = if (selectedSliceIndex == null) 0 else {
                            val next = selectedSliceIndex!! + 1
                            if (next >= entries.size) null else next
                        }
                    }
            ) {
                var startAngle = -90f
                entries.forEachIndexed { index, entry ->
                    val sweepAngle = ((entry.value / totalSum) * 360f).toFloat()
                    val color = ChartColorPalette[index % ChartColorPalette.size]
                    val isHighlighted = selectedSliceIndex == index
                    
                    val strokeWidth = if (isHighlighted) 28f else 18f
                    val sizeScale = if (isHighlighted) 12f else 0f
                    
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(14f + sizeScale / 2, 14f + sizeScale / 2),
                        size = Size(size.width - 28f - sizeScale, size.height - 28f - sizeScale)
                    )
                    startAngle += sweepAngle
                }
            }

            // Info center focus inside Donut cutout
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (selectedSliceIndex != null && selectedSliceIndex!! < entries.size) {
                    val activeEntry = entries[selectedSliceIndex!!]
                    val prc = (activeEntry.value / totalSum) * 100.0
                    Text(
                        text = activeEntry.key, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = if (isDark) Color.LightGray else Color(0xFF64748B)
                    )
                    Text(
                        text = "₹${formatAmountNoSuffix(activeEntry.value)}", 
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = if (isDark) Color.White else Color(0xFF0F1B6B)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", prc), 
                        fontSize = 10.sp, 
                        color = ChartColorPalette[selectedSliceIndex!! % ChartColorPalette.size],
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Ledger Total", 
                        fontSize = 11.sp, 
                        color = if (isDark) Color.LightGray else Color(0xFF64748B)
                    )
                    Text(
                        text = "₹${formatAmountNoSuffix(totalSum)}", 
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Black, 
                        color = if (isDark) Color.White else Color(0xFF0F1B6B)
                    )
                    Text(
                        text = "Tap slices", 
                        fontSize = 10.sp, 
                        color = if (isDark) Color.Gray else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Row or Grid display of Legends
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 130.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(entries) { index, entry ->
                val color = ChartColorPalette[index % ChartColorPalette.size]
                val pct = (entry.value / totalSum) * 100.0
                val isSelected = selectedSliceIndex == index

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) (if (isDark) Color(0xFF1E293B) else Color(0xFFF3F4F6)) else Color.Transparent)
                        .clickable { selectedSliceIndex = if (isSelected) null else index }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = entry.key,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF1E293B),
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "₹${formatAmountShort(entry.value)} (${String.format(Locale.getDefault(), "%.1f%%", pct)})",
                            fontSize = 10.sp,
                            color = if (isDark) Color.LightGray else Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Premium custom date scope filter widget supporting standard quick modes + detailed Date pickers
 */
@Composable
fun AdvancedDateRangeFilter(
    isDark: Boolean,
    selectedInterval: String, // "Daily", "Weekly", "Monthly", "Quarterly", "All Time", "Custom"
    onIntervalSelected: (String) -> Unit,
    customStartDate: Long?,
    customEndDate: Long?,
    onDatesChanged: (Long?, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var startInput by remember { mutableStateOf("") }
    var endInput by remember { mutableStateOf("") }

    val primaryCol = if (isDark) Color.White else Color(0xFF0F1B6B)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val borderCol = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Filter Ledger Scope",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.LightGray else Color(0xFF475569)
        )

        // Slide Capsule Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val intervals = listOf("Daily", "Weekly", "Monthly", "All Time", "Custom")
            intervals.forEach { interval ->
                val active = selectedInterval == interval
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) Color(0xFF1E2A7E) else Color.Transparent)
                        .clickable { onIntervalSelected(interval) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = interval,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else (if (isDark) Color.White.copy(0.6f) else Color(0xFF64748B))
                    )
                }
            }
        }

        // Show inputs immediately if "Custom" is active
        AnimatedVisibility(
            visible = selectedInterval == "Custom",
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = startInput,
                        onValueChange = { startInput = it },
                        label = { Text("Start Date (DD/MM/YYYY)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("e.g. 10/05/2026") }
                    )

                    OutlinedTextField(
                        value = endInput,
                        onValueChange = { endInput = it },
                        label = { Text("End Date (DD/MM/YYYY)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("e.g. 29/05/2026") }
                    )
                }

                Button(
                    onClick = {
                        val parsedStart = parseSimpleDate(startInput)
                        val parsedEnd = parseSimpleDate(endInput)
                        if (parsedStart != null && parsedEnd != null) {
                            onDatesChanged(parsedStart, parsedEnd)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1B6B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply Range Filter", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (customStartDate != null && customEndDate != null) {
                    Text(
                        text = "Applied: ${formatCalendarDate(customStartDate)} to ${formatCalendarDate(customEndDate)}",
                        fontSize = 11.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Universal Unified Dropdown Form for Add and Modify entries. Premium, smooth Material 3, autolinking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedTransactionFormFields(
    categories: List<DbCategory>,
    isDark: Boolean,
    txTitle: String,
    onTitleChange: (String) -> Unit,
    txAmount: String,
    onAmountChange: (String) -> Unit,
    txType: String,
    onTypeChange: (String) -> Unit,
    txCategory: String,
    onCategoryChange: (String) -> Unit,
    txSubCategory: String,
    onSubCategoryChange: (String) -> Unit,
    txPaymentMethod: String,
    onPaymentMethodChange: (String) -> Unit,
    allowTypeChange: Boolean = true, // If false, locks the entry type (e.g. only Income in Income screen)
    originOfMoney: String = "Income",
    onOriginOfMoneyChange: (String) -> Unit = {},
    originIncomeCategory: String = "",
    onOriginIncomeCategoryChange: (String) -> Unit = {},
    originIncomeSubCategory: String = "",
    onOriginIncomeSubCategoryChange: (String) -> Unit = {},
    originOtherDescription: String = "",
    onOriginOtherDescriptionChange: (String) -> Unit = {}
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var subCategoryExpanded by remember { mutableStateOf(false) }

    // Dynamic filtering lists
    val matchingCategories = remember(categories, txType) {
        val targetType = if (txType.equals("Asset", ignoreCase = true) || txType.equals("Asset & Investment", ignoreCase = true)) {
            "Asset & Investment"
        } else {
            txType
        }
        categories.filter { it.type.equals(targetType, ignoreCase = true) }
    }
    
    // Auto presets based on Assets/Other
    val availableCategoryNames = remember(matchingCategories, txType) {
        val list = matchingCategories.map { it.name }.filter { it.isNotBlank() }
        if (list.isEmpty()) {
            if (txType.equals("Asset", ignoreCase = true) || txType.equals("Assets", ignoreCase = true) || txType.equals("Asset & Investment", ignoreCase = true)) {
                listOf("Stock", "Mutual Fund", "Crypto", "FD", "RD", "Other")
            } else {
                listOf("Other")
            }
        } else {
            list
        }
    }

    val matchingCategoryObj = remember(categories, txCategory) {
        categories.firstOrNull { it.name == txCategory }
    }

    val availableSubCategories = remember(matchingCategoryObj, txType) {
        val list = matchingCategoryObj?.getSubcategories()?.filter { it.isNotBlank() } ?: emptyList()
        if (list.isEmpty()) {
            if (txType.equals("Asset", ignoreCase = true) || txType.equals("Assets", ignoreCase = true) || txType.equals("Asset & Investment", ignoreCase = true)) {
                listOf("Large Cap", "Mid Cap", "Small Cap", "Sovereign Gold Bond", "ETF", "Index Funds", "Savings", "Other")
            } else {
                listOf("General")
            }
        } else {
            list
        }
    }

    // Direct auto selection triggered upon UI interaction or Type change
    LaunchedEffect(txType) {
        if (txType.equals("Asset", ignoreCase = true) || txType.equals("Assets", ignoreCase = true)) {
            val matching = categories.filter { it.type.equals("Asset & Investment", ignoreCase = true) }
            if (matching.isNotEmpty()) {
                onCategoryChange(matching.first().name)
                onSubCategoryChange(matching.first().getSubcategories().firstOrNull() ?: "Large Cap")
            } else {
                onCategoryChange("Stock")
                onSubCategoryChange("Large Cap")
            }
        } else {
            val matching = categories.filter { it.type.equals(txType, ignoreCase = true) }
            if (matching.isNotEmpty()) {
                onCategoryChange(matching.first().name)
                onSubCategoryChange(matching.first().getSubcategories().firstOrNull() ?: "General")
            } else {
                onCategoryChange("Other")
                onSubCategoryChange("General")
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Option select pill box
        if (allowTypeChange) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) BrandSurfaceContainerLow else Color(0xFFF1F5F9))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val types = listOf("Expense", "Income", "Asset")
                types.forEach { typeOption ->
                    val selected = txType == typeOption
                    val actBg = when (typeOption) {
                        "Expense" -> Color(0xFFEF4444)
                        "Income" -> Color(0xFF10B981)
                        else -> Color(0xFF3B82F6)
                    }
                    Button(
                        onClick = { onTypeChange(typeOption) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) actBg else Color.Transparent,
                            contentColor = if (selected) Color.White else (if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B))
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                    ) {
                        Text(text = typeOption, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Displays a stylish locked category title text
            val descCol = when (txType) {
                "Expense" -> Color(0xFFEF4444)
                "Income" -> Color(0xFF10B981)
                else -> Color(0xFF3B82F6)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(descCol.copy(alpha = 0.1f))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LOCKED CATEGORY: $txType", 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = descCol
                )
            }
        }

        // Title text input with custom visual cues
        OutlinedTextField(
            value = txTitle,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth().testTag("form_tx_title"),
            leadingIcon = { Icon(imageVector = Icons.Default.EditNote, contentDescription = null, tint = if (isDark) Color.Gray else Color.LightGray) },
            label = { Text("Transaction details (e.g. Whole Foods)") },
            singleLine = true
        )

        // Amount input with India currency symbol
        OutlinedTextField(
            value = txAmount,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth().testTag("form_tx_amount"),
            leadingIcon = { Text("₹ ", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF1E293B)) },
            label = { Text("Funds amount") },
            singleLine = true
        )

        // Selection Box for Categories (Auto-Linked)
        Text(text = "Linked Asset Category", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B))
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, if (isDark) BrandOutline else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                    .clickable { categoryExpanded = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = txCategory.ifBlank { "Select Category" }, fontSize = 13.sp, color = if (isDark) Color.White else Color.Black)
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false },
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                availableCategoryNames.forEach { catName ->
                    DropdownMenuItem(
                        text = { Text(text = catName, fontSize = 13.sp) },
                        onClick = {
                            onCategoryChange(catName)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        // Selection Box for Subcategories (Auto-Linked)
        Text(text = "Segment Subclass", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B))
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, if (isDark) BrandOutline else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                    .clickable { subCategoryExpanded = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = txSubCategory.ifBlank { "Select Subcategory" }, fontSize = 13.sp, color = if (isDark) Color.White else Color.Black)
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = subCategoryExpanded,
                onDismissRequest = { subCategoryExpanded = false },
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                availableSubCategories.forEach { subName ->
                    DropdownMenuItem(
                        text = { Text(text = subName, fontSize = 13.sp) },
                        onClick = {
                            onSubCategoryChange(subName)
                            subCategoryExpanded = false
                        }
                    )
                }
            }
        }

        // Payment Method choices
        Text(text = "Payment Conduit", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val payments = listOf("Cash", "Debit Card", "Credit Card")
            payments.forEach { pay ->
                val active = txPaymentMethod == pay
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (active) Color(0xFF0F1B6B) else (if (isDark) BrandSurfaceContainer else Color(0xFFEEF2F6))
                        )
                        .clickable { onPaymentMethodChange(pay) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pay,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else (if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF4A5568))
                    )
                }
            }
        }

        // New Asset data entry logic
        if (txType.equals("Asset", ignoreCase = true) || txType.equals("Assets", ignoreCase = true) || txType.equals("Asset & Investment", ignoreCase = true)) {
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = if (isDark) BrandOutline.copy(alpha = 0.4f) else Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "ORIGIN OF MONEY (MANDATORY)",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color(0xFF3B82F6) else Color(0xFF0F1B6B),
                letterSpacing = 0.5.sp
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) BrandSurfaceContainerLow else Color(0xFFF1F5F9))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val originOptions = listOf("Income", "Other")
                originOptions.forEach { option ->
                    val selected = originOfMoney == option
                    val actBg = if (option == "Income") Color(0xFF10B981) else Color(0xFF2563EB)
                    Button(
                        onClick = { onOriginOfMoneyChange(option) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) actBg else Color.Transparent,
                            contentColor = if (selected) Color.White else (if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B))
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(text = option, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (originOfMoney == "Income") {
                val incomeCategories = remember(categories) {
                    categories.filter { it.type.equals("Income", ignoreCase = true) }
                }
                
                LaunchedEffect(incomeCategories) {
                    if (originIncomeCategory.isBlank() && incomeCategories.isNotEmpty()) {
                        onOriginIncomeCategoryChange(incomeCategories.first().name)
                        onOriginIncomeSubCategoryChange(incomeCategories.first().getSubcategories().firstOrNull() ?: "General")
                    }
                }
                
                var originCatExpanded by remember { mutableStateOf(false) }
                var originSubCatExpanded by remember { mutableStateOf(false) }
                
                val selectedIncomeCatObj = remember(incomeCategories, originIncomeCategory) {
                    incomeCategories.firstOrNull { it.name == originIncomeCategory }
                }
                
                val originSubCategoriesList = remember(selectedIncomeCatObj) {
                    selectedIncomeCatObj?.getSubcategories()?.ifEmpty { listOf("General") } ?: listOf("General")
                }
                
                Text(
                    text = "Select Origin Income Category",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, if (isDark) BrandOutline else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                            .clickable { originCatExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = originIncomeCategory.ifBlank { "Select Income Category" }, fontSize = 13.sp, color = if (isDark) Color.White else Color.Black)
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = originCatExpanded,
                        onDismissRequest = { originCatExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.75f)
                    ) {
                        incomeCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(text = cat.name, fontSize = 13.sp) },
                                onClick = {
                                    onOriginIncomeCategoryChange(cat.name)
                                    onOriginIncomeSubCategoryChange(cat.getSubcategories().firstOrNull() ?: "General")
                                    originCatExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Text(
                    text = "Select Origin Income Subcategory",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, if (isDark) BrandOutline else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                            .clickable { originSubCatExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = originIncomeSubCategory.ifBlank { "Select Income Subcategory" }, fontSize = 13.sp, color = if (isDark) Color.White else Color.Black)
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = originSubCatExpanded,
                        onDismissRequest = { originSubCatExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.75f)
                    ) {
                        originSubCategoriesList.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(text = sub, fontSize = 13.sp) },
                                onClick = {
                                    onOriginIncomeSubCategoryChange(sub)
                                    originSubCatExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = originOtherDescription,
                    onValueChange = {
                        if (it.length <= 100) {
                            onOriginOtherDescriptionChange(it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(imageVector = Icons.Default.Description, contentDescription = null) },
                    label = { Text("Origin Description (Max 100 chars)") },
                    supportingText = {
                        Text(
                            text = "${originOtherDescription.length}/100",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    },
                    singleLine = true,
                    isError = originOtherDescription.isBlank()
                )
            }
        }
    }
}

/**
 * Universal Unified Dialog supporting standard "Save" naming conventions. Reusable design system wide.
 */
@Composable
fun UnifiedAddEditTxDialog(
    show: Boolean,
    editingTxn: DbTransaction?, // Null if creating a new transaction
    onDismiss: () -> Unit,
    categories: List<DbCategory>,
    isDark: Boolean,
    txTypeLocked: String? = null, // Locks the transaction type if provided (e.g. "Income" inside Income Panel)
    viewModel: AppViewModel? = null, // Holds references to execute dynamic budget operations & on-the-fly additions
    onSave: (
        title: String, 
        amount: Double, 
        type: String, 
        category: String, 
        subCategory: String, 
        payment: String,
        originOfMoney: String?,
        originIncomeCategory: String?,
        originIncomeSubCategory: String?,
        originOtherDescription: String?
    ) -> Unit,
    onSaveWithFundsSource: ((
        title: String, 
        amount: Double, 
        type: String, 
        category: String, 
        subCategory: String, 
        payment: String,
        originOfMoney: String?,
        originIncomeCategory: String?,
        originIncomeSubCategory: String?,
        originOtherDescription: String?,
        fundsSource: String
    ) -> Unit)? = null
) {
    if (!show) return

    val context = LocalContext.current
    var showSaveConfirmation by remember { mutableStateOf(false) }
    
    // Core transaction input states
    var txTitle by remember { mutableStateOf(editingTxn?.title ?: "") }
    var txAmount by remember { mutableStateOf(editingTxn?.amount?.toString() ?: "") }
    var txType by remember { mutableStateOf(editingTxn?.type ?: txTypeLocked ?: "Expense") }
    var txCategory by remember { mutableStateOf(editingTxn?.category ?: "") }
    var txSubCategory by remember { mutableStateOf(editingTxn?.subCategory ?: "") }
    var txPaymentMethod by remember { mutableStateOf(editingTxn?.paymentMethod ?: "Cash") }
    var txFundsSource by remember { mutableStateOf(editingTxn?.fundsSource ?: "") }

    val recurringItemsState = if (viewModel != null) {
        viewModel.userRecurringItems.collectAsState()
    } else {
        remember { mutableStateOf(emptyList<DbRecurringItem>()) }
    }
    val recurringItems = recurringItemsState.value

    var recurringTypeSelection by remember { mutableStateOf("Other") }
    var recurringTypeExpanded by remember { mutableStateOf(false) }
    var selectedRecurringItemId by remember { mutableStateOf<Long?>(null) }
    var recurringItemSelectorExpanded by remember { mutableStateOf(false) }

    // Asset origin states
    var originOfMoney by remember(editingTxn) {
        mutableStateOf(
            if (editingTxn != null) {
                if (editingTxn.sourceType == "SOURCE_OTHER") "Other" else "Income"
            } else "Income"
        )
    }
    var originIncomeCategory by remember { mutableStateOf("") }
    var originIncomeSubCategory by remember { mutableStateOf("") }
    var originOtherDescription by remember { mutableStateOf("") }

    // Budget ledger fields (only applicable when tab is Budget)
    val userBudgets by if (viewModel != null) {
        viewModel.userBudgets.collectAsState()
    } else {
        remember { mutableStateOf(emptyList<DbBudget>()) }
    }
    
    var selectedBudgetId by remember { mutableStateOf<Long?>(null) }
    var budgetAction by remember { mutableStateOf("Top up") } // "Top up" or "Spend"
    var budgetAmount by remember { mutableStateOf("") }
    var budgetNotes by remember { mutableStateOf("") }
    
    // Budget Top Up source
    var topUpSource by remember { mutableStateOf("Income") } // "Income", "Asset & Investment", "Other"
    var topUpDeductionCategory by remember { mutableStateOf("") }
    var topUpDeductionSubCategory by remember { mutableStateOf("") }

    // Budget Spend fields
    var spendMerchantName by remember { mutableStateOf("") }
    var spendSubcategorySegment by remember { mutableStateOf("") }

    // Dropdown expanded states
    var categoryExpanded by remember { mutableStateOf(false) }
    var subCategoryExpanded by remember { mutableStateOf(false) }
    var budgetSelectExpanded by remember { mutableStateOf(false) }
    var topUpSourceExpanded by remember { mutableStateOf(false) }
    var topUpCatExpanded by remember { mutableStateOf(false) }
    var topUpSubcatExpanded by remember { mutableStateOf(false) }

    // On-the-fly overlays
    var showCategoryCreateDialog by remember { mutableStateOf(false) }
    var onTheFlyCategoryName by remember { mutableStateOf("") }
    
    var showSubcategoryCreateDialog by remember { mutableStateOf(false) }
    var onTheFlySubcategoryName by remember { mutableStateOf("") }

    var showBudgetCreateDialog by remember { mutableStateOf(false) }
    var onTheFlyBudgetName by remember { mutableStateOf("") }
    var onTheFlyBudgetLimit by remember { mutableStateOf("") }

    // Initialize auto selection values for Category and Subcategory
    val availableCategoryNames = remember(categories, txType) {
        val list = categories.filter {
            if (txType == "Asset") {
                it.type.equals("Asset & Investment", ignoreCase = true)
            } else {
                it.type.equals(txType, ignoreCase = true)
            }
        }.map { it.name }
        if (list.isEmpty()) listOf("Other") else list
    }

    val matchingCategoryObj = remember(categories, txCategory) {
        categories.firstOrNull { it.name == txCategory }
    }

    val availableSubCategories = remember(matchingCategoryObj, txType) {
        val list = matchingCategoryObj?.getSubcategories()?.filter { it.isNotBlank() } ?: emptyList()
        if (list.isEmpty()) {
            if (txType == "Asset") {
                listOf("Large Cap", "Mid Cap", "Small Cap", "Sovereign Gold Bond", "ETF", "Index Funds", "Savings", "Other")
            } else {
                listOf("General")
            }
        } else {
            list
        }
    }

    LaunchedEffect(txType) {
        if (txType == "Budget") {
            // No-op for category initialization since Budgets handles parent list directly
            if (selectedBudgetId == null && userBudgets.isNotEmpty()) {
                selectedBudgetId = userBudgets.first().id
            }
        } else {
            if (txCategory.isBlank() && availableCategoryNames.isNotEmpty()) {
                txCategory = availableCategoryNames.first()
            }
        }
    }

    LaunchedEffect(txCategory) {
        if (availableSubCategories.isNotEmpty()) {
            txSubCategory = availableSubCategories.first()
        } else {
            txSubCategory = "General"
        }
    }

    // Main Dialog UI container
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (txType == "Budget") {
                        val amt = budgetAmount.toDoubleOrNull() ?: 0.0
                        val budgetId = selectedBudgetId
                        if (budgetId == null) {
                            Toast.makeText(context, "Please select/create a budget first!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (amt <= 0.0) {
                            Toast.makeText(context, "Amount must be greater than zero!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                    } else {
                        // Standard Universal transaction flow submission
                        val amtValue = txAmount.toDoubleOrNull() ?: 0.0
                        if (txTitle.isBlank()) {
                            Toast.makeText(context, "Transaction details cannot be empty!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (amtValue <= 0.0) {
                            Toast.makeText(context, "Please key in a valid positive funds amount!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (txType == "Asset") {
                            if (originOfMoney == "Other" && originOtherDescription.isBlank()) {
                                Toast.makeText(context, "Kindly describe the outside source of capital!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                        }
                    }
                    showSaveConfirmation = true
                },
                modifier = Modifier.fillMaxWidth().testTag("add_transaction_submit"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (txType) {
                        "Income" -> Color(0xFF10B981)
                        "Expense" -> Color(0xFFEF4444)
                        "Asset" -> Color(0xFF2563EB)
                        "Budget" -> Color(0xFFEC4899)
                        else -> Color(0xFF0F1B6B)
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (txType == "Budget") "Confirm Budget Action" else "Confirm & Register Entry",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Discard / Cancel", color = if (isDark) Color.LightGray else Color.Gray, fontSize = 12.sp)
            }

            // High priority final stage submit confirmation trigger dialog
            if (showSaveConfirmation) {
                AlertDialog(
                    onDismissRequest = { showSaveConfirmation = false },
                    title = {
                        Text(
                            text = "Are you sure?",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (isDark) Color.White else Color(0xFF0F1B6B)
                        )
                    },
                    text = {
                        Text(
                            text = "Do you want to confirm and register this entry in the ledger?",
                            fontSize = 13.sp,
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showSaveConfirmation = false
                                if (txType == "Budget") {
                                    val amt = budgetAmount.toDoubleOrNull() ?: 0.0
                                    val budgetId = selectedBudgetId
                                    if (budgetId != null && amt > 0.0) {
                                        if (budgetAction == "Top up") {
                                            viewModel?.topUpBudgetOrGoal(
                                                parentId = budgetId,
                                                isBudget = true,
                                                amount = amt,
                                                source = topUpSource,
                                                category = topUpDeductionCategory,
                                                subCategory = topUpDeductionSubCategory,
                                                description = budgetNotes
                                            ) { success, msg ->
                                                if (success) onDismiss() else Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            viewModel?.spendFromBudgetOrGoal(
                                                parentId = budgetId,
                                                isBudget = true,
                                                amount = amt,
                                                merchant = spendMerchantName,
                                                date = System.currentTimeMillis(),
                                                subcategory = spendSubcategorySegment.ifBlank { "General" },
                                                description = budgetNotes
                                            ) { success, msg ->
                                                if (success) onDismiss() else Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    val amtValue = txAmount.toDoubleOrNull() ?: 0.0
                                    if (onSaveWithFundsSource != null) {
                                        onSaveWithFundsSource(
                                            txTitle,
                                            amtValue,
                                            txType,
                                            txCategory.ifBlank { "Other" },
                                            txSubCategory.ifBlank { "General" },
                                            txPaymentMethod,
                                            originOfMoney,
                                            if (originOfMoney == "Income") originIncomeCategory else null,
                                            if (originOfMoney == "Income") originIncomeSubCategory else null,
                                            if (originOfMoney == "Other") originOtherDescription else null,
                                            txFundsSource
                                        )
                                    } else {
                                        onSave(
                                            txTitle, 
                                            amtValue, 
                                            txType, 
                                            txCategory.ifBlank { "Other" }, 
                                            txSubCategory.ifBlank { "General" }, 
                                            txPaymentMethod,
                                            originOfMoney,
                                            if (originOfMoney == "Income") originIncomeCategory else null,
                                            if (originOfMoney == "Income") originIncomeSubCategory else null,
                                            if (originOfMoney == "Other") originOtherDescription else null
                                        )
                                    }
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandPrimary else Color(0xFF0F1B6B))
                        ) {
                            Text("Yes", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSaveConfirmation = false }) {
                            Text("No", fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    },
                    containerColor = if (isDark) BrandSurface else Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        title = {
            Text(
                text = if (editingTxn != null) "Modify Existing Transaction Ledger" else "Universal Transaction Panel",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = if (isDark) Color.White else Color(0xFF0F1B6B)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Dynamic Option Select Row (Tabs)
                if (txTypeLocked == null && editingTxn == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "TRANSACTION GROUP SELECTION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val tabs = listOf("Income", "Expense", "Asset", "Budget")
                            tabs.forEach { tabName ->
                                val selected = txType == tabName
                                val activeBg = when (tabName) {
                                    "Income" -> Color(0xFFDCFCE7)
                                    "Expense" -> Color(0xFFFEE2E2)
                                    "Asset" -> Color(0xFFDBEAFE)
                                    "Budget" -> Color(0xFFFCE7F3)
                                    else -> Color.Gray
                                }
                                val activeText = when (tabName) {
                                    "Income" -> Color(0xFF15803D)
                                    "Expense" -> Color(0xFFB91C1C)
                                    "Asset" -> Color(0xFF1D4ED8)
                                    "Budget" -> Color(0xFFBE185D)
                                    else -> Color.Black
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) activeBg else Color.Transparent)
                                        .clickable { txType = tabName }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tabName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (selected) activeText else (if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B))
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Displays locked type label
                    val activeColor = when (txType) {
                        "Income" -> Color(0xFF10B981)
                        "Expense" -> Color(0xFFEF4444)
                        "Asset" -> Color(0xFF2563EB)
                        "Budget" -> Color(0xFFEC4899)
                        else -> Color(0xFF0F1B6B)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(activeColor.copy(alpha = 0.1f))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LOCKED FLOW: ${txType.uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = activeColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Conditional Layout Rendering: Standard Flows vs Budget Specific Flows
                if (txType == "Budget") {
                    // --- BUDGET-SPECIFIC FLOWS ---
                    
                    // Input 1: Select Budget
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SELECT TARGET BUDGET",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "+ Create New Budget",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEC4899),
                                modifier = Modifier.clickable { showBudgetCreateDialog = true }
                            )
                        }
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val selectedBudgetObj = userBudgets.firstOrNull { it.id == selectedBudgetId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isDark) BrandOutline else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                    .clickable { budgetSelectExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedBudgetObj?.let { "${it.name} (Limit: ₹${it.limitAmount})" } ?: "Select Active Budget Profile",
                                    fontSize = 12.sp,
                                    color = if (isDark) Color.White else Color.Black
                                )
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = budgetSelectExpanded,
                                onDismissRequest = { budgetSelectExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                if (userBudgets.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No active budgets found.", fontSize = 13.sp) },
                                        onClick = { budgetSelectExpanded = false }
                                    )
                                } else {
                                    userBudgets.forEach { budgetObj ->
                                        DropdownMenuItem(
                                            text = { Text("${budgetObj.name} (Limit: ₹${budgetObj.limitAmount})", fontSize = 12.sp) },
                                            onClick = {
                                                selectedBudgetId = budgetObj.id
                                                budgetSelectExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Input 2: Action cards Side-by-Side (Top up vs Spend)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "CHOOSE BUDGET ACTION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Top-Up Action Selector
                            val selectionTopUp = budgetAction == "Top up"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (selectionTopUp) 2.dp else 1.dp,
                                        color = if (selectionTopUp) Color(0xFF10B981) else (if (isDark) BrandOutline else Color(0xFFCBD5E1)),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(if (selectionTopUp) Color(0xFFDCFCE7).copy(alpha = 0.4f) else Color.Transparent)
                                    .clickable { budgetAction = "Top up"; budgetAmount = "" }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Top Up", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectionTopUp) Color(0xFF15803D) else (if (isDark) Color.White else Color.Black))
                                }
                            }

                            // Spend Action Selector
                            val selectionSpend = budgetAction == "Spend"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (selectionSpend) 2.dp else 1.dp,
                                        color = if (selectionSpend) Color(0xFFEF4444) else (if (isDark) BrandOutline else Color(0xFFCBD5E1)),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(if (selectionSpend) Color(0xFFFEE2E2).copy(alpha = 0.4f) else Color.Transparent)
                                    .clickable { budgetAction = "Spend"; budgetAmount = "" }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Spend", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectionSpend) Color(0xFFB91C1C) else (if (isDark) Color.White else Color.Black))
                                }
                            }
                        }
                    }

                    // Render Sub-Panel Inputs for the Selected Action
                    if (budgetAction == "Top up") {
                        // Amount Top Up Input
                        OutlinedTextField(
                            value = budgetAmount,
                            onValueChange = { budgetAmount = it },
                            modifier = Modifier.fillMaxWidth().testTag("budget_topup_amount"),
                            leadingIcon = { Text("₹ ", fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981)) },
                            label = { Text("Top Up Funds Amount") },
                            singleLine = true
                        )

                        // Source Selector for Top up
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "TOP UP FUNDING SOURCE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B)
                            )
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, if (isDark) BrandOutline else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                        .clickable { topUpSourceExpanded = true }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = topUpSource, fontSize = 12.sp, color = if (isDark) Color.White else Color.Black)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = topUpSourceExpanded,
                                    onDismissRequest = { topUpSourceExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                ) {
                                    val sources = listOf("Income", "Asset", "Other")
                                    sources.forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(s, fontSize = 12.sp) },
                                            onClick = {
                                                topUpSource = s
                                                topUpSourceExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Select Source Categories if Income or Asset & Investment
                        if (topUpSource == "Income" || topUpSource == "Asset") {
                            val resolvedSourceType = if (topUpSource == "Asset") "Asset & Investment" else "Income"
                            val filteredCats = remember(categories, resolvedSourceType) {
                                categories.filter { it.type.equals(resolvedSourceType, ignoreCase = true) }
                            }
                            
                            LaunchedEffect(filteredCats) {
                                if (filteredCats.isNotEmpty()) {
                                    topUpDeductionCategory = filteredCats.first().name
                                    topUpDeductionSubCategory = filteredCats.first().getSubcategories().firstOrNull() ?: "General"
                                }
                            }
                            
                            val activeDeductionCatObj = remember(categories, topUpDeductionCategory) {
                                categories.firstOrNull { it.name == topUpDeductionCategory }
                            }
                            
                            val deductionSubcats = remember(activeDeductionCatObj) {
                                activeDeductionCatObj?.getSubcategories()?.ifEmpty { listOf("General") } ?: listOf("General")
                            }

                            // Category
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "DEDUCTION CATEGORY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B))
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, if (isDark) BrandOutline else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                            .clickable { topUpCatExpanded = true }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = topUpDeductionCategory.ifBlank { "Select Category" }, fontSize = 12.sp, color = if (isDark) Color.White else Color.Black)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = topUpCatExpanded,
                                        onDismissRequest = { topUpCatExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    ) {
                                        filteredCats.forEach { c ->
                                            DropdownMenuItem(
                                                text = { Text(c.name, fontSize = 12.sp) },
                                                onClick = {
                                                    topUpDeductionCategory = c.name
                                                    topUpDeductionSubCategory = c.getSubcategories().firstOrNull() ?: "General"
                                                    topUpCatExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Subcategory
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "DEDUCTION SUBCATEGORY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B))
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, if (isDark) BrandOutline else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                            .clickable { topUpSubcatExpanded = true }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = topUpDeductionSubCategory.ifBlank { "Select Subcategory" }, fontSize = 12.sp, color = if (isDark) Color.White else Color.Black)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = topUpSubcatExpanded,
                                        onDismissRequest = { topUpSubcatExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    ) {
                                        deductionSubcats.forEach { sub ->
                                            DropdownMenuItem(
                                                text = { Text(sub, fontSize = 12.sp) },
                                                onClick = {
                                                    topUpDeductionSubCategory = sub
                                                    topUpSubcatExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Notes Field
                        OutlinedTextField(
                            value = budgetNotes,
                            onValueChange = { budgetNotes = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Notes / Purpose (Optional)") },
                            singleLine = true
                        )

                    } else {
                        // Spend sub-form
                        OutlinedTextField(
                            value = budgetAmount,
                            onValueChange = { budgetAmount = it },
                            modifier = Modifier.fillMaxWidth().testTag("budget_spend_amount"),
                            leadingIcon = { Text("₹ ", fontWeight = FontWeight.ExtraBold, color = Color(0xFFEF4444)) },
                            label = { Text("Spend Funds Amount") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = spendMerchantName,
                            onValueChange = { spendMerchantName = it },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(imageVector = Icons.Default.Storefront, contentDescription = null) },
                            label = { Text("Merchant / Receiver details") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = spendSubcategorySegment,
                            onValueChange = { spendSubcategorySegment = it },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(imageVector = Icons.Default.Segment, contentDescription = null) },
                            label = { Text("Segment / Sub-Budget classification") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = budgetNotes,
                            onValueChange = { budgetNotes = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Description / Notes (Optional)") },
                            singleLine = true
                        )
                    }

                } else {
                    // --- UNIVERSAL FLOWS (INCOME, EXPENSE, ASSET) ---

                    if (txType == "Expense") {
                        // RECURRING LOGIC SELECTION
                        // RECURRING LOGIC SELECTION IN 3x3 GRID FORMAT
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "SELECT TRANSACTION ENTRY METHOD",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            
                            val methods = listOf(
                                Triple("Other", Icons.Default.Receipt, "One-time spend"),
                                Triple("Pre-existing Bills", Icons.Default.Description, "Bills"),
                                Triple("EMIs", Icons.Default.CalendarToday, "Installments"),
                                Triple("SIPs", Icons.Default.TrendingUp, "Investments"),
                                Triple("UDHAR", Icons.Default.People, "Udhar / Borrow")
                            )
                            
                            val chunks = methods.chunked(3)
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                chunks.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowItems.forEach { (name, icon, desc) ->
                                            val isSelected = recurringTypeSelection == name
                                            val borderCol = if (isSelected) {
                                                if (isDark) Color(0xFF10B981) else Color(0xFF0F1B6B)
                                            } else {
                                                if (isDark) BrandOutline else Color(0xFFCBD5E1)
                                            }
                                            val bgCol = if (isSelected) {
                                                if (isDark) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF0F1B6B).copy(alpha = 0.08f)
                                            } else {
                                                if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                                            }
                                            val textCol = if (isSelected) {
                                                if (isDark) Color(0xFF10B981) else Color(0xFF0F1B6B)
                                            } else {
                                                if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF475569)
                                            }
                                            
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(72.dp)
                                                    .testTag("entry_grid_${name.replace(" ", "_").lowercase(java.util.Locale.getDefault())}")
                                                    .clickable {
                                                        recurringTypeSelection = name
                                                        selectedRecurringItemId = null
                                                        if (name == "Other") {
                                                            txTitle = ""
                                                            txFundsSource = ""
                                                            txCategory = ""
                                                            txSubCategory = ""
                                                            txAmount = ""
                                                        }
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = bgCol),
                                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderCol)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(6.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = name,
                                                        tint = textCol,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = name,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = textCol,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 2,
                                                        lineHeight = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                        
                                        // Align empty slots beautifully in 3x3 layout
                                        if (rowItems.size < 3) {
                                            repeat(3 - rowItems.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (recurringTypeSelection != "Other") {
                            // Option B: scan matching items
                            val mappedType = when (recurringTypeSelection) {
                                "Pre-existing Bills" -> "Bill"
                                "EMIs" -> "EMI"
                                "SIPs" -> "SIP"
                                "UDHAR" -> "UDHAR"
                                else -> ""
                            }
                            val matchingItems = recurringItems.filter { it.type.equals(mappedType, ignoreCase = true) }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "SELECT RECURRING ITEM (${matchingItems.size} found)",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                                    letterSpacing = 0.5.sp
                                )
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    val selectedItem = matchingItems.firstOrNull { it.id == selectedRecurringItemId }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.dp, if (isDark) BrandOutline else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                            .clickable { recurringItemSelectorExpanded = true }
                                            .padding(horizontal = 12.dp, vertical = 11.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedItem?.let { "${it.title} (₹${it.amount})" } ?: "Choose previously created recurring item",
                                            fontSize = 12.sp,
                                            color = if (isDark) Color.White else Color.Black
                                        )
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = recurringItemSelectorExpanded,
                                        onDismissRequest = { recurringItemSelectorExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    ) {
                                        if (matchingItems.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("No recurrent items found of this type.", fontSize = 12.sp) },
                                                onClick = { recurringItemSelectorExpanded = false }
                                            )
                                        } else {
                                            matchingItems.forEach { rItem ->
                                                DropdownMenuItem(
                                                    text = { Text("${rItem.title} - ₹${rItem.amount} (Due: ${rItem.dueDate})", fontSize = 12.sp) },
                                                    onClick = {
                                                        selectedRecurringItemId = rItem.id
                                                        recurringItemSelectorExpanded = false
                                                        // ACTION UPON SELECTION: prefill fields
                                                        txTitle = rItem.title
                                                        txFundsSource = rItem.fundsSource
                                                        txCategory = rItem.category
                                                        txSubCategory = rItem.subCategory
                                                        txAmount = rItem.amount.toString()
                                                        if (rItem.paymentMode.isNotBlank()) {
                                                            txPaymentMethod = rItem.paymentMode
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Input 1: Transaction name/detail
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "TRANSACTION DETAIL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        OutlinedTextField(
                            value = txTitle,
                            onValueChange = { if (recurringTypeSelection == "Other") txTitle = it },
                            modifier = Modifier.fillMaxWidth().testTag("add_transaction_title_field"),
                            placeholder = { Text("e.g. Monthly Rent Payment", color = Color.Gray.copy(alpha = 0.7f)) },
                            singleLine = true,
                            enabled = (recurringTypeSelection == "Other"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Funds Source Input
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "FUNDS SOURCE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        OutlinedTextField(
                            value = txFundsSource,
                            onValueChange = { if (recurringTypeSelection == "Other") txFundsSource = it },
                            modifier = Modifier.fillMaxWidth().testTag("add_transaction_funds_source_field"),
                            placeholder = { Text("e.g. SBI Bank, Wallet, HDFC Credit Card", color = Color.Gray.copy(alpha = 0.7f)) },
                            singleLine = true,
                            enabled = (recurringTypeSelection == "Other"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Input 2: Funds
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "FUNDS (₹)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        OutlinedTextField(
                            value = txAmount,
                            onValueChange = { txAmount = it },
                            modifier = Modifier.fillMaxWidth().testTag("add_transaction_amount_field"),
                            placeholder = { Text("₹ 0.00", color = Color.Gray.copy(alpha = 0.7f)) },
                            leadingIcon = { Text("₹ ", fontWeight = FontWeight.ExtraBold, color = if (isDark) Color.White else Color.Black) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Input 3: Category Selector with "+ Create New" link below
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CATEGORY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            if (recurringTypeSelection == "Other") {
                                Text(
                                    text = "+ Create New",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F1B6B),
                                    modifier = Modifier.clickable { showCategoryCreateDialog = true }
                                )
                            }
                        }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, if (isDark) BrandOutline else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                    .clickable { if (recurringTypeSelection == "Other") categoryExpanded = true }
                                    .alpha(if (recurringTypeSelection == "Other") 1f else 0.6f)
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = txCategory.ifBlank { "Select Category" }, fontSize = 12.sp, color = if (isDark) Color.White else Color.Black)
                                if (recurringTypeSelection == "Other") {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                availableCategoryNames.forEach { catName ->
                                    DropdownMenuItem(
                                        text = { Text(catName, fontSize = 12.sp) },
                                        onClick = {
                                            txCategory = catName
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Input 4: Subcategory Row Selector with "+ Create New" link below
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SUBCATEGORY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            if (recurringTypeSelection == "Other") {
                                Text(
                                    text = "+ Create New",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F1B6B),
                                    modifier = Modifier.clickable { showSubcategoryCreateDialog = true }
                                )
                            }
                        }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, if (isDark) BrandOutline else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                    .clickable { if (recurringTypeSelection == "Other") subCategoryExpanded = true }
                                    .alpha(if (recurringTypeSelection == "Other") 1f else 0.6f)
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = txSubCategory.ifBlank { "Select Subcategory" }, fontSize = 12.sp, color = if (isDark) Color.White else Color.Black)
                                if (recurringTypeSelection == "Other") {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(
                                expanded = subCategoryExpanded,
                                onDismissRequest = { subCategoryExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                availableSubCategories.forEach { subName ->
                                    DropdownMenuItem(
                                        text = { Text(subName, fontSize = 12.sp) },
                                        onClick = {
                                            txSubCategory = subName
                                            subCategoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Input 5: Mode of Payment (CASH, UPI, DEBIT, CREDIT, NETBANK, UDHAR)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "PAYMENT MODE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        
                        // Render 3x2 Grid using standard Columns & Rows for optimal dialog container wrapping
                        val paymentModesLine1 = listOf("Cash" to Icons.Default.Payments, "UPI" to Icons.Default.QrCodeScanner, "Debit Card" to Icons.Default.CreditCard)
                        val paymentModesLine2 = listOf("Credit Card" to Icons.Default.Favorite, "Netbanking" to Icons.Default.AccountBalance, "Udhar" to Icons.Default.Handshake)
                        
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Line 1 Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                paymentModesLine1.forEach { (payName, payIcon) ->
                                    val isSelected = txPaymentMethod == payName
                                    val cellBg = when (payName) {
                                        "Cash" -> Color(0xFFDCFCE7)
                                        "UPI" -> Color(0xFFE0F2FE)
                                        else -> Color(0xFFF3E8FF)
                                    }
                                    val cellBorder = if (isSelected) Color.Black else Color.Transparent
                                    val cellBorderWidth = if (isSelected) 1.5.dp else 0.dp
                                    val cellTextColor = when (payName) {
                                        "Cash" -> Color(0xFF15803D)
                                        "UPI" -> Color(0xFF0369A1)
                                        else -> Color(0xFF6B21A8)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(cellBg)
                                            .border(cellBorderWidth, cellBorder, RoundedCornerShape(10.dp))
                                            .clickable { txPaymentMethod = payName }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(imageVector = payIcon, contentDescription = null, tint = cellTextColor, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(payName.uppercase(), fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = cellTextColor)
                                        }
                                    }
                                }
                            }

                            // Line 2 Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                paymentModesLine2.forEach { (payName, payIcon) ->
                                    val isSelected = txPaymentMethod == payName
                                    val cellBg = when (payName) {
                                        "Credit Card" -> Color(0xFFFEE2E2)
                                        "Netbanking" -> Color(0xFFF1F5F9)
                                        else -> Color(0xFFFCE7F3) // Udhar
                                    }
                                    val cellBorder = if (isSelected) Color.Black else Color.Transparent
                                    val cellBorderWidth = if (isSelected) 1.5.dp else 0.dp
                                    val cellTextColor = when (payName) {
                                        "Credit Card" -> Color(0xFFB91C1C)
                                        "Netbanking" -> Color(0xFF334155)
                                        else -> Color(0xFFBE185D) // Udhar (Pink)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(cellBg)
                                            .border(cellBorderWidth, cellBorder, RoundedCornerShape(10.dp))
                                            .clickable { txPaymentMethod = payName }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(imageVector = payIcon, contentDescription = null, tint = cellTextColor, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(payName.uppercase(), fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = cellTextColor)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Origin details logic (for all financial tracking integrity!)
                    if (txType.equals("Asset", ignoreCase = true) || txType.equals("Investment", ignoreCase = true) || txType.equals("Expense", ignoreCase = true) || txType.equals("Income", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = if (isDark) BrandOutline.copy(alpha = 0.4f) else Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val originTitle = if (txType.equals("Income", ignoreCase = true)) "SOURCE OF INFLOW (MANDATORY)" else "SOURCE OF FUNDS (MANDATORY)"
                        Text(
                            text = originTitle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFF3B82F6) else Color(0xFF0F1B6B),
                            letterSpacing = 0.5.sp
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) BrandSurfaceContainerLow else Color(0xFFF1F5F9))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val originOptions = listOf("Income", "Other")
                            originOptions.forEach { option ->
                                val selected = originOfMoney == option
                                val actBg = if (option == "Income") Color(0xFF10B981) else Color(0xFF2563EB)
                                Button(
                                    onClick = { originOfMoney = option },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) actBg else Color.Transparent,
                                        contentColor = if (selected) Color.White else (if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B))
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Text(text = option, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (originOfMoney == "Income") {
                            val incomeCategories = remember(categories) {
                                categories.filter { it.type.equals("Income", ignoreCase = true) }
                            }
                            
                            LaunchedEffect(incomeCategories) {
                                if (originIncomeCategory.isBlank() && incomeCategories.isNotEmpty()) {
                                    originIncomeCategory = incomeCategories.first().name
                                    originIncomeSubCategory = incomeCategories.first().getSubcategories().firstOrNull() ?: "General"
                                }
                            }
                            
                            var originCatExpanded by remember { mutableStateOf(false) }
                            var originSubCatExpanded by remember { mutableStateOf(false) }
                            
                            val selectedIncomeCatObj = remember(incomeCategories, originIncomeCategory) {
                                incomeCategories.firstOrNull { it.name == originIncomeCategory }
                            }
                            
                            val originSubCategoriesList = remember(selectedIncomeCatObj) {
                                selectedIncomeCatObj?.getSubcategories()?.ifEmpty { listOf("General") } ?: listOf("General")
                            }
                            
                            Text(
                                text = "Select Origin Income Category",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
                            )
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, if (isDark) BrandOutline else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                        .clickable { originCatExpanded = true }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = originIncomeCategory.ifBlank { "Select Income Category" }, fontSize = 12.sp, color = if (isDark) Color.White else Color.Black)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = originCatExpanded,
                                    onDismissRequest = { originCatExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.75f)
                                ) {
                                    incomeCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(text = cat.name, fontSize = 13.sp) },
                                            onClick = {
                                                originIncomeCategory = cat.name
                                                originIncomeSubCategory = cat.getSubcategories().firstOrNull() ?: "General"
                                                originCatExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            Text(
                                text = "Select Origin Income Subcategory",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
                            )
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, if (isDark) BrandOutline else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                        .clickable { originSubCatExpanded = true }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = originIncomeSubCategory.ifBlank { "Select Income Subcategory" }, fontSize = 12.sp, color = if (isDark) Color.White else Color.Black)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = originSubCatExpanded,
                                    onDismissRequest = { originSubCatExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.75f)
                                ) {
                                    originSubCategoriesList.forEach { sub ->
                                        DropdownMenuItem(
                                            text = { Text(text = sub, fontSize = 13.sp) },
                                            onClick = {
                                                originIncomeSubCategory = sub
                                                originSubCatExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = originOtherDescription,
                                onValueChange = {
                                    if (it.length <= 100) {
                                        originOtherDescription = it
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(imageVector = Icons.Default.Description, contentDescription = null) },
                                label = { Text("Origin Description (Max 100 chars)") },
                                supportingText = {
                                    Text(
                                        text = "${originOtherDescription.length}/100",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    )
                                },
                                singleLine = true,
                                isError = originOtherDescription.isBlank()
                            )
                        }
                    }

                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = if (isDark) Color(0xFF0C101B) else Color.White
    )

    // Overlays for On-The-Fly category creation and budgets
    if (showCategoryCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryCreateDialog = false },
            title = { Text("Create New Category", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                OutlinedTextField(
                    value = onTheFlyCategoryName,
                    onValueChange = { onTheFlyCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onTheFlyCategoryName.isNotBlank()) {
                            val resolvedType = if (txType == "Asset") "Asset & Investment" else txType
                            viewModel?.addNewCategory(
                                name = onTheFlyCategoryName.trim(),
                                type = resolvedType,
                                description = "On-the-fly created"
                            )
                            txCategory = onTheFlyCategoryName.trim()
                            showCategoryCreateDialog = false
                            onTheFlyCategoryName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1B6B))
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSubcategoryCreateDialog) {
        AlertDialog(
            onDismissRequest = { showSubcategoryCreateDialog = false },
            title = { Text("Create New Subcategory", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                OutlinedTextField(
                    value = onTheFlySubcategoryName,
                    onValueChange = { onTheFlySubcategoryName = it },
                    label = { Text("Subcategory Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onTheFlySubcategoryName.isNotBlank() && txCategory.isNotBlank()) {
                            val catObj = categories.firstOrNull { it.name == txCategory }
                            if (catObj != null) {
                                viewModel?.addSubcategoryTo(catObj, onTheFlySubcategoryName.trim())
                                txSubCategory = onTheFlySubcategoryName.trim()
                            }
                            showSubcategoryCreateDialog = false
                            onTheFlySubcategoryName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1B6B))
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubcategoryCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBudgetCreateDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetCreateDialog = false },
            title = { Text("Create New Budget", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = onTheFlyBudgetName,
                        onValueChange = { onTheFlyBudgetName = it },
                        label = { Text("Budget Profile Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = onTheFlyBudgetLimit,
                        onValueChange = { onTheFlyBudgetLimit = it },
                        label = { Text("Monthly Limit Amount (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = onTheFlyBudgetLimit.toDoubleOrNull() ?: 0.0
                        if (onTheFlyBudgetName.isNotBlank() && limit > 0.0) {
                            viewModel?.createBudget(onTheFlyBudgetName.trim(), limit) { success, msg ->
                                // Budget flow updates dynamically
                            }
                            showBudgetCreateDialog = false
                            onTheFlyBudgetName = ""
                            onTheFlyBudgetLimit = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1B6B))
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Low-overhead formatting support methods
private fun formatAmountNoSuffix(amount: Double): String {
    return String.format(Locale.getDefault(), "%,.0f", amount)
}

private fun formatAmountShort(amount: Double): String {
    return if (amount >= 100000) {
        String.format(Locale.getDefault(), "%.2fL", amount / 100000.0)
    } else if (amount >= 1000) {
        String.format(Locale.getDefault(), "%.1fk", amount / 1000.0)
    } else {
        String.format(Locale.getDefault(), "%,.0f", amount)
    }
}

private fun parseSimpleDate(dateStr: String): Long? {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.parse(dateStr)?.time
    } catch (e: Exception) {
        null
    }
}

private fun formatCalendarDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun TransactionRowCard(
    txn: DbTransaction,
    isDark: Boolean,
    onModifyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Format timestamp nicely
    val timeFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val dateText = remember(txn.date) { timeFormat.format(Date(txn.date)) }

    val isBudgetType = txn.category.equals("Budget & Goal", ignoreCase = true) || 
                       txn.type.equals("Budget", ignoreCase = true) ||
                       txn.type.equals("Goal", ignoreCase = true) ||
                       txn.type.equals("Budget/Goal Top-Up", ignoreCase = true) ||
                       txn.type.equals("Budget & Goal", ignoreCase = true) ||
                       txn.category.lowercase().contains("budget") ||
                       txn.category.lowercase().contains("goal") ||
                       txn.subCategory.lowercase().contains("budget") ||
                       txn.subCategory.lowercase().contains("goal") ||
                       txn.title.lowercase().contains("budget") ||
                       txn.title.lowercase().contains("goal")

    val isInvestment = txn.type.equals("Asset", ignoreCase = true) || 
                       txn.type.equals("Investment", ignoreCase = true) ||
                       txn.category.lowercase().contains("invest") || 
                       txn.subCategory.lowercase().contains("invest") || 
                       txn.title.lowercase().contains("invest")

    val isCommitment = txn.category.lowercase().contains("bill") || 
                       txn.category.lowercase().contains("sub") || 
                       txn.category.lowercase().contains("emi") || 
                       txn.category.lowercase().contains("udhar") || 
                       txn.category.lowercase().contains("liability") ||
                       txn.title.lowercase().contains("paid") ||
                       txn.title.lowercase().contains("commitment") ||
                       txn.type.equals("Commitment", ignoreCase = true)

    val isIncome = txn.type.equals("Income", ignoreCase = true)

    // Determine classification
    val classification = remember(txn, isBudgetType, isInvestment, isCommitment, isIncome) {
        when {
            isBudgetType -> "Budget & Goal"
            isInvestment -> "Investment"
            isIncome -> "Income"
            isCommitment -> "Commitment"
            else -> "General Expense"
        }
    }

    val mainTextCol = if (isDark) Color.White else Color(0xFF0F1B6B)
    
    // Theme configurations mapping to user's exact colors
    val cardBackground = if (isDark) {
        when (classification) {
            "Budget & Goal" -> Color(0xFF3B1D28) // Lite Pink influence (Dark Rose)
            "Investment"    -> Color(0xFF13223A) // Lite Blue influence (Dark Slate Blue)
            "Income"        -> Color(0xFF0C2B1D) // Green (Dark Emerald)
            "Commitment"    -> Color(0xFF2E1500) // Orange (Dark Rust)
            else            -> Color(0xFF3F0C0C) // Red (Dark Crimson)
        }
    } else {
        when (classification) {
            "Budget & Goal" -> Color(0xFFFFF0F5) // Lite Pink (Lavender Blush)
            "Investment"    -> Color(0xFFEFF6FF) // Lite Blue
            "Income"        -> Color(0xFFECFDF5) // Green (Lite Emerald)
            "Commitment"    -> Color(0xFFFFF7ED) // Orange (Lite Bronze)
            else            -> Color(0xFFFEF2F2) // Red (Lite Coral)
        }
    }

    val borderColor = if (isDark) {
        when (classification) {
            "Budget & Goal" -> Color(0xFFEC4899).copy(alpha = 0.5f)
            "Investment"    -> Color(0xFF3B82F6).copy(alpha = 0.5f)
            "Income"        -> Color(0xFF10B981).copy(alpha = 0.5f)
            "Commitment"    -> Color(0xFFF97316).copy(alpha = 0.5f)
            else            -> Color(0xFFEF4444).copy(alpha = 0.5f)
        }
    } else {
        when (classification) {
            "Budget & Goal" -> Color(0xFFFBCFE8)
            "Investment"    -> Color(0xFFBFDBFE)
            "Income"        -> Color(0xFFA7F3D0)
            "Commitment"    -> Color(0xFFFED7AA)
            else            -> Color(0xFFFCA5A5)
        }
    }

    val tagTextCol = if (isDark) {
        when (classification) {
            "Budget & Goal" -> Color(0xFFF472B6)
            "Investment"    -> Color(0xFF60A5FA)
            "Income"        -> Color(0xFF34D399)
            "Commitment"    -> Color(0xFFFB923C)
            else            -> Color(0xFFF87171)
        }
    } else {
        when (classification) {
            "Budget & Goal" -> Color(0xFFDB2777)
            "Investment"    -> Color(0xFF1D4ED8)
            "Income"        -> Color(0xFF047857)
            "Commitment"    -> Color(0xFFC2410C)
            else            -> Color(0xFFB91C1C)
        }
    }

    val tagBgColor = if (isDark) {
        when (classification) {
            "Budget & Goal" -> Color(0xFFEC4899).copy(alpha = 0.15f)
            "Investment"    -> Color(0xFF3B82F6).copy(alpha = 0.15f)
            "Income"        -> Color(0xFF10B981).copy(alpha = 0.15f)
            "Commitment"    -> Color(0xFFF97316).copy(alpha = 0.15f)
            else            -> Color(0xFFEF4444).copy(alpha = 0.15f)
        }
    } else {
        when (classification) {
            "Budget & Goal" -> Color(0xFFFBCFE8).copy(alpha = 0.35f)
            "Investment"    -> Color(0xFFBFDBFE).copy(alpha = 0.35f)
            "Income"        -> Color(0xFFA7F3D0).copy(alpha = 0.35f)
            "Commitment"    -> Color(0xFFFED7AA).copy(alpha = 0.35f)
            else            -> Color(0xFFFCA5A5).copy(alpha = 0.35f)
        }
    }

    val iconBoxBg = if (isDark) {
        when (classification) {
            "Budget & Goal" -> Color(0xFFEC4899).copy(alpha = 0.15f)
            "Investment"    -> Color(0xFF3B82F6).copy(alpha = 0.15f)
            "Income"        -> Color(0xFF10B981).copy(alpha = 0.15f)
            "Commitment"    -> Color(0xFFF97316).copy(alpha = 0.15f)
            else            -> Color(0xFFEF4444).copy(alpha = 0.15f)
        }
    } else {
        when (classification) {
            "Budget & Goal" -> Color(0xFFEC4899).copy(alpha = 0.15f)
            "Investment"    -> Color(0xFF3B82F6).copy(alpha = 0.15f)
            "Income"        -> Color(0xFF10B981).copy(alpha = 0.15f)
            "Commitment"    -> Color(0xFFF97316).copy(alpha = 0.15f)
            else            -> Color(0xFFEF4444).copy(alpha = 0.15f)
        }
    }

    val iconTint = if (isDark) {
        when (classification) {
            "Budget & Goal" -> Color(0xFFEC4899)
            "Investment"    -> Color(0xFF3B82F6)
            "Income"        -> Color(0xFF10B981)
            "Commitment"    -> Color(0xFFF97316)
            else            -> Color(0xFFEF4444)
        }
    } else {
        when (classification) {
            "Budget & Goal" -> Color(0xFFEC4899)
            "Investment"    -> Color(0xFF3B82F6)
            "Income"        -> Color(0xFF10B981)
            "Commitment"    -> Color(0xFFF97316)
            else            -> Color(0xFFEF4444)
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (com.example.ui.theme.isCompactViewActive) 1.dp else 4.dp)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded }
            .testTag("txn_item_${txn.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground)
    ) {
        Column(modifier = Modifier.padding(if (com.example.ui.theme.isCompactViewActive) 6.dp else 12.dp)) {
            // Main Top Row (Header/Summary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(if (com.example.ui.theme.isCompactViewActive) 28.dp else 36.dp)
                            .clip(CircleShape)
                            .background(iconBoxBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (txn.category.lowercase()) {
                                "food & dining", "food", "groceries" -> Icons.Default.ShoppingCart
                                "salary & wages", "income", "salary" -> Icons.Default.Payments
                                "investments", "investment", "asset" -> Icons.Default.TrendingUp
                                "housing" -> Icons.Default.Home
                                else -> when (classification) {
                                    "Income" -> Icons.Default.ArrowDownward
                                    "Investment" -> Icons.Default.ShowChart
                                    "Commitment" -> Icons.Default.Assignment
                                    "Budget & Goal" -> Icons.Default.Star
                                    else -> Icons.Default.ArrowUpward
                                }
                            },
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(if (com.example.ui.theme.isCompactViewActive) 12.dp else 16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(if (com.example.ui.theme.isCompactViewActive) 6.dp else 10.dp))
                    Column {
                        Text(
                            text = txn.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (com.example.ui.theme.isCompactViewActive) 11.sp else 13.sp,
                            color = mainTextCol
                        )
                        Spacer(modifier = Modifier.height(if (com.example.ui.theme.isCompactViewActive) 1.dp else 2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = dateText,
                                fontSize = if (com.example.ui.theme.isCompactViewActive) 8.sp else 10.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(if (com.example.ui.theme.isCompactViewActive) 4.dp else 6.dp))
                            // Premium rounded micro-badge tag
                            Box(
                                modifier = Modifier
                                    .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                                    .background(tagBgColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = if (com.example.ui.theme.isCompactViewActive) 4.dp else 6.dp, vertical = if (com.example.ui.theme.isCompactViewActive) 0.dp else 1.dp)
                            ) {
                                Text(
                                    text = classification.uppercase(),
                                    fontSize = if (com.example.ui.theme.isCompactViewActive) 7.sp else 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = tagTextCol,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val amountSign = when (classification) {
                        "Income" -> "+"
                        "Investment" -> "📈 "
                        "Budget & Goal" -> "🎀 "
                        "Commitment" -> "📅 "
                        else -> "-"
                    }
                    Text(
                        text = "$amountSign${if (txn.paymentMethod.contains("Credit")) "💳" else ""} ₹${String.format(Locale.getDefault(), "%,.2f", txn.amount)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (com.example.ui.theme.isCompactViewActive) 12.sp else 14.sp,
                        color = iconTint
                    )
                    Spacer(modifier = Modifier.width(if (com.example.ui.theme.isCompactViewActive) 2.dp else 4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand details",
                        tint = if (isDark) Color.White.copy(0.7f) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded detail section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = borderColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Detail Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Category", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = if (txn.subCategory.isNotEmpty()) "${txn.category} > ${txn.subCategory}" else txn.category,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = mainTextCol
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Entry Time", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = dateText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = mainTextCol
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Method", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = txn.paymentMethod,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = mainTextCol
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Transaction ID", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = "KT-TX-${30000 + txn.id}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = mainTextCol
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onModifyClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                contentColor = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF)
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Modify", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Modify", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = onDeleteClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2),
                                contentColor = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalConfirmationModal(
    show: Boolean,
    title: String = "Are you sure?",
    message: String,
    confirmText: String = "Yes",
    cancelText: String = "No",
    confirmButtonColor: Color = Color(0xFFEF4444),
    isDark: Boolean = isSystemInDarkTheme(),
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = if (isDark) Color.White else Color(0xFF0F1B6B)
                )
            },
            text = {
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = confirmButtonColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(confirmText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onCancel
                ) {
                    Text(cancelText, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}


