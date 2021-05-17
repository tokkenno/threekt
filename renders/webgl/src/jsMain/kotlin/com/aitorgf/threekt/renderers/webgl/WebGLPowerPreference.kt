package com.aitorgf.threekt.renderers.webgl

@ExperimentalJsExport
@JsExport
data class WebGLPowerPreference(val key: String) {
    companion object {
        val Default: WebGLPowerPreference = WebGLPowerPreference("default")
        val LowPower: WebGLPowerPreference = WebGLPowerPreference("low-power")
        val HighPerformance: WebGLPowerPreference = WebGLPowerPreference("high-performance")
    }
}
