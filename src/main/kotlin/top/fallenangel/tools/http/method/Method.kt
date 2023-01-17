package top.fallenangel.tools.http.method

import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpRequestBase

enum class Method {
    GET, POST, PUT, DELETE;

    companion object {
        fun instance(method: Method): HttpRequestBase {
            return when (method) {
                GET -> HttpGet()
                POST -> HttpPost()
                PUT -> HttpPut()
                DELETE -> HttpDelete()
            }
        }
    }
}
