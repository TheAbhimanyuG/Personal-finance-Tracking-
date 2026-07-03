package com.example.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.model.DbAsset
import com.example.data.model.DbCategory
import com.example.data.model.DbTransaction
import com.example.data.model.DbUser
import com.example.data.model.DbBudget
import com.example.data.model.DbGoal
import com.example.data.model.DbBudgetGoalActivity
import com.example.data.model.DbLiability
import com.example.data.model.DbRecurringItem

@Database(
    entities = [
        DbUser::class,
        DbTransaction::class,
        DbAsset::class,
        DbCategory::class,
        DbBudget::class,
        DbGoal::class,
        DbBudgetGoalActivity::class,
        DbLiability::class,
        DbRecurringItem::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val financialDao: FinancialDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kinetic_trust_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
