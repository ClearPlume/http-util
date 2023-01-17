package top.fallenangel.tools.http

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import org.junit.jupiter.api.Test
import top.fallenangel.tools.http.method.Method

class HttpUtilTest {
    @Test
    fun testDefault() {
        val response = HttpUtil.configurer()
                .method(Method.POST)
                .url("http://192.168.31.121:8000/mgt/user/login")
                .addParam("account", "admin")
                .addParam("password", "123456")
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
                .addParam("password", "123456")
                .execute(Result::class.java)
        println("response with class")
        println(response.getBody())
    }

    @Test
    fun testType() {
        val response: Response<Result> = HttpUtil.configurer()
                .method(Method.POST)
                .url("http://192.168.31.121:8000/mgt/user/login")
                .addParam("account", "admin")
                .addParam("password", "123456")
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
                .addParam("password", "123456")
                .execute {
                    val data = jacksonObjectMapper().readTree(it)
                    Result(
                        data.get("code").textValue(),
                        LoginResult(
                            data.get("data").get("token").textValue(),
                            with(data.get("data").get("user_info")) {
                                UserResult(
                                    get("user_id").intValue(),
                                    get("name").textValue(),
                                    get("role_id").intValue(),
                                    get("project_id").intValue()
                                )
                            }
                        ),
                        data.get("code_detail").textValue()
                    )
                }
        println("response with converter")
        println(response.getBody())
    }
}
