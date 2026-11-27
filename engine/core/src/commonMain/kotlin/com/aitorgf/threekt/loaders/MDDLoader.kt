package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.core.BufferAttribute
import com.aitorgf.threekt.core.Float32BufferAttribute
import kotlin.js.JsExport

/** Decodes a big-endian MDD point cache. Threekt returns frame times for use with its interpolants. */
@JsExport
class MDDLoader {
    class Result(val times: DoubleArray, val morphTargets: Array<BufferAttribute>)

    /** Returns absolute positions for each frame. Frame order matches the original mesh's vertices. */
    fun parse(data: ByteArray): Result {
        val reader = BinaryReader(data, false)
        val frames = reader.int()
        val points = reader.int()
        require(frames > 0 && points > 0 && points <= (data.size - 8) / 12 &&
            frames <= (data.size - 8) / (4 + points * 12)) { "Invalid MDD frame or point count." }
        val times = DoubleArray(frames) { reader.float().toDouble() }
        val targets = Array<BufferAttribute>(frames) { i ->
            val positions = FloatArray(points * 3) { reader.float() }
            Float32BufferAttribute(positions, 3).apply { name = "morph_$i" }
        }
        return Result(times, targets)
    }
}
