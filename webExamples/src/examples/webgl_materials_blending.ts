import {
    Blending, Color, ImageTexture, Mesh, MeshBasicMaterial, PerspectiveCamera, PlaneGeometry,
    Scene, WebGL2Context, WebGLRenderer, Wrapping,
} from 'threekt';
import grid from './assets/uv_grid_opengl.jpg?url';
import spriteJpeg from './assets/sprite0.jpg?url';
import spritePng from './assets/sprite0.png?url';
import lensflare from './assets/lensflare0.png?url';
import lensflareAlpha from './assets/lensflare0_alpha.png?url';
import { loadImageTexture } from './exampleControls';
import { fitPerspective } from './fitPerspective';

function textureCanvas(width: number, height: number, draw: (context: CanvasRenderingContext2D) => void) {
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    draw(canvas.getContext('2d')!);
    return new ImageTexture(canvas);
}

export async function createExample(canvas: HTMLCanvasElement) {
    const maps = await Promise.all([grid, spriteJpeg, spritePng, lensflare, lensflareAlpha].map(loadImageTexture));
    const camera = new PerspectiveCamera(70, 1, 1, 1000);
    camera.position.z = 600;
    const scene = new Scene();
    const background = textureCanvas(128, 128, (context) => {
        context.fillStyle = '#ddd';
        context.fillRect(0, 0, 128, 128);
        context.fillStyle = '#555';
        context.fillRect(0, 0, 64, 64);
        context.fillRect(64, 64, 64, 64);
        context.fillStyle = '#999';
        context.fillRect(32, 32, 32, 32);
        context.fillStyle = '#777';
        context.fillRect(96, 96, 32, 32);
    });
    background.wrapS = background.wrapT = Wrapping.Repeat;
    background.repeat.set(64, 32);
    const white = Color.fromHex(0xffffff);
    const backdrop = new Mesh(new PlaneGeometry(), new MeshBasicMaterial(white, background));
    backdrop.position.z = -1;
    scene.add(backdrop);

    const modes = [
        ['No blending', Blending.None], ['Normal', Blending.Normal], ['Additive', Blending.Additive],
        ['Subtractive', Blending.Subtractive], ['Multiply', Blending.Multiply],
    ] as const;
    const tile = new PlaneGeometry(100, 100);
    const label = new PlaneGeometry(100, 25);
    const labels = modes.map(([name]) => {
        const map = textureCanvas(128, 32, (context) => {
            context.fillStyle = 'rgba(0, 0, 0, 0.95)';
            context.fillRect(0, 0, 128, 32);
            context.fillStyle = '#fff';
            context.font = 'bold 16px system-ui';
            context.fillText(name, 10, 22);
        });
        const material = new MeshBasicMaterial(white, map);
        material.transparent = true;
        return material;
    });
    for (const [row, map] of maps.entries()) {
        for (const [column, [, blending]] of modes.entries()) {
            const material = new MeshBasicMaterial(white, map);
            material.transparent = true;
            material.blending = blending;
            material.premultipliedAlpha = true;
            const sample = new Mesh(tile, material);
            const caption = new Mesh(label, labels[column]);
            const x = (column - 2) * 110;
            const y = (2 - row) * 150 + 20;
            sample.position.set(x, y, 0);
            caption.position.set(x, y - 75, 0);
            scene.add(sample);
            scene.add(caption);
        }
    }
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            // Widen the view on tall canvases so that all five columns remain visible.
            const fov = 2 * Math.atan(Math.max(Math.tan(35 * Math.PI / 180), 285 / (600 * camera.aspect))) * 180 / Math.PI;
            if (camera.fov !== fov) {
                camera.fov = fov;
                camera.updateProjectionMatrix();
            }
            const height = 2 * 601 * Math.tan(fov * Math.PI / 360);
            backdrop.scale.set(height * camera.aspect, height, 1);
            background.offset.set((-seconds * 0.16) % 1, (-seconds * 0.08) % 1);
            renderer.render(scene, camera);
        },
        dispose() { renderer.dispose(); },
    };
}
