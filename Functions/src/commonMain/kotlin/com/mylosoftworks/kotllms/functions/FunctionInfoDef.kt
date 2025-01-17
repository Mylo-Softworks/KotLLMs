package com.mylosoftworks.kotllms.functions

abstract class FunctionInfoDef {
    abstract fun getInfoForAllFunctions(functionDefs: FunctionDefs): String
    abstract fun getInfoForFunction(functionDefinition: FunctionDefinition<*>): String
    abstract fun getInfoForParameter(functionParameter: FunctionParameter<*>): String
}

/**
 * Pseudo-json format containing available function calls
 */
object DefaultFunctionInfoDef : FunctionInfoDef() {
    override fun getInfoForAllFunctions(functionDefs: FunctionDefs): String {
        return functionDefs.functions.values.joinToString(",", "{", "}") {
            getInfoForFunction(it)
        }
    }

    override fun getInfoForFunction(functionDefinition: FunctionDefinition<*>): String {
        val name = functionDefinition.name
        val comment = functionDefinition.comment
        val params = functionDefinition.params
        return "$name: {${if (comment != null) "comment: \"$comment\", " else ""}parameters: ${params.values.joinToString(", ", "[", "]") { getInfoForParameter(it) }}}"
    }

    override fun getInfoForParameter(functionParameter: FunctionParameter<*>): String {
        val name = functionParameter.name
        val comment = functionParameter.comment
        val typeName = functionParameter.typeName
        return "{$name: {${if (comment != null) "comment: \"$comment\"," else ""}type: $typeName}}"
    }

}