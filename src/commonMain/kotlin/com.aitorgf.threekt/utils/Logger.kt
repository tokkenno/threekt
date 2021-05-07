package com.aitorgf.threekt.utils

expect class Logger(
    context: String
) {
    fun message(level: LoggerLevel, vararg params: Any)
    fun info(vararg params: Any)
    fun warn(vararg params: Any)
    fun error(vararg params: Any)
}
