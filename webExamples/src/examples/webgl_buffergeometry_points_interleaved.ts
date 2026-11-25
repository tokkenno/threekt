import {
    BufferGeometry, Color, ColorSpace, Euler, Fog, InterleavedBuffer, InterleavedBufferAttribute, PerspectiveCamera, Points, PointsMaterial,
    Scene, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { fitPerspective } from './fitPerspective';

/** Port of Three.js examples/webgl_buffergeometry_points_interleaved.html, without the Stats panel. */
export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(27, 1, 5, 3500);
    camera.position.z = 2750;

    const scene = new Scene();
    scene.background = Color.fromHex(0x050505);
    scene.fog = new Fog(Color.fromHex(0x050505), 2000, 3500);

    const particles = 500000;
    // Position occupies the first 12 bytes and normalized color the last four.
    const storage = new ArrayBuffer(particles * 16);
    const positions = new Float32Array(storage);
    const colors = new Int8Array(storage);
    const color = new Color(1, 1, 1);

    for (let i = 0; i < particles; i++) {
        const x = Math.random() * 1000 - 500;
        const y = Math.random() * 1000 - 500;
        const z = Math.random() * 1000 - 500;
        positions[i * 4] = x;
        positions[i * 4 + 1] = y;
        positions[i * 4 + 2] = z;

        color.setRGB(x / 1000 + 0.5, y / 1000 + 0.5, z / 1000 + 0.5, ColorSpace.SRGB);
        colors[i * 16 + 12] = color.r * 255;
        colors[i * 16 + 13] = color.g * 255;
        colors[i * 16 + 14] = color.b * 255;
    }

    const geometry = new BufferGeometry();
    const positionsBuffer = InterleavedBuffer.fromFloat32(positions, 4);
    const colorsBuffer = InterleavedBuffer.fromUint8(colors, 16);
    geometry.setAttribute('position', new InterleavedBufferAttribute(positionsBuffer, 3, 0));
    geometry.setAttribute('color', new InterleavedBufferAttribute(colorsBuffer, 3, 12, true));

    const material = new PointsMaterial();
    material.size = 15;
    material.vertexColors = true;

    const points = new Points(geometry, material);
    scene.add(points);
    const rotation = new Euler();

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const time = Date.now() * 0.001;

            points.quaternion.setFromEuler(rotation.set(time * 0.25, time * 0.5, 0));

            renderer.render(scene, camera);
        },
        dispose() {
            renderer.dispose();
        },
    };
}
