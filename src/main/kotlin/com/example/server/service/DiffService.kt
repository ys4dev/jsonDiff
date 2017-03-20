package com.example.server.service

import com.example.controller.indices
import com.example.domain.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.MissingNode
import org.springframework.stereotype.Service

/**
 *
 */
interface DiffService {

    fun diff(node1: JsonNode, node2: JsonNode, name: String = ""): DiffTree
}

@Service
class DiffServiceImpl : DiffService {

    override fun diff(node1: JsonNode, node2: JsonNode, name: String): DiffTree {
        val indices = node1.indices() + node2.indices()
        if (indices.isEmpty()) {
            val (state1, state2) = when {
                node1.toString() == node2.toString() -> Pair(DiffState.Same, DiffState.Same)
                node1 is MissingNode -> Pair(DiffState.Missing, DiffState.Different)
                node2 is MissingNode -> Pair(DiffState.Different, DiffState.Missing)
                else -> Pair(DiffState.Different, DiffState.Different)
            }
            return DiffValue(Diff(state1, node1), Diff(state2, node2), name)
        }

        val nameds: MutableList<Pair<String, DiffTree>> = mutableListOf()
        indices.stringKeys.forEach { key ->
            val value1 = node1[key] ?: MissingNode.getInstance()
            val value2 = node2[key] ?: MissingNode.getInstance()
            nameds.add(Pair(key, diff(value1, value2, key)))
        }

        val indexed = mutableListOf<DiffTree>()
        indices.intKeys.forEach { index ->
            val value1 = node1[index] ?: MissingNode.getInstance()
            val value2 = node2[index] ?: MissingNode.getInstance()
            indexed.add(diff(value1, value2, index.toString()))
        }
        val (statel, stater) =
                if (node1 is MissingNode) {
                    Pair(DiffState.Missing, DiffState.Different)
                } else if (node2 is MissingNode) {
                    Pair(DiffState.Different, DiffState.Missing)
                } else {
                    val containsDiff = (nameds.flatMap { listOf(it.second.left, it.second.right) } + indexed.flatMap { listOf(it.left, it.right) }).any { it.state != DiffState.Same }
                    if (containsDiff) Pair(DiffState.ContainsDifferent, DiffState.ContainsDifferent)
                    else Pair(DiffState.Same, DiffState.Same)
                }
        return DiffNode(nameds, indexed, Diff(statel, node1), Diff(stater, node2), name)
    }
}
