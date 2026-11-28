import {
    Color, Mesh, MeshPhongMaterial, PerspectiveCamera, PointLight, Scene, Side, Uniform, Uniforms,
} from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { SphereGeometry } from 'threekt/geometry';
import { ShaderMaterial, WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import woodUrl from './assets/hardwood2_diffuse.jpg?url';
import { addSelect, addToggle, examplePanel } from './exampleControls';
import { addSliders } from './addSliders';
import { cubeLookup, loadExampleCubeMap } from './exampleCubeMap';
import { fitPerspective } from './fitPerspective';
import { encodeSrgb, fullscreenPass } from './fullscreenPass';
import { loadImageTexture } from './loadImageTexture';

export async function createExample(canvas: HTMLCanvasElement) {
    const [wood, cube] = await Promise.all([loadImageTexture(woodUrl), loadExampleCubeMap('pisa')]);
    const textureOpacity = new Uniform(1), cubeOpacity = new Uniform(1);
    const texture = fullscreenPass(`
        uniform sampler2D wood;
        uniform float opacity;
        ${encodeSrgb}
        void main() { gl_FragColor = vec4(encodeSrgb(texture2D(wood, vUv).rgb), opacity); }
    `, new Uniforms().set('wood', new Uniform(wood)).set('opacity', textureOpacity));
    texture.material.transparent = true;
    const cubeMaterial = new ShaderMaterial(`
        varying vec3 vWorld;
        void main() {
            vWorld = (modelMatrix * vec4(position, 1.0)).xyz;
            gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }
    `, `
        varying vec3 vWorld;
        uniform float opacity;
        ${cubeLookup}
        ${encodeSrgb}
        void main() { gl_FragColor = vec4(encodeSrgb(sampleCube(vWorld - cameraPosition)), opacity); }
    `, cube.set('opacity', cubeOpacity));
    cubeMaterial.side = Side.Back;
    cubeMaterial.transparent = true;
    cubeMaterial.depthTest = false;
    cubeMaterial.depthWrite = false;
    const backdrop = new Mesh(new SphereGeometry(8, 32, 16), cubeMaterial);
    const cubeScene = new Scene();
    cubeScene.add(backdrop);
    const scene = new Scene();
    const ball = new Mesh(new SphereGeometry(1, 48, 24), new MeshPhongMaterial(Color.fromHex(0xc1c1c1)));
    scene.add(ball);
    const lights = [0xefffef, 0xffefef, 0xefefff].map(hex => new PointLight(Color.fromHex(hex), 100));
    for (const light of lights) scene.add(light);
    const camera = new PerspectiveCamera(65, 1, 0.1, 30);
    camera.position.z = 7;
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.enablePan = false;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    renderer.autoClear = false;

    let clear = true, showTexture = true, showCube = true, showScene = true, alpha = 1;
    const color = new Color(0, 0, 0);
    const panel = examplePanel(canvas);
    addToggle(panel, 'Clear pass', true, value => { clear = value; });
    addSelect(panel, 'Clear color', { '000000': 'Black', ffffff: 'White', '0000ff': 'Blue', '008000': 'Green', ff0000: 'Red' },
        '000000', value => { color.setHex(parseInt(value, 16)); });
    addToggle(panel, 'Texture pass', true, value => { showTexture = value; });
    addToggle(panel, 'Cube texture pass', true, value => { showCube = value; });
    addToggle(panel, 'Render pass', true, value => { showScene = value; });
    addSliders(canvas, [
        { label: 'Clear alpha', min: 0, max: 1, step: 0.01, value: 1, change: value => { alpha = value; } },
        { label: 'Texture opacity', min: 0, max: 1, step: 0.01, value: 1, change: value => { textureOpacity.value = value; } },
        { label: 'Cube opacity', min: 0, max: 1, step: 0.01, value: 1, change: value => { cubeOpacity.value = value; } },
    ], panel);
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            backdrop.position.copyFrom(camera.position);
            for (let i = 0; i < lights.length; i++) {
                const t = seconds + i * Math.PI * 2 / 3;
                lights[i].position.set(Math.sin(t) * 5, 2 * Math.cos(t * 0.7), Math.cos(t) * 5);
            }
            renderer.setClearColor(color, clear ? alpha : 0);
            renderer.clear();
            if (showTexture) renderer.render(texture.scene, texture.camera);
            if (showCube) renderer.render(cubeScene, camera);
            if (showScene) renderer.render(scene, camera);
        },
        dispose() { panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
