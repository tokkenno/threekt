package com.aitorgf.threekt.renderers

import kotlin.js.JsExport

/** Clip-space depth convention used to build projection matrices and extract frustum planes. */
@JsExport
enum class CoordinateSystem { WebGL, WebGPU }