import {
    BufferGeometry, CatmullRomCurve3, Color, ColorSpace, Euler, Float32BufferAttribute, Line,
    LineBasicMaterial, PerspectiveCamera, Scene, Vector3, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { fitPerspective } from './fitPerspective';
import { followPointer } from './followPointer';

import { hilbert3D } from './hilbert3D';

export async function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    const camera = new PerspectiveCamera(33, 1, 1, 10000);
    camera.position.z = 1000;
    const corners = hilbert3D(new Vector3(), 200, 1);
    const spline = new CatmullRomCurve3(corners);
    const points = Array.from({ length: corners.length * 6 }, (_, index) => spline.getPoint(index / (corners.length * 6)));
    const material = new LineBasicMaterial();
    material.vertexColors = true;
    const color = new Color(1, 1, 1);
    const lines: Line[] = [];

    for (const [row, samples] of [points, corners].entries()) {
        const positions = new Float32BufferAttribute(Float32Array.from(samples.flatMap(p => [p.x, p.y, p.z])), 3);
        for (let column = 0; column < 3; column++) {
            const colors = new Float32Array(samples.length * 3);
            samples.forEach((p, i) => {
                if (column === 2) color.setHSL(i / samples.length, 1, 0.5, ColorSpace.SRGB);
                else if (row === 0) {
                    color.setHSL(column === 0 ? 0.6 : 0.9, 1, Math.max(0, -(column === 0 ? p.x : p.y) / 200) + 0.5, ColorSpace.SRGB);
                } else if (column === 0) color.setHSL(0.6, 1, Math.max(0, (200 - p.x) / 400) * 0.5 + 0.5, ColorSpace.SRGB);
                else color.setHSL(0.3, 1, Math.max(0, (200 + p.x) / 400) * 0.5, ColorSpace.SRGB);
                colors.set([color.r, color.g, color.b], i * 3);
            });
            const geometry = new BufferGeometry()
                .setAttribute('position', positions)
                .setAttribute('color', new Float32BufferAttribute(colors, 3));
            const line = new Line(geometry, material);
            line.scale.set(0.45, 0.45, 0.45);
            line.position.set((column - 1) * 225, (row - 0.5) * 225, 0);
            scene.add(line);
            lines.push(line);
        }
    }

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const pointer = followPointer(canvas);
    const rotation = new Euler();
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            pointer.update(camera, scene.position, 200);
            const time = Date.now() * 0.0005;
            lines.forEach((line, i) => line.quaternion.setFromEuler(rotation.set(0, time * (i % 2 ? 1 : -1), 0)));
            renderer.render(scene, camera);
        },
        dispose() {
            pointer.dispose();
            renderer.dispose();
        },
    };
}
