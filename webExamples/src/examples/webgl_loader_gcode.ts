import {
    DomInputSurface, GCodeLoader, Group, LineSegments, OrbitControls, PerspectiveCamera, Scene,
    WebGL2Context, WebGLRenderer,
} from 'threekt';
import benchy from './assets/gcode/benchy.gcode?url';
import m82 from './assets/gcode/test_m82.gcode?url';
import m83 from './assets/gcode/test_m83.gcode?url';
import { addSelect, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';

const assets: Record<string, string> = { benchy, test_m82: m82, test_m83: m83 };

export async function createExample(canvas: HTMLCanvasElement) {
    const loader = new GCodeLoader();
    const scene = new Scene();
    const camera = new PerspectiveCamera(60, 1, 1, 1000);
    camera.position.z = 70;

    const read = async (asset: string, signal?: AbortSignal) => {
        const response = await fetch(assets[asset], { signal });
        if (!response.ok) throw new Error(`Could not load ${asset} (HTTP ${response.status}).`);
        const model = loader.parse(await response.text());
        if (asset === 'benchy') model.position.set(-100, -20, 100);
        return model;
    };
    let model = await read('benchy');
    scene.add(model);

    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 10;
    controls.maxDistance = 100;
    const panel = examplePanel(canvas);
    const status = document.createElement('p');
    status.setAttribute('role', 'status');
    status.hidden = true;
    let request: AbortController | undefined;
    function disposeModel(object: Group) {
        object.traverse(child => { if (child instanceof LineSegments) child.geometry.dispose(); });
    }
    addSelect(panel, 'Toolpath', { benchy: 'Benchy', test_m82: 'M82 (absolute)', test_m83: 'M83 (relative)' }, 'benchy', async asset => {
        request?.abort();
        const { signal } = request = new AbortController();
        status.hidden = true;
        try {
            const next = await read(asset, signal);
            if (signal.aborted) return;
            scene.remove(model);
            disposeModel(model);
            model = next;
            scene.add(model);
            controls.reset();
        } catch (error) {
            if (signal.aborted) return;
            status.textContent = String(error);
            status.hidden = false;
        }
    });
    panel.append(status);

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() { request?.abort(); panel.remove(); disposeModel(model); controls.dispose(); renderer.dispose(); },
    };
}
