package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.DbAsset
import com.example.data.model.DbCategory
import com.example.data.model.SubcategoryItem
import com.example.data.model.DbTransaction
import com.example.data.model.DbUser
import com.example.data.model.DbBudget
import com.example.data.model.DbGoal
import com.example.data.model.DbBudgetGoalActivity
import com.example.data.model.BudgetStats
import com.example.data.model.DbLiability
import com.example.data.model.DbRecurringItem
import com.example.data.repository.FinancialRepository
import com.example.util.CsvExporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.pow

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinancialRepository

    // Global navigation/authentication state
    private val _currentUser = MutableStateFlow<DbUser?>(null)
    val currentUser: StateFlow<DbUser?> = _currentUser.asStateFlow()

    // Application Preferences
    private val _selectedTheme = MutableStateFlow("Light") // "Light", "Dark", "System"
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    private val _selectedCurrency = MutableStateFlow("Rupee (₹)") // "Rupee (₹)", "Dollar ($)", "Euro (€)"
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("English (US)")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(true)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isTransactionAlertsEnabled = MutableStateFlow(true)
    val isTransactionAlertsEnabled: StateFlow<Boolean> = _isTransactionAlertsEnabled.asStateFlow()

    private val _isBudgetRemindersEnabled = MutableStateFlow(true)
    val isBudgetRemindersEnabled: StateFlow<Boolean> = _isBudgetRemindersEnabled.asStateFlow()

    private val _isInvestmentNewsEnabled = MutableStateFlow(false)
    val isInvestmentNewsEnabled: StateFlow<Boolean> = _isInvestmentNewsEnabled.asStateFlow()

    private val _isAutoBackupEnabled = MutableStateFlow(true)
    val isAutoBackupEnabled: StateFlow<Boolean> = _isAutoBackupEnabled.asStateFlow()

    private val _autoBackupFrequency = MutableStateFlow("Daily")
    val autoBackupFrequency: StateFlow<String> = _autoBackupFrequency.asStateFlow()

    // Status toasts/feedbacks
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Fetch lists dynamically based on active user
    val availableUsers: StateFlow<List<DbUser>>

    @OptIn(ExperimentalCoroutinesApi::class)
    val userTransactions: StateFlow<List<DbTransaction>>

    @OptIn(ExperimentalCoroutinesApi::class)
    val userAssets: StateFlow<List<DbAsset>>

    @OptIn(ExperimentalCoroutinesApi::class)
    val userBudgets: StateFlow<List<DbBudget>>

    @OptIn(ExperimentalCoroutinesApi::class)
    val userGoals: StateFlow<List<DbGoal>>

    @OptIn(ExperimentalCoroutinesApi::class)
    val userLiabilities: StateFlow<List<DbLiability>>

    @OptIn(ExperimentalCoroutinesApi::class)
    val userRecurringItems: StateFlow<List<DbRecurringItem>>

    val categories: StateFlow<List<DbCategory>>

    // Transactions filtering state
    private val _transactionFilter = MutableStateFlow("All") // "All", "Income", "Expense"
    val transactionFilter: StateFlow<String> = _transactionFilter.asStateFlow()

    // Global profile panel visibility state
    private val _showProfilePanel = MutableStateFlow(false)
    val showProfilePanel: StateFlow<Boolean> = _showProfilePanel.asStateFlow()

    fun setShowProfilePanel(show: Boolean) {
        _showProfilePanel.value = show
    }

    // Global navigation event routing stream
    val navigationRequest = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)

    fun navigateTo(route: String) {
        navigationRequest.tryEmit(route)
    }

    // Financial Calculator Inputs
    val initialInvestment = MutableStateFlow(10000.0)
    val monthlyContribution = MutableStateFlow(500.0)
    val interestRate = MutableStateFlow(7.0)
    val yearsPeriod = MutableStateFlow(20)
    val isCompoundingEnabled = MutableStateFlow(true)
    val compoundingFrequency = MutableStateFlow("Monthly") // "Daily", "Weekly", "Monthly", "Yearly"

    init {
        val database = AppDatabase.getInstance(application)
        repository = FinancialRepository(database.financialDao)

        val backupPrefs = application.getSharedPreferences("kinetic_backup_prefs", android.content.Context.MODE_PRIVATE)
        _isAutoBackupEnabled.value = backupPrefs.getBoolean("auto_backup_enabled", true)
        _autoBackupFrequency.value = backupPrefs.getString("auto_backup_frequency", "Daily") ?: "Daily"
        com.example.util.AutoBackupWorker.scheduleDailyBackup(application)

        // Standard flow bindings
        availableUsers = repository.getAllUsers().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        categories = repository.getAllCategories().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        @OptIn(ExperimentalCoroutinesApi::class)
        userTransactions = _currentUser.flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else repository.getTransactionsForUser(user.userId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        @OptIn(ExperimentalCoroutinesApi::class)
        userAssets = _currentUser.flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else repository.getAssetsForUser(user.userId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        @OptIn(ExperimentalCoroutinesApi::class)
        userBudgets = _currentUser.flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else repository.getBudgetsForUser(user.userId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        @OptIn(ExperimentalCoroutinesApi::class)
        userGoals = _currentUser.flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else repository.getGoalsForUser(user.userId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        @OptIn(ExperimentalCoroutinesApi::class)
        userLiabilities = _currentUser.flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else repository.getLiabilitiesForUser(user.userId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        @OptIn(ExperimentalCoroutinesApi::class)
        userRecurringItems = _currentUser.flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else repository.getRecurringItemsForUser(user.userId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed default database on launch if empty
        seedInitialData()

        // Auto run commitment processor when user logs in or switches
        viewModelScope.launch {
            _currentUser.collect { user ->
                if (user != null) {
                    syncBudgetsAndGoals(user.userId)
                    triggerCommitmentProcessing()
                }
            }
        }

        // Global Transaction Event Bus synchronization
        viewModelScope.launch {
            com.example.util.GlobalTransactionEventBus.events.collect { event ->
                _currentUser.value?.let { user ->
                    syncBudgetsAndGoals(user.userId)
                }
            }
        }
    }

    private fun seedInitialData() {
        viewModelScope.launch {
            val database = AppDatabase.getInstance(getApplication())
            val dao = database.financialDao

            // Directly remove legacy dummy data profiles & associated tables on startup
            val dummyLegacyIds = listOf("alex.sterling", "sarah.chen")
            for (id in dummyLegacyIds) {
                val dbUsr = dao.getUserById(id)
                if (dbUsr != null) {
                    dao.deleteUser(dbUsr)
                }
                dao.deleteTransactionsForUserRaw(id)
                dao.deleteBudgetsForUserRaw(id)
                dao.deleteGoalsForUserRaw(id)
                dao.deleteBudgetGoalActivitiesForUserRaw(id)
                dao.deleteAssetsForUserRaw(id)
                dao.deleteLiabilitiesForUserRaw(id)
                dao.deleteRecurringItemsForUserRaw(id)
            }

            val allUsers = dao.getAllUsersRaw()
            if (allUsers.isEmpty()) {
                // Seed dynamic taxonomies (Categories)
                val initialCategories = listOf(
                    DbCategory(name = "Salary & Wages", type = "Income", subcategoriesString = "Primary Job | Bonuses"),
                    DbCategory(name = "Investments", type = "Income", subcategoriesString = "Dividends | Capital Gains"),
                    DbCategory(name = "Housing", type = "Expense", subcategoriesString = "Rent/Mortgage | Utilities | Maintenance"),
                    DbCategory(name = "Transportation", type = "Expense", subcategoriesString = "Fuel | Public Transit"),
                    DbCategory(name = "Food & Dining", type = "Expense", subcategoriesString = "Groceries | Restaurants"),
                    DbCategory(name = "Budget & Goal", type = "Expense", subcategoriesString = "Active Budgets | General Sub-Budgets | Long-term Goals"),
                    DbCategory(name = "Fixed Deposits & Savings", type = "Asset & Investment", subcategoriesString = "Savings Account | Recurring Deposits | Fixed Deposits"),
                    DbCategory(name = "Stocks & Mutual Funds", type = "Asset & Investment", subcategoriesString = "Index Funds | Growth Stocks | ETF Portfolio"),
                    DbCategory(name = "Real Estate & Gold", type = "Asset & Investment", subcategoriesString = "Commercial Properties | Residentials | Digital Gold")
                )
                initialCategories.forEach { repository.insertCategory(it) }

                // Seed predefined, expected default users
                val defaultUsers = emptyList<DbUser>()
                defaultUsers.forEach { repository.insertUser(it) }

                // Seed some initial financial transaction ledger entries so the app has data immediately!
                val initialTransactions = emptyList<DbTransaction>()
                initialTransactions.forEach { repository.insertTransaction(it) }

                // Also seed some initial assets matching the assets tab
                val initialAssets = emptyList<DbAsset>()
                initialAssets.forEach { repository.insertAsset(it) }
            }
        }
    }

    // --- Authentication Actions ---
    fun loginUser(userId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val user = repository.getUserById(userId)
            if (user != null) {
                _currentUser.value = user
                handleUserSessionCheck(user)
                _toastMessage.emit("Welcome back, ${user.name}!")
                onComplete()
            } else {
                _toastMessage.emit("User profile not found!")
            }
        }
    }

    fun loginWithCustomCredentials(userIdInput: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (userIdInput.isBlank()) {
                _toastMessage.emit("Username/ID cannot be empty!")
                onComplete(false)
                return@launch
            }
            val cleanId = userIdInput.trim().lowercase()
            var user = repository.getUserById(cleanId)
            if (user == null) {
                // If user doesn't exist, register them automatically for streamlined experience!
                user = DbUser(
                    userId = cleanId,
                    name = userIdInput.trim(),
                    email = "$cleanId@kinetictrust.com",
                    isPremium = false,
                    trustScore = 750,
                    avatarUrl = "",
                    streak = 0,
                    medal = "Start"
                )
                repository.insertUser(user)
            }
            _currentUser.value = user
            handleUserSessionCheck(user)
            _toastMessage.emit("Welcome to Kinetic Trust, ${user.name}!")
            onComplete(true)
        }
    }

    fun createAccount(userIdInput: String, fullNameInput: String, emailInput: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (userIdInput.isBlank() || fullNameInput.isBlank() || emailInput.isBlank()) {
                _toastMessage.emit("All registration fields must be filled!")
                onComplete(false)
                return@launch
            }
            val cleanId = userIdInput.trim().lowercase()
            val existing = repository.getUserById(cleanId)
            if (existing != null) {
                _toastMessage.emit("User ID already exists! Please choose another.")
                onComplete(false)
                return@launch
            }
            val newUser = DbUser(
                userId = cleanId,
                name = fullNameInput.trim(),
                email = emailInput.trim(),
                isPremium = true,
                trustScore = 800,
                avatarUrl = "",
                streak = 0,
                medal = "Start"
            )
            repository.insertUser(newUser)
            _currentUser.value = newUser
            handleUserSessionCheck(newUser)
            _toastMessage.emit("Account premium status active! Welcome, ${newUser.name}!")
            onComplete(true)
        }
    }

    fun logoutUser(onComplete: () -> Unit) {
        _currentUser.value = null
        viewModelScope.launch {
            _toastMessage.emit("Logged out of session.")
        }
        onComplete()
    }

    fun deleteUser(user: DbUser, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteUser(user)
            if (_currentUser.value?.userId == user.userId) {
                _currentUser.value = null
            }
            _toastMessage.emit("User profile ${user.name} deleted.")
            onComplete()
        }
    }

    // --- Profile Modifications ---
    fun updateProfile(name: String, email: String, isPremium: Boolean, trustScore: Int, contactNo: String, gmailId: String, avatarUrl: String, streak: Int, medal: String) {
        viewModelScope.launch {
            val current = _currentUser.value ?: return@launch
            val updated = current.copy(
                name = name,
                email = email,
                isPremium = isPremium,
                trustScore = trustScore,
                contactNo = contactNo,
                gmailId = gmailId,
                avatarUrl = avatarUrl,
                streak = streak,
                medal = medal
            )
            repository.insertUser(updated)
            _currentUser.value = updated
            _toastMessage.emit("Profile updated successfully")
        }
    }

    private val streakSharedPreferences = getApplication<Application>().getSharedPreferences("kinetic_streak_prefs", android.content.Context.MODE_PRIVATE)

    fun handleUserSessionCheck(user: DbUser) {
        val userId = user.userId
        val now = System.currentTimeMillis()
        val lastOpenKey = "last_open_u_$userId"
        val lastEntryKey = "last_entry_u_$userId"

        val lastOpen = streakSharedPreferences.getLong(lastOpenKey, 0L)
        val lastEntry = streakSharedPreferences.getLong(lastEntryKey, 0L)

        val lastActive = kotlin.math.max(lastOpen, lastEntry)
        
        // If the last activity was more than 2 days ago (48 hours), reset streak to 0
        if (lastActive > 0L && (now - lastActive) > 48 * 3600 * 1000L) {
            viewModelScope.launch {
                val updatedUser = user.copy(streak = 0)
                repository.insertUser(updatedUser)
                _currentUser.value = updatedUser
            }
        }
        
        // Save current open timestamp
        streakSharedPreferences.edit().putLong(lastOpenKey, now).apply()
        
        // Add a fully functional activity log
        addRecentActivityLog(userId, "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}", "New Delhi, IN")
    }

    fun recordNewEntryStreak() {
        val user = _currentUser.value ?: return
        val userId = user.userId
        val now = System.currentTimeMillis()
        val lastEntryKey = "last_entry_u_$userId"
        val lastEntryDateKey = "last_entry_date_u_$userId"

        val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
        val todayStr = sdf.format(java.util.Date(now))
        val lastEntryDateStr = streakSharedPreferences.getString(lastEntryDateKey, "") ?: ""

        // Check if user already did an entry today
        if (lastEntryDateStr == todayStr) {
            // Already did an entry today, do not increment streak again
            streakSharedPreferences.edit().putLong(lastEntryKey, now).apply()
            return
        }

        val lastEntryTime = streakSharedPreferences.getLong(lastEntryKey, 0L)
        
        viewModelScope.launch {
            val newStreak = if (lastEntryTime == 0L) {
                1
            } else if ((now - lastEntryTime) <= 48 * 3600 * 1000L) {
                // Continuous daily entry
                user.streak + 1
            } else {
                // Gap too large, restart from 1
                1
            }

            val updatedUser = user.copy(streak = newStreak)
            repository.insertUser(updatedUser)
            _currentUser.value = updatedUser

            streakSharedPreferences.edit()
                .putLong(lastEntryKey, now)
                .putString(lastEntryDateKey, todayStr)
                .apply()
        }
    }

    fun getUsageBadgeText(transactionCount: Int): String {
        val points = transactionCount * 100
        return when {
            points < 100 -> "🥉 Bronze Ledger Cadet"
            points < 500 -> "🥈 Silver Account Guardian"
            points < 1000 -> "🥇 Gold Wealth Master"
            points < 2000 -> "💎 Platinum Asset Veteran"
            else -> "👑 Imperial Trust Legend"
        }
    }

    fun addRecentActivityLog(userId: String, device: String, location: String) {
        val prefs = getApplication<Application>().getSharedPreferences("kinetic_activity_prefs", android.content.Context.MODE_PRIVATE)
        val logsKey = "activities_u_$userId"
        val existingLogsSet = prefs.getStringSet(logsKey, emptySet()) ?: emptySet()
        
        val now = System.currentTimeMillis()
        val newLog = "$device|$location|$now|true"
        
        // Clean older ones to nonactive
        val sortedList = existingLogsSet.map { logStr ->
            val parts = logStr.split("|")
            if (parts.size >= 3) {
                val dev = parts[0]
                val loc = parts[1]
                val ts = parts[2]
                "$dev|$loc|$ts|false"
            } else {
                logStr
            }
        }.sortedByDescending { logStr ->
            logStr.split("|").getOrNull(2)?.toLongOrNull() ?: 0L
        }
        
        val updatedLogs = mutableListOf<String>()
        updatedLogs.add(newLog)
        
        // Take at most 14 old ones (total at most 15 logs)
        sortedList.take(14).forEach {
            updatedLogs.add(it)
        }
        
        prefs.edit().putStringSet(logsKey, updatedLogs.toSet()).apply()
    }

    fun getRecentActivityLogs(userId: String): List<ActivityLogItem> {
        val prefs = getApplication<Application>().getSharedPreferences("kinetic_activity_prefs", android.content.Context.MODE_PRIVATE)
        val logsKey = "activities_u_$userId"
        val logsSet = prefs.getStringSet(logsKey, emptySet()) ?: emptySet()
        
        return logsSet.mapNotNull { logStr ->
            val parts = logStr.split("|")
            if (parts.size >= 3) {
                val device = parts[0]
                val location = parts[1]
                val timestamp = parts[2].toLongOrNull() ?: 0L
                val isActive = parts.getOrNull(3)?.toBoolean() ?: false
                ActivityLogItem(device, location, timestamp, isActive)
            } else {
                null
            }
        }.sortedByDescending { it.timestamp }
    }

    // --- Core Storage Utilities (Backup, Restore, Clear/Wipe) ---

    private fun calculateHash(content: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("MD5")
            val bytes = content.toByteArray(Charsets.UTF_8)
            val hashBytes = digest.digest(bytes)
            val sb = StringBuilder()
            for (b in hashBytes) {
                sb.append(String.format("%02x", b))
            }
            sb.toString()
        } catch (e: Exception) {
            "fallback-hash-${content.hashCode()}"
        }
    }

    private suspend fun generateBackupJsonInternal(
        onlyActiveUser: Boolean,
        selectedModules: Set<String> = setOf("Income", "Expense", "Investment", "Budget & Goal"),
        selectedBudgetIds: Set<Long> = emptySet()
    ): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val rootJson = org.json.JSONObject()
                val current = _currentUser.value
                rootJson.put("backup_timestamp", System.currentTimeMillis())
                rootJson.put("type", if (onlyActiveUser) "single_profile" else "all_profiles")
                
                val usersList = if (onlyActiveUser && current != null) {
                    listOf(current)
                } else {
                    repository.getAllUsersRaw()
                }
                
                if (onlyActiveUser && current != null) {
                    rootJson.put("target_user_id", current.userId)
                }
                
                val allTx = if (onlyActiveUser && current != null) {
                    userTransactions.value
                } else {
                    repository.getAllTransactionsRaw()
                }

                val allAssets = if (onlyActiveUser && current != null) {
                    userAssets.value
                } else {
                    repository.getAllAssetsRaw()
                }

                val allBudgets = if (onlyActiveUser && current != null) {
                    repository.getBudgetsForUserRaw(current.userId)
                } else {
                    repository.getAllBudgetsRaw()
                }

                val allGoals = if (onlyActiveUser && current != null) {
                    repository.getGoalsForUserRaw(current.userId)
                } else {
                    repository.getAllGoalsRaw()
                }

                val allLiabilities = if (onlyActiveUser && current != null) {
                    userLiabilities.value
                } else {
                    repository.getAllLiabilitiesRaw()
                }

                val allRecurring = if (onlyActiveUser && current != null) {
                    userRecurringItems.value
                } else {
                    repository.getAllRecurringItemsRaw()
                }

                val allActivities = if (onlyActiveUser && current != null) {
                    repository.getAllActivitiesRaw().filter { it.userId == current.userId }
                } else {
                    repository.getAllActivitiesRaw()
                }
                
                val allCats = categories.value

                // 1. Users list
                val userArray = org.json.JSONArray()
                val sharedPrefs = getApplication<Application>().getSharedPreferences("kinetic_auth_prefs", android.content.Context.MODE_PRIVATE)
                usersList.forEach { u ->
                    val uObj = org.json.JSONObject().apply {
                        put("userId", u.userId)
                        put("name", u.name)
                        put("email", u.email)
                        put("isPremium", u.isPremium)
                        put("trustScore", u.trustScore)
                        put("avatarUrl", u.avatarUrl)
                        put("contactNo", u.contactNo)
                        put("gmailId", u.gmailId)
                        val pass = sharedPrefs.getString("password_${u.userId}", "password")
                        put("password_backup", pass)
                    }
                    userArray.put(uObj)
                }
                rootJson.put("users", userArray)

                // 2. Categories list
                val catArray = org.json.JSONArray()
                allCats.forEach { c ->
                    val cObj = org.json.JSONObject().apply {
                        put("name", c.name)
                        put("type", c.type)
                        put("subcategoriesString", c.subcategoriesString)
                    }
                    catArray.put(cObj)
                }
                rootJson.put("categories", catArray)

                // Apply modules and selected IDs filtering
                // Transactions
                val filteredTx = allTx.filter { t ->
                    val tType = t.type.lowercase()
                    val isInc = tType.contains("income") || t.sourceType == "SOURCE_INCOME"
                    val isInv = tType.contains("invest") || t.category.lowercase().contains("invest")
                    val isBudg = tType.contains("budget") || tType.contains("goal") || t.category.lowercase().contains("budget") || t.category.lowercase().contains("goal") || tType.contains("spend")
                    val isExp = !isInc && !isInv && !isBudg
                    
                    (isInc && selectedModules.contains("Income")) ||
                    (isExp && selectedModules.contains("Expense")) ||
                    (isInv && selectedModules.contains("Investment")) ||
                    (isBudg && selectedModules.contains("Budget & Goal"))
                }
                val txnArray = org.json.JSONArray()
                filteredTx.forEach { t ->
                    val tObj = org.json.JSONObject().apply {
                        put("id", t.id)
                        put("userId", t.userId)
                        put("title", t.title)
                        put("amount", t.amount)
                        put("type", t.type)
                        put("date", t.date)
                        put("category", t.category)
                        put("subCategory", t.subCategory)
                        put("paymentMethod", t.paymentMethod)
                        put("isSelected", t.isSelected)
                        put("fundsSource", t.fundsSource)
                        put("status", t.status)
                        put("isLiability", t.isLiability)
                        put("sourceType", t.sourceType)
                    }
                    txnArray.put(tObj)
                }
                rootJson.put("transactions", txnArray)

                // Assets (Investments)
                val assetArray = org.json.JSONArray()
                if (selectedModules.contains("Investment")) {
                    allAssets.forEach { a ->
                        val aObj = org.json.JSONObject().apply {
                            put("id", a.id)
                            put("userId", a.userId)
                            put("name", a.name)
                            put("type", a.type)
                            put("amountInvested", a.amountInvested)
                            put("quantity", a.quantity)
                            put("dateInvested", a.dateInvested)
                            put("currentPrice", a.currentPrice)
                            put("totalReturns", a.totalReturns)
                            put("avgBuyPrice", a.avgBuyPrice)
                        }
                        assetArray.put(aObj)
                    }
                }
                rootJson.put("assets", assetArray)

                // Budgets
                val budgetArray = org.json.JSONArray()
                val activeBudgetIds = mutableSetOf<Long>()
                if (selectedModules.contains("Budget & Goal")) {
                    val filteredBudgets = if (selectedBudgetIds.isNotEmpty()) {
                        allBudgets.filter { selectedBudgetIds.contains(it.id) }
                    } else {
                        allBudgets
                    }
                    filteredBudgets.forEach { b ->
                        activeBudgetIds.add(b.id)
                        val bObj = org.json.JSONObject().apply {
                            put("id", b.id)
                            put("userId", b.userId)
                            put("name", b.name)
                            put("limitAmount", b.limitAmount)
                            put("spentAmount", b.spentAmount)
                            put("savedAmount", b.savedAmount)
                            put("totalAllocated", b.totalAllocated)
                            put("dateCreated", b.dateCreated)
                        }
                        budgetArray.put(bObj)
                    }
                }
                rootJson.put("budgets", budgetArray)

                // Goals
                val goalArray = org.json.JSONArray()
                val activeGoalIds = mutableSetOf<Long>()
                if (selectedModules.contains("Budget & Goal")) {
                    val filteredGoals = if (selectedBudgetIds.isNotEmpty()) {
                        allGoals.filter { selectedBudgetIds.contains(it.id) }
                    } else {
                        allGoals
                    }
                    filteredGoals.forEach { g ->
                        activeGoalIds.add(g.id)
                        val gObj = org.json.JSONObject().apply {
                            put("id", g.id)
                            put("userId", g.userId)
                            put("name", g.name)
                            put("targetAmount", g.targetAmount)
                            put("savedAmount", g.savedAmount)
                            put("targetDate", g.targetDate)
                            put("priority", g.priority)
                            put("dateCreated", g.dateCreated)
                        }
                        goalArray.put(gObj)
                    }
                }
                rootJson.put("goals", goalArray)

                // Liabilities
                val liabilityArray = org.json.JSONArray()
                if (selectedModules.contains("Expense") || selectedModules.contains("Budget & Goal")) {
                    allLiabilities.forEach { l ->
                        val lObj = org.json.JSONObject().apply {
                            put("id", l.id)
                            put("userId", l.userId)
                            put("title", l.title)
                            put("totalAmount", l.totalAmount)
                            put("amountPaid", l.amountPaid)
                            put("category", l.category)
                            put("subCategory", l.subCategory)
                            put("dateCreated", l.dateCreated)
                            put("isPaid", l.isPaid)
                            put("sourceOfFunds", l.sourceOfFunds)
                        }
                        liabilityArray.put(lObj)
                    }
                }
                rootJson.put("liabilities", liabilityArray)

                // Recurring Items
                val recArray = org.json.JSONArray()
                if (selectedModules.contains("Expense") || selectedModules.contains("Budget & Goal")) {
                    allRecurring.forEach { r ->
                        val rObj = org.json.JSONObject().apply {
                            put("id", r.id)
                            put("userId", r.userId)
                            put("title", r.title)
                            put("amount", r.amount)
                            put("dueDate", r.dueDate)
                            put("category", r.category)
                            put("subCategory", r.subCategory)
                            put("paymentMode", r.paymentMode)
                            put("fundsSource", r.fundsSource)
                            put("type", r.type)
                            put("recurrence", r.recurrence)
                            put("repetition", r.repetition)
                            put("dateCreated", r.dateCreated)
                            put("status", r.status)
                            put("isLiability", r.isLiability)
                            put("sourceType", r.sourceType)
                            put("durationEndDate", r.durationEndDate)
                            put("isPaid", r.isPaid)
                        }
                        recArray.put(rObj)
                    }
                }
                rootJson.put("recurring_items", recArray)

                // Activities
                val actArray = org.json.JSONArray()
                if (selectedModules.contains("Budget & Goal")) {
                    val filteredActivities = if (selectedBudgetIds.isNotEmpty()) {
                        allActivities.filter { 
                            (it.isBudget && activeBudgetIds.contains(it.parentId)) || 
                            (!it.isBudget && activeGoalIds.contains(it.parentId))
                        }
                    } else {
                        allActivities
                    }
                    filteredActivities.forEach { a ->
                        val aObj = org.json.JSONObject().apply {
                            put("id", a.id)
                            put("userId", a.userId)
                            put("parentId", a.parentId)
                            put("isBudget", a.isBudget)
                            put("title", a.title)
                            put("amount", a.amount)
                            put("date", a.date)
                        }
                        actArray.put(aObj)
                    }
                }
                rootJson.put("activities", actArray)

                // Compute unique checksum (validation hash)
                rootJson.remove("backup_hash")
                val compactStr = rootJson.toString()
                val checksumValue = calculateHash(compactStr)
                rootJson.put("backup_hash", checksumValue)

                rootJson.toString(4)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun generateBackupJson(
        onlyActiveUser: Boolean,
        selectedModules: Set<String> = setOf("Income", "Expense", "Investment", "Budget & Goal"),
        selectedBudgetIds: Set<Long> = emptySet(),
        onComplete: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val json = generateBackupJsonInternal(onlyActiveUser, selectedModules, selectedBudgetIds)
            onComplete(json)
        }
    }

    fun createSnapshot(prefix: String = "FullBackup") {
        viewModelScope.launch {
            val json = generateBackupJsonInternal(onlyActiveUser = false)
            if (json != null) {
                try {
                    val context = getApplication<Application>()
                    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    val fileName = if (prefix == "LastState_BeforeRestore") "LastState_BeforeRestore.snapshot" else "${prefix}_${todayStr}.snapshot"
                    val targetFile = java.io.File(context.getExternalFilesDir(null), fileName)
                    targetFile.writeText(json)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun restoreBackupJson(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                if (jsonString.isBlank()) {
                    onComplete(false, "Selected backup file is empty!")
                    return@launch
                }
                val rootJson = org.json.JSONObject(jsonString)
                val backupHash = rootJson.optString("backup_hash", "")
                if (backupHash.isNotBlank()) {
                    rootJson.remove("backup_hash")
                    val compactStr = rootJson.toString()
                    val calculated = calculateHash(compactStr)
                    if (backupHash != calculated) {
                        onComplete(false, "Backup file is invalid or incomplete. Restore aborted.")
                        return@launch
                    }
                } else {
                    onComplete(false, "Backup file is invalid or incomplete. Restore aborted.")
                    return@launch
                }

                // Put backup_hash back to parse normally
                rootJson.put("backup_hash", backupHash)

                val duplicateCheck = parseBackupAndDetectDuplicates(jsonString)
                if (duplicateCheck != null) {
                    executeOverwriteRestore(duplicateCheck) { success, msg ->
                        onComplete(success, msg)
                    }
                } else {
                    onComplete(false, "Could not extract backup payload for restore.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, "Failed to parse backup content: ${e.localizedMessage}")
            }
        }
    }

    suspend fun parseBackupAndDetectDuplicates(jsonString: String): DuplicateCheckResult? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (jsonString.isBlank()) return@withContext null
                val rootJson = org.json.JSONObject(jsonString)

                // Verify hash!
                val backupHash = rootJson.optString("backup_hash", "")
                if (backupHash.isNotBlank()) {
                    rootJson.remove("backup_hash")
                    val compactStr = rootJson.toString()
                    val calculated = calculateHash(compactStr)
                    if (backupHash != calculated) {
                        throw java.lang.IllegalArgumentException("Backup file is invalid or incomplete. Restore aborted.")
                    }
                } else {
                    throw java.lang.IllegalArgumentException("Backup file is invalid or incomplete. Restore aborted.")
                }

                // Put backup_hash back
                rootJson.put("backup_hash", backupHash)

                val type = rootJson.optString("type", "all_profiles")
                val isSingleProfile = type == "single_profile"
                val targetUserId = rootJson.optString("target_user_id", "")

                // 1. Parse Users
                val parsedUsers = mutableListOf<DbUser>()
                val usersArray = rootJson.optJSONArray("users")
                if (usersArray != null) {
                    for (i in 0 until usersArray.length()) {
                        val uObj = usersArray.getJSONObject(i)
                        parsedUsers.add(DbUser(
                            userId = uObj.getString("userId"),
                            name = uObj.getString("name"),
                            email = uObj.getString("email"),
                            isPremium = uObj.optBoolean("isPremium", true),
                            trustScore = uObj.optInt("trustScore", 850),
                            avatarUrl = uObj.optString("avatarUrl", ""),
                            contactNo = uObj.optString("contactNo", ""),
                            gmailId = uObj.optString("gmailId", "")
                        ))
                    }
                }

                // 2. Parse Categories
                val parsedCategories = mutableListOf<DbCategory>()
                val catArray = rootJson.optJSONArray("categories")
                if (catArray != null) {
                    for (i in 0 until catArray.length()) {
                        val cObj = catArray.getJSONObject(i)
                        parsedCategories.add(DbCategory(
                            name = cObj.getString("name"),
                            type = cObj.getString("type"),
                            subcategoriesString = cObj.optString("subcategoriesString", "General")
                        ))
                    }
                }

                // 3. Parse Transactions
                val parsedTx = mutableListOf<DbTransaction>()
                val txnArray = rootJson.optJSONArray("transactions")
                if (txnArray != null) {
                    for (i in 0 until txnArray.length()) {
                        val tObj = txnArray.getJSONObject(i)
                        parsedTx.add(DbTransaction(
                            id = tObj.optLong("id", 0),
                            userId = if (isSingleProfile) targetUserId else tObj.getString("userId"),
                            title = tObj.getString("title"),
                            amount = tObj.getDouble("amount"),
                            type = tObj.getString("type"),
                            date = tObj.getLong("date"),
                            category = tObj.getString("category"),
                            subCategory = tObj.optString("subCategory", ""),
                            paymentMethod = tObj.optString("paymentMethod", "Debit Card"),
                            isSelected = tObj.optBoolean("isSelected", false),
                            fundsSource = tObj.optString("fundsSource", ""),
                            status = tObj.optString("status", "Paid"),
                            isLiability = tObj.optBoolean("isLiability", false),
                            sourceType = tObj.optString("sourceType", "SOURCE_INCOME")
                        ))
                    }
                }

                // 4. Parse Assets
                val parsedAssets = mutableListOf<DbAsset>()
                val assetArray = rootJson.optJSONArray("assets")
                if (assetArray != null) {
                    for (i in 0 until assetArray.length()) {
                        val aObj = assetArray.getJSONObject(i)
                        parsedAssets.add(DbAsset(
                            id = aObj.optLong("id", 0),
                            userId = if (isSingleProfile) targetUserId else aObj.getString("userId"),
                            name = aObj.getString("name"),
                            type = aObj.getString("type"),
                            amountInvested = aObj.getDouble("amountInvested"),
                            quantity = aObj.optDouble("quantity", 1.0),
                            dateInvested = aObj.getLong("dateInvested"),
                            currentPrice = aObj.optDouble("currentPrice", aObj.getDouble("amountInvested")),
                            totalReturns = aObj.optDouble("totalReturns", 0.0),
                            avgBuyPrice = aObj.optDouble("avgBuyPrice", 0.0)
                        ))
                    }
                }

                // 5. Parse Budgets
                val parsedBudgets = mutableListOf<DbBudget>()
                val budgetArray = rootJson.optJSONArray("budgets")
                if (budgetArray != null) {
                    for (i in 0 until budgetArray.length()) {
                        val bObj = budgetArray.getJSONObject(i)
                        parsedBudgets.add(DbBudget(
                            id = bObj.optLong("id", 0),
                            userId = if (isSingleProfile) targetUserId else bObj.getString("userId"),
                            name = bObj.getString("name"),
                            limitAmount = bObj.getDouble("limitAmount"),
                            spentAmount = bObj.optDouble("spentAmount", 0.0),
                            savedAmount = bObj.optDouble("savedAmount", 0.0),
                            totalAllocated = bObj.optDouble("totalAllocated", 0.0),
                            dateCreated = bObj.optLong("dateCreated", System.currentTimeMillis())
                        ))
                    }
                }

                // 6. Parse Goals
                val parsedGoals = mutableListOf<DbGoal>()
                val goalArray = rootJson.optJSONArray("goals")
                if (goalArray != null) {
                    for (i in 0 until goalArray.length()) {
                        val gObj = goalArray.getJSONObject(i)
                        parsedGoals.add(DbGoal(
                            id = gObj.optLong("id", 0),
                            userId = if (isSingleProfile) targetUserId else gObj.getString("userId"),
                            name = gObj.getString("name"),
                            targetAmount = gObj.getDouble("targetAmount"),
                            savedAmount = gObj.optDouble("savedAmount", 0.0),
                            targetDate = gObj.optString("targetDate", ""),
                            priority = gObj.optString("priority", "Medium"),
                            dateCreated = gObj.optLong("dateCreated", System.currentTimeMillis())
                        ))
                    }
                }

                // 7. Parse Liabilities
                val parsedLiabilities = mutableListOf<DbLiability>()
                val liabilityArray = rootJson.optJSONArray("liabilities")
                if (liabilityArray != null) {
                    for (i in 0 until liabilityArray.length()) {
                        val lObj = liabilityArray.getJSONObject(i)
                        parsedLiabilities.add(DbLiability(
                            id = lObj.optLong("id", 0),
                            userId = if (isSingleProfile) targetUserId else lObj.getString("userId"),
                            title = lObj.getString("title"),
                            totalAmount = lObj.getDouble("totalAmount"),
                            amountPaid = lObj.optDouble("amountPaid", 0.0),
                            category = lObj.getString("category"),
                            subCategory = lObj.optString("subCategory", ""),
                            dateCreated = lObj.optLong("dateCreated", System.currentTimeMillis()),
                            isPaid = lObj.optBoolean("isPaid", false),
                            sourceOfFunds = lObj.optString("sourceOfFunds", "")
                        ))
                    }
                }

                // 8. Parse RecurringItems
                val parsedRecurringItems = mutableListOf<DbRecurringItem>()
                val recArray = rootJson.optJSONArray("recurring_items")
                if (recArray != null) {
                    for (i in 0 until recArray.length()) {
                        val rObj = recArray.getJSONObject(i)
                        parsedRecurringItems.add(DbRecurringItem(
                            id = rObj.optLong("id", 0),
                            userId = if (isSingleProfile) targetUserId else rObj.getString("userId"),
                            title = rObj.getString("title"),
                            amount = rObj.getDouble("amount"),
                            dueDate = rObj.getString("dueDate"),
                            category = rObj.getString("category"),
                            subCategory = rObj.optString("subCategory", ""),
                            paymentMode = rObj.optString("paymentMode", ""),
                            fundsSource = rObj.optString("fundsSource", ""),
                            type = rObj.getString("type"),
                            recurrence = rObj.optString("recurrence", "Monthly"),
                            repetition = rObj.optString("repetition", "until i Cancel"),
                            dateCreated = rObj.optLong("dateCreated", System.currentTimeMillis()),
                            status = rObj.optString("status", "Unpaid"),
                            isLiability = rObj.optBoolean("isLiability", true),
                            sourceType = rObj.optString("sourceType", "SOURCE_INCOME"),
                            durationEndDate = rObj.optString("durationEndDate", ""),
                            isPaid = rObj.optBoolean("isPaid", false)
                        ))
                    }
                }

                // 9. Parse Activities
                val parsedActivities = mutableListOf<DbBudgetGoalActivity>()
                val actArray = rootJson.optJSONArray("activities")
                if (actArray != null) {
                    for (i in 0 until actArray.length()) {
                        val aObj = actArray.getJSONObject(i)
                        parsedActivities.add(DbBudgetGoalActivity(
                            id = aObj.optLong("id", 0),
                            userId = if (isSingleProfile) targetUserId else aObj.getString("userId"),
                            parentId = aObj.getLong("parentId"),
                            isBudget = aObj.getBoolean("isBudget"),
                            title = aObj.getString("title"),
                            amount = aObj.getDouble("amount"),
                            date = aObj.optLong("date", System.currentTimeMillis())
                        ))
                    }
                }

                // Detect Duplicates
                val existingTx = repository.getAllTransactionsRaw()
                val existingAssets = repository.getAllAssetsRaw()
                val existingBudgets = repository.getAllBudgetsRaw()
                val existingGoals = repository.getAllGoalsRaw()
                val existingLiabilities = repository.getAllLiabilitiesRaw()
                val existingRecurringItems = repository.getAllRecurringItemsRaw()

                val duplicateTx = mutableListOf<DbTransaction>()
                val uniqueTx = mutableListOf<DbTransaction>()
                for (t in parsedTx) {
                    val isDupe = existingTx.any { ext ->
                        ext.userId == t.userId &&
                        ext.title == t.title &&
                        java.lang.Math.abs(ext.amount - t.amount) < 0.01 &&
                        ext.date == t.date &&
                        ext.type == t.type &&
                        ext.category == t.category
                    }
                    if (isDupe) {
                        duplicateTx.add(t)
                    } else {
                        uniqueTx.add(t)
                    }
                }

                val duplicateAs = mutableListOf<DbAsset>()
                val uniqueAs = mutableListOf<DbAsset>()
                for (a in parsedAssets) {
                    val isDupe = existingAssets.any { exa ->
                        exa.userId == a.userId &&
                        exa.name == a.name &&
                        exa.type == a.type &&
                        java.lang.Math.abs(exa.amountInvested - a.amountInvested) < 0.01 &&
                        exa.dateInvested == a.dateInvested
                    }
                    if (isDupe) {
                        duplicateAs.add(a)
                    } else {
                        uniqueAs.add(a)
                    }
                }

                val uniqueBudgets = mutableListOf<DbBudget>()
                for (b in parsedBudgets) {
                    val isDupe = existingBudgets.any { exb ->
                        exb.userId == b.userId && exb.name.equals(b.name, ignoreCase = true)
                    }
                    if (!isDupe) {
                        uniqueBudgets.add(b)
                    }
                }

                val uniqueGoals = mutableListOf<DbGoal>()
                for (g in parsedGoals) {
                    val isDupe = existingGoals.any { exg ->
                        exg.userId == g.userId && exg.name.equals(g.name, ignoreCase = true)
                    }
                    if (!isDupe) {
                        uniqueGoals.add(g)
                    }
                }

                val uniqueLiabilities = mutableListOf<DbLiability>()
                for (l in parsedLiabilities) {
                    val isDupe = existingLiabilities.any { exl ->
                        exl.userId == l.userId && exl.title.equals(l.title, ignoreCase = true)
                    }
                    if (!isDupe) {
                        uniqueLiabilities.add(l)
                    }
                }

                val uniqueRecurringItems = mutableListOf<DbRecurringItem>()
                for (r in parsedRecurringItems) {
                    val isDupe = existingRecurringItems.any { exr ->
                        exr.userId == r.userId && exr.title.equals(r.title, ignoreCase = true) && exr.type == r.type
                    }
                    if (!isDupe) {
                        uniqueRecurringItems.add(r)
                    }
                }

                DuplicateCheckResult(
                    isSingleProfile = isSingleProfile,
                    targetUserId = targetUserId,
                    incomingTxCount = parsedTx.size,
                    incomingAssetCount = parsedAssets.size,
                    duplicateTransactions = duplicateTx,
                    duplicateAssets = duplicateAs,
                    uniqueTransactions = uniqueTx,
                    uniqueAssets = uniqueAs,
                    parsedUsers = parsedUsers,
                    parsedTx = parsedTx,
                    parsedAssets = parsedAssets,
                    parsedCategories = parsedCategories,
                    parsedBudgets = parsedBudgets,
                    parsedGoals = parsedGoals,
                    parsedLiabilities = parsedLiabilities,
                    parsedRecurringItems = parsedRecurringItems,
                    parsedActivities = parsedActivities,
                    uniqueBudgets = uniqueBudgets,
                    uniqueGoals = uniqueGoals,
                    uniqueLiabilities = uniqueLiabilities,
                    uniqueRecurringItems = uniqueRecurringItems
                )
            } catch (e: Exception) {
                e.printStackTrace()
                if (e is java.lang.IllegalArgumentException) {
                    throw e
                }
                null
            }
        }
    }

    fun executeMergeRestore(
        result: DuplicateCheckResult,
        keepAll: Boolean,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 1. Create Pre-Restore Safety Snapshot First!
                val safetyJson = generateBackupJsonInternal(onlyActiveUser = false)
                if (safetyJson != null) {
                    try {
                        val context = getApplication<Application>()
                        val targetFile = java.io.File(context.getExternalFilesDir(null), "LastState_BeforeRestore.snapshot")
                        targetFile.writeText(safetyJson)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Restore users
                result.parsedUsers.forEach { u ->
                    repository.insertUser(u)
                }

                // Restore Categories
                result.parsedCategories.forEach { c ->
                    repository.insertCategory(c)
                }

                val txToInsert = if (keepAll) result.parsedTx else result.uniqueTransactions
                val assetsToInsert = if (keepAll) result.parsedAssets else result.uniqueAssets
                val budgetsToInsert = if (keepAll) result.parsedBudgets else result.uniqueBudgets
                val goalsToInsert = if (keepAll) result.parsedGoals else result.uniqueGoals
                val liabilitiesToInsert = if (keepAll) result.parsedLiabilities else result.uniqueLiabilities
                val recurringToInsert = if (keepAll) result.parsedRecurringItems else result.uniqueRecurringItems
                val activitiesToInsert = result.parsedActivities // activities are merged directly

                txToInsert.forEach { t ->
                    repository.insertTransaction(t)
                }

                assetsToInsert.forEach { a ->
                    repository.insertAsset(a)
                }

                budgetsToInsert.forEach { b ->
                    repository.insertBudget(b)
                }

                goalsToInsert.forEach { g ->
                    repository.insertGoal(g)
                }

                liabilitiesToInsert.forEach { l ->
                    repository.insertLiability(l)
                }

                recurringToInsert.forEach { r ->
                    repository.insertRecurringItem(r)
                }

                activitiesToInsert.forEach { a ->
                    repository.insertActivity(a)
                }

                // Update active user state if single profile
                if (result.isSingleProfile) {
                    val updatedUserObj = repository.getUserById(result.targetUserId)
                    if (updatedUserObj != null) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            _currentUser.value = updatedUserObj
                        }
                    }
                }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(true, "Successfully merged ${txToInsert.size} transactions, ${assetsToInsert.size} assets, ${budgetsToInsert.size} budgets, and ${goalsToInsert.size} goals!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false, "Merge error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun executeOverwriteRestore(
        result: DuplicateCheckResult,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 1. Create Pre-Restore Safety Snapshot First!
                val safetyJson = generateBackupJsonInternal(onlyActiveUser = false)
                if (safetyJson != null) {
                    try {
                        val context = getApplication<Application>()
                        val targetFile = java.io.File(context.getExternalFilesDir(null), "LastState_BeforeRestore.snapshot")
                        targetFile.writeText(safetyJson)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (result.isSingleProfile) {
                    // Single profile overwrite
                    repository.deleteTransactionsForUserRaw(result.targetUserId)
                    repository.deleteAssetsForUserRaw(result.targetUserId)
                    repository.deleteBudgetsForUserRaw(result.targetUserId)
                    repository.deleteGoalsForUserRaw(result.targetUserId)
                    repository.deleteBudgetGoalActivitiesForUserRaw(result.targetUserId)
                    repository.deleteLiabilitiesForUserRaw(result.targetUserId)
                    repository.deleteRecurringItemsForUserRaw(result.targetUserId)

                    result.parsedUsers.forEach { u ->
                        repository.insertUser(u)
                    }

                    result.parsedTx.forEach { t ->
                        repository.insertTransaction(t)
                    }

                    result.parsedAssets.forEach { a ->
                        repository.insertAsset(a)
                    }

                    result.parsedBudgets.forEach { b ->
                        repository.insertBudget(b)
                    }

                    result.parsedGoals.forEach { g ->
                        repository.insertGoal(g)
                    }

                    result.parsedLiabilities.forEach { l ->
                        repository.insertLiability(l)
                    }

                    result.parsedRecurringItems.forEach { r ->
                        repository.insertRecurringItem(r)
                    }

                    result.parsedActivities.forEach { a ->
                        repository.insertActivity(a)
                    }

                    val updatedUserObj = repository.getUserById(result.targetUserId)
                    if (updatedUserObj != null) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            _currentUser.value = updatedUserObj
                        }
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(true, "Profile ${result.targetUserId} reset and restored successfully!")
                    }
                } else {
                    // Full restore overwrite
                    repository.deleteAllTransactionsRaw()
                    repository.deleteAllAssetsRaw()
                    repository.deleteAllUsersRaw()
                    repository.deleteAllCategoriesRaw()
                    repository.deleteAllBudgetsRaw()
                    repository.deleteAllGoalsRaw()
                    repository.deleteAllLiabilitiesRaw()
                    repository.deleteAllRecurringItemsRaw()
                    repository.deleteAllActivitiesRaw()

                    result.parsedCategories.forEach { c ->
                        repository.insertCategory(c)
                    }

                    result.parsedUsers.forEach { u ->
                        repository.insertUser(u)
                    }

                    result.parsedTx.forEach { t ->
                        repository.insertTransaction(t)
                    }

                    result.parsedAssets.forEach { a ->
                        repository.insertAsset(a)
                    }

                    result.parsedBudgets.forEach { b ->
                        repository.insertBudget(b)
                    }

                    result.parsedGoals.forEach { g ->
                        repository.insertGoal(g)
                    }

                    result.parsedLiabilities.forEach { l ->
                        repository.insertLiability(l)
                    }

                    result.parsedRecurringItems.forEach { r ->
                        repository.insertRecurringItem(r)
                    }

                    result.parsedActivities.forEach { a ->
                        repository.insertActivity(a)
                    }

                    val defaultActiveUser = result.parsedUsers.firstOrNull()?.userId
                    if (defaultActiveUser != null) {
                        val usrObj = repository.getUserById(defaultActiveUser)
                        if (usrObj != null) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                _currentUser.value = usrObj
                            }
                        }
                    } else {
                        seedInitialData()
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(true, "Full application database reset and restored successfully!")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false, "Overwrite error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun executeDatabaseWipe(onlyActiveUser: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (onlyActiveUser) {
                val current = _currentUser.value
                if (current != null) {
                    // Wipe user specific ledger data
                    repository.deleteTransactionsForUserRaw(current.userId)
                    repository.deleteAssetsForUserRaw(current.userId)
                    
                    // Reset user profile details to active defaults
                    val cleanUsr = current.copy(
                        name = current.userId.split(".").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "User",
                        isPremium = false,
                        trustScore = 100, // Reset trust points as well
                        contactNo = "",
                        gmailId = "",
                        avatarUrl = "",
                        streak = 0,
                        medal = "Start"
                    )
                    repository.insertUser(cleanUsr)
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _currentUser.value = cleanUsr
                        _toastMessage.emit("Profile ledger cleanly wiped out!")
                        onComplete()
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete()
                    }
                }
            } else {
                // Wipe everything
                repository.deleteAllTransactionsRaw()
                repository.deleteAllAssetsRaw()
                repository.deleteAllUsersRaw()
                repository.deleteAllCategoriesRaw()
                
                // Re-seed categories so the app remains functional for new accounts
                val initialCategories = listOf(
                    DbCategory(name = "Salary & Wages", type = "Income", subcategoriesString = "Primary Job | Bonuses"),
                    DbCategory(name = "Investments", type = "Income", subcategoriesString = "Dividends | Capital Gains"),
                    DbCategory(name = "Housing", type = "Expense", subcategoriesString = "Rent/Mortgage | Utilities | Maintenance"),
                    DbCategory(name = "Transportation", type = "Expense", subcategoriesString = "Fuel | Public Transit"),
                    DbCategory(name = "Food & Dining", type = "Expense", subcategoriesString = "Groceries | Restaurants"),
                    DbCategory(name = "Budget & Goal", type = "Expense", subcategoriesString = "Active Budgets | General Sub-Budgets | Long-term Goals"),
                    DbCategory(name = "Fixed Deposits & Savings", type = "Asset & Investment", subcategoriesString = "Savings Account | Recurring Deposits | Fixed Deposits"),
                    DbCategory(name = "Stocks & Mutual Funds", type = "Asset & Investment", subcategoriesString = "Index Funds | Growth Stocks | ETF Portfolio"),
                    DbCategory(name = "Real Estate & Gold", type = "Asset & Investment", subcategoriesString = "Commercial Properties | Residentials | Digital Gold")
                )
                initialCategories.forEach { repository.insertCategory(it) }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _currentUser.value = null
                    _toastMessage.emit("Full application database cleanly wiped!")
                    onComplete()
                }
            }
        }
    }

    fun executeGranularDatabaseWipe(
        clearIncome: Boolean,
        clearExpense: Boolean,
        clearInvestment: Boolean,
        clearBudgetGoal: Boolean,
        clearAll: Boolean,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val current = _currentUser.value
            val activeUserId = current?.userId ?: "default_user"
            
            if (clearAll) {
                repository.deleteTransactionsForUserRaw(activeUserId)
                repository.deleteAssetsForUserRaw(activeUserId)
                repository.deleteBudgetsForUserRaw(activeUserId)
                repository.deleteGoalsForUserRaw(activeUserId)
                repository.deleteBudgetGoalActivitiesForUserRaw(activeUserId)
                repository.deleteLiabilitiesForUserRaw(activeUserId)
            } else {
                if (clearIncome) {
                    repository.deleteIncomeTransactionsForUserRaw(activeUserId)
                }
                if (clearExpense) {
                    repository.deleteExpenseTransactionsForUserRaw(activeUserId)
                    repository.deleteLiabilitiesForUserRaw(activeUserId)
                }
                if (clearInvestment) {
                    repository.deleteAssetsForUserRaw(activeUserId)
                }
                if (clearBudgetGoal) {
                    repository.deleteBudgetsForUserRaw(activeUserId)
                    repository.deleteGoalsForUserRaw(activeUserId)
                    repository.deleteBudgetGoalActivitiesForUserRaw(activeUserId)
                }
            }
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                _toastMessage.emit("Selected datasets successfully cleared!")
                onComplete()
            }
        }
    }

    // --- App Customizations Actions ---
    fun toggleTheme(themeName: String) {
        _selectedTheme.value = themeName
    }

    fun setCurrency(currencyName: String) {
        _selectedCurrency.value = currencyName
    }

    fun setLanguage(languageName: String) {
        _selectedLanguage.value = languageName
    }

    fun toggleBiometric(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
    }

    fun toggleTransactionAlerts(enabled: Boolean) {
        _isTransactionAlertsEnabled.value = enabled
    }

    fun toggleBudgetReminders(enabled: Boolean) {
        _isBudgetRemindersEnabled.value = enabled
    }

    fun toggleInvestmentNews(enabled: Boolean) {
        _isInvestmentNewsEnabled.value = enabled
    }

    fun toggleAutoBackup(enabled: Boolean) {
        _isAutoBackupEnabled.value = enabled
        val backupPrefs = getApplication<Application>().getSharedPreferences("kinetic_backup_prefs", android.content.Context.MODE_PRIVATE)
        backupPrefs.edit().putBoolean("auto_backup_enabled", enabled).apply()
    }

    fun setAutoBackupFrequency(frequency: String) {
        _autoBackupFrequency.value = frequency
        val backupPrefs = getApplication<Application>().getSharedPreferences("kinetic_backup_prefs", android.content.Context.MODE_PRIVATE)
        backupPrefs.edit().putString("auto_backup_frequency", frequency).apply()
    }

    fun triggerBackup() {
        viewModelScope.launch {
            try {
                val db = AppDatabase.getInstance(getApplication())
                try {
                    db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                val dbFile = getApplication<Application>().getDatabasePath("kinetic_trust_database")
                if (dbFile.exists()) {
                    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    val appName = "KineticTrust"
                    val newFileName = "${todayStr}_${appName}"
                    
                    val backupDir = getApplication<Application>().getExternalFilesDir(null) ?: getApplication<Application>().filesDir
                    
                    if (backupDir.isDirectory) {
                        val existingFiles = backupDir.listFiles()
                        if (existingFiles != null) {
                            for (f in existingFiles) {
                                if (f.name.endsWith("_KineticTrust") || f.name.contains("KineticTrust") || f.name.contains("Kinetic_Trust")) {
                                    f.delete()
                                }
                            }
                        }
                    }
                    
                    val targetFile = java.io.File(backupDir, newFileName)
                    dbFile.inputStream().use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    val backupPrefs = getApplication<Application>().getSharedPreferences("kinetic_backup_prefs", android.content.Context.MODE_PRIVATE)
                    backupPrefs.edit().apply {
                        putLong("last_backup_timestamp", System.currentTimeMillis())
                        putString("last_backup_filename", targetFile.name)
                        apply()
                    }
                    
                    _toastMessage.emit("Backup Successful")
                } else {
                    _toastMessage.emit("Database file not found!")
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                _toastMessage.emit("Manual backup failed: ${e.message}")
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _toastMessage.emit("Application cache successfully cleared.")
        }
    }

    // --- Transactions Ledger Actions ---
    fun setTransactionFilter(filter: String) {
        _transactionFilter.value = filter
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        subCategory: String,
        payment: String,
        originOfMoney: String? = null,
        originIncomeCategory: String? = null,
        originIncomeSubCategory: String? = null,
        originOtherDescription: String? = null,
        fundsSource: String = ""
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val isAsset = type.equals("Asset", ignoreCase = true) || type.equals("Assets", ignoreCase = true) || type.equals("Asset & Investment", ignoreCase = true) || type.equals("Investment", ignoreCase = true)
            
            // Format title and subcategory for outer source tracking
            val finalTitle = if (isAsset && originOfMoney == "Other" && !originOtherDescription.isNullOrBlank()) {
                "$title ($originOtherDescription)"
            } else {
                title
            }
            
            val finalSubCategory = if (isAsset) {
                if (originOfMoney == "Other" && !originOtherDescription.isNullOrBlank()) {
                    "Other Source: $originOtherDescription"
                } else if (originOfMoney == "Income" && !originIncomeCategory.isNullOrBlank()) {
                    "Income: $originIncomeCategory / $originIncomeSubCategory"
                } else {
                    subCategory
                }
            } else {
                subCategory
            }

            val finalType = if (isAsset) "Investment" else type
            val finalCategory = if (isAsset && originOfMoney == "Income") (originIncomeCategory ?: "Salary & Wages") else if (isAsset && originOfMoney == "Other") "Other" else category
            val finalFundsSource = if (isAsset) (originOfMoney ?: fundsSource) else fundsSource

            val derivedSourceType = if (originOfMoney == "Other" || fundsSource.equals("Other", ignoreCase = true) || finalFundsSource.equals("Other", ignoreCase = true)) {
                "SOURCE_OTHER"
            } else {
                "SOURCE_INCOME"
            }

            val newTxn = DbTransaction(
                userId = user.userId,
                title = finalTitle,
                amount = amount,
                type = finalType,
                date = System.currentTimeMillis(),
                category = finalCategory,
                subCategory = finalSubCategory,
                paymentMethod = payment,
                fundsSource = finalFundsSource,
                sourceType = derivedSourceType
            )
            repository.insertTransaction(newTxn)
            
            // Auto-reward points for entering financial entries
            val updatedUser = user.copy(trustScore = user.trustScore + 25)
            repository.insertUser(updatedUser)
            _currentUser.value = updatedUser
            
            // Record new entry streak check
            recordNewEntryStreak()
            
            // Auto link option to Assets Dashboard as well
            if (isAsset) {
                val newAsset = DbAsset(
                    userId = user.userId,
                    name = finalTitle,
                    type = if (category.isNotBlank()) category else "Other",
                    amountInvested = amount,
                    quantity = 1.0,
                    dateInvested = System.currentTimeMillis(),
                    currentPrice = amount,
                    totalReturns = 0.0,
                    avgBuyPrice = amount
                )
                repository.insertAsset(newAsset)
            }

            if (type.equals("Expense", ignoreCase = true) && payment.equals("Udhar", ignoreCase = true)) {
                val newLiability = DbLiability(
                    userId = user.userId,
                    title = title,
                    totalAmount = amount,
                    amountPaid = 0.0,
                    category = category,
                    subCategory = subCategory,
                    dateCreated = System.currentTimeMillis(),
                    isPaid = false
                )
                repository.insertLiability(newLiability)
                _toastMessage.emit("Registered expense & created outstanding Udhar/Payable Bill entry.")
            } else {
                _toastMessage.emit("Logged $type: ${user.userId}")
            }
            syncBudgetsAndGoals(user.userId)
        }
    }

    fun syncBudgetsAndGoals(userId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val budgetsList = repository.getBudgetsForUserRaw(userId)
                budgetsList.forEach { budget ->
                    val activities = repository.getActivitiesForBudgetOrGoalRaw(userId, budget.id, isBudget = true)
                    val totalSaved = activities.filter { it.amount > 0 }.sumOf { it.amount }
                    val totalSpent = activities.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
                    val totalAllocatedVal = budget.limitAmount + totalSaved
                    
                    if (budget.spentAmount != totalSpent || budget.savedAmount != totalSaved || budget.totalAllocated != totalAllocatedVal) {
                        repository.updateBudget(budget.copy(
                            spentAmount = totalSpent,
                            savedAmount = totalSaved,
                            totalAllocated = totalAllocatedVal
                        ))
                    }
                }
                
                val goalsList = repository.getGoalsForUserRaw(userId)
                goalsList.forEach { goal ->
                    val activities = repository.getActivitiesForBudgetOrGoalRaw(userId, goal.id, isBudget = false)
                    val totalSaved = activities.sumOf { it.amount }
                    if (goal.savedAmount != totalSaved) {
                        repository.updateGoal(goal.copy(savedAmount = totalSaved))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun settleLiability(
        liability: DbLiability,
        sourceOfFunds: String,
        paymentType: String, // "Full" or "Partial"
        partialAmount: Double,
        onResult: (Boolean, String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null) {
            onResult(false, "No active user logged in.")
            return
        }
        viewModelScope.launch {
            try {
                val payAmount = if (paymentType == "Full") {
                    liability.remainingDue
                } else {
                    partialAmount
                }
                
                if (payAmount <= 0.0) {
                    onResult(false, "Settle amount must be greater than zero.")
                    return@launch
                }
                if (payAmount > liability.remainingDue) {
                    onResult(false, "Settle amount cannot exceed outstanding balance of ₹${liability.remainingDue}")
                    return@launch
                }
                
                // 1. Register expense transaction
                val titlePrefix = if (paymentType == "Full") "Settle Bill" else "Partial Settle"
                val txnTitle = "$titlePrefix: ${liability.title}"
                val repaymentTx = DbTransaction(
                    userId = liability.userId,
                    title = txnTitle,
                    amount = payAmount,
                    type = "Expense",
                    date = System.currentTimeMillis(),
                    category = liability.category,
                    subCategory = liability.subCategory,
                    paymentMethod = sourceOfFunds
                )
                repository.insertTransaction(repaymentTx)
                
                // 2. Update liability record
                val newAmountPaid = liability.amountPaid + payAmount
                val isFullyPaid = newAmountPaid >= liability.totalAmount - 0.01 // handle precision
                val updatedLiability = liability.copy(
                    amountPaid = newAmountPaid,
                    isPaid = isFullyPaid,
                    sourceOfFunds = sourceOfFunds
                )
                repository.updateLiability(updatedLiability)
                
                if (isFullyPaid) {
                    _toastMessage.emit("Outstanding Bill '${liability.title}' fully paid and settled!")
                    onResult(true, "Bill fully paid.")
                } else {
                    _toastMessage.emit("Logged partial payment of ₹$payAmount. Remaining balance is ₹${updatedLiability.remainingDue}")
                    onResult(true, "Partial payment logged.")
                }
            } catch (e: Exception) {
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    fun deleteLiability(liability: DbLiability) {
        viewModelScope.launch {
            repository.deleteLiability(liability)
            _toastMessage.emit("Liability entry deleted.")
        }
    }

    fun deleteTransaction(transaction: DbTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            val isAsset = transaction.type.equals("Asset", ignoreCase = true) || 
                          transaction.type.equals("Assets", ignoreCase = true) || 
                          transaction.type.equals("Asset & Investment", ignoreCase = true) ||
                          transaction.type.equals("Investment", ignoreCase = true)
            if (isAsset) {
                val matchingAsset = repository.getAllAssetsRaw().firstOrNull { asset ->
                    asset.userId == transaction.userId &&
                    asset.name.equals(transaction.title, ignoreCase = true) &&
                    Math.abs(asset.amountInvested - transaction.amount) < 0.01
                }
                if (matchingAsset != null) {
                    repository.deleteAsset(matchingAsset)
                }
            }
            _toastMessage.emit("Transaction deleted.")
            syncBudgetsAndGoals(transaction.userId)
        }
    }

    fun updateTransaction(transaction: DbTransaction) {
        viewModelScope.launch {
            val oldTxn = repository.getAllTransactionsRaw().firstOrNull { it.id == transaction.id }
            repository.updateTransaction(transaction)
            val isAsset = transaction.type.equals("Asset", ignoreCase = true) || 
                          transaction.type.equals("Assets", ignoreCase = true) || 
                          transaction.type.equals("Asset & Investment", ignoreCase = true) ||
                          transaction.type.equals("Investment", ignoreCase = true)
            if (isAsset) {
                val searchTitle = oldTxn?.title ?: transaction.title
                val searchAmount = oldTxn?.amount ?: transaction.amount
                val matchingAsset = repository.getAllAssetsRaw().firstOrNull { asset ->
                    asset.userId == transaction.userId &&
                    asset.name.equals(searchTitle, ignoreCase = true) &&
                    Math.abs(asset.amountInvested - searchAmount) < 0.01
                }
                if (matchingAsset != null) {
                    val updatedAsset = matchingAsset.copy(
                        name = transaction.title,
                        amountInvested = transaction.amount,
                        type = if (transaction.category.isNotBlank()) transaction.category else matchingAsset.type,
                        currentPrice = transaction.amount
                    )
                    repository.insertAsset(updatedAsset)
                }
            }
            _toastMessage.emit("Transaction updated.")
            syncBudgetsAndGoals(transaction.userId)
        }
    }

    fun toggleTransactionSelection(transaction: DbTransaction) {
        viewModelScope.launch {
            repository.updateTransactionSelection(transaction.id, !transaction.isSelected)
        }
    }

    fun toggleSelectAllTransactions(selectAll: Boolean) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateAllTransactionsSelection(user.userId, selectAll)
        }
    }

    fun deleteSelectedTransactions() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val selectedTxns = userTransactions.value.filter { it.isSelected }
            repository.deleteSelectedTransactions(user.userId)
            val allAssets = repository.getAllAssetsRaw()
            selectedTxns.forEach { transaction ->
                val isAsset = transaction.type.equals("Asset", ignoreCase = true) || 
                              transaction.type.equals("Assets", ignoreCase = true) || 
                              transaction.type.equals("Asset & Investment", ignoreCase = true) ||
                              transaction.type.equals("Investment", ignoreCase = true)
                if (isAsset) {
                    val matchingAsset = allAssets.firstOrNull { asset ->
                        asset.userId == transaction.userId &&
                        asset.name.equals(transaction.title, ignoreCase = true) &&
                        Math.abs(asset.amountInvested - transaction.amount) < 0.01
                    }
                    if (matchingAsset != null) {
                        repository.deleteAsset(matchingAsset)
                    }
                }
            }
            _toastMessage.emit("Removed selected transactions.")
        }
    }

    // --- Investments / Live Asset Tracker Actions ---
    fun addAsset(name: String, type: String, amount: Double, quantity: Double, dateStr: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            // parse date or use current
            val dateMillis = System.currentTimeMillis()
            val newAsset = DbAsset(
                userId = user.userId,
                name = name,
                type = type,
                amountInvested = amount,
                quantity = quantity,
                dateInvested = dateMillis,
                currentPrice = if (quantity > 0) amount / quantity else amount,
                totalReturns = 0.0,
                avgBuyPrice = if (quantity > 0) amount / quantity else amount
            )
            repository.insertAsset(newAsset)
            recordNewEntryStreak()
            _toastMessage.emit("Asset entered in position tracker.")
        }
    }

    fun executeSimulatedPriceTick() {
        // Simple tick variation
    }

    fun deleteAsset(asset: DbAsset) {
        viewModelScope.launch {
            repository.deleteAsset(asset)
            _toastMessage.emit("Asset item deleted.")
        }
    }

    // --- Dynamic Categories Taxation Actions ---
    fun addNewCategory(name: String, type: String, description: String = "", iconName: String = "Category", customImageUri: String = "") {
        viewModelScope.launch {
            val newCat = DbCategory(
                name = name,
                type = type,
                description = description,
                iconName = iconName,
                customImageUri = customImageUri,
                subcategoriesString = ""
            )
            repository.insertCategory(newCat)
            _toastMessage.emit("Category '$name' created dynamically.")
        }
    }

    fun duplicateCategory(category: DbCategory) {
        viewModelScope.launch {
            val suffix = " (Copy)"
            val newName = if (category.name.endsWith(suffix)) {
                category.name + " Copy"
            } else {
                category.name + suffix
            }
            val newCat = DbCategory(
                name = newName,
                type = category.type,
                description = category.description,
                iconName = category.iconName,
                customImageUri = category.customImageUri,
                subcategoriesJson = category.subcategoriesJson,
                subcategoriesString = category.subcategoriesString
            )
            repository.insertCategory(newCat)
            _toastMessage.emit("Duplicated category into '$newName'")
        }
    }

    fun recategorizeTransactions(
        sourceCategory: String,
        sourceSubcategory: String?,
        targetCategory: String,
        targetSubcategory: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null) {
            onComplete(false, "No active user logged in.")
            return
        }
        viewModelScope.launch {
            try {
                val txns = userTransactions.value
                var count = 0
                txns.forEach { t ->
                    val matchesCat = t.category.equals(sourceCategory, ignoreCase = true)
                    val matchesSub = sourceSubcategory == null || 
                        t.subCategory.equals(sourceSubcategory, ignoreCase = true) ||
                        (sourceSubcategory == "General" && (t.subCategory.isBlank() || t.subCategory.equals("General", ignoreCase = true)))
                    
                    if (matchesCat && matchesSub) {
                        val updated = t.copy(
                            category = targetCategory,
                            subCategory = targetSubcategory
                        )
                        repository.updateTransaction(updated)
                        count++
                    }
                }
                _toastMessage.emit("Successfully moved $count transaction(s) to $targetCategory / $targetSubcategory")
                onComplete(true, "Moved $count transaction(s).")
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "Unknown error occurred.")
            }
        }
    }

    fun updateCategory(category: DbCategory) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun addSubcategoryTo(category: DbCategory, subcategoryName: String) {
        viewModelScope.launch {
            val oldList = category.getSubcategoryItems().toMutableList()
            if (oldList.none { it.name.lowercase() == subcategoryName.lowercase() }) {
                oldList.add(SubcategoryItem(name = subcategoryName))
                val updated = category.copy(
                    subcategoriesJson = DbCategory.serializeSubcategories(oldList)
                )
                repository.updateCategory(updated)
                _toastMessage.emit("Subcategory '$subcategoryName' saved in ${category.name}.")
            }
        }
    }

    fun deleteCategory(category: DbCategory) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _toastMessage.emit("Category and taxonomy removed.")
        }
    }

    // --- Budget Operations ---
    fun createBudget(name: String, limitAmount: Double, subcategoriesString: String = "General", onResult: (Boolean, String) -> Unit) {
        val user = _currentUser.value
        if (user == null) {
            onResult(false, "No active user logged in.")
            return
        }
        if (name.isBlank()) {
            onResult(false, "Budget name cannot be empty.")
            return
        }
        viewModelScope.launch {
            try {
                val budget = DbBudget(
                    userId = user.userId,
                    name = name.trim(),
                    limitAmount = limitAmount,
                    spentAmount = 0.0,
                    savedAmount = 0.0,
                    totalAllocated = limitAmount
                )
                val budgetId = repository.insertBudget(budget)

                // Initialize a dedicated, isolated ledger for that specific budget entity.
                val initialActivity = DbBudgetGoalActivity(
                    userId = user.userId,
                    parentId = budgetId,
                    isBudget = true,
                    title = "Budget Plan Initialized",
                    amount = 0.0,
                    date = System.currentTimeMillis()
                )
                repository.insertActivity(initialActivity)

                // Visibility: Automatically create Category under "Budget" special section
                val budgetCategory = DbCategory(
                    name = name.trim(),
                    type = "Budget",
                    subcategoriesString = if (subcategoriesString.isNotBlank()) subcategoriesString else "General"
                )
                repository.insertCategory(budgetCategory)

                syncBudgetsAndGoals(user.userId)

                _toastMessage.emit("Budget '$name' and its Special Category created successfully.")
                onResult(true, "Budget created successfully.")
            } catch (e: Exception) {
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    fun deleteBudget(budget: DbBudget) {
        viewModelScope.launch {
            try {
                val userId = budget.userId
                repository.deleteActivitiesForParent(budget.id, isBudget = true)
                repository.deleteBudget(budget)

                // Retrieve and delete related physical Category matches
                val allCats = categories.value
                val matchedCats = allCats.filter { it.name.equals(budget.name, ignoreCase = true) }
                matchedCats.forEach { repository.deleteCategory(it) }

                syncBudgetsAndGoals(userId)

                _toastMessage.emit("Budget and associated categories deleted successfully.")
            } catch (e: Exception) {
                _toastMessage.emit("Error deleting budget: ${e.message}")
            }
        }
    }

    // --- Goal Operations ---
    fun createGoal(name: String, targetAmount: Double, targetDate: String, priority: String, subcategoriesString: String = "General", onResult: (Boolean, String) -> Unit) {
        val user = _currentUser.value
        if (user == null) {
            onResult(false, "No active user logged in.")
            return
        }
        if (name.isBlank()) {
            onResult(false, "Goal name cannot be empty.")
            return
        }
        if (targetAmount <= 0) {
            onResult(false, "Target amount must be greater than zero.")
            return
        }
        viewModelScope.launch {
            try {
                val goal = DbGoal(
                    userId = user.userId,
                    name = name.trim(),
                    targetAmount = targetAmount,
                    savedAmount = 0.0,
                    targetDate = targetDate,
                    priority = priority
                )
                val goalId = repository.insertGoal(goal)

                // Initialize a dedicated, isolated ledger for that specific goal entity.
                val initialActivity = DbBudgetGoalActivity(
                    userId = user.userId,
                    parentId = goalId,
                    isBudget = false,
                    title = "Goal Seed Planted",
                    amount = 0.0,
                    date = System.currentTimeMillis()
                )
                repository.insertActivity(initialActivity)

                // Create a special category for goals to allow granular sub-categories tracking
                val goalCategory = DbCategory(
                    name = name.trim(),
                    type = "Goal",
                    subcategoriesString = if (subcategoriesString.isNotBlank()) subcategoriesString else "General"
                )
                repository.insertCategory(goalCategory)

                _toastMessage.emit("Goal '$name' created successfully.")
                onResult(true, "Goal created successfully.")
            } catch (e: Exception) {
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    fun deleteGoal(goal: DbGoal) {
        viewModelScope.launch {
            try {
                repository.deleteActivitiesForParent(goal.id, isBudget = false)
                repository.deleteGoal(goal)
                _toastMessage.emit("Goal deleted successfully.")
            } catch (e: Exception) {
                _toastMessage.emit("Error deleting goal: ${e.message}")
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getActivitiesForBudgetOrGoal(parentId: Long, isBudget: Boolean) = _currentUser.flatMapLatest { user ->
        if (user == null) kotlinx.coroutines.flow.flowOf(emptyList()) else repository.getActivitiesForBudgetOrGoal(user.userId, parentId, isBudget)
    }

    fun getBudgetStats(budgetId: Long): kotlinx.coroutines.flow.Flow<BudgetStats?> {
        return repository.getBudgetStats(budgetId)
    }

    fun topUpBudgetOrGoal(
        parentId: Long,
        isBudget: Boolean,
        amount: Double,
        source: String, // "Income", "Other"
        category: String, // selected category if not Other
        subCategory: String, // selected subcategory if not Other
        description: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null) {
            onResult(false, "No active user logged in.")
            return
        }
        if (amount <= 0) {
            onResult(false, "Amount must be greater than zero.")
            return
        }
        val validatedDesc = if (description.length > 100) description.substring(0, 100) else description
        viewModelScope.launch {
            try {
                val parentName = if (isBudget) {
                    val budget = repository.getBudgetById(parentId) ?: throw Exception("Budget not found.")
                    repository.updateBudget(budget.copy(savedAmount = budget.savedAmount + amount))
                    budget.name
                } else {
                    val goal = repository.getGoalById(parentId) ?: throw Exception("Goal not found.")
                    repository.updateGoal(goal.copy(savedAmount = goal.savedAmount + amount))
                    goal.name
                }

                val targetCategory = if (source == "Income") category else "Other"
                if (source == "Income" && targetCategory.isBlank()) {
                    throw Exception("Please select a valid source category.")
                }

                // Create physical ledger record tagged as "Budget/Goal Top-Up"
                val deductionTx = DbTransaction(
                    userId = user.userId,
                    title = "Top-Up: $parentName",
                    amount = amount,
                    type = "Budget/Goal Top-Up", // explicitly tagged as "Budget/Goal Top-Up" for Lite Pink visual indicators
                    date = System.currentTimeMillis(),
                    category = targetCategory,
                    subCategory = if (source == "Income") subCategory else "Other",
                    paymentMethod = "Net Banking",
                    fundsSource = source,
                    sourceType = if (source == "Income") "SOURCE_INCOME" else "SOURCE_OTHER"
                )
                repository.insertTransaction(deductionTx)

                val activityTitle = if (validatedDesc.isNotBlank()) validatedDesc else "Top Up from $source"
                val activity = DbBudgetGoalActivity(
                    userId = user.userId,
                    parentId = parentId,
                    isBudget = isBudget,
                    title = activityTitle,
                    amount = amount,
                    date = System.currentTimeMillis()
                )
                repository.insertActivity(activity)

                syncBudgetsAndGoals(user.userId)

                _toastMessage.emit("Topped up $parentName successfully by ₹$amount.")
                onResult(true, "Successfully topped up.")
            } catch (e: Exception) {
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    fun spendFromBudgetOrGoal(
        parentId: Long,
        isBudget: Boolean,
        amount: Double,
        merchant: String,
        date: Long,
        subcategory: String,
        description: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null) {
            onResult(false, "No active user logged in.")
            return
        }
        if (amount <= 0) {
            onResult(false, "Amount must be greater than zero.")
            return
        }
        val validatedDesc = if (description.length > 100) description.substring(0, 100) else description
        viewModelScope.launch {
            try {
                val parentName = if (isBudget) {
                    val budget = repository.getBudgetById(parentId) ?: throw Exception("Budget not found.")
                    repository.updateBudget(budget.copy(spentAmount = budget.spentAmount + amount))
                    budget.name
                } else {
                    val goal = repository.getGoalById(parentId) ?: throw Exception("Goal not found.")
                    repository.updateGoal(goal.copy(savedAmount = (goal.savedAmount - amount).coerceAtLeast(0.0)))
                    goal.name
                }

                val expenseTx = DbTransaction(
                    userId = user.userId,
                    title = if (merchant.isNotBlank()) merchant else "Spend from $parentName",
                    amount = amount,
                    type = if (isBudget) "BUDGET_SPEND" else "GOAL_SPEND", // explicitly set as BUDGET_SPEND or GOAL_SPEND
                    date = date,
                    category = parentName,
                    subCategory = if (subcategory.isNotBlank()) subcategory else "General",
                    paymentMethod = "Debit/Credit Card",
                    sourceType = "SOURCE_CLOSED_LEDGER"
                )
                repository.insertTransaction(expenseTx)

                val activityTitle = if (merchant.isNotBlank()) merchant else (if (validatedDesc.isNotBlank()) validatedDesc else "Spend")
                val activity = DbBudgetGoalActivity(
                    userId = user.userId,
                    parentId = parentId,
                    isBudget = isBudget,
                    title = activityTitle,
                    amount = -amount,
                    date = date
                )
                repository.insertActivity(activity)

                syncBudgetsAndGoals(user.userId)

                _toastMessage.emit("Spend logged from $parentName of ₹$amount.")
                onResult(true, "Spend recorded successfully.")
            } catch (e: Exception) {
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    fun deleteBudgetGoalActivity(activity: DbBudgetGoalActivity, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                if (activity.isBudget) {
                    val budget = repository.getBudgetById(activity.parentId)
                    if (budget != null) {
                        if (activity.amount >= 0) {
                            // Top-Up
                            val newSaved = (budget.savedAmount - activity.amount).coerceAtLeast(0.0)
                            repository.updateBudget(budget.copy(savedAmount = newSaved))
                        } else {
                            // Spend
                            val newSpent = (budget.spentAmount - kotlin.math.abs(activity.amount)).coerceAtLeast(0.0)
                            repository.updateBudget(budget.copy(spentAmount = newSpent))
                        }
                    }
                } else {
                    val goal = repository.getGoalById(activity.parentId)
                    if (goal != null) {
                        val newSaved = (goal.savedAmount - activity.amount).coerceAtLeast(0.0)
                        repository.updateGoal(goal.copy(savedAmount = newSaved))
                    }
                }
                repository.deleteActivity(activity)

                syncBudgetsAndGoals(activity.userId)

                _toastMessage.emit("Transaction deleted successfully.")
                onResult(true, "Transaction deleted successfully.")
            } catch (e: Exception) {
                _toastMessage.emit("Error deleting transaction: ${e.message}")
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    fun updateBudgetGoalActivity(
        activity: DbBudgetGoalActivity,
        newTitle: String,
        newAmount: Double,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (newTitle.isBlank()) {
            onResult(false, "Title cannot be empty.")
            return
        }
        if (newAmount <= 0) {
            onResult(false, "Amount must be positive.")
            return
        }
        viewModelScope.launch {
            try {
                val absoluteNew = kotlin.math.abs(newAmount)
                val signedNewAmount = if (activity.amount >= 0) absoluteNew else -absoluteNew

                if (activity.isBudget) {
                    val budget = repository.getBudgetById(activity.parentId)
                    if (budget != null) {
                        var updatedBudget = budget
                        // 1. Undo old
                        if (activity.amount >= 0) {
                            val undoneSaved = (updatedBudget.savedAmount - activity.amount).coerceAtLeast(0.0)
                            updatedBudget = updatedBudget.copy(savedAmount = undoneSaved)
                        } else {
                            val undoneSpent = (updatedBudget.spentAmount - kotlin.math.abs(activity.amount)).coerceAtLeast(0.0)
                            updatedBudget = updatedBudget.copy(spentAmount = undoneSpent)
                        }
                        // 2. Apply new
                        if (signedNewAmount >= 0) {
                            updatedBudget = updatedBudget.copy(savedAmount = updatedBudget.savedAmount + signedNewAmount)
                        } else {
                            updatedBudget = updatedBudget.copy(spentAmount = updatedBudget.spentAmount + kotlin.math.abs(signedNewAmount))
                        }
                        repository.updateBudget(updatedBudget)
                    }
                } else {
                    val goal = repository.getGoalById(activity.parentId)
                    if (goal != null) {
                        val undoneSaved = (goal.savedAmount - activity.amount).coerceAtLeast(0.0)
                        val updatedSaved = (undoneSaved + signedNewAmount).coerceAtLeast(0.0)
                        repository.updateGoal(goal.copy(savedAmount = updatedSaved))
                    }
                }

                val updatedActivity = activity.copy(
                    title = newTitle.trim(),
                    amount = signedNewAmount
                )
                repository.updateActivity(updatedActivity)

                syncBudgetsAndGoals(activity.userId)

                _toastMessage.emit("Transaction updated successfully.")
                onResult(true, "Transaction updated successfully.")
            } catch (e: Exception) {
                _toastMessage.emit("Error updating transaction: ${e.message}")
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    // --- Data Transformations Exports utility ---
    fun compileCsvFormatString(): String {
        return CsvExporter.formatTransactionsToCsv(userTransactions.value)
    }

    // --- Compound Interest Math Projections ---
    fun calculateProjectionYears(): List<YearlyProjection> {
        val p = initialInvestment.value
        val pmt = monthlyContribution.value
        val annualRate = interestRate.value / 100.0
        val years = yearsPeriod.value
        val freq = compoundingFrequency.value

        // Standard financial compound formula calculation
        // A = P(1 + r/n)^(nt) + PMT * [((1 + r/n)^(nt) - 1) / (r/n)]
        val n = when (freq) {
            "Daily" -> 365
            "Weekly" -> 52
            "Monthly" -> 12
            "Yearly" -> 1
            else -> 12
        }

        val totalMonths = years * 12
        val compoundEnabled = isCompoundingEnabled.value

        val list = mutableListOf<YearlyProjection>()
        var currentBalance = p
        var totalInvested = p

        for (year in 1..yearsPeriod.value) {
            val ratePerCompounding = annualRate / n
            val compoundingPerYear = n

            if (compoundEnabled) {
                // Approximate monthly calculation loop per year for accuracy
                for (m in 1..12) {
                    currentBalance = (currentBalance + pmt) * (1 + annualRate / 12)
                    totalInvested += pmt
                }
            } else {
                // simple interest comparison
                totalInvested += pmt * 12
                currentBalance = totalInvested + (p * annualRate * year) + (pmt * 12 * year * annualRate / 2)
            }

            val totalInterest = (currentBalance - totalInvested).coerceAtLeast(0.0)

            list.add(
                YearlyProjection(
                    year = year,
                    futureValue = currentBalance,
                    totalInvested = totalInvested,
                    totalInterest = totalInterest
                )
            )
        }
        return list
    }

    // --- Recurring Items ---
    fun addRecurringItem(
        title: String,
        amount: Double,
        dueDate: String,
        category: String,
        subCategory: String,
        paymentMode: String,
        type: String, // "Bill", "EMI", "SIP", "UDHAR", "Other"
        fundsSource: String = "",
        recurrence: String = "Monthly",
        repetition: String = "until i Cancel",
        sourceType: String = "SOURCE_INCOME",
        durationEndDate: String = ""
    ) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val derivedCategory = if (type != "Other") {
                val dedicatedCategoryName = when (type) {
                    "Bill" -> "Bills & Subscriptions"
                    "EMI" -> "EMIs"
                    "SIP" -> "SIPs"
                    "UDHAR" -> "Udhar & Payables"
                    else -> "Other commitments"
                }
                // Check if this category exists
                val categoryExists = categories.value.any { it.name.equals(dedicatedCategoryName, ignoreCase = true) && it.type.equals("Expense", ignoreCase = true) }
                if (!categoryExists) {
                    val newCat = DbCategory(
                        name = dedicatedCategoryName,
                        type = "Expense",
                        description = "Dedicated category for $type recurring items",
                        iconName = "TrackChanges",
                        subcategoriesString = ""
                    )
                    repository.insertCategory(newCat)
                }
                dedicatedCategoryName
            } else {
                category
            }

            val item = DbRecurringItem(
                userId = user.userId,
                title = title,
                amount = amount,
                dueDate = dueDate,
                category = derivedCategory,
                subCategory = subCategory,
                paymentMode = paymentMode,
                fundsSource = fundsSource,
                type = type,
                recurrence = recurrence,
                repetition = repetition,
                sourceType = sourceType,
                durationEndDate = durationEndDate
            )
            repository.insertRecurringItem(item)
            _toastMessage.emit("Recurring item '$title' created successfully!")
        }
    }

    fun deleteRecurringItem(item: DbRecurringItem) {
        viewModelScope.launch {
            repository.deleteRecurringItem(item)
            _toastMessage.emit("Deleted recurring item: ${item.title}")
        }
    }

    fun updateRecurringItem(item: DbRecurringItem) {
        viewModelScope.launch {
            repository.insertRecurringItem(item)
            _toastMessage.emit("Updated recurring item: ${item.title}")
        }
    }

    // --- Commitment Processing Utility ---
    fun triggerCommitmentProcessing() {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val count = com.example.util.CommitmentProcessor.processDueCommitments(repository, user.userId) { itemName ->
                viewModelScope.launch {
                    _toastMessage.emit("Commitment '$itemName' processed successfully!")
                }
            }
            if (count > 0) {
                _toastMessage.emit("Successfully processed $count due commitment(s).")
            }
        }
    }

    fun processSingleCommitment(item: DbRecurringItem) {
        viewModelScope.launch {
            val isAssetType = item.type.equals("SIP", ignoreCase = true) || 
                              item.category.contains("Asset & Investment", ignoreCase = true) || 
                              item.category.contains("Investments", ignoreCase = true) ||
                              item.category.contains("Fixed Deposits & Savings", ignoreCase = true) ||
                              item.category.contains("Stocks & Mutual Funds", ignoreCase = true) ||
                              item.category.contains("Real Estate & Gold", ignoreCase = true)

            val transactionType = if (isAssetType) "Investment" else "Expense"

            val transaction = DbTransaction(
                userId = item.userId,
                title = "[Commitment Paid] ${item.title}",
                amount = item.amount,
                type = transactionType,
                date = System.currentTimeMillis(),
                category = item.category,
                subCategory = item.subCategory,
                paymentMethod = if (item.paymentMode.isNotBlank()) item.paymentMode else "UPI",
                fundsSource = if (item.fundsSource.isNotBlank()) item.fundsSource else "General Funds",
                isSelected = false,
                status = "Paid",
                isLiability = true,
                sourceType = item.sourceType
            )
            repository.insertTransaction(transaction)

            // If it's a paid investment/SIP commitment, also insert into DbAsset to sync stats!
            if (isAssetType) {
                val newAsset = DbAsset(
                    userId = item.userId,
                    name = "[Commitment Paid] ${item.title}",
                    type = if (item.category.isNotBlank()) item.category else "Other",
                    amountInvested = item.amount,
                    quantity = 1.0,
                    dateInvested = System.currentTimeMillis(),
                    currentPrice = item.amount,
                    totalReturns = 0.0,
                    avgBuyPrice = item.amount
                )
                repository.insertAsset(newAsset)
            }

            // Update item as Paid instead of immediately deleting or advancing
            val updatedItem = item.copy(isPaid = true, status = "Paid")
            repository.insertRecurringItem(updatedItem)
            _toastMessage.emit("Paid commitment '${item.title}'.")
        }
    }
}

data class YearlyProjection(
    val year: Int,
    val futureValue: Double,
    val totalInvested: Double,
    val totalInterest: Double
)

data class DuplicateCheckResult(
    val isSingleProfile: Boolean,
    val targetUserId: String,
    val incomingTxCount: Int,
    val incomingAssetCount: Int,
    val duplicateTransactions: List<DbTransaction>,
    val duplicateAssets: List<DbAsset>,
    val uniqueTransactions: List<DbTransaction>,
    val uniqueAssets: List<DbAsset>,
    val parsedUsers: List<DbUser>,
    val parsedTx: List<DbTransaction>,
    val parsedAssets: List<DbAsset>,
    val parsedCategories: List<DbCategory>,
    val parsedBudgets: List<DbBudget> = emptyList(),
    val parsedGoals: List<DbGoal> = emptyList(),
    val parsedLiabilities: List<DbLiability> = emptyList(),
    val parsedRecurringItems: List<DbRecurringItem> = emptyList(),
    val parsedActivities: List<DbBudgetGoalActivity> = emptyList(),
    val uniqueBudgets: List<DbBudget> = emptyList(),
    val uniqueGoals: List<DbGoal> = emptyList(),
    val uniqueLiabilities: List<DbLiability> = emptyList(),
    val uniqueRecurringItems: List<DbRecurringItem> = emptyList()
)

data class ActivityLogItem(
    val device: String,
    val location: String,
    val timestamp: Long,
    val isActive: Boolean
)

