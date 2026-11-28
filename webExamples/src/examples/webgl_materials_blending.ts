import { Blending, Color, Mesh, MeshBasicMaterial, PerspectiveCamera, Scene } from 'threekt';
import { PlaneGeometry } from 'threekt/geometry';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import gridUrl from './assets/uv_grid_opengl.jpg?url';
import spriteJpegUrl from './assets/sprite0.jpg?url';
import spritePngUrl from './assets/sprite0.png?url';
import flareUrl from './assets/lensflare0.png?url';
import flareAlphaUrl from './assets/lensflare0_alpha.png?url';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

import { addBlendingBackground, generateLabelMaterial } from './blendingBackground';

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(70, 1, 1, 1000);
    camera.position.z = 600;
    const scene = new Scene();

    const updateBackground = addBlendingBackground(scene, camera);

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
            updateBackground();
            renderer.render(scene, camera);
        },
        dispose() {
            renderer.dispose();
        },
    };
}
