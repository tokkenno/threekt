import {
    AmbientLight, Color, DirectionalLight, Float32BufferAttribute, Mesh, MeshPhongMaterial, PerspectiveCamera,
    Scene,
} from 'threekt';
import { BoxGeometry } from 'threekt/geometry';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { addSliders } from './addSliders';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const geometry = new BoxGeometry(2, 2, 2, 32, 32, 32);
    const position = geometry.getAttribute('position')!;
    const sphere = new Float32Array(position.count * 3);
    const twist = new Float32Array(sphere.length);
    const project = (v: number, a: number, b: number) => v * Math.sqrt(1 - a * a / 2 - b * b / 2 + a * a * b * b / 3);
    for (let i = 0; i < position.count; i++) {
        const x = position.getX(i), y = position.getY(i), z = position.getZ(i);
        sphere.set([project(x, y, z), project(y, z, x), project(z, x, y)], i * 3);
        const theta = Math.PI * x / 2;
        twist.set([2 * x, y * Math.cos(theta) - z * Math.sin(theta), y * Math.sin(theta) + z * Math.cos(theta)], i * 3);
    }
    geometry.setMorphAttribute('position', [new Float32BufferAttribute(sphere, 3), new Float32BufferAttribute(twist, 3)]);

    const camera = new PerspectiveCamera(45, 1, 1, 20);
    camera.position.z = 10;
    const scene = new Scene();
    const blue = Color.fromHex(0x8fbcd4);
    scene.background = blue;
    scene.add(new AmbientLight(blue, 1.5));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 2);
    camera.add(light);
    scene.add(camera);
    const material = new MeshPhongMaterial(Color.fromHex(0xff0000));
    material.flatShading = true;
    const mesh = new Mesh(geometry, material);
    scene.add(mesh);

    const removePanel = addSliders(canvas, ['Esferificar', 'Torcer'].map((label, index) => ({
        label, value: 0, min: 0, max: 1, step: 0.01,
        change: (value: number) => { mesh.morphTargetInfluences![index] = value; },
    })));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.enableZoom = false;
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() { removePanel(); controls.dispose(); renderer.dispose(); },
    };
}
