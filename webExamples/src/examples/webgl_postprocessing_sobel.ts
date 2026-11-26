import {
    AmbientLight, Color, DomInputSurface, Mesh, MeshPhongMaterial, OrbitControls, PerspectiveCamera, PointLight,
    Scene, TorusKnotGeometry, Uniform, Uniforms, Vector2, WebGL2Context, WebGLRenderer, WebGLRenderTarget,
} from 'threekt';
import { addToggle, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';
import { fullscreenPass } from './fullscreenPass';

export function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(70, 1, 0.1, 100);
    camera.position.z = 5;
    const scene = new Scene();
    scene.add(new Mesh(new TorusKnotGeometry(1, 0.3, 256, 32), new MeshPhongMaterial(Color.fromHex(0xffff00))));
    scene.add(new AmbientLight(Color.fromHex(0xe7e7e7)));
    camera.add(new PointLight(Color.fromHex(0xffffff), 20));
    scene.add(camera);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    const target = new WebGLRenderTarget();
    const resolution = new Vector2();
    const pass = fullscreenPass(`
        uniform sampler2D tDiffuse;
        uniform vec2 resolution;
        float gray(vec2 offset) {
            return dot(texture2D(tDiffuse, vUv + offset / resolution).rgb, vec3(0.299, 0.587, 0.114));
        }
        void main() {
            float a = gray(vec2(-1, -1)), b = gray(vec2(0, -1)), c = gray(vec2(1, -1));
            float d = gray(vec2(-1, 0)), f = gray(vec2(1, 0));
            float g = gray(vec2(-1, 1)), h = gray(vec2(0, 1)), i = gray(vec2(1, 1));
            vec2 gradient = vec2(c + 2.0*f + i - a - 2.0*d - g, g + 2.0*h + i - a - 2.0*b - c);
            gl_FragColor = vec4(vec3(length(gradient)), 1.0);
        }
    `, new Uniforms().set('tDiffuse', new Uniform(target.texture)).set('resolution', new Uniform(resolution)));
    const panel = examplePanel(canvas);
    let enabled = true;
    addToggle(panel, 'Sobel edges', enabled, value => { enabled = value; });
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            if (!enabled) {
                renderer.render(scene, camera);
                return;
            }
            resolution.set(canvas.width, canvas.height);
            target.setSize(canvas.width, canvas.height);
            renderer.setRenderTarget(target);
            renderer.render(scene, camera);
            renderer.setRenderTarget(null);
            renderer.render(pass.scene, pass.camera);
        },
        dispose() { panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
