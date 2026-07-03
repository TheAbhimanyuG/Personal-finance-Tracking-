package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DbAsset
import com.example.data.model.DbCategory
import com.example.data.model.DbTransaction
import com.example.data.model.DbUser
import com.example.data.model.DbBudget
import com.example.data.model.DbGoal
import com.example.data.model.DbBudgetGoalActivity
import com.example.data.model.BudgetStats
import com.example.data.model.DbLiability
import com.example.data.model.DbRecurringItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialDao {

    // --- Users ---
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<DbUser>>

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): DbUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: DbUser)

    @Delete
    suspend fun deleteUser(user: DbUser)

    // --- Transactions ---
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getTransactionsForUser(userId: String): Flow<List<DbTransaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    suspend fun getTransactionsForUserRaw(userId: String): List<DbTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: DbTransaction)

    @Update
    suspend fun updateTransaction(transaction: DbTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: DbTransaction)

    @Query("UPDATE transactions SET isSelected = :isSelected WHERE id = :id")
    suspend fun updateTransactionSelection(id: Long, isSelected: Boolean)

    @Query("UPDATE transactions SET isSelected = :isSelected WHERE userId = :userId")
    suspend fun updateAllTransactionsSelection(userId: String, isSelected: Boolean)

    @Query("DELETE FROM transactions WHERE userId = :userId AND isSelected = 1")
    suspend fun deleteSelectedTransactions(userId: String)

    // --- Assets ---
    @Query("SELECT * FROM assets WHERE userId = :userId ORDER BY dateInvested DESC")
    fun getAssetsForUser(userId: String): Flow<List<DbAsset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: DbAsset)

    @Delete
    suspend fun deleteAsset(asset: DbAsset)

    // --- Categories ---
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<DbCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: DbCategory)

    @Update
    suspend fun updateCategory(category: DbCategory)

    @Delete
    suspend fun deleteCategory(category: DbCategory)

    // --- Bulk Extraction & Restoration Queries for Backups ---
    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsRaw(): List<DbTransaction>

    @Query("SELECT * FROM assets")
    suspend fun getAllAssetsRaw(): List<DbAsset>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersRaw(): List<DbUser>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesRaw(): List<DbCategory>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsRaw(): List<DbBudget>

    @Query("SELECT * FROM goals")
    suspend fun getAllGoalsRaw(): List<DbGoal>

    @Query("SELECT * FROM liabilities")
    suspend fun getAllLiabilitiesRaw(): List<DbLiability>

    @Query("SELECT * FROM recurring_items")
    suspend fun getAllRecurringItemsRaw(): List<DbRecurringItem>

    @Query("SELECT * FROM budget_goal_activities")
    suspend fun getAllActivitiesRaw(): List<DbBudgetGoalActivity>

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactionsRaw()

    @Query("DELETE FROM assets")
    suspend fun deleteAllAssetsRaw()

    @Query("DELETE FROM users")
    suspend fun deleteAllUsersRaw()

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategoriesRaw()

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgetsRaw()

    @Query("DELETE FROM goals")
    suspend fun deleteAllGoalsRaw()

    @Query("DELETE FROM liabilities")
    suspend fun deleteAllLiabilitiesRaw()

    @Query("DELETE FROM recurring_items")
    suspend fun deleteAllRecurringItemsRaw()

    @Query("DELETE FROM budget_goal_activities")
    suspend fun deleteAllActivitiesRaw()

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteTransactionsForUserRaw(userId: String)

    @Query("DELETE FROM transactions WHERE userId = :userId AND type = 'Income'")
    suspend fun deleteIncomeTransactionsForUserRaw(userId: String)

    @Query("DELETE FROM transactions WHERE userId = :userId AND type = 'Expense'")
    suspend fun deleteExpenseTransactionsForUserRaw(userId: String)

    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun deleteBudgetsForUserRaw(userId: String)

    @Query("DELETE FROM goals WHERE userId = :userId")
    suspend fun deleteGoalsForUserRaw(userId: String)

    @Query("DELETE FROM budget_goal_activities WHERE userId = :userId")
    suspend fun deleteBudgetGoalActivitiesForUserRaw(userId: String)

    @Query("DELETE FROM assets WHERE userId = :userId")
    suspend fun deleteAssetsForUserRaw(userId: String)

    // --- Budgets ---
    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY dateCreated DESC")
    fun getBudgetsForUser(userId: String): Flow<List<DbBudget>>

    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY dateCreated DESC")
    suspend fun getBudgetsForUserRaw(userId: String): List<DbBudget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: DbBudget): Long

    @Update
    suspend fun updateBudget(budget: DbBudget)

    @Delete
    suspend fun deleteBudget(budget: DbBudget)

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getBudgetById(id: Long): DbBudget?

    // --- Goals ---
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY dateCreated DESC")
    fun getGoalsForUser(userId: String): Flow<List<DbGoal>>

    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY dateCreated DESC")
    suspend fun getGoalsForUserRaw(userId: String): List<DbGoal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: DbGoal): Long

    @Update
    suspend fun updateGoal(goal: DbGoal)

    @Delete
    suspend fun deleteGoal(goal: DbGoal)

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: Long): DbGoal?

    // --- Budget / Goal Activities ---
    @Query("""
        SELECT 
            b.id AS budgetId,
            (b.limitAmount + COALESCE(SUM(CASE WHEN a.amount > 0 THEN a.amount ELSE 0.0 END), 0.0)) AS totalAllocated,
            COALESCE(SUM(CASE WHEN a.amount < 0 THEN ABS(a.amount) ELSE 0.0 END), 0.0) AS totalSpent,
            ((b.limitAmount + COALESCE(SUM(CASE WHEN a.amount > 0 THEN a.amount ELSE 0.0 END), 0.0)) - COALESCE(SUM(CASE WHEN a.amount < 0 THEN ABS(a.amount) ELSE 0.0 END), 0.0)) AS remainingBudget
        FROM budgets b
        LEFT JOIN budget_goal_activities a ON b.id = a.parentId AND a.isBudget = 1
        WHERE b.id = :budgetId
        GROUP BY b.id
    """)
    fun getBudgetStats(budgetId: Long): Flow<BudgetStats?>

    @Query("SELECT * FROM budget_goal_activities WHERE userId = :userId AND parentId = :parentId AND isBudget = :isBudget ORDER BY date DESC")
    fun getActivitiesForBudgetOrGoal(userId: String, parentId: Long, isBudget: Boolean): Flow<List<DbBudgetGoalActivity>>

    @Query("SELECT * FROM budget_goal_activities WHERE userId = :userId AND parentId = :parentId AND isBudget = :isBudget ORDER BY date DESC")
    suspend fun getActivitiesForBudgetOrGoalRaw(userId: String, parentId: Long, isBudget: Boolean): List<DbBudgetGoalActivity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: DbBudgetGoalActivity)

    @Update
    suspend fun updateActivity(activity: DbBudgetGoalActivity)

    @Delete
    suspend fun deleteActivity(activity: DbBudgetGoalActivity)

    @Query("DELETE FROM budget_goal_activities WHERE parentId = :parentId AND isBudget = :isBudget")
    suspend fun deleteActivitiesForParent(parentId: Long, isBudget: Boolean)

    // --- Liabilities (Udhar) ---
    @Query("SELECT * FROM liabilities WHERE userId = :userId ORDER BY dateCreated DESC")
    fun getLiabilitiesForUser(userId: String): Flow<List<DbLiability>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiability(liability: DbLiability): Long

    @Update
    suspend fun updateLiability(liability: DbLiability)

    @Delete
    suspend fun deleteLiability(liability: DbLiability)

    @Query("DELETE FROM liabilities WHERE userId = :userId")
    suspend fun deleteLiabilitiesForUserRaw(userId: String)

    // --- Recurring Items (Bills, EMIs, SIPs, UDHAR) ---
    @Query("SELECT * FROM recurring_items WHERE userId = :userId ORDER BY dateCreated DESC")
    fun getRecurringItemsForUser(userId: String): Flow<List<DbRecurringItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringItem(item: DbRecurringItem): Long

    @Delete
    suspend fun deleteRecurringItem(item: DbRecurringItem)

    @Query("DELETE FROM recurring_items WHERE userId = :userId")
    suspend fun deleteRecurringItemsForUserRaw(userId: String)
}
