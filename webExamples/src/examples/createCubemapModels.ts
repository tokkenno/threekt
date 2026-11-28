import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { Mesh, PerspectiveCamera, Scene, Uniforms, type BufferGeometry } from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { loadExampleCubeMap } from './exampleCubeMap';
import { addEnvironment, environmentMaterial } from './exampleEnvironment';
import { fitPerspective } from './fitPerspective';

export async function createCubemapModels(canvas: HTMLCanvasElement, geometry: BufferGeometry, refract: boolean) {
    const folder = refract ? 'Park3Med' : 'SwedishRoyalCastle';
    const environment = await loadExampleCubeMap(folder);
    const scene = new Scene();
    const dome = addEnvironment(scene, environment, 60);
    geometry.computeBoundingBox();
    geometry.center();
    const box = geometry.boundingBox!;
    const scale = 2.6 / (box.max.y - box.min.y);
    const variants = refract
        ? [[0xffffff, 0.98, 1], [0xccfffd, 0.985, 1], [0xccddff, 0.98, 0.9]]
        : [[0xffffff, 0, 1], [0xfff700, 0.95, 1], [0xffaa00, 0, 0.3]];
    // Each material shares the face textures but owns its refraction and tint parameters.
    for (const [i, [tint, ratio, mix]] of variants.entries()) {
        const uniforms = new Uniforms();
        for (const name of environment.names) uniforms.set(name, environment.get(name)!);
        const mesh = new Mesh(geometry, environmentMaterial(uniforms, tint, ratio, mix));
        mesh.scale.setScalar(scale);
        mesh.position.x = (i - 1) * 3;
        scene.add(mesh);
    }
    const camera = new PerspectiveCamera(50, 1, 0.1, 100);
    camera.position.set(0, 0.8, 10);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 4;
    controls.maxDistance = 24;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            dome.position.copyFrom(camera.position);
            renderer.render(scene, camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); },
    };
}
