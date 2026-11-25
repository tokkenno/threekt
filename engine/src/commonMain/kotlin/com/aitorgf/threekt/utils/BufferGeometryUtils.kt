package com.aitorgf.threekt.utils

import com.aitorgf.threekt.core.BufferAttribute
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.core.Int16BufferAttribute
import com.aitorgf.threekt.core.Uint8BufferAttribute
import kotlin.js.JsExport

/**
 * Geometry helpers from the Three.js addon `BufferGeometryUtils`. Functions that fail return null, as
 * upstream does after logging the reason; Threekt reports nothing. Draw groups are not supported yet.
 */
@JsExport
object BufferGeometryUtils {
    /**
     * Merges [geometries] into one geometry with the vertices of each in order. All of them must have the
     * same attributes and morph attributes, and either all or none must be indexed; indices are offset
     * by the preceding vertex counts. Returns null when they are incompatible.
     */
    fun mergeGeometries(geometries: Array<BufferGeometry>): BufferGeometry? {
        if (geometries.isEmpty()) return null
        val first = geometries[0]
        val isIndexed = first.index != null
        val attributesUsed = first.attributeNames.toSet()
        val morphAttributesUsed = first.morphAttributes.keys
        val attributes = linkedMapOf<String, MutableList<BufferAttribute>>()
        val morphAttributes = linkedMapOf<String, MutableList<List<BufferAttribute>>>()
        val morphTargetsRelative = first.morphTargetsRelative
        val merged = BufferGeometry()
        merged.morphTargetsRelative = morphTargetsRelative
        for (geometry in geometries) {
            if (isIndexed != (geometry.index != null)) return null
            val names = geometry.attributeNames
            if (names.toSet() != attributesUsed) return null
            for (name in names) attributes.getOrPut(name) { mutableListOf() }.add(geometry.getAttribute(name)!!)
            if (morphTargetsRelative != geometry.morphTargetsRelative) return null
            if (geometry.morphAttributes.keys != morphAttributesUsed) return null
            for ((name, targets) in geometry.morphAttributes) {
                if (targets.size != first.morphAttributes[name]!!.size) return null
                morphAttributes.getOrPut(name) { mutableListOf() }.add(targets)
            }
        }
        if (isIndexed) {
            var indexOffset = 0
            val mergedIndex = mutableListOf<Int>()
            for (geometry in geometries) {
                for (value in geometry.index!!) mergedIndex.add(value + indexOffset)
                indexOffset += geometry.getAttribute("position")?.count ?: 0
            }
            merged.setIndex(mergedIndex.toIntArray())
        }
        for ((name, list) in attributes) {
            merged.setAttribute(name, mergeAttributes(list.toTypedArray()) ?: return null)
        }
        for ((name, perGeometry) in morphAttributes) {
            val targetCount = perGeometry[0].size
            if (targetCount == 0) continue
            merged.morphAttributes[name] = List(targetCount) { target ->
                mergeAttributes(Array(perGeometry.size) { perGeometry[it][target] }) ?: return null
            }
        }
        return merged
    }

    /**
     * Concatenates [attributes] of the same type, item size and normalization into one attribute.
     * Returns null when they differ or are interleaved.
     */
    fun mergeAttributes(attributes: Array<BufferAttribute>): BufferAttribute? {
        if (attributes.isEmpty()) return null
        val first = attributes[0]
        if (attributes.any { it::class != first::class || it.itemSize != first.itemSize || it.normalized != first.normalized }) {
            return null
        }
        val result: BufferAttribute = when (first) {
            is Float32BufferAttribute -> {
                val array = FloatArray(attributes.sumOf { (it as Float32BufferAttribute).array.size })
                var offset = 0
                for (attribute in attributes) {
                    val source = (attribute as Float32BufferAttribute).array
                    source.copyInto(array, offset)
                    offset += source.size
                }
                Float32BufferAttribute(array, first.itemSize, first.normalized)
            }
            is Uint8BufferAttribute -> Uint8BufferAttribute(
                attributes.map { (it as Uint8BufferAttribute).array }.reduce { a, b -> a + b }, first.itemSize, first.normalized,
            )
            is Int16BufferAttribute -> Int16BufferAttribute(
                attributes.map { (it as Int16BufferAttribute).array }.reduce { a, b -> a + b }, first.itemSize, first.normalized,
            )
            else -> return null
        }
        result.gpuType = first.gpuType
        return result
    }
}
