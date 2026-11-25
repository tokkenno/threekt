import {
    BufferGeometry, CatmullRomCurve3, Color, ColorSpace, Euler, Float32BufferAttribute, Line,
    LineBasicMaterial, PerspectiveCamera, Scene, Vector3, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { followPointer } from './exampleControls';
import { fitPerspective } from './fitPerspective';
import { hilbert3D } from './hilbert3D';

export function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(33, 1, 1, 10000);
    camera.position.z = 1000;
    const scene = new Scene();
    const points = hilbert3D(new Vector3(), 200);
    const spline = new CatmullRomCurve3(points);
    const smooth = Array.from({ length: points.length * 6 }, (_, i) => spline.getPoint(i / (points.length * 6)));
    const material = new LineBasicMaterial();
    material.vertexColors = true;
    const lines: Line[] = [];
    const color = Color.fromHex(0xffffff);

    for (const [row, samples] of [smooth, points].entries()) {
        const positions = new Float32BufferAttribute(Float32Array.from(samples.flatMap(p => [p.x, p.y, p.z])), 3);
        for (let column = 0; column < 3; column++) {
            const colors = new Float32Array(samples.length * 3);
            for (const [i, point] of samples.entries()) {
                if (column === 2) color.setHSL(i / samples.length, 1, 0.5, ColorSpace.SRGB);
                else if (row === 0) {
                    const value = column === 0 ? point.x : point.y;
                    color.setHSL(column === 0 ? 0.6 : 0.9, 1, Math.max(0, -value / 200) + 0.5, ColorSpace.SRGB);
                } else {
                    const factor = column === 0 ? (200 - point.x) : (200 + point.x);
                    color.setHSL(column === 0 ? 0.6 : 0.3, 1, Math.max(0, factor / 400) * 0.5 + 0.5, ColorSpace.SRGB);
                }
                colors.set([color.r, color.g, color.b], i * 3);
            }
            const geometry = new BufferGeometry().setAttribute('position', positions)
                .setAttribute('color', new Float32BufferAttribute(colors, 3));
            const line = new Line(geometry, material);
            line.scale.setScalar(0.45);
            line.position.set((column - 1) * 225, (row - 0.5) * 225, 0);
            lines.push(line);
            scene.add(line);
        }
    }

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const pointer = followPointer(canvas, camera, scene, 200);
    const rotation = new Euler();
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            pointer.update();
            lines.forEach((line, index) => line.quaternion.setFromEuler(rotation.set(0, seconds * (index % 2 ? 0.5 : -0.5), 0)));
            renderer.render(scene, camera);
        },
        dispose() {
            pointer.dispose();
            renderer.dispose();
        },
    };
}
