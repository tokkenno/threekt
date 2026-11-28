import {
    BoxGeometry, Color, DomInputSurface, Group, Mesh, MeshPhongMaterial, OrbitControls, PerspectiveCamera,
    PointLight, Scene, Uniform, Uniforms,
} from 'threekt';
import { ShaderMaterial, WebGL2Context, WebGLRenderer, WebGLRenderTarget } from 'threekt/render-gl';
import { addSelect, addToggle, examplePanel } from './exampleControls';
import { addSliders } from './addSliders';
import { fitPerspective } from './fitPerspective';
import { fullscreenPass } from './fullscreenPass';
import halftone from './shaders/HalftoneShader.glsl?raw';

export function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    scene.background = Color.fromHex(0x444444);
    const group = new Group();
    scene.add(group);
    const floor = new Mesh(new BoxGeometry(100, 1, 100), new MeshPhongMaterial());
    floor.position.y = -10;
    scene.add(floor);
    const light = new PointLight(Color.fromHex(0xffffff), 250);
    light.position.y = 2;
    scene.add(light);
    const geometry = new BoxGeometry(2, 2, 2);
    const material = new ShaderMaterial(`
        varying vec2 texcoord;
        varying vec3 faceNormal;
        void main() {
            texcoord = uv; faceNormal = normal;
            gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }
    `, `
        varying vec2 texcoord;
        varying vec3 faceNormal;
        void main() { gl_FragColor = vec4(abs(faceNormal) + vec3(texcoord, 0.0), 1.0); }
    `);
    for (let i = 0; i < 50; i++) {
        const mesh = new Mesh(geometry, material);
        mesh.position.set(Math.random() * 16 - 8, Math.random() * 16 - 8, Math.random() * 16 - 8);
        mesh.rotation.set(Math.random() * 6.28, Math.random() * 6.28, Math.random() * 6.28);
        group.add(mesh);
    }
    const camera = new PerspectiveCamera(75, 1, 1, 1000);
    camera.position.z = 12;
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const target = new WebGLRenderTarget();
    const width = new Uniform(1), height = new Uniform(1), radius = new Uniform(4), scatter = new Uniform(0);
    const shape = new Uniform(1), disable = new Uniform(false), greyscale = new Uniform(false), blending = new Uniform(1);
    const pass = fullscreenPass(halftone, new Uniforms().set('tDiffuse', new Uniform(target.texture))
        .set('width', width).set('height', height).set('radius', radius).set('scatter', scatter)
        .set('rotateR', new Uniform(Math.PI / 12)).set('rotateG', new Uniform(Math.PI / 6)).set('rotateB', new Uniform(Math.PI / 4))
        .set('shape', shape).set('disable', disable).set('greyscale', greyscale).set('blending', blending).set('blendingMode', new Uniform(1)));
    const panel = examplePanel(canvas);
    addSelect(panel, 'Dot shape', { 1: 'Dot', 2: 'Ellipse', 3: 'Line', 4: 'Square' }, '1', value => { shape.value = Number(value); });
    addToggle(panel, 'Greyscale', false, value => { greyscale.value = value; });
    addToggle(panel, 'Halftone', true, value => { disable.value = !value; });
    addSliders(canvas, [
        { label: 'Radius', min: 1, max: 25, step: 0.5, value: 4, change: value => { radius.value = value; } },
        { label: 'Scatter', min: 0, max: 1, step: 0.01, value: 0, change: value => { scatter.value = value; } },
    ], panel);
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            group.rotation.set(seconds * 0.6, seconds * 0.6, 0);
            target.setSize(canvas.width, canvas.height);
            width.value = canvas.width;
            height.value = canvas.height;
            renderer.setRenderTarget(target);
            renderer.render(scene, camera);
            renderer.setRenderTarget(null);
            renderer.render(pass.scene, pass.camera);
        },
        dispose() { panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
