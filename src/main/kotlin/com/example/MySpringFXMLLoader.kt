package com.example

import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.util.Callback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.URL

/**
 *
 */
/**

 */
@Component // ★コンポーネントとして登録
class MySpringFXMLLoader {

    @Autowired
    lateinit private var context: ApplicationContext

    @Throws(IOException::class)
    fun load(url: URL): Parent {
        val loader = FXMLLoader(url) // ★オリジナルの FXMLLoader を生成

        loader.controllerFactory = Callback<Class<*>, Any> { this.context.getBean(it) } // ★ControllerFactory に ApplicationContext を利用する

        return loader.load<Parent>()
    }
}