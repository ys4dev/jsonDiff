package com.example

import com.example.domain.nullAsFalse
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class DemoApplicationTests {

	@Test
	fun contextLoads() {
	}

	@Test
	fun nullAsFalse1() {
		val p: (Int) -> Boolean = { it % 2 == 0 }
		val target = p.nullAsFalse()
		Assert.assertFalse(target(null))
		Assert.assertTrue(target(0))
		Assert.assertFalse(target(1))
	}

}
