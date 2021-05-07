package com.aitorgf.threekt.utils

class LoggerLevel private constructor(val priority: Int) {
    companion object {
        val Info = LoggerLevel(10)
        val Warn = LoggerLevel(20)
        val Error = LoggerLevel(30)
    }
}
