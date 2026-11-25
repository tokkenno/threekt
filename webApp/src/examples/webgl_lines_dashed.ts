import {
    BufferGeometry, CatmullRomCurve3, Color, Euler, Float32BufferAttribute, Fog, Line, LineDashedMaterial,
    LineSegments, PerspectiveCamera, Scene, Vector3, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { fitPerspective } from './fitPerspective';
import { hilbert3D } from './hilbert3D';

export async function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    scene.background = Color.fromHex(0x111111);
    scene.fog = new Fog(Color.fromHex(0x111111), 150, 200);
    const camera = new PerspectiveCamera(60, 1, 1, 200);
    camera.position.z = 150;

    const points = hilbert3D(new Vector3(), 25, 1);
    const spline = new CatmullRomCurve3(points);
    const divisions = points.length * 6;
    const samples = Array.from({ length: divisions + 1 }, (_, i) => spline.getPoint(i / divisions));
    const positions = Float32Array.from(samples.flatMap(p => [p.x, p.y, p.z]));
    const material = new LineDashedMaterial(Color.fromHex(0xffffff));
    material.dashSize = 1;
    material.gapSize = 0.5;
    const line = new Line(new BufferGeometry().setAttribute('position', new Float32BufferAttribute(positions, 3)), material);
    line.computeLineDistances();
    scene.add(line);

    const corners = [[-25,-25,-25],[-25,25,-25],[25,25,-25],[25,-25,-25],[-25,-25,25],[-25,25,25],[25,25,25],[25,-25,25]];
    const edges = [0,1,1,2,2,3,3,0,4,5,5,6,6,7,7,4,0,4,1,5,2,6,3,7];
    const boxMaterial = new LineDashedMaterial(Color.fromHex(0xffaa00));
    const box = new LineSegments(new BufferGeometry().setAttribute('position',
        new Float32BufferAttribute(Float32Array.from(edges.flatMap(i => corners[i])), 3)), boxMaterial);
    box.computeLineDistances();
    scene.add(box);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const rotation = new Euler();
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            rotation.set(seconds * 0.25, seconds * 0.25, 0);
            line.quaternion.setFromEuler(rotation);
            box.quaternion.setFromEuler(rotation);
            renderer.render(scene, camera);
        },
        dispose() { renderer.dispose(); },
    };
}
