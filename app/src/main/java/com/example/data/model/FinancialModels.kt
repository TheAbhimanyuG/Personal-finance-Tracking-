package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class DbUser(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String,
    val isPremium: Boolean = true,
    val trustScore: Int = 850,
    val avatarUrl: String = "",
    val contactNo: String = "",
    val gmailId: String = "",
    val streak: Int = 0,
    val medal: String = "Start"
)

@Entity(tableName = "transactions")
data class DbTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val title: String,
    val amount: Double,
    val type: String, // "Income" or "Expense"
    val date: Long, // timestamp
    val category: String,
    val subCategory: String = "",
    val paymentMethod: String = "Debit Card",
    val isSelected: Boolean = false,
    val fundsSource: String = "",
    val status: String = "Paid",
    val isLiability: Boolean = false,
    val sourceType: String = "SOURCE_INCOME"
)

@Entity(tableName = "assets")
data class DbAsset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val name: String,
    val type: String, // "Stock", "Mutual Fund", "Crypto", "FD", "RD", "Other"
    val amountInvested: Double,
    val quantity: Double,
    val dateInvested: Long,
    val currentPrice: Double,
    val totalReturns: Double = 0.0,
    val avgBuyPrice: Double = 0.0
)

data class SubcategoryItem(
    val name: String,
    val description: String = "",
    val iconName: String = "",
    val customImageUri: String = ""
)

@Entity(tableName = "categories")
data class DbCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "Income" or "Expense"
    val subcategoriesString: String = "", // Pipe-delimited list of subcategories for basic backward compatibility
    val description: String = "",         // Detailed description (under 100 chars)
    val iconName: String = "Category",     // Selected icon
    val customImageUri: String = "",      // Custom image URI
    val subcategoriesJson: String = ""    // Serialized List<SubcategoryItem>
) {
    fun getSubcategories(): List<String> {
        val items = getSubcategoryItems()
        return if (items.isNotEmpty()) {
            items.map { it.name }
        } else {
            if (subcategoriesString.isBlank()) {
                emptyList()
            } else {
                subcategoriesString.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            }
        }
    }

    fun getSubcategoryItems(): List<SubcategoryItem> {
        if (subcategoriesJson.isBlank()) {
            if (subcategoriesString.isNotBlank()) {
                return subcategoriesString.split("|").map { it.trim() }.filter { it.isNotEmpty() }.map {
                    SubcategoryItem(name = it)
                }
            }
            return emptyList()
        }
        return deserializeSubcategories(subcategoriesJson)
    }

    companion object {
        fun serializeSubcategories(items: List<SubcategoryItem>): String {
            return items.joinToString("||") { item ->
                val nameEsc = item.name.replace("^", "").replace("|", "").replace("~", "")
                val descEsc = item.description.replace("^", "").replace("|", "").replace("~", "")
                val iconEsc = item.iconName.replace("^", "").replace("|", "").replace("~", "")
                val uriEsc = item.customImageUri.replace("^", "").replace("|", "").replace("~", "")
                "$nameEsc^$descEsc^$iconEsc^$uriEsc"
            }
        }

        fun deserializeSubcategories(serialized: String): List<SubcategoryItem> {
            if (serialized.isBlank()) return emptyList()
            return serialized.split("||").mapNotNull { part ->
                val tokens = part.split("^")
                if (tokens.isNotEmpty()) {
                    val name = tokens.getOrNull(0) ?: ""
                    if (name.isBlank()) return@mapNotNull null
                    val desc = tokens.getOrNull(1) ?: ""
                    val icon = tokens.getOrNull(2) ?: ""
                    val uri = tokens.getOrNull(3) ?: ""
                    SubcategoryItem(name, desc, icon, uri)
                } else {
                    null
                }
            }
        }
    }
}

@Entity(tableName = "budgets")
data class DbBudget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val name: String,
    val limitAmount: Double,
    val spentAmount: Double = 0.0,
    val savedAmount: Double = 0.0,
    val totalAllocated: Double = 0.0,
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(tableName = "goals")
data class DbGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val targetDate: String = "",
    val priority: String = "Medium", // "Low", "Medium", "High"
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(tableName = "budget_goal_activities")
data class DbBudgetGoalActivity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val parentId: Long,
    val isBudget: Boolean,
    val title: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "liabilities")
data class DbLiability(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val title: String,
    val totalAmount: Double,
    val amountPaid: Double = 0.0,
    val category: String,
    val subCategory: String = "",
    val dateCreated: Long = System.currentTimeMillis(),
    val isPaid: Boolean = false,
    val sourceOfFunds: String = ""
) {
    val remainingDue: Double
        get() = (totalAmount - amountPaid).coerceAtLeast(0.0)
}

@Entity(tableName = "recurring_items")
data class DbRecurringItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val title: String,
    val amount: Double,
    val dueDate: String,
    val category: String,
    val subCategory: String = "",
    val paymentMode: String = "",
    val fundsSource: String = "",
    val type: String, // "Bill", "EMI", "SIP", "UDHAR", "Other"
    val recurrence: String = "Monthly", // "Monthly", "Quarterly", "Half Yearly", "Yearly"
    val repetition: String = "until i Cancel", // "until i Cancel" or a number like "12 times"
    val dateCreated: Long = System.currentTimeMillis(),
    val status: String = "Unpaid",
    val isLiability: Boolean = true,
    val sourceType: String = "SOURCE_INCOME",
    val durationEndDate: String = "",
    val isPaid: Boolean = false
)

data class BudgetStats(
    val budgetId: Long,
    val totalAllocated: Double,
    val totalSpent: Double,
    val remainingBudget: Double
)




