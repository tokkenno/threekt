package com.aitorgf.threekt.examples

import com.aitorgf.threekt.renderers.webgl.GLES30Context
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengles.GLES
import org.lwjgl.opengles.GLES20
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

/**
 * Shows [entry] in a GLFW window with an OpenGL ES 3.0 context until the window is closed. Must run on
 * the main thread. When [screenshot] is set, the window closes after [screenshotDelay] seconds and saves
 * the last frame there as a PNG.
 */
class ExampleWindow(
    private val entry: ExampleEntry,
    private val screenshot: File? = null,
    private val screenshotDelay: Double = 2.0,
) {
    fun run() {
        GLFWErrorCallback.createPrint(System.err).set()
        check(GLFW.glfwInit()) { "GLFW could not be initialized." }
        try {
            GLFW.glfwDefaultWindowHints()
            GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_ES_API)
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_EGL_CONTEXT_API)
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 0)
            GLFW.glfwWindowHint(GLFW.GLFW_SCALE_TO_MONITOR, GLFW.GLFW_TRUE)
            val window = GLFW.glfwCreateWindow(960, 640, "${entry.id} · Threekt", MemoryUtil.NULL, MemoryUtil.NULL)
            check(window != MemoryUtil.NULL) { "The OpenGL ES 3.0 window could not be created." }
            try {
                GLFW.glfwMakeContextCurrent(window)
                GLES.createCapabilities()
                GLFW.glfwSwapInterval(1)
                loop(window)
            } finally {
                GLES.setCapabilities(null)
                GLFW.glfwDestroyWindow(window)
            }
        } finally {
            GLFW.glfwTerminate()
            GLFW.glfwSetErrorCallback(null)?.free()
        }
    }

    private fun loop(window: Long) {
        val (framebufferWidth, framebufferHeight) = framebufferSize(window)
        val example = entry.create(GLES30Context(framebufferWidth, framebufferHeight))
        try {
            val start = System.nanoTime()
            var size = IntArray(0)
            while (!GLFW.glfwWindowShouldClose(window)) {
                GLFW.glfwPollEvents()
                val current = windowSize(window) + framebufferSize(window)
                if (!current.contentEquals(size) && current.none { it == 0 }) {
                    size = current
                    example.resize(current[0], current[1], current[2].toDouble() / current[0])
                }
                val seconds = (System.nanoTime() - start) / 1e9
                if (size.isNotEmpty()) example.render(seconds)
                if (screenshot != null && seconds >= screenshotDelay) {
                    save(screenshot, size[2], size[3])
                    GLFW.glfwSetWindowShouldClose(window, true)
                }
                GLFW.glfwSwapBuffers(window)
            }
        } finally {
            example.dispose()
        }
    }

    private fun windowSize(window: Long): IntArray = MemoryStack.stackPush().use { stack ->
        val width = stack.mallocInt(1)
        val height = stack.mallocInt(1)
        GLFW.glfwGetWindowSize(window, width, height)
        intArrayOf(width[0], height[0])
    }

    private fun framebufferSize(window: Long): IntArray = MemoryStack.stackPush().use { stack ->
        val width = stack.mallocInt(1)
        val height = stack.mallocInt(1)
        GLFW.glfwGetFramebufferSize(window, width, height)
        intArrayOf(width[0], height[0])
    }

    /** Reads the back buffer, which holds the frame just rendered, and writes it as a PNG. */
    private fun save(file: File, width: Int, height: Int) {
        val pixels = MemoryUtil.memAlloc(width * height * 4)
        try {
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels)
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            for (y in 0 until height) for (x in 0 until width) {
                val offset = ((height - 1 - y) * width + x) * 4
                val red = pixels.get(offset).toInt() and 0xff
                val green = pixels.get(offset + 1).toInt() and 0xff
                val blue = pixels.get(offset + 2).toInt() and 0xff
                image.setRGB(x, y, (red shl 16) or (green shl 8) or blue)
            }
            ImageIO.write(image, "png", file)
        } finally {
            MemoryUtil.memFree(pixels)
        }
    }
}
