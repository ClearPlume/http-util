package top.clearplume.httputil.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val <reified T : Any> T.log: Logger
    inline get() = LoggerFactory.getLogger(T::class.java)

fun ObjectMapper.isValid(json: String): Boolean {
    return try {
        readTree(json)
        true
    } catch (_: Exception) {
        false
    }
}
