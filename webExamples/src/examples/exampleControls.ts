import { ImageTexture, type PerspectiveCamera, type Scene } from 'threekt';

export async function loadImageTexture(url: string): Promise<ImageTexture> {
    const image = new Image();
    image.src = url;
    await image.decode();
    return new ImageTexture(image);
}

export function followPointer(canvas: HTMLCanvasElement, camera: PerspectiveCamera, scene: Scene) {
    let x = 0;
    let y = 0;
    const move = (event: PointerEvent) => {
        if (!event.isPrimary) return;
        const bounds = canvas.getBoundingClientRect();
        x = event.clientX - bounds.left - bounds.width / 2;
        y = bounds.top + bounds.height / 2 - event.clientY;
    };
    const previousTouchAction = canvas.style.touchAction;
    canvas.style.touchAction = 'none';
    canvas.addEventListener('pointermove', move);
    return {
        update() {
            camera.position.x += (x - camera.position.x) * 0.05;
            camera.position.y += (y - camera.position.y) * 0.05;
            camera.lookAt(scene.position);
        },
        dispose() {
            canvas.removeEventListener('pointermove', move);
            canvas.style.touchAction = previousTouchAction;
        },
    };
}

export function examplePanel(canvas: HTMLCanvasElement): HTMLElement {
    const panel = document.createElement('section');
    panel.className = 'example-panel';
    canvas.after(panel);
    return panel;
}

export function addToggle(panel: HTMLElement, name: string, initial: boolean, update: (value: boolean) => void) {
    const label = document.createElement('label');
    label.className = 'example-setting';
    label.textContent = name;
    const input = document.createElement('input');
    input.type = 'checkbox';
    input.checked = initial;
    input.addEventListener('change', () => update(input.checked));
    label.append(input);
    panel.append(label);
}
