import { Group, PerspectiveCamera, Scene, Vector3 } from 'threekt';
import { PDBLoader } from 'threekt/loaders';
import { addSelect, examplePanel } from './exampleControls';
import { cssPanel, cssTrackball, exampleCss3d, fitCssCamera } from './exampleCss3d';

const urls = import.meta.glob<string>('./assets/pdb/*.pdb', { eager: true, query: '?url', import: 'default' });
const molecules = {
    ethanol: 'Ethanol', aspirin: 'Aspirin', caffeine: 'Caffeine', nicotine: 'Nicotine', lsd: 'LSD',
    cocaine: 'Cocaine', cholesterol: 'Cholesterol', lycopene: 'Lycopene', glucose: 'Glucose',
    Al2O3: 'Aluminium oxide', cubane: 'Cubane', cu: 'Copper', caf2: 'Fluorite', nacl: 'Salt',
    ybco: 'YBCO superconductor', buckyball: 'Buckyball', graphite: 'Graphite',
};

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(70, 1, 1, 5000);
    camera.position.z = 1000;
    const scene = new Scene();
    const css = exampleCss3d(canvas, '#08090c');
    const controls = cssTrackball(canvas, camera);
    controls.minDistance = 250;
    controls.maxDistance = 2500;
    const root = new Group(), atoms = new Group(), bonds = new Group();
    scene.add(root);
    root.add(atoms);
    root.add(bonds);
    const yAxis = new Vector3(0, 1, 0);
    const end = new Vector3(), direction = new Vector3();
    const loader = new PDBLoader();
    const load = async (name: string, signal?: AbortSignal) => {
        const response = await fetch(urls[`./assets/pdb/${name}.pdb`], { signal });
        if (!response.ok) throw new Error('Could not load ' + name + '.');
        const parsed = loader.parse(await response.text());
        if (signal?.aborted) return;
        for (const group of [atoms, bonds]) for (const node of [...group.children.asJsReadonlyArrayView()]) css.remove(node);
        parsed.geometryAtoms.computeBoundingBox();
        const center = parsed.geometryAtoms.boundingBox!.getCenter(new Vector3());
        for (const atom of parsed.atoms) {
            const color = '#' + atom.color.toString(16).padStart(6, '0');
            const element = cssPanel(52, 52, `radial-gradient(circle at 32% 28%, white, ${color} 35%, #151515 100%)`);
            element.style.borderRadius = '50%';
            element.title = atom.element;
            const node = css.add(element, true);
            node.position.copyFrom(atom.position).sub(center).multiplyScalar(75);
            atoms.add(node);
        }
        const positions = parsed.geometryBonds.getAttribute('position')!;
        for (let i = 0; i < positions.count; i += 2) {
            direction.set(positions.getX(i), positions.getY(i), positions.getZ(i)).sub(center).multiplyScalar(75);
            end.set(positions.getX(i + 1), positions.getY(i + 1), positions.getZ(i + 1)).sub(center).multiplyScalar(75);
            const length = direction.distanceTo(end);
            const height = Math.max(1, length - 36);
            const first = css.add(cssPanel(9, height, 'linear-gradient(90deg,#667484,#eee,#667484)'));
            const second = css.add(cssPanel(9, height, '#aab5c0'));
            first.position.copyFrom(direction).lerp(end, 0.5);
            second.position.copyFrom(first.position);
            direction.subVectors(end, direction).normalize();
            first.quaternion.setFromUnitVectors(yAxis, direction);
            const rotate = new Group();
            rotate.rotation.y = Math.PI / 2;
            second.quaternion.copyFrom(first.quaternion).multiply(rotate.quaternion);
            bonds.add(first);
            bonds.add(second);
        }
    };
    await load('caffeine');
    const panel = examplePanel(canvas);
    const status = document.createElement('p');
    status.setAttribute('role', 'status');
    let request: AbortController | undefined;
    addSelect(panel, 'Molecule', molecules, 'caffeine', async name => {
        request?.abort();
        const { signal } = request = new AbortController();
        status.textContent = '';
        try { await load(name, signal); }
        catch (error) { if (!signal.aborted) status.textContent = String(error); }
    });
    addSelect(panel, 'Show', { both: 'Atoms and bonds', atoms: 'Atoms', bonds: 'Bonds' }, 'both', value => {
        atoms.visible = value !== 'bonds';
        bonds.visible = value !== 'atoms';
    });
    panel.append(status);
    return {
        render(seconds: number) {
            if (!fitCssCamera(canvas, camera)) return;
            controls.update();
            root.rotation.set(seconds * 0.22, seconds * 0.32, 0);
            css.render(scene, camera);
        },
        dispose() { request?.abort(); panel.remove(); controls.dispose(); css.dispose(); },
    };
}
