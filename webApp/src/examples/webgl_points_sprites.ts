import {
    Blending, BufferGeometry, Color, ColorSpace, Euler, Float32BufferAttribute, FogExp2,
    PerspectiveCamera, Points, PointsMaterial, Scene, WebGL2Context, WebGLRenderer,
} from 'threekt';
import snowflake1 from './assets/snowflake1.png?url';
import snowflake2 from './assets/snowflake2.png?url';
import snowflake3 from './assets/snowflake3.png?url';
import snowflake4 from './assets/snowflake4.png?url';
import snowflake5 from './assets/snowflake5.png?url';
import { addToggle } from './addToggle';
import { fitPerspective } from './fitPerspective';
import { followPointer } from './followPointer';
import { loadImageTexture } from './loadImageTexture';

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(75, 1, 1, 2000);
    camera.position.z = 1000;
    const scene = new Scene();
    scene.fog = new FogExp2(Color.fromHex(0), 0.0008);
    const positions = Float32Array.from({ length: 10000 * 3 }, () => Math.random() * 2000 - 1000);
    const geometry = new BufferGeometry().setAttribute('position', new Float32BufferAttribute(positions, 3));
    const parameters = [
        [1, 0.2, snowflake2, 20], [0.95, 0.1, snowflake3, 15], [0.9, 0.05, snowflake1, 10],
        [0.85, 0, snowflake5, 8], [0.8, 0, snowflake4, 5],
    ] as const;
    const layers = await Promise.all(parameters.map(async ([hue, saturation, url, size]) => {
        const texture = await loadImageTexture(url);
        const material = new PointsMaterial();
        material.size = size;
        material.map = texture;
        material.blending = Blending.Additive;
        material.depthTest = false;
        material.transparent = true;
        material.color.setHSL(hue, saturation, 0.5, ColorSpace.SRGB);
        const points = new Points(geometry, material);
        const rotation = new Euler(Math.random() * 6, Math.random() * 6, Math.random() * 6);
        points.quaternion.setFromEuler(rotation);
        return { points, material, texture, hue, saturation, rotation };
    }));
    layers.forEach(({ points }) => scene.add(points));

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const pointer = followPointer(canvas);
    const removeToggle = addToggle(canvas, 'Texturas de copos', true, (value) => {
        layers.forEach(({ material, texture }) => { material.map = value ? texture : null; });
    });

    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            pointer.update(camera, scene.position);
            const time = Date.now() * 0.00005;
            layers.forEach(({ points, material, hue, saturation, rotation }, i) => {
                rotation.y = time * (i < 4 ? i + 1 : -(i + 1));
                points.quaternion.setFromEuler(rotation);
                material.color.setHSL((hue + time) % 1, saturation, 0.5, ColorSpace.SRGB);
            });
            renderer.render(scene, camera);
        },
        dispose() {
            pointer.dispose();
            removeToggle();
            renderer.dispose();
        },
    };
}
