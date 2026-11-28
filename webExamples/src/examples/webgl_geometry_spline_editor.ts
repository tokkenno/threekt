import {
    AmbientLight, BoxGeometry, BufferGeometry, Color, DirectionalLight, DomInputSurface, Float32BufferAttribute, GridHelper,
    Line, LineBasicMaterial, Mesh, MeshPhongMaterial, OrbitControls, PerspectiveCamera, Scene, Usage, Vector3,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { dragMeshes } from './dragMeshes';
import { addToggle, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';

function interpolate(a: number, b: number, m0: number, m1: number, t: number) {
    return a + t * (m0 + t * (3 * (b - a) - 2 * m0 - m1 + t * (2 * (a - b) + m0 + m1)));
}

export function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    scene.background = Color.fromHex(0xf0f0f0);
    scene.add(new GridHelper(2000, 100));
    scene.add(new AmbientLight(Color.fromHex(0xf0f0f0), 1.5));
    const sun = new DirectionalLight(Color.fromHex(0xffffff), 1.8);
    sun.position.set(0, 1000, 500);
    scene.add(sun);
    const camera = new PerspectiveCamera(70, 1, 1, 10000);
    camera.position.set(0, 250, 1000);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.target.y = 150;
    controls.update();

    const points: Mesh[] = [];
    const geometry = new BoxGeometry(20, 20, 20);
    function addPoint(x: number, y: number, z: number) {
        const mesh = new Mesh(geometry, new MeshPhongMaterial(Color.fromHex(Math.floor(Math.random() * 0x1000000))));
        mesh.position.set(x, y, z);
        scene.add(mesh);
        points.push(mesh);
    }
    addPoint(290, 453, 56);
    addPoint(-54, 291, -227);
    addPoint(-224, 77, -133);
    addPoint(-113, 96, 206);

    const samples = 200;
    const curves = [
        { name: 'Uniform', color: 0xff0000, exponent: 0 },
        { name: 'Centripetal', color: 0x00a000, exponent: 0.25 },
        { name: 'Chordal', color: 0x0055ff, exponent: 0.5 },
    ].map(definition => {
        const values = new Float32Array(samples * 3);
        const position = new Float32BufferAttribute(values, 3);
        position.setUsage(Usage.DynamicDraw);
        const geometry = new BufferGeometry().setAttribute('position', position);
        const line = new Line(geometry, new LineBasicMaterial(Color.fromHex(definition.color)));
        scene.add(line);
        return { ...definition, position, values, line };
    });

    const start = new Vector3(), end = new Vector3();
    let tension = 0.5, dirty = true;
    function updateCurves() {
        for (const { exponent, values, position, line } of curves) {
            for (let i = 0; i < samples; i++) {
                const scaled = i * (points.length - 1) / (samples - 1);
                const part = Math.min(points.length - 2, Math.floor(scaled));
                const b = points[part].position;
                const c = points[part + 1].position;
                const a = points[part - 1]?.position ?? start.copyFrom(b).multiplyScalar(2).sub(c);
                const d = points[part + 2]?.position ?? end.copyFrom(c).multiplyScalar(2).sub(b);
                let dt0 = a.distanceToSquared(b) ** exponent;
                let dt1 = b.distanceToSquared(c) ** exponent;
                let dt2 = c.distanceToSquared(d) ** exponent;
                if (dt1 < 1e-4) dt1 = 1;
                if (dt0 < 1e-4) dt0 = dt1;
                if (dt2 < 1e-4) dt2 = dt1;
                const axes = ['x', 'y', 'z'] as const;
                axes.forEach((axis, component) => {
                    const left = b[axis] - a[axis], middle = c[axis] - b[axis], right = d[axis] - c[axis];
                    const m0 = exponent === 0 ? tension * (left + middle)
                        : (left / dt0 - (left + middle) / (dt0 + dt1) + middle / dt1) * dt1;
                    const m1 = exponent === 0 ? tension * (middle + right)
                        : (middle / dt1 - (middle + right) / (dt1 + dt2) + right / dt2) * dt1;
                    values[i * 3 + component] = interpolate(b[axis], c[axis], m0, m1, scaled - part);
                });
            }
            position.markUpdated();
            line.geometry.computeBoundingSphere();
        }
    }

    let dragging = false;
    const disposeDrag = dragMeshes(canvas, camera, points, {
        begin(mesh) { dragging = true; return [mesh]; },
        end() { dragging = false; dirty = true; },
    });
    const panel = examplePanel(canvas);
    for (const curve of curves) addToggle(panel, curve.name, true, value => { curve.line.visible = value; });
    const label = document.createElement('label');
    label.className = 'example-setting';
    const slider = document.createElement('input');
    slider.type = 'range';
    slider.min = '0';
    slider.max = '1';
    slider.step = '0.01';
    slider.value = String(tension);
    slider.addEventListener('input', () => { tension = slider.valueAsNumber; dirty = true; });
    label.append('Tension', slider);
    panel.append(label);

    function button(label: string, action: () => void) {
        const button = document.createElement('button');
        button.textContent = label;
        button.addEventListener('click', action);
        panel.append(button);
    }
    button('Add point', () => { addPoint(Math.random() * 500 - 250, Math.random() * 300 + 50, Math.random() * 500 - 250); dirty = true; });
    button('Remove point', () => {
        if (points.length > 4) {
            scene.remove(points.pop()!);
            dirty = true;
        }
    });
    button('Export spline', () => {
        const payload = points.map(({ position }) => [position.x, position.y, position.z]);
        const url = URL.createObjectURL(new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' }));
        const link = document.createElement('a');
        link.href = url;
        link.download = 'spline.json';
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 0);
    });

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (dirty || dragging) { updateCurves(); dirty = false; }
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() { disposeDrag(); controls.dispose(); panel.remove(); renderer.dispose(); },
    };
}
