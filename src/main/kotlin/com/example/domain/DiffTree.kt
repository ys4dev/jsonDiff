package com.example.domain

import com.fasterxml.jackson.databind.JsonNode

/**
 *
 */
sealed class DiffTree {
    abstract val state: DiffState
    abstract val left: JsonNode
    abstract val right: JsonNode
}

data class DiffNode(
        override val state: DiffState,
        val namedValues: List<Pair<String, DiffTree>>,
        val indexedValues: List<DiffTree>,
        override val left: JsonNode,
        override val right: JsonNode
) : DiffTree() {
    fun keys(): List<String> = namedValues.map { it.first }
    fun type(): String {
        if (namedValues.isNotEmpty() && indexedValues.isEmpty()) {
            return "object"
        } else if (namedValues.isEmpty() && indexedValues.isNotEmpty()) {
            return "array"
        } else {
            return "complex"
        }
    }
}

data class DiffValue(
        override val state: DiffState,
        override val left: JsonNode,
        override val right: JsonNode
) : DiffTree() {

}

enum class DiffState {
    Same,
    Different,
    ContainsDifferent
}