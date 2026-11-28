import { Uniform, Uniforms } from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { addSelect, examplePanel } from './exampleControls';
import { fullscreenPass } from './fullscreenPass';

export function createExample(canvas: HTMLCanvasElement) {
    const procedure = new Uniform(3);
    const pass = fullscreenPass(`
        uniform float procedure;
        float rand(vec2 uv) {
            return fract(sin(mod(dot(uv, vec2(12.9898, 78.233)), 3.14159265)) * 43758.5453);
        }
        void main() {
            float r = rand(vUv);
            float g = rand(vUv + vec2(0.4, 0.6));
            float b = rand(vUv + vec2(0.6, 0.4));
            vec3 color = procedure < 1.5 ? vec3(r) : procedure < 2.5
                ? mix(mix(vec3(1.0), vec3(0.0, 0.0, 1.0), r), vec3(0.0), g) : vec3(r, g, b);
            gl_FragColor = vec4(color, 1.0);
        }
    `, new Uniforms().set('procedure', procedure));
    const panel = examplePanel(canvas);
    addSelect(panel, 'Noise', { 1: 'Random 1D', 2: 'Random 2D', 3: 'Random 3D' }, '3',
        value => { procedure.value = Number(value); });
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!canvas.clientWidth || !canvas.clientHeight) return;
            renderer.setSize(canvas.clientWidth, canvas.clientHeight, window.devicePixelRatio);
            renderer.render(pass.scene, pass.camera);
        },
        dispose() { panel.remove(); renderer.dispose(); },
    };
}
