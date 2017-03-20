package com.example

import com.example.controller.MyAppController
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.Pane
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
        primaryStage!!.title = "Json diff"
        val (pane, _) = loader.load<Pane, MyAppController>(javaClass.getResource("/myapp.fxml"))
        primaryStage.scene = Scene(pane)
        primaryStage.scene.stylesheets.add("/diff.css")
        primaryStage.show()
    }

    @Throws(Exception::class)
    override fun stop() {
        // LOG.debug("Stop JavaFX application");
        super.stop()
        applicationContext.close()
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
