package com.example.web

import com.example.controller.MyAppController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 *
 */
@RestController
class DiffController {

    @Autowired
    lateinit private var myAppController: MyAppController

    @RequestMapping("/diff")
    fun diff(@RequestParam("json1") json1: String, @RequestParam("json2") json2: String): String {
        myAppController.diff(json1, json2)
        return "OK"
    }
}

data class Input(
        val json1: Map<String, *>,
        val json2: Map<String, *>
)