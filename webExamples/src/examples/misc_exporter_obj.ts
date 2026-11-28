import {
    AmbientLight, BoxGeometry, BufferGeometry, Color, CylinderGeometry, DirectionalLight, DomInputSurface,
    Euler, Float32BufferAttribute, Group, Mesh, MeshPhongMaterial, OBJExporter, OrbitControls,
    PerspectiveCamera, Points, PointsMaterial, Scene,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { downloadGeometry } from './downloadGeometry';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(70, 1, 1, 1000);
    camera.position.z = 400;
    const scene = new Scene();
    const objects = new Group();
    scene.add(objects);
    scene.add(new AmbientLight(Color.fromHex(0xffffff)));
    const directional = new DirectionalLight(Color.fromHex(0xffffff), 2.5);
    directional.position.set(0, 1, 1);
    scene.add(directional);

    const triangle = new BufferGeometry()
        .setAttribute('position', new Float32BufferAttribute(new Float32Array([-50,-50,0,50,-50,0,50,50,0]), 3))
        .setAttribute('normal', new Float32BufferAttribute(new Float32Array([0,0,1,0,0,1,0,0,1]), 3));
    const box = new BoxGeometry(100, 100, 100);
    const cylinder = new CylinderGeometry(50, 50, 100, 30);
    const material = new MeshPhongMaterial(Color.fromHex(0x00cc00));
    material.specular.setHex(0);
    const pointMaterial = new PointsMaterial();
    pointMaterial.size = 10;
    pointMaterial.vertexColors = true;
    const cloud = new BufferGeometry()
        .setAttribute('position', new Float32BufferAttribute(new Float32Array([0,0,0,100,0,0,100,100,0,0,100,0]), 3))
        .setAttribute('color', new Float32BufferAttribute(new Float32Array([0.5,0,0,0.5,0,0,0,0.5,0,0,0.5,0]), 3));

    const select = document.createElement('select');
    ['Triangle', 'Cube', 'Cylinder', 'Multiple objects', 'Transformed objects', 'Point cloud']
        .forEach((name, i) => select.add(new Option(name, String(i))));
    select.setAttribute('aria-label', 'Geometry');
    const current: (Mesh | Points)[] = [];
    const update = () => {
        for (const child of current) objects.remove(child);
        current.length = 0;
        const index = select.selectedIndex;
        if (index === 5) {
            const points = new Points(cloud, pointMaterial);
            points.name = 'point cloud';
            objects.add(points);
            current.push(points);
            return;
        }
        const shapes = [triangle, box, cylinder];
        (index < 3 ? [shapes[index]] : shapes).forEach((geometry, i) => {
            const mesh = new Mesh(geometry, material);
            if (index >= 3) mesh.position.x = (i - 1) * 200;
            if (index === 4) mesh.quaternion.setFromEuler(new Euler(0, Math.PI / 4, 0));
            objects.add(mesh);
            current.push(mesh);
        });
    };
    select.addEventListener('change', update);
    update();
    const panel = document.createElement('aside');
    panel.className = 'example-panel';
    const button = document.createElement('button');
    button.textContent = 'Export OBJ';
    const exporter = new OBJExporter();
    button.addEventListener('click', () => downloadGeometry(exporter.parse(objects), 'object.obj'));
    panel.append(select, button);
    canvas.parentElement?.append(panel);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.enablePan = false;
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() { panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
