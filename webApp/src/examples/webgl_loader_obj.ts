import {
    AmbientLight, Color, DirectionalLight, DomInputSurface, OBJLoader, OrbitControls,
    PerspectiveCamera, Scene, WebGL2Context, WebGLRenderer,
} from 'threekt';
import modelUrl from './assets/male02/male02.obj?url';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const response = await fetch(modelUrl);
    if (!response.ok) throw new Error(`No se pudo cargar el modelo OBJ (HTTP ${response.status}).`);
    const model = new OBJLoader().parse(await response.text());
    model.position.y = -0.95;
    model.scale.set(0.01, 0.01, 0.01);

    const camera = new PerspectiveCamera(45, 1, 0.1, 20);
    camera.position.z = 2.5;
    const scene = new Scene();
    scene.add(new AmbientLight(Color.fromHex(0xffffff)));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 2);
    light.position.set(1, 1, 2);
    scene.add(light);
    scene.add(model);

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.enableDamping = true;
    controls.minDistance = 2;
    controls.maxDistance = 5;

    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() {
            controls.dispose();
            renderer.dispose();
        },
    };
}
