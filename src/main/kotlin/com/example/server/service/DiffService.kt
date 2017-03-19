package com.example.server.service

import com.example.domain.DiffTree
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Service

typealias DiffResult = DiffTree

/**
 *
 */
interface DiffService {

    fun diff(node1: JsonNode, node2: JsonNode): DiffResult
}

@Service
class DiffServiceImpl : DiffService {

    override fun diff(node1: JsonNode, node2: JsonNode): DiffResult {
        if (node1 is ObjectNode && node2 is ObjectNode) {
            return diff(node1, node2)
        } else if (node1 is ArrayNode && node2 is ArrayNode) {

        } else if (node1.nodeType == node2.nodeType) {

        } else {

        }
        TODO()
    }

    fun diff(node1: ObjectNode, node2: ObjectNode): DiffResult {
        val keys: List<String> = fromIterator(node1.fieldNames()) + fromIterator(node1.fieldNames())
        val results = keys.map { key ->
            val value1 = node1[key]
            val value2 = node2[key]

            if (value1.nodeType != value2.nodeType) {
                Pair(key, typemismatch(value1, value2))
            } else if (isContainer(value1)) {
//                Pair(key, )
            } else if (equalsValue(value1, value2)) {
                Pair(key, equalValue(value1))
            } else {
//                Pair()
            }
        }
        TODO()
    }

    private fun equalValue(value: JsonNode): DiffResult {
        TODO()
    }

    private fun isContainer(node1: JsonNode): Boolean {
        return node1.nodeType == JsonNodeType.ARRAY || node1.nodeType == JsonNodeType.OBJECT
    }

    private fun equalsValue(value1: JsonNode, value2: JsonNode): Boolean {
        val nodeType1 = value1.nodeType
        val nodeType2 = value2.nodeType
        assert(nodeType1 == nodeType2)
        when (Pair(nodeType1, nodeType2)) {
            Pair(JsonNodeType.BOOLEAN, JsonNodeType.BOOLEAN) ->
                return value1.booleanValue() == value2.booleanValue()
            Pair(JsonNodeType.NUMBER, JsonNodeType.NUMBER) ->
                return value1.doubleValue() == value2.doubleValue()
        }
        return false
    }

    fun typemismatch(node1: JsonNode, node2: JsonNode): DiffResult {
        TODO()
    }

    fun <T> fromIterator(iterator: Iterator<T>): List<T> {
        val result = listOf<T>()
        iterator.forEach {
            result + it
        }
        return result
    }
}
