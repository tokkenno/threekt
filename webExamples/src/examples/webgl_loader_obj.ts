import {
    AmbientLight, Color, DirectionalLight, DomInputSurface, MTLLoader, OBJLoader, OrbitControls,
    PerspectiveCamera, Scene, WebGL2Context, WebGLRenderer,
} from 'threekt';
import materialsUrl from './assets/male02/male02.mtl?url';
import texture01Url from './assets/male02/01_-_Default1noCulling.JPG?url';
import texture02Url from './assets/male02/orig_02_-_Defaul1noCulling.JPG?url';
import textureMaleUrl from './assets/male02/male-02-1noCulling.JPG?url';
import modelUrl from './assets/male02/male02.obj?url';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

const textureUrls = new Map([
    ['01_-_Default1noCulling.JPG', texture01Url],
    ['orig_02_-_Defaul1noCulling.JPG', texture02Url],
    ['male-02-1noCulling.JPG', textureMaleUrl],
]);

async function loadText(url: string, format: string) {
    const response = await fetch(url);
    if (!response.ok) throw new Error(`Could not load the ${format} file (HTTP ${response.status}).`);
    return response.text();
}

export async function createExample(canvas: HTMLCanvasElement) {
    const [objText, mtlText] = await Promise.all([loadText(modelUrl, 'OBJ'), loadText(materialsUrl, 'MTL')]);
    const materials = new MTLLoader().parse(mtlText);
    await Promise.all(materials.textureFiles.map(async (fileName) => {
        const url = textureUrls.get(fileName);
        if (!url) throw new Error(`Textura MTL desconocida: ${fileName}`);
        materials.setTexture(fileName, await loadImageTexture(url));
    }));
    const model = new OBJLoader().setMaterials(materials).parse(objText);
    model.position.y = -0.95;
    model.scale.set(0.01, 0.01, 0.01);

    const camera = new PerspectiveCamera(45, 1, 0.1, 20);
    camera.position.z = 2.5;
    const scene = new Scene();
    scene.add(new AmbientLight(Color.fromHex(0xffffff)));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 2);
    light.position.set(1, 1, 2);
    scene.add(light);
    scene.add(model);

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.enableDamping = true;
    controls.minDistance = 2;
    controls.maxDistance = 5;

    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() {
            controls.dispose();
            renderer.dispose();
        },
    };
}
