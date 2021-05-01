package core

@ExperimentalJsExport
@JsExport
data class CameraView(
    var enabled: Boolean = false,
    var fullWidth: Int = 1,
    var fullHeight: Int = 1,
    var offsetX:Int =  0,
    var offsetY: Int = 0,
    var width: Int = 1,
    var height: Int = 1
)
