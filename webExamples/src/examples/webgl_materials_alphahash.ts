import {
    Color, DomInputSurface, IcosahedronGeometry, InstancedMesh, Object3D, OrbitControls,
    PerspectiveCamera, Scene, Uniform, Uniforms
} from 'threekt';
import { ShaderMaterial, WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { addSliders } from './addSliders';
import { addToggle, examplePanel } from './exampleControls';
import { exampleSupersampling } from './exampleSupersampling';
import { fitPerspective } from './fitPerspective';
import alphaHash from './shaders/AlphaHash.glsl?raw';

export function createExample(canvas: HTMLCanvasElement) {
    const opacity = new Uniform(0.5), hashed = new Uniform(true);
    const material = new ShaderMaterial(`
        varying vec3 p, n, tint;
        void main() {
            vec4 transformed = instanceMatrix * vec4(position, 1.0);
            p = transformed.xyz;
            n = normalMatrix * mat3(instanceMatrix) * normal;
            tint = instanceColor;
            gl_Position = projectionMatrix * modelViewMatrix * transformed;
        }
    `, `
        varying vec3 p, n, tint;
        uniform float opacity;
        uniform bool hashed;
        ${alphaHash}
        void main() {
            if (hashed && opacity < getAlphaHashThreshold(p)) discard;
            float light = max(0.0, dot(normalize(n), normalize(vec3(0.55,0.9,1.0))));
            gl_FragColor = vec4(tint * (0.28 + 0.72 * light), hashed ? 1.0 : opacity);
        }
    `, new Uniforms().set('opacity', opacity).set('hashed', hashed));
    const scene = new Scene();
    scene.background = Color.fromHex(0x202730);
    const mesh = new InstancedMesh(new IcosahedronGeometry(0.54, 3), material, 27);
    const dummy = new Object3D();
    const color = new Color(1, 1, 1);
    for (let i = 0; i < mesh.count; i++) {
        dummy.position.set(i % 3 - 1, Math.floor(i / 3) % 3 - 1, Math.floor(i / 9) - 1);
        dummy.updateMatrix();
        mesh.setMatrixAt(i, dummy.matrix);
        mesh.setColorAt(i, color.setHSL(i / 27, 0.78, 0.54));
    }
    scene.add(mesh);
    const camera = new PerspectiveCamera(60, 1, 0.1, 100);
    camera.position.set(3.4, 3.0, 4.4);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    const context = new WebGL2Context(canvas);
    const renderer = new WebGLRenderer(context);
    const sampling = exampleSupersampling(renderer);
    const panel = examplePanel(canvas);
    let temporal = true;
    addToggle(panel, 'Alpha hash', true, value => {
        hashed.value = value;
        material.transparent = !value;
        material.depthWrite = value;
        sampling.reset();
    });
    addToggle(panel, 'Temporal antialiasing', true, value => { temporal = value; sampling.reset(); });
    addSliders(canvas, [{ label: 'Opacity', min: 0, max: 1, step: 0.01, value: 0.5,
        change: value => { opacity.value = value; sampling.reset(); } }], panel);
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            if (controls.update()) sampling.reset();
            sampling.resize(canvas.width, canvas.height);
            if (!temporal) sampling.reset();
            sampling.sample(scene, camera, temporal ? 32 : 1);
            sampling.present();
        },
        dispose() { controls.dispose(); renderer.dispose(); panel.remove(); },
    };
}
