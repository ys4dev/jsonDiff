package com.example.domain

import com.fasterxml.jackson.databind.JsonNode

data class Diff(val state: DiffState, val value: JsonNode)

/**
 *
 */
sealed class DiffTree {
    abstract val left: Diff
    abstract val right: Diff
}

data class DiffNode(
        val namedValues: List<Pair<String, DiffTree>>,
        val indexedValues: List<DiffTree>,
        override val left: Diff,
        override val right: Diff
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
        override val left: Diff,
        override val right: Diff
) : DiffTree() {

}

enum class DiffState {
    Same,
    Different,
    ContainsDifferent,
    Missing
}