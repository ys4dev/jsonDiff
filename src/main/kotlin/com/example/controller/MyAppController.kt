package com.example.controller

import com.example.domain.DiffNode
import com.example.domain.DiffState
import com.example.domain.DiffTree
import com.example.domain.DiffValue
import com.example.server.service.DiffResult
import com.example.server.service.DiffService
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.TextField
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableColumn
import javafx.scene.control.TreeTableView
import javafx.scene.control.cell.TreeItemPropertyValueFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.URL
import java.util.*

/**
 *
 */

@Component
class MyAppController : Initializable {

    @Autowired
    lateinit private var diffService: DiffService

    @FXML
    lateinit private var text: TextField

    @FXML
    lateinit private var leftTree: TreeTableView<NameValue>

    @FXML
    lateinit private var leftNameColumn: TreeTableColumn<NameValue, String>

    @FXML
    lateinit private var leftValueColumn: TreeTableColumn<NameValue, String>

    @FXML
    lateinit private var rightTree: TreeTableView<NameValue>

    @FXML
    lateinit private var rightNameColumn: TreeTableColumn<NameValue, String>

    @FXML
    lateinit private var rightValueColumn: TreeTableColumn<NameValue, String>

    override fun initialize(location: URL, resources: ResourceBundle?) {
        leftNameColumn.cellValueFactory = TreeItemPropertyValueFactory("name")
        leftValueColumn.cellValueFactory = TreeItemPropertyValueFactory("value")

        rightNameColumn.cellValueFactory = TreeItemPropertyValueFactory("name")
        rightValueColumn.cellValueFactory = TreeItemPropertyValueFactory("value")

        val mapper = ObjectMapper().registerKotlinModule()
        val json1 = mapper.readTree("""{"a":0, "b":"c", "parent":{"child":"child"}, "array":[1,"2",3.0,null,true]}""")
        val json2 = mapper.readTree("""{"a":0}""")

        val root = toSame(json1, json2)//diffService.diff(json1, json2)
        val leftRootItem = toTreeItem("", root, { it.left })
        val rightRootItem = toTreeItem("", root, { it.right })
        leftTree.root = leftRootItem
        rightTree.root = rightRootItem
    }

    fun toTreeItem(name: String = "", diff: DiffResult, f: (DiffTree) -> JsonNode): TreeItem<NameValue> {
        when (diff) {
            is DiffNode -> {
                val (_, namedValues, indexedValues) = diff
                val node = f(diff)
                val label = if (node.isMissingNode) {
                    ""
                } else {
                    diff.type()
                }
                val result = TreeItem(NameValue(diff.state, name, label))
                for ((k, v) in namedValues) {
                    result.children.add(toTreeItem(k, v, f))
                }
                indexedValues.forEachIndexed { index, child ->
                    result.children.add(toTreeItem(index.toString(), child, f))
                }
                return result
            }
//            is DiffArray -> {
//                val result = TreeItem(NameValue(diff.state, name, "array"))
//                diff.values.forEachIndexed { index, child ->
//                    result.children.add(toTreeItem(index.toString(), child, f))
//                }
//                return result
//            }
            is DiffValue -> {
                return TreeItem(NameValue(DiffState.Same, name, f(diff).toString()))
            }
        }
    }

    data class NameValue(
        val state: DiffState,
        val name: String,
        val value: String
    )

    fun toSame(node1: JsonNode, node2: JsonNode): DiffResult {
        val indices = node1.indices() + node2.indices()
        if (indices.isEmpty()) {
            return DiffValue(DiffState.Same, node1, node2)
        }

        val nameds: MutableList<Pair<String, DiffResult>> = mutableListOf()
        indices.stringKeys.forEach { key ->
            val value1 = node1[key] ?: MissingNode.getInstance()
            val value2 = node2[key] ?: MissingNode.getInstance()
            nameds.add(Pair(key, toSame(value1, value2)))
        }

        val indexed = mutableListOf<DiffResult>()
        indices.intKeys.forEach { index ->
            val value1 = node1[index] ?: MissingNode.getInstance()
            val value2 = node2[index] ?: MissingNode.getInstance()
            indexed.add(toSame(value1, value2))
        }
        return DiffNode(DiffState.Same, nameds, indexed, node1, node2)

//
//        if (node1 is ObjectNode && node2 is ObjectNode) {
//            val keys = linkedSetOf<String>()
//            node1.fieldNames().forEach { keys.add(it) }
//            node2.fieldNames().forEach { keys.add(it) }
//
//            val results: MutableList<Pair<String, DiffResult>> = mutableListOf()
//            keys.forEach { key ->
//                val value1 = node1[key] ?: MissingNode.getInstance()
//                val value2 = node2[key] ?: MissingNode.getInstance()
//                results.add(Pair(key, toSame(value1, value2)))
//            }
//            return DiffNode(DiffState.Same, results)
//        } else if (node1 is ArrayNode && node2 is ArrayNode) {
//            val children = mutableListOf<DiffResult>()
//            val max = maxOf(node1.size(), node2.size())
//            (0..max).forEach { index ->
//                children.add(toSame(node1[index], node2[index]))
//            }
//            return DiffArray(DiffState.Same, children)
//        } else if (node1 is ObjectNode) {
//
//        } else {
//            return DiffValue(DiffState.Same, node1, node2)
//        }
    }

    @FXML
    private fun send(event: ActionEvent) {
        println(event)
    }
}

fun JsonNode.indices(): Indices {
    when (this) {
        is ObjectNode -> {
            val keys = linkedSetOf<String>()
            this.fieldNames().forEach { keys.add(it) }
            return Indices(keys, 0)
        }
        is ArrayNode -> {
            return Indices(emptySet(), this.size())
        }
        else -> return Indices(emptySet(), 0)
    }
}


data class Indices(val stringKeys: Set<String>, val maxIndex: Int) {
    operator fun plus(other: Indices): Indices {
        return Indices(linkedSetOf<String>() + stringKeys + other.stringKeys, maxOf(maxIndex, other.maxIndex))
    }

    val intKeys = 0 until maxIndex

    fun isEmpty(): Boolean {
        return stringKeys.isEmpty() && maxIndex == 0
    }
}
