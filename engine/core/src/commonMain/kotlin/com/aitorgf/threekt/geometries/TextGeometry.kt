package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.extras.core.Shape
import com.aitorgf.threekt.loaders.Font
import com.aitorgf.threekt.math.Vector2
import kotlin.js.JsExport

/**
 * Extruded 3D text, as the Three.js addon `TextGeometry`: the shapes of [text] from [Parameters.font],
 * extruded like [ExtrudeGeometry]. Without a font it is the default extruded square. Caps form draw
 * group 0 and sides group 1.
 */
@JsExport
class TextGeometry(text: String, parameters: Parameters = Parameters()) : ExtrudeGeometry(
    parameters.font?.generateShapes(text, parameters.size, parameters.direction) ?: DEFAULT_SHAPES,
    if (parameters.font != null) parameters else Options(),
) {
    /** Text settings, with the TextGeometry defaults for depth and bevels. */
    class Parameters : Options() {
        /** Typeface of the text, or null for the default extruded square. */
        var font: Font? = null
        /** Font size: the height of an em in scene units. */
        var size = 100.0
        /** Writing direction. */
        var direction = Font.Direction.Ltr

        init {
            depth = 50.0
            bevelThickness = 10.0
            bevelSize = 8.0
            bevelEnabled = false
        }
    }

    private companion object {
        val DEFAULT_SHAPES: Array<Shape>
            get() = arrayOf(Shape(arrayOf(Vector2(0.5, 0.5), Vector2(-0.5, 0.5), Vector2(-0.5, -0.5), Vector2(0.5, -0.5))))
    }
}
