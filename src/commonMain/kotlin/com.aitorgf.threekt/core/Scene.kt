package com.aitorgf.threekt.core

import com.aitorgf.threekt.math.Color

@ExperimentalJsExport
@JsExport
class Scene: Object3() {
    /** If not null, sets the background used when rendering the scene, and is always rendered first. */
    var background: Background? = null

    /** If not null, this texture is set as the environment map for all physical materials in the scene. However, it's not possible to overwrite an existing texture assigned to MeshStandardMaterial.envMap. */
    var environment: Texture? = null

    /** A fog instance defining the type of fog that affects everything rendered in the scene. */
    var fog: Fog? = null

    /**  If set, then the renderer checks every frame if the scene and its com.aitorgf.threekt.objects needs matrix updates. When it isn't, then you have to maintain all matrices in the scene yourself. */
    var autoUpdate: Boolean = true

    override fun clone(recursive: Boolean): Camera {
        return Camera().copy(this, recursive)
    }

    override fun copy(source: Object3, recursive: Boolean): Scene {
        super.copy(source, recursive)

        if (source is Scene) {
            this.background = source.background?.clone() ?: this.background
            this.environment = source.environment?.clone() ?: this.environment
            this.fog = source.fog?.clone() ?: this.fog

            this.autoUpdate = source.autoUpdate;
            this.matrixAutoUpdate = source.matrixAutoUpdate;
        }

        return this
    }

    class Fog (
        var color: Color,
        var near: Double = 1.0,
        var far: Double = 1000.0
    ) {
        fun clone(): Fog {
            return Fog(this.color.clone(), this.near, this.far)
        }
    }

    class Background private constructor(
        val color: Color? = null,
        val texture: Texture? = null
    ) {
        val isColor: Boolean
            get() { return this.color != null }
        val isTexture: Boolean
            get() { return this.texture != null }

        fun clone(): Background {
            return Background(this.color, this.texture)
        }

        companion object {
            fun fromTexture(texture: Texture): Background {
                return Background(texture = texture)
            }

            fun fromColor(color: Color): Background {
                return Background(color = color)
            }
        }
    }
}
