import type { PerspectiveCamera, Vector3 } from 'threekt';

/** Eases the camera toward the primary pointer using offsets from the center of the canvas. */
export function followPointer(canvas: HTMLCanvasElement) {
    const events = new AbortController();
    const touchAction = canvas.style.touchAction;
    canvas.style.touchAction = "none";
    let mouseX = 0;
    let mouseY = 0;

    canvas.addEventListener('pointermove', (event) => {
        if (!event.isPrimary) return;
        const bounds = canvas.getBoundingClientRect();
        mouseX = event.clientX - bounds.left - bounds.width / 2;
        mouseY = event.clientY - bounds.top - bounds.height / 2;
    }, { signal: events.signal });

    return {
        update(camera: PerspectiveCamera, target: Vector3, yOffset = 0, minY = -Infinity, maxY = Infinity) {
            camera.position.x += (mouseX - camera.position.x) * 0.05;
            const y = camera.position.y + (yOffset - mouseY - camera.position.y) * 0.05;
            camera.position.y = Math.max(minY, Math.min(maxY, y));
            camera.lookAt(target);
        },
        dispose() {
            events.abort();
            canvas.style.touchAction = touchAction;
        },
    };
}
