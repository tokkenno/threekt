import {
    Color, DomInputSurface, Mesh, MeshBasicMaterial, MeshPhongMaterial, PerspectiveCamera, PlaneGeometry,
    PointLight, Scene, SphereGeometry, TrackballControls,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { fitPerspective } from './fitPerspective';

const characters = ' .:-+*=%@#';

export function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(70, 1, 1, 1000);
    camera.position.set(0, 150, 500);
    const scene = new Scene();
    scene.background = Color.fromHex(0);
    for (const [position, intensity] of [[500, 3], [-500, 1]]) {
        const light = new PointLight(Color.fromHex(0xffffff), intensity, 0, 0);
        light.position.set(position, position, position);
        scene.add(light);
    }
    const material = new MeshPhongMaterial();
    material.flatShading = true;
    const sphere = new Mesh(new SphereGeometry(200, 20, 10), material);
    scene.add(sphere);
    const floor = new Mesh(new PlaneGeometry(400, 400), new MeshBasicMaterial(Color.fromHex(0xe0e0e0)));
    floor.position.y = -200;
    floor.rotation.x = -Math.PI / 2;
    scene.add(floor);

    const buffer = document.createElement('canvas');
    const context = buffer.getContext('2d', { willReadFrequently: true })!;
    const output = document.createElement('pre');
    output.setAttribute('aria-hidden', 'true');
    Object.assign(output.style, {
        position: 'absolute', inset: '0', margin: '0', pointerEvents: 'none', overflow: 'hidden',
        color: 'white', background: 'black', font: '8px/8px monospace',
    });
    canvas.after(output);

    const controls = new TrackballControls(camera, new DomInputSurface(canvas));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            sphere.position.y = Math.abs(Math.sin(seconds * 2)) * 150;
            sphere.rotation.set(seconds * 0.3, 0, seconds * 0.2);
            renderer.render(scene, camera);

            const columns = Math.max(1, Math.floor(canvas.clientWidth / 4.82));
            const rows = Math.max(1, Math.floor(canvas.clientHeight / 8));
            if (buffer.width !== columns) buffer.width = columns;
            if (buffer.height !== rows) buffer.height = rows;
            context.drawImage(canvas, 0, 0, columns, rows);
            const { data } = context.getImageData(0, 0, columns, rows);
            const lines: string[] = [];
            for (let y = 0; y < rows; y++) {
                let line = '';
                for (let x = 0; x < columns; x++) {
                    const i = (x + y * columns) * 4;
                    const brightness = (0.3 * data[i] + 0.59 * data[i + 1] + 0.11 * data[i + 2]) / 255;
                    line += characters[Math.round(brightness * (characters.length - 1))];
                }
                lines.push(line);
            }
            output.textContent = lines.join('\n');
        },
        dispose() { output.remove(); controls.dispose(); renderer.dispose(); },
    };
}
