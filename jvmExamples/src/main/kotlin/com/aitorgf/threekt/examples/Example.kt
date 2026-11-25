package com.aitorgf.threekt.examples

/**
 * A running example, the JVM counterpart of the object returned by `createExample` in the web app.
 * All methods are called on the thread that owns the OpenGL ES context.
 */
interface Example {
    /** Updates the view to a window of [width] × [height] logical pixels shown at [pixelRatio]. */
    fun resize(width: Int, height: Int, pixelRatio: Double)

    /** Draws a frame. [seconds] is the time since the example started. */
    fun render(seconds: Double)

    /** Releases the example's GPU resources. */
    fun dispose()
}
