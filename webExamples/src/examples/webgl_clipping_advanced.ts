import { BoxGeometry, PlaneGeometry } from 'threekt/geometry';
import {
    Color, Group, InstancedMesh, Matrix4, Mesh, MeshBasicMaterial, MeshPhongMaterial, Object3D, Plane, Side,
    Vector3,
} from 'threekt';
import { clippingScene } from './clippingScene';
import { addToggle, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';

export function createExample(canvas: HTMLCanvasElement) {
    const { scene, camera, controls, renderer } = clippingScene(canvas, true);
    const vertices = [
        new Vector3(1, 0, Math.SQRT1_2), new Vector3(-1, 0, Math.SQRT1_2),
        new Vector3(0, 1, -Math.SQRT1_2), new Vector3(0, -1, -Math.SQRT1_2),
    ];
    const originalPlanes = [[0, 1, 2], [0, 2, 3], [0, 3, 1], [1, 3, 2]].map(([a, b, c]) =>
        new Plane().setFromCoplanarPoints(vertices[a], vertices[b], vertices[c]));
    const planes = originalPlanes.map(plane => plane.copy());
    const material = new MeshPhongMaterial(Color.fromHex(0xee0a10));
    material.clippingPlanes = planes;
    const lattice = new InstancedMesh(new BoxGeometry(0.18, 0.18, 0.18), material, 125);
    const dummy = new Object3D();
    let index = 0;
    for (let z = -2; z <= 2; z++) for (let y = -2; y <= 2; y++) for (let x = -2; x <= 2; x++) {
        dummy.position.set(x / 5, y / 5, z / 5);
        dummy.updateMatrix();
        lattice.setMatrixAt(index++, dummy.matrix);
    }
    scene.add(lattice);

    const originalGlobal = Array.from({ length: 5 }, (_, i) => {
        const angle = i * 2 * Math.PI / 5;
        return new Plane(new Vector3(Math.cos(angle), 0, Math.sin(angle)), 2.5);
    });
    const global = originalGlobal.map(plane => plane.copy());
    const visualization = new Group();
    visualization.visible = false;
    scene.add(visualization);
    const faceGeometry = new PlaneGeometry(3, 3);
    const zAxis = new Vector3(0, 0, 1);
    const faces = planes.map((_, i) => {
        const faceMaterial = new MeshBasicMaterial(new Color(1, 1, 1).setHSL(i / planes.length, 0.5, 0.5));
        faceMaterial.side = Side.Double;
        faceMaterial.transparent = true;
        faceMaterial.opacity = 0.25;
        faceMaterial.depthWrite = false;
        faceMaterial.clippingPlanes = planes.filter((__, index) => index !== i);
        const face = new Mesh(faceGeometry, faceMaterial);
        visualization.add(face);
        return face;
    });
    const transform = new Matrix4();
    const size = new Vector3();
    const panel = examplePanel(canvas);
    addToggle(panel, 'Local clipping', true, value => {
        renderer.localClippingEnabled = value;
        visualization.visible = value && showVolume;
    });
    let showVolume = false;
    addToggle(panel, 'Show volume', false, value => {
        showVolume = value;
        visualization.visible = value && renderer.localClippingEnabled;
    });
    addToggle(panel, 'Global clipping', false, value => { renderer.clippingPlanes = value ? global : []; });
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            lattice.position.y = 1;
            lattice.rotation.set(seconds * 0.5, seconds * 0.2, 0);
            lattice.updateMatrix();
            transform.copyFrom(lattice.matrix).scale(size.setScalar(Math.cos(seconds * 0.5) * 0.5 + 0.7));
            for (let i = 0; i < planes.length; i++) {
                const plane = planes[i].copyFrom(originalPlanes[i]).applyMatrix4(transform);
                plane.coplanarPoint(faces[i].position);
                faces[i].quaternion.setFromUnitVectors(zAxis, plane.normal);
            }
            transform.makeRotationY(seconds * 0.1);
            for (let i = 0; i < global.length; i++) global[i].copyFrom(originalGlobal[i]).applyMatrix4(transform);
            renderer.render(scene, camera);
        },
        dispose() { panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
