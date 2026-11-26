import {
    AxesHelper, Box, DiscreteInterpolant, LinearInterpolant, Mesh, MeshBasicMaterial, PerspectiveCamera,
    Quaternion, QuaternionLinearInterpolant, Scene, Vector3, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { fitPerspective } from './fitPerspective';

export function createKeyframeExample(canvas: HTMLCanvasElement, grouped: boolean) {
    const camera = new PerspectiveCamera(40, 1, 1, 1000);
    camera.position.set(grouped ? 50 : 25, grouped ? 50 : 25, grouped ? 100 : 50);
    camera.lookAt(new Vector3());
    const scene = new Scene();
    const geometry = new Box(5, 5, 5);
    const material = new MeshBasicMaterial();
    material.transparent = true;
    const items = Array.from({ length: grouped ? 25 : 1 }, (_, i) => {
        const mesh = new Mesh(geometry, material);
        if (grouped) mesh.position.set(32 - 16 * Math.floor(i / 5), 0, 32 - 16 * (i % 5));
        scene.add(mesh);
        return mesh;
    });
    if (!grouped) scene.add(new AxesHelper(10));

    const times = new Float64Array([0, 1, 2]);
    const axis = new Vector3(1, 0, 0);
    const end = new Quaternion().setFromAxisAngle(axis, Math.PI);
    const rotation = new QuaternionLinearInterpolant(times, new Float64Array([0, 0, 0, 1, end.x, end.y, end.z, end.w, 0, 0, 0, 1]), 4);
    const color = new DiscreteInterpolant(times, new Float64Array([1, 0, 0, 0, 1, 0, 0, 0, 1]), 3);
    const opacity = new LinearInterpolant(times, new Float64Array([1, 0, 1]), 1);
    const position = new LinearInterpolant(times, new Float64Array([0, 0, 0, 30, 0, 0, 0, 0, 0]), 3);
    const scale = new LinearInterpolant(times, new Float64Array([1, 1, 1, 2, 2, 2, 1, 1, 1]), 3);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const time = seconds % 3;
            const [x, y, z, w] = rotation.evaluate(time);
            const [r, g, b] = color.evaluate(time);
            for (const mesh of items) mesh.quaternion.set(x, y, z, w);
            material.color.set(r, g, b);
            material.opacity = opacity.evaluate(time)[0];
            if (!grouped) {
                const [x, y, z] = position.evaluate(time);
                const [sx, sy, sz] = scale.evaluate(time);
                items[0].position.set(x, y, z);
                items[0].scale.set(sx, sy, sz);
            }
            renderer.render(scene, camera);
        },
        dispose() { renderer.dispose(); },
    };
}
