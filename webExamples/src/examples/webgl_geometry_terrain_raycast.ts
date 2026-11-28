import { Color, Mesh, MeshBasicMaterial, PerspectiveCamera, Raycaster, Scene, Vector2, Vector3 } from 'threekt';
import { CylinderGeometry } from 'threekt/geometry';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { createTerrain } from './createTerrain';
import { fitPerspective } from './fitPerspective';
import { raycastPointer } from './raycastPointer';

export function createExample(canvas: HTMLCanvasElement) {
    const { mesh, centerHeight } = createTerrain();
    const scene = new Scene();
    scene.background = Color.fromHex(0xbfd1e5);
    scene.add(mesh);
    const camera = new PerspectiveCamera(60, 1, 10, 20000);
    camera.position.set(2000, centerHeight + 2200, 2100);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 1000;
    controls.maxDistance = 10000;
    controls.maxPolarAngle = Math.PI / 2;
    controls.target.y = centerHeight + 250;
    controls.update();

    const marker = new Mesh(new CylinderGeometry(0, 30, 115, 3, 1), new MeshBasicMaterial(Color.fromHex(0x6df5ff)));
    scene.add(marker);
    const raycaster = new Raycaster();
    const pointer = raycastPointer(canvas);
    const up = new Vector3(0, 1, 0);
    const previousPointer = new Vector2(Infinity, Infinity);
    const previousCamera = new Vector3(Infinity, Infinity, Infinity);
    const previousTarget = new Vector3(Infinity, Infinity, Infinity);
    let previousAspect = 0;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            if (!previousPointer.equals(pointer.pointer) || !previousCamera.equals(camera.position)
                || !previousTarget.equals(controls.target) || previousAspect !== camera.aspect) {
                previousPointer.copyFrom(pointer.pointer);
                previousCamera.copyFrom(camera.position);
                previousTarget.copyFrom(controls.target);
                previousAspect = camera.aspect;
                camera.updateMatrixWorld();
                raycaster.setFromCamera(pointer.pointer, camera);
                const hit = raycaster.intersectObject(mesh, false)[0];
                const normal = hit?.face?.normal;
                marker.visible = Boolean(normal);
                if (hit && normal) {
                    marker.position.copyFrom(hit.point).addScaledVector(normal, 57.5);
                    marker.quaternion.setFromUnitVectors(up, normal);
                }
            }
            renderer.render(scene, camera);
        },
        dispose() { pointer.dispose(); controls.dispose(); renderer.dispose(); },
    };
}
