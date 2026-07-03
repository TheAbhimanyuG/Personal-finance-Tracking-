package com.example.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.data.db.AppDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
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

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("kinetic_backup_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("auto_backup_enabled", true)
        if (!isEnabled) {
            return Result.success()
        }
        
        val frequency = prefs.getString("auto_backup_frequency", "Daily") ?: "Daily"
        val lastBackupTime = prefs.getLong("last_backup_timestamp", 0L)
        val now = System.currentTimeMillis()
        
        var shouldExecute = false
        if (lastBackupTime == 0L) {
            shouldExecute = true
        } else {
            val diffMs = now - lastBackupTime
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
            when (frequency) {
                "Daily" -> shouldExecute = true
                "Weekly" -> if (diffDays >= 7) shouldExecute = true
                "Monthly" -> if (diffDays >= 30) shouldExecute = true
            }
        }
        
        if (shouldExecute) {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val dao = db.financialDao
                
                val rootJson = org.json.JSONObject()
                rootJson.put("backup_timestamp", System.currentTimeMillis())
                rootJson.put("type", "all_profiles")
                
                val allUsers = dao.getAllUsersRaw()
                val allTx = dao.getAllTransactionsRaw()
                val allAssets = dao.getAllAssetsRaw()
                val allCats = dao.getAllCategoriesRaw()
                val allBudgets = dao.getAllBudgetsRaw()
                val allGoals = dao.getAllGoalsRaw()
                val allLiabilities = dao.getAllLiabilitiesRaw()
                val allRecurring = dao.getAllRecurringItemsRaw()
                val allActivities = dao.getAllActivitiesRaw()
                
                // Users list
                val userArray = org.json.JSONArray()
                val sharedPrefs = applicationContext.getSharedPreferences("kinetic_auth_prefs", Context.MODE_PRIVATE)
                allUsers.forEach { u ->
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

                // Categories list
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

                // Transactions
                val txnArray = org.json.JSONArray()
                allTx.forEach { t ->
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

                // Assets
                val assetArray = org.json.JSONArray()
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
                rootJson.put("assets", assetArray)

                // Budgets
                val budgetArray = org.json.JSONArray()
                allBudgets.forEach { b ->
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
                rootJson.put("budgets", budgetArray)

                // Goals
                val goalArray = org.json.JSONArray()
                allGoals.forEach { g ->
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
                rootJson.put("goals", goalArray)

                // Liabilities
                val liabilityArray = org.json.JSONArray()
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
                rootJson.put("liabilities", liabilityArray)

                // Recurring Items
                val recArray = org.json.JSONArray()
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
                rootJson.put("recurring_items", recArray)

                // Activities
                val actArray = org.json.JSONArray()
                allActivities.forEach { a ->
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
                rootJson.put("activities", actArray)

                // Validation Hash
                rootJson.remove("backup_hash")
                val compactStr = rootJson.toString()
                val checksumValue = calculateHash(compactStr)
                rootJson.put("backup_hash", checksumValue)

                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
                val newFileName = "FullBackup_${todayStr}.snapshot"
                val backupDir = applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir
                val targetFile = File(backupDir, newFileName)
                targetFile.writeText(rootJson.toString(4))
                
                prefs.edit().apply {
                    putLong("last_backup_timestamp", now)
                    putString("last_backup_filename", targetFile.name)
                    apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return Result.retry()
            }
        }
        
        return Result.success()
    }
    
    companion object {
        fun scheduleDailyBackup(context: Context) {
            val calendar = Calendar.getInstance()
            val nowCalendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            if (calendar.before(nowCalendar)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            val initialDelay = calendar.timeInMillis - nowCalendar.timeInMillis
            
            val backupWorkRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "AutoBackupJob",
                ExistingPeriodicWorkPolicy.KEEP,
                backupWorkRequest
            )
        }
    }
}
