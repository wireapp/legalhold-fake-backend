package com.wire.com.wire.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiVersion(val supported: List<Int>)
