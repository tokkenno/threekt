package com.aitorgf.threekt.objects

import com.aitorgf.threekt.core.BufferGeometry

/**
 * Morph target state shared by [Mesh], [Line], and [Points], which Three.js duplicates in each class. [update]
 * creates one zero influence per morph target of the first morph attribute, keyed by the target names.
 */
internal class MorphTargets {
    var influences: DoubleArray? = null
    var dictionary: Map<String, Int>? = null

    fun update(geometry: BufferGeometry) {
        val morphAttribute = geometry.morphAttributes.values.firstOrNull() ?: return
        influences = DoubleArray(morphAttribute.size)
        dictionary = morphAttribute.withIndex().associate { (index, attribute) -> attribute.name.ifEmpty { index.toString() } to index }
    }

    /** Copies the influences and the dictionary of [source], as the `copy()` of the Three.js objects. */
    fun copy(source: MorphTargets) {
        influences = source.influences?.copyOf()
        dictionary = source.dictionary?.toMap()
    }
}
