import {
    BufferGeometry, Color, Float32BufferAttribute, Lut, Mesh, MeshLambertMaterial, PerspectiveCamera,
    PointLight, Scene, Side,
} from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import pressureUrl from './assets/pressure.json?url';
import { addSelect, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';

type PressureData = { data: { attributes: { position: { array: number[] }; pressure: { array: number[] } } } };

export async function createExample(canvas: HTMLCanvasElement) {
    const response = await fetch(pressureUrl);
    if (!response.ok) throw new Error('Could not load pressure.json.');
    const { data: { attributes: { position, pressure } } }: PressureData = await response.json();
    const colors = new Float32BufferAttribute(new Float32Array(position.array.length), 3);
    const geometry = new BufferGeometry()
        .setAttribute('position', new Float32BufferAttribute(new Float32Array(position.array), 3))
        .setAttribute('color', colors);
    geometry.center().computeVertexNormals();
    const material = new MeshLambertMaterial(Color.fromHex(0xf5f5f5));
    material.side = Side.Double;
    material.vertexColors = true;

    const camera = new PerspectiveCamera(60, 1, 1, 100);
    camera.position.z = 10;
    const scene = new Scene();
    scene.background = Color.fromHex(0xffffff);
    scene.add(camera);
    camera.add(new PointLight(Color.fromHex(0xffffff), 3, 0, 0));
    scene.add(new Mesh(geometry, material));

    const lut = new Lut().setMin(0).setMax(2000);
    const scale = document.createElement('canvas');
    scale.width = 1;
    scale.height = lut.n + 1;
    const context = scale.getContext('2d')!;
    const legend = document.createElement('div');
    legend.className = 'pressure-legend';
    legend.setAttribute('aria-label', 'Pressure scale from 0 to 2000');
    const maximum = document.createElement('span');
    maximum.textContent = '2000';
    const minimum = document.createElement('span');
    minimum.textContent = '0';
    legend.append(maximum, scale, minimum);
    canvas.after(legend);
    function updatePalette(name: string) {
        lut.setColorMap(name);
        const color = new Color(0, 0, 0);
        pressure.array.forEach((value, index) => {
            color.copySRGBToLinear(lut.getColor(value));
            colors.setXYZ(index, color.r, color.g, color.b);
        });
        colors.markUpdated();
        const image = context.createImageData(1, scale.height);
        for (let y = 0; y < scale.height; y++) {
            const { r, g, b } = lut.getColor(2000 * (1 - y / lut.n));
            image.data.set([r * 255, g * 255, b * 255, 255], y * 4);
        }
        context.putImageData(image, 0, 0);
    }
    updatePalette('rainbow');
    const panel = examplePanel(canvas);
    addSelect(panel, 'Color map', { rainbow: 'Rainbow', cooltowarm: 'Cool to warm', blackbody: 'Blackbody', grayscale: 'Grayscale' },
        'rainbow', updatePalette);

    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() { legend.remove(); panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
