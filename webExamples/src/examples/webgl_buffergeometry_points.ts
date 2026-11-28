import {
    BufferGeometry, Color, ColorSpace, Euler, Float32BufferAttribute, Fog, PerspectiveCamera, Points, PointsMaterial,
    Scene,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { fitPerspective } from './fitPerspective';

/** Port of Three.js examples/webgl_buffergeometry_points.html, without the Stats panel. */
export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(27, 1, 5, 3500);
    camera.position.z = 2750;

    const scene = new Scene();
    scene.background = Color.fromHex(0x050505);
    scene.fog = new Fog(Color.fromHex(0x050505), 2000, 3500);

    //

    const particles = 500000;

    const geometry = new BufferGeometry();

    const positions = new Float32Array(particles * 3);
    const colors = new Float32Array(particles * 3);

    const color = new Color(1, 1, 1);

    const n = 1000, n2 = n / 2; // particles spread in the cube

    for (let i = 0; i < particles; i++) {
        // positions

        const x = Math.random() * n - n2;
        const y = Math.random() * n - n2;
        const z = Math.random() * n - n2;

        positions.set([x, y, z], i * 3);

        // colors

        const vx = (x / n) + 0.5;
        const vy = (y / n) + 0.5;
        const vz = (z / n) + 0.5;

        color.setRGB(vx, vy, vz, ColorSpace.SRGB);

        colors.set([color.r, color.g, color.b], i * 3);
    }

    geometry.setAttribute('position', new Float32BufferAttribute(positions, 3));
    geometry.setAttribute('color', new Float32BufferAttribute(colors, 3));

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
