import { BoxGeometry, CylinderGeometry, SphereGeometry } from 'threekt/geometry';
import {
    CalibratedPerspectiveCamera, Color, Euler, Mesh, MeshBasicMaterial, PerspectiveCamera, Scene, Vector2,
    Vector3,
} from 'threekt';
import { CameraHelper, GridHelper } from 'threekt/helpers';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';

type Setting = { name: string; value: number; min: number; max: number; step: number };

function createMenu(canvas: HTMLCanvasElement, groups: [string, Setting[]][]) {
    const panel = document.createElement('aside');
    panel.className = 'example-panel';
    panel.setAttribute('aria-label', 'Camera calibration');
    const inputs = new Map<string, HTMLInputElement>();
    let changed = true;
    for (const [title, settings] of groups) {
        const section = document.createElement('details');
        section.open = title === 'Lens and image';
        const heading = document.createElement('summary');
        heading.textContent = title;
        section.append(heading);
        for (const { name, value, min, max, step } of settings) {
            const label = document.createElement('label');
            label.className = 'example-setting';
            const field = document.createElement('input');
            field.type = 'range';
            field.min = String(min);
            field.max = String(max);
            field.step = String(step);
            field.value = String(value);
            field.setAttribute('aria-label', name);
            const output = document.createElement('output');
            const update = () => {
                output.value = field.value;
                changed = true;
            };
            field.addEventListener('input', update);
            update();
            label.append(name, output, field);
            section.append(label);
            inputs.set(name, field);
        }
        panel.append(section);
    }
    const reset = document.createElement('button');
    reset.textContent = 'Reset camera';
    reset.addEventListener('click', () => {
        for (const [, settings] of groups) for (const { name, value } of settings) {
            const input = inputs.get(name)!;
            input.value = String(value);
            input.dispatchEvent(new Event('input'));
        }
    });
    panel.append(reset);
    canvas.parentElement?.append(panel);
    return {
        value: (name: string) => inputs.get(name)!.valueAsNumber,
        takeChanges() {
            const result = changed;
            changed = false;
            return result;
        },
        dispose: () => panel.remove(),
    };
}

const setting = (name: string, value: number, min: number, max: number, step = 0.01): Setting =>
    ({ name, value, min, max, step });

export async function createExample(canvas: HTMLCanvasElement) {
    const calibrated = new CalibratedPerspectiveCamera(50, 1, 0.5, 16);
    const observer = new PerspectiveCamera(45, 1, 0.1, 100);
    observer.position.set(19, 14, 23);
    observer.lookAt(new Vector3());
    const scene = new Scene();
    scene.add(new GridHelper(20, 20, Color.fromHex(0x526783), Color.fromHex(0x334155)));
    const addShape = (geometry: BoxGeometry | CylinderGeometry | SphereGeometry, color: number, x: number, y: number, z: number) => {
        const material = new MeshBasicMaterial(Color.fromHex(color));
        material.wireframe = true;
        const mesh = new Mesh(geometry, material);
        mesh.position.set(x, y, z);
        scene.add(mesh);
    };
    addShape(new BoxGeometry(1.3, 1.3, 1.3), 0x55caff, -1.8, 0.65, 1);
    addShape(new SphereGeometry(1, 16, 12), 0xffb454, 0, 1, -2);
    addShape(new CylinderGeometry(0.6, 0.85, 2, 16), 0x75e39f, 2.2, 1, -4);
    const helper = new CameraHelper(calibrated);
    scene.add(helper);

    const menu = createMenu(canvas, [
        ['Lens and image', [
            setting('Focal length', 50, 15, 100, 1), setting('Film width', 35, 15, 70, 1),
            setting('Film aspect', 1, 0.5, 2.5), setting('Zoom', 1, 1, 4),
            setting('Principal X', 0, -0.4, 0.4), setting('Principal Y', 0, -0.4, 0.4),
        ]],
        ['Pan and crop', [
            setting('Pan X', 0, -1, 1), setting('Pan Y', 0, -1, 1),
            setting('Crop left', 0, 0, 0.45), setting('Crop right', 0, 0, 0.45),
            setting('Crop bottom', 0, 0, 0.45), setting('Crop top', 0, 0, 0.45),
        ]],
        ['Position and rotation', [
            setting('Position X', 0, -6, 6, 0.1), setting('Position Y', 2, 0.2, 7, 0.1),
            setting('Position Z', 8, 3, 14, 0.1), setting('Pitch', 0, -45, 45, 1),
            setting('Yaw', 0, -45, 45, 1), setting('Roll', 0, -45, 45, 1),
        ]],
        ['Clipping planes', [setting('Near', 0.5, 0.1, 5, 0.1), setting('Far', 16, 6, 24, 0.1)]],
    ]);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const dark = Color.fromHex(0x0b1220);
    const observerBackground = Color.fromHex(0x19263a);
    const principal = new Vector2(), pan = new Vector2(), rotation = new Euler();
    const angle = Math.PI / 180;
    renderer.setScissorTest(true);

    return {
        render() {
            const width = canvas.clientWidth, height = canvas.clientHeight;
            if (width < 2 || !height) return;
            const left = Math.floor(width / 2);
            renderer.setSize(width, height, window.devicePixelRatio);
            if (menu.takeChanges()) {
                const v = menu.value;
                calibrated.focal = v('Focal length');
                calibrated.filmGauge = v('Film width');
                calibrated.filmAR = v('Film aspect');
                calibrated.zoom = v('Zoom');
                calibrated.principalPoint = principal.set(v('Principal X'), v('Principal Y'));
                calibrated.pan = pan.set(v('Pan X'), v('Pan Y'));
                calibrated.setCropArea(v('Crop left'), v('Crop bottom'), 1 - v('Crop right'), 1 - v('Crop top'));
                calibrated.near = v('Near');
                calibrated.far = v('Far');
                calibrated.position.set(v('Position X'), v('Position Y'), v('Position Z'));
                calibrated.quaternion.setFromEuler(rotation.set(v('Pitch') * angle, v('Yaw') * angle, v('Roll') * angle));
                calibrated.updateProjectionMatrix();
                helper.update();
            }

            const aspect = calibrated.aspectRatio;
            const imageWidth = Math.max(1, Math.min(left, Math.floor(height * aspect)));
            const imageHeight = Math.max(1, Math.min(height, Math.floor(left / aspect)));
            helper.visible = false;
            renderer.setClearColor(dark);
            renderer.setScissor(0, 0, left, height);
            renderer.setViewport(Math.floor((left - imageWidth) / 2), Math.floor((height - imageHeight) / 2), imageWidth, imageHeight);
            renderer.render(scene, calibrated);

            helper.visible = true;
            const observerAspect = (width - left) / height;
            if (observer.aspect !== observerAspect) {
                observer.aspect = observerAspect;
                observer.updateProjectionMatrix();
            }
            renderer.setClearColor(observerBackground);
            renderer.setViewport(left, 0, width - left, height);
            renderer.setScissor(left, 0, width - left, height);
            renderer.render(scene, observer);
        },
        dispose() { menu.dispose(); renderer.dispose(); },
    };
}
