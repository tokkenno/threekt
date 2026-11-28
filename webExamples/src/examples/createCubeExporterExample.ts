import { BoxGeometry, PlaneGeometry } from 'threekt/geometry';
import {
    Color, DirectionalLight, Euler, Fog, HemisphereLight, Mesh, MeshPhongMaterial, PerspectiveCamera, Scene,
    Uint8BufferAttribute,
} from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { GridHelper } from 'threekt/helpers';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { downloadGeometry } from './downloadGeometry';
import { fitPerspective } from './fitPerspective';

type ExportFormat = {
    label: string;
    file: string;
    serialize: (mesh: Mesh) => string | Int8Array;
};

export function createCubeExporterExample(canvas: HTMLCanvasElement, vertexColors: boolean, formats: ExportFormat[]) {
    const camera = new PerspectiveCamera(45, 1, 0.1, 100);
    camera.position.set(4, 2, 4);
    const scene = new Scene();
    scene.background = Color.fromHex(0xa0a0a0);
    scene.fog = new Fog(Color.fromHex(0xa0a0a0), 4, 20);
    scene.add(new HemisphereLight(Color.fromHex(0xffffff), Color.fromHex(0x444444), 3));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 3);
    light.position.set(0, 20, 10);
    scene.add(light);

    const floorMaterial = new MeshPhongMaterial(Color.fromHex(vertexColors ? 0xcbcbcb : 0xbbbbbb));
    floorMaterial.depthWrite = false;
    const ground = new Mesh(new PlaneGeometry(40, 40), floorMaterial);
    ground.quaternion.setFromEuler(new Euler(-Math.PI / 2, 0, 0));
    scene.add(ground);
    const grid = new GridHelper(40, 20, Color.fromHex(0), Color.fromHex(0));
    grid.material.opacity = 0.2;
    grid.material.transparent = true;
    scene.add(grid);

    const geometry = new BoxGeometry();
    const material = new MeshPhongMaterial(Color.fromHex(vertexColors ? 0xffffff : 0x00ff00));
    material.vertexColors = vertexColors;
    if (vertexColors) {
        const positions = geometry.getAttribute('position')!;
        const colors = new Int8Array(positions.count * 3);
        for (let i = 0; i < positions.count; i++) {
            colors[i * 3] = positions.getX(i) > 0 ? 128 : 0;
            colors[i * 3 + 1] = positions.getY(i) > 0 ? 128 : 0;
            colors[i * 3 + 2] = positions.getZ(i) > 0 ? 128 : 0;
        }
        geometry.setAttribute('color', new Uint8BufferAttribute(colors, 3, true));
    }
    const mesh = new Mesh(geometry, material);
    mesh.position.y = 0.5;
    scene.add(mesh);

    const panel = document.createElement('aside');
    panel.className = 'example-panel';
    for (const format of formats) {
        const button = document.createElement('button');
        button.textContent = format.label;
        button.addEventListener('click', () => downloadGeometry(format.serialize(mesh), format.file));
        panel.append(button);
    }
    canvas.parentElement?.append(panel);

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.target.set(0, 0.5, 0);
    controls.update();
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() { panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
