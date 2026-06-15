package com.example.dobby

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class DobbyApplicationTests {

    @Autowired
    private lateinit var appProperties: AppProperties

    @Test
    fun contextLoads() {
    }

    @Test
    fun `should successfully load AppProperties bean into application context`() {
        assertNotNull(appProperties)
    }
}
