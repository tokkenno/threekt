type Slider = {
    label: string;
    min: number;
    max: number;
    step: number;
    value: number;
    change: (value: number) => void;
};

export function addSliders(canvas: HTMLCanvasElement, sliders: Slider[], parent?: HTMLElement) {
    const panel = parent ?? document.createElement('aside');
    if (!parent) panel.className = 'example-panel';
    const labels: HTMLLabelElement[] = [];
    for (const slider of sliders) {
        const label = document.createElement('label');
        const input = document.createElement('input');
        input.type = 'range';
        input.min = String(slider.min);
        input.max = String(slider.max);
        input.step = String(slider.step);
        input.value = String(slider.value);
        input.addEventListener('input', () => slider.change(input.valueAsNumber));
        label.className = "example-setting";
        label.append(slider.label, input);
        panel.append(label);
        labels.push(label);
    }
    if (!parent) canvas.parentElement?.append(panel);
    return () => parent ? labels.forEach(label => label.remove()) : panel.remove();
}
