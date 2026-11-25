import {
    Blending, Color, ImageTexture, Mesh, MeshBasicMaterial, PerspectiveCamera, PlaneGeometry,
    Scene, WebGL2Context, WebGLRenderer, Wrapping,
} from 'threekt';
import gridUrl from './assets/uv_grid_opengl.jpg?url';
import spriteJpegUrl from './assets/sprite0.jpg?url';
import spritePngUrl from './assets/sprite0.png?url';
import flareUrl from './assets/lensflare0.png?url';
import flareAlphaUrl from './assets/lensflare0_alpha.png?url';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

function backgroundTexture() {
    const canvas = document.createElement('canvas');
    canvas.width = canvas.height = 128;
    const context = canvas.getContext('2d')!;
    context.fillStyle = '#ddd';
    context.fillRect(0, 0, 128, 128);
    context.fillStyle = '#555';
    context.fillRect(0, 0, 64, 64);
    context.fillStyle = '#999';
    context.fillRect(32, 32, 32, 32);
    context.fillStyle = '#555';
    context.fillRect(64, 64, 64, 64);
    context.fillStyle = '#777';
    context.fillRect(96, 96, 32, 32);
    const map = new ImageTexture(canvas);
    map.wrapS = map.wrapT = Wrapping.Repeat;
    map.repeat.set(64, 32);
    return map;
}

function generateLabelMaterial(text: string) {
    const canvas = document.createElement('canvas');
    canvas.width = 128;
    canvas.height = 32;
    const context = canvas.getContext('2d')!;
    context.fillStyle = 'rgba(0, 0, 0, 0.95)';
    context.fillRect(0, 0, 128, 32);
    context.fillStyle = 'white';
    context.font = 'bold 12pt arial';
    context.fillText(text, 10, 22);
    const material = new MeshBasicMaterial(Color.fromHex(0xffffff), new ImageTexture(canvas));
    material.transparent = true;
    return material;
}

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(70, 1, 1, 1000);
    camera.position.z = 600;
    const scene = new Scene();

    const mapBg = backgroundTexture();
    const backgroundMaterial = new MeshBasicMaterial(Color.fromHex(0xffffff), mapBg);
    backgroundMaterial.depthWrite = false;
    const background = new Mesh(new PlaneGeometry(2, 2), backgroundMaterial);
    // Fill the camera view with a textured plane behind the swatches.
    background.position.z = -390;
    scene.add(background);

    const blendings = [
        ['No', Blending.None], ['Normal', Blending.Normal], ['Additive', Blending.Additive],
        ['Subtractive', Blending.Subtractive], ['Multiply', Blending.Multiply],
    ] as const;
    const labels = blendings.map(([name]) => generateLabelMaterial(name));
    const maps = await Promise.all([gridUrl, spriteJpegUrl, spritePngUrl, flareUrl, flareAlphaUrl].map(loadImageTexture));
    const imageGeometry = new PlaneGeometry(100, 100);
    const labelGeometry = new PlaneGeometry(100, 25);

    maps.forEach((map, row) => {
        const y = 300 - row * 150;
        blendings.forEach(([, blending], column) => {
            const x = (column - blendings.length / 2) * 110;
            const material = new MeshBasicMaterial(Color.fromHex(0xffffff), map);
            material.transparent = true;
            material.blending = blending;
            material.premultipliedAlpha = true;
            const image = new Mesh(imageGeometry, material);
            image.position.set(x, y, 0);
            scene.add(image);

            const label = new Mesh(labelGeometry, labels[column]);
            label.position.set(x, y - 75, 0);
            scene.add(label);
        });
    });

    const renderer = new WebGLRenderer(new WebGL2Context(canvas, false));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const halfHeight = Math.tan(camera.fov * Math.PI / 360) * (camera.position.z - background.position.z);
            background.scale.set(camera.aspect * halfHeight, halfHeight, 1);
            const time = Date.now() * 0.00025;
            mapBg.offset.set((time * -0.01 * mapBg.repeat.x) % 1, (time * -0.01 * mapBg.repeat.y) % 1);
            renderer.render(scene, camera);
        },
        dispose() {
            renderer.dispose();
        },
    };
}
