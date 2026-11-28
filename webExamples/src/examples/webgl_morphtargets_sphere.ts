import {
    AmbientLight, Color, Mesh, MeshPhongMaterial, PerspectiveCamera, PointLight, Points, PointsMaterial, Scene,
} from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import modelUrl from './assets/morph-sphere/AnimatedMorphSphere.gltf?url';
import bufferUrl from './assets/morph-sphere/AnimatedMorphSphere.bin?url';
import discUrl from './assets/disc.png';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';
import { readExampleGltfGeometry, type ExampleGltf } from './readExampleGltf';

export async function createExample(canvas: HTMLCanvasElement) {
    const [model, buffer, disc] = await Promise.all([fetch(modelUrl), fetch(bufferUrl), loadImageTexture(discUrl)]);
    if (!model.ok || !buffer.ok) throw new Error('Could not load AnimatedMorphSphere.');
    const document: ExampleGltf = await model.json();
    const node = document.nodes.find(node => node.name === 'AnimatedMorphSphere')!;
    const geometry = readExampleGltfGeometry(document, await buffer.arrayBuffer(), node.mesh);
    const mesh = new Mesh(geometry, new MeshPhongMaterial());
    mesh.quaternion.set(...node.rotation);
    mesh.rotation.z = Math.PI / 2;
    mesh.scale.set(...node.scale);

    const material = new PointsMaterial();
    material.size = 10;
    material.sizeAttenuation = false;
    material.map = disc;
    material.alphaTest = 0.5;
    const points = new Points(geometry, material);
    mesh.add(points);

    const camera = new PerspectiveCamera(45, 1, 0.2, 100);
    camera.position.set(0, 5, 5);
    const scene = new Scene();
    const red = new PointLight(Color.fromHex(0xff2200), 50000);
    red.position.set(100, 100, 100);
    const green = new PointLight(Color.fromHex(0x22ff00), 10000);
    green.position.set(-100, -100, -100);
    scene.add(red);
    scene.add(green);
    scene.add(new AmbientLight(Color.fromHex(0x111111)));
    scene.add(mesh);

    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 1;
    controls.maxDistance = 20;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            const step = seconds * 0.5;
            mesh.rotation.y = step;
            const influence = 1 - Math.abs(step % 2 - 1);
            mesh.morphTargetInfluences![1] = influence;
            points.morphTargetInfluences![1] = influence;
            renderer.render(scene, camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); },
    };
}
