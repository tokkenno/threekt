import {
    Color, ColorSpace, Euler, InstancedBufferGeometry, InstancedMesh, InterleavedBuffer, InterleavedBufferAttribute, Matrix4,
    MeshBasicMaterial, PerspectiveCamera, Quaternion, Scene, Vector3,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import crateUrl from './assets/crate.gif?url';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

/** Port of Three.js examples/webgl_buffergeometry_instancing_interleaved.html, without the Stats panel. */
export async function createExample(canvas: HTMLCanvasElement) {
    const instances = 5000;
    const moveQ = new Quaternion(0.5, 0.5, 0.5, 0.0).normalize();
    const tmpQ = new Quaternion();
    const tmpM = new Matrix4();
    const currentM = new Matrix4();

    const camera = new PerspectiveCamera(50, 1, 1, 1000);
    const scene = new Scene();
    scene.background = Color.fromHex(0x101010);

    const geometry = new InstancedBufferGeometry();
    // Per-vertex data x, y, z, w, u, v, s, t for 4-element alignment; only x, y, z and u, v are used.
    const vertexBuffer = InterleavedBuffer.fromFloat32Array(new Float32Array([
        // Front
        -1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, -1, -1, 1, 0, 0, 1, 0, 0, 1, -1, 1, 0, 1, 1, 0, 0,
        // Back
        1, 1, -1, 0, 1, 0, 0, 0, -1, 1, -1, 0, 0, 0, 0, 0, 1, -1, -1, 0, 1, 1, 0, 0, -1, -1, -1, 0, 0, 1, 0, 0,
        // Left
        -1, 1, -1, 0, 1, 1, 0, 0, -1, 1, 1, 0, 1, 0, 0, 0, -1, -1, -1, 0, 0, 1, 0, 0, -1, -1, 1, 0, 0, 0, 0, 0,
        // Right
        1, 1, 1, 0, 1, 0, 0, 0, 1, 1, -1, 0, 1, 1, 0, 0, 1, -1, 1, 0, 0, 0, 0, 0, 1, -1, -1, 0, 0, 1, 0, 0,
        // Top
        -1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, -1, 1, -1, 0, 0, 1, 0, 0, 1, 1, -1, 0, 1, 1, 0, 0,
        // Bottom
        1, -1, 1, 0, 1, 0, 0, 0, -1, -1, 1, 0, 0, 0, 0, 0, 1, -1, -1, 0, 1, 1, 0, 0, -1, -1, -1, 0, 0, 1, 0, 0,
    ]), 8);
    geometry.setAttribute('position', new InterleavedBufferAttribute(vertexBuffer, 3, 0));
    geometry.setAttribute('uv', new InterleavedBufferAttribute(vertexBuffer, 2, 4));
    geometry.setIndexArray(new Int32Array([
        0, 2, 1, 2, 3, 1, 4, 6, 5, 6, 7, 5, 8, 10, 9, 10, 11, 9,
        12, 14, 13, 14, 15, 13, 16, 17, 18, 18, 17, 19, 20, 21, 22, 22, 21, 23,
    ]));

    const material = new MeshBasicMaterial();
    material.map = await loadImageTexture(crateUrl);
    material.map.colorSpace = ColorSpace.SRGB;
    material.map.flipY = false;

    const matrix = new Matrix4();
    const offset = new Vector3();
    const orientation = new Quaternion();
    const scale = new Vector3(1, 1, 1);
    const mesh = new InstancedMesh(geometry, material, instances);
    for (let i = 0; i < instances; i++) {
        let x = Math.random() * 100 - 50;
        let y = Math.random() * 100 - 50;
        let z = Math.random() * 100 - 50;
        // Move out at least 5 units from the center in the current direction.
        offset.set(x, y, z).normalize().multiplyScalar(5);
        offset.set(x + offset.x, y + offset.y, z + offset.z);
        x = Math.random() * 2 - 1;
        y = Math.random() * 2 - 1;
        z = Math.random() * 2 - 1;
        const w = Math.random() * 2 - 1;
        orientation.set(x, y, z, w).normalize();
        matrix.compose(offset, orientation, scale);
        mesh.setMatrixAt(i, matrix);
    }
    scene.add(mesh);
    const rotation = new Euler();

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    let lastTime = 0;
    return {
        render(seconds: number) {
            const time = seconds * 1000;
            mesh.quaternion.setFromEuler(rotation.set(0, time * 0.00005, 0));
            const delta = (time - lastTime) / 5000;
            tmpQ.set(moveQ.x * delta, moveQ.y * delta, moveQ.z * delta, 1).normalize();
            tmpM.makeRotationFromQuaternion(tmpQ);
            for (let i = 0; i < instances; i++) {
                mesh.getMatrixAt(i, currentM);
                currentM.multiply(tmpM);
                mesh.setMatrixAt(i, currentM);
            }
            mesh.instanceMatrix.markUpdated();
            mesh.computeBoundingSphere();
            lastTime = time;
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() {
            renderer.dispose();
        },
    };
}
