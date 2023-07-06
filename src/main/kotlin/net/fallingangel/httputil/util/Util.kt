package net.fallingangel.httputil.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal val <reified T : Any> T.log: Logger
    inline get() = LoggerFactory.getLogger(T::class.java)

internal fun ObjectMapper.isValid(json: String): Boolean {
    return try {
        readTree(json)
        true
    } catch (_: Exception) {
        false
    }
}

@Suppress("unused")
typealias TypeReference<T> = TypeReference<T>
