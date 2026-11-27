import { Object3D, PerspectiveCamera, Scene, Vector3 } from 'threekt';
import { examplePanel } from './exampleControls';
import { cssTrackball, exampleCss3d, fitCssCamera } from './exampleCss3d';
import { elements } from './periodicTable';

export function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    const camera = new PerspectiveCamera(40, 1, 1, 10000);
    camera.position.z = 3300;
    const css = exampleCss3d(canvas, '#000b10');
    const controls = cssTrackball(canvas, camera);
    controls.minDistance = 500;
    controls.maxDistance = 6000;
    const look = new Vector3();
    const cards = elements.map(([symbol, name, weight, column, row], i) => {
        const card = document.createElement('div');
        card.className = 'css3d-card';
        card.style.background = `rgba(0, 126, 128, ${0.35 + Math.random() * 0.5})`;
        for (const [tag, text] of [['small', String(i + 1)], ['strong', symbol], ['span', name], ['span', weight]]) {
            const field = document.createElement(tag);
            field.textContent = text;
            card.append(field);
        }
        const node = css.add(card);
        node.position.set(Math.random() * 3500 - 1750, Math.random() * 3000 - 1500, Math.random() * 1800 - 900);
        scene.add(node);
        const table = new Object3D();
        table.position.set(column * 140 - 1330, -row * 180 + 990, 0);
        const phi = Math.acos(-1 + 2 * i / elements.length);
        const sphere = new Object3D();
        sphere.position.setFromSphericalCoords(800, phi, Math.sqrt(elements.length * Math.PI) * phi);
        sphere.lookAt(look.copyFrom(sphere.position).multiplyScalar(2));
        const helix = new Object3D();
        helix.position.setFromCylindricalCoords(900, i * 0.175 + Math.PI, -i * 8 + 450);
        helix.lookAt(look.set(helix.position.x * 2, helix.position.y, helix.position.z * 2));
        const grid = new Object3D();
        grid.position.set(i % 5 * 400 - 800, 800 - Math.floor(i / 5) % 5 * 400, Math.floor(i / 25) * 1000 - 2000);
        return { node, start: node.position.copy(), orientation: node.quaternion.copy(), layouts: { table, sphere, helix, grid } };
    });
    let layout: keyof typeof cards[number]['layouts'] = 'table';
    let startTime = 0, time = 0;
    const panel = examplePanel(canvas);
    for (const name of ['table', 'sphere', 'helix', 'grid'] as const) {
        const button = document.createElement('button');
        button.textContent = name[0].toUpperCase() + name.slice(1);
        button.addEventListener('click', () => {
            layout = name;
            startTime = time;
            for (const { node, start, orientation } of cards) {
                start.copyFrom(node.position);
                orientation.copyFrom(node.quaternion);
            }
        });
        panel.append(button);
    }
    return {
        render(seconds: number) {
            if (!fitCssCamera(canvas, camera)) return;
            time = seconds;
            controls.update();
            const t = Math.min(1, (seconds - startTime) / 2);
            const eased = t * t * (3 - 2 * t);
            for (const { node, start, orientation, layouts } of cards) {
                node.position.copyFrom(start).lerp(layouts[layout].position, eased);
                node.quaternion.copyFrom(orientation).slerp(layouts[layout].quaternion, eased);
            }
            css.render(scene, camera);
        },
        dispose() { panel.remove(); controls.dispose(); css.dispose(); },
    };
}
