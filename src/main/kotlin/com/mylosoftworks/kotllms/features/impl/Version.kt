package com.mylosoftworks.kotllms.features.impl

import com.mylosoftworks.kotllms.features.Feature
import kotlinx.serialization.Serializable

interface Version<V : VersionInfo> : Feature {
    suspend fun version(): V
}

@Serializable
abstract class VersionInfo() {
    abstract val versionNumber: String
}