import {
    AmbientLight, type Camera, Color, DirectionalLight, FogExp2, InstancedMesh, MeshPhongMaterial, Object3D,
    OrthographicCamera, PerspectiveCamera, Scene,
} from 'threekt';
import { ConeGeometry } from 'threekt/geometry';
import { DomInputSurface, TrackballControls } from 'threekt/controls';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { addToggle, examplePanel } from './exampleControls';

/**
 * Port of Three.js examples/misc_controls_trackball.html. Checkboxes replace lil-gui, and the A, S and D
 * keys are not supported.
 */
export function createExample(canvas: HTMLCanvasElement) {
    const params = { orthographicCamera: false, multiTouchRoll: false };
    const frustumSize = 400;
    const perspectiveCamera = new PerspectiveCamera(60, 1, 1, 1000);
    perspectiveCamera.position.z = 500;
    const orthographicCamera = new OrthographicCamera(-frustumSize / 2, frustumSize / 2, frustumSize / 2, -frustumSize / 2, 1, 1000);
    orthographicCamera.position.z = 500;

    const scene = new Scene();
    scene.background = Color.fromHex(0xcccccc);
    scene.fog = new FogExp2(Color.fromHex(0xcccccc), 0.002);
    const geometry = new ConeGeometry(10, 30, 4, 1);
    const material = new MeshPhongMaterial(Color.fromHex(0xffffff));
    material.flatShading = true;
    const mesh = new InstancedMesh(geometry, material, 500);
    const dummy = new Object3D();
    for (let i = 0; i < 500; i++) {
        dummy.position.set((Math.random() - 0.5) * 1000, (Math.random() - 0.5) * 1000, (Math.random() - 0.5) * 1000);
        dummy.updateMatrix();
        mesh.setMatrixAt(i, dummy.matrix);
    }
    scene.add(mesh);

    const dirLight1 = new DirectionalLight(Color.fromHex(0xffffff), 3);
    dirLight1.position.set(1, 1, 1);
    scene.add(dirLight1);
    const dirLight2 = new DirectionalLight(Color.fromHex(0x002288), 3);
    dirLight2.position.set(-1, -1, -1);
    scene.add(dirLight2);
    scene.add(new AmbientLight(Color.fromHex(0x555555)));

    const surface = new DomInputSurface(canvas);
    let controls: TrackballControls;
    function createControls(camera: Camera) {
        controls = new TrackballControls(camera, surface);
        controls.rotateSpeed = 1.0;
        controls.zoomSpeed = 1.2;
        controls.panSpeed = 0.8;
        controls.multiTouchRoll = params.multiTouchRoll;
    }
    createControls(perspectiveCamera);

    const panel = examplePanel(canvas);
    panel.append('Drag to rotate, middle-drag or scroll to zoom, right-drag to pan.');
    addToggle(panel, 'Use orthographic', params.orthographicCamera, value => {
        params.orthographicCamera = value;
        controls.dispose();
        createControls(value ? orthographicCamera : perspectiveCamera);
    });
    addToggle(panel, 'Multi-touch roll', params.multiTouchRoll, value => {
        params.multiTouchRoll = value;
        controls.multiTouchRoll = value;
    });

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            const width = canvas.clientWidth;
            const height = canvas.clientHeight;
            if (width === 0 || height === 0) return;
            const aspect = width / height;
            if (perspectiveCamera.aspect !== aspect) {
                perspectiveCamera.aspect = aspect;
                perspectiveCamera.updateProjectionMatrix();
                orthographicCamera.left = -frustumSize * aspect / 2;
                orthographicCamera.right = frustumSize * aspect / 2;
                orthographicCamera.updateProjectionMatrix();
            }
            renderer.setSize(width, height, window.devicePixelRatio);
            controls.update();
            renderer.render(scene, params.orthographicCamera ? orthographicCamera : perspectiveCamera);
        },
        dispose() {
            panel.remove();
            controls.dispose();
            renderer.dispose();
        },
    };
}
