package net.fallingangel.httputil

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import net.fallingangel.httputil.method.Method
import net.fallingangel.httputil.util.log
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.NameValuePair
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.ContentBody
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicNameValuePair
import java.io.IOException
import java.lang.reflect.Array
import java.lang.reflect.Type
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.nio.charset.StandardCharsets

object HttpUtil {
    val CONTENT_TYPE_STR: ContentType = ContentType.create("text/plain", StandardCharsets.UTF_8)

    @JvmStatic
    fun configurer(): HttpUtilBuilder {
        log.warn("===============开始构建Http请求===============")
        return HttpUtilBuilder()
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

    class HttpUtilBuilder {
        private val params: MutableList<Pair<String, Any>> = mutableListOf()
        private val headers: MutableMap<String, String> = mutableMapOf()

        private var method = Method.GET
        private var contentType = ContentType.APPLICATION_JSON
        private var connectTimeout = 0
        private var readTimeout = 0

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
        fun connectTimeout(timeout: Int) {
            connectTimeout = timeout
            log.info("设置连接超时时间：$connectTimeout")
        }

        /**
         * 设置获取数据超时时间
         *
         * @param timeout 单位毫秒，默认值为0，永不超时
         */
        fun readTimeout(timeout: Int) {
            readTimeout = timeout
            log.info("设置获取数据超时时间：$readTimeout")
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
                jacksonObjectMapper()
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
         * 注：此[TypeReference]为jackson的[com.fasterxml.jackson.core.type.TypeReference]，也可使用[net.fallingangel.httputil.util.TypeReference]
         */
        fun <T> execute(type: TypeReference<T>): Response<T> {
            return execute(type.type)
        }

        /**
         * 发起请求，结果转换为`type`类型
         *
         * <pre>`Response<Map<String, Object>> response = HttpUtil.configurer()
         *                                                        .url("http://192.168.31.167:8300/mgt/task_log/0")
         *                                                        .addParam("page_index", 1)
         *                                                        .addParam("page_size", 10)
         *                                                        .execute(new TypeReference<>() {}.getType());
         * Map<String, Object> body = response.getBody();`</pre>
         */
        fun <T> execute(type: Type): Response<T> {
            return execute {
                val objectMapper = jacksonObjectMapper()
                objectMapper.readValue(it, objectMapper.constructType(type))
            }
        }

        /**
         * 发起请求，结果转换为`klass`类型
         *
         * <pre>`Response<Map> response = HttpUtil.configurer()
         *                                        .url("http://192.168.31.167:8300/mgt/task_log/0")
         *                                        .addParam("page_index", 1)
         *                                        .addParam("page_size", 10)
         *                                        .execute(Map.class);
         * Map body = response.getBody();
        `</pre> *
         */
        fun <T : Any> execute(klass: Class<T>): Response<T> {
            return execute { jacksonObjectMapper().readValue(it, klass) }
        }

        /**
         * 发起请求，结果以`converter`转换为自定义类型
         *
         * <pre>`Response<List<JSONObject>> response = HttpUtil.configurer()
         *                                                     .url(cdapUrl + path)
         *                                                     .method(Method.GET)
         *                                                     .execute(JSON::parseArray);`</pre>
         */
        fun <T> execute(converter: (ByteArray) -> Any): Response<T> {
            val response: CloseableHttpResponse
            return try {
                log.info("发起请求...")
                response = HttpClientBuilder.create().build().execute(buildRequest())
                Response.build(response, converter)
            } catch (e: IOException) {
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
                instance.setURI(URI.create(urlBuilder.toString()))
            } else {
                // 根据请求ContentType决定请求体设置方式
                // 注：也可能是JSON格式的单个对象，也就是直接设置一个对象，转成JSON
                // 注：也可能直接就是一个JSON字符串
                val request = instance as HttpEntityEnclosingRequest
                if (singleParam != null) {
                    val paramStr = (singleParam as? String) ?: jacksonObjectMapper().writeValueAsString(singleParam)
                    request.entity = StringEntity(paramStr, StandardCharsets.UTF_8)
                    log.info("param: {}", paramStr)
                } else {
                    if (contentTypeEquals(contentType, ContentType.APPLICATION_JSON)) { // 如果是JSON，把参数装进Map转为json字符串，以StringEntity的形式发送
                        val param = jacksonObjectMapper().writeValueAsString(collectParam())
                        request.entity = StringEntity(param, StandardCharsets.UTF_8)
                        log.info("param: {}", param)
                    } else if (contentTypeEquals(contentType, ContentType.APPLICATION_FORM_URLENCODED)) { // 如果是普通表单
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
                                        body.addTextBody(name, o.toString(), CONTENT_TYPE_STR)
                                    }
                                }
                            } else {
                                if (value is ContentBody) {
                                    body.addPart(name, value)
                                } else {
                                    body.addTextBody(name, value.toString(), CONTENT_TYPE_STR)
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
                    for (i in 0 until Array.getLength(value)) {
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
}
