package com.wire.com.wire.model.legalhold

import com.wire.com.wire.model.PreKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InitResponse(
    @SerialName("last_prekey")
    val lastPrekey: PreKey,
    val prekeys: List<PreKey>,
    val fingerprint: String
)
