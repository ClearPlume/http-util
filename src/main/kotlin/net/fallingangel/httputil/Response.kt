package net.fallingangel.httputil

import org.apache.http.HttpVersion
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.entity.ContentType
import org.apache.http.message.BasicStatusLine
import org.apache.http.util.EntityUtils
import java.io.IOException
import java.nio.charset.StandardCharsets

@Suppress("MemberVisibilityCanBePrivate")
class Response<T> {
    var status: StatusLine
    private var haveBody = false

    lateinit var bodyString: String
    private var body: Any? = null

    private constructor(errorMsg: String) {
        status = BasicStatusLine(HttpVersion.HTTP_1_1, 500, "Internal Server Error")
        bodyString = errorMsg
    }

    private constructor(response: CloseableHttpResponse, converter: (ByteArray) -> Any) {
        status = response.statusLine
        val entity = response.entity
        haveBody = entity != null
        if (haveBody) {
            val data = try {
                EntityUtils.toByteArray(entity)
            } catch (e: IOException) {
                e.printStackTrace()
                return
            }
            bodyString = String(data, StandardCharsets.UTF_8)
            if (HttpUtil.contentTypeEquals(ContentType.get(entity), ContentType.APPLICATION_JSON)) {
                if (jsonMapper.isValid(bodyString)) {
                    body = converter(data)
                }
            }
        }
        try {
            response.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        log.info("==========请求结果==========")
        log.info("状态：{}", status)
        log.info("响应体类型：{}", ContentType.get(entity))
        log.info("响应体：{}", body)
        log.info("响应体字符串：{}", bodyString)
        log.info("==========请求结果==========")
        log.warn("===============Http请求结束===============")
    }

    @Suppress("UNCHECKED_CAST")
    fun getBody(): T? {
        return body as T?
    }

    companion object {
        fun <T> build(response: CloseableHttpResponse, converter: (ByteArray) -> Any): Response<T> {
            return Response(response, converter)
        }

        fun <T> build(errorMsg: String): Response<T> {
            return Response(errorMsg)
        }
    }
}
