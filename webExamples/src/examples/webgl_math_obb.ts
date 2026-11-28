import {
    Color, DomInputSurface, Euler, HemisphereLight, BoxGeometry, Mesh, MeshBasicMaterial, MeshLambertMaterial, OBB, OrbitControls,
    PerspectiveCamera, Raycaster, Scene, Vector2, Vector3,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { fitPerspective } from './fitPerspective';
import { setPointer } from './raycastPointer';

/**
 * Port of Three.js examples/webgl_math_obb.html. Boxes that collide turn red; click a box to outline it.
 * Three.js keeps the boxes in `userData`; this port keeps them next to each mesh.
 */
export function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(70, 1, 1, 1000);
    camera.position.set(0, 0, 75);
    const scene = new Scene();
    scene.background = Color.fromHex(0xffffff);
    const raycaster = new Raycaster();
    const hemiLight = new HemisphereLight(Color.fromHex(0xffffff), Color.fromHex(0x222222), 4);
    hemiLight.position.set(1, 1, 1);
    scene.add(hemiLight);

    const size = new Vector3(10, 5, 6);
    const geometry = new BoxGeometry(size.x, size.y, size.z);
    // OBB on geometry level.
    const geometryObb = new OBB();
    geometryObb.halfSize.copyFrom(size).multiplyScalar(0.5);

    const objects: { mesh: Mesh; material: MeshLambertMaterial; obb: OBB; rotation: Euler }[] = [];
    for (let i = 0; i < 100; i++) {
        const material = new MeshLambertMaterial(Color.fromHex(0x00ff00));
        const mesh = new Mesh(geometry, material);
        mesh.matrixAutoUpdate = false;
        mesh.position.set(Math.random() * 80 - 40, Math.random() * 80 - 40, Math.random() * 80 - 40);
        const rotation = new Euler(Math.random() * 2 * Math.PI, Math.random() * 2 * Math.PI, Math.random() * 2 * Math.PI);
        mesh.scale.set(Math.random() + 0.5, Math.random() + 0.5, Math.random() + 0.5);
        scene.add(mesh);
        // Bounding volume on object level, reflecting the current world transform.
        objects.push({ mesh, material, obb: new OBB(), rotation });
    }

    const hitbox = new Mesh(geometry, Object.assign(new MeshBasicMaterial(Color.fromHex(0x000000)), { wireframe: true }));

    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.enableDamping = true;

    const mouse = new Vector2();
    const intersectionPoint = new Vector3();
    let downX = 0;
    let downY = 0;
    const events = new AbortController();
    canvas.addEventListener('pointerdown', event => { downX = event.clientX; downY = event.clientY; }, { signal: events.signal });
    canvas.addEventListener('click', event => {
        // Ignore clicks that end an orbit drag.
        if (Math.hypot(event.clientX - downX, event.clientY - downY) > 4) return;
        setPointer(canvas, event as PointerEvent, mouse);
        raycaster.setFromCamera(mouse, camera);
        const ray = raycaster.ray;
        let nearest: Mesh | null = null;
        let nearestDistance = Infinity;
        for (const { mesh, obb } of objects) {
            if (obb.intersectRay(ray, intersectionPoint) !== null) {
                const distance = ray.origin.distanceTo(intersectionPoint);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = mesh;
                }
            }
        }
        if (nearest) nearest.add(hitbox);
        else hitbox.parent?.remove(hitbox);
    }, { signal: events.signal });

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    let last: number | undefined;
    return {
        render(seconds: number) {
            const delta = last === undefined ? 0 : seconds - last;
            last = seconds;
            controls.update();
            for (const object of objects) {
                object.rotation.x += delta * Math.PI * 0.20;
                object.rotation.y += delta * Math.PI * 0.1;
                object.mesh.quaternion.setFromEuler(object.rotation);
                object.mesh.updateMatrix();
                object.mesh.updateMatrixWorld();
                object.obb.copy(geometryObb);
                object.obb.applyMatrix4(object.mesh.matrixWorld);
                object.material.color.setHex(0x00ff00);
            }
            // Collision detection.
            for (let i = 0; i < objects.length; i++) {
                for (let j = i + 1; j < objects.length; j++) {
                    if (objects[i].obb.intersectsOBB(objects[j].obb)) {
                        objects[i].material.color.setHex(0xff0000);
                        objects[j].material.color.setHex(0xff0000);
                    }
                }
            }
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() {
            events.abort();
            controls.dispose();
            renderer.dispose();
        },
    };
}
