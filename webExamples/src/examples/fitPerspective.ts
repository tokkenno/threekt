import type { PerspectiveCamera, WebGLRenderer } from 'threekt';

/** Synchronizes the perspective projection and drawing buffer with the canvas layout. */
export function fitPerspective(canvas: HTMLCanvasElement, camera: PerspectiveCamera, renderer: WebGLRenderer): boolean {
    const width = canvas.clientWidth;
    const height = canvas.clientHeight;
    if (width === 0 || height === 0) return false;

    const aspect = width / height;
    if (camera.aspect !== aspect) {
        camera.aspect = aspect;
        camera.updateProjectionMatrix();
    }
    renderer.setSize(width, height, window.devicePixelRatio);
    return true;
}
