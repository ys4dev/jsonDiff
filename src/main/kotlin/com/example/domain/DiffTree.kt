package com.example.domain

import com.fasterxml.jackson.databind.JsonNode

data class Diff(val state: DiffState, val value: JsonNode)

/**
 *
 */
sealed class DiffTree {
    abstract val left: Diff
    abstract val right: Diff
    abstract val name: String
    abstract val type: String

    abstract fun filter(p: (DiffTree) -> Boolean): DiffTree?
}

data class DiffNode(
        val namedValues: List<Pair<String, DiffTree>>,
        val indexedValues: List<DiffTree>,
        override val left: Diff,
        override val right: Diff,
        override val name: String
) : DiffTree() {
    override val type: String
        get() {
        if (namedValues.isNotEmpty() && indexedValues.isEmpty()) {
            return "object"
        } else if (namedValues.isEmpty() && indexedValues.isNotEmpty()) {
            return "array"
        } else {
            return "complex"
        }
    }

    override fun filter(p: (DiffTree) -> Boolean): DiffTree? {
        val namedFiltered = namedValues.map { (k, v) ->
            Pair(k, v.filter(p))
        }.filterMap({ (k, v) -> v != null }, { (k, v) -> Pair(k, v!!) })
        val indexedFiltered = indexedValues.map{ it.filter(p) }.nonnull()
        if (namedFiltered.isEmpty() && indexedFiltered.isEmpty()) {
            if (! p(this)) {
                return null
            } else {
                return DiffValue(left, right, this.name, this.type)
            }
        } else {
            return DiffNode(namedFiltered, indexedFiltered, left, right, this.name)
        }
    }
}

data class DiffValue(
        override val left: Diff,
        override val right: Diff,
        override val name: String,
        override val type: String = ""
) : DiffTree() {
    override fun filter(p: (DiffTree) -> Boolean): DiffTree? {
        if (p(this)) {
            return this
        } else {
            return null
        }
    }
}

fun <T> List<T?>.nonnull(): List<T> {
    return this.filterMap({it != null}, { it!! })
}

fun <T, U> List<T>.filterMap(p: (T) -> Boolean, f: (T) -> U) : List<U> {
    return this.filter(p).map(f)
}

fun <T> ((T) -> Boolean).nullAsFalse(): (T?) -> Boolean {
    return {
        if (it == null) false else this(it)
    }
}

enum class DiffState {
    Same,
    Different,
    ContainsDifferent,
    Missing
}