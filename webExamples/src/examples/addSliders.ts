type Slider = {
    label: string;
    min: number;
    max: number;
    step: number;
    value: number;
    change: (value: number) => void;
};

export function addSliders(canvas: HTMLCanvasElement, sliders: Slider[]) {
    const panel = document.createElement('aside');
    panel.className = 'example-panel';
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
    }
    canvas.parentElement?.append(panel);
    return () => panel.remove();
}
