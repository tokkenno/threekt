import {
    AmbientLight, Color, InstancedMesh, Matrix4, Mesh, MeshLambertMaterial, MeshSurfaceSampler, Object3D,
    PerspectiveCamera, PointLight, Scene, Sphere, TorusKnotGeometry, Usage, Vector3,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import flowerUrl from './assets/flower/Flower.glb?url';
import { addSelect, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';
import { readExampleGltfGeometry, type ExampleGltf } from './readExampleGltf';

const capacity = 2000;
const blossomPalette = [0xf20587, 0xf2d479, 0xf2c879, 0xf2b077, 0xf24405];
const scaleCurve = (age: number) => 1 - (1 - Math.min(age, 1 - age) * 2) ** 3;

export async function createExample(canvas: HTMLCanvasElement) {
    const response = await fetch(flowerUrl);
    if (!response.ok) throw new Error('Could not load Flower.glb.');
    const bytes = await response.arrayBuffer();
    const header = new DataView(bytes);
    const jsonLength = header.getUint32(12, true);
    const gltf: ExampleGltf = JSON.parse(new TextDecoder().decode(new Uint8Array(bytes, 20, jsonLength)));
    const transform = new Matrix4().makeRotationX(Math.PI).multiply(new Matrix4().makeScale(7, 7, 7));
    const makePart = (name: string, material: MeshLambertMaterial) => {
        const index = gltf.meshes.findIndex(mesh => mesh.name === name);
        const geometry = readExampleGltfGeometry(gltf, bytes, index, 28 + jsonLength).applyMatrix4(transform);
        const mesh = new InstancedMesh(geometry, material, capacity);
        mesh.boundingSphere = new Sphere(new Vector3(), 20);
        mesh.instanceMatrix.setUsage(Usage.DynamicDraw);
        return mesh;
    };
    const [r, g, b] = gltf.materials[1].pbrMetallicRoughness.baseColorFactor!;
    const stem = makePart('Stem', new MeshLambertMaterial(new Color(r, g, b)));
    const blossom = makePart('Blossom', new MeshLambertMaterial());
    for (let i = 0; i < capacity; i++) {
        blossom.setColorAt(i, Color.fromHex(blossomPalette[Math.floor(Math.random() * blossomPalette.length)]));
    }

    const surface = new Mesh(new TorusKnotGeometry(10, 3, 100, 16).toNonIndexed(), new MeshLambertMaterial(Color.fromHex(0xfff784)));
    const sampler = new MeshSurfaceSampler(surface);
    const dummy = new Object3D();
    const position = new Vector3();
    const normal = new Vector3();
    const ages = new Float32Array(capacity);
    const scales = new Float32Array(capacity);
    let distribution = 'random';
    const writeMatrix = (i: number) => {
        stem.setMatrixAt(i, dummy.matrix);
        blossom.setMatrixAt(i, dummy.matrix);
    };
    const resampleParticle = (i: number) => {
        sampler.sample(position, normal);
        dummy.position.copyFrom(position);
        dummy.scale.setScalar(scales[i]);
        dummy.lookAt(normal.add(position));
        dummy.updateMatrix();
        writeMatrix(i);
    };
    const resample = () => {
        sampler.setWeightAttribute(distribution === 'weighted' ? 'uv' : null).build();
        for (let i = 0; i < capacity; i++) {
            ages[i] = Math.random();
            scales[i] = scaleCurve(ages[i]);
            resampleParticle(i);
        }
        stem.instanceMatrix.markUpdated();
        blossom.instanceMatrix.markUpdated();
    };
    resample();

    const camera = new PerspectiveCamera(60, 1, 0.1, 100);
    camera.position.set(25, 25, 25);
    camera.lookAt(new Vector3());
    const scene = new Scene();
    scene.background = Color.fromHex(0xe39469);
    const light = new PointLight(Color.fromHex(0xaa8899), 2.5, 0, 0);
    light.position.set(50, -25, 75);
    scene.add(light);
    scene.add(new AmbientLight(Color.fromHex(0xffffff), 3));
    scene.add(stem);
    scene.add(blossom);
    scene.add(surface);

    const panel = examplePanel(canvas);
    const label = document.createElement('label');
    label.className = 'example-setting';
    label.textContent = 'Flower count';
    const count = document.createElement('input');
    count.type = 'range';
    count.min = '0';
    count.max = count.value = String(capacity);
    count.addEventListener('input', () => { stem.count = blossom.count = count.valueAsNumber; });
    label.append(count);
    panel.append(label);
    addSelect(panel, 'Distribution', { random: 'Random', weighted: 'UV weighted' }, distribution, value => {
        distribution = value;
        resample();
    });
    const button = document.createElement('button');
    button.textContent = 'Resample';
    button.addEventListener('click', resample);
    panel.append(button);

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const scale = new Vector3();
    let previous = 0;
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const step = Math.min(seconds - previous, 0.1) * 0.3;
            previous = seconds;
            for (let i = 0; i < stem.count; i++) {
                ages[i] += step;
                if (ages[i] >= 1 || scales[i] === 0) {
                    ages[i] = 0.001;
                    scales[i] = scaleCurve(ages[i]);
                    resampleParticle(i);
                } else {
                    const lastScale = scales[i];
                    scales[i] = scaleCurve(ages[i]);
                    stem.getMatrixAt(i, dummy.matrix);
                    dummy.matrix.scale(scale.setScalar(scales[i] / lastScale));
                    writeMatrix(i);
                }
            }
            stem.instanceMatrix.markUpdated();
            blossom.instanceMatrix.markUpdated();
            scene.rotation.set(Math.sin(seconds / 4), Math.sin(seconds / 2), 0);
            renderer.render(scene, camera);
        },
        dispose() { panel.remove(); renderer.dispose(); },
    };
}
