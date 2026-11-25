package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.materials.MeshPhongMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.color.ColorSpace
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.textures.Texture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MTLLoaderTest {
    @Test
    fun meshBatchesResolveNamedMaterialsAndShareDecodedTextures() {
        val materials = MTLLoader().parse(
            """
            newmtl Matte panel
            Kd 0.5 0.25 0
            Ks 0.1 0.1 0.1
            Ke 0 0 0.2
            Ns 42
            Tr 0.25
            map_Kd panel texture.jpg # comment
            newmtl Gloss
            d 1
            map_Kd panel texture.jpg
            """.trimIndent(),
        )
        assertEquals(listOf("panel texture.jpg"), materials.textureFiles.toList())
        val texture = object : Texture() {}
        materials.setTexture("panel texture.jpg", texture)
        val group = OBJLoader().setMaterials(materials).parse(
            """
            v 0 0 0
            v 1 0 0
            v 0 1 0
            vt 0 0
            vt 1 0
            vt 0 1
            usemtl Matte panel
            s off
            f 1/1 2/2 3/3
            usemtl Gloss
            s 1
            f 1/1 2/2 3/3
            usemtl Missing
            f 1 2 3
            """.trimIndent(),
        )
        val matte = (group.children[0] as Mesh).material as MeshPhongMaterial
        val gloss = (group.children[1] as Mesh).material as MeshPhongMaterial
        val fallback = (group.children[2] as Mesh).material as MeshPhongMaterial

        assertEquals(Color().setRGB(0.5, 0.25, 0.0, ColorSpace.SRGB), matte.color)
        assertEquals(Color().setRGB(0.1, 0.1, 0.1, ColorSpace.SRGB), matte.specular)
        assertEquals(Color().setRGB(0.0, 0.0, 0.2, ColorSpace.SRGB), matte.emissive)
        assertEquals(42.0, matte.shininess)
        assertEquals(0.75, matte.opacity)
        assertTrue(matte.transparent)
        assertTrue(matte.flatShading)
        assertFalse(gloss.flatShading)
        assertSame(texture, matte.map)
        assertSame(texture, gloss.map)
        assertNull(fallback.map)
    }
}
