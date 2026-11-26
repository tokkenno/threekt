import {
    Color, ImageTexture, Mesh, MeshBasicMaterial, PerspectiveCamera, Scene, SphereGeometry, WebGL2Context,
    WebGLRenderer,
} from 'threekt';
import { examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';

export function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    scene.background = Color.fromHex(0xffffff);
    const camera = new PerspectiveCamera(60, 1, 1, 10000);
    camera.position.z = 200;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const panel = examplePanel(canvas);
    const pixels = document.createElement('canvas');
    pixels.width = pixels.height = 256;
    const image = pixels.getContext('2d')!;
    let frames = 0;
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const geometry = new SphereGeometry(50, Math.max(3, Math.floor(Math.random() * 64)),
                Math.max(2, Math.floor(Math.random() * 32)));
            image.fillStyle = `hsl(${Math.random() * 360} 80% 45%)`;
            image.fillRect(0, 0, 256, 256);
            const texture = new ImageTexture(pixels);
            const material = new MeshBasicMaterial();
            material.map = texture;
            material.wireframe = true;
            const mesh = new Mesh(geometry, material);
            scene.add(mesh);
            renderer.render(scene, camera);
            scene.remove(mesh);
            geometry.dispose();
            texture.dispose();
            material.dispose();
            panel.textContent = `Frames: ${++frames} · Cached programs: ${renderer.programCount}`;
        },
        dispose() { panel.remove(); renderer.dispose(); },
    };
}
