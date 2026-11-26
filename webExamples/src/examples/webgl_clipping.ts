import { Color, Mesh, MeshPhongMaterial, Plane, Side, TorusKnotGeometry, Vector3 } from 'threekt';
import { addSliders } from './addSliders';
import { clippingScene } from './clippingScene';
import { addToggle, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';

export function createExample(canvas: HTMLCanvasElement) {
    const { scene, camera, controls, renderer } = clippingScene(canvas, true);
    const localPlane = new Plane(new Vector3(0, -1, 0), 0.8);
    const globalPlane = new Plane(new Vector3(-1, 0, 0), 0.1);
    const material = new MeshPhongMaterial(Color.fromHex(0x80ee10));
    material.shininess = 100;
    material.side = Side.Double;
    material.clippingPlanes = [localPlane];
    const knot = new Mesh(new TorusKnotGeometry(0.4, 0.08, 95, 20), material);
    scene.add(knot);

    const panel = examplePanel(canvas);
    addToggle(panel, 'Local clipping', true, value => { renderer.localClippingEnabled = value; });
    addToggle(panel, 'Global clipping', false, value => { renderer.clippingPlanes = value ? [globalPlane] : []; });
    addSliders(canvas, [
        { label: 'Local plane', min: 0, max: 2, step: 0.01, value: 0.8, change: value => { localPlane.constant = value; } },
        { label: 'Global plane', min: -1, max: 1, step: 0.01, value: 0.1, change: value => { globalPlane.constant = value; } },
    ], panel);
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            knot.position.y = 0.8;
            knot.rotation.set(seconds * 0.5, seconds * 0.2, 0);
            knot.scale.setScalar(1.1 + 0.3 * Math.cos(seconds));
            renderer.render(scene, camera);
        },
        dispose() { panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
