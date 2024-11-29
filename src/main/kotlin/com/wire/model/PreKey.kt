package com.wire.com.wire.model

import kotlinx.serialization.Serializable

@Serializable
data class PreKey(val id: Int, val key: String)