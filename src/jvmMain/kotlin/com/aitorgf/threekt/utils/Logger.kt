package com.aitorgf.threekt.utils

import java.util.logging.Logger as NativeLogger
import com.aitorgf.threekt.utils.LoggerLevel

actual class Logger actual constructor(context: kotlin.String) {
    private var logger: NativeLogger = NativeLogger.getLogger(context)

    actual fun message(level: com.aitorgf.threekt.utils.LoggerLevel, vararg params: kotlin.Any) {
        val msg = StringBuilder()
        params.forEach {
            when {
                it is String -> msg.append(it)
                else -> msg.append(it.toString())
            }
        }

        when {
            level.priority >= LoggerLevel.Error.priority -> this.logger.severe(msg.toString())
            level.priority >= LoggerLevel.Warn.priority -> this.logger.warning(msg.toString())
            level.priority >= LoggerLevel.Info.priority -> this.logger.info(msg.toString())
            else -> this.logger.finest(msg.toString())
        }
    }

    actual fun info(vararg params: kotlin.Any) { this.message(LoggerLevel.Info, *params) }

    actual fun warn(vararg params: kotlin.Any) { this.message(LoggerLevel.Warn, *params) }

    actual fun error(vararg params: kotlin.Any) { this.message(LoggerLevel.Error, *params) }
}
