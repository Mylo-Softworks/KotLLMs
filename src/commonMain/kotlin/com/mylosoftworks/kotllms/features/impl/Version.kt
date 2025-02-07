package com.mylosoftworks.kotllms.features.impl

import kotlinx.serialization.Serializable

interface Version<V : VersionInfo> {
    suspend fun version(): Result<V>
}

@Serializable
abstract class VersionInfo() {
    abstract val versionNumber: String
}