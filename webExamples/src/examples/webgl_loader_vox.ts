import {
    Color, DirectionalLight, DomInputSurface, HemisphereLight, Mesh, MeshLambertMaterial, OrbitControls,
    PerspectiveCamera, Scene,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { VOXLoader } from 'threekt/loaders';
import model from './assets/vox/monu10.vox?url';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const response = await fetch(model);
    if (!response.ok) throw new Error('Could not load the MagicaVoxel model.');
    const [geometry] = new VOXLoader().parse(new Int8Array(await response.arrayBuffer()));
    const material = new MeshLambertMaterial();
    material.vertexColors = true;
    const monument = new Mesh(geometry, material);
    monument.scale.setScalar(0.0015);
    const scene = new Scene();
    scene.background = Color.fromHex(0x888888);
    scene.add(monument);
    scene.add(new HemisphereLight(Color.fromHex(0xcccccc), Color.fromHex(0x444444), 3));
    for (const [sign, intensity] of [[1, 2.5], [-1, 1.5]]) {
        const light = new DirectionalLight(Color.fromHex(0xffffff), intensity);
        light.position.set(sign * 1.5, sign * 3, sign * 2.5);
        scene.add(light);
    }
    const camera = new PerspectiveCamera(70, 1, 0.001, 10);
    camera.position.set(0.175, 0.075, 0.175);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 0.1;
    controls.maxDistance = 0.5;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); },
    };
}
