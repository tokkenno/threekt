import {
    Color, CylinderGeometry, Euler, Group, Matrix4, Mesh, MeshBasicMaterial, PerspectiveCamera, Quaternion,
    Scene, ShaderMaterial, SphereGeometry, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { addToggle } from './addToggle';
import { fitPerspective } from './fitPerspective';

const vertexShader = `
    varying vec3 vNormal;
    void main() {
        vNormal = normalize(normalMatrix * normal);
        gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
    }
`;
const fragmentShader = `
    varying vec3 vNormal;
    void main() {
        gl_FragColor = vec4(normalize(vNormal) * 0.5 + 0.5, 1.0);
    }
`;

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(70, 1, 0.01, 10);
    camera.position.z = 5;
    const scene = new Scene();
    const rig = new Group();
    const cone = new Mesh(new CylinderGeometry(0, 0.1, 0.5, 8), new ShaderMaterial(vertexShader, fragmentShader));
    cone.quaternion.setFromEuler(new Euler(Math.PI / 2, 0, 0));
    rig.add(cone);
    scene.add(rig);
    const target = new Mesh(new SphereGeometry(0.05), new MeshBasicMaterial(Color.fromHex(0xff0000)));
    scene.add(target);
    const wireframe = new MeshBasicMaterial(Color.fromHex(0xcccccc));
    wireframe.wireframe = wireframe.transparent = true;
    wireframe.opacity = 0.3;
    scene.add(new Mesh(new SphereGeometry(2, 32, 32), wireframe));

    const matrix = new Matrix4();
    const desired = new Quaternion();
    let interval = -1, previous = 0, lookAt = false;
    const remove = addToggle(canvas, 'Orientación instantánea (lookAt)', false, value => { lookAt = value; });
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const next = Math.floor(seconds / 2);
            if (next !== interval) {
                interval = next;
                target.position.setFromSphericalCoords(2, Math.acos(2 * Math.random() - 1), Math.random() * Math.PI * 2);
                desired.setFromRotationMatrix(matrix.lookAt(target.position, rig.position, rig.up));
            }
            if (lookAt) rig.lookAt(target.position);
            else rig.quaternion.rotateTowards(desired, (seconds - previous) * Math.PI / 2);
            previous = seconds;
            renderer.render(scene, camera);
        },
        dispose() { remove(); renderer.dispose(); },
    };
}
