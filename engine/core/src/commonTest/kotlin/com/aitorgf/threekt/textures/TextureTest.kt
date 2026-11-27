package com.aitorgf.threekt.textures

import com.aitorgf.threekt.math.Matrix3
import com.aitorgf.threekt.math.Vector2
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextureTest {
    private class TestTexture : Texture()

    @Test
    fun defaultsMatchThreeJs() {
        val texture = TestTexture()
        assertEquals(Wrapping.ClampToEdge, texture.wrapS)
        assertEquals(Wrapping.ClampToEdge, texture.wrapT)
        assertEquals(1, texture.anisotropy)
        assertTrue(texture.matrixAutoUpdate)
        texture.updateMatrix()
        // Compare numerically: the default transform contains -0.0 translations.
        val identity = Matrix3().elements
        texture.matrix.elements.forEachIndexed { index, value -> assertEquals(identity[index], value, 0.0) }
    }

    @Test
    fun updateMatrixAppliesRepeatAndRotationAroundTheCenterBeforeOffset() {
        val texture = TestTexture()
        texture.offset.set(0.1, 0.2)
        texture.repeat.set(2.0, 3.0)
        texture.center.set(0.5, 0.5)
        texture.rotation = PI / 4
        texture.updateMatrix()
        assertEquals(Matrix3().setUvTransform(0.1, 0.2, 2.0, 3.0, PI / 4, 0.5, 0.5), texture.matrix)

        texture.repeat.set(1.0, 1.0)
        texture.offset.set(0.0, 0.0)
        texture.rotation = PI
        texture.updateMatrix()
        val uv = Vector2(0.75, 0.5).applyMatrix3(texture.matrix)
        assertEquals(0.25, uv.x, 1e-12)
        assertEquals(0.5, uv.y, 1e-12)
    }
}
