package net.fallingangel.httputil

import net.fallingangel.httputil.configuration.HttpUtilBuilder
import org.apache.http.entity.ContentType
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

@Suppress("unused")
object HttpUtil {
    @JvmStatic
    fun configurer(): HttpUtilBuilder {
        log.warn("===============开始构建Http请求===============")
        return HttpUtilBuilder()
    }

    @JvmStatic
    fun contentTypeIsStream(contentType: ContentType?): Boolean {
        if (contentType == null) {
            return false
        }
        val streamTypes = arrayOf(
            ContentType.IMAGE_BMP,
            ContentType.IMAGE_GIF,
            ContentType.IMAGE_JPEG,
            ContentType.IMAGE_PNG,
            ContentType.IMAGE_SVG,
            ContentType.IMAGE_TIFF,
            ContentType.IMAGE_WEBP,
            ContentType.APPLICATION_OCTET_STREAM
        )
        return streamTypes.any { contentTypeEquals(contentType, it) }
    }

    @JvmStatic
    fun contentTypeEquals(ct1: ContentType?, ct2: ContentType?): Boolean {
        if (ct1 == ct2) {
            return true
        }
        return if (ct1 == null || ct2 == null) {
            false
        } else {
            ct1.mimeType == ct2.mimeType
        }
    }

    /**
     * 测试地址"host:port"是否可以连接，默认超时时间300毫秒
     *
     * @param host 不带协议的ip地址或者域名，如："blog.csdn.net", "69.230.200.111"
     * @param port 端口号
     * @param timeout 超时时间
     */
    @JvmStatic
    @JvmOverloads
    fun canConnect(host: String, port: Int, timeout: Int = 300): Boolean {
        val socket = Socket()
        try {
            socket.connect(InetSocketAddress(host, port), timeout)
            socket.close()
        } catch (_: IOException) {
        }
        return socket.isConnected
    }
}
