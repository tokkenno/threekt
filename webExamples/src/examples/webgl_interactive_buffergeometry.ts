import {
    AmbientLight, BufferGeometry, Color, DirectionalLight, Euler, Float32BufferAttribute, Fog, Line,
    LineBasicMaterial, Mesh, MeshPhongMaterial, PerspectiveCamera, Raycaster, Scene, Side, Usage, Vector3, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { fitPerspective } from './fitPerspective';
import { raycastPointer } from './raycastPointer';

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(27, 1, 1, 3500);
    camera.position.z = 2750;
    const scene = new Scene();
    scene.background = Color.fromHex(0x050505);
    scene.fog = new Fog(Color.fromHex(0x050505), 2000, 3500);
    scene.add(new AmbientLight(Color.fromHex(0x444444), 3));
    const light1 = new DirectionalLight(Color.fromHex(0xffffff), 1.5);
    light1.position.set(1, 1, 1);
    scene.add(light1);
    const light2 = new DirectionalLight(Color.fromHex(0xffffff), 4.5);
    light2.position.set(0, -1, 0);
    scene.add(light2);
    const count = 5000;
    const positions = new Float32Array(count * 9), colors = new Float32Array(positions.length), normals = new Float32Array(positions.length);
    const points = [new Vector3(), new Vector3(), new Vector3()];
    const edge = new Vector3(), normal = new Vector3();
    const color = new Color(1, 1, 1);
    const spread = (size: number) => size * (Math.random() - 0.5);
    for (let i = 0; i < count; i++) {
        const x = spread(800), y = spread(800), z = spread(800);
        points.forEach(p => p.set(x + spread(120), y + spread(120), z + spread(120)));
        normal.subVectors(points[2], points[1]).cross(edge.subVectors(points[0], points[1])).normalize();
        color.setRGB(x / 800 + 0.5, y / 800 + 0.5, z / 800 + 0.5);
        points.forEach((p, j) => {
            const offset = i * 9 + j * 3;
            positions.set([p.x, p.y, p.z], offset);
            normals.set([normal.x, normal.y, normal.z], offset);
            colors.set([color.r, color.g, color.b], offset);
        });
    }
    const geometry = new BufferGeometry()
        .setAttribute('position', new Float32BufferAttribute(positions, 3))
        .setAttribute('normal', new Float32BufferAttribute(normals, 3))
        .setAttribute('color', new Float32BufferAttribute(colors, 3));
    const material = new MeshPhongMaterial();
    material.vertexColors = true;
    material.side = Side.Double;
    const mesh = new Mesh(geometry, material);
    scene.add(mesh);

    const outline = new Float32Array(12);
    const border = new Float32BufferAttribute(outline, 3).setUsage(Usage.DynamicDraw);
    const borderMaterial = new LineBasicMaterial();
    borderMaterial.transparent = true;
    const line = new Line(new BufferGeometry().setAttribute('position', border), borderMaterial);
    mesh.add(line);
    const input = raycastPointer(canvas);
    const raycaster = new Raycaster();
    const rotation = new Euler();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            mesh.quaternion.setFromEuler(rotation.set(seconds * 0.25, seconds * 0.5, 0));
            raycaster.setFromCamera(input.pointer, camera);
            const hit = raycaster.intersectObject(mesh, false)[0];
            line.visible = !!hit;
            if (hit) {
                const face = hit.face!;
                [face.a, face.b, face.c, face.a].forEach((i, j) => outline.set(positions.subarray(i * 3, i * 3 + 3), j * 3));
                border.markUpdated();
            }
            renderer.render(scene, camera);
        },
        dispose() { input.dispose(); renderer.dispose(); },
    };
}
