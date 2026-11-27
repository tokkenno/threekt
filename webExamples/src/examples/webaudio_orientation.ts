import { BoxGeometry, BufferGeometry, Color, DirectionalLight, DomInputSurface, Float32BufferAttribute, Fog, GridHelper,
    HemisphereLight, LineBasicMaterial, LineSegments, Mesh, MeshBasicMaterial, MeshPhongMaterial,
    OrbitControls, PerspectiveCamera, PlaneGeometry, Scene, WebGL2Context, WebGLRenderer } from 'threekt';
import model from './assets/BoomBox.glb?url';
import { badCatSound, exampleAudio, loadAudioBuffer, playAudioBuffer, spatialAudio } from './exampleAudio';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';
import { readExampleGltfGeometry, type ExampleGltf } from './readExampleGltf';

async function loadBoomBox() {
    const response = await fetch(model);
    if (!response.ok) throw new Error('Could not load BoomBox.');
    const binary = await response.arrayBuffer();
    const length = new DataView(binary).getUint32(12, true);
    const start = length + 28;
    const json = JSON.parse(new TextDecoder().decode(new Uint8Array(binary, 20, length))) as ExampleGltf & {
        images: { bufferView: number; mimeType: string }[];
        textures: { source?: number; extensions?: { EXT_texture_webp?: { source: number } } }[];
        materials: { pbrMetallicRoughness: { baseColorTexture: { index: number } } }[];
    };
    const geometry = readExampleGltfGeometry(json, binary, 0, start);
    const texture = json.textures[json.materials[0].pbrMetallicRoughness.baseColorTexture.index];
    const source = texture.extensions?.EXT_texture_webp?.source ?? texture.source;
    if (source === undefined) throw new Error('BoomBox does not contain a base color image.');
    const image = json.images[source];
    const view = json.bufferViews[image.bufferView];
    const url = URL.createObjectURL(new Blob([new Uint8Array(binary, start + (view.byteOffset ?? 0), view.byteLength)], { type: image.mimeType }));
    try {
        const material = new MeshPhongMaterial();
        material.map = await loadImageTexture(url);
        material.map.flipY = false;
        return new Mesh(geometry, material);
    } finally { URL.revokeObjectURL(url); }
}

export async function createExample(canvas: HTMLCanvasElement) {
    const radio = await loadBoomBox();
    radio.geometry.rotateY(-Math.PI);
    radio.scale.setScalar(20);
    radio.position.y = 0.2;
    const scene = new Scene();
    const grey = Color.fromHex(0xa0a0a0);
    scene.background = grey;
    scene.fog = new Fog(grey, 2, 20);
    scene.add(new HemisphereLight(Color.fromHex(0xffffff), Color.fromHex(0x8d8d8d), 3));
    const sun = new DirectionalLight(Color.fromHex(0xffffff), 3);
    sun.position.set(5, 5, 0);
    scene.add(sun);
    const floor = new Mesh(new PlaneGeometry(50, 50), new MeshPhongMaterial(Color.fromHex(0xcbcbcb)));
    floor.rotation.x = -Math.PI / 2;
    scene.add(floor);
    const grid = new GridHelper(50, 50, Color.fromHex(0xc1c1c1), Color.fromHex(0xc1c1c1));
    grid.position.y = 0.001;
    scene.add(grid);
    scene.add(radio);
    const red = new MeshBasicMaterial(Color.fromHex(0xff0000));
    red.transparent = true;
    red.opacity = 0.42;
    const wall = new Mesh(new BoxGeometry(2, 1, 0.1), red);
    wall.position.set(0, 0.5, -0.5);
    scene.add(wall);

    // Draw the inner hemisphere and outer cone in the horizontal plane around the emitter.
    const guides: number[] = [];
    for (const range of [Math.PI / 2, 230 * Math.PI / 360]) {
        const radius = range < 2 ? 0.72 : 1;
        const point = (direction: number) => [radius * Math.sin(direction), 0.05, radius * Math.cos(direction)];
        guides.push(0, 0.2, 0, ...point(-range), 0, 0.2, 0, ...point(range));
        for (let i = 0; i < 24; i++) guides.push(...point(range * (i / 12 - 1)), ...point(range * ((i + 1) / 12 - 1)));
    }
    scene.add(new LineSegments(new BufferGeometry().setAttribute('position', new Float32BufferAttribute(new Float32Array(guides), 3)),
        new LineBasicMaterial(Color.fromHex(0x1eaf61))));
    const camera = new PerspectiveCamera(45, 1, 0.1, 100);
    camera.position.set(3, 2, 3);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.target.y = 0.1;
    controls.maxDistance = 7;
    const spatial = spatialAudio();
    const audio = exampleAudio(canvas, async context => {
        const sample = await loadAudioBuffer(context, badCatSound);
        if (context.state === 'closed') return;
        const panner = new PannerNode(context, { panningModel: 'HRTF', refDistance: 1, coneInnerAngle: 180,
            coneOuterAngle: 230, coneOuterGain: 0.1 });
        spatial.updatePanner(panner, radio);
        panner.connect(context.destination);
        playAudioBuffer(context, sample, panner, true);
    });
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            if (audio.context?.state === 'running') spatial.updateListener(audio.context, camera);
            renderer.render(scene, camera);
        },
        dispose() { audio.dispose(); controls.dispose(); renderer.dispose(); },
    };
}
