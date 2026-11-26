import {
    Box, Color, DirectionalLight, HemisphereLight, InstancedMesh, MeshLambertMaterial, Object3D,
    PerspectiveCamera, Scene, Usage, Vector3, WebGL2Context, WebGLRenderer,
} from 'threekt';
import edge from './assets/edge3.jpg?url';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

export async function createExample(canvas: HTMLCanvasElement) {
    const amount = 100;
    const scene = new Scene();
    scene.background = Color.fromHex(0xadd8e6);
    scene.add(new HemisphereLight(Color.fromHex(0xffffff), Color.fromHex(0x536273), 2));
    const sun = new DirectionalLight(Color.fromHex(0xffffff), 2);
    sun.position.set(4, 8, 3);
    scene.add(sun);

    const camera = new PerspectiveCamera(60, 1, 0.1, 100);
    const target = new Vector3();
    const texture = await loadImageTexture(edge);
    const material = new MeshLambertMaterial(Color.fromHex(0xffffff));
    material.map = texture;
    const mesh = new InstancedMesh(new Box(), material, amount ** 2);
    mesh.instanceMatrix.setUsage(Usage.DynamicDraw);
    scene.add(mesh);
    const dummy = new Object3D();
    dummy.scale.set(1, 2, 1);
    const color = new Color(1, 1, 1);
    const colors = [0x00ffff, 0xffff00, 0xff00ff].map(hex => Color.fromHex(hex));
    const seeds = Float32Array.from({ length: mesh.count }, () => Math.random());
    const baseColors = Array.from(seeds, () => new Color(1, 1, 1).setHSL(1, 0.5 + Math.random() * 0.5, 0.5 + Math.random() * 0.5));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            camera.position.set(Math.sin(seconds / 4) * 10, 8 + Math.cos(seconds / 2) * 2, Math.cos(seconds / 4) * 10);
            camera.lookAt(target.set(-Math.sin(seconds / 4) * 8, 0, -Math.cos(seconds / 2) * 8));
            const cycle = Math.max(0, seconds - 3) / 3;
            const current = Math.floor(cycle);
            const progress = 1 - Math.cos(Math.min(1, (cycle - current) * 1.5) * Math.PI / 2);
            for (let i = 0; i < mesh.count; i++) {
                const x = (amount - 1) / 2 - Math.floor(i / amount);
                const z = (amount - 1) / 2 - i % amount;
                dummy.position.set(x, Math.abs(Math.sin(seconds * 2 + seeds[i] * 3)), z);
                dummy.updateMatrix();
                mesh.setMatrixAt(i, dummy.matrix);
                const index = current + Number(Math.hypot(x, z) / 75 < progress);
                mesh.setColorAt(i, color.copyFrom(baseColors[i]).multiply(colors[index % colors.length]));
            }
            mesh.instanceMatrix.markUpdated();
            mesh.instanceColor?.markUpdated();
            renderer.render(scene, camera);
        },
        dispose() { renderer.dispose(); },
    };
}
