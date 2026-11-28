import {
    AmbientLight, Color, DirectionalLight, Euler, Fog, Mesh, MeshPhongMaterial, PerspectiveCamera, Scene,
    Wrapping,
} from 'threekt';
import { PlaneGeometry } from 'threekt/geometry';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import crateUrl from './assets/crate.gif?url';
import { loadImageTexture } from './exampleControls';
import { createTextureComparison } from './textureComparison';

export async function createExample(canvas: HTMLCanvasElement) {
    const textures = await Promise.all([loadImageTexture(crateUrl), loadImageTexture(crateUrl)]);
    const camera = new PerspectiveCamera(35, 1, 1, 25000);
    camera.position.z = 1500;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const maxAnisotropy = renderer.capabilities.getMaxAnisotropy();
    const geometry = new PlaneGeometry(100, 100);
    const scenes = textures.map((texture, index) => {
        const scene = new Scene();
        scene.background = Color.fromHex(0xf2f7ff);
        scene.fog = new Fog(Color.fromHex(0xf2f7ff), 1, 25000);
        scene.add(new AmbientLight(Color.fromHex(0xeef0ff), 3));
        const light = new DirectionalLight(Color.fromHex(0xffffff), 6);
        light.position.set(1, 1, 1);
        scene.add(light);

        texture.anisotropy = index === 0 ? Math.max(1, maxAnisotropy) : 1;
        texture.wrapS = texture.wrapT = Wrapping.Repeat;
        texture.repeat.set(512, 512);
        const material = new MeshPhongMaterial();
        material.map = texture;
        const floor = new Mesh(geometry, material);
        floor.quaternion.setFromEuler(new Euler(-Math.PI / 2, 0, 0));
        floor.scale.setScalar(1000);
        scene.add(floor);
        return scene;
    });
    return createTextureComparison(canvas, camera, renderer, [scenes[0], scenes[1]], [
        maxAnisotropy > 1 ? `Anisotropy ×${maxAnisotropy}` : 'Anisotropy unavailable',
        'Anisotropy ×1',
    ], true);
}
