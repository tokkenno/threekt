import {
    AmbientLight, BoxGeometry, Color, DirectionalLight, Euler, GridHelper, Mesh, MeshBasicMaterial, MeshPhongMaterial,
    PerspectiveCamera, PlaneGeometry, Raycaster, Scene, Vector2, Vector3, WebGL2Context, WebGLRenderer,
} from 'threekt';
import textureUrl from './assets/square-outline-textured.png?url';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';
import { setPointer } from './raycastPointer';

export async function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    scene.background = Color.fromHex(0xf0f0f0);
    const box = new BoxGeometry(50, 50, 50);
    const previewMaterial = new MeshBasicMaterial(Color.fromHex(0xff0000));
    previewMaterial.opacity = 0.5;
    previewMaterial.transparent = true;
    const preview = new Mesh(box, previewMaterial);
    preview.visible = false;
    scene.add(preview);
    const voxelMaterial = new MeshPhongMaterial(Color.fromHex(0xfeb74c));
    voxelMaterial.specular.setHex(0);
    voxelMaterial.map = await loadImageTexture(textureUrl);
    scene.add(new GridHelper(1000, 20));
    const plane = new Mesh(new PlaneGeometry(1000, 1000), new MeshBasicMaterial());
    plane.quaternion.setFromEuler(new Euler(-Math.PI / 2, 0, 0));
    plane.visible = false;
    scene.add(plane);
    const objects = [plane];

    scene.add(new AmbientLight(Color.fromHex(0x606060), 3));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 3);
    light.position.set(1, 0.75, 0.5).normalize();
    scene.add(light);
    const camera = new PerspectiveCamera(45, 1, 1, 10000);
    camera.position.set(500, 800, 1300);
    camera.lookAt(scene.position);
    const raycaster = new Raycaster();
    const pointer = new Vector2();
    const point = new Vector3();
    const events = new AbortController();
    const intersect = (event: PointerEvent) => {
        setPointer(canvas, event, pointer);
        raycaster.setFromCamera(pointer, camera);
        return raycaster.intersectObjects(objects, false)[0];
    };
    const snap = (hit: ReturnType<typeof intersect>) => point.set(
        hit.point.x + (hit.face?.normal.x ?? 0),
        hit.point.y + (hit.object === plane ? 1 : hit.face?.normal.y ?? 0),
        hit.point.z + (hit.object === plane ? 0 : hit.face?.normal.z ?? 0),
    ).divideScalar(50).floor().multiplyScalar(50).addScalar(25);
    const label = document.createElement('aside');
    label.className = 'example-panel';
    label.textContent = 'Click to add a block. Shift-click to remove one.';
    canvas.parentElement?.append(label);
    canvas.addEventListener('pointermove', event => {
        const hit = intersect(event);
        preview.visible = !!hit;
        if (hit) {
            const position = snap(hit);
            preview.position.set(position.x, position.y, position.z);
        }
    }, { signal: events.signal });
    canvas.addEventListener('pointerleave', () => { preview.visible = false; }, { signal: events.signal });
    canvas.addEventListener('pointerdown', event => {
        if (event.button !== 0) return;
        const hit = intersect(event);
        if (!hit) return;
        if (event.shiftKey) {
            if (hit.object === plane) return;
            scene.remove(hit.object);
            const index = objects.indexOf(hit.object as Mesh);
            if (index >= 0) objects.splice(index, 1);
        } else {
            const voxel = new Mesh(box, voxelMaterial);
            const position = snap(hit);
            voxel.position.set(position.x, position.y, position.z);
            objects.push(voxel);
            scene.add(voxel);
        }
    }, { signal: events.signal });
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() { events.abort(); label.remove(); renderer.dispose(); },
    };
}
