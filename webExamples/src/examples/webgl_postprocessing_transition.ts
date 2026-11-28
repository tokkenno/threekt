import {
    AmbientLight, BoxGeometry, Color, ColorSpace, DirectionalLight, IcosahedronGeometry, InstancedMesh,
    MeshPhongMaterial, Object3D, PerspectiveCamera, Scene, Uniform, Uniforms,
    type BufferGeometry,
} from 'threekt';
import { WebGL2Context, WebGLRenderer, WebGLRenderTarget } from 'threekt/render-gl';
import { addSelect, addToggle, examplePanel } from './exampleControls';
import { addSliders } from './addSliders';
import { fitPerspective } from './fitPerspective';
import { encodeSrgb, fullscreenPass } from './fullscreenPass';
import { loadImageTexture } from './loadImageTexture';

const masks = import.meta.glob('./assets/transition/*.png', { query: '?url', import: 'default', eager: true }) as Record<string, string>;

function createScene(geometry: BufferGeometry, background: number, tint: number) {
    const scene = new Scene();
    scene.background = Color.fromHex(background);
    scene.add(new AmbientLight(Color.fromHex(0xaaaaaa), 3));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 3);
    light.position.set(1, 1, 1);
    scene.add(light);
    const material = new MeshPhongMaterial(Color.fromHex(tint));
    material.flatShading = true;
    const mesh = new InstancedMesh(geometry, material, 500);
    const dummy = new Object3D(), color = new Color(1, 1, 1);
    for (let i = 0; i < mesh.count; i++) {
        dummy.position.set(Math.random() * 100 - 50, Math.random() * 60 - 30, Math.random() * 80 - 40);
        dummy.rotation.set(Math.random() * 6.28, Math.random() * 6.28, Math.random() * 6.28);
        dummy.scale.setScalar(Math.random() * 2 + 1);
        dummy.updateMatrix();
        mesh.setMatrixAt(i, dummy.matrix);
        mesh.setColorAt(i, color.setScalar(Math.random() * 0.9 + 0.1));
    }
    scene.add(mesh);
    return { scene, mesh, target: new WebGLRenderTarget() };
}

export async function createExample(canvas: HTMLCanvasElement) {
    const textures = await Promise.all(Array.from({ length: 6 }, (_, i) => loadImageTexture(masks[`./assets/transition/transition${i + 1}.png`])));
    for (const texture of textures) texture.colorSpace = ColorSpace.None;
    const a = createScene(new BoxGeometry(2, 2, 2), 0xffffff, 0x4070bd);
    const b = createScene(new IcosahedronGeometry(1, 1), 0, 0xd15537);
    const camera = new PerspectiveCamera(50, 1, 0.1, 100);
    camera.position.z = 20;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const progress = new Uniform(0), threshold = new Uniform(0.3);
    const mask = new Uniform(textures[0]), useMask = new Uniform(true);
    const pass = fullscreenPass(`
        uniform sampler2D firstScene, secondScene, mask;
        uniform float progress, threshold;
        uniform bool useMask;
        ${encodeSrgb}
        void main() {
            float edge = max(0.001, threshold);
            float t = progress;
            if (useMask) {
                float value = texture2D(mask, vUv).r;
                t = clamp((t * (1.0 + 2.0 * edge) - edge - value) / edge + 1.0, 0.0, 1.0);
            }
            vec3 first = texture2D(firstScene, vUv).rgb;
            vec3 second = texture2D(secondScene, vUv).rgb;
            gl_FragColor = vec4(encodeSrgb(mix(first, second, t)), 1.0);
        }
    `, new Uniforms().set('firstScene', new Uniform(a.target.texture)).set('secondScene', new Uniform(b.target.texture))
        .set('mask', mask).set('threshold', threshold).set('progress', progress).set('useMask', useMask));
    let sceneTime = 0, transitionTime = 0, previous = 0;
    let animateScene = true, animateTransition = true, cycle = true, selected = 0;
    const panel = examplePanel(canvas);
    addToggle(panel, 'Animate scene', true, value => { animateScene = value; });
    const animate = addToggle(panel, 'Animate transition', true, value => { animateTransition = value; });
    addToggle(panel, 'Use texture', true, value => { useMask.value = value; });
    addToggle(panel, 'Cycle textures', true, value => { cycle = value; });
    addSelect(panel, 'Texture', { 0: 'Perlin', 1: 'Squares', 2: 'Cells', 3: 'Distort', 4: 'Gradient', 5: 'Radial' }, '0', value => {
        selected = Number(value);
    });
    addSliders(canvas, [
        { label: 'Transition', min: 0, max: 1, step: 0.01, value: 0, change: value => {
            animateTransition = false;
            animate.checked = false;
            progress.value = value;
        } },
        { label: 'Threshold', min: 0, max: 1, step: 0.01, value: 0.3, change: value => { threshold.value = value; } },
    ], panel);
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const delta = Math.min(0.05, seconds - previous);
            previous = seconds;
            if (animateScene) sceneTime += delta;
            if (animateTransition) {
                transitionTime += delta;
                progress.value = (1 - Math.cos(transitionTime)) / 2;
            }
            mask.value = textures[(selected + (cycle ? Math.floor(transitionTime / (2 * Math.PI)) : 0)) % textures.length];
            a.mesh.rotation.y = -0.4 * sceneTime;
            b.mesh.rotation.set(0, 0.2 * sceneTime, 0.1 * sceneTime);
            for (const { scene, target } of [a, b]) {
                target.setSize(canvas.width, canvas.height);
                renderer.setRenderTarget(target);
                renderer.render(scene, camera);
            }
            renderer.setRenderTarget(null);
            renderer.render(pass.scene, pass.camera);
        },
        dispose() { panel.remove(); renderer.dispose(); },
    };
}
