package com.example

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext

@SpringBootApplication
class DemoApplication : Application() {

    lateinit var applicationContext: ConfigurableApplicationContext

    companion object Args {
        lateinit var args: Array<String>
    }

    override fun start(primaryStage: Stage?) {
        Thread.currentThread().name = "main-ui"

        val loader = applicationContext.getBean(MySpringFXMLLoader::class.java)
        primaryStage!!.title = "Hello World!"
        primaryStage.scene = Scene(loader.load(javaClass.getResource("/myapp.fxml")))
        primaryStage.show()
    }

    @Throws(Exception::class)
    override fun init() {
        // set Thread name
        Thread.currentThread().name = "main"

        applicationContext = SpringApplication.run(DemoApplication::class.java, *args)
        applicationContext.autowireCapableBeanFactory.autowireBean(this)
    }
}

fun main(args: Array<String>) {
    DemoApplication.args = args
    Application.launch(DemoApplication::class.java, *args)
}
