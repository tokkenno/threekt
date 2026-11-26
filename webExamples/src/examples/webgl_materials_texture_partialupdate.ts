import {
    DataTexture, Mesh, MeshBasicMaterial, PerspectiveCamera, PlaneGeometry, Scene, TextureFilter,
    WebGL2Context, WebGLRenderer,
} from 'threekt';
import checkerboard from './assets/FloorsCheckerboard_S_Diffuse.jpg';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

export async function createExample(canvas: HTMLCanvasElement) {
    const texture = await loadImageTexture(checkerboard);
    texture.minFilter = TextureFilter.Linear;
    texture.generateMipmaps = false;
    const pixels = new Int8Array(32 * 32 * 4);
    const patch = new DataTexture(pixels, 32, 32);
    const material = new MeshBasicMaterial();
    material.map = texture;
    const scene = new Scene();
    scene.add(new Mesh(new PlaneGeometry(2, 2), material));
    const camera = new PerspectiveCamera(70, 1, 0.01, 10);
    camera.position.z = 2;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    let last = 0;
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            if (seconds - last > 0.1) {
                last = seconds;
                const rgba = [...Array.from({ length: 3 }, () => Math.floor(Math.random() * 256)), 255];
                for (let i = 0; i < pixels.length; i += 4) pixels.set(rgba, i);
                renderer.copyTextureToTexture(patch, texture,
                    Math.floor(Math.random() * 16) * 32, Math.floor(Math.random() * 16) * 32);
            }
            renderer.render(scene, camera);
        },
        dispose() { renderer.dispose(); },
    };
}
