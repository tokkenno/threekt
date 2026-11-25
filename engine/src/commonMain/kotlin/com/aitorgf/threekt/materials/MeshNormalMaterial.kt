package com.aitorgf.threekt.materials

import kotlin.js.JsExport

/**
 * Material that colors each fragment with its view-space normal mapped from `[-1, 1]` to RGB, as
 * Three.js `MeshNormalMaterial`. The output is not color-encoded, so it matches Three.js pixel for
 * pixel. It ignores lights and fog. Bump and normal maps are not supported yet.
 */
@JsExport
class MeshNormalMaterial : Material() {
    /** Whether each face uses its own geometric normal instead of interpolated vertex normals. */
    var flatShading = false
}
