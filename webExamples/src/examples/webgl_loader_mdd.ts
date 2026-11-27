import {
    Box, LinearInterpolant, Mesh, MeshNormalMaterial, PerspectiveCamera, Scene, Vector3, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { MDDLoader } from 'threekt/loaders';
import mdd from './assets/mdd/cube.mdd?url';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const response = await fetch(mdd);
    if (!response.ok) throw new Error('Could not load the MDD point cache.');
    const { times, morphTargets } = new MDDLoader().parse(new Int8Array(await response.arrayBuffer()));
    const geometry = new Box();
    geometry.setMorphAttribute('position', morphTargets);
    const mesh = new Mesh(geometry, new MeshNormalMaterial());
    const influences = mesh.morphTargetInfluences!;
    const weights = new Float64Array(times.length * times.length);
    times.forEach((_, index) => { weights[index * (times.length + 1)] = 1; });
    const frames = new LinearInterpolant(times, weights, times.length, influences);
    const scene = new Scene();
    scene.add(mesh);
    const camera = new PerspectiveCamera(45, 1, 0.1, 100);
    camera.position.set(8, 8, 8);
    camera.lookAt(new Vector3());
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            frames.evaluate(seconds % times[times.length - 1]);
            renderer.render(scene, camera);
        },
        dispose() { renderer.dispose(); },
    };
}
