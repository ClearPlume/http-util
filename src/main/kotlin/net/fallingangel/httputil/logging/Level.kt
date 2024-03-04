package net.fallingangel.httputil.logging

enum class Level {
    /** 没有日志 */
    NONE,

    /**
     * 基本响应信息
     *
     * 示例:
     * ```
     * 200 OK (6 bytes)
     * ```
     */
    BASIC,

    /**
     * 所有日志
     */
    ALL
}
