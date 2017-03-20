package com.example.controller

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import org.springframework.stereotype.Component

/**
 *
 */
@Component
class InputController {
    @FXML
    lateinit private var input1: TextArea

    @FXML
    lateinit private var input2: TextArea

    @FXML
    lateinit private var button: Button

    var text1: String
        get() = input1.textProperty().get()
        set(value) = input1.textProperty().set(value)

    var text2: String
        get() = input2.textProperty().get()
        set(value) = input2.textProperty().set(value)

    fun button(): Button = button
}