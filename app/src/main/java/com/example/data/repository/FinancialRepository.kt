package com.example.data.repository

import com.example.data.db.FinancialDao
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

class FinancialRepository(private val dao: FinancialDao) {

    // --- Users ---
    fun getAllUsers(): Flow<List<DbUser>> = dao.getAllUsers()

    suspend fun getUserById(userId: String): DbUser? = dao.getUserById(userId)

    suspend fun insertUser(user: DbUser) = dao.insertUser(user)

    suspend fun deleteUser(user: DbUser) = dao.deleteUser(user)

    // --- Transactions ---
    fun getTransactionsForUser(userId: String): Flow<List<DbTransaction>> {
        return dao.getTransactionsForUser(userId)
    }

    suspend fun getTransactionsForUserRaw(userId: String): List<DbTransaction> {
        return dao.getTransactionsForUserRaw(userId)
    }

    suspend fun insertTransaction(transaction: DbTransaction) {
        dao.insertTransaction(transaction)
        com.example.util.GlobalTransactionEventBus.post(com.example.util.GlobalTransactionEvent.TransactionAdded(transaction))
    }

    suspend fun updateTransaction(transaction: DbTransaction) {
        dao.updateTransaction(transaction)
        com.example.util.GlobalTransactionEventBus.post(com.example.util.GlobalTransactionEvent.TransactionUpdated(transaction))
    }

    suspend fun deleteTransaction(transaction: DbTransaction) {
        dao.deleteTransaction(transaction)
        com.example.util.GlobalTransactionEventBus.post(com.example.util.GlobalTransactionEvent.TransactionDeleted(transaction))
    }

    suspend fun updateTransactionSelection(id: Long, isSelected: Boolean) {
        dao.updateTransactionSelection(id, isSelected)
    }

    suspend fun updateAllTransactionsSelection(userId: String, isSelected: Boolean) {
        dao.updateAllTransactionsSelection(userId, isSelected)
    }

    suspend fun deleteSelectedTransactions(userId: String) = dao.deleteSelectedTransactions(userId)

    // --- Assets ---
    fun getAssetsForUser(userId: String): Flow<List<DbAsset>> = dao.getAssetsForUser(userId)

    suspend fun insertAsset(asset: DbAsset) = dao.insertAsset(asset)

    suspend fun deleteAsset(asset: DbAsset) = dao.deleteAsset(asset)

    // --- Categories ---
    fun getAllCategories(): Flow<List<DbCategory>> = dao.getAllCategories()

    suspend fun insertCategory(category: DbCategory) = dao.insertCategory(category)

    suspend fun updateCategory(category: DbCategory) = dao.updateCategory(category)

    suspend fun deleteCategory(category: DbCategory) = dao.deleteCategory(category)

    // --- Bulk Extraction & Purge pass-throughs ---
    suspend fun getAllTransactionsRaw(): List<DbTransaction> = dao.getAllTransactionsRaw()

    suspend fun getAllAssetsRaw(): List<DbAsset> = dao.getAllAssetsRaw()

    suspend fun getAllUsersRaw(): List<DbUser> = dao.getAllUsersRaw()

    suspend fun getAllCategoriesRaw(): List<DbCategory> = dao.getAllCategoriesRaw()

    suspend fun getAllBudgetsRaw(): List<DbBudget> = dao.getAllBudgetsRaw()

    suspend fun getAllGoalsRaw(): List<DbGoal> = dao.getAllGoalsRaw()

    suspend fun getAllLiabilitiesRaw(): List<DbLiability> = dao.getAllLiabilitiesRaw()

    suspend fun getAllRecurringItemsRaw(): List<DbRecurringItem> = dao.getAllRecurringItemsRaw()

    suspend fun getAllActivitiesRaw(): List<DbBudgetGoalActivity> = dao.getAllActivitiesRaw()

    suspend fun deleteAllTransactionsRaw() = dao.deleteAllTransactionsRaw()

    suspend fun deleteAllAssetsRaw() = dao.deleteAllAssetsRaw()

    suspend fun deleteAllUsersRaw() = dao.deleteAllUsersRaw()

    suspend fun deleteAllCategoriesRaw() = dao.deleteAllCategoriesRaw()

    suspend fun deleteAllBudgetsRaw() = dao.deleteAllBudgetsRaw()

    suspend fun deleteAllGoalsRaw() = dao.deleteAllGoalsRaw()

    suspend fun deleteAllLiabilitiesRaw() = dao.deleteAllLiabilitiesRaw()

    suspend fun deleteAllRecurringItemsRaw() = dao.deleteAllRecurringItemsRaw()

    suspend fun deleteAllActivitiesRaw() = dao.deleteAllActivitiesRaw()

    suspend fun deleteTransactionsForUserRaw(userId: String) = dao.deleteTransactionsForUserRaw(userId)

    suspend fun deleteIncomeTransactionsForUserRaw(userId: String) = dao.deleteIncomeTransactionsForUserRaw(userId)

    suspend fun deleteExpenseTransactionsForUserRaw(userId: String) = dao.deleteExpenseTransactionsForUserRaw(userId)

    suspend fun deleteBudgetsForUserRaw(userId: String) = dao.deleteBudgetsForUserRaw(userId)

    suspend fun deleteGoalsForUserRaw(userId: String) = dao.deleteGoalsForUserRaw(userId)

    suspend fun deleteBudgetGoalActivitiesForUserRaw(userId: String) = dao.deleteBudgetGoalActivitiesForUserRaw(userId)

    suspend fun deleteAssetsForUserRaw(userId: String) = dao.deleteAssetsForUserRaw(userId)

    // --- Budgets ---
    fun getBudgetsForUser(userId: String): Flow<List<DbBudget>> = dao.getBudgetsForUser(userId)

    suspend fun getBudgetsForUserRaw(userId: String): List<DbBudget> = dao.getBudgetsForUserRaw(userId)

    suspend fun insertBudget(budget: DbBudget): Long = dao.insertBudget(budget)

    suspend fun updateBudget(budget: DbBudget) = dao.updateBudget(budget)

    suspend fun deleteBudget(budget: DbBudget) = dao.deleteBudget(budget)

    suspend fun getBudgetById(id: Long): DbBudget? = dao.getBudgetById(id)

    // --- Goals ---
    fun getGoalsForUser(userId: String): Flow<List<DbGoal>> = dao.getGoalsForUser(userId)

    suspend fun getGoalsForUserRaw(userId: String): List<DbGoal> = dao.getGoalsForUserRaw(userId)

    suspend fun insertGoal(goal: DbGoal): Long = dao.insertGoal(goal)

    suspend fun updateGoal(goal: DbGoal) = dao.updateGoal(goal)

    suspend fun deleteGoal(goal: DbGoal) = dao.deleteGoal(goal)

    suspend fun getGoalById(id: Long): DbGoal? = dao.getGoalById(id)

    // --- Budget / Goal Activities ---
    fun getBudgetStats(budgetId: Long): Flow<BudgetStats?> {
        return dao.getBudgetStats(budgetId)
    }

    fun getActivitiesForBudgetOrGoal(userId: String, parentId: Long, isBudget: Boolean): Flow<List<DbBudgetGoalActivity>> {
        return dao.getActivitiesForBudgetOrGoal(userId, parentId, isBudget)
    }

    suspend fun getActivitiesForBudgetOrGoalRaw(userId: String, parentId: Long, isBudget: Boolean): List<DbBudgetGoalActivity> {
        return dao.getActivitiesForBudgetOrGoalRaw(userId, parentId, isBudget)
    }

    suspend fun insertActivity(activity: DbBudgetGoalActivity) = dao.insertActivity(activity)
 
    suspend fun updateActivity(activity: DbBudgetGoalActivity) = dao.updateActivity(activity)

    suspend fun deleteActivity(activity: DbBudgetGoalActivity) = dao.deleteActivity(activity)

    suspend fun deleteActivitiesForParent(parentId: Long, isBudget: Boolean) = dao.deleteActivitiesForParent(parentId, isBudget)
 
    // --- Liabilities (Udhar) ---
    fun getLiabilitiesForUser(userId: String): Flow<List<DbLiability>> = dao.getLiabilitiesForUser(userId)
 
    suspend fun insertLiability(liability: DbLiability): Long = dao.insertLiability(liability)
 
    suspend fun updateLiability(liability: DbLiability) = dao.updateLiability(liability)
 
    suspend fun deleteLiability(liability: DbLiability) = dao.deleteLiability(liability)
 
    suspend fun deleteLiabilitiesForUserRaw(userId: String) = dao.deleteLiabilitiesForUserRaw(userId)

    // --- Recurring Items ---
    fun getRecurringItemsForUser(userId: String): Flow<List<DbRecurringItem>> = dao.getRecurringItemsForUser(userId)

    suspend fun insertRecurringItem(item: DbRecurringItem): Long = dao.insertRecurringItem(item)

    suspend fun deleteRecurringItem(item: DbRecurringItem) = dao.deleteRecurringItem(item)

    suspend fun deleteRecurringItemsForUserRaw(userId: String) = dao.deleteRecurringItemsForUserRaw(userId)
}
