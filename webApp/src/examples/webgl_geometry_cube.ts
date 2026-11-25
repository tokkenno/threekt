import { Box, Color, Euler, ImageTexture, Mesh, MeshBasicMaterial, PerspectiveCamera, Scene, WebGL2Context, WebGLRenderer } from 'threekt';
import crateUrl from './assets/crate.gif?url';
import { fitPerspective } from './fitPerspective';

/** Port of Three.js examples/webgl_geometry_cube.html. */
export async function createExample(canvas: HTMLCanvasElement) {
    const image = new Image();
    image.src = crateUrl;
    await image.decode();

    const scene = new Scene();
    const camera = new PerspectiveCamera(70, 1, 0.1, 100);
    camera.position.set(0, 0, 2);
    const texture = new ImageTexture(image);
    const cube = new Mesh(new Box(), new MeshBasicMaterial(Color.fromHex(0xffffff), texture));
    scene.add(cube);
    const rotation = new Euler();

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            // Equivalent to the original increments of 0.005 and 0.01 radians per frame at 60 Hz.
            cube.quaternion.setFromEuler(rotation.set(seconds * 0.3, seconds * 0.6, 0));
            renderer.render(scene, camera);
        },
        dispose() {
            renderer.dispose();
        },
    };
}
