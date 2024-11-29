package com.wire.com.wire.model.backend

import com.wire.com.wire.model.PreKey
import kotlinx.serialization.Serializable

@Serializable
data class ClientAddRequest(
    val type: String = "permanent",
    val lastkey: PreKey,
    val prekeys: List<PreKey>,
)
