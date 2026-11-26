import {
    Box, Color, Fog, Mesh, MeshNormalMaterial, PerspectiveCamera, Scene, Uniform, Uniforms,
    WebGL2Context, WebGLRenderer, WebGLRenderTarget,
} from 'threekt';
import { addToggle, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';
import { fullscreenPass, texturePass } from './fullscreenPass';

export function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(70, 1, 1, 1000);
    camera.position.z = 400;
    const scene = new Scene();
    scene.fog = new Fog(Color.fromHex(0), 1, 1000);
    const cube = new Mesh(new Box(150, 150, 150, 2, 2, 2), new MeshNormalMaterial());
    scene.add(cube);
    const source = new WebGLRenderTarget();
    let history = new WebGLRenderTarget(1, 1, undefined, false);
    let output = new WebGLRenderTarget(1, 1, undefined, false);
    const old = new Uniform(history.texture);
    const damp = new Uniform(0.96);
    const pass = fullscreenPass(`
        uniform sampler2D tOld, tNew;
        uniform float damp;
        void main() {
            vec4 old = texture2D(tOld, vUv);
            gl_FragColor = max(texture2D(tNew, vUv), old * damp * max(sign(old - 0.1), 0.0));
        }
    `, new Uniforms().set('tOld', old).set('tNew', new Uniform(source.texture)).set('damp', damp));
    const display = texturePass(output.texture);

    const panel = examplePanel(canvas);
    const label = document.createElement('label');
    label.className = 'example-setting';
    label.textContent = 'Damping';
    const input = document.createElement('input');
    Object.assign(input, { type: 'range', min: '0', max: '1', step: '0.001', value: '0.96' });
    input.addEventListener('input', () => { damp.value = input.valueAsNumber; });
    label.append(input);
    panel.append(label);
    let enabled = true;
    addToggle(panel, 'Afterimage', enabled, value => {
        enabled = value;
        history.dispose();
    });
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            cube.rotation.set(seconds * 0.3, seconds * 0.6, 0);
            if (!enabled) {
                renderer.render(scene, camera);
                return;
            }
            for (const target of [source, history, output]) target.setSize(canvas.width, canvas.height);
            renderer.setRenderTarget(source);
            renderer.render(scene, camera);
            renderer.setRenderTarget(output);
            old.value = history.texture;
            renderer.render(pass.scene, pass.camera);
            renderer.setRenderTarget(null);
            display.input.value = output.texture;
            renderer.render(display.scene, display.camera);
            [history, output] = [output, history];
        },
        dispose() { panel.remove(); renderer.dispose(); },
    };
}
