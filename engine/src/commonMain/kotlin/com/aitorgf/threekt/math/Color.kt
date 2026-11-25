package com.aitorgf.threekt.math

import com.aitorgf.threekt.core.BufferAttribute
import com.aitorgf.threekt.math.color.ColorManagement
import com.aitorgf.threekt.math.color.ColorSpace
import com.aitorgf.threekt.math.color.CssColorNames
import com.aitorgf.threekt.math.color.HSL
import com.aitorgf.threekt.math.color.RGB
import com.aitorgf.threekt.math.color.SRGBEncoder
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.math.floor

/**
 * A mutable RGB color with components in [ColorManagement.workingColorSpace]. Hexadecimal colors and CSS
 * strings are interpreted as sRGB by default.
 */
@JsExport
data class Color(var r: Double, var g: Double, var b: Double) {
    /**
     * Creates a color from its default value, a packed hexadecimal RGB value, a CSS string, or another
     * color.
     */
    @JsExport.Ignore
    constructor() : this(1.0, 1.0, 1.0)

    /**
     * Creates a color from its default value, a packed hexadecimal RGB value, a CSS string, or another
     * color.
     */
    @JsName("fromHex")
    constructor(hex: Int) : this() { setHex(hex) }

    /**
     * Creates a color from its default value, a packed hexadecimal RGB value, a CSS string, or another
     * color.
     */
    @JsName("fromStyle")
    constructor(style: String) : this() { setStyle(style) }

    /**
     * Creates a color from its default value, a packed hexadecimal RGB value, a CSS string, or another
     * color.
     */
    @JsExport.Ignore
    constructor(color: Color) : this(color.r, color.g, color.b)

    /**
     * Sets this color from the supplied RGB components, CSS style, hexadecimal value, or other color.
     * Returns this color.
     */
    @JsExport.Ignore
    fun set(color: Color): Color = copy(color)

    /**
     * Sets this color from the supplied RGB components, CSS style, hexadecimal value, or other color.
     * Returns this color.
     */
    @JsExport.Ignore
    fun set(hex: Int): Color = setHex(hex)

    /**
     * Sets this color from the supplied RGB components, CSS style, hexadecimal value, or other color.
     * Returns this color.
     */
    @JsExport.Ignore
    fun set(style: String): Color = setStyle(style)

    /**
     * Sets this color from the supplied RGB components, CSS style, hexadecimal value, or other color.
     * Returns this color.
     */
    fun set(r: Double, g: Double, b: Double): Color = setRGB(r, g, b)

    /** Sets the colors's components to the given scalar value. Returns this instance. */
    fun setScalar(scalar: Double): Color = assign(scalar, scalar, scalar)
    private fun assign(r: Double, g: Double, b: Double): Color {
        this.r = r
        this.g = g
        this.b = b
        return this
    }

    /** Sets this color from a hexadecimal value. Returns this instance. */
    fun setHex(hex: Int, colorSpace: ColorSpace = ColorSpace.SRGB): Color =
        setRGB((hex shr 16 and 255) / 255.0, (hex shr 8 and 255) / 255.0, (hex and 255) / 255.0, colorSpace)

    /** Sets this color from RGB values. Returns this instance. */
    fun setRGB(r: Double, g: Double, b: Double, colorSpace: ColorSpace = ColorManagement.workingColorSpace): Color =
        ColorManagement.colorSpaceToWorking(assign(r, g, b), colorSpace)

    /** Sets this color from HSL values. Returns this instance. */
    fun setHSL(h: Double, s: Double, l: Double, colorSpace: ColorSpace = ColorManagement.workingColorSpace): Color {
        val hue = MathUtils.euclideanModulo(h, 1.0)
        val saturation = MathUtils.clamp(s, 0.0, 1.0)
        val lightness = MathUtils.clamp(l, 0.0, 1.0)
        val q = if (lightness <= 0.5) lightness * (1.0 + saturation) else lightness + saturation - lightness * saturation
        val p = 2.0 * lightness - q
        fun channel(offset: Double): Double {
            val t = MathUtils.euclideanModulo(hue + offset, 1.0)
            return when {
                t < 1.0 / 6.0 -> p + (q - p) * 6.0 * t
                t < 0.5 -> q
                t < 2.0 / 3.0 -> p + (q - p) * (2.0 / 3.0 - t) * 6.0
                else -> p
            }
        }
        return if (saturation == 0.0) setRGB(lightness, lightness, lightness, colorSpace)
        else setRGB(channel(1.0 / 3.0), channel(0.0), channel(-1.0 / 3.0), colorSpace)
    }

    /**
     * Sets this color from a supported CSS color string. Accepts hexadecimal values, named colors, and
     * comma-separated RGB or HSL values. The alpha component is ignored when present. Returns this color.
     */
    fun setStyle(style: String, colorSpace: ColorSpace = ColorSpace.SRGB): Color {
        val value = style.trim().lowercase()
        if (value.startsWith("#")) {
            val digits = value.drop(1)
            val hex = when (digits.length) {
                3 -> digits.map { "$it$it" }.joinToString("")
                6 -> digits
                else -> return this
            }.toIntOrNull(16) ?: return this
            return setHex(hex, colorSpace)
        }
        val match = CSS_FUNCTION.matchEntire(value) ?: return setColorName(value, colorSpace)
        val function = match.groupValues[1]
        val components = match.groupValues[2].split(',').map { it.trim() }
        if (components.size !in 3..4) return this
        val numbers = components.take(3).map { it.removeSuffix("%").toDoubleOrNull() ?: return this }
        return when (function) {
            "rgb", "rgba" -> {
                val channels = numbers.mapIndexed { i, number ->
                    val divisor = if (components[i].endsWith("%")) 100.0 else 255.0
                    MathUtils.clamp(number / divisor, 0.0, 1.0)
                }
                setRGB(channels[0], channels[1], channels[2], colorSpace)
            }
            "hsl", "hsla" -> setHSL(numbers[0] / 360.0, numbers[1] / 100.0, numbers[2] / 100.0, colorSpace)
            else -> this
        }
    }

    /**
     * Sets this color from a color name. Faster than [Color.setStyle] if you don't need the other
     * CSS-style formats. For convenience, the list of names is exposed in `Color.NAMES` as a hash. Returns
     * this instance.
     */
    fun setColorName(style: String, colorSpace: ColorSpace = ColorSpace.SRGB): Color =
        CssColorNames.codesByName[style.lowercase()]?.let { setHex(it, colorSpace) } ?: this

    /** Copies the supplied values into this instance and returns it. */
    @JsName("copyFrom")
    fun copy(color: Color): Color = assign(color.r, color.g, color.b)

    /**
     * Copies the given color into this color, and then converts this color from [ColorSpace.SRGB] to
     * [ColorSpace.LinearSRGB]. Returns this instance.
     */
    fun copySRGBToLinear(color: Color): Color =
        assign(SRGBEncoder.decode(color.r), SRGBEncoder.decode(color.g), SRGBEncoder.decode(color.b))

    /**
     * Copies the given color into this color, and then converts this color from [ColorSpace.LinearSRGB] to
     * [ColorSpace.SRGB]. Returns this instance.
     */
    fun copyLinearToSRGB(color: Color): Color =
        assign(SRGBEncoder.encode(color.r), SRGBEncoder.encode(color.g), SRGBEncoder.encode(color.b))

    /** Converts this color from [ColorSpace.SRGB] to [ColorSpace.LinearSRGB]. Returns this instance. */
    fun convertSRGBToLinear(): Color = copySRGBToLinear(this)

    /** Converts this color from [ColorSpace.LinearSRGB] to [ColorSpace.SRGB]. Returns this instance. */
    fun convertLinearToSRGB(): Color = copyLinearToSRGB(this)

    /** Returns the hexadecimal value of this color. */
    fun getHex(colorSpace: ColorSpace = ColorSpace.SRGB): Int {
        val color = ColorManagement.workingToColorSpace(copy(), colorSpace)
        fun channel(value: Double): Int = floor(MathUtils.clamp(value, 0.0, 1.0) * 255.0 + 0.5).toInt()
        return (channel(color.r) shl 16) or (channel(color.g) shl 8) or channel(color.b)
    }

    /** Returns the hexadecimal value of this color as a string (for example, 'FFFFFF'). */
    fun getHexString(colorSpace: ColorSpace = ColorSpace.SRGB): String = getHex(colorSpace).toString(16).padStart(6, '0')

    /** Converts the colors RGB values into the HSL format and stores them into the given target object. */
    fun getHSL(target: HSL, colorSpace: ColorSpace = ColorManagement.workingColorSpace): HSL {
        val color = ColorManagement.workingToColorSpace(copy(), colorSpace)
        val max = maxOf(color.r, color.g, color.b)
        val min = minOf(color.r, color.g, color.b)
        val delta = max - min
        val lightness = (min + max) * 0.5
        target.l = lightness
        if (delta == 0.0) {
            target.h = 0.0
            target.s = 0.0
            return target
        }
        target.s = delta / (if (lightness <= 0.5) max + min else 2.0 - max - min)
        target.h = when (max) {
            color.r -> (color.g - color.b) / delta + if (color.g < color.b) 6.0 else 0.0
            color.g -> (color.b - color.r) / delta + 2.0
            else -> (color.r - color.g) / delta + 4.0
        } / 6.0
        return target
    }

    /** Returns the RGB values of this color and stores them into the given target object. */
    fun getRGB(target: RGB, colorSpace: ColorSpace = ColorManagement.workingColorSpace): RGB {
        val color = ColorManagement.workingToColorSpace(copy(), colorSpace)
        target.r = color.r
        target.g = color.g
        target.b = color.b
        return target
    }

    /**
     * Returns a CSS color string in [colorSpace]. Uses `rgb(...)` for sRGB and `color(...)` for other
     * spaces.
     */
    fun getStyle(colorSpace: ColorSpace = ColorSpace.SRGB): String {
        val color = ColorManagement.workingToColorSpace(copy(), colorSpace)
        return if (colorSpace == ColorSpace.SRGB) {
            fun channel(value: Double): Int = floor(value * 255.0 + 0.5).toInt()
            "rgb(${channel(color.r)},${channel(color.g)},${channel(color.b)})"
        } else {
            fun fixed(value: Double): String {
                val scaled = floor(kotlin.math.abs(value) * 1000.0 + 0.5).toLong()
                val sign = if (value < 0.0) "-" else ""
                return "$sign${scaled / 1000}.${(scaled % 1000).toString().padStart(3, '0')}"
            }
            "color(${colorSpace.id} ${fixed(color.r)} ${fixed(color.g)} ${fixed(color.b)})"
        }
    }

    /**
     * Adds the given HSL values to this color's values. Internally, this converts the color's RGB values
     * to HSL, adds HSL and then converts the color back to RGB. Returns this instance.
     */
    fun offsetHSL(h: Double, s: Double, l: Double): Color {
        val hsl = getHSL(HSL())
        return setHSL(hsl.h + h, hsl.s + s, hsl.l + l)
    }

    /** Adds the RGB values of the given color to the RGB values of this color. Returns this instance. */
    fun add(color: Color): Color = addColors(this, color)

    /**
     * Adds the RGB values of the given colors and stores the result in this instance. Returns this
     * instance.
     */
    fun addColors(a: Color, b: Color): Color = assign(a.r + b.r, a.g + b.g, a.b + b.b)

    /** Adds the given scalar value to the RGB values of this color. Returns this instance. */
    fun addScalar(s: Double): Color = assign(r + s, g + s, b + s)

    /**
     * Subtracts the RGB values of the given color from the RGB values of this color. Returns this
     * instance.
     */
    fun sub(color: Color): Color = assign(maxOf(0.0, r - color.r), maxOf(0.0, g - color.g), maxOf(0.0, b - color.b))

    /**
     * Multiplies the RGB values of the given color with the RGB values of this color. Returns this
     * instance.
     */
    fun multiply(color: Color): Color = assign(r * color.r, g * color.g, b * color.b)

    /** Multiplies the given scalar value with the RGB values of this color. Returns this instance. */
    fun multiplyScalar(s: Double): Color = assign(r * s, g * s, b * s)

    /**
     * Linearly interpolates this color's RGB values toward the RGB values of the given color. The alpha
     * argument can be thought of as the ratio between the two colors, where `0.0` is this color and `1.0`
     * is the first argument. Returns this instance.
     */
    fun lerp(color: Color, alpha: Double): Color = lerpColors(this, color, alpha)

    /**
     * Linearly interpolates between the given colors and stores the result in this instance. The alpha
     * argument can be thought of as the ratio between the two colors, where `0.0` is the first and `1.0`
     * is the second color. Returns this instance.
     */
    fun lerpColors(a: Color, b: Color, alpha: Double): Color =
        assign(a.r + (b.r - a.r) * alpha, a.g + (b.g - a.g) * alpha, a.b + (b.b - a.b) * alpha)

    /**
     * Linearly interpolates this color's HSL values toward the HSL values of the given color. It differs
     * from [Color.lerp] by not interpolating straight from one color to the other, but instead going
     * through all the hues in between those two colors. The alpha argument can be thought of as the ratio
     * between the two colors, where 0.0 is this color and 1.0 is the first argument. Returns this
     * instance.
     */
    fun lerpHSL(color: Color, alpha: Double): Color {
        val a = getHSL(HSL())
        val b = color.getHSL(HSL())
        return setHSL(MathUtils.lerp(a.h, b.h, alpha), MathUtils.lerp(a.s, b.s, alpha), MathUtils.lerp(a.l, b.l, alpha))
    }

    /** Sets the color's RGB components from the given 3D vector. Returns this instance. */
    fun setFromVector3(vector: Vector3): Color = assign(vector.x, vector.y, vector.z)

    /** Transforms this color with the given 3x3 matrix. Returns this instance. */
    fun applyMatrix3(matrix: Matrix3): Color = setFromVector3(Vector3(r, g, b).applyMatrix3(matrix))

    /** Sets this color's RGB components from the given array. Returns this instance. */
    fun fromArray(array: DoubleArray, offset: Int = 0): Color = assign(array[offset], array[offset + 1], array[offset + 2])

    /** Writes the current RGB components to [array] starting at [offset] and returns that array. */
    fun toArray(array: DoubleArray = DoubleArray(3), offset: Int = 0): DoubleArray {
        array[offset] = r
        array[offset + 1] = g
        array[offset + 2] = b
        return array
    }

    /** Sets the components of this color from the given buffer attribute. Returns this instance. */
    fun fromBufferAttribute(attribute: BufferAttribute, index: Int): Color =
        setFromVector3(Vector3().fromBufferAttribute(attribute, index))


    companion object {
        val NAMES: Map<String, Int> get() = CssColorNames.codesByName
        private val CSS_FUNCTION = Regex("([a-z]+)\\((.*)\\)")
    }
}
