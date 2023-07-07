package net.fallingangel.httputil

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import net.fallingangel.httputil.method.Method
import org.junit.jupiter.api.Test

class HttpUtilTest {
    @Test
    fun testDefault() {
        val response = HttpUtil.configurer()
                .method(Method.POST)
                .url("http://192.168.31.121:8000/mgt/user/login")
                .addParam("account", "admin")
                .addParam(
                    "password",
                    "lpy8NmX+BrXckebSoPEhEG4Msii3pkgzG0A/HJx79Xs+EcZtQJCMrGZ8zMS2arjgquc6Lu0vPMry1BbbMltXQoASkYdJfUgH7hcQDepH7ttcenfupXjL+eYMfnRPmJWcJl5/VhfcLx8JDlSQba3UmZNbMXWM7QSKPJtG0JtChO4="
                )
                .execute()
        println("default response")
        println(response.getBody())
    }

    @Test
    fun testClass() {
        val response = HttpUtil.configurer()
                .method(Method.POST)
                .url("http://192.168.31.121:8000/mgt/user/login")
                .addParam("account", "admin")
                .addParam(
                    "password",
                    "lpy8NmX+BrXckebSoPEhEG4Msii3pkgzG0A/HJx79Xs+EcZtQJCMrGZ8zMS2arjgquc6Lu0vPMry1BbbMltXQoASkYdJfUgH7hcQDepH7ttcenfupXjL+eYMfnRPmJWcJl5/VhfcLx8JDlSQba3UmZNbMXWM7QSKPJtG0JtChO4="
                )
                .execute(Result::class.java)
        println("response with class")
        println(response.getBody())
    }

    @Test
    fun testTypeReference() {
        val response: Response<Result> = HttpUtil.configurer()
                .method(Method.POST)
                .url("http://192.168.31.121:8000/mgt/user/login")
                .addParam("account", "admin")
                .addParam(
                    "password",
                    "lpy8NmX+BrXckebSoPEhEG4Msii3pkgzG0A/HJx79Xs+EcZtQJCMrGZ8zMS2arjgquc6Lu0vPMry1BbbMltXQoASkYdJfUgH7hcQDepH7ttcenfupXjL+eYMfnRPmJWcJl5/VhfcLx8JDlSQba3UmZNbMXWM7QSKPJtG0JtChO4="
                )
                .execute(jacksonTypeRef<Result>())
        println("response with type reference")
        println(response.getBody())
    }

    @Test
    fun testType() {
        val response: Response<Result> = HttpUtil.configurer()
                .method(Method.POST)
                .url("http://192.168.31.121:8000/mgt/user/login")
                .addParam("account", "admin")
                .addParam(
                    "password",
                    "lpy8NmX+BrXckebSoPEhEG4Msii3pkgzG0A/HJx79Xs+EcZtQJCMrGZ8zMS2arjgquc6Lu0vPMry1BbbMltXQoASkYdJfUgH7hcQDepH7ttcenfupXjL+eYMfnRPmJWcJl5/VhfcLx8JDlSQba3UmZNbMXWM7QSKPJtG0JtChO4="
                )
                .execute(jacksonTypeRef<Result>().type)
        println("response with type")
        println(response.getBody())
    }

    @Test
    fun testConverter() {
        val response: Response<Result> = HttpUtil.configurer()
                .method(Method.POST)
                .url("http://192.168.31.121:8000/mgt/user/login")
                .addParam("account", "admin")
                .addParam(
                    "password",
                    "lpy8NmX+BrXckebSoPEhEG4Msii3pkgzG0A/HJx79Xs+EcZtQJCMrGZ8zMS2arjgquc6Lu0vPMry1BbbMltXQoASkYdJfUgH7hcQDepH7ttcenfupXjL+eYMfnRPmJWcJl5/VhfcLx8JDlSQba3UmZNbMXWM7QSKPJtG0JtChO4="
                )
                .execute {
                    val data = jacksonObjectMapper().readTree(it)
                    Result(
                        data["code"].textValue(),
                        data["code_detail"].textValue(),
                        LoginResult(
                            data["data"]["token"].textValue(),
                            with(data["data"]["user_info"]) {
                                UserResult(
                                    get("user_id").intValue(),
                                    get("name").textValue(),
                                    get("roles").elements().asSequence().map { element -> element.textValue() }.toList(),
                                    get("auths").elements().asSequence().map { element -> element.textValue() }.toList(),
                                    get("project_id").intValue()
                                )
                            }
                        ),
                        data["msg"].asText(),
                    )
                }
        println("response with converter")
        println(response.getBody())
    }
}
