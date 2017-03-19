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
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.control.cell.TreeItemPropertyValueFactory
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
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
    lateinit private var rightValueColumn: TreeTableColumn<NameValue, JsonNode>

    override fun initialize(location: URL, resources: ResourceBundle?) {
        leftNameColumn.cellValueFactory = TreeItemPropertyValueFactory("name")
        leftNameColumn.cellFactory = Callback { HeadCell() }
        leftValueColumn.cellValueFactory = TreeItemPropertyValueFactory("value")
        leftValueColumn.cellFactory = Callback { MyCell() }
        leftTree.rowFactory = Callback { MyRow() }

        rightNameColumn.cellValueFactory = TreeItemPropertyValueFactory("name")
        rightNameColumn.cellFactory = Callback { HeadCell() }
        rightValueColumn.cellValueFactory = TreeItemPropertyValueFactory("value")
        rightValueColumn.cellFactory = Callback { MyCell() }
        rightTree.rowFactory = Callback { MyRow() }

        val mapper = ObjectMapper().registerKotlinModule()
        val json1 = mapper.readTree("""{"a":0, "b":"c", "parent":{"child":"child"}, "array":[1,"2",3.0,null,true], "c":{"c1":"v1","c2":2}}""")
        val json2 = mapper.readTree("""{"a":0, "c":{"c1":"v1","c2":2}, "d":{"e":3, "f":[], "g":{}}}""")

        val root = toSame(json1, json2)//diffService.diff(json1, json2)
        val leftRootItem = toTreeItem("", root, { it.left })
        val rightRootItem = toTreeItem("", root, { it.right })
        leftTree.root = leftRootItem
        rightTree.root = rightRootItem
    }

    fun toTreeItem(name: String = "", diff: DiffResult, f: (DiffTree) -> JsonNode, statusList: List<DiffState> = listOf()): TreeItem<NameValue> {
        val stack = statusList + diff.state
        when (diff) {
            is DiffNode -> {
                val (_, namedValues, indexedValues) = diff
                val node = f(diff)
                val label = if (node.isMissingNode) {
                    ""
                } else {
                    diff.type()
                }
                val result: TreeItem<NameValue> = TreeItem(NodeValue(diff.state, name, node, label, stack))
                for ((k, v) in namedValues) {
                    result.children.add(toTreeItem(k, v, f, stack))
                }
                indexedValues.forEachIndexed { index, child ->
                    result.children.add(toTreeItem(index.toString(), child, f, stack))
                }
                return result
            }
            is DiffValue -> {
                return TreeItem(LeafValue(diff.state, name, f(diff), stack))
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
    abstract val statusList: List<DiffState>
}
data class LeafValue(
        override val state: DiffState,
        override val name: String,
        override val value: JsonNode,
        override val statusList: List<DiffState>
) : NameValue()
data class NodeValue(
        override val state: DiffState,
        override val name: String,
        override val value: JsonNode,
        val label: String,
        override val statusList: List<DiffState>
) : NameValue()


class MyCell: TreeTableCell<NameValue, JsonNode>() {

    fun buildText(row: NameValue, value: JsonNode): String {
        return when (row) {
            is NodeValue -> row.label
            is LeafValue -> row.value.toString()
            else -> ""
        }
    }

    override fun updateItem(item: JsonNode?, empty: Boolean) {
        super.updateItem(item, empty)

        val treeItem = treeTableView.getTreeItem(index)
        val data = treeItem?.value
        if (item == null || data == null) {
            text = ""
        } else {
            text = buildText(data, item)
        }
    }
}

class MyRow: TreeTableRow<NameValue>() {

    fun buildStyle(row: NameValue): String {
        val style =
                if (row.value.isMissingNode) {
                    "-fx-background-color: lightgray;"
                } else {
                    when (row.state) {
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
        return style
    }

    override fun updateItem(item: NameValue?, empty: Boolean) {
        super.updateItem(item, empty)

        val treeItem = treeTableView.getTreeItem(index)
        val data = treeItem?.value
        val selected = treeTableView.selectionModel.selectedIndex == index

        if (item == null || data == null || selected) {
            style = ""
        } else {
            style = buildStyle(data)
        }
    }

    override fun updateSelected(selected: Boolean) {
        super.updateSelected(selected)

        val treeItem = treeTableView.getTreeItem(index)
        val data = treeItem?.value

        if (data == null || selected) {
            style = ""
        } else {
            style = buildStyle(data)
        }
    }
}

class HeadCell : TreeTableCell<NameValue, String>() {

    override fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)

        text = item ?: ""

        val treeItem = treeTableView.getTreeItem(index)
        val data = treeItem?.value
        val selected = treeTableView.selectionModel.selectedIndex == index

        if (data?.statusList == null) {
            background = Background.EMPTY
            return
        }

        val w = tableColumn.width
        val fills: List<BackgroundFill> = data.statusList.mapIndexed { index, diffState -> BackgroundFill(diffState.color(), null, Insets(0.0, w - 20 * (index + 1), 0.0, 20.0 * index)) }

        background = Background(*fills.toTypedArray())
    }
}

fun DiffState.color(): Color {
    return when (this) {
        DiffState.Same -> Color.TRANSPARENT
        DiffState.Different -> Color.ORANGE
        DiffState.ContainsDifferent -> Color.YELLOW
        DiffState.Missing -> Color.LIGHTGRAY
    }
}