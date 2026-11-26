import {
    BufferGeometry, Color, Euler, Float32BufferAttribute, Group, Line, LineBasicMaterial, LineSegments,
    Mesh, MeshBasicMaterial, PerspectiveCamera, Raycaster, Scene, SphereGeometry, Vector3, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { fitPerspective } from './fitPerspective';
import { raycastPointer } from './raycastPointer';

export async function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    scene.background = Color.fromHex(0xf0f0f0);
    const camera = new PerspectiveCamera(70, 1, 1, 10000);
    const marker = new Mesh(new SphereGeometry(5), new MeshBasicMaterial(Color.fromHex(0xff0000)));
    scene.add(marker);
    const positions = new Float32Array(50 * 3);
    const point = new Vector3(), direction = new Vector3();
    const spread = (size: number) => size * (Math.random() - 0.5);
    for (let i = 0; i < 50; i++) {
        direction.x += spread(1); direction.y += spread(1); direction.z += spread(1);
        point.add(direction.normalize().multiplyScalar(10));
        positions.set([point.x, point.y, point.z], i * 3);
    }
    const geometry = new BufferGeometry().setAttribute('position', new Float32BufferAttribute(positions, 3));
    const parent = new Group();
    function randomTransform(object: Group | Line, size: number) {
        object.position.set(spread(size), spread(size), spread(size));
        object.quaternion.setFromEuler(new Euler(spread(2 * Math.PI), spread(2 * Math.PI), spread(2 * Math.PI)));
        object.scale.set(1 + spread(1), 1 + spread(1), 1 + spread(1));
    }
    randomTransform(parent, 40);
    for (let i = 0; i < 50; i++) {
        const material = new LineBasicMaterial(Color.fromHex(Math.floor(Math.random() * 0x1000000)));
        const line = Math.random() > 0.5 ? new Line(geometry, material) : new LineSegments(geometry, material);
        randomTransform(line, 400);
        parent.add(line);
    }
    scene.add(parent);
    const input = raycastPointer(canvas);
    const raycaster = new Raycaster();
    raycaster.params.Line.threshold = 3;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));

    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const angle = seconds * Math.PI / 30;
            camera.position.set(100 * Math.sin(angle), 100 * Math.sin(angle), 100 * Math.cos(angle));
            camera.lookAt(scene.position);
            raycaster.setFromCamera(input.pointer, camera);
            const hit = raycaster.intersectObject(parent)[0];
            marker.visible = !!hit;
            if (hit) marker.position.set(hit.point.x, hit.point.y, hit.point.z);
            renderer.render(scene, camera);
        },
        dispose() { input.dispose(); renderer.dispose(); },
    };
}
