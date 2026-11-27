import { Color, InstancedMesh, Object3D, PerspectiveCamera, Scene, SphereGeometry } from 'threekt';
import { addSliders } from './addSliders';
import { followPointer } from './exampleControls';
import { loadExampleCubeMap } from './exampleCubeMap';
import { exampleDepthOfField } from './exampleDepthOfField';
import { addEnvironment, environmentMaterial } from './exampleEnvironment';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const uniforms = await loadExampleCubeMap('SwedishRoyalCastle');
    const scene = new Scene();
    const environment = addEnvironment(scene, uniforms, 2800, true);
    const camera = new PerspectiveCamera(70, 1, 1, 3000);
    camera.position.z = 200;
    const pointer = followPointer(canvas, camera, scene);
    const geometry = new SphereGeometry(1, 20, 10);
    const spheres = new InstancedMesh(geometry, environmentMaterial(uniforms, 0xffffff, 0, 1, true), 14 * 9 * 14);
    const transform = new Object3D();
    const tint = new Color(1, 1, 1);
    let i = 0;
    for (let x = 0; x < 14; x++) for (let y = 0; y < 9; y++) for (let z = 0; z < 14; z++) {
        transform.position.set((x - 7) * 200, (y - 4.5) * 200, (z - 7) * 200);
        transform.scale.setScalar(57);
        transform.updateMatrix();
        spheres.setMatrixAt(i, transform.matrix);
        spheres.setColorAt(i++, tint.setHSL((x + z) / 28, 0.75, 0.62));
    }
    scene.add(spheres);
    const effect = exampleDepthOfField(canvas, camera, false);
    const removeSliders = addSliders(canvas, [
        { label: 'Focus distance', min: 10, max: 3000, step: 10, value: 500, change: value => { effect.focus.value = value; } },
        { label: 'Aperture', min: 0, max: 10, step: 0.1, value: 5, change: value => { effect.aperture.value = value * 0.00001; } },
        { label: 'Maximum blur', min: 0, max: 0.03, step: 0.001, value: 0.011, change: value => { effect.maxBlur.value = value; } },
    ]);
    return {
        render() {
            if (!fitPerspective(canvas, camera, effect.renderer)) return;
            pointer.update();
            environment.position.copyFrom(camera.position);
            effect.render(scene);
        },
        dispose() { removeSliders(); pointer.dispose(); effect.renderer.dispose(); },
    };
}
