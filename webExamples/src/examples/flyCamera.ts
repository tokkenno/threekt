import { Euler, type PerspectiveCamera, Vector3 } from 'threekt';
import { examplePanel } from './exampleControls';

export function flyCamera(canvas: HTMLCanvasElement, camera: PerspectiveCamera, speed: number, roll = false) {
    const events = new AbortController();
    const signal = events.signal;
    const keys = new Set<string>();
    const movement = new Vector3();
    const rotation = new Euler().setFromQuaternion(camera.quaternion, Euler.Order.YXZ);
    const oldTabIndex = canvas.getAttribute('tabindex');
    const oldTouchAction = canvas.style.touchAction;
    const panel = examplePanel(canvas);
    panel.textContent = roll
        ? 'Drag to steer. WASD to fly, R/F to rise or descend, Q/E to roll.'
        : 'Drag to look around. Use WASD or the arrow keys to move.';
    canvas.tabIndex = 0;
    canvas.style.touchAction = 'none';
    canvas.addEventListener('keydown', event => {
        if (/^(Key[WASDRFQE]|Arrow(Left|Right|Up|Down))$/.test(event.code)) {
            keys.add(event.code);
            event.preventDefault();
        }
    }, { signal });
    window.addEventListener('keyup', event => keys.delete(event.code), { signal });
    canvas.addEventListener('blur', () => keys.clear(), { signal });
    let pointer: number | undefined;
    let x = 0, y = 0;
    canvas.addEventListener('pointerdown', event => {
        if (pointer !== undefined || event.button !== 0) return;
        canvas.focus({ preventScroll: true });
        canvas.setPointerCapture(event.pointerId);
        pointer = event.pointerId;
        x = event.clientX;
        y = event.clientY;
    }, { signal });
    canvas.addEventListener('pointermove', event => {
        if (event.pointerId !== pointer) return;
        rotation.y -= (event.clientX - x) * 0.004;
        rotation.x = Math.max(-1.55, Math.min(1.55, rotation.x - (event.clientY - y) * 0.004));
        camera.quaternion.setFromEuler(rotation);
        x = event.clientX;
        y = event.clientY;
    }, { signal });
    canvas.addEventListener('lostpointercapture', event => {
        if (event.pointerId === pointer) pointer = undefined;
    }, { signal });
    let last: number | undefined;
    const pressed = (...codes: string[]) => Number(codes.some(code => keys.has(code)));
    return {
        panel,
        update(seconds: number) {
            const step = Math.min(0.05, Math.max(0, seconds - (last ?? seconds)));
            last = seconds;
            movement.set(pressed('KeyD', 'ArrowRight') - pressed('KeyA', 'ArrowLeft'),
                pressed('KeyR') - pressed('KeyF'), pressed('KeyS', 'ArrowDown') - pressed('KeyW', 'ArrowUp'));
            movement.normalize().applyQuaternion(camera.quaternion).multiplyScalar(step * speed);
            camera.position.add(movement);
            if (roll) {
                rotation.z += (pressed('KeyQ') - pressed('KeyE')) * step * Math.PI / 10;
                camera.quaternion.setFromEuler(rotation);
            }
        },
        dispose() {
            if (pointer !== undefined && canvas.hasPointerCapture(pointer)) canvas.releasePointerCapture(pointer);
            events.abort();
            panel.remove();
            canvas.style.touchAction = oldTouchAction;
            if (oldTabIndex === null) canvas.removeAttribute('tabindex');
            else canvas.setAttribute('tabindex', oldTabIndex);
        },
    };
}
