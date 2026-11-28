import {
    Color, Fog, Group, IcosahedronGeometry, Mesh, MeshBasicMaterial, PerspectiveCamera, Scene,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { fitPerspective } from './fitPerspective';
import { flyCamera } from './flyCamera';

export function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    scene.background = Color.fromHex(0);
    scene.fog = new Fog(Color.fromHex(0), 1, 15000);
    const camera = new PerspectiveCamera(45, 1, 1, 15000);
    camera.position.z = 1400;
    const controls = flyCamera(canvas, camera, 1000, true);
    const thresholds = [0, 300, 1000, 2000, 8000];
    const geometries = [16, 8, 4, 2, 1].map(detail => new IcosahedronGeometry(100, detail));
    const material = new MeshBasicMaterial(Color.fromHex(0xffffff));
    material.wireframe = true;
    const objects = Array.from({ length: 300 }, (_, index) => {
        const group = new Group();
        if (index === 0) group.position.z = 500;
        else group.position.set(10000 * (0.5 - Math.random()), 7500 * (0.5 - Math.random()),
            10000 * (0.5 - Math.random()));
        const meshes = geometries.map(geometry => {
            const mesh = new Mesh(geometry, material);
            mesh.scale.set(1.5, 1.5, 1.5);
            group.add(mesh);
            return mesh;
        });
        scene.add(group);
        return { group, meshes, active: -1 };
    });

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            controls.update(seconds);
            if (!fitPerspective(canvas, camera, renderer)) return;
            for (const object of objects) {
                const distance = object.group.position.distanceTo(camera.position) / camera.zoom;
                let level = thresholds.length - 1;
                while (level > 0 && distance < thresholds[level]) level--;
                if (level !== object.active) {
                    object.meshes.forEach((mesh, index) => { mesh.visible = index === level; });
                    object.active = level;
                }
            }
            renderer.render(scene, camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); },
    };
}
