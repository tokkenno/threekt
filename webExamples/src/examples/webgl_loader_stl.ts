import {
    Color, DirectionalLight, Euler, Fog, HemisphereLight, Mesh, MeshPhongMaterial, PerspectiveCamera, PlaneGeometry,
    Scene, Vector3,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { STLLoader } from 'threekt/loaders';
import diskUrl from './assets/stl/slotted_disk.stl?url';
import panUrl from './assets/stl/pr2_head_pan.stl?url';
import tiltUrl from './assets/stl/pr2_head_tilt.stl?url';
import coloredUrl from './assets/stl/colored.stl?url';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(35, 1, 1, 15);
    const target = new Vector3(0, -0.25, 0);
    const scene = new Scene();
    scene.background = Color.fromHex(0x72645b);
    scene.fog = new Fog(Color.fromHex(0x72645b), 2, 15);
    const floorMaterial = new MeshPhongMaterial(Color.fromHex(0xcbcbcb));
    floorMaterial.specular.setHex(0x474747);
    const floor = new Mesh(new PlaneGeometry(40, 40), floorMaterial);
    floor.position.y = -0.5;
    floor.quaternion.setFromEuler(new Euler(-Math.PI / 2, 0, 0));
    scene.add(floor);

    const loader = new STLLoader();
    const files = [diskUrl, panUrl, tiltUrl, coloredUrl];
    const geometries = await Promise.all(files.map(async url => {
        const response = await fetch(url);
        if (!response.ok) throw new Error(`Could not load the STL (HTTP ${response.status}).`);
        return loader.parse(new Int8Array(await response.arrayBuffer()));
    }));
    const transforms = [
        { x: 0, y: -0.25, z: 0.6, rx: 0, ry: -Math.PI / 2, scale: 0.5 },
        { x: 0, y: -0.37, z: -0.6, rx: -Math.PI / 2, ry: 0, scale: 2 },
        { x: 0.136, y: -0.37, z: -0.6, rx: -Math.PI / 2, ry: 0.3, scale: 2 },
        { x: 0.5, y: 0.2, z: 0, rx: -Math.PI / 2, ry: Math.PI / 2, scale: 0.3 },
    ];
    geometries.forEach((geometry, index) => {
        const material = new MeshPhongMaterial(Color.fromHex(geometry.hasColors ? 0xffffff : index === 0 ? 0xff9c7c : 0xd5d5d5));
        material.vertexColors = geometry.hasColors;
        material.opacity = geometry.alpha;
        material.transparent = geometry.alpha < 1;
        if (!geometry.hasColors) {
            material.specular.setHex(0x494949);
            material.shininess = 200;
        }
        const { x, y, z, rx, ry, scale } = transforms[index];
        const mesh = new Mesh(geometry, material);
        mesh.position.set(x, y, z);
        mesh.quaternion.setFromEuler(new Euler(rx, ry, 0));
        mesh.scale.set(scale, scale, scale);
        scene.add(mesh);
    });
    scene.add(new HemisphereLight(Color.fromHex(0x8d7c7c), Color.fromHex(0x494966), 3));
    const white = new DirectionalLight(Color.fromHex(0xffffff), 3.5);
    white.position.set(1, 1, 1);
    scene.add(white);
    const yellow = new DirectionalLight(Color.fromHex(0xffd500), 3);
    yellow.position.set(0.5, 1, -1);
    scene.add(yellow);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            camera.position.set(Math.cos(seconds * 0.5) * 3, 0.15, Math.sin(seconds * 0.5) * 3);
            camera.lookAt(target);
            renderer.render(scene, camera);
        },
        dispose() { renderer.dispose(); },
    };
}
