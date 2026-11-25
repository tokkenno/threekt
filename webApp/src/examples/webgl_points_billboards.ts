import {
    BufferGeometry, Color, ColorSpace, Float32BufferAttribute, FogExp2, PerspectiveCamera,
    Points, PointsMaterial, Scene, WebGL2Context, WebGLRenderer,
} from 'threekt';
import discUrl from './assets/disc.png?url';
import { addToggle } from './addToggle';
import { fitPerspective } from './fitPerspective';
import { followPointer } from './followPointer';
import { loadImageTexture } from './loadImageTexture';

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(55, 1, 2, 2000);
    camera.position.z = 1000;
    const scene = new Scene();
    scene.fog = new FogExp2(Color.fromHex(0), 0.001);
    const positions = Float32Array.from({ length: 10000 * 3 }, () => Math.random() * 2000 - 1000);
    const geometry = new BufferGeometry().setAttribute('position', new Float32BufferAttribute(positions, 3));
    const material = new PointsMaterial();
    material.size = 35;
    material.map = await loadImageTexture(discUrl);
    material.alphaTest = 0.5;
    material.transparent = true;
    material.color.setHSL(1, 0.3, 0.7, ColorSpace.SRGB);
    scene.add(new Points(geometry, material));

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const pointer = followPointer(canvas);
    const removeToggle = addToggle(canvas, 'Atenuar con la distancia', true, (value) => {
        material.sizeAttenuation = value;
    });
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            pointer.update(camera, scene.position);
            const time = Date.now() * 0.00005;
            material.color.setHSL((1 + time) % 1, 0.5, 0.5);
            renderer.render(scene, camera);
        },
        dispose() {
            pointer.dispose();
            removeToggle();
            renderer.dispose();
        },
    };
}
