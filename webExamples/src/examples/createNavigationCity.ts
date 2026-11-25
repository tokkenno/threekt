import {
    AmbientLight, Box, Color, CylinderGeometry, DirectionalLight, DomInputSurface, FogExp2, Mesh,
    MeshPhongMaterial, MouseAction, OrbitControls, PerspectiveCamera, Scene, TouchAction, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { addToggle, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';

export function createNavigationCity(canvas: HTMLCanvasElement, map: boolean) {
    const scene = new Scene();
    scene.background = Color.fromHex(0xcccccc);
    scene.fog = new FogExp2(Color.fromHex(0xcccccc), 0.002);
    const camera = new PerspectiveCamera(60, 1, 1, 1000);
    camera.position.set(400, 200, 0);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.enableDamping = true;
    controls.screenSpacePanning = false;
    controls.minDistance = 100;
    controls.maxDistance = 500;
    controls.maxPolarAngle = Math.PI / 2;
    if (map) {
        controls.mouseButtons.left = MouseAction.Pan;
        controls.mouseButtons.right = MouseAction.Rotate;
        controls.touches.one = TouchAction.Pan;
        controls.touches.two = TouchAction.DollyRotate;
    }
    controls.update();

    const geometry = map ? new Box(1, 1, 1) : new CylinderGeometry(0, 10, 30, 4, 1);
    const material = new MeshPhongMaterial(Color.fromHex(map ? 0xeeeeee : 0xffffff));
    material.flatShading = true;
    for (let i = 0; i < 500; i++) {
        const mesh = new Mesh(geometry, material);
        const height = map ? Math.random() * 80 + 10 : 30;
        if (map) mesh.scale.set(20, height, 20);
        mesh.position.set(Math.random() * 1600 - 800, height / 2, Math.random() * 1600 - 800);
        scene.add(mesh);
    }
    const sun = new DirectionalLight(Color.fromHex(0xffffff), 3);
    sun.position.set(1, 1, 1);
    const fill = new DirectionalLight(Color.fromHex(0x002288), 3);
    fill.position.set(-1, -1, -1);
    scene.add(sun);
    scene.add(fill);
    scene.add(new AmbientLight(Color.fromHex(0x555555)));

    const panel = examplePanel(canvas);
    panel.append(map ? 'Drag to pan. Right-drag to orbit.' : 'Drag to orbit. Right-drag to pan.');
    if (map) {
        addToggle(panel, 'Zoom to cursor', controls.zoomToCursor, value => { controls.zoomToCursor = value; });
        addToggle(panel, 'Screen-space panning', controls.screenSpacePanning, value => { controls.screenSpacePanning = value; });
    }
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            controls.update();
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() { panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
