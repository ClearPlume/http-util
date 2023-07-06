package net.fallingangel.httputil

data class Pair<K, V>(val key: K, val value: V) {
    companion object {
        @JvmStatic
        fun <K, V> of(key: K, value: V) = Pair(key, value)
    }
}
