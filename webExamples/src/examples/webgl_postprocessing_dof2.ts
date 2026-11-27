import { AmbientLight, Color, DirectionalLight, InstancedMesh, Mesh, MeshPhongMaterial, Object3D, PerspectiveCamera,
    PlaneGeometry, Scene, Side, SphereGeometry, Vector3 } from 'threekt';
import suzanne from './assets/geometry-json/suzanne_buffergeometry.json?url';
import { addSliders } from './addSliders';
import { addToggle, examplePanel } from './exampleControls';
import { loadExampleCubeMap } from './exampleCubeMap';
import { exampleDepthOfField } from './exampleDepthOfField';
import { addEnvironment } from './exampleEnvironment';
import { fitPerspective } from './fitPerspective';
import { loadExampleGeometry } from './loadExampleGeometry';

export async function createExample(canvas: HTMLCanvasElement) {
    const [geometry, uniforms] = await Promise.all([loadExampleGeometry(suzanne), loadExampleCubeMap('Park3Med')]);
    const scene = new Scene();
    const environment = addEnvironment(scene, uniforms, 2800, true);
    const camera = new PerspectiveCamera(70, 1, 1, 3000);
    const look = new Vector3(0, 20, -50);
    scene.add(new AmbientLight(Color.fromHex(0xcccccc), 1.2));
    for (const [sign, intensity] of [[1, 6], [-1, 3]]) {
        const light = new DirectionalLight(Color.fromHex(0xffffff), intensity);
        light.position.set(sign * 2, 1.2, sign * 10);
        scene.add(light);
    }
    const monkeys = Array.from({ length: 12 }, (_, i) => {
        const material = new MeshPhongMaterial(new Color(1, 1, 1).setHSL(i / 12, 0.7, 0.5));
        const mesh = new Mesh(geometry, material);
        const angle = i * Math.PI * 2 / 12;
        mesh.position.set(200 * Math.sin(angle), 20 * Math.sin(angle * 1.5), 200 * Math.cos(angle));
        mesh.scale.setScalar(30);
        scene.add(mesh);
        return mesh;
    });
    const material = new MeshPhongMaterial(Color.fromHex(0xe6bc78));
    material.side = Side.Double;
    const particles = new InstancedMesh(new PlaneGeometry(10, 10), material, 120);
    scene.add(particles);
    const dummy = new Object3D();
    const seeds = Array.from({ length: particles.count }, () =>
        [Math.random() * 300 - 150, Math.random() * 300, Math.random() * 300 - 150]);
    const balls = new InstancedMesh(new SphereGeometry(8, 16, 12), new MeshPhongMaterial(Color.fromHex(0x306aaa)), 18);
    for (let i = 0; i < balls.count; i++) {
        dummy.position.set(Math.random() * 200 - 100, Math.random() * 50, Math.random() * 200 - 100);
        dummy.updateMatrix();
        balls.setMatrixAt(i, dummy.matrix);
    }
    scene.add(balls);
    const effect = exampleDepthOfField(canvas, camera, true);
    effect.aperture.value = 0.00012;
    effect.maxBlur.value = 0.025;
    const panel = examplePanel(canvas);
    addToggle(panel, 'Focus under pointer', true, value => { effect.automatic.value = value; });
    addToggle(panel, 'Show focal plane', false, value => { effect.showFocus.value = value; });
    addSliders(canvas, [
        { label: 'Focus distance', min: 20, max: 1000, step: 1, value: 500, change: value => { effect.focus.value = value; } },
        { label: 'Aperture', min: 0, max: 20, step: 0.1, value: 12, change: value => { effect.aperture.value = value * 0.00001; } },
        { label: 'Maximum blur', min: 0, max: 0.04, step: 0.001, value: 0.025, change: value => { effect.maxBlur.value = value; } },
    ], panel);
    const onMove = (event: PointerEvent) => {
        const bounds = canvas.getBoundingClientRect();
        effect.focusPoint.set((event.clientX - bounds.left) / bounds.width, 1 - (event.clientY - bounds.top) / bounds.height);
    };
    canvas.addEventListener('pointermove', onMove);
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, effect.renderer)) return;
            const t = seconds * 0.24;
            camera.position.set(Math.sin(t) * 420, 120 + Math.sin(t * 0.7) * 50, Math.cos(t) * 450);
            camera.lookAt(look);
            environment.position.copyFrom(camera.position);
            for (const mesh of monkeys) mesh.rotation.y = seconds * 0.35;
            seeds.forEach(([x, y, z], i) => {
                dummy.position.set(x + 10 * Math.sin(t + i), (y + 300 - (seconds * 45) % 300) % 300, z);
                dummy.rotation.set(t + i, t * 2 + i, 0);
                dummy.updateMatrix();
                particles.setMatrixAt(i, dummy.matrix);
            });
            particles.instanceMatrix.markUpdated();
            effect.render(scene);
        },
        dispose() { canvas.removeEventListener('pointermove', onMove); panel.remove(); effect.renderer.dispose(); },
    };
}
