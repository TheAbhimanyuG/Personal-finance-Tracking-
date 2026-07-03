package com.example.util

import com.example.data.model.DbTransaction

sealed class GlobalTransactionEvent {
    data class TransactionAdded(val transaction: DbTransaction) : GlobalTransactionEvent()
    data class TransactionUpdated(val transaction: DbTransaction) : GlobalTransactionEvent()
    data class TransactionDeleted(val transaction: DbTransaction) : GlobalTransactionEvent()
    object TransactionsRefreshed : GlobalTransactionEvent()
}
