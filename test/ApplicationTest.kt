package info.benjaminhill

import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertTrue(response.content!!.contains("EZ Cache"))
            }
        }
    }

    /*
    @Test
    fun testCache() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Put, "/cache") {

            }
        }
    }

     */
}
