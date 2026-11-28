import {
    AmbientLight, Color, DirectionalLight, Mesh, MeshNormalMaterial, MeshPhongMaterial, OrthographicCamera,
    Scene, TextureFilter, Uniform, Uniforms, Vector2, Wrapping,
} from 'threekt';
import { BoxGeometry, IcosahedronGeometry, PlaneGeometry } from 'threekt/geometry';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { WebGL2Context, WebGLRenderer, WebGLRenderTarget } from 'threekt/render-gl';
import checkerUrl from './assets/checker.png?url';
import { addSliders } from './addSliders';
import { addToggle, examplePanel } from './exampleControls';
import { encodeSrgb, fullscreenPass } from './fullscreenPass';
import { loadImageTexture } from './loadImageTexture';

export async function createExample(canvas: HTMLCanvasElement) {
    const [floorMap, boxMap] = await Promise.all([loadImageTexture(checkerUrl), loadImageTexture(checkerUrl)]);
    for (const texture of [floorMap, boxMap]) {
        texture.wrapS = texture.wrapT = Wrapping.Repeat;
        texture.magFilter = texture.minFilter = TextureFilter.Nearest;
    }
    floorMap.repeat.set(3, 3);
    boxMap.repeat.set(1.5, 1.5);
    const scene = new Scene();
    scene.background = Color.fromHex(0x151729);
    const normals = new Scene();
    const normalMaterial = new MeshNormalMaterial();
    normalMaterial.flatShading = true;
    const register = (mesh: Mesh) => {
        scene.add(mesh);
        const normal = new Mesh(mesh.geometry, normalMaterial);
        normal.position.copyFrom(mesh.position);
        normal.rotation.copyFrom(mesh.rotation);
        normals.add(normal);
        return normal;
    };
    const floorMaterial = new MeshPhongMaterial();
    floorMaterial.map = floorMap;
    const floor = new Mesh(new PlaneGeometry(2, 2), floorMaterial);
    floor.rotation.x = -Math.PI / 2;
    register(floor);
    const boxMaterial = new MeshPhongMaterial();
    boxMaterial.map = boxMap;
    for (const [size, x, z] of [[0.4, 0, 0], [0.5, -0.5, -0.5]]) {
        const box = new Mesh(new BoxGeometry(size, size, size), boxMaterial);
        box.position.set(x, size / 2, z);
        box.rotation.y = Math.PI / 4;
        register(box);
    }
    const crystalMaterial = new MeshPhongMaterial(Color.fromHex(0x68b7e9));
    crystalMaterial.emissive.setHex(0x4f7e8b);
    const crystal = new Mesh(new IcosahedronGeometry(0.2), crystalMaterial);
    const normalCrystal = register(crystal);
    scene.add(new AmbientLight(Color.fromHex(0x757f8e), 3));
    const sun = new DirectionalLight(Color.fromHex(0xfffecd), 1.5);
    sun.position.set(1, 1, 1);
    scene.add(sun);
    const camera = new OrthographicCamera(-1, 1, 1, -1, 0.1, 10);
    camera.position.set(0, 2 * Math.tan(Math.PI / 6), 2);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.maxZoom = 2;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const target = new WebGLRenderTarget(1, 1, undefined, true, true);
    const normalTarget = new WebGLRenderTarget();
    for (const texture of [target.texture, normalTarget.texture]) texture.magFilter = TextureFilter.Nearest;
    const resolution = new Vector2();
    const normalEdge = new Uniform(0.3), depthEdge = new Uniform(0.4);
    const pass = fullscreenPass(`
        uniform sampler2D colorMap, depthMap, normalMap;
        uniform vec2 resolution;
        uniform float normalEdge, depthEdge;
        ${encodeSrgb}
        void main() {
            vec2 uv = (floor(vUv * resolution) + 0.5) / resolution;
            vec3 color = texture2D(colorMap, uv).rgb;
            vec3 n = texture2D(normalMap, uv).rgb;
            float d = texture2D(depthMap, uv).r;
            float normalDiff = 0.0, depthDiff = 0.0;
            for (int i = 0; i < 4; i++) {
                vec2 offset = (i < 2 ? vec2(i == 0 ? 1 : -1, 0) : vec2(0, i == 2 ? 1 : -1)) / resolution;
                normalDiff += distance(n, texture2D(normalMap, uv + offset).rgb);
                depthDiff += max(0.0, texture2D(depthMap, uv + offset).r - d);
            }
            color *= 1.0 - depthEdge * step(0.035, depthDiff);
            color *= 1.0 + normalEdge * step(0.2, normalDiff) * (1.0 - step(0.035, depthDiff));
            gl_FragColor = vec4(encodeSrgb(color), 1.0);
        }
    `, new Uniforms().set('colorMap', new Uniform(target.texture)).set('depthMap', new Uniform(target.depthTexture))
        .set('normalMap', new Uniform(normalTarget.texture)).set('resolution', new Uniform(resolution))
        .set('normalEdge', normalEdge).set('depthEdge', depthEdge));
    const panel = examplePanel(canvas);
    let pixelSize = 6;
    let snapping = true;
    addToggle(panel, 'Pixel snapping', true, value => { snapping = value; });
    addSliders(canvas, [
        { label: 'Pixel size', min: 1, max: 16, step: 1, value: 6, change: value => { pixelSize = value; } },
        { label: 'Normal edges', min: 0, max: 2, step: 0.05, value: 0.3, change: value => { normalEdge.value = value; } },
        { label: 'Depth edges', min: 0, max: 1, step: 0.05, value: 0.4, change: value => { depthEdge.value = value; } },
    ], panel);
    return {
        render(seconds: number) {
            const { clientWidth: w, clientHeight: h } = canvas;
            if (!w || !h) return;
            renderer.setSize(w, h);
            camera.left = -w / h;
            camera.right = w / h;
            camera.updateProjectionMatrix();
            controls.update();
            const t = snapping ? Math.round(seconds * 12) / 12 : seconds;
            crystal.position.set(0.35, 0.45 + 0.12 * Math.sin(t * 2), -0.25);
            crystal.rotation.set(t * 0.6, t, t * 0.2);
            normalCrystal.position.copyFrom(crystal.position);
            normalCrystal.rotation.copyFrom(crystal.rotation);
            const width = Math.max(1, Math.floor(w / pixelSize)), height = Math.max(1, Math.floor(h / pixelSize));
            resolution.set(width, height);
            target.setSize(width, height);
            normalTarget.setSize(width, height);
            renderer.setRenderTarget(target);
            renderer.render(scene, camera);
            renderer.setRenderTarget(normalTarget);
            renderer.render(normals, camera);
            renderer.setRenderTarget(null);
            renderer.render(pass.scene, pass.camera);
        },
        dispose() { panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
