import {
    BufferGeometry, CapsuleGeometry, Color, Float32BufferAttribute, LineBasicMaterial, LineSegments,
    Mesh, MeshNormalMaterial, PerspectiveCamera, Points, PointsMaterial, Raycaster, Scene, Side,
    Vector3,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { fitPerspective } from './fitPerspective';

export function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(70, 1, 1, 1000);
    camera.position.z = 10;
    const scene = new Scene();
    scene.background = Color.fromHex(0);
    const geometry = new CapsuleGeometry(0.5, 0.5, 4, 32);
    const material = new MeshNormalMaterial();
    material.side = Side.Double;
    const capsules = [-2, 0, 2].map(x => {
        const mesh = new Mesh(geometry, material);
        mesh.position.x = x;
        scene.add(mesh);
        return mesh;
    });
    const raycaster = new Raycaster(new Vector3(-4, 0, 0), new Vector3(1, 0, 0), 1, 8);
    const rayGeometry = new BufferGeometry();
    rayGeometry.setAttribute('position', new Float32BufferAttribute(new Float32Array([
        -4, 0, 0, -3, 0, 0, -3, 0, 0, 4, 0, 0,
    ]), 3));
    rayGeometry.setAttribute('color', new Float32BufferAttribute(new Float32Array([
        0.4, 0.4, 0.4, 0.4, 0.4, 0.4, 1, 0.7, 0, 1, 0.7, 0,
    ]), 3));
    const rayMaterial = new LineBasicMaterial();
    rayMaterial.vertexColors = true;
    rayMaterial.depthTest = false;
    const ray = new LineSegments(rayGeometry, rayMaterial);
    ray.renderOrder = 1;
    scene.add(ray);

    const hitsGeometry = new BufferGeometry();
    const positions = new Float32BufferAttribute(new Float32Array(6 * 3), 3);
    hitsGeometry.setAttribute('position', positions);
    const hitMaterial = new PointsMaterial(Color.fromHex(0x44ff99));
    hitMaterial.size = 8;
    hitMaterial.sizeAttenuation = false;
    hitMaterial.depthTest = false;
    const hits = new Points(hitsGeometry, hitMaterial);
    hits.renderOrder = 2;
    scene.add(hits);

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const angle = seconds * 0.5;
            for (const capsule of capsules) {
                capsule.position.y = Math.sin(angle + capsule.position.x);
                capsule.rotation.z = Math.sin(angle) * Math.PI;
            }
            scene.updateMatrixWorld(true);
            // At shared triangle edges, distinct triangle hits can report the same position.
            const points: Vector3[] = [];
            for (const { point } of raycaster.intersectObjects(capsules, false)) {
                if (!points.some(p => p.distanceToSquared(point) < 1e-10)) points.push(point);
            }
            const count = Math.min(points.length, positions.count);
            for (let i = 0; i < count; i++) positions.setXYZ(i, points[i].x, points[i].y, points[i].z);
            positions.markUpdated();
            hitsGeometry.setDrawRange(0, count);
            renderer.render(scene, camera);
        },
        dispose() { renderer.dispose(); },
    };
}
