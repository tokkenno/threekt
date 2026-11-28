import { Color, Mesh, MeshBasicMaterial, OrthographicCamera, PerspectiveCamera, Scene, Side } from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { PlaneGeometry } from 'threekt/geometry';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { addSliders } from './addSliders';
import { addToggle, examplePanel } from './exampleControls';
import { cssPanel, exampleCss3d } from './exampleCss3d';

export function createCssPlanesExample(canvas: HTMLCanvasElement, orthographic: boolean) {
    const camera = orthographic ? new OrthographicCamera(-150, 150, 150, -150, 1, 1000)
        : new PerspectiveCamera(45, 1, 1, 1000);
    camera.position.set(orthographic ? -200 : 200, 200, 200);
    const scene = new Scene(), wireScene = new Scene();
    const css = exampleCss3d(canvas, '#f0f0f0', true);
    const clear = new Color(0, 0, 0);
    const wire = new MeshBasicMaterial(Color.fromHex(0x121212));
    wire.wireframe = true;
    wire.side = Side.Double;

    const addPlane = (width: number, height: number, color: string, x: number, y: number, z: number, rx: number, ry: number, rz = 0) => {
        const node = css.add(cssPanel(width, height, color));
        node.position.set(x, y, z);
        node.rotation.set(rx, ry, rz);
        scene.add(node);
        const mesh = new Mesh(new PlaneGeometry(width, height), wire);
        mesh.position.copyFrom(node.position);
        mesh.rotation.copyFrom(node.rotation);
        wireScene.add(mesh);
    };
    if (orthographic) {
        addPlane(100, 100, 'chocolate', -50, 0, 0, 0, -Math.PI / 2);
        addPlane(100, 100, 'saddlebrown', 0, 0, 50, 0, 0);
        addPlane(100, 100, 'yellowgreen', 0, 50, 0, -Math.PI / 2, 0);
        addPlane(300, 300, 'seagreen', 0, -50, 0, -Math.PI / 2, 0);
    } else {
        for (let i = 0; i < 10; i++) {
            addPlane(100, 100, `hsl(${Math.random() * 360} 65% 55%)`, Math.random() * 200 - 100,
                Math.random() * 200 - 100, Math.random() * 200 - 100, Math.random() * Math.PI,
                Math.random() * Math.PI, Math.random() * Math.PI);
        }
    }

    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const panel = examplePanel(canvas);
    let showCss = true, showWire = true, offset = false, offsetX = 0, offsetY = 0;
    const oldStyle = canvas.style.cssText;
    Object.assign(canvas.style, { position: 'relative', background: 'transparent' });
    addToggle(panel, 'CSS panels', true, value => { showCss = value; });
    addToggle(panel, 'WebGL wireframes', true, value => { showWire = value; });
    addToggle(panel, 'View offset', false, value => { offset = value; });
    addSliders(canvas, [
        { label: 'Offset X', min: -150, max: 150, step: 1, value: 0, change: value => { offsetX = value; } },
        { label: 'Offset Y', min: -150, max: 150, step: 1, value: 0, change: value => { offsetY = value; } },
    ], panel);
    return {
        render() {
            const { clientWidth: width, clientHeight: height } = canvas;
            if (!width || !height) return;
            if (camera instanceof PerspectiveCamera) camera.aspect = width / height;
            else {
                camera.right = 150 * width / height;
                camera.left = -camera.right;
            }
            if (offset) camera.setViewOffset(width, height, offsetX, offsetY, width, height);
            else camera.clearViewOffset();
            camera.updateProjectionMatrix();
            controls.update();
            renderer.setSize(width, height, window.devicePixelRatio);
            renderer.setClearColor(clear.setHex(showCss ? 0 : 0xf0f0f0), showCss ? 0 : 1);
            if (showWire) renderer.render(wireScene, camera);
            else renderer.clear();
            css.layer.hidden = !showCss;
            if (showCss) css.render(scene, camera);
        },
        dispose() { css.dispose(); panel.remove(); controls.dispose(); renderer.dispose(); canvas.style.cssText = oldStyle; },
    };
}
