import {
    AmbientLight, AxesHelper, Color, DirectionalLight, DomInputSurface, Mesh, MeshPhongMaterial,
    Object3D, OrbitControls, PerspectiveCamera, Scene, SphereGeometry, Vector3
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import earthUrl from './assets/planets/earth_atmos_2048.jpg?url';
import moonUrl from './assets/planets/moon_1024.jpg?url';
import { addToggle, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

export async function createExample(canvas: HTMLCanvasElement) {
    const [earthMap, moonMap] = await Promise.all([earthUrl, moonUrl].map(loadImageTexture));
    const scene = new Scene();
    scene.add(new AmbientLight(Color.fromHex(0x606060)));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 3);
    light.position.set(0, 0, 1);
    scene.add(light);
    scene.add(new AxesHelper(5));
    const earth = new Mesh(new SphereGeometry(1, 32, 16), new MeshPhongMaterial());
    const moon = new Mesh(new SphereGeometry(0.27, 32, 16), new MeshPhongMaterial());
    (earth.material as MeshPhongMaterial).map = earthMap;
    (moon.material as MeshPhongMaterial).map = moonMap;
    scene.add(earth);
    scene.add(moon);
    const layer = document.createElement('div');
    layer.className = 'atom-labels';
    canvas.after(layer);
    const labels = [
        { body: earth, name: 'Earth', mass: '5.972 × 10²⁴ kg', x: 1.5 },
        { body: moon, name: 'Moon', mass: '7.342 × 10²² kg', x: 0.405 },
    ].map(({ body, name, mass, x }) => {
        const node = new Object3D();
        node.position.set(x, 0, 0);
        body.add(node);
        const label = document.createElement('span');
        label.style.transform = 'translateY(-100%)';
        const detail = document.createElement('small');
        label.textContent = name;
        detail.textContent = mass;
        detail.style.display = 'block';
        label.append(detail);
        layer.append(label);
        return { node, label, detail };
    });
    const panel = examplePanel(canvas);
    addToggle(panel, 'Mass labels', true, value => { for (const { detail } of labels) detail.hidden = !value; });
    const camera = new PerspectiveCamera(45, 1, 0.1, 200);
    camera.position.set(10, 5, 20);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const projected = new Vector3();
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            moon.position.set(Math.sin(seconds) * 5, 0, Math.cos(seconds) * 5);
            renderer.render(scene, camera);
            for (const { node, label } of labels) {
                projected.setFromMatrixPosition(node.matrixWorld).project(camera);
                label.hidden = Math.abs(projected.z) > 1;
                label.style.left = (projected.x + 1) * 50 + '%';
                label.style.top = (1 - projected.y) * 50 + '%';
            }
        },
        dispose() { controls.dispose(); renderer.dispose(); layer.remove(); panel.remove(); },
    };
}
