import {
    Color, HemisphereLight, InstancedMesh, Matrix4, MeshPhongMaterial, PerspectiveCamera, Raycaster, Scene,
    Vector3,
} from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { IcosahedronGeometry } from 'threekt/geometry';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { addSliders } from './addSliders';
import { fitPerspective } from './fitPerspective';
import { raycastPointer } from './raycastPointer';

/** Port of Three.js examples/webgl_instancing_raycast.html. A slider replaces lil-gui. */
export function createExample(canvas: HTMLCanvasElement) {
    const amount = 10;
    const count = amount ** 3;
    const raycaster = new Raycaster();
    const color = Color.fromHex(0xffffff);
    const white = Color.fromHex(0xffffff);

    const camera = new PerspectiveCamera(60, 1, 0.1, 100);
    camera.position.set(amount, amount, amount);
    camera.lookAt(new Vector3(0, 0, 0));
    const scene = new Scene();
    const light = new HemisphereLight(Color.fromHex(0xffffff), Color.fromHex(0x888888), 3);
    light.position.set(0, 1, 0);
    scene.add(light);

    const geometry = new IcosahedronGeometry(0.5, 3);
    const material = new MeshPhongMaterial(Color.fromHex(0xffffff));
    const mesh = new InstancedMesh(geometry, material, count);
    let i = 0;
    const offset = (amount - 1) / 2;
    const matrix = new Matrix4();
    for (let x = 0; x < amount; x++) {
        for (let y = 0; y < amount; y++) {
            for (let z = 0; z < amount; z++) {
                matrix.setPosition(offset - x, offset - y, offset - z);
                mesh.setMatrixAt(i, matrix);
                mesh.setColorAt(i, color);
                i++;
            }
        }
    }
    scene.add(mesh);

    const removePanel = addSliders(canvas, [{
        label: 'Count', min: 0, max: count, step: 1, value: count,
        change: value => { mesh.count = value; },
    }]);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.enableDamping = true;
    controls.enableZoom = false;
    controls.enablePan = false;
    const pointer = raycastPointer(canvas);
    // The pointer starts outside the view, as in Three.js.
    pointer.pointer.set(1, 1);
    canvas.addEventListener('pointerleave', () => pointer.pointer.set(1, 1));

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            controls.update();
            raycaster.setFromCamera(pointer.pointer, camera);
            const intersection = raycaster.intersectObject(mesh);
            if (intersection.length > 0) {
                const instanceId = intersection[0].instanceId!;
                mesh.getColorAt(instanceId, color);
                if (color.equals(white)) {
                    mesh.setColorAt(instanceId, color.setHex(Math.floor(Math.random() * 0xffffff)));
                    mesh.instanceColor!.markUpdated();
                }
            }
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() {
            removePanel();
            pointer.dispose();
            controls.dispose();
            renderer.dispose();
        },
    };
}
