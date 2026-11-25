import {
    AmbientLight, Box, Color, DirectionalLight, DomInputSurface, ImageTexture, Mesh, MeshPhongMaterial,
    OrbitControls, PerspectiveCamera, Scene, TGALoader, WebGL2Context, WebGLRenderer,
} from 'threekt';
import greyUrl from './assets/crate_grey8.tga?url';
import colorUrl from './assets/crate_color8.tga?url';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const loader = new TGALoader();
    const textures = await Promise.all([greyUrl, colorUrl].map(async url => {
        const response = await fetch(url);
        if (!response.ok) throw new Error(`Could not load the TGA texture (HTTP ${response.status}).`);
        const image = loader.parse(new Int8Array(await response.arrayBuffer()));
        const source = document.createElement('canvas');
        source.width = image.width;
        source.height = image.height;
        const rgba = new Uint8ClampedArray(image.pixels.buffer, image.pixels.byteOffset, image.pixels.byteLength).slice();
        source.getContext('2d')!.putImageData(new ImageData(rgba, image.width, image.height), 0, 0);
        return new ImageTexture(source);
    }));
    const geometry = new Box();
    const scene = new Scene();
    textures.forEach((texture, index) => {
        const material = new MeshPhongMaterial();
        material.map = texture;
        const mesh = new Mesh(geometry, material);
        mesh.position.x = index === 0 ? -1 : 1;
        scene.add(mesh);
    });
    scene.add(new AmbientLight(Color.fromHex(0xffffff), 1.5));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 2.5);
    light.position.set(1, 1, 1);
    scene.add(light);
    const camera = new PerspectiveCamera(45, 1, 0.1, 100);
    camera.position.set(0, 1, 5);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.enableZoom = false;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); },
    };
}
