package com.example.server.service

import com.example.domain.DiffState
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 *
 */
class DiffTest {

    lateinit var diff: DiffService
    lateinit var mapper: ObjectMapper

    @Before
    fun setup() {
        diff = DiffServiceImpl()
        mapper = ObjectMapper().registerKotlinModule()
    }

    @Test
    fun sameInt() {
        val str1 = """{"a":0}"""
        val node1 = mapper.readTree(str1)
        val node2 = mapper.readTree(str1)
        val result = diff.diff(node1, node2)
        assertEquals(DiffState.Same, result.left.state)
        assertEquals(DiffState.Same, result.right.state)
    }

    @Test
    fun diffInt() {
        val node1 = IntNode(1)
        val node2 = IntNode(2)
        val result = diff.diff(node1, node2)
        assertEquals(DiffState.Different, result.left.state)
        assertEquals(DiffState.Different, result.right.state)
    }
}