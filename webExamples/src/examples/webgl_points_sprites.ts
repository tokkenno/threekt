import {
    Blending, BufferGeometry, Color, ColorSpace, Euler, Float32BufferAttribute, FogExp2, PerspectiveCamera,
    Points, PointsMaterial, Scene, WebGL2Context, WebGLRenderer,
} from 'threekt';
import snowflake1 from './assets/snowflake1.png?url';
import snowflake2 from './assets/snowflake2.png?url';
import snowflake3 from './assets/snowflake3.png?url';
import snowflake4 from './assets/snowflake4.png?url';
import snowflake5 from './assets/snowflake5.png?url';
import { addToggle, examplePanel, followPointer, loadImageTexture } from './exampleControls';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(75, 1, 1, 2000);
    camera.position.z = 1000;
    const scene = new Scene();
    scene.fogExp2 = new FogExp2(Color.fromHex(0), 0.0008);

    const positions = Float32Array.from({ length: 10000 * 3 }, () => Math.random() * 2000 - 1000);
    const geometry = new BufferGeometry().setAttribute('position', new Float32BufferAttribute(positions, 3));
    const parameters = [
        [1.0, 0.2, 0.5, snowflake2, 20],
        [0.95, 0.1, 0.5, snowflake3, 15],
        [0.90, 0.05, 0.5, snowflake1, 10],
        [0.85, 0, 0.5, snowflake5, 8],
        [0.80, 0, 0.5, snowflake4, 5],
    ] as const;
    const layers = await Promise.all(parameters.map(async ([h, s, l, url, size]) => {
        const texture = await loadImageTexture(url);
        const material = new PointsMaterial();
        material.size = size;
        material.map = texture;
        material.blending = Blending.Additive;
        material.depthTest = false;
        material.transparent = true;
        material.color.setHSL(h, s, l, ColorSpace.SRGB);
        const points = new Points(geometry, material);
        const rotation = new Euler(Math.random() * 6, Math.random() * 6, Math.random() * 6);
        points.quaternion.setFromEuler(rotation);
        return { texture, material, points, rotation, h, s, l };
    }));
    for (const { points } of layers) scene.add(points);

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const pointer = followPointer(canvas, camera, scene);
    const panel = examplePanel(canvas);
    addToggle(panel, 'Textures', true, (value) => {
        for (const { material, texture } of layers) material.map = value ? texture : null;
    });
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            pointer.update();
            const time = seconds * 0.05;
            for (const [i, { material, points, rotation, h, s, l }] of layers.entries()) {
                rotation.y = time * (i < 4 ? i + 1 : -(i + 1));
                points.quaternion.setFromEuler(rotation);
                material.color.setHSL((h + time) % 1, s, l, ColorSpace.SRGB);
            }
            renderer.render(scene, camera);
        },
        dispose() {
            panel.remove();
            pointer.dispose();
            renderer.dispose();
        },
    };
}
