import {
    BufferGeometry, Color, Float32BufferAttribute, Mesh, MeshBasicMaterial, PerspectiveCamera, Points, PointsMaterial,
    Raycaster, Scene, SphereGeometry,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { fitPerspective } from './fitPerspective';
import { raycastPointer } from './raycastPointer';

export async function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    const camera = new PerspectiveCamera(45, 1, 1, 10000);
    const width = 80, length = 160;
    const positions = new Float32Array(width * length * 3);
    const intensity = new Float32Array(width * length);
    for (let i = 0; i < width; i++) {
        for (let j = 0; j < length; j++) {
            const u = i / width, v = j / length, index = i * length + j;
            const y = (Math.cos(u * Math.PI * 4) + Math.sin(v * Math.PI * 8)) / 20;
            positions.set([u - 0.5, y, v - 0.5], index * 3);
            intensity[index] = (y + 0.1) * 5;
        }
    }
    const position = new Float32BufferAttribute(positions, 3);
    const clouds = [[1, 0, 0], [0, 1, 0], [0, 1, 1]].map(([r, g, b], i) => {
        const colors = new Float32Array(positions.length);
        intensity.forEach((value, j) => colors.set([r * value, g * value, b * value], j * 3));
        const geometry = new BufferGeometry().setAttribute('position', position).setAttribute('color', new Float32BufferAttribute(colors, 3));
        if (i > 0) {
            const offset = i === 2 ? 1 : 0;
            const index = new Int32Array(intensity.length + offset);
            index.set(Int32Array.from({ length: intensity.length }, (_, j) => j), offset);
            geometry.setIndexArray(index);
            if (offset) geometry.setDrawRange(offset, intensity.length);
        }
        const material = new PointsMaterial();
        material.size = 0.05;
        material.vertexColors = true;
        const cloud = new Points(geometry, material);
        cloud.scale.set(5, 10, 10);
        cloud.position.x = (i - 1) * 5;
        scene.add(cloud);
        return cloud;
    });
    const shape = new SphereGeometry(0.1, 32, 32);
    const markerMaterial = new MeshBasicMaterial(Color.fromHex(0xff0000));
    const trail = Array.from({ length: 40 }, () => {
        const marker = new Mesh(shape, markerMaterial);
        marker.visible = false;
        scene.add(marker);
        return marker;
    });
    let next = 0, lastHit = 0;
    const input = raycastPointer(canvas);
    const raycaster = new Raycaster();
    raycaster.params.Points.threshold = 0.1;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const angle = seconds * 0.3;
            camera.position.set(10 * (Math.cos(angle) + Math.sin(angle)), 10, 10 * (Math.cos(angle) - Math.sin(angle)));
            camera.lookAt(scene.position);
            raycaster.setFromCamera(input.pointer, camera);
            const hit = raycaster.intersectObjects(clouds, false)[0];
            if (hit && seconds - lastHit > 0.02) {
                const marker = trail[next];
                marker.position.set(hit.point.x, hit.point.y, hit.point.z);
                marker.scale.set(1, 1, 1);
                marker.visible = true;
                next = (next + 1) % trail.length;
                lastHit = seconds;
            }
            trail.forEach(marker => {
                if (!marker.visible) return;
                marker.scale.multiplyScalar(0.98);
                if (marker.scale.x < 0.01) marker.visible = false;
            });
            renderer.render(scene, camera);
        },
        dispose() { input.dispose(); renderer.dispose(); },
    };
}
