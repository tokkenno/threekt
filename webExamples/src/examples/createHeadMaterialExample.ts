import { Color, DomInputSurface, Mesh, OrbitControls, PerspectiveCamera, Scene,
    WebGL2Context, WebGLRenderer } from 'threekt';
import head from './assets/lee-perry-smith/LeePerrySmith.glb?url';
import nefertiti from './assets/nefertiti/Nefertiti.glb?url';
import bumpUrl from './assets/lee-perry-smith/Infinite-Level_02_Disp_NoSmoothUV-4096.jpg?url';
import normalUrl from './assets/lee-perry-smith/Infinite-Level_02_Tangent_SmoothUV.jpg?url';
import diffuseUrl from './assets/lee-perry-smith/Map-COL.jpg?url';
import specularUrl from './assets/lee-perry-smith/Map-SPEC.jpg?url';
import matcapUrl from './assets/matcap/040full.png?url';
import { addSliders } from './addSliders';
import { addToggle, examplePanel } from './exampleControls';
import { loadExampleGlb, loadGlbImage } from './exampleGlb';
import { exampleMappedMaterial } from './exampleMappedMaterial';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';
import { readExampleGltfGeometry, type ExampleGltf } from './readExampleGltf';

type ImageGlb = ExampleGltf & { images: { bufferView: number; mimeType: string }[] };
export async function createHeadMaterialExample(canvas: HTMLCanvasElement, mode: 'bump' | 'normal' | 'object' | 'matcap') {
    const glb = await loadExampleGlb<ImageGlb>(mode === 'object' ? nefertiti : head);
    const geometry = readExampleGltfGeometry(glb.json, glb.buffer, 0, glb.start);
    geometry.center();
    const bounds = geometry.boundingBox!;
    const scale = 4 / (bounds.max.y - bounds.min.y);
    const maps = await (async () => {
        switch (mode) {
            case 'bump': return { bump: await loadImageTexture(bumpUrl) };
            case 'normal': {
                const [normal, diffuse, specular] = await Promise.all([normalUrl,diffuseUrl,specularUrl].map(loadImageTexture));
                return { normal, diffuse, specular };
            }
            case 'object': {
                const [diffuse, normal] = await Promise.all(glb.json.images.map(image => loadGlbImage(glb, image)));
                return { diffuse, normal };
            }
            case 'matcap': {
                const [normal, matcap] = await Promise.all([normalUrl,matcapUrl].map(loadImageTexture));
                return { normal, matcap };
            }
        }
    })();
    for (const texture of Object.values(maps)) texture.flipY = false;
    if (maps.matcap) maps.matcap.flipY = true;
    const shader = exampleMappedMaterial(maps, mode === 'object');
    const mesh = new Mesh(geometry, shader.material);
    mesh.scale.setScalar(scale);
    const scene = new Scene();
    scene.background = Color.fromHex(mode === 'normal' ? 0x494949 : 0x14171c);
    scene.add(mesh);
    const camera = new PerspectiveCamera(32, 1, 0.1, 100);
    camera.position.set(mode === 'object' ? -2.6 : 0, 0.35, 9);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.enablePan = false;
    controls.minDistance = 4;
    controls.maxDistance = 18;
    const panel = examplePanel(canvas);
    if (mode === 'matcap') {
        const label = document.createElement('label');
        label.className = 'example-setting';
        label.textContent = 'Tint';
        const tint = document.createElement('input');
        tint.type = 'color';
        tint.value = '#ffffff';
        tint.addEventListener('input', () => shader.tint.setStyle(tint.value));
        label.append(tint);
        panel.append(label);
        addSliders(canvas, [{ label: 'Exposure', min: 0, max: 2, step: 0.02, value: 1,
            change: value => { shader.exposure.value = value; } }], panel);
    }
    const name = mode === 'bump' ? 'Bump map' : 'Normal map';
    const amount = mode === 'bump' ? 10 : 1;
    if (mode === 'bump') shader.tint.setHex(0x9c6e49);
    shader.scale.setScalar(amount);
    addToggle(panel, name, true, value => { shader.normalEnabled.value = value; });
    addSliders(canvas, [{ label: name + ' scale', min: 0, max: mode === 'bump' ? 40 : 2, step: 0.05, value: amount,
        change: value => shader.scale.setScalar(value) }], panel);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            shader.lightAngle.value = mode === 'bump' ? seconds * 0.7 : 0.7;
            renderer.render(scene, camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); panel.remove(); },
    };
}
