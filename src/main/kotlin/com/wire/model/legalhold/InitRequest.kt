package com.wire.com.wire.model.legalhold

import com.wire.com.wire.model.QualifiedId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InitRequest(@SerialName("qualified_user_id") val userId: QualifiedId, @SerialName("team_id") val teamId: String)
