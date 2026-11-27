import {
    AmbientLight, BoxGeometry, Color, DirectionalLight, Euler, Mesh, MeshPhongMaterial, PerspectiveCamera, Scene,
    WebGL2Context, WebGLRenderer,
} from 'threekt';
import { dragMeshes } from './dragMeshes';
import { examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';

export function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    scene.background = Color.fromHex(0xf0f0f0);
    scene.add(new AmbientLight(Color.fromHex(0xaaaaaa)));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 2.2);
    light.position.set(0, 25, 50);
    scene.add(light);
    const camera = new PerspectiveCamera(70, 1, 0.1, 500);
    camera.position.z = 25;
    const geometry = new BoxGeometry(1, 1, 1);
    const rotation = new Euler();
    const meshes = Array.from({ length: 200 }, () => {
        const mesh = new Mesh(geometry, new MeshPhongMaterial(Color.fromHex(Math.floor(Math.random() * 0x1000000))));
        mesh.position.set(Math.random() * 30 - 15, Math.random() * 15 - 7.5, Math.random() * 20 - 10);
        mesh.quaternion.setFromEuler(rotation.set(Math.random() * 2 * Math.PI, Math.random() * 2 * Math.PI, Math.random() * 2 * Math.PI));
        mesh.scale.set(Math.random() * 2 + 1, Math.random() * 2 + 1, Math.random() * 2 + 1);
        scene.add(mesh);
        return mesh;
    });

    const selected = new Set<Mesh>();
    const panel = examplePanel(canvas);
    panel.textContent = 'Drag to move. Right-drag to rotate. Shift-click to select boxes and move them together.';
    const disposeDrag = dragMeshes(canvas, camera, meshes, {
        rotate: true,
        begin(mesh, event) {
            const material = mesh.material as MeshPhongMaterial;
            if (event.shiftKey) {
                if (selected.delete(mesh)) material.emissive.setHex(0);
                else {
                    selected.add(mesh);
                    material.emissive.setHex(0x224488);
                }
                return [];
            }
            return selected.has(mesh) ? [...selected] : [mesh];
        },
    });
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() { disposeDrag(); panel.remove(); renderer.dispose(); },
    };
}
