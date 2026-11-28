import {
    Color, DirectionalLight, Fog, HemisphereLight, Mesh, MeshPhongMaterial, PerspectiveCamera, PlaneGeometry, Scene,
    Vector3,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { PLYLoader } from 'threekt/loaders';
import dolphins from './assets/ply/dolphins.ply?url';
import lucy from './assets/ply/Lucy100k.ply?url';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const loader = new PLYLoader();
    const [ascii, binary] = await Promise.all([dolphins, lucy].map(async url => {
        const response = await fetch(url);
        if (!response.ok) throw new Error(`Could not load PLY (HTTP ${response.status}).`);
        const geometry = loader.parse(new Int8Array(await response.arrayBuffer()));
        geometry.computeVertexNormals();
        return geometry;
    }));
    const scene = new Scene();
    scene.background = Color.fromHex(0x72645b);
    scene.fog = new Fog(Color.fromHex(0x72645b), 2, 15);
    const ground = new MeshPhongMaterial(Color.fromHex(0xcbcbcb));
    ground.specular.setHex(0x474747);
    const floor = new Mesh(new PlaneGeometry(40, 40), ground);
    floor.rotation.x = -Math.PI / 2;
    floor.position.y = -0.5;
    scene.add(floor);

    const material = new MeshPhongMaterial(Color.fromHex(0x009cff));
    material.flatShading = true;
    const pod = new Mesh(ascii, material);
    pod.position.set(0, -0.2, 0.3);
    pod.rotation.x = -Math.PI / 2;
    pod.scale.setScalar(0.001);
    scene.add(pod);
    const sculpture = new Mesh(binary, material);
    sculpture.position.set(-0.2, -0.02, -0.2);
    sculpture.scale.setScalar(0.0006);
    scene.add(sculpture);

    scene.add(new HemisphereLight(Color.fromHex(0x8d7c7c), Color.fromHex(0x494966), 3));
    for (const [hex, intensity, x, z] of [[0xffffff, 3.5, 1, 1], [0xffd500, 3, 0.5, -1]]) {
        const light = new DirectionalLight(Color.fromHex(hex), intensity);
        light.position.set(x, 1, z);
        scene.add(light);
    }
    const camera = new PerspectiveCamera(35, 1, 1, 15);
    const target = new Vector3(0, -0.1, 0);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            camera.position.set(Math.sin(seconds * 0.5) * 2.5, 0.15, Math.cos(seconds * 0.5) * 2.5);
            camera.lookAt(target);
            renderer.render(scene, camera);
        },
        dispose() { renderer.dispose(); },
    };
}
