package com.mylosoftworks.kotllms.features.impl

import com.mylosoftworks.kotllms.features.Feature

interface ListModels<D : ListedModelDef> : Feature {
    suspend fun listModels(): List<D>
}

open class ListedModelDef(val modelName: String)

interface GetCurrentModel<D : ListedModelDef> : Feature {
    suspend fun getCurrentModel(): D
}