import {
    Color, Mesh, PerspectiveCamera, Scene, SphereGeometry,
} from 'threekt';
import { ShaderMaterial, WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';

const vertex = `void main() { gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }`;
function randomMaterial() {
    const color = Array.from({ length: 3 }, () => Math.random().toFixed(5)).join(', ');
    return new ShaderMaterial(vertex, `
        void main() {
            bool grid = mod(gl_FragCoord.x, 4.0001) < 1.0 || mod(gl_FragCoord.y, 4.0001) < 1.0;
            gl_FragColor = grid ? vec4(${color}, 1.0) : vec4(1.0);
        }
    `);
}

export function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    scene.background = Color.fromHex(0xffffff);
    const camera = new PerspectiveCamera(40, 1, 1, 10000);
    camera.position.z = 2000;
    const geometry = new SphereGeometry(15, 64, 32);
    const meshes = Array.from({ length: 100 }, () => {
        const mesh = new Mesh(geometry, randomMaterial());
        mesh.position.set(...Array.from({ length: 3 }, () => (Math.random() - 0.5) * 1000) as [number, number, number]);
        scene.add(mesh);
        return mesh;
    });
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const panel = examplePanel(canvas);
    const button = document.createElement('button');
    button.textContent = 'Run shader disposal test';
    const result = document.createElement('output');
    panel.append(button, result);
    let dirty = true;
    button.addEventListener('click', () => { dirty = true; });
    return {
        render() {
            if (!dirty || !fitPerspective(canvas, camera, renderer)) return;
            dirty = false;
            for (const mesh of meshes) mesh.material = randomMaterial();
            renderer.render(scene, camera);
            const before = renderer.programCount;
            for (const mesh of meshes) mesh.material.dispose();
            result.textContent = ` Linked programs: ${before} → ${renderer.programCount}`;
        },
        dispose() { panel.remove(); renderer.dispose(); },
    };
}
