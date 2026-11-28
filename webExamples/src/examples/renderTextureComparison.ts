import { type PerspectiveCamera,type Scene } from 'threekt';
import { type WebGLRenderer } from 'threekt/render-gl';

/** Draws two scenes with the same full-canvas projection and a gap between the scissor rectangles. */
export function renderTextureComparison(canvas: HTMLCanvasElement, renderer: WebGLRenderer, camera: PerspectiveCamera, scenes: [Scene, Scene]) {
    const width = canvas.clientWidth;
    const height = canvas.clientHeight;
    const middle = Math.floor(width / 2);
    renderer.setScissorTest(true);
    try {
        renderer.setScissor(0, 0, Math.max(0, middle - 2), height);
        renderer.render(scenes[0], camera);
        renderer.setScissor(middle, 0, Math.max(0, width - middle - 2), height);
        renderer.render(scenes[1], camera);
    } finally {
        renderer.setScissorTest(false);
    }
}
