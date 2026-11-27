import {
    AmbientLight, Color, ColorSpace, DataTexture, DirectionalLight, DomInputSurface, Mesh,
    MeshPhongMaterial, OrbitControls, PerspectiveCamera, Scene, TextureFilter, Uniform, Uniforms,
    WebGL2Context, WebGLRenderer, WebGLRenderTarget, type Texture,
} from 'threekt';
import model from './assets/damaged-helmet/DamagedHelmet.gltf?url';
import binary from './assets/damaged-helmet/DamagedHelmet.bin?url';
import albedo from './assets/damaged-helmet/Default_albedo.jpg?url';
import neutral from './assets/luts/NeutralLUT.png?url';
import bw from './assets/luts/B&WLUT.png?url';
import night from './assets/luts/NightLUT.png?url';
import bourbon from './assets/luts/Bourbon 64.CUBE?url';
import { addSliders } from './addSliders';
import { addSelect, examplePanel } from './exampleControls';
import { loadExampleCubeMap } from './exampleCubeMap';
import { addEnvironment } from './exampleEnvironment';
import { fitPerspective } from './fitPerspective';
import { encodeSrgb, fullscreenPass } from './fullscreenPass';
import { loadImageTexture } from './loadImageTexture';
import { readExampleGltfGeometry } from './readExampleGltf';

function configure(texture: Texture) {
    texture.colorSpace = ColorSpace.None;
    texture.flipY = false;
    texture.generateMipmaps = false;
    texture.minFilter = texture.magFilter = TextureFilter.Linear;
    return texture;
}

async function loadCubeTable() {
    const response = await fetch(bourbon);
    if (!response.ok) throw new Error('Could not load the Bourbon lookup table.');
    const text = await response.text();
    const size = Number(text.match(/^LUT_3D_SIZE\s+(\d+)$/m)?.[1]);
    if (!Number.isInteger(size) || size < 2) throw new Error('Invalid lookup table size.');
    const data = new Int8Array(size ** 3 * 4);
    let index = 0;
    for (const line of text.split('\n')) {
        if (!/^\s*[-+\d.]/.test(line)) continue;
        const values = line.trim().split(/\s+/).map(Number);
        const r = index % size, g = Math.floor(index / size) % size, b = Math.floor(index / (size ** 2));
        const offset = (g * size * size + b * size + r) * 4;
        data.set(values.slice(0, 3).map(value => Math.round(Math.max(0, Math.min(1, value)) * 255)), offset);
        data[offset + 3] = -1;
        index++;
    }
    if (index !== size ** 3) throw new Error('Lookup table data has the wrong length.');
    return { texture: configure(new DataTexture(data, size * size, size)), size };
}

export async function createExample(canvas: HTMLCanvasElement) {
    const [json, buffer, map, environment, ...tables] = await Promise.all([
        fetch(model).then(response => response.json()), fetch(binary).then(response => response.arrayBuffer()),
        loadImageTexture(albedo), loadExampleCubeMap('SwedishRoyalCastle'),
        ...[neutral, bw, night].map(async url => ({ texture: configure(await loadImageTexture(url)), size: 32 })),
        loadCubeTable(),
    ]);
    const scene = new Scene();
    const backdrop = addEnvironment(scene, environment, 15, true);
    const geometry = readExampleGltfGeometry(json, buffer, 0);
    const material = new MeshPhongMaterial(Color.fromHex(0xffffff));
    material.map = map;
    material.map.flipY = false;
    material.shininess = 30;
    const helmet = new Mesh(geometry, material);
    helmet.rotation.x = Math.PI / 2;
    scene.add(helmet);
    scene.add(new AmbientLight(Color.fromHex(0xa0b1c2), 1.7));
    for (const [x, color] of [[5, 0xffefd9], [-4, 0x86b9ff]]) {
        const light = new DirectionalLight(Color.fromHex(color), 2.5);
        light.position.set(x, 3, 4);
        scene.add(light);
    }
    const camera = new PerspectiveCamera(45, 1, 0.25, 20);
    camera.position.set(-1.8, 0.6, 3.4);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 2;
    controls.maxDistance = 8;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const target = new WebGLRenderTarget();
    const lut = new Uniform(tables[3].texture);
    const size = new Uniform(tables[3].size);
    const intensity = new Uniform(1);
    const pass = fullscreenPass(`
        uniform sampler2D image, lut;
        uniform float size, intensity;
        ${encodeSrgb}
        void main() {
            vec3 original = clamp(encodeSrgb(texture2D(image, vUv).rgb), 0.0, 1.0);
            vec3 p = original * (size - 1.0);
            float slice = floor(p.b);
            float y = (p.g + 0.5) / size;
            float x0 = (slice * size + p.r + 0.5) / (size * size);
            float x1 = (min(size - 1.0, slice + 1.0) * size + p.r + 0.5) / (size * size);
            vec3 graded = mix(texture2D(lut, vec2(x0, y)).rgb, texture2D(lut, vec2(x1, y)).rgb, fract(p.b));
            gl_FragColor = vec4(mix(original, graded, intensity), 1.0);
        }
    `, new Uniforms().set('image', new Uniform(target.texture)).set('lut', lut).set('size', size).set('intensity', intensity));
    const panel = examplePanel(canvas);
    addSelect(panel, 'Lookup table', { 0: 'Neutral', 1: 'Black and white', 2: 'Night', 3: 'Bourbon' }, '3', value => {
        lut.value = tables[Number(value)].texture;
        size.value = tables[Number(value)].size;
    });
    addSliders(canvas, [{ label: 'Intensity', min: 0, max: 1, step: 0.01, value: 1,
        change: value => { intensity.value = value; } }], panel);
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            backdrop.position.copyFrom(camera.position);
            target.setSize(canvas.width, canvas.height);
            renderer.setRenderTarget(target);
            renderer.render(scene, camera);
            renderer.setRenderTarget(null);
            renderer.render(pass.scene, pass.camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); panel.remove(); },
    };
}
