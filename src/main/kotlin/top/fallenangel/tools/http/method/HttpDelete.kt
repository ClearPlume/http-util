package top.fallenangel.tools.http.method

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import java.net.URI

class HttpDelete() : HttpEntityEnclosingRequestBase() {
    constructor(uri: URI?) : this() {
        setURI(uri)
    }

    /**
     * @throws IllegalArgumentException if the uri is invalid.
     */
    constructor(uri: String) : this() {
        setURI(URI.create(uri))
    }

    override fun getMethod(): String {
        return METHOD_NAME
    }

    companion object {
        const val METHOD_NAME = "DELETE"
    }
}
