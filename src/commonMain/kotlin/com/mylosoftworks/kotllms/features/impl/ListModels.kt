package com.mylosoftworks.kotllms.features.impl

interface ListModels<D : ListedModelDef> {
    suspend fun listModels(): Result<List<D>>
}

open class ListedModelDef(val modelName: String)

interface GetCurrentModel<D : ListedModelDef> {
    suspend fun getCurrentModel(): Result<D>
}