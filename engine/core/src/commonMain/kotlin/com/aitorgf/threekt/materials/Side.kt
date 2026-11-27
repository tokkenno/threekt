package com.aitorgf.threekt.materials

import kotlin.js.JsExport

/** Which faces of a mesh are rendered. Front faces wind counterclockwise as seen by the camera. */
@JsExport
enum class Side { Front, Back, Double }
