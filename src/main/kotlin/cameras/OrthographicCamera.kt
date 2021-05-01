package cameras

import core.Camera
import core.CameraView
import core.Object3
import math.MutableMatrix4

@ExperimentalJsExport
@JsExport
class OrthographicCamera(
    var left: Double = -1.0,
    var right: Double = 1.0,
    var top: Double = 1.0,
    var bottom: Double = -1.0,
    var near: Double = 0.1,
    var far: Double = 2000.0,
) : Camera() {
    var zoom: Double = 1.0
    var view: CameraView = CameraView()

    override fun copy(source: Object3, recursive: Boolean): OrthographicCamera {
        super.copy(source, recursive)

        if (source is OrthographicCamera) {
            this.left = source.left;
            this.right = source.right;
            this.top = source.top;
            this.bottom = source.bottom;
            this.near = source.near;
            this.far = source.far;

            this.zoom = source.zoom;
            this.view = source.view.copy();
        }

        return this
    }

    override fun updateProjectionMatrix() {
        val dx = (this.right - this.left) / (2 * this.zoom);
        val dy = (this.top - this.bottom) / (2 * this.zoom);
        val cx = (this.right + this.left) / 2;
        val cy = (this.top + this.bottom) / 2;

        var left = cx - dx;
        var right = cx + dx;
        var top = cy + dy;
        var bottom = cy - dy;

        if (this.view.enabled) {
            val scaleW = (this.right - this.left) / this.view.fullWidth / this.zoom;
            val scaleH = (this.top - this.bottom) / this.view.fullHeight / this.zoom;

            left += scaleW * this.view.offsetX;
            right = left + scaleW * this.view.width;
            top -= scaleH * this.view.offsetY;
            bottom = top - scaleH * this.view.height;
        }

        this.projectionMatrix.makeOrthographic(left, right, top, bottom, this.near, this.far);
        this.projectionMatrixInverse.set(this.projectionMatrix).invert();
    }

    fun MutableMatrix4.makeOrthographic(
        left: Double,
        right: Double,
        top: Double,
        bottom: Double,
        near: Double,
        far: Double
    ): MutableMatrix4 {
        val te = this.elements;
        val w = 1.0 / (right - left);
        val h = 1.0 / (top - bottom);
        val p = 1.0 / (far - near);

        val x = (right + left) * w;
        val y = (top + bottom) * h;
        val z = (far + near) * p;

        te[0] = 2 * w; te[4] = 0.0; te[8] = 0.0; te[12] = -x
        te[1] = 0.0; te[5] = 2 * h; te[9] = 0.0; te[13] = -y
        te[2] = 0.0; te[6] = 0.0; te[10] = -2 * p; te[14] = -z
        te[3] = 0.0; te[7] = 0.0; te[11] = 0.0; te[15] = 1.0

        return this
    }
}
