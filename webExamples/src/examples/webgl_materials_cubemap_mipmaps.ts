import {
    DomInputSurface, ImageTexture, Mesh, OrbitControls, PerspectiveCamera, Scene,
    SphereGeometry, Uniform, Uniforms
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { cubeFaces } from './exampleCubeMap';
import { addSliders } from './addSliders';
import { examplePanel } from './exampleControls';
import { environmentMaterial } from './exampleEnvironment';
import { fitPerspective } from './fitPerspective';

const assets = import.meta.glob<string>('./assets/cube/angus/*.jpg', { eager: true, query: '?url', import: 'default' });

export async function createExample(canvas: HTMLCanvasElement) {
    const automatic = new Uniforms(), manual = new Uniforms();
    await Promise.all(cubeFaces.map(async (face, index) => {
        const levels = await Promise.all(Array.from({ length: 9 }, async (_, level) => {
            const image = new Image();
            image.src = assets[`./assets/cube/angus/cube_m0${level}_c0${index}.jpg`];
            await image.decode();
            return image;
        }));
        const texture = new ImageTexture(levels[0]);
        texture.mipmaps = levels;
        texture.generateMipmaps = false;
        manual.set(face, new Uniform(texture));
        automatic.set(face, new Uniform(new ImageTexture(levels[0])));
    }));
    const scene = new Scene();
    const geometry = new SphereGeometry(1, 128, 128);
    for (const [x, uniforms] of [[-1.1, automatic], [1.1, manual]] as const) {
        const mesh = new Mesh(geometry, environmentMaterial(uniforms));
        mesh.position.x = x;
        scene.add(mesh);
    }
    const camera = new PerspectiveCamera(50, 1, 0.1, 1000);
    camera.position.z = 6;
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.maxDistance = 250;
    const panel = examplePanel(canvas);
    panel.textContent = 'Left: generated mipmaps. Right: supplied mipmaps.';
    addSliders(canvas, [{ label: 'Distance', min: 6, max: 200, step: 1, value: 6, change: value => {
        camera.position.normalize().multiplyScalar(value);
    } }], panel);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() { panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
