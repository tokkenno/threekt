package com.aitorgf.threekt.lights

import com.aitorgf.threekt.cameras.OrthographicCamera

@ExperimentalJsExport
@JsExport
class DirectionalLightShadow: LightShadow(
    OrthographicCamera(-5.0, 5.0, 5.0, -5.0, 0.5, 500.0)
)
