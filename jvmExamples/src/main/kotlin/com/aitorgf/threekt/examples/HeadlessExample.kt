package com.aitorgf.threekt.examples

import com.aitorgf.threekt.controls.Cursor
import com.aitorgf.threekt.controls.InputListener
import com.aitorgf.threekt.controls.InputSurface
import com.aitorgf.threekt.renderers.EglHeadlessContext
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO

internal class HeadlessExample(
    private val entry: ExampleEntry,
    private val width: Int,
    private val height: Int,
    private val seconds: Double,
    private val software: Boolean,
    private val angleDirectory: Path?,
) {
    fun save(screenshot: File) {
        System.setProperty("java.awt.headless", "true")
        EglHeadlessContext(width, height, software, angleDirectory).use { egl ->
            println("OpenGL ES renderer: ${egl.driver}")
            val example = entry.create(egl.context, OffscreenInput(width.toDouble(), height.toDouble()))
            try {
                example.resize(width, height, 1.0)
                example.render(seconds)
                val glError = egl.context.getError()
                check(glError == 0) { "Rendering failed (OpenGL ES error 0x${glError.toString(16)})." }
                val image = egl.readImage()
                val readError = egl.context.getError()
                check(readError == 0) { "Reading pixels failed (OpenGL ES error 0x${readError.toString(16)})." }
                check(ImageIO.write(image, "png", screenshot)) { "No PNG writer available." }
            } finally {
                example.dispose()
            }
        }
    }

    private class OffscreenInput(override val width: Double, override val height: Double) : InputSurface {
        override val left = 0.0
        override val top = 0.0
        override var cursor = Cursor.Auto
        override fun addInputListener(listener: InputListener) = Unit
        override fun removeInputListener(listener: InputListener) = Unit
    }
}
