import {
    AmbientLight, Color, DirectionalLight, Euler, Fog, ImageTexture, Mesh, MeshPhongMaterial,
    PerspectiveCamera, PlaneGeometry, Scene, WebGL2Context, WebGLRenderer, Wrapping,
} from 'threekt';
import crateUrl from './assets/crate.gif?url';
import { fitPerspective } from './fitPerspective';
import { followPointer } from './followPointer';
import { loadImageTexture } from './loadImageTexture';
import { renderTextureComparison } from './renderTextureComparison';

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(35, 1, 1, 25000);
    camera.position.z = 1500;
    const texture = await loadImageTexture(crateUrl);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const maximum = renderer.capabilities.getMaxAnisotropy();
    const geometry = new PlaneGeometry(100, 100);
    const rotation = new Euler(-Math.PI / 2, 0, 0);
    const createScene = (map: ImageTexture, anisotropy: number) => {
        const scene = new Scene();
        scene.background = Color.fromHex(0xf2f7ff);
        scene.fog = new Fog(Color.fromHex(0xf2f7ff), 1, 25000);
        scene.add(new AmbientLight(Color.fromHex(0xeef0ff), 3));
        const directional = new DirectionalLight(Color.fromHex(0xffffff), 6);
        directional.position.set(1, 1, 1);
        scene.add(directional);
        map.anisotropy = anisotropy;
        map.wrapS = map.wrapT = Wrapping.Repeat;
        map.repeat.set(512, 512);
        const material = new MeshPhongMaterial();
        material.map = map;
        const floor = new Mesh(geometry, material);
        floor.quaternion.setFromEuler(rotation);
        floor.scale.set(1000, 1000, 1000);
        scene.add(floor);
        return scene;
    };
    const scenes: [Scene, Scene] = [
        createScene(texture, Math.max(1, maximum)),
        createScene(new ImageTexture(texture.image), 1),
    ];
    const panel = document.createElement('aside');
    panel.className = 'example-panel';
    panel.textContent = maximum ? `Anisotropía · izquierda ${maximum}× · derecha 1×` : 'Anisotropía no disponible';
    canvas.parentElement?.append(panel);
    const pointer = followPointer(canvas);

    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            pointer.update(camera, scenes[0].position, 200, 50, 1000);
            renderTextureComparison(canvas, renderer, camera, scenes);
        },
        dispose() {
            pointer.dispose();
            panel.remove();
            renderer.dispose();
        },
    };
}
