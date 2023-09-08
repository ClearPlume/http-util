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
    var body: T? = null
    var haveBody = false

    lateinit var bodyString: String

    private constructor(errorMsg: String) {
        status = BasicStatusLine(HttpVersion.HTTP_1_1, 500, "Internal Server Error")
        bodyString = errorMsg
    }

    private constructor(response: CloseableHttpResponse, converter: (ByteArray) -> T) {
        status = response.statusLine
        val entity = response.entity
        haveBody = entity != null

        log.info("==========请求结果==========")
        log.info("状态：{}", status)
        log.info("响应体类型：{}", ContentType.get(entity))

        if (haveBody) {
            val data = try {
                EntityUtils.toByteArray(entity)
            } catch (e: IOException) {
                e.printStackTrace()
                return
            }
            bodyString = String(data, StandardCharsets.UTF_8)

            log.info("响应体字符串：{}", bodyString)
            if (HttpUtil.contentTypeEquals(ContentType.get(entity), ContentType.APPLICATION_JSON)) {
                if (jsonMapper.isValid(bodyString)) {
                    body = converter(data)
                    log.info("响应体：{}", body)
                }
            }
        }
        try {
            response.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        log.info("==========请求结果==========")
        log.warn("===============Http请求结束===============")
    }

    companion object {
        fun <T> build(response: CloseableHttpResponse, converter: (ByteArray) -> T): Response<T> {
            return Response(response, converter)
        }

        fun <T> build(errorMsg: String): Response<T> {
            return Response(errorMsg)
        }
    }
}
