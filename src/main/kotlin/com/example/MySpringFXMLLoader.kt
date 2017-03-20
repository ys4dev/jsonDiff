package com.example

import javafx.fxml.FXMLLoader
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
    fun <T, U> load(url: URL): Pair<T, U> {
        val loader = FXMLLoader(url) // ★オリジナルの FXMLLoader を生成

        loader.controllerFactory = Callback<Class<*>, Any> { this.context.getBean(it) } // ★ControllerFactory に ApplicationContext を利用する

        val scene = loader.load<T>()
        val controller = loader.getController<U>()
        return Pair(scene, controller)
    }
}