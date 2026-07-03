package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.UnifiedBottomNavBar
import com.example.ui.AppViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecentExport(
    val fileName: String,
    val info: String,
    val format: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionExportScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScreen: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentUser by viewModel.currentUser.collectAsState()

    // --- State Management for Forms ---
    var reportTitle by remember { mutableStateOf("") }
    var fileNameInput by remember { mutableStateOf("report_23_q4") }
    
    // Checklist selection database mappings
    var netWorthSelected by remember { mutableStateOf(true) }
    var incomeLedgerSelected by remember { mutableStateOf(true) }
    var expenseLedgerSelected by remember { mutableStateOf(true) }
    var investmentPortfolioSelected by remember { mutableStateOf(false) }
    var assetGrowthSelected by remember { mutableStateOf(true) }
    var auditTrailSelected by remember { mutableStateOf(false) }

    // Time periods State (Daily, Weekly, Monthly, Custom)
    var selectedPeriod by remember { mutableStateOf("Monthly") }

    // Date inputs
    var fromDate by remember { mutableStateOf("10/01/2023") }
    var toDate by remember { mutableStateOf("10/31/2023") }

    // Format options: PDF Document, Excel (XLSX), JSON / CSV
    var exportFormat by remember { mutableStateOf("PDF Document") }

    // Detail levels: Summary Report, Full Audit Details
    var detailLevel by remember { mutableStateOf("Summary Report") }

    // Dynamic stateful database list for Recent Exports
    var recentExports by remember {
        mutableStateOf(
            listOf(
                RecentExport("September_Full_Audit.pdf", "3.2 MB • Oct 05, 2023", "PDF Document"),
                RecentExport("Annual_Projection_2023.xlsx", "1.1 MB • Sep 28, 2023", "Excel (XLSX)")
            )
        )
    }

    // Adaptive Theme styling variables mirroring exact requested specs
    val isDark = isDarkThemeActive
    val scaffoldBg = if (isDark) BrandBackground else Color(0xFFF8FAFC)
    val cardBg = if (isDark) BrandSurface else Color.White
    val borderOutline = if (isDark) BrandOutline else Color(0xFFE2E8F0)
    val mainTextCol = if (isDark) Color.White else Color(0xFF0F1B6B)
    val subTextCol = if (isDark) StitchOnSurfaceVariant else Color(0xFF4B5563)

    Scaffold(
        containerColor = scaffoldBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(scaffoldBg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // --- HEADER ---
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Report Configuration",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = if (isDark) Color.White else Color(0xFF0B1759)
                )
                Text(
                    text = "Customize your financial report for download or archival.",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    color = subTextCol
                )
            }

            // --- REPORT TITLE OUTLINED FIELD ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "REPORT TITLE",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = subTextCol
                )
                OutlinedTextField(
                    value = reportTitle,
                    onValueChange = { reportTitle = it },
                    placeholder = { Text("e.g. Q4 Financial Statement", color = subTextCol.copy(alpha = 0.5f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_report_title_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0F1B6B),
                        unfocusedBorderColor = borderOutline,
                        focusedContainerColor = Color.Transparent
                    )
                )
            }

            // --- FILE NAME SELECTOR with responsive suffix matching selected format ---
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "FILE NAME",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = subTextCol
                )
                OutlinedTextField(
                    value = fileNameInput,
                    onValueChange = { fileNameInput = it },
                    placeholder = { Text("report_2023_q4") },
                    trailingIcon = {
                        Text(
                            text = when (exportFormat) {
                                "PDF Document" -> ".pdf"
                                "Excel (XLSX)" -> ".xlsx"
                                else -> ".csv"
                            },
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) BrandPrimary else Color(0xFF1E293B),
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_file_name_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0F1B6B),
                        unfocusedBorderColor = borderOutline,
                        focusedContainerColor = Color.Transparent
                    )
                )
            }

            // --- DATA SOURCE SELECTION CHECKLISTS ----
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "DATA SOURCE SELECTION",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = subTextCol
                )

                DataSourceCheckRow(
                    label = "Net Worth Summary",
                    checked = netWorthSelected,
                    onToggle = { netWorthSelected = it },
                    isDark = isDark
                )
                DataSourceCheckRow(
                    label = "Income Ledger",
                    checked = incomeLedgerSelected,
                    onToggle = { incomeLedgerSelected = it },
                    isDark = isDark
                )
                DataSourceCheckRow(
                    label = "Expense Ledger",
                    checked = expenseLedgerSelected,
                    onToggle = { expenseLedgerSelected = it },
                    isDark = isDark
                )
                DataSourceCheckRow(
                    label = "Investment Portfolio",
                    checked = investmentPortfolioSelected,
                    onToggle = { investmentPortfolioSelected = it },
                    isDark = isDark
                )
                DataSourceCheckRow(
                    label = "Asset Growth Charts",
                    checked = assetGrowthSelected,
                    onToggle = { assetGrowthSelected = it },
                    isDark = isDark
                )
                DataSourceCheckRow(
                    label = "Audit Trail",
                    checked = auditTrailSelected,
                    onToggle = { auditTrailSelected = it },
                    isDark = isDark
                )
            }

            // --- TIME PERIOD GRID 2x2 ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TIME PERIOD",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = subTextCol
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TimePeriodGridCapsule(
                            label = "Daily",
                            selected = selectedPeriod == "Daily",
                            onClick = { selectedPeriod = "Daily" },
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        TimePeriodGridCapsule(
                            label = "Weekly",
                            selected = selectedPeriod == "Weekly",
                            onClick = { selectedPeriod = "Weekly" },
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TimePeriodGridCapsule(
                            label = "Monthly",
                            selected = selectedPeriod == "Monthly",
                            onClick = { selectedPeriod = "Monthly" },
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        TimePeriodGridCapsule(
                            label = "Custom",
                            selected = selectedPeriod == "Custom",
                            onClick = { selectedPeriod = "Custom" },
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // --- FROM & TO DATE SELECTIONS SYSTEM ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "FROM",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = subTextCol
                    )
                    OutlinedTextField(
                        value = fromDate,
                        onValueChange = { fromDate = it },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Calendar From Icon",
                                tint = subTextCol,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0F1B6B),
                            unfocusedBorderColor = borderOutline,
                            focusedContainerColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "TO",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = subTextCol
                    )
                    OutlinedTextField(
                        value = toDate,
                        onValueChange = { toDate = it },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Calendar To Icon",
                                tint = subTextCol,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0F1B6B),
                            unfocusedBorderColor = borderOutline,
                            focusedContainerColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- EXPORT FORMAT SELECTORS LIST ---
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "EXPORT FORMAT",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = subTextCol
                )

                listOf("PDF Document", "Excel (XLSX)", "JSON / CSV").forEach { format ->
                    val isSelected = exportFormat == format
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { exportFormat = format }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { exportFormat = format },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF0B1759),
                                unselectedColor = if (isDark) Color(0xFF4B5563) else Color(0xFF9CA3AF)
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = format,
                            fontFamily = InterFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp,
                            color = if (isDark) Color.White else Color(0xFF1E293B)
                        )
                    }
                }
            }

            // --- DETAIL LEVEL ---
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "DETAIL LEVEL",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = subTextCol
                )

                listOf("Summary Report", "Full Audit Details").forEach { detail ->
                    val isSelected = detailLevel == detail
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { detailLevel = detail }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { detailLevel = detail },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF0B1759),
                                unselectedColor = if (isDark) Color(0xFF4B5563) else Color(0xFF9CA3AF)
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = detail,
                            fontFamily = InterFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp,
                            color = if (isDark) Color.White else Color(0xFF1E293B)
                        )
                    }
                }
            }

            // --- RECENT EXPORTS LIST SECTION (DYNAMIC & INTERACTIVE) ---
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Recent Exports",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = if (isDark) Color.White else Color(0xFF0B1759),
                    modifier = Modifier.padding(top = 8.dp)
                )

                recentExports.forEach { export ->
                    val isPdf = export.format == "PDF Document"
                    val iconBack = if (isPdf) {
                        if (isDark) Color(0xFF312E81) else Color(0xFFF3E8FF)
                    } else {
                        if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                    }
                    val iconTint = if (isPdf) {
                        Color(0xFF7C3AED)
                    } else {
                        Color(0xFF16A34A)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, borderOutline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(iconBack),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = export.fileName,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isDark) Color.White else Color(0xFF1F2937),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = export.info,
                                        fontFamily = InterFontFamily,
                                        fontSize = 11.sp,
                                        color = subTextCol
                                    )
                                }
                            }

                            // Dynamic click to trigger actual file action simulation
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Initiated download of ${export.fileName} to local system filesystem.", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Download Report Entry",
                                    tint = if (isDark) Color.White else Color(0xFF4B5563)
                                )
                            }
                        }
                    }
                }
            }

            // --- BOTTOM PRIMARY ACTION BUTTON GENERATE & DOWNLOAD --
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val titleVal = if (reportTitle.isEmpty()) "Unnamed Financial Report" else reportTitle
                        val nameVal = if (fileNameInput.isEmpty()) "report_output" else fileNameInput

                        // Compiles real database CSV strings
                        val csvDataString = viewModel.compileCsvFormatString()
                        clipboardManager.setText(AnnotatedString(csvDataString))

                        // Compute active extension dynamically
                        val extension = when (exportFormat) {
                            "PDF Document" -> ".pdf"
                            "Excel (XLSX)" -> ".xlsx"
                            else -> ".csv"
                        }
                        
                        val activeFileTitle = if (nameVal.endsWith(extension)) nameVal else "$nameVal$extension"
                        
                        // Metadata stats formulation
                        val sizeData = when (exportFormat) {
                            "PDF Document" -> "3.5 MB"
                            "Excel (XLSX)" -> "1.8 MB"
                            else -> "0.9 MB"
                        }
                        val todayDateString = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())

                        // Instatiate new export item
                        val generatedExportItem = RecentExport(
                            fileName = activeFileTitle,
                            info = "$sizeData • $todayDateString",
                            format = exportFormat
                        )

                        // Insert reactively inside list
                        recentExports = listOf(generatedExportItem) + recentExports

                        Toast.makeText(
                            context,
                            "Report '$activeFileTitle' successfully prepared and appended to Recent list! Ledger CSV copied to Clipboard.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("generate_download_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) BrandPrimary else Color(0xFF030712),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Generate PDF Icon",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Generate & Download Report",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                Text(
                    text = "Generating large audit reports may take up to 30 seconds.",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = subTextCol
                )
            }

            Spacer(modifier = Modifier.height(56.dp)) // Avoid any overlaps
        }
    }
}

@Composable
fun DataSourceCheckRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    isDark: Boolean
) {
    val bgCol = if (checked) {
        if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFE9F5FE)
    } else {
        if (isDark) Color(0xFF111827) else Color.White
    }
    
    val borderCol = if (checked) {
        if (isDark) Color(0xFF3B82F6) else Color(0xFFBBE5FD)
    } else {
        if (isDark) Color(0xFF1F2937) else Color(0xFFE2E8F0)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgCol)
            .border(BorderStroke(1.dp, borderCol), RoundedCornerShape(12.dp))
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = if (isDark) Color.White else Color(0xFF0F1B6B)
        )
        
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) Color(0xFF0F1B6B) else Color.Transparent)
                .border(
                    BorderStroke(
                        width = 1.5.dp,
                        color = if (checked) Color(0xFF0F1B6B) else (if (isDark) Color(0xFF4B5563) else Color(0xFFcbd5e1))
                    ),
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun TimePeriodGridCapsule(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bgCol = if (selected) {
        Color(0xFF0F1B6B)
    } else {
        if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.25f) else Color(0xFFEBF5FE)
    }
    
    val textCol = if (selected) {
        Color.White
    } else {
        if (isDark) BrandPrimary else Color(0xFF0F1B6B)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgCol)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = textCol
        )
    }
}
