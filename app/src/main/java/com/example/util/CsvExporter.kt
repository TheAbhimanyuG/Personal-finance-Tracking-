package com.example.util

import com.example.data.model.DbTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {
    fun formatTransactionsToCsv(transactions: List<DbTransaction>): String {
        val header = "ID,Title,Amount,Type,Date,Category,Subcategory,PaymentMethod\n"
        val builder = StringBuilder(header)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        
        for (txn in transactions) {
            val dateStr = dateFormat.format(Date(txn.date))
            // Escape double quotes inside the title
            val escapedTitle = txn.title.replace("\"", "\"\"")
            val formattedTitle = if (escapedTitle.contains(",") || escapedTitle.contains("\n")) {
                "\"$escapedTitle\""
            } else {
                escapedTitle
            }
            
            builder.append("${txn.id},")
                .append("$formattedTitle,")
                .append("${txn.amount},")
                .append("${txn.type},")
                .append("$dateStr,")
                .append("${txn.category},")
                .append("${txn.subCategory},")
                .append("${txn.paymentMethod}\n")
        }
        return builder.toString()
    }

    fun formatActivitiesToCsv(activities: List<com.example.data.model.DbBudgetGoalActivity>): String {
        val header = "ID,ParentID,IsBudget,Title,Amount,Date\n"
        val builder = StringBuilder(header)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        
        for (act in activities) {
            val dateStr = dateFormat.format(Date(act.date))
            val escapedTitle = act.title.replace("\"", "\"\"")
            val formattedTitle = if (escapedTitle.contains(",") || escapedTitle.contains("\n")) {
                "\"$escapedTitle\""
            } else {
                escapedTitle
            }
            builder.append("${act.id},")
                .append("${act.parentId},")
                .append("${act.isBudget},")
                .append("$formattedTitle,")
                .append("${act.amount},")
                .append("$dateStr\n")
        }
        return builder.toString()
    }

    fun exportToCsvFile(context: android.content.Context, filename: String, csvContent: String) {
        try {
            val file = java.io.File(context.getExternalFilesDir(null), filename)
            file.writeText(csvContent)
            
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Ledger CSV", csvContent))
            
            android.widget.Toast.makeText(
                context,
                "CSV exported successfully to ${file.name} and copied to clipboard!",
                android.widget.Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context,
                "Export failed: ${e.localizedMessage}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}
