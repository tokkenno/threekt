package com.aitorgf.threekt.utils

import kotlin.reflect.KClass

actual class Logger actual constructor(
    context: String
) {
    val context = context

    actual fun message(level: LoggerLevel, vararg params: Any) {
        when {
            level.priority >= LoggerLevel.Error.priority -> console.error(*params)
            level.priority >= LoggerLevel.Warn.priority -> console.warn(*params)
            level.priority >= LoggerLevel.Info.priority -> console.info(*params)
            else -> console.log(*params)
        }
    }

    actual fun info(vararg params: Any) { this.message(LoggerLevel.Info, *params) }
    actual fun warn(vararg params: Any) { this.message(LoggerLevel.Warn, *params) }
    actual fun error(vararg params: Any) { this.message(LoggerLevel.Error, *params) }
}
