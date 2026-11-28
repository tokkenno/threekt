import { Color, ColorSpace, PerspectiveCamera, Scene, Vector3 } from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { addColoredPolyhedra } from './coloredPolyhedra';

export async function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    addColoredPolyhedra(scene, true);
    const views = [
        { rect: [0, 0, 0.5, 1], eye: [0, 300, 1800], fov: 30, color: [0.5, 0.5, 0.7] },
        { rect: [0.5, 0, 0.5, 0.5], eye: [0, 1800, 0], fov: 45, color: [0.7, 0.5, 0.5] },
        { rect: [0.5, 0.5, 0.5, 0.5], eye: [1400, 800, 1400], fov: 60, color: [0.5, 0.7, 0.7] },
    ].map(view => {
        const camera = new PerspectiveCamera(view.fov, 1, 1, 10000);
        camera.position.set(view.eye[0], view.eye[1], view.eye[2]);
        const [r, g, b] = view.color;
        return { ...view, camera, background: new Color(1, 1, 1).setRGB(r, g, b, ColorSpace.SRGB) };
    });
    views[1].camera.up.set(0, 0, 1);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    renderer.setScissorTest(true);
    const events = new AbortController();
    let mouseX = 0;
    canvas.addEventListener('pointermove', event => { mouseX = event.offsetX - canvas.clientWidth / 2; }, { signal: events.signal });
    const target = new Vector3();
    const clamp = (value: number, limit: number) => Math.max(-limit, Math.min(limit, value));

    return {
        render() {
            const width = canvas.clientWidth, height = canvas.clientHeight;
            if (width < 2 || height < 2) return;
            renderer.setSize(width, height, window.devicePixelRatio);
            views.forEach(({ rect, camera, background }, i) => {
                if (i === 0) camera.position.x = clamp(camera.position.x + mouseX * 0.05, 2000);
                if (i === 1) camera.position.x = clamp(camera.position.x - mouseX * 0.05, 2000);
                if (i === 2) camera.position.y = clamp(camera.position.y - mouseX * 0.05, 1600);
                camera.lookAt(i === 1 ? target.set(camera.position.x, 0, camera.position.z) : scene.position);
                const [x, y, w, h] = rect;
                const left = Math.floor(width * x), bottom = Math.floor(height * y);
                const viewWidth = Math.floor(width * (x + w)) - left;
                const viewHeight = Math.floor(height * (y + h)) - bottom;
                camera.aspect = viewWidth / viewHeight;
                camera.updateProjectionMatrix();
                renderer.setViewport(left, bottom, viewWidth, viewHeight);
                renderer.setScissor(left, bottom, viewWidth, viewHeight);
                scene.background = background;
                renderer.render(scene, camera);
            });
        },
        dispose() { events.abort(); renderer.dispose(); },
    };
}
