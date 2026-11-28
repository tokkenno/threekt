package com.aitorgf.threekt.examples

import com.aitorgf.threekt.render.gl.webgl.GLES30Context
import java.io.File
import javax.imageio.ImageIO
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengles.GLES
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
                GlfwInputSurface(window).use { input -> loop(window, input) }
            } finally {
                GLES.setCapabilities(null)
                GLFW.glfwDestroyWindow(window)
            }
        } finally {
            GLFW.glfwTerminate()
            GLFW.glfwSetErrorCallback(null)?.free()
        }
    }

    private fun loop(window: Long, input: GlfwInputSurface) {
        val (framebufferWidth, framebufferHeight) = framebufferSize(window)
        val context = GLES30Context(framebufferWidth, framebufferHeight)
        val example = entry.create(context, input)
        try {
            val start = System.nanoTime()
            var size = IntArray(0)
            while (!GLFW.glfwWindowShouldClose(window)) {
                GLFW.glfwPollEvents()
                val current = windowSize(window) + framebufferSize(window)
                if (!current.contentEquals(size) && current.none { it == 0 }) {
                    size = current
                    input.resize(current[0], current[1])
                    example.resize(current[0], current[1], current[2].toDouble() / current[0])
                }
                val seconds = (System.nanoTime() - start) / 1e9
                if (size.isNotEmpty()) example.render(seconds)
                if (screenshot != null && seconds >= screenshotDelay) {
                    ImageIO.write(context.readImage(includeAlpha = false), "png", screenshot)
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
}
