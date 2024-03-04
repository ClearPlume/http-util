package net.fallingangel.httputil

import net.fallingangel.httputil.logging.Level
import net.fallingangel.httputil.utils.isValid
import net.fallingangel.httputil.utils.jsonMapper
import net.fallingangel.httputil.utils.log
import org.apache.http.Header
import org.apache.http.HttpVersion
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.entity.ContentType
import org.apache.http.message.BasicStatusLine
import org.apache.http.util.EntityUtils
import java.nio.charset.StandardCharsets

@Suppress("MemberVisibilityCanBePrivate")
class Response<T> {
    private val logLevel: Level
    private val haveBody: Boolean

    val status: StatusLine
    val body: T?
    val bodyString: String?
    val headers: Array<Header>

    private constructor(errorMsg: String, logLevel: Level) {
        this.logLevel = logLevel
        status = BasicStatusLine(HttpVersion.HTTP_1_1, 500, "Internal Server Error")
        headers = emptyArray()
        haveBody = false
        body = null
        bodyString = errorMsg
    }

    private constructor(response: CloseableHttpResponse, converter: (ByteArray) -> T, logLevel: Level) {
        this.logLevel = logLevel
        status = response.statusLine
        headers = response.allHeaders
        val entity = response.entity
        haveBody = entity != null

        if (logLevel > Level.NONE) {
            log.info("==========请求结果==========")
            log.info("状态：{}", status)

            if (logLevel > Level.BASIC) {
                log.info(
                    "响应头：{}", jsonMapper.writeValueAsString(
                        response.allHeaders
                                .map { it.name to it.value }
                                .associate { it }
                    )
                )
            }

            log.info("响应体类型：{}", ContentType.get(entity))
        }

        if (haveBody) {
            val data = EntityUtils.toByteArray(entity)
            if (HttpUtil.contentTypeIsStream(ContentType.get(entity))) {
                body = converter(data)
                bodyString = null
                if (logLevel > Level.BASIC) {
                    log.info("响应体为流，不在此展示响应体字符串")
                }
            } else {
                bodyString = String(data, StandardCharsets.UTF_8)
                if (logLevel > Level.BASIC) {
                    log.info("响应体字符串：{}", bodyString)
                }

                if (HttpUtil.contentTypeEquals(ContentType.get(entity), ContentType.APPLICATION_JSON)) {
                    if (jsonMapper.isValid(bodyString)) {
                        body = converter(data)
                        if (logLevel > Level.BASIC) {
                            log.info("响应体：{}", body)
                        }
                    } else {
                        body = null
                    }
                } else {
                    body = null
                }
            }
        } else {
            body = null
            bodyString = null
        }
        response.close()
        if (logLevel > Level.NONE) {
            log.info("==========请求结果==========")
        }
        log.warn("===============Http请求结束===============")
    }

    companion object {
        fun <T> build(response: CloseableHttpResponse, converter: (ByteArray) -> T, logLevel: Level): Response<T> {
            return Response(response, converter, logLevel)
        }

        fun <T> build(errorMsg: String, logLevel: Level): Response<T> {
            return Response(errorMsg, logLevel)
        }
    }
}
