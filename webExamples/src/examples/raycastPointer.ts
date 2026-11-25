import { Vector2 } from 'threekt';

export function setPointer(canvas: HTMLCanvasElement, event: PointerEvent, target: Vector2) {
    const bounds = canvas.getBoundingClientRect();
    target.set((event.clientX - bounds.left) / bounds.width * 2 - 1, 1 - (event.clientY - bounds.top) / bounds.height * 2);
}

export function raycastPointer(canvas: HTMLCanvasElement) {
    const pointer = new Vector2();
    const events = new AbortController();
    canvas.addEventListener('pointermove', event => setPointer(canvas, event, pointer), { signal: events.signal });
    return { pointer, dispose: () => events.abort() };
}
