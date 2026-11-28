import {
    BufferGeometry, Color, ColorSpace, Euler, Fog, InterleavedBuffer, InterleavedBufferAttribute,
    PerspectiveCamera, Points, PointsMaterial,
    Scene,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { fitPerspective } from './fitPerspective';

/** Port of Three.js examples/webgl_buffergeometry_points_interleaved.html, without the Stats panel. */
export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(27, 1, 5, 3500);
    camera.position.z = 2750;

    const scene = new Scene();
    scene.background = Color.fromHex(0x050505);
    scene.fog = new Fog(Color.fromHex(0x050505), 2000, 3500);

    //

    const particles = 500000;

    const geometry = new BufferGeometry();

    // Every particle occupies 16 bytes: three floats, three color bytes and one padding byte.
    const arrayBuffer = new ArrayBuffer(particles * 16);
    const interleavedFloat32Buffer = new Float32Array(arrayBuffer);
    const interleavedUint8Buffer = new Int8Array(arrayBuffer);

    const color = new Color(1, 1, 1);
    const n = 1000, n2 = n / 2;

    for (let i = 0; i < interleavedFloat32Buffer.length; i += 4) {
        const x = Math.random() * n - n2;
        const y = Math.random() * n - n2;
        const z = Math.random() * n - n2;

        interleavedFloat32Buffer[i] = x;
        interleavedFloat32Buffer[i + 1] = y;
        interleavedFloat32Buffer[i + 2] = z;

        color.setRGB(x / n + 0.5, y / n + 0.5, z / n + 0.5, ColorSpace.SRGB);

        const j = (i + 3) * 4;
        interleavedUint8Buffer[j] = color.r * 255;
        interleavedUint8Buffer[j + 1] = color.g * 255;
        interleavedUint8Buffer[j + 2] = color.b * 255;
    }

    const interleavedBuffer32 = InterleavedBuffer.fromFloat32Array(interleavedFloat32Buffer, 4);
    const interleavedBuffer8 = InterleavedBuffer.fromUint8Array(interleavedUint8Buffer, 16);

    geometry.setAttribute('position', new InterleavedBufferAttribute(interleavedBuffer32, 3, 0));
    geometry.setAttribute('color', new InterleavedBufferAttribute(interleavedBuffer8, 3, 12, true));

    geometry.computeBoundingSphere();

    //

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
