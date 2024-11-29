package com.wire.com.wire.model.legalhold

import com.wire.com.wire.model.QualifiedId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfirmRequest(
    @SerialName("qualified_user_id") val userId: QualifiedId,
    @SerialName("team_id") val teamId: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("client_id") val clientId: String,
)
