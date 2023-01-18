package top.clearplume.httputil.method

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase

class HttpDelete : HttpEntityEnclosingRequestBase() {
    override fun getMethod(): String {
        return METHOD_NAME
    }

    companion object {
        const val METHOD_NAME = "DELETE"
    }
}
