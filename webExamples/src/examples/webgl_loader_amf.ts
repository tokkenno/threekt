import {
    AmbientLight, BufferGeometry, Color, DomInputSurface, Float32BufferAttribute, GridHelper, Mesh,
    MeshPhongMaterial, OrbitControls, PerspectiveCamera, PointLight, Scene, Side, WebGL2Context, WebGLRenderer,
} from 'threekt';
import rook from './assets/amf/rook.amf?url';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const response = await fetch(rook);
    if (!response.ok) throw new Error('Could not load the AMF rook.');
    const document = new DOMParser().parseFromString(await response.text(), 'application/xml');
    if (document.documentElement.localName !== 'amf') throw new Error('Expected AMF XML.');
    const scale: Record<string, number> = { millimeter: 1, meter: 1000, inch: 25.4, feet: 304.8, micron: 0.001 };
    const unit = scale[document.documentElement.getAttribute('unit') ?? 'millimeter'] ?? 1;
    const numberAt = (node: Element, name: string) => Number(node.querySelector(name)?.textContent);
    const scene = new Scene();
    scene.background = Color.fromHex(0x999999);
    scene.add(new AmbientLight(Color.fromHex(0x999999)));
    for (const mesh of document.querySelectorAll('object > mesh')) {
        const position = Float32Array.from(Array.from(mesh.querySelectorAll('vertex > coordinates')).flatMap(
            vertex => ['x', 'y', 'z'].map(axis => unit * numberAt(vertex, axis))));
        const material = new MeshPhongMaterial(Color.fromHex(0xaaaaff));
        material.side = Side.Double;
        for (const volume of mesh.querySelectorAll('volume')) {
            const indices = Int32Array.from(Array.from(volume.querySelectorAll('triangle')).flatMap(
                triangle => ['v1', 'v2', 'v3'].map(key => numberAt(triangle, key))));
            const geometry = new BufferGeometry().setAttribute('position', new Float32BufferAttribute(position, 3));
            geometry.setIndexArray(indices);
            geometry.computeVertexNormals();
            scene.add(new Mesh(geometry, material));
        }
    }
    const camera = new PerspectiveCamera(35, 1, 1, 500);
    camera.up.set(0, 0, 1);
    camera.position.set(0, -9, 6);
    camera.add(new PointLight(Color.fromHex(0xffffff), 250));
    scene.add(camera);
    const grid = new GridHelper(50, 50, Color.fromHex(0xffffff), Color.fromHex(0x555555));
    grid.rotation.x = Math.PI / 2;
    scene.add(grid);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.target.set(0, 0, 2);
    controls.enableZoom = false;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); },
    };
}
