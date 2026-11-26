import { Euler, type Mesh, type PerspectiveCamera, Raycaster, Vector2, Vector3 } from 'threekt';
import { setPointer } from './raycastPointer';

export function dragMeshes(canvas: HTMLCanvasElement, camera: PerspectiveCamera, meshes: Mesh[], options: {
    begin(mesh: Mesh, event: PointerEvent): Mesh[];
    rotate?: boolean;
    end?(): void;
}) {
    const events = new AbortController();
    const signal = events.signal;
    const rotation = new Euler();
    const raycaster = new Raycaster();
    const pointer = new Vector2();
    const normal = new Vector3();
    const origin = new Vector3();
    const direction = new Vector3();
    const intersection = new Vector3();
    const initial = new Vector3();
    const oldCursor = canvas.style.cursor;
    const oldTouchAction = canvas.style.touchAction;
    canvas.style.touchAction = 'none';
    let drag: { id: number; targets: { mesh: Mesh; position: Vector3 }[]; x: number; y: number; rotate: boolean } | undefined;

    function project() {
        direction.set(pointer.x, pointer.y, 0.5).unproject(camera).sub(camera.position).normalize();
        const denominator = direction.dot(normal);
        if (Math.abs(denominator) < 1e-6) return false;
        const distance = intersection.copyFrom(origin).sub(camera.position).dot(normal) / denominator;
        intersection.copyFrom(camera.position).addScaledVector(direction, distance);
        return true;
    }

    canvas.addEventListener('pointerdown', event => {
        if (drag || (event.button !== 0 && !(options.rotate && event.button === 2))) return;
        setPointer(canvas, event, pointer);
        camera.updateMatrixWorld();
        raycaster.setFromCamera(pointer, camera);
        const hit = raycaster.intersectObjects(meshes, false)[0];
        if (!hit) return;
        const mesh = meshes.find(candidate => candidate === hit.object)!;
        const targets = options.begin(mesh, event);
        if (!targets.length) return;
        camera.getWorldDirection(normal);
        origin.copyFrom(hit.point);
        if (!project()) return;
        initial.copyFrom(intersection);
        drag = {
            id: event.pointerId, x: event.clientX, y: event.clientY, rotate: event.button === 2,
            targets: targets.map(mesh => ({ mesh, position: mesh.position.copy() })),
        };
        canvas.setPointerCapture(event.pointerId);
        canvas.style.cursor = 'grabbing';
        event.stopImmediatePropagation();
    }, { capture: true, signal });

    canvas.addEventListener('pointermove', event => {
        if (!drag || event.pointerId !== drag.id) return;
        if (drag.rotate) {
            for (const { mesh } of drag.targets) {
                rotation.setFromQuaternion(mesh.quaternion);
                rotation.x += (event.clientY - drag.y) * 0.02;
                rotation.y += (event.clientX - drag.x) * 0.02;
                mesh.quaternion.setFromEuler(rotation);
            }
            drag.x = event.clientX;
            drag.y = event.clientY;
        } else {
            setPointer(canvas, event, pointer);
            if (project()) {
                intersection.sub(initial);
                for (const { mesh, position } of drag.targets) mesh.position.copyFrom(position).add(intersection);
            }
        }
    }, { signal });

    canvas.addEventListener('lostpointercapture', event => {
        if (drag?.id !== event.pointerId) return;
        drag = undefined;
        canvas.style.cursor = oldCursor;
        options.end?.();
    }, { signal });
    if (options.rotate) canvas.addEventListener('contextmenu', event => event.preventDefault(), { signal });

    return () => {
        if (drag && canvas.hasPointerCapture(drag.id)) canvas.releasePointerCapture(drag.id);
        events.abort();
        canvas.style.cursor = oldCursor;
        canvas.style.touchAction = oldTouchAction;
    };
}
