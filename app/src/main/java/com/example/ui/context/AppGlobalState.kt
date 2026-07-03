package com.example.ui.context

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import com.example.data.model.*
import com.example.ui.AppViewModel

/**
 * Global App Context representing the synchronized reactive state of the application.
 */
data class AppGlobalContext(
    val transactions: List<DbTransaction> = emptyList(),
    val categories: List<DbCategory> = emptyList(),
    val assets: List<DbAsset> = emptyList(),
    val budgets: List<DbBudget> = emptyList(),
    val liabilities: List<DbLiability> = emptyList(),
    val recurringItems: List<DbRecurringItem> = emptyList(),
    val currencyUnit: String = "Rupee (₹)",
    val currencyPrefix: String = "₹",
    val addTransaction: (String, Double, String, String, String, String) -> Unit = { _, _, _, _, _, _ -> },
    val deleteTransaction: (DbTransaction) -> Unit = {},
    val updateTransaction: (DbTransaction) -> Unit = {}
)

val LocalAppGlobalContext = compositionLocalOf { AppGlobalContext() }

/**
 * A handy hook-like property/getter to retrieve the global context state directly inside any Composable.
 */
val appGlobalContext: AppGlobalContext
    @Composable
    @ReadOnlyComposable
    get() = LocalAppGlobalContext.current

/**
 * Context Provider that gathers the core Flow bindings from the AppViewModel and pumps
 * them down the Compose tree as a unified context, forcing synchronous instant updates everywhere.
 */
@Composable
fun AppGlobalStateProvider(
    viewModel: AppViewModel,
    content: @Composable () -> Unit
) {
    val txns by viewModel.userTransactions.collectAsState()
    val cats by viewModel.categories.collectAsState()
    val rawAssets by viewModel.userAssets.collectAsState()
    val bgs by viewModel.userBudgets.collectAsState()
    val liabs by viewModel.userLiabilities.collectAsState()
    val recs by viewModel.userRecurringItems.collectAsState()
    val currencyUnit by viewModel.selectedCurrency.collectAsState()

    val currencyPrefix = when (currencyUnit) {
        "Dollar ($)" -> "$"
        "Euro (€)" -> "€"
        else -> "₹"
    }

    val contextValue = AppGlobalContext(
        transactions = txns,
        categories = cats,
        assets = rawAssets,
        budgets = bgs,
        liabilities = liabs,
        recurringItems = recs,
        currencyUnit = currencyUnit,
        currencyPrefix = currencyPrefix,
        addTransaction = { title, amt, type, cat, sub, pay ->
            viewModel.addTransaction(
                title = title,
                amount = amt,
                type = type,
                category = cat,
                subCategory = sub,
                payment = pay
            )
        },
        deleteTransaction = { txn ->
            viewModel.deleteTransaction(txn)
        },
        updateTransaction = { txn ->
            viewModel.updateTransaction(txn)
        }
    )

    CompositionLocalProvider(LocalAppGlobalContext provides contextValue) {
        content()
    }
}
