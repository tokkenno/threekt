import {
    AmbientLight, Color, DirectionalLight, DomInputSurface, EdgesGeometry, LineBasicMaterial, LineSegments,
    Mesh, MeshPhongMaterial, OrbitControls, PerspectiveCamera, PlaneGeometry, Scene,
    Vector3, type Plane,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';

export function clippingScene(canvas: HTMLCanvasElement, ground: boolean) {
    const scene = new Scene();
    scene.background = Color.fromHex(ground ? 0x24272c : 0x151515);
    scene.add(new AmbientLight(Color.fromHex(0xb5bec7), 1.6));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 3);
    light.position.set(3, 6, 3);
    scene.add(light);
    if (ground) {
        const floor = new Mesh(new PlaneGeometry(9, 9), new MeshPhongMaterial(Color.fromHex(0xa0adaf)));
        floor.rotation.x = -Math.PI / 2;
        scene.add(floor);
    }
    const camera = new PerspectiveCamera(36, 1, 0.1, 30);
    camera.position.set(2.3, 2.7, 3.5);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.target.y = ground ? 1 : 0;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    renderer.localClippingEnabled = true;
    return { scene, camera, controls, renderer };
}

export function clippingPlaneHelper(plane: Plane, size: number, color: Color) {
    const mesh = new LineSegments(new EdgesGeometry(new PlaneGeometry(size, size)), new LineBasicMaterial(color));
    const z = new Vector3(0, 0, 1);
    const update = () => {
        plane.coplanarPoint(mesh.position);
        mesh.quaternion.setFromUnitVectors(z, plane.normal);
    };
    update();
    return { mesh, update };
}
