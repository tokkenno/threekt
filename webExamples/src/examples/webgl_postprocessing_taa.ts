import { Box, Color, Mesh, MeshBasicMaterial, PerspectiveCamera, Scene, TextureFilter, WebGL2Context, WebGLRenderer } from 'threekt';
import brickUrl from './assets/brick_diffuse.jpg?url';
import { addSelect, addToggle, examplePanel } from './exampleControls';
import { exampleSupersampling } from './exampleSupersampling';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

export async function createExample(canvas: HTMLCanvasElement) {
    const texture = await loadImageTexture(brickUrl);
    texture.minFilter = texture.magFilter = TextureFilter.Nearest;
    texture.generateMipmaps = false;
    const camera = new PerspectiveCamera(70, 1, 1, 1000);
    camera.position.z = 330;
    const scene = new Scene();
    const geometry = new Box(120, 120, 120);
    const wireMaterial = new MeshBasicMaterial(Color.fromHex(0xffffff));
    wireMaterial.wireframe = true;
    const wire = new Mesh(geometry, wireMaterial);
    wire.position.x = -100;
    const textured = new Mesh(geometry, new MeshBasicMaterial(Color.fromHex(0xffffff), texture));
    textured.position.x = 100;
    scene.add(wire);
    scene.add(textured);
    const panel = examplePanel(canvas);
    const status = document.createElement('output');
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const accumulation = exampleSupersampling(renderer);
    let enabled = true, perFrame = 1, manualPause = false, last = 0, angle = 0;
    addToggle(panel, 'Temporal antialiasing', true, value => { enabled = value; accumulation.reset(); });
    addToggle(panel, 'Hold still', false, value => { manualPause = value; });
    addSelect(panel, 'Samples per frame', { 1: '1', 2: '2', 4: '4', 8: '8', 16: '16', 32: '32' }, '1', value => { perFrame = Number(value); });
    panel.append(status);
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const moving = !manualPause && Math.floor(seconds / 3.5) % 2 === 0;
            if (moving) {
                angle += Math.min(0.05, seconds - last) * 0.6;
                for (const mesh of [wire, textured]) mesh.rotation.set(angle * 0.5, angle, 0);
                accumulation.reset();
            }
            last = seconds;
            if (!enabled) {
                renderer.render(scene, camera);
                status.textContent = 'Antialiasing disabled';
                return;
            }
            const total = moving ? perFrame : 32;
            accumulation.resize(canvas.width, canvas.height);
            for (let i = 0; i < perFrame; i++) accumulation.sample(scene, camera, total);
            accumulation.present();
            status.textContent = moving ? 'Moving · spatial samples' : `Accumulating · ${accumulation.count} / 32`;
        },
        dispose() { panel.remove(); renderer.dispose(); },
    };
}
