package com.example.controller

import com.example.MySpringFXMLLoader
import com.example.domain.*
import com.example.server.service.DiffService
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.TreeItemPropertyValueFactory
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.util.Callback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
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

    @Autowired
    lateinit var applicationContext: ConfigurableApplicationContext

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

    lateinit private var inputController: InputController

    lateinit private var json1: JsonNode
    lateinit private var json2: JsonNode

    lateinit private var diffRoot: DiffTree
    lateinit private var leftRootItem: TreeItem<NameValue>
    lateinit private var rightRootItem: TreeItem<NameValue>

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
        json1 = mapper.readTree("{}")
        json2 = mapper.readTree("{}")
//        json1 = mapper.readTree("""{"a":0, "b":"c", "parent":{"child":"child"}, "array":[1,"2",3.0,null,true], "c":{"c1":"v1","c2":2}, "h":"node1","i":"あ"}""")
//        json2 = mapper.readTree("""{"a":0, "c":{"c1":"v1","c2":2}, "d":{"e":3, "f":[], "g":{}}, "h":"node2"}""")

        updateJson()

        text.textProperty().addListener { observableValue, oldValue, newValue ->
            if (newValue.isBlank()) {
                leftTree.root = leftRootItem
                rightTree.root = rightRootItem
            } else {
                val filtered = diffRoot.filter {
                    it.name.contains(newValue) ||
                    it.left.value.isValueNode && it.left.value.toString().contains(newValue) ||
                    it.right.value.isValueNode && it.right.value.toString().contains(newValue)
                }

                if (filtered != null) {
                    leftTree.root = toTreeItem("", filtered, { it.left })
                    rightTree.root = toTreeItem("", filtered, { it.right })
                    leftTree.root.forEach { it.isExpanded = true }
                    rightTree.root.forEach { it.isExpanded = true }
                    leftTree.root.zipWith(rightTree.root, {a, b -> link(a, b)})
                } else {
                    leftTree.root = null
                    rightTree.root = null
                }
            }
            leftTree.root?.isExpanded = true
            rightTree.root?.isExpanded = true
        }
    }

    private fun updateJson() {
        diffRoot = diffService.diff(json1, json2)
        leftRootItem = toTreeItem("", diffRoot, { it.left })
        rightRootItem = toTreeItem("", diffRoot, { it.right })
        leftRootItem.zipWith(rightRootItem, { a, b -> link(a, b) })

        leftTree.root = leftRootItem
        rightTree.root = rightRootItem
        leftTree.root.isExpanded = true
        rightTree.root.isExpanded = true
    }

    private fun toTreeItem(name: String = "", diff: DiffTree, f: (DiffTree) -> Diff, statusList: List<DiffState> = listOf()): TreeItem<NameValue> {
        val stack = statusList + f(diff).state
        when (diff) {
            is DiffNode -> {
                val (namedValues, indexedValues) = diff
                val node = f(diff).value
                val label = if (node.isMissingNode) {
                    ""
                } else {
                    diff.type
                }
                val result: TreeItem<NameValue> = TreeItem(NodeValue(f(diff).state, name, node, label, stack))
                for ((k, v) in namedValues) {
                    result.children.add(toTreeItem(k, v, f, stack))
                }
                indexedValues.forEachIndexed { index, child ->
                    result.children.add(toTreeItem(index.toString(), child, f, stack))
                }
                return result
            }
            is DiffValue -> {
                if (f(diff).value.isValueNode) {
                    return TreeItem(LeafValue(f(diff).state, name, f(diff).value, stack))
                } else {
                    return TreeItem(NodeValue(f(diff).state, name, f(diff).value, diff.type, stack))
                }
            }
        }
    }

    private fun <T> link(a: TreeItem<T>, b: TreeItem<T>): Unit {
        a.expandedProperty().addListener { observable, oldValue, newValue ->
            b.isExpanded = newValue
        }
        b.expandedProperty().addListener { o, oldValue, newValue ->
            a.isExpanded = newValue
        }
    }

    @FXML
    private fun send(event: ActionEvent) {
        val stage = Stage()
        val loader = applicationContext.getBean(MySpringFXMLLoader::class.java)
        stage.title = "Input"
        val (scene, controller) = loader.load<Pane, InputController>(javaClass.getResource("/input.fxml"))
        stage.scene = Scene(scene)
        inputController = controller

        inputController.button().onMouseClicked = EventHandler { _ ->
            val mapper = ObjectMapper().registerKotlinModule()
            val tree1 = mapper.readTree(inputController.text1)
            val tree2 = mapper.readTree(inputController.text2)
            json1 = tree1
            json2 = tree2
            updateJson()
            stage.close()
        }

        val mapper = ObjectMapper().registerKotlinModule().enable(SerializationFeature.INDENT_OUTPUT)
        inputController.text1 = mapper.writeValueAsString(json1)
        inputController.text2 = mapper.writeValueAsString(json2)

        stage.show()
    }

    fun diff(json1: String, json2: String) {
        val mapper = ObjectMapper().registerKotlinModule()
        val tree1 = mapper.readTree(json1)
        val tree2 = mapper.readTree(json2)
        Platform.runLater {
            this.json1 = tree1
            this.json2 = tree2
            updateJson()
        }
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
//        val selected = treeTableView.selectionModel.selectedIndex == index

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

fun <T> TreeItem<T>.forEach(f: (TreeItem<T>) -> Unit): Unit {
    f(this)
    this.children.forEach(f)
}

fun <T> TreeItem<T>.zipWith(other: TreeItem<T>, f: (TreeItem<T>, TreeItem<T>) -> Unit): Unit {
    f(this, other)
    children.zip(other.children).forEach { (a, b) -> a.zipWith(b, f) }
}
