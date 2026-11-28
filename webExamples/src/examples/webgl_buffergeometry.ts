import {
    AmbientLight, BufferGeometry, Color, DirectionalLight, Euler, Float32BufferAttribute, Fog, Mesh, MeshPhongMaterial,
    PerspectiveCamera, Scene, Side, Vector3,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { fitPerspective } from './fitPerspective';

/**
 * Port of Three.js examples/webgl_buffergeometry.html, without the Stats panel. Threekt keeps attribute
 * arrays after upload, so the upstream `onUpload( disposeArray )` calls are omitted.
 */
export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(27, 1, 1, 3500);
    camera.position.z = 2750;

    const scene = new Scene();
    scene.background = Color.fromHex(0x050505);
    scene.fog = new Fog(Color.fromHex(0x050505), 2000, 3500);

    //

    scene.add(new AmbientLight(Color.fromHex(0xcccccc)));

    const light1 = new DirectionalLight(Color.fromHex(0xffffff), 1.5);
    light1.position.set(1, 1, 1);
    scene.add(light1);

    const light2 = new DirectionalLight(Color.fromHex(0xffffff), 4.5);
    light2.position.set(0, -1, 0);
    scene.add(light2);

    //

    const triangles = 160000;

    const geometry = new BufferGeometry();

    const positions = new Float32Array(triangles * 3 * 3);
    const normals = new Float32Array(triangles * 3 * 3);
    const colors = new Float32Array(triangles * 3 * 4);

    const color = new Color(1, 1, 1);

    const n = 800, n2 = n / 2; // triangles spread in the cube
    const d = 12, d2 = d / 2; // individual triangle size

    const pA = new Vector3();
    const pB = new Vector3();
    const pC = new Vector3();

    const cb = new Vector3();
    const ab = new Vector3();

    for (let i = 0; i < triangles; i++) {
        // positions

        const x = Math.random() * n - n2;
        const y = Math.random() * n - n2;
        const z = Math.random() * n - n2;

        const ax = x + Math.random() * d - d2;
        const ay = y + Math.random() * d - d2;
        const az = z + Math.random() * d - d2;

        const bx = x + Math.random() * d - d2;
        const by = y + Math.random() * d - d2;
        const bz = z + Math.random() * d - d2;

        const cx = x + Math.random() * d - d2;
        const cy = y + Math.random() * d - d2;
        const cz = z + Math.random() * d - d2;

        positions.set([ax, ay, az, bx, by, bz, cx, cy, cz], i * 9);

        // flat face normals

        pA.set(ax, ay, az);
        pB.set(bx, by, bz);
        pC.set(cx, cy, cz);

        cb.subVectors(pC, pB);
        ab.subVectors(pA, pB);
        cb.cross(ab);

        cb.normalize();

        const nx = cb.x;
        const ny = cb.y;
        const nz = cb.z;

        normals.set([nx, ny, nz, nx, ny, nz, nx, ny, nz], i * 9);

        // colors

        const vx = (x / n) + 0.5;
        const vy = (y / n) + 0.5;
        const vz = (z / n) + 0.5;

        color.setRGB(vx, vy, vz);

        const alpha = Math.random();

        colors.set([
            color.r, color.g, color.b, alpha,
            color.r, color.g, color.b, alpha,
            color.r, color.g, color.b, alpha,
        ], i * 12);
    }

    geometry.setAttribute('position', new Float32BufferAttribute(positions, 3));
    geometry.setAttribute('normal', new Float32BufferAttribute(normals, 3));
    geometry.setAttribute('color', new Float32BufferAttribute(colors, 4));

    geometry.computeBoundingSphere();

    const material = new MeshPhongMaterial(Color.fromHex(0xd5d5d5));
    material.specular.setHex(0xffffff);
    material.shininess = 250;
    material.side = Side.Double;
    material.vertexColors = true;
    material.transparent = true;

    const mesh = new Mesh(geometry, material);
    scene.add(mesh);
    const rotation = new Euler();

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const time = Date.now() * 0.001;

            mesh.quaternion.setFromEuler(rotation.set(time * 0.25, time * 0.5, 0));

            renderer.render(scene, camera);
        },
        dispose() {
            renderer.dispose();
        },
    };
}
