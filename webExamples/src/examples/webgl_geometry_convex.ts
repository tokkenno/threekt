import {
    AmbientLight, AxesHelper, BufferGeometry, Color, ConvexGeometry, DodecahedronGeometry, DomInputSurface,
    Float32BufferAttribute, Group, Mesh, MeshLambertMaterial, OrbitControls, PerspectiveCamera, PointLight,
    Points, PointsMaterial, Scene, Side, Vector3,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import disc from './assets/disc.png';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

export async function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    const camera = new PerspectiveCamera(40, 1, 1, 1000);
    camera.position.set(15, 20, 30);
    camera.add(new PointLight(Color.fromHex(0xffffff), 3, 0, 0));
    scene.add(camera);
    scene.add(new AmbientLight(Color.fromHex(0x666666)));
    scene.add(new AxesHelper(20));
    const group = new Group();
    scene.add(group);

    const position = new DodecahedronGeometry(10).getAttribute('position')!;
    const unique = new Map<string, Vector3>();
    for (let i = 0; i < position.count; i++) {
        const point = new Vector3(position.getX(i), position.getY(i), position.getZ(i));
        unique.set([point.x, point.y, point.z].map(value => Math.round(value * 10000)).join(','), point);
    }
    const vertices = Array.from(unique.values());
    const pointsMaterial = new PointsMaterial(Color.fromHex(0x0080ff));
    pointsMaterial.size = 1;
    pointsMaterial.map = await loadImageTexture(disc);
    pointsMaterial.alphaTest = 0.5;
    const coordinates = Float32Array.from(vertices.flatMap(({ x, y, z }) => [x, y, z]));
    group.add(new Points(new BufferGeometry().setAttribute('position', new Float32BufferAttribute(coordinates, 3)), pointsMaterial));
    const hullMaterial = new MeshLambertMaterial();
    hullMaterial.opacity = 0.5;
    hullMaterial.transparent = true;
    hullMaterial.side = Side.Double;
    group.add(new Mesh(new ConvexGeometry(vertices), hullMaterial));
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 20;
    controls.maxDistance = 50;
    controls.maxPolarAngle = Math.PI / 2;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            group.rotation.y = seconds * 0.3;
            renderer.render(scene, camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); },
    };
}
