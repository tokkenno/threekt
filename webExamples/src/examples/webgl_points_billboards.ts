import {
    BufferGeometry, Color, ColorSpace, Float32BufferAttribute, FogExp2, PerspectiveCamera, Points,
    PointsMaterial, Scene, WebGL2Context, WebGLRenderer,
} from 'threekt';
import discUrl from './assets/disc.png?url';
import { addToggle, examplePanel, followPointer, loadImageTexture } from './exampleControls';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(55, 1, 2, 2000);
    camera.position.z = 1000;
    const scene = new Scene();
    scene.fogExp2 = new FogExp2(Color.fromHex(0), 0.001);

    const positions = Float32Array.from({ length: 10000 * 3 }, () => Math.random() * 2000 - 1000);
    const geometry = new BufferGeometry().setAttribute('position', new Float32BufferAttribute(positions, 3));
    const material = new PointsMaterial();
    const sprite = await loadImageTexture(discUrl);
    sprite.colorSpace = null;
    material.map = sprite;
    material.size = 35;
    material.alphaTest = 0.5;
    material.transparent = true;
    material.color.setHSL(1, 0.3, 0.7, ColorSpace.SRGB);
    scene.add(new Points(geometry, material));

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const pointer = followPointer(canvas, camera, scene);
    const panel = examplePanel(canvas);
    addToggle(panel, 'Atenuación por distancia', true, (value) => { material.sizeAttenuation = value; });
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            pointer.update();
            material.color.setHSL((seconds * 0.05) % 1, 0.5, 0.5);
            renderer.render(scene, camera);
        },
        dispose() {
            panel.remove();
            pointer.dispose();
            renderer.dispose();
        },
    };
}
