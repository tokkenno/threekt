import {
    DomInputSurface, Euler, OrbitControls, PerspectiveCamera, Points, PointsMaterial, Scene, Vector3,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { PCDLoader } from 'threekt/loaders';
import asciiUrl from './assets/pcd/simple.pcd?url';
import binaryUrl from './assets/pcd/Zaghetto.pcd?url';
import binary64Url from './assets/pcd/Zaghetto_8bit.pcd?url';
import compressedUrl from './assets/pcd/pcl_logo.pcd?url';
import { fitPerspective } from './fitPerspective';

const files = [
    ['ASCII · simple.pcd', asciiUrl], ['Binario · Zaghetto.pcd', binaryUrl],
    ['Binario de 64 bits · Zaghetto_8bit.pcd', binary64Url], ['Binario comprimido · pcl_logo.pcd', compressedUrl],
];

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(30, 1, 0.01, 40);
    camera.position.z = 1;
    const scene = new Scene();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 0.5;
    controls.maxDistance = 10;

    const panel = document.createElement('aside');
    panel.className = 'example-panel';
    const select = document.createElement('select');
    select.setAttribute('aria-label', 'PCD file');
    files.forEach(([name], i) => select.add(new Option(name, String(i))));
    select.selectedIndex = 1;
    const size = document.createElement('input');
    size.type = 'range';
    size.min = '0.001';
    size.max = '0.01';
    size.step = '0.001';
    size.value = '0.005';
    size.setAttribute('aria-label', 'Point size');
    const color = document.createElement('input');
    color.type = 'color';
    color.setAttribute('aria-label', 'Point color');
    color.value = '#ffffff';
    const status = document.createElement('output');
    panel.append(select, size, color, status);
    canvas.parentElement?.append(panel);

    const events = new AbortController();
    const loader = new PCDLoader();
    const cache = new Map<number, Promise<Points>>();
    let current: Points | undefined;
    let selection = 0;
    function load(index: number) {
        const cached = cache.get(index);
        if (cached) return cached;
        const result = (async () => {
            const [name, url] = files[index];
            const response = await fetch(url, { signal: events.signal });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const points = loader.parse(new Int8Array(await response.arrayBuffer()));
            points.name = name;
            points.geometry.computeBoundingBox();
            const center = points.geometry.boundingBox!.getCenter(new Vector3());
            points.quaternion.setFromEuler(new Euler(Math.PI, 0, 0));
            points.position.set(-center.x, -center.y, -center.z).applyQuaternion(points.quaternion);
            return points;
        })();
        cache.set(index, result);
        void result.catch(() => cache.delete(index));
        return result;
    }

    const applyStyle = () => {
        const material = current?.material as PointsMaterial | undefined;
        if (material) {
            material.size = Number(size.value);
            material.color.setHex(Number.parseInt(color.value.slice(1), 16));
        }
    };
    const selectFile = async () => {
        const request = ++selection;
        status.textContent = 'Loading…';
        try {
            const points = await load(select.selectedIndex);
            if (events.signal.aborted || request !== selection) return;
            if (current) scene.remove(current);
            current = points;
            scene.add(points);
            applyStyle();
            status.textContent = '';
        } catch (error) {
            if (!events.signal.aborted && request === selection) status.textContent = `Could not load the file. ${String(error)}`;
        }
    };
    select.addEventListener('change', selectFile, { signal: events.signal });
    size.addEventListener('input', applyStyle, { signal: events.signal });
    color.addEventListener('input', applyStyle, { signal: events.signal });
    void selectFile();

    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() {
            events.abort();
            panel.remove();
            controls.dispose();
            renderer.dispose();
        },
    };
}
