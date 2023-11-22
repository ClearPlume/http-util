package net.fallingangel.httputil.configuration

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import net.fallingangel.httputil.HttpUtil
import net.fallingangel.httputil.Response
import net.fallingangel.httputil.exception.NetException
import net.fallingangel.httputil.jsonMapper
import net.fallingangel.httputil.log
import net.fallingangel.httputil.method.Method
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.NameValuePair
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.conn.socket.LayeredConnectionSocketFactory
import org.apache.http.cookie.Cookie
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.ContentBody
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.cookie.BasicClientCookie
import org.apache.http.message.BasicNameValuePair
import java.lang.reflect.Array
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext

class HttpUtilBuilder {
    // mimeType: text/plain
    private val contentTypeStr: ContentType = ContentType.create("text/plain", StandardCharsets.UTF_8)

    private val params = mutableListOf<Pair<String, Any>>()
    private val headers = mutableMapOf<String, String>()
    private val cookies = mutableListOf<Cookie>()

    private var method = Method.GET
    private var contentType = ContentType.APPLICATION_JSON
    private var connectTimeout = 0
    private var readTimeout = 0
    private var hostnameVerifier: HostnameVerifier? = null
    private var sslContext: SSLContext? = null
    private var socketFactory: LayeredConnectionSocketFactory? = null

    private var singleParam: Any? = null
    private lateinit var url: String

    fun url(url: String): HttpUtilBuilder {
        this.url = url
        log.info("url: {}", url)
        return this
    }

    fun method(method: Method): HttpUtilBuilder {
        this.method = method
        log.info("method: {}", method)
        return this
    }

    fun skipHostnameVerify(): HttpUtilBuilder {
        hostnameVerifier = HostnameVerifier { _, _ -> true }
        return this
    }

    fun hostnameVerifier(hostnameVerifier: HostnameVerifier): HttpUtilBuilder {
        this.hostnameVerifier = hostnameVerifier
        return this
    }

    fun sslContext(sslContext: SSLContext): HttpUtilBuilder {
        this.sslContext = sslContext
        return this
    }

    fun socketFactory(socketFactory: LayeredConnectionSocketFactory): HttpUtilBuilder {
        this.socketFactory = socketFactory
        return this
    }

    fun addHeader(name: String, value: String): HttpUtilBuilder {
        headers[name] = value
        log.info("新增请求头: \"{}\" = {}", name, value)
        return this
    }

    fun addHeader(header: Map<String, String>): HttpUtilBuilder {
        headers.putAll(header)
        log.info("新增请求头: {}", header)
        return this
    }

    fun addCookie(name: String, value: String): HttpUtilBuilder {
        val cookie = BasicClientCookie(name, value).apply {
            val currUrl = URI(url)
            domain = currUrl.host
            path = currUrl.path
        }
        cookies.add(cookie)
        log.info("新增Cookie: \"{}\" = {}", name, value)
        return this
    }

    fun addCookie(cookie: Map<String, String>): HttpUtilBuilder {
        val currUrl = URI(url)
        cookies.addAll(cookie.map {
            BasicClientCookie(it.key, it.value).apply {
                domain = currUrl.host
                path = currUrl.path
            }
        })
        log.info("新增Cookie: {}", cookie)
        return this
    }

    fun contentType(contentType: ContentType): HttpUtilBuilder {
        this.contentType = contentType
        log.info("contentType: {}", contentType)
        return this
    }

    fun addParam(name: String, value: Any?): HttpUtilBuilder {
        if (value == null || "null" == value.toString()) {
            log.info("参数<{}>的值<{}>为null或者\"null\"，已将其忽略！", name, value)
        } else {
            params.add(Pair(name, value))
            log.info("新增参数: \"{}\" = {}", name, value)
        }
        return this
    }

    fun addParam(param: Map<String, Any?>): HttpUtilBuilder {
        param.forEach { (name, value) -> addParam(name, value) }
        return this
    }

    /**
     * 直接把一个json字符串或者对象设置为请求体
     */
    fun singleParam(param: Any?): HttpUtilBuilder {
        singleParam = param
        log.info("新增请求体参数：$param")
        return this
    }

    /**
     * 设置连接超时时间
     *
     * @param timeout 单位毫秒，默认值为0，永不超时
     */
    fun connectTimeout(timeout: Int): HttpUtilBuilder {
        connectTimeout = timeout
        log.info("设置连接超时时间：$connectTimeout")
        return this
    }

    /**
     * 设置获取数据超时时间
     *
     * @param timeout 单位毫秒，默认值为0，永不超时
     */
    fun readTimeout(timeout: Int): HttpUtilBuilder {
        readTimeout = timeout
        log.info("设置获取数据超时时间：$readTimeout")
        return this
    }

    /**
     * 发起请求，结果以`Map<String, Object>`的形式处理
     *
     * <pre>`HttpUtil.HttpUtilBuilder httpBuilder = HttpUtil.configurer()
     *                                                      .url(cdapUrl + path)
     *                                                      .addParam("start", start)
     *                                                      .addParam("end", "now")
     *                                                      .addParam("direction", direction)
     *                                                      .method(Method.GET);
     * // Response<?> response = httpBuilder.execute();
     * Response<Map<String, Object>> response = httpBuilder.execute();`</pre>
     */
    fun execute(): Response<Map<String, Any?>> {
        return execute {
            jsonMapper
                    .registerModule(SimpleModule().addDeserializer(Map::class.java, object : JsonDeserializer<Map<String, Any?>>() {
                        override fun deserialize(p: JsonParser, ctxt: DeserializationContext) = deserializeObj(p.codec.readTree(p))

                        /**
                         * 将一个[JsonNode]反序列化为Map<String, Any?>
                         */
                        fun deserializeObj(node: JsonNode): Map<String, Any?> {
                            return node.fields()
                                    .asSequence()
                                    .map { (key, value) -> key to deserializeValue(value) }
                                    .associate { pair -> pair }
                        }

                        fun deserializeValue(value: JsonNode): Any? {
                            return when (value.nodeType) {
                                JsonNodeType.OBJECT -> deserializeObj(value)

                                JsonNodeType.ARRAY -> {
                                    value.elements()
                                            .asSequence()
                                            .map { element -> deserializeValue(element) }
                                            .toList()
                                }

                                JsonNodeType.BOOLEAN -> value.booleanValue()

                                JsonNodeType.MISSING, JsonNodeType.NULL -> null

                                JsonNodeType.NUMBER -> value.numberValue()

                                JsonNodeType.STRING -> value.textValue()

                                else -> value
                            }
                        }
                    }))
                    .readValue(it, jacksonTypeRef())
        }
    }

    /**
     * 发起请求，结果转换为`type`类型
     *
     * <pre>`Response<Map<String, Object>> response = HttpUtil.configurer()
     *                                                        .url("http://192.168.31.167:8300/mgt/task_log/0")
     *                                                        .addParam("page_index", 1)
     *                                                        .addParam("page_size", 10)
     *                                                        .execute(new TypeReference<>() {});
     * Map<String, Object> body = response.getBody();`</pre>
     *
     * 注：此[TypeReference]为jackson的[com.fasterxml.jackson.core.type.TypeReference]，也可使用[net.fallingangel.httputil.TypeReference]
     */
    fun <T> execute(type: TypeReference<T>): Response<T> {
        return execute { jsonMapper.readValue(it, type) }
    }

    /**
     * 发起请求，结果转换为`klass`类型
     *
     * <pre>`Response<Map> response = HttpUtil.configurer()
     *                                        .url("http://192.168.31.167:8300/mgt/task_log/0")
     *                                        .addParam("page_index", 1)
     *                                        .addParam("page_size", 10)
     *                                        .execute(Map.class);
     * Map body = response.getBody();`</pre>
     */
    fun <T : Any> execute(klass: Class<T>): Response<T> {
        return execute { jsonMapper.readValue(it, klass) }
    }

    /**
     * 发起请求，直接返回收到的响应数据流，一般用于从下载接口获取文件
     *
     * <pre>`Response<ByteArray> response = HttpUtil.configurer()
     *                                        .url("http://192.168.31.167:8300/mgt/task_log/0")
     *                                        .addParam("page_index", 1)
     *                                        .addParam("page_size", 10)
     *                                        .executeForStream();
     * ByteArray body = response.getBody();`</pre>
     */
    fun executeForStream(): Response<ByteArray> {
        return execute { it }
    }

    /**
     * 发起请求，结果以`converter`转换为自定义类型
     *
     * <pre>`Response<List<JSONObject>> response = HttpUtil.configurer()
     *                                                     .url(cdapUrl + path)
     *                                                     .method(Method.GET)
     *                                                     .execute(JSON::parseArray);`</pre>
     */
    fun <T> execute(converter: (ByteArray) -> T): Response<T> {
        return try {
            log.info("发起请求...")
            val cookieStore = BasicCookieStore().apply {
                addCookies(this@HttpUtilBuilder.cookies.toTypedArray())
            }
            Response.build(
                HttpClientBuilder.create()
                        .setDefaultCookieStore(cookieStore)
                        .setSSLHostnameVerifier(hostnameVerifier)
                        .setSSLContext(sslContext)
                        .setSSLSocketFactory(socketFactory)
                        .build()
                        .execute(buildRequest()),
                converter
            )
        } catch (e: NetException) {
            e.printStackTrace()
            val errorMsg = e.toString()
            log.error("请求失败，错误信息如下：")
            log.error(errorMsg)
            Response.build(errorMsg)
        }
    }

    /**
     * 构建Http请求
     */
    private fun buildRequest(): HttpRequestBase {
        // 获取Http请求实例
        val instance = Method.instance(method)

        // 设置请求配置
        instance.config = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                // 连接超时时间
                .setConnectTimeout(connectTimeout)
                .setConnectionRequestTimeout(connectTimeout)
                // 获取数据超时时间
                .setSocketTimeout(readTimeout)
                .build()

        // 设置URL
        instance.uri = URI.create(url)

        // 添加Headers
        headers.forEach { (name, value) -> instance.addHeader(name, value) }

        // 不默认使用长连接
        if ("Connection" !in headers) {
            log.info("未指定Connection请求头，添加<Connection=close>以避免长连接！")
            instance.addHeader("Connection", "close")
        }

        if (instance is HttpGet) { // 如果是Get请求，参数直接拼接在URL之后；否则，需要根据请求ContentType决定请求体设置方式
            val urlBuilder = StringBuilder(url).append('?')

            for ((name, second) in params) {
                val value = handleValue(second)
                // 参数有可能是集合
                if (value is List<*>) {
                    value.forEach { urlBuilder.append(name).append('=').append(it).append('&') }
                } else {
                    urlBuilder.append(name).append('=').append(value).append('&')
                }
            }
            // 删除最后一个多余的字符：'&'
            urlBuilder.deleteCharAt(urlBuilder.length - 1)
            // 重新设置URL
            instance.uri = URI.create(urlBuilder.toString())
        } else {
            // 根据请求ContentType决定请求体设置方式
            // 注：也可能是JSON格式的单个对象，也就是直接设置一个对象，转成JSON
            // 注：也可能直接就是一个JSON字符串
            val request = instance as HttpEntityEnclosingRequest
            if (singleParam != null) {
                val paramStr = (singleParam as? String) ?: jsonMapper.writeValueAsString(singleParam)
                request.entity = StringEntity(paramStr, contentType)
                log.info("param: {}", paramStr)
            } else {
                if (HttpUtil.contentTypeEquals(contentType, ContentType.APPLICATION_JSON)) { // 如果是JSON，把参数装进Map转为json字符串，以StringEntity的形式发送
                    val param = jsonMapper.writeValueAsString(collectParam())
                    request.entity = StringEntity(param, contentType)
                    log.info("param: {}", param)
                } else if (HttpUtil.contentTypeEquals(contentType, ContentType.APPLICATION_FORM_URLENCODED)) { // 如果是普通表单
                    val params = mutableListOf<NameValuePair>()

                    for ((name, second) in this.params) {
                        val value = handleValue(second)
                        if (value is List<*>) {
                            value.forEach { params.add(BasicNameValuePair(name, it.toString())) }
                        } else {
                            params.add(BasicNameValuePair(name, value.toString()))
                        }
                    }
                    request.entity = UrlEncodedFormEntity(params, StandardCharsets.UTF_8)
                    log.info("param: {}", collectParam())
                } else { // 否则按照文件处理
                    val body = MultipartEntityBuilder
                            .create()
                            .setMode(HttpMultipartMode.RFC6532)
                            .setContentType(contentType)
                    for ((name, second) in params) {
                        val value = handleValue(second)

                        if (value is List<*>) {
                            for (o in value) {
                                if (o is ContentBody) {
                                    body.addPart(name, o)
                                } else {
                                    body.addTextBody(name, o.toString(), contentTypeStr)
                                }
                            }
                        } else {
                            if (value is ContentBody) {
                                body.addPart(name, value)
                            } else {
                                body.addTextBody(name, value.toString(), contentTypeStr)
                            }
                        }
                    }
                    request.entity = body.build()
                    log.info("param: {}", collectParam())
                }
            }
        }
        log.info(instance.uri.toString())
        return instance
    }

    /**
     * 把List<Pair>转为Map，有名字相同的参数则转为List
     */
    private fun collectParam(): Map<String, Any> {
        return params.fold(mutableMapOf()) { result, (name, value) ->
            if (name in result) {
                val oldValue = result[name]!!
                if (oldValue is MutableList<*>) {
                    @Suppress("UNCHECKED_CAST")
                    if (value is List<*>) {
                        (oldValue as MutableList<Any>).addAll(listOf(value))
                    } else {
                        (oldValue as MutableList<Any>).add(value)
                    }
                } else {
                    val values = mutableListOf(oldValue)
                    if (value is List<*>) {
                        values.addAll(listOf(value))
                    } else {
                        values.add(value)
                    }
                    result[name] = values
                }
            } else {
                result[name] = value
            }
            result
        }
    }

    /**
     * 处理参数值，如果它是集合、数组，统一转成ArrayList，否则原样返回
     *
     * @return 处理后的参数，可能是ArrayList，可能是原样
     */
    private fun handleValue(value: Any?): Any? {
        if (value != null) {
            if (value.javaClass.isArray) {
                val values = mutableListOf<Any>()
                for (i in 0..<Array.getLength(value)) {
                    values.add(Array.get(value, i))
                }
                return values
            } else if (value is Collection<*>) {
                return mutableListOf(value)
            }
        }
        return value
    }
}
