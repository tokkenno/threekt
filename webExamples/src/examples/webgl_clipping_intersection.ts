import { Color, Group, Mesh, MeshLambertMaterial, Plane, Side, Vector3 } from 'threekt';
import { SphereGeometry } from 'threekt/geometry';
import { addSliders } from './addSliders';
import { clippingPlaneHelper, clippingScene } from './clippingScene';
import { addToggle, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';

export function createExample(canvas: HTMLCanvasElement) {
    const { scene, camera, controls, renderer } = clippingScene(canvas, false);
    camera.position.set(-1.5, 2.5, 3);
    const planes = [new Plane(new Vector3(1, 0, 0), 0), new Plane(new Vector3(0, -1, 0), 0), new Plane(new Vector3(0, 0, -1), 0)];
    const materials: MeshLambertMaterial[] = [];
    for (let i = 1; i <= 30; i += 2) {
        const material = new MeshLambertMaterial(new Color(1, 1, 1).setHSL(i / 30, 0.5, 0.5));
        material.side = Side.Double;
        material.clippingPlanes = planes;
        material.clipIntersection = true;
        scene.add(new Mesh(new SphereGeometry(i / 30, 48, 24), material));
        materials.push(material);
    }
    const helpers = new Group();
    helpers.visible = false;
    const guides = planes.map((plane, i) => clippingPlaneHelper(plane, 2, Color.fromHex([0xff0000, 0x00ff00, 0x0000ff][i])));
    for (const { mesh } of guides) helpers.add(mesh);
    scene.add(helpers);
    const panel = examplePanel(canvas);
    addToggle(panel, 'Clip intersection', true, value => {
        for (const material of materials) material.clipIntersection = value;
    });
    addToggle(panel, 'Show planes', false, value => { helpers.visible = value; });
    addSliders(canvas, [{ label: 'Plane constant', min: -1, max: 1, step: 0.01, value: 0, change: value => {
        for (const plane of planes) plane.constant = value;
        for (const guide of guides) guide.update();
    } }], panel);
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() { panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
