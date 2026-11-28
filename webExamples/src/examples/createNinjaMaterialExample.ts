import { Color, Mesh, OrthographicCamera, PerspectiveCamera, Scene, Side } from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { OBJLoader } from 'threekt/loaders';
import objectUrl from './assets/ninja/ninjaHead_Low.obj?url';
import normalUrl from './assets/ninja/normal.png?url';
import aoUrl from './assets/ninja/ao.jpg?url';
import displacementUrl from './assets/ninja/displacement.jpg?url';
import { addSliders } from './addSliders';
import { addSelect, examplePanel } from './exampleControls';
import { exampleMappedMaterial } from './exampleMappedMaterial';
import { loadImageTexture } from './loadImageTexture';

export async function createNinjaMaterialExample(canvas: HTMLCanvasElement, channels: boolean) {
    const [response, normal, occlusion, displacement] = await Promise.all([
        fetch(objectUrl), loadImageTexture(normalUrl), loadImageTexture(aoUrl), loadImageTexture(displacementUrl),
    ]);
    if (!response.ok) throw new Error('Could not load the ninja head.');
    const root = new OBJLoader().parse(await response.text());
    const source = root.children.asJsReadonlyArrayView().find(node => node instanceof Mesh);
    if (!source) throw new Error('Expected a mesh in the ninja OBJ.');
    const geometry = source.geometry.center();
    const bounds = geometry.boundingBox!;
    const shader = exampleMappedMaterial({ normal, occlusion, displacement });
    shader.displacement.value = 2.436143;
    shader.tint.setHex(0x96867e);
    shader.scale.y = -1;
    const mesh = new Mesh(geometry, shader.material);
    mesh.scale.setScalar(4 / (bounds.max.y - bounds.min.y));
    const scene = new Scene();
    scene.background = Color.fromHex(0x1c2531);
    scene.add(mesh);
    const perspective = new PerspectiveCamera(36, 1, 4, 20);
    const ortho = new OrthographicCamera(-3, 3, 3, -3, 4, 20);
    perspective.position.set(0, 0, 10);
    ortho.position.copyFrom(perspective.position);
    const controls = new OrbitControls(perspective, new DomInputSurface(canvas));
    controls.enablePan = false;
    controls.minDistance = 6;
    controls.maxDistance = 13;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas, false));
    const panel = examplePanel(canvas);
    let useOrtho = !channels;
    if (channels) {
        shader.channel.value = 1;
        addSelect(panel, 'Channel', { 0: 'Lit surface', 1: 'Normals', 2: 'Depth', 3: 'Packed RGB depth',
            4: 'Packed RG depth', 5: 'Packed RGBA depth', 6: 'Velocity' }, '1', value => { shader.channel.value = Number(value); });
        addSelect(panel, 'Camera', { p: 'Perspective', o: 'Orthographic' }, 'p', value => { useOrtho = value === 'o'; });
        addSelect(panel, 'Side', { front: 'Front', back: 'Back', double: 'Double' }, 'double', value => {
            shader.material.side = value === 'front' ? Side.Front : value === 'back' ? Side.Back : Side.Double;
        });
    } else {
        addSliders(canvas, [
            { label: 'Displacement', min: 0, max: 3, step: 0.01, value: 2.436143, change: value => { shader.displacement.value = value; } },
            { label: 'Normal scale', min: -1, max: 1, step: 0.02, value: 1, change: value => { shader.scale.set(value, -value); } },
            { label: 'Ambient occlusion', min: 0, max: 1, step: 0.02, value: 1, change: value => { shader.aoIntensity.value = value; } },
            { label: 'Ambient light', min: 0, max: 1, step: 0.02, value: 0.26, change: value => { shader.ambient.value = value; } },
            { label: 'Metalness', min: 0, max: 1, step: 0.02, value: 0, change: value => { shader.metalness.value = value; } },
            { label: 'Roughness', min: 0, max: 1, step: 0.02, value: 0.55, change: value => { shader.roughness.value = value; } },
        ], panel);
    }
    return {
        render(seconds: number) {
            const { clientWidth: width, clientHeight: height } = canvas;
            if (!width || !height) return;
            perspective.aspect = width / height;
            perspective.updateProjectionMatrix();
            controls.update();
            ortho.position.copyFrom(perspective.position);
            ortho.quaternion.copyFrom(perspective.quaternion);
            ortho.right = 3 * width / height;
            ortho.left = -ortho.right;
            ortho.updateProjectionMatrix();
            mesh.rotation.y = Math.sin(seconds * 0.55) * 0.42;
            shader.lightAngle.value = seconds * 0.85;
            renderer.setSize(width, height, window.devicePixelRatio);
            renderer.render(scene, useOrtho ? ortho : perspective);
            shader.previous.copyFrom(mesh.modelViewMatrix);
        },
        dispose() { panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
