package com.aitorgf.threekt.renderers

import com.aitorgf.threekt.renderers.webgl.GLContext
import com.aitorgf.threekt.renderers.webgl.GLES30Context
import org.lwjgl.egl.EGL
import org.lwjgl.egl.EGL10.*
import org.lwjgl.egl.EGL12.*
import org.lwjgl.egl.EGL14.*
import org.lwjgl.egl.EGL15.EGL_OPENGL_ES3_BIT
import org.lwjgl.egl.EXTDeviceEnumeration.eglQueryDevicesEXT
import org.lwjgl.egl.EXTDeviceQuery.eglQueryDeviceStringEXT
import org.lwjgl.egl.EXTPlatformDevice.EGL_PLATFORM_DEVICE_EXT
import org.lwjgl.opengles.GLES
import org.lwjgl.opengles.GLES20.GL_RENDERER
import org.lwjgl.opengles.GLES20.glGetInteger
import org.lwjgl.opengles.GLES20.glGetString
import org.lwjgl.opengles.GLES30.GL_READ_FRAMEBUFFER
import org.lwjgl.opengles.GLES30.GL_READ_FRAMEBUFFER_BINDING
import org.lwjgl.opengles.GLES30.glBindFramebuffer
import org.lwjgl.system.Configuration
import org.lwjgl.system.JNI.callPPP
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAddressSafe
import org.lwjgl.system.Platform
import java.awt.image.BufferedImage
import java.nio.IntBuffer
import java.nio.file.Files
import java.nio.file.Path

/**
 * Fixed-size OpenGL ES 3 pbuffer for rendering without a window. On Linux uses Mesa EGL. On Windows
 * requires ANGLE with D3D11 support. [software] selects a Mesa software device or Windows WARP.
 * Create, render, read and close on the same thread. Dispose GPU resources before closing.
 *
 * Supply [angleDirectory] before the first use of LWJGL EGL or OpenGL ES in the process to specify
 * the directory containing ANGLE's libEGL.dll and libGLESv2.dll.
 */
class EglHeadlessContext(
    width: Int,
    height: Int,
    software: Boolean = false,
    angleDirectory: Path? = null,
) : AutoCloseable {
    private val owner = Thread.currentThread()
    private val display: Long
    private val surface: Long
    private val nativeContext: Long
    private val gles: GLES30Context
    private var closed = false

    /** Context for [WebGLRenderer]. Renderers must use the pbuffer's exact pixel dimensions. */
    val context: GLContext

    /** Driver name reported by OpenGL ES. */
    val driver: String

    init {
        require(width > 0 && height > 0 && width.toLong() * height <= Int.MAX_VALUE / 4)
        display = acquireDisplay(software, angleDirectory)
        var newSurface = EGL_NO_SURFACE
        var newContext = EGL_NO_CONTEXT
        var current = false
        try {
            requireEgl(eglBindAPI(EGL_OPENGL_ES_API), "eglBindAPI")
            val config = MemoryStack.stackPush().use { stack ->
                val configs = stack.mallocPointer(1)
                val count = stack.mallocInt(1)
                val attributes = stack.ints(
                    EGL_SURFACE_TYPE, EGL_PBUFFER_BIT, EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
                    EGL_RED_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_BLUE_SIZE, 8, EGL_ALPHA_SIZE, 8,
                    EGL_DEPTH_SIZE, 24, EGL_NONE,
                )
                requireEgl(eglChooseConfig(display, attributes, configs, count), "eglChooseConfig")
                check(count[0] > 0) { "EGL has no RGBA8 OpenGL ES 3 pbuffer configuration with depth." }
                configs[0]
            }
            newSurface = eglCreatePbufferSurface(display, config, intArrayOf(EGL_WIDTH, width, EGL_HEIGHT, height, EGL_NONE))
            requireEgl(newSurface != EGL_NO_SURFACE, "eglCreatePbufferSurface")
            newContext = eglCreateContext(display, config, EGL_NO_CONTEXT, intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE))
            requireEgl(newContext != EGL_NO_CONTEXT, "eglCreateContext")
            requireEgl(eglMakeCurrent(display, newSurface, newSurface, newContext), "eglMakeCurrent")
            current = true
            check(GLES.createCapabilities().GLES30) { "The EGL context does not provide OpenGL ES 3.0." }

            surface = newSurface
            nativeContext = newContext
            driver = checkNotNull(glGetString(GL_RENDERER))
            gles = GLES30Context(width, height)
            context = object : GLContext by gles {
                override fun resizeDrawingBuffer(width: Int, height: Int) {
                    require(width == drawingBufferWidth && height == drawingBufferHeight) {
                        "An EGL pbuffer has a fixed size. Create a new context to change its dimensions."
                    }
                }
            }
        } catch (error: Throwable) {
            if (current) {
                GLES.setCapabilities(null)
                eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)
            }
            if (newContext != EGL_NO_CONTEXT) eglDestroyContext(display, newContext)
            if (newSurface != EGL_NO_SURFACE) eglDestroySurface(display, newSurface)
            releaseDisplay(display)
            throw error
        }
    }

    /** Reads the pbuffer into an independent, top-down RGBA image. */
    fun readImage(): BufferedImage {
        checkCurrent()
        val readFramebuffer = glGetInteger(GL_READ_FRAMEBUFFER_BINDING)
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0)
        try {
            return gles.readImage()
        } finally {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, readFramebuffer)
        }
    }

    private fun checkCurrent() {
        check(Thread.currentThread() === owner) { "Use the EGL session on the thread that owns it." }
        check(!closed && eglGetCurrentContext() == nativeContext) { "The headless EGL context is no longer current." }
    }

    override fun close() {
        check(Thread.currentThread() === owner) { "Close the EGL session on the thread that owns it." }
        if (closed) return
        checkCurrent()
        GLES.setCapabilities(null)
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)
        eglDestroyContext(display, nativeContext)
        eglDestroySurface(display, surface)
        closed = true
        releaseDisplay(display)
    }
}

// EGL displays can be shared by sessions on different threads. eglTerminate invalidates the display
// for all of them, so only terminate after the last session has released it.
private val displayUsers = mutableMapOf<Long, Int>()
private var configuredAngleDirectory: Path? = null

private fun acquireDisplay(software: Boolean, angleDirectory: Path?): Long = synchronized(displayUsers) {
    val platform = Platform.get()
    if (platform == Platform.WINDOWS && angleDirectory != null) {
        val directory = angleDirectory.toAbsolutePath().normalize()
        check(configuredAngleDirectory == null || configuredAngleDirectory == directory) {
            "Only one set of ANGLE libraries can be used per JVM."
        }
        val egl = directory.resolve("libEGL.dll")
        val gles = directory.resolve("libGLESv2.dll")
        require(Files.isRegularFile(egl) && Files.isRegularFile(gles)) { "ANGLE DLLs not found in $directory." }
        Configuration.EGL_LIBRARY_NAME.set(egl.toString())
        Configuration.OPENGLES_LIBRARY_NAME.set(gles.toString())
        configuredAngleDirectory = directory
    } else {
        require(angleDirectory == null) { "ANGLE paths are supported on Windows only." }
    }
    check(platform == Platform.LINUX || platform == Platform.WINDOWS) { "Headless EGL supports Linux and Windows." }
    check(eglGetCurrentContext() == EGL_NO_CONTEXT) { "This thread already owns a current EGL context." }
    val extensions = eglQueryString(EGL_NO_DISPLAY, EGL_EXTENSIONS).orEmpty().split(' ')
    check("EGL_EXT_platform_base" in extensions) { "EGL_EXT_platform_base is required for headless EGL." }
    val display = MemoryStack.stackPush().use { stack ->
        when {
            platform == Platform.WINDOWS -> {
                check("EGL_ANGLE_platform_angle_d3d" in extensions) { "ANGLE with D3D11 support is required on Windows." }
                val attributes = stack.ints(
                    EGL_PLATFORM_ANGLE_TYPE_ANGLE, EGL_PLATFORM_ANGLE_TYPE_D3D11_ANGLE,
                    EGL_PLATFORM_ANGLE_DEVICE_TYPE_ANGLE,
                    if (software) EGL_PLATFORM_ANGLE_DEVICE_TYPE_D3D_WARP_ANGLE else EGL_PLATFORM_ANGLE_DEVICE_TYPE_HARDWARE_ANGLE,
                    EGL_NONE,
                )
                getPlatformDisplay(EGL_PLATFORM_ANGLE_ANGLE, EGL_DEFAULT_DISPLAY, attributes)
            }
            software -> {
                check(EGL.getCapabilities().eglQueryDevicesEXT != 0L && EGL.getCapabilities().eglQueryDeviceStringEXT != 0L &&
                    "EGL_EXT_platform_device" in extensions) { "Mesa EGL device enumeration is required for software rendering." }
                val count = stack.mallocInt(1)
                requireEgl(eglQueryDevicesEXT(null, count), "eglQueryDevicesEXT")
                val devices = stack.mallocPointer(count[0])
                requireEgl(eglQueryDevicesEXT(devices, count), "eglQueryDevicesEXT")
                val device = (0 until count[0]).map { devices[it] }.firstOrNull {
                    eglQueryDeviceStringEXT(it, EGL_EXTENSIONS).orEmpty().split(' ').contains("EGL_MESA_device_software")
                } ?: error("Mesa did not expose a software EGL device.")
                getPlatformDisplay(EGL_PLATFORM_DEVICE_EXT, device, null)
            }
            else -> {
                check("EGL_MESA_platform_surfaceless" in extensions) { "Mesa EGL surfaceless is required on Linux." }
                getPlatformDisplay(EGL_PLATFORM_SURFACELESS_MESA, EGL_DEFAULT_DISPLAY, null)
            }
        }
    }
    requireEgl(display != EGL_NO_DISPLAY, "eglGetPlatformDisplayEXT")
    val users = displayUsers[display] ?: 0
    if (users == 0) requireEgl(eglInitialize(display, null as IntBuffer?, null), "eglInitialize")
    displayUsers[display] = users + 1
    display
}

private fun getPlatformDisplay(platform: Int, nativeDisplay: Long, attributes: IntBuffer?): Long {
    val function = EGL.getCapabilities().eglGetPlatformDisplayEXT
    check(function != 0L) { "eglGetPlatformDisplayEXT is unavailable." }
    // The generated LWJGL wrapper rejects the zero native handle used for EGL_DEFAULT_DISPLAY.
    return callPPP(platform, nativeDisplay, memAddressSafe(attributes), function)
}

private fun releaseDisplay(display: Long) = synchronized(displayUsers) {
    val users = displayUsers.getValue(display)
    if (users == 1) {
        displayUsers.remove(display)
        eglTerminate(display)
    } else {
        displayUsers[display] = users - 1
    }
}

private fun requireEgl(success: Boolean, operation: String) {
    check(success) { "$operation failed (EGL error 0x${eglGetError().toString(16)})." }
}

// Platform extension constants not generated in LWJGL's EGL bindings.
private const val EGL_PLATFORM_SURFACELESS_MESA = 0x31DD
private const val EGL_PLATFORM_ANGLE_ANGLE = 0x3202
private const val EGL_PLATFORM_ANGLE_TYPE_ANGLE = 0x3203
private const val EGL_PLATFORM_ANGLE_TYPE_D3D11_ANGLE = 0x3208
private const val EGL_PLATFORM_ANGLE_DEVICE_TYPE_ANGLE = 0x3209
private const val EGL_PLATFORM_ANGLE_DEVICE_TYPE_HARDWARE_ANGLE = 0x320A
private const val EGL_PLATFORM_ANGLE_DEVICE_TYPE_D3D_WARP_ANGLE = 0x320B
