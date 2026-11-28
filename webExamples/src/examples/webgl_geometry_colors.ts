import { Color, PerspectiveCamera, Scene } from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { addColoredPolyhedra } from './coloredPolyhedra';
import { fitPerspective } from './fitPerspective';
import { followPointer } from './followPointer';

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(20, 1, 1, 10000);
    camera.position.z = 1800;
    const scene = new Scene();
    scene.background = Color.fromHex(0xffffff);
    addColoredPolyhedra(scene);

    const pointer = followPointer(canvas);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            pointer.update(camera, scene.position);
            renderer.render(scene, camera);
        },
        dispose() {
            pointer.dispose();
            renderer.dispose();
        },
    };
}
