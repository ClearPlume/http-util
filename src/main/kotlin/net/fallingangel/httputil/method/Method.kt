package net.fallingangel.httputil.method

import org.apache.http.client.methods.*

enum class Method {
    GET, POST, PUT, DELETE, PATCH;

    companion object {
        fun instance(method: Method): HttpRequestBase {
            return when (method) {
                GET -> HttpGet()
                POST -> HttpPost()
                PUT -> HttpPut()
                DELETE -> HttpDelete()
                PATCH -> HttpPatch()
            }
        }
    }
}
