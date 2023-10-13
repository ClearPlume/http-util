package net.fallingangel.httputil

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import net.fallingangel.httputil.method.Method
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals

class HttpUtilTest {
    @Test
    fun testDefault() {
        val response = HttpUtil.configurer()
                .method(Method.GET)
                .url("https://randomuser.me/api")
                .addParam("results", 5)
                .addParam("exc", "registered,dob,login,id,cell,picture,nat")
                .execute()
        assertAll(
            { assertEquals(200, response.status.statusCode) },
            { assertEquals(2, response.body?.size) },
            { assertContains(response.body!!, "info") },
            { assertContains(response.body!!, "results") }
        )
    }

    @Test
    fun testClass() {
        val response = HttpUtil.configurer()
                .method(Method.GET)
                .url("https://randomuser.me/api")
                .addParam("results", 5)
                .addParam("exc", "registered,dob,login,id,cell,picture,nat")
                .execute(Result::class.java)
        assertAll(
            { assertEquals(200, response.status.statusCode) },
            { assertEquals(5, response.body?.results?.size) },
        )
    }

    @Test
    fun testTypeReference() {
        val response = HttpUtil.configurer()
                .method(Method.GET)
                .url("https://randomuser.me/api")
                .addParam("results", 5)
                .addParam("exc", "registered,dob,login,id,cell,picture,nat")
                .execute(jacksonTypeRef<Result>())
        assertAll(
            { assertEquals(200, response.status.statusCode) },
            { assertEquals(5, response.body?.results?.size) },
        )
    }

    @Test
    fun testConverter() {
        val response = HttpUtil.configurer()
                .method(Method.GET)
                .url("https://randomuser.me/api")
                .addParam("results", 5)
                .addParam("exc", "registered,dob,login,id,cell,picture,nat")
                .execute {
                    val data = jsonMapper.readTree(it)
                    Result(
                        with(data["info"]) {
                            Info(
                                get("version").textValue(),
                                get("results").intValue(),
                                get("page").intValue(),
                                get("seed").textValue()
                            )
                        },
                        with(data["results"]) {
                            map { user ->
                                User(
                                    with(user.get("name")) {
                                        Name(
                                            get("first").textValue(),
                                            get("title").textValue(),
                                            get("last").textValue()
                                        )
                                    },
                                    user.get("email").textValue(),
                                    user.get("phone").textValue(),
                                    Gender.valueOf(user.get("gender").textValue()),
                                    with(user.get("location")) {
                                        Location(
                                            get("country").textValue(),
                                            get("state").textValue(),
                                            get("city").textValue(),
                                            with(get("street")) {
                                                Street(
                                                    get("name").textValue(),
                                                    get("number").intValue()
                                                )
                                            },
                                            get("postcode"),
                                            with(get("timezone")) {
                                                Timezone(
                                                    get("offset").textValue(),
                                                    get("description").textValue()
                                                )
                                            },
                                            with(get("coordinates")) {
                                                Coordinates(
                                                    get("latitude").textValue(),
                                                    get("longitude").textValue()
                                                )
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    )
                }
        assertAll(
            { assertEquals(200, response.status.statusCode) },
            { assertEquals(5, response.body?.results?.size) },
        )
    }

    @Test
    fun testStream() {
        val response = HttpUtil.configurer()
                .method(Method.GET)
                .url("https://zos.alipayobjects.com/rmsportal/jkjgkEfvpUPVyRjUImniVslZfWPnJuuZ.png")
                .executeForStream()
        println("response with stream")
        println("headers:")
        response.headers.forEach { println("${it.name}: ${it.value}") }
        val imageFile = File("jkjgkEfvpUPVyRjUImniVslZfWPnJuuZ.png")
        if (imageFile.exists()) {
            imageFile.delete()
        }
        imageFile.createNewFile()
        imageFile.writeBytes(response.body!!)
        assertEquals(200, response.status.statusCode)
        assertEquals(1435417, response.body?.size)
        assertEquals(1435417, imageFile.length())
        imageFile.delete()
    }
}
