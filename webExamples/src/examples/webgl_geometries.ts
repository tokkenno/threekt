import {
    AmbientLight, BoxGeometry, CapsuleGeometry, CircleGeometry, Color, ColorSpace, CylinderGeometry, Euler, IcosahedronGeometry,
    LatheGeometry, Mesh, MeshPhongMaterial, OctahedronGeometry, ParametricFunctions, ParametricGeometry, PerspectiveCamera,
    PlaneGeometry, PointLight, RingGeometry, Scene, Side, SphereGeometry, TetrahedronGeometry, TorusGeometry,
    TorusKnotGeometry, Vector2, Vector3, Wrapping, type BufferGeometry,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import uvGridUrl from './assets/uv_grid_opengl.jpg?url';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

/** Port of Three.js examples/webgl_geometries.html, without the Stats panel. */
export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(45, 1, 1, 2000);
    camera.position.y = 500;
    const scene = new Scene();
    scene.add(new AmbientLight(Color.fromHex(0xcccccc), 1.5));
    camera.add(new PointLight(Color.fromHex(0xffffff), 2.5, 0, 0));
    scene.add(camera);

    const map = await loadImageTexture(uvGridUrl);
    map.wrapS = map.wrapT = Wrapping.Repeat;
    map.anisotropy = 16;
    map.colorSpace = ColorSpace.SRGB;
    const material = new MeshPhongMaterial();
    material.map = map;
    material.side = Side.Double;

    const meshes: Mesh[] = [];
    const add = (geometry: BufferGeometry, x: number, z: number, scale = 1) => {
        const object = new Mesh(geometry, material);
        object.position.set(x, 0, z);
        object.scale.multiplyScalar(scale);
        scene.add(object);
        meshes.push(object);
    };

    add(new SphereGeometry(75, 20, 10), -300, 300);
    add(new IcosahedronGeometry(75), -100, 300);
    add(new OctahedronGeometry(75), 100, 300);
    add(new TetrahedronGeometry(75), 300, 300);

    add(new PlaneGeometry(100, 100, 4, 4), -300, 100);
    add(new BoxGeometry(100, 100, 100, 4, 4, 4), -100, 100);
    add(new CircleGeometry(50, 20, 0, Math.PI * 2), 100, 100);
    add(new RingGeometry(10, 50, 20, 5, 0, Math.PI * 2), 300, 100);

    add(new CylinderGeometry(25, 75, 100, 40, 5), -300, -100);
    const points: Vector2[] = [];
    for (let i = 0; i < 50; i++) points.push(new Vector2(Math.sin(i * 0.2) * Math.sin(i * 0.1) * 15 + 50, (i - 5) * 2));
    add(new LatheGeometry(points, 20), -100, -100);
    add(new TorusGeometry(50, 20, 20, 20), 100, -100);
    add(new TorusKnotGeometry(50, 10, 50, 20), 300, -100);

    add(new CapsuleGeometry(20, 50), -300, -300);
    const functions = ParametricFunctions.getInstance();
    const plane = new ParametricGeometry((u, v, target) => functions.plane(u, v, target), 10, 10);
    plane.scale(100, 100, 100);
    plane.center();
    add(plane, -100, -300);
    add(new ParametricGeometry((u, v, target) => functions.klein(u, v, target), 20, 20), 100, -300, 5);
    add(new ParametricGeometry((u, v, target) => functions.mobius(u, v, target), 20, 20), 300, -300, 30);

    const rotation = new Euler();
    const origin = new Vector3();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            const timer = Date.now() * 0.0001;
            camera.position.x = Math.cos(timer) * 800;
            camera.position.z = Math.sin(timer) * 800;
            camera.lookAt(origin);
            rotation.set(timer * 5, timer * 2.5, 0);
            for (const mesh of meshes) mesh.quaternion.setFromEuler(rotation);
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() {
            renderer.dispose();
        },
    };
}
