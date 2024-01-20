package net.fallingangel.httputil.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
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

internal val jsonMapper by lazy {
    jsonMapper {
        val kotlinModule = kotlinModule {
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            serializationInclusion(JsonInclude.Include.USE_DEFAULTS)
        }
        addModule(kotlinModule)
        addModule(JavaTimeModule())
    }
}
