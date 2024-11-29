package com.wire.com.wire.model

import kotlinx.serialization.Serializable

@Serializable
data class QualifiedId(val id: String, val domain: String)