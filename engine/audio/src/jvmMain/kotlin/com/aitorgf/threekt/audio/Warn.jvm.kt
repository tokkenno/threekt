package com.aitorgf.threekt.audio

internal actual fun warn(message: String) {
    System.err.println("THREE.$message")
}
