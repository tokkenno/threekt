import {
    Box, Color, DirectionalLight, DomInputSurface, Group, IcosahedronGeometry, Mesh, MeshPhongMaterial,
    PDBLoader, PerspectiveCamera, Scene, TrackballControls, Vector3, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { addSelect, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';

const urls = import.meta.glob<string>('./assets/pdb/*.pdb', { eager: true, query: '?url', import: 'default' });
const names = {
    ethanol: 'Ethanol', aspirin: 'Aspirin', caffeine: 'Caffeine', nicotine: 'Nicotine', lsd: 'LSD',
    cocaine: 'Cocaine', cholesterol: 'Cholesterol', lycopene: 'Lycopene', glucose: 'Glucose',
    Al2O3: 'Aluminium oxide', cubane: 'Cubane', cu: 'Copper', caf2: 'Fluorite', nacl: 'Salt',
    ybco: 'YBCO superconductor', buckyball: 'Buckyball', graphite: 'Graphite',
};

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(70, 1, 1, 5000);
    camera.position.z = 1000;
    const scene = new Scene();
    scene.background = Color.fromHex(0x050505);
    for (const [sign, intensity] of [[1, 2.5], [-1, 1.5]]) {
        const light = new DirectionalLight(Color.fromHex(0xffffff), intensity);
        light.position.set(sign, sign, 1);
        scene.add(light);
    }
    const loader = new PDBLoader();
    const atomGeometry = new IcosahedronGeometry(1, 3);
    const bondGeometry = new Box(1, 1, 1);
    const bondMaterial = new MeshPhongMaterial();
    const read = async (name: string, signal?: AbortSignal) => {
        const response = await fetch(urls[`./assets/pdb/${name}.pdb`], { signal });
        if (!response.ok) throw new Error(`Could not load ${name} (HTTP ${response.status}).`);
        const { geometryAtoms, geometryBonds, atoms } = loader.parse(await response.text());
        geometryAtoms.computeBoundingBox();
        const center = geometryAtoms.boundingBox!.getCenter(new Vector3());
        const root = new Group();
        const fragment = document.createDocumentFragment();
        const labels = atoms.map(atom => {
            const mesh = new Mesh(atomGeometry, new MeshPhongMaterial(Color.fromHex(atom.color)));
            mesh.position.copyFrom(atom.position).sub(center).multiplyScalar(75);
            mesh.scale.setScalar(25);
            root.add(mesh);
            const label = document.createElement('span');
            label.textContent = atom.element;
            label.style.color = `#${atom.color.toString(16).padStart(6, '0')}`;
            fragment.append(label);
            return { mesh, label };
        });

        const bonds = geometryBonds.getAttribute('position')!;
        const readPosition = (index: number) =>
            new Vector3(bonds.getX(index), bonds.getY(index), bonds.getZ(index)).sub(center).multiplyScalar(75);
        for (let i = 0; i < bonds.count; i += 2) {
            const start = readPosition(i);
            const end = readPosition(i + 1);
            const mesh = new Mesh(bondGeometry, bondMaterial);
            mesh.position.copyFrom(start).lerp(end, 0.5);
            mesh.scale.set(5, 5, start.distanceTo(end));
            mesh.lookAt(end);
            root.add(mesh);
        }
        return { root, labels, fragment };
    };

    let model = await read('caffeine');
    scene.add(model.root);
    const overlay = document.createElement('div');
    overlay.className = 'atom-labels';
    overlay.setAttribute('aria-hidden', 'true');
    overlay.append(model.fragment);
    canvas.after(overlay);

    const panel = examplePanel(canvas);
    const status = document.createElement('p');
    status.setAttribute('role', 'status');
    status.hidden = true;
    let request: AbortController | undefined;
    addSelect(panel, 'Molecule', names, 'caffeine', async name => {
        request?.abort();
        const { signal } = request = new AbortController();
        status.hidden = true;
        try {
            const next = await read(name, signal);
            if (signal.aborted) return;
            scene.remove(model.root);
            model = next;
            scene.add(model.root);
            overlay.replaceChildren(model.fragment);
        } catch (error) {
            if (signal.aborted) return;
            status.textContent = String(error);
            status.hidden = false;
        }
    });
    panel.append(status);

    const controls = new TrackballControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 500;
    controls.maxDistance = 2000;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const projected = new Vector3();
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            model.root.rotation.set(seconds * 0.4, seconds * 0.28, 0);
            renderer.render(scene, camera);
            for (const { mesh, label } of model.labels) {
                projected.setFromMatrixPosition(mesh.matrixWorld).project(camera);
                label.hidden = Math.abs(projected.z) > 1;
                label.style.left = `${(projected.x + 1) * 50}%`;
                label.style.top = `${(1 - projected.y) * 50}%`;
                label.style.zIndex = String(Math.round((1 - projected.z) * 100000));
            }
        },
        dispose() { request?.abort(); overlay.remove(); panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
