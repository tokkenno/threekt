import { BlendEquation, BlendFactor, Blending, Color, Mesh, MeshBasicMaterial, PerspectiveCamera, PlaneGeometry, Scene, WebGL2Context, WebGLRenderer } from 'threekt';
import flareUrl from './assets/lensflare0_alpha.png?url';
import { addBlendingBackground, generateLabelMaterial } from './blendingBackground';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

export async function createExample(canvas: HTMLCanvasElement) {
    // Keep the full matrix in view at desktop and narrower viewport sizes.
    const camera = new PerspectiveCamera(80, 1, 1, 1000);
    camera.position.z = 700;
    const scene = new Scene();
    const updateBackground = addBlendingBackground(scene, camera);
    const texture = await loadImageTexture(flareUrl);
    const source = [
        BlendFactor.Zero, BlendFactor.One, BlendFactor.SrcColor, BlendFactor.OneMinusSrcColor,
        BlendFactor.SrcAlpha, BlendFactor.OneMinusSrcAlpha, BlendFactor.DstAlpha, BlendFactor.OneMinusDstAlpha,
        BlendFactor.DstColor, BlendFactor.OneMinusDstColor, BlendFactor.SrcAlphaSaturate,
    ];
    const destination = source.slice(0, -1);
    const materials: MeshBasicMaterial[] = [];
    const square = new PlaneGeometry(100, 100);
    const labelGeometry = new PlaneGeometry(100, 25);
    const xAt = (column: number) => (column - source.length / 2) * 110;
    const yAt = (row: number) => -((row - destination.length / 2) * 110 + 50);
    const addLabel = (factor: BlendFactor, background: string, x: number, y: number) => {
        const material = generateLabelMaterial(factor.name, background, 'bold 11pt arial', 8);
        const mesh = new Mesh(labelGeometry, material);
        mesh.position.set(x, y, 0);
        scene.add(mesh);
    };
    destination.forEach((dst, row) => {
        source.forEach((src, column) => {
            const material = new MeshBasicMaterial(Color.fromHex(0xffffff), texture);
            material.transparent = true;
            material.blending = Blending.Custom;
            material.blendSrc = src;
            material.blendDst = dst;
            const mesh = new Mesh(square, material);
            mesh.position.set(xAt(column), yAt(row), 0);
            scene.add(mesh);
            materials.push(material);
        });
        addLabel(dst, '#960000', xAt(0) - 125, yAt(row) + 5);
    });
    source.forEach((src, column) => addLabel(src, '#009600', xAt(column), yAt(0) + 70));

    const panel = document.createElement('aside');
    panel.className = 'example-panel';
    const label = document.createElement('label');
    const select = document.createElement('select');
    const equations = [BlendEquation.Add, BlendEquation.Subtract, BlendEquation.ReverseSubtract, BlendEquation.Min, BlendEquation.Max];
    equations.forEach((equation, index) => select.add(new Option(equation.name, String(index))));
    select.addEventListener('change', () => {
        const equation = equations[select.selectedIndex];
        for (const material of materials) material.blendEquation = equation;
    });
    label.append('Ecuación de mezcla ', select);
    panel.append(label);
    canvas.parentElement?.append(panel);

    const renderer = new WebGLRenderer(new WebGL2Context(canvas, false));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            // Match the upstream perspective while keeping the table visible in narrow layouts.
            camera.zoom = Math.min(1, camera.aspect * 0.72);
            camera.updateProjectionMatrix();
            updateBackground();
            renderer.render(scene, camera);
        },
        dispose() {
            panel.remove();
            renderer.dispose();
        },
    };
}
