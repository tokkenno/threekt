import { AmbientLight, Color, InstancedMesh, MeshPhongMaterial, Object3D, OrthographicCamera, PerspectiveCamera,
    PointLight, Scene, SphereGeometry, WebGL2Context, WebGLRenderer } from 'threekt';
import { addSliders } from './addSliders';
import { addSelect, addToggle, examplePanel } from './exampleControls';
import { exampleSupersampling } from './exampleSupersampling';

export function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    const perspective = new PerspectiveCamera(65, 1, 3, 10);
    const orthographic = new OrthographicCamera(-4, 4, 4, -4, 3, 10);
    perspective.position.z = orthographic.position.z = 7;
    scene.add(new AmbientLight(Color.fromHex(0xffffff), 0.2));
    for (const [tint, x, y] of [[0xefffef, -10, -10], [0xffefef, -10, 10], [0xefefff, 10, -10]]) {
        const light = new PointLight(Color.fromHex(tint), 500);
        light.position.set(x, y, 10);
        scene.add(light);
    }
    const cluster = new InstancedMesh(new SphereGeometry(3, 24, 16), new MeshPhongMaterial(), 120);
    const dummy = new Object3D();
    const color = new Color(1, 1, 1);
    for (let i = 0; i < cluster.count; i++) {
        dummy.position.set(Math.random() * 4 - 2, Math.random() * 4 - 2, Math.random() * 4 - 2);
        dummy.scale.setScalar(0.05 + Math.random() * 0.2);
        dummy.updateMatrix();
        cluster.setMatrixAt(i, dummy.matrix);
        cluster.setColorAt(i, color.setHSL(Math.random(), 1, 0.3));
    }
    scene.add(cluster);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const supersampling = exampleSupersampling(renderer);
    let samples = 8, usePerspective = true, unbiased = true, autoRotate = true, rotation = 0, last = 0;
    let offset = 0, alpha = 1;
    const clearColor = new Color(0, 0, 0);
    const panel = examplePanel(canvas);
    addSelect(panel, 'Samples', { 1: '1', 2: '2', 4: '4', 8: '8', 16: '16', 32: '32' }, '8', value => { samples = Number(value); });
    addSelect(panel, 'Camera', { p: 'Perspective', o: 'Orthographic' }, 'p', value => { usePerspective = value === 'p'; });
    addSelect(panel, 'Clear color', { '000000': 'Black', ffffff: 'White', '0000ff': 'Blue', '008000': 'Green', ff0000: 'Red' },
        '000000', value => clearColor.setHex(parseInt(value, 16)));
    addToggle(panel, 'Unbiased weights', true, value => { unbiased = value; });
    addToggle(panel, 'Auto rotate', true, value => { autoRotate = value; });
    addSliders(canvas, [
        { label: 'Clear alpha', min: 0, max: 1, step: 0.01, value: 1, change: value => { alpha = value; } },
        { label: 'View offset X', min: -100, max: 100, step: 1, value: 0, change: value => { offset = value; } },
    ], panel);
    return {
        render(seconds: number) {
            const { clientWidth: w, clientHeight: h } = canvas;
            if (!w || !h) return;
            renderer.setSize(w, h, Math.min(1.5, window.devicePixelRatio));
            const camera = usePerspective ? perspective : orthographic;
            perspective.aspect = w / h;
            orthographic.right = 4 * w / h;
            orthographic.left = -orthographic.right;
            camera.setViewOffset(w, h, offset, 0, w, h);
            if (autoRotate) rotation += Math.min(0.05, seconds - last) * 0.35;
            last = seconds;
            cluster.rotation.set(rotation * 0.6, rotation, 0);
            renderer.setClearColor(clearColor, alpha);
            supersampling.resize(canvas.width, canvas.height);
            supersampling.reset();
            for (let i = 0; i < samples; i++) supersampling.sample(scene, camera, samples, unbiased);
            supersampling.present();
        },
        dispose() { panel.remove(); renderer.dispose(); },
    };
}
