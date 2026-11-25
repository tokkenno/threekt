package com.aitorgf.threekt.helpers

import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.core.Usage
import com.aitorgf.threekt.geometries.Box
import com.aitorgf.threekt.materials.LineBasicMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.objects.Group
import com.aitorgf.threekt.objects.Mesh
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class BoxHelperTest {
    @Test
    fun boxHelperOutlinesTheWorldBoundsOfItsObject() {
        val mesh = Mesh(Box(2.0, 4.0, 6.0))
        val helper = BoxHelper(mesh)
        assertEquals(Color(0xffff00), assertIs<LineBasicMaterial>(helper.material).color)
        assertEquals(24, helper.geometry.index!!.size)
        val position = helper.geometry.getAttribute("position") as Float32BufferAttribute
        assertContentEquals(floatArrayOf(1f, 2f, 3f), position.array.copyOfRange(0, 3))
        assertContentEquals(floatArrayOf(-1f, -2f, -3f), position.array.copyOfRange(18, 21))
        assertEquals(sqrt14(), helper.geometry.boundingSphere!!.radius, 1e-12)

        val version = position.version
        mesh.position.x = 10.0
        helper.update()
        assertEquals(11f, position.array[0])
        assertEquals(version + 1, position.version)
    }

    @Test
    fun boxHelperCanFollowAnotherObject() {
        val group = Group()
        val mesh = Mesh(Box()).apply { position.y = 5.0 }
        group.add(mesh)
        val helper = BoxHelper(color = 0x474747)
        helper.setFromObject(group)
        assertSame(group, helper.`object`)
        val position = helper.geometry.getAttribute("position") as Float32BufferAttribute
        assertEquals(5.5f, position.array[1])
    }

    @Test
    fun setUsageReturnsTheAttribute() {
        val attribute = Float32BufferAttribute(FloatArray(3), 3)
        assertEquals(Usage.StaticDraw, attribute.usage)
        assertSame(attribute, attribute.setUsage(Usage.DynamicDraw))
        assertEquals(Usage.DynamicDraw, attribute.usage)
        assertEquals(0x88E8, Usage.DynamicDraw.glValue)
    }

    private fun sqrt14() = kotlin.math.sqrt(14.0)
}
