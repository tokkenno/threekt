import {
    BufferGeometry, CatmullRomCurve3, Color, Euler, Float32BufferAttribute, Fog, Line,
    LineDashedMaterial, LineSegments, PerspectiveCamera, Scene, Vector3,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { fitPerspective } from './fitPerspective';
import { hilbert3D } from './hilbert3D';

export function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(60, 1, 1, 200);
    camera.position.z = 150;
    const scene = new Scene();
    scene.background = Color.fromHex(0x111111);
    scene.fog = new Fog(Color.fromHex(0x111111), 150, 200);

    const points = hilbert3D(new Vector3(), 25, 1);
    const spline = new CatmullRomCurve3(points);
    const divisions = points.length * 6;
    const position = new Float32Array((divisions + 1) * 3);
    const point = new Vector3();
    for (let i = 0; i <= divisions; i++) {
        spline.getPoint(i / divisions, point);
        position.set([point.x, point.y, point.z], i * 3);
    }
    const splineGeometry = new BufferGeometry().setAttribute('position', new Float32BufferAttribute(position, 3));
    const curveMaterial = new LineDashedMaterial(Color.fromHex(0xffffff));
    curveMaterial.dashSize = 1;
    curveMaterial.gapSize = 0.5;
    const curve = new Line(splineGeometry, curveMaterial);
    curve.computeLineDistances();
    scene.add(curve);

    const corners = [
        [-25, -25, -25], [-25, 25, -25], [25, 25, -25], [25, -25, -25],
        [-25, -25, 25], [-25, 25, 25], [25, 25, 25], [25, -25, 25],
    ];
    const edges = [0, 1, 1, 2, 2, 3, 3, 0, 4, 5, 5, 6, 6, 7, 7, 4, 0, 4, 1, 5, 2, 6, 3, 7];
    const box = new BufferGeometry().setAttribute(
        'position', new Float32BufferAttribute(Float32Array.from(edges.flatMap(i => corners[i])), 3),
    );
    const frame = new LineSegments(box, new LineDashedMaterial(Color.fromHex(0xffaa00)));
    frame.computeLineDistances();
    scene.add(frame);

    const rotation = new Euler();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            rotation.set(seconds * 0.25, seconds * 0.25, 0);
            curve.quaternion.setFromEuler(rotation);
            frame.quaternion.setFromEuler(rotation);
            renderer.render(scene, camera);
        },
        dispose() { renderer.dispose(); },
    };
}
