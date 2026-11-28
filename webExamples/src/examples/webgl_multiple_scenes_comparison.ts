import { Color, HemisphereLight, Mesh, MeshPhongMaterial, PerspectiveCamera, Scene } from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { IcosahedronGeometry } from 'threekt/geometry';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { fitPerspective } from './fitPerspective';

export function createExample(canvas: HTMLCanvasElement) {
    const geometry = new IcosahedronGeometry(1, 3);
    const scenes = [0xbcd48f, 0x8fbcd4].map((background, index) => {
        const scene = new Scene();
        scene.background = Color.fromHex(background);
        scene.add(new HemisphereLight(Color.fromHex(0xffffff), Color.fromHex(0x444444), 3));
        const material = new MeshPhongMaterial(Color.fromHex(0xffffff));
        material.wireframe = index === 1;
        scene.add(new Mesh(geometry, material));
        return scene;
    });
    const camera = new PerspectiveCamera(35, 1, 0.1, 100);
    camera.position.z = 6;
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const split = document.createElement('input');
    split.type = 'range';
    split.min = '0';
    split.max = '100';
    split.value = '50';
    split.setAttribute('aria-label', 'Comparison split');
    split.className = 'scene-comparison-slider';
    canvas.after(split);
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const width = canvas.clientWidth, height = canvas.clientHeight;
            const divider = Math.round(width * split.valueAsNumber / 100);
            renderer.resetViewport();
            renderer.setScissorTest(true);
            renderer.setScissor(0, 0, divider, height);
            renderer.render(scenes[0], camera);
            renderer.setScissor(divider, 0, width - divider, height);
            renderer.render(scenes[1], camera);
            renderer.setScissorTest(false);
        },
        dispose() { split.remove(); controls.dispose(); renderer.dispose(); },
    };
}
