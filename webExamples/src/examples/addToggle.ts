export function addToggle(canvas: HTMLCanvasElement, text: string, checked: boolean, change: (value: boolean) => void) {
    const panel = document.createElement('aside');
    panel.className = 'example-panel';
    const label = document.createElement('label');
    const input = document.createElement('input');
    input.type = 'checkbox';
    input.checked = checked;
    input.addEventListener('change', () => change(input.checked));
    label.append(input, ' ', text);
    panel.append(label);
    canvas.parentElement?.append(panel);
    return () => panel.remove();
}
