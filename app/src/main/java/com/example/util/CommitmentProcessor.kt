package com.example.util

import android.util.Log
import com.example.data.model.DbRecurringItem
import com.example.data.model.DbTransaction
import com.example.data.repository.FinancialRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object CommitmentProcessor {
    private const val TAG = "CommitmentProcessor"

    /**
     * Parses the custom due date string across multiple acceptable formats.
     */
    fun parseDueDate(dueDateStr: String): Calendar? {
        val formats = listOf("dd-MM-yyyy", "d-M-yyyy", "yyyy-MM-dd")
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.isLenient = false
                val date = sdf.parse(dueDateStr.trim()) ?: continue
                val cal = Calendar.getInstance()
                cal.time = date
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return cal
            } catch (e: Exception) {
                // Ignore and try next format
            }
        }
        return null
    }

    /**
     * Helper to verify if we are today on or after the specified due date.
     */
    fun isDue(dueDateStr: String): Boolean {
        val dueCal = parseDueDate(dueDateStr) ?: return false
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return !today.before(dueCal)
    }

    /**
     * Calculates the days left from today to the due date.
     */
    fun calculateDaysLeft(dueDateStr: String): Int {
        val dueCal = parseDueDate(dueDateStr) ?: return 999
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffInMillis = dueCal.timeInMillis - today.timeInMillis
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    /**
     * Checks if today is within 7 days of the due date (currentDate >= dueDate - 7 days) and not paid.
     */
    fun shouldDisplayRed(dueDateStr: String, isPaid: Boolean): Boolean {
        if (isPaid) return false
        val dueCal = parseDueDate(dueDateStr) ?: return false
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val testCal = Calendar.getInstance()
        testCal.timeInMillis = dueCal.timeInMillis
        testCal.add(Calendar.DAY_OF_YEAR, -7)
        return !today.before(testCal)
    }

    /**
     * Checks if currentDate > durationEndDate.
     */
    fun isPastDurationEnd(durationEndDateStr: String): Boolean {
        if (durationEndDateStr.isBlank()) return false
        val endCal = parseDueDate(durationEndDateStr) ?: return false
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return today.after(endCal)
    }

    /**
     * Advances the commitment's due date based on frequency and decrements repetition count if limited.
     * Returns the updated DbRecurringItem, or null if all repetitions are exhausted.
     */
    fun advanceCommitment(item: DbRecurringItem): DbRecurringItem? {
        val dueCal = parseDueDate(item.dueDate) ?: Calendar.getInstance()
        val addedMonths = when (item.recurrence) {
            "Quarterly" -> 3
            "Half Yearly", "Half-Yearly" -> 6
            "Yearly" -> 12
            else -> 1 // Monthly
        }
        dueCal.add(Calendar.MONTH, addedMonths)
        val nextDueDateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(dueCal.time)

        val repLower = item.repetition.lowercase()
        if (repLower.contains("until i cancel") || repLower.contains("indefinite") || repLower.isBlank()) {
            return item.copy(dueDate = nextDueDateStr, status = "Unpaid", isPaid = false, isLiability = true)
        } else {
            val digits = repLower.filter { it.isDigit() }
            val count = digits.toIntOrNull() ?: 12
            if (count <= 1) {
                return null // Completed!
            } else {
                return item.copy(
                    dueDate = nextDueDateStr,
                    repetition = "${count - 1} times",
                    status = "Unpaid",
                    isPaid = false,
                    isLiability = true
                )
            }
        }
    }

    /**
     * Processes pending recurring items. If any item is due (current date is on or after
     * its due date), it is automatically transitioned to an active transaction.
     */
    suspend fun processDueCommitments(
        repository: FinancialRepository,
        userId: String,
        onProcessed: (String) -> Unit = {}
    ): Int {
        var processedCount = 0
        try {
            // Retrieve all recurring items for user
            val items = repository.getRecurringItemsForUser(userId).first()
            if (items.isEmpty()) return 0

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            for (item in items) {
                // Update liability flags first
                val pastEnd = isPastDurationEnd(item.durationEndDate)
                val newIsLiability = if (item.durationEndDate.isNotBlank()) !pastEnd else item.isLiability
                
                var currentItem = item
                if (newIsLiability != item.isLiability) {
                    currentItem = item.copy(isLiability = newIsLiability)
                    repository.insertRecurringItem(currentItem)
                }

                if (currentItem.isPaid) {
                    continue
                }

                val dueCal = parseDueDate(currentItem.dueDate)
                if (dueCal != null) {
                    // Switch to trigger active transaction on/after due date
                    if (!today.before(dueCal)) {
                        val isAssetType = currentItem.type.equals("SIP", ignoreCase = true) || 
                                          currentItem.category.contains("Asset & Investment", ignoreCase = true) || 
                                          currentItem.category.contains("Investments", ignoreCase = true) ||
                                          currentItem.category.contains("Fixed Deposits & Savings", ignoreCase = true) ||
                                          currentItem.category.contains("Stocks & Mutual Funds", ignoreCase = true) ||
                                          currentItem.category.contains("Real Estate & Gold", ignoreCase = true)

                        val transactionType = if (isAssetType) "Investment" else "Expense"

                        // Create active DbTransaction with status="Paid" and isLiability=true
                        val transaction = DbTransaction(
                            userId = currentItem.userId,
                            title = "[Commitment Paid] ${currentItem.title}",
                            amount = currentItem.amount,
                            type = transactionType,
                            date = today.timeInMillis, // Timestamp of transaction execution
                            category = currentItem.category,
                            subCategory = currentItem.subCategory,
                            paymentMethod = if (currentItem.paymentMode.isNotBlank()) currentItem.paymentMode else "UPI",
                            fundsSource = if (currentItem.fundsSource.isNotBlank()) currentItem.fundsSource else "General Funds",
                            isSelected = false,
                            status = "Paid",
                            isLiability = true,
                            sourceType = currentItem.sourceType
                        )
                        repository.insertTransaction(transaction)

                        val advanced = advanceCommitment(currentItem)
                        if (advanced != null) {
                            repository.insertRecurringItem(advanced)
                            Log.d(TAG, "Processed and rolled over commitment '${currentItem.title}' to ${advanced.dueDate}")
                        } else {
                            repository.deleteRecurringItem(currentItem)
                            Log.d(TAG, "Processed and completed commitment '${currentItem.title}'")
                        }

                        processedCount++
                        onProcessed(currentItem.title)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception running processDueCommitments", e)
        }
        return processedCount
    }
}
