package com.example.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object GlobalTransactionEventBus {
    private val _events = MutableSharedFlow<GlobalTransactionEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    fun post(event: GlobalTransactionEvent) {
        _events.tryEmit(event)
    }
}
