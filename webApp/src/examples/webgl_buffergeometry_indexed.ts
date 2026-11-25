import {
    BufferGeometry, Color, ColorSpace, Euler, Float32BufferAttribute, HemisphereLight,
    Mesh, MeshPhongMaterial, PerspectiveCamera, Scene, Side, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { addToggle } from './addToggle';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(27, 1, 1, 3500);
    camera.position.z = 64;
    const scene = new Scene();
    scene.background = Color.fromHex(0x050505);
    const light = new HemisphereLight();
    light.intensity = 3;
    scene.add(light);

    const size = 20;
    const segments = 10;
    const columns = segments + 1;
    const vertices = new Float32Array(columns * columns * 3);
    const normals = new Float32Array(vertices.length);
    const colors = new Float32Array(vertices.length);
    const indices = new Int32Array(segments * segments * 6);
    const color = new Color(1, 1, 1);

    for (let row = 0; row <= segments; row++) {
        const y = row * (size / segments) - size / 2;
        for (let column = 0; column <= segments; column++) {
            const x = column * (size / segments) - size / 2;
            const offset = (row * columns + column) * 3;
            vertices.set([x, -y, 0], offset);
            normals[offset + 2] = 1;
            color.setRGB(x / size + 0.5, y / size + 0.5, 1, ColorSpace.SRGB);
            colors.set([color.r, color.g, color.b], offset);
        }
    }
    for (let row = 0; row < segments; row++) {
        for (let column = 0; column < segments; column++) {
            const b = row * columns + column;
            const a = b + 1, c = b + columns, d = c + 1;
            indices.set([a, b, d, b, c, d], (row * segments + column) * 6);
        }
    }

    const geometry = new BufferGeometry().setIndex(indices)
        .setAttribute('position', new Float32BufferAttribute(vertices, 3))
        .setAttribute('normal', new Float32BufferAttribute(normals, 3))
        .setAttribute('color', new Float32BufferAttribute(colors, 3));
    const material = new MeshPhongMaterial();
    material.side = Side.Double;
    material.vertexColors = true;

    const mesh = new Mesh(geometry, material);
    scene.add(mesh);
    const rotation = new Euler();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const removeToggle = addToggle(canvas, 'Mostrar aristas', false, (value) => { material.wireframe = value; });

    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const time = Date.now() * 0.001;
            mesh.quaternion.setFromEuler(rotation.set(time * 0.25, time * 0.5, 0));
            renderer.render(scene, camera);
        },
        dispose() {
            removeToggle();
            renderer.dispose();
        },
    };
}
