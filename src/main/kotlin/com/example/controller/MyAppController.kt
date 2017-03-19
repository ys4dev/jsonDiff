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
import javafx.scene.control.*
import javafx.scene.control.cell.TreeItemPropertyValueFactory
import javafx.util.Callback
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
    lateinit private var leftValueColumn: TreeTableColumn<NameValue, JsonNode>

    @FXML
    lateinit private var rightTree: TreeTableView<NameValue>

    @FXML
    lateinit private var rightNameColumn: TreeTableColumn<NameValue, String>

    @FXML
    lateinit private var rightValueColumn: TreeTableColumn<NameValue, String>

    override fun initialize(location: URL, resources: ResourceBundle?) {
        leftNameColumn.cellValueFactory = TreeItemPropertyValueFactory("name")
        leftValueColumn.cellValueFactory = TreeItemPropertyValueFactory("value")
        leftValueColumn.cellFactory = Callback { column ->
            MyCell()
        }

        rightNameColumn.cellValueFactory = TreeItemPropertyValueFactory("name")
        rightValueColumn.cellValueFactory = TreeItemPropertyValueFactory("value")

        val mapper = ObjectMapper().registerKotlinModule()
        val json1 = mapper.readTree("""{"a":0, "b":"c", "parent":{"child":"child"}, "array":[1,"2",3.0,null,true], "c":{"c1":"v1","c2":2}}""")
        val json2 = mapper.readTree("""{"a":0, "c":{"c1":"v1","c2":2}, "d":{"e":3, "f":[], "g":{}}}""")

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
                val result: TreeItem<NameValue> = TreeItem(NodeValue(diff.state, name, node, label))
                for ((k, v) in namedValues) {
                    result.children.add(toTreeItem(k, v, f))
                }
                indexedValues.forEachIndexed { index, child ->
                    result.children.add(toTreeItem(index.toString(), child, f))
                }
                return result
            }
            is DiffValue -> {
                return TreeItem(LeafValue(diff.state, name, f(diff)))
            }
        }
    }


    fun toSame(node1: JsonNode, node2: JsonNode): DiffResult {
        val indices = node1.indices() + node2.indices()
        if (indices.isEmpty()) {
            val state = if (node1.toString() == node2.toString()) DiffState.Same else DiffState.Different
            return DiffValue(state, node1, node2)
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
        val state =
        if (node1 is MissingNode || node2 is MissingNode) {
            DiffState.Different
        } else {
            val containsDiff = (nameds.map { it.second } + indexed).any { it.state != DiffState.Same }
            if (containsDiff) DiffState.ContainsDifferent
            else DiffState.Same
        }
        return DiffNode(state, nameds, indexed, node1, node2)
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


sealed class NameValue {
    abstract val state: DiffState
    abstract val name: String
    abstract val value: JsonNode
}
data class LeafValue(
        override val state: DiffState,
        override val name: String,
        override val value: JsonNode
) : NameValue()
data class NodeValue(
        override val state: DiffState,
        override val name: String,
        override val value: JsonNode,
        val label: String
) : NameValue()


class MyCell: TreeTableCell<NameValue, JsonNode>() {
    override fun updateItem(item: JsonNode?, empty: Boolean) {
        super.updateItem(item, empty)

        if (item == null) {
            text = ""
            style = ""
        } else {
            var treeItem = treeTableView.getTreeItem(index)
            val data = treeItem?.value
            val newStyle =
                    if (data?.value?.isMissingNode ?: false) {
                        "-fx-background-color: lightgray;"
                    } else {
                        when (data?.state) {
                            DiffState.Same -> {
                                ""
                            }
                            DiffState.Different -> {
                                "-fx-background-color: orange;"
                            }
                            DiffState.ContainsDifferent -> {
                                "-fx-background-color: yellow;"
                            }
                            else -> {
                                ""
                            }
                        }
                    }
            style = newStyle

            text = when (data) {
                is NodeValue -> data.label
                is LeafValue -> data.value.toString()
                else -> ""
            }
        }
    }
}
