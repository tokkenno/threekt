import { Euler, PerspectiveCamera, Points, PointsMaterial, Scene, Vector3, WebGL2Context, WebGLRenderer, XYZLoader } from 'threekt';
import helixUrl from './assets/helix_201.xyz?url';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const response = await fetch(helixUrl);
    if (!response.ok) throw new Error(`Could not load the XYZ point cloud (HTTP ${response.status}).`);
    const geometry = new XYZLoader().parse(await response.text());
    geometry.computeBoundingBox();
    const center = geometry.boundingBox!.getCenter(new Vector3());
    const material = new PointsMaterial();
    material.size = 0.1;
    material.vertexColors = geometry.hasAttribute('color');
    const points = new Points(geometry, material);
    const scene = new Scene();
    scene.add(points);
    const camera = new PerspectiveCamera(50, 1, 0.1, 100);
    camera.position.set(10, 7, 10);
    camera.lookAt(scene.position);
    const rotation = new Euler();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            points.quaternion.setFromEuler(rotation.set(seconds * 0.2, seconds * 0.5, 0));
            points.position.set(-center.x, -center.y, -center.z).applyQuaternion(points.quaternion);
            renderer.render(scene, camera);
        },
        dispose() { renderer.dispose(); },
    };
}
