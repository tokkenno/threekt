import {
    BufferGeometry, Euler, Float32BufferAttribute, Line, LineBasicMaterial, PerspectiveCamera, Scene, WebGL2Context,
    WebGLRenderer,
} from 'threekt';
import { fitPerspective } from './fitPerspective';

const segments = 10000;
const r = 800;

/**
 * Port of Three.js examples/webgl_buffergeometry_lines.html, without the Stats panel. The gallery passes
 * the elapsed time, which replaces the upstream Timer.
 */
export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(27, 1, 1, 4000);
    camera.position.z = 2750;

    const scene = new Scene();

    const geometry = new BufferGeometry();
    const material = new LineBasicMaterial();
    material.vertexColors = true;

    const positions = new Float32Array(segments * 3);
    const colors = new Float32Array(segments * 3);

    for (let i = 0; i < segments; i++) {
        const x = Math.random() * r - r / 2;
        const y = Math.random() * r - r / 2;
        const z = Math.random() * r - r / 2;

        // positions
        positions.set([x, y, z], i * 3);

        // colors
        colors.set([(x / r) + 0.5, (y / r) + 0.5, (z / r) + 0.5], i * 3);
    }

    geometry.setAttribute('position', new Float32BufferAttribute(positions, 3));
    geometry.setAttribute('color', new Float32BufferAttribute(colors, 3));
    generateMorphTargets(geometry);
    geometry.computeBoundingSphere();

    const line = new Line(geometry, material);
    scene.add(line);
    const rotation = new Euler();

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    let previous = 0;
    let t = 0;
    return {
        render(time: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const delta = time - previous;
            previous = time;

            line.quaternion.setFromEuler(rotation.set(time * 0.25, time * 0.5, 0));

            t += delta * 0.5;
            line.morphTargetInfluences![0] = Math.abs(Math.sin(t));

            renderer.render(scene, camera);
        },
        dispose() {
            renderer.dispose();
        },
    };
}

function generateMorphTargets(geometry: BufferGeometry) {
    const data = new Float32Array(segments * 3);

    for (let i = 0; i < segments; i++) {
        const x = Math.random() * r - r / 2;
        const y = Math.random() * r - r / 2;
        const z = Math.random() * r - r / 2;

        data.set([x, y, z], i * 3);
    }

    const morphTarget = new Float32BufferAttribute(data, 3);
    morphTarget.name = 'target1';
    geometry.setMorphAttribute('position', [morphTarget]);
}
