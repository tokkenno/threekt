import {
    BufferGeometry, Color, ColorSpace, Euler, Float32BufferAttribute, HemisphereLight, Mesh,
    MeshPhongMaterial, PerspectiveCamera, Scene, Side, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { addToggle, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';

export function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(27, 1, 1, 3500);
    camera.position.z = 64;
    const scene = new Scene();
    scene.background = Color.fromHex(0x050505);
    scene.add(new HemisphereLight());

    const indices: number[] = [];
    const positions: number[] = [];
    const normals: number[] = [];
    const colors: number[] = [];
    const color = Color.fromHex(0xffffff);
    const size = 20;
    const segments = 10;
    for (let i = 0; i <= segments; i++) {
        const y = (i / segments - 0.5) * size;
        for (let j = 0; j <= segments; j++) {
            const x = (j / segments - 0.5) * size;
            positions.push(x, -y, 0);
            normals.push(0, 0, 1);
            color.setRGB(x / size + 0.5, y / size + 0.5, 1, ColorSpace.SRGB);
            colors.push(color.r, color.g, color.b);
        }
    }
    for (let i = 0; i < segments; i++) {
        for (let j = 0; j < segments; j++) {
            const b = i * (segments + 1) + j;
            const a = b + 1;
            const c = b + segments + 1;
            const d = c + 1;
            indices.push(a, b, d, b, c, d);
        }
    }
    const geometry = new BufferGeometry()
        .setIndex(Int32Array.from(indices))
        .setAttribute('position', new Float32BufferAttribute(Float32Array.from(positions), 3))
        .setAttribute('normal', new Float32BufferAttribute(Float32Array.from(normals), 3))
        .setAttribute('color', new Float32BufferAttribute(Float32Array.from(colors), 3));
    const material = new MeshPhongMaterial();
    material.vertexColors = true;
    material.side = Side.Double;
    const mesh = new Mesh(geometry, material);
    scene.add(mesh);

    const rotation = new Euler();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const panel = examplePanel(canvas);
    addToggle(panel, 'Malla de alambre', false, (value) => { material.wireframe = value; });
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            mesh.quaternion.setFromEuler(rotation.set(seconds * 0.25, seconds * 0.5, 0));
            renderer.render(scene, camera);
        },
        dispose() {
            panel.remove();
            renderer.dispose();
        },
    };
}
