import {
    Box, Color, DomInputSurface, ImageTexture, Mesh, MeshBasicMaterial, OrbitControls, PerspectiveCamera, Scene, WebGL2Context, WebGLRenderer,
    Wrapping,
} from 'threekt';
import uvGridUrl from './assets/uv_grid_opengl.jpg?url';
import { fitPerspective } from './fitPerspective';

type Setting = { label: string; min: number; max: number; value: number };

/** Port of Three.js examples/webgl_materials_texture_rotation.html. */
export async function createExample(canvas: HTMLCanvasElement) {
    const image = new Image();
    image.src = uvGridUrl;
    await image.decode();

    const scene = new Scene();
    const camera = new PerspectiveCamera(40, 1, 1, 1000);
    camera.position.set(10, 15, 25);
    scene.add(camera);

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 20;
    controls.maxDistance = 50;
    controls.maxPolarAngle = Math.PI / 2;

    const texture = new ImageTexture(image);
    texture.wrapS = Wrapping.Repeat;
    texture.wrapT = Wrapping.Repeat;
    texture.anisotropy = renderer.capabilities.getMaxAnisotropy();
    scene.add(new Mesh(new Box(10, 10, 10), new MeshBasicMaterial(Color.fromHex(0xffffff), texture)));

    const settings = {
        offsetX: { label: 'offset.x', min: 0, max: 1, value: 0 },
        offsetY: { label: 'offset.y', min: 0, max: 1, value: 0 },
        repeatX: { label: 'repeat.x', min: 0.25, max: 2, value: 0.25 },
        repeatY: { label: 'repeat.y', min: 0.25, max: 2, value: 0.25 },
        // Positive values rotate counterclockwise around the center.
        rotation: { label: 'rotation', min: -2, max: 2, value: Math.PI / 4 },
        centerX: { label: 'center.x', min: 0, max: 1, value: 0.5 },
        centerY: { label: 'center.y', min: 0, max: 1, value: 0.5 },
    } satisfies Record<string, Setting>;

    const updateUvTransform = () => {
        texture.offset.set(settings.offsetX.value, settings.offsetY.value);
        texture.repeat.set(settings.repeatX.value, settings.repeatY.value);
        texture.center.set(settings.centerX.value, settings.centerY.value);
        texture.rotation = settings.rotation.value;
    };
    updateUvTransform();

    const panel = document.createElement('aside');
    panel.className = 'example-panel';
    const instructions = document.createElement('p');
    instructions.textContent = 'Arrastra para orbitar, usa la rueda para acercarte y el botón derecho para desplazarte.';
    panel.append(instructions);
    for (const setting of Object.values(settings)) {
        const label = document.createElement('label');
        label.className = 'example-setting';
        const name = document.createElement('span');
        name.textContent = setting.label;
        const output = document.createElement('output');
        output.textContent = setting.value.toFixed(2);
        const input = document.createElement('input');
        input.type = 'range';
        input.min = String(setting.min);
        input.max = String(setting.max);
        input.step = 'any';
        input.value = String(setting.value);
        input.addEventListener('input', () => {
            setting.value = input.valueAsNumber;
            output.textContent = setting.value.toFixed(2);
            updateUvTransform();
        });
        label.append(name, output, input);
        panel.append(label);
    }
    canvas.parentElement?.append(panel);

    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            renderer.render(scene, camera);
        },
        dispose() {
            panel.remove();
            controls.dispose();
            renderer.dispose();
        },
    };
}
