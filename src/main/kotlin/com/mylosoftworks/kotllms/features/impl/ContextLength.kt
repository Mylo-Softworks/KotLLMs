package com.mylosoftworks.kotllms.features.impl

import com.mylosoftworks.kotllms.features.Feature

interface ContextLength : Feature {
    suspend fun contextLength(): Int
}