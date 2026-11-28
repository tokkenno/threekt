import {
    AmbientLight, Color, DirectionalLight, Mesh, MeshBasicMaterial, MeshLambertMaterial, MeshPhongMaterial,
    PerspectiveCamera, Scene, Side,
} from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { TeapotGeometry } from 'threekt/geometry';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import texture from './assets/uv_grid_opengl.jpg?url';
import { loadExampleCubeMap } from './exampleCubeMap';
import { addSelect, addToggle, examplePanel } from './exampleControls';
import { environmentMaterial } from './exampleEnvironment';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

export async function createExample(canvas: HTMLCanvasElement) {
    const [map, environment] = await Promise.all([loadImageTexture(texture), loadExampleCubeMap('SwedishRoyalCastle')]);
    const wireframe = new MeshBasicMaterial();
    wireframe.wireframe = true;
    const flat = new MeshPhongMaterial();
    flat.flatShading = true;
    flat.specular.setHex(0);
    const glossy = new MeshPhongMaterial(Color.fromHex(0xc0c0c0));
    glossy.specular.setHex(0x404040);
    glossy.shininess = 300;
    const textured = new MeshPhongMaterial();
    textured.map = map;
    const styles = { wireframe, flat, smooth: new MeshLambertMaterial(), glossy, textured,
        reflective: environmentMaterial(environment) };
    for (const material of Object.values(styles)) material.side = Side.Double;
    let segments = 15, bottom = true, lid = true, body = true, fitLid = true, blinn = true;
    const mesh = new Mesh(new TeapotGeometry(1.4, segments, bottom, lid, body, fitLid, blinn), glossy);
    const rebuild = () => {
        mesh.geometry.dispose();
        mesh.geometry = new TeapotGeometry(1.4, segments, bottom, lid, body, fitLid, blinn);
        mesh.visible = body || lid || bottom;
    };
    const scene = new Scene();
    scene.background = Color.fromHex(0xaaaaaa);
    scene.add(mesh);
    scene.add(new AmbientLight(Color.fromHex(0x7c7c7c), 2));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 2);
    light.position.set(-2, 4, 4);
    scene.add(light);
    const camera = new PerspectiveCamera(38, 1, 0.1, 100);
    camera.position.set(4.8, 2.9, 6.6);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const panel = examplePanel(canvas);
    addSelect(panel, 'Tessellation', Object.fromEntries([2,3,4,5,6,8,10,15,20,30,40,50].map(n => [n,String(n)])),
        '15', value => { segments = Number(value); rebuild(); });
    addSelect(panel, 'Shading', Object.fromEntries(Object.keys(styles).map(name => [name, name])), 'glossy', value => {
        mesh.material = styles[value as keyof typeof styles];
    });
    addToggle(panel, 'Lid', true, value => { lid = value; rebuild(); });
    addToggle(panel, 'Body', true, value => { body = value; rebuild(); });
    addToggle(panel, 'Bottom', true, value => { bottom = value; rebuild(); });
    addToggle(panel, 'Snug lid', true, value => { fitLid = value; rebuild(); });
    addToggle(panel, 'Original proportions', false, value => { blinn = !value; rebuild(); });
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() { panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
