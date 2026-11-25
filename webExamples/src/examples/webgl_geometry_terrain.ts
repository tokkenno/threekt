import { Color, FogExp2, PerspectiveCamera, Scene, Vector3, WebGL2Context, WebGLRenderer } from 'threekt';
import { createTerrain } from './createTerrain';
import { fitPerspective } from './fitPerspective';
import { flyCamera } from './flyCamera';

export function createExample(canvas: HTMLCanvasElement) {
    const { mesh, heightAt } = createTerrain();
    const scene = new Scene();
    const sky = Color.fromHex(0xefd1b5);
    scene.background = sky;
    scene.fog = new FogExp2(sky, 0.0025);
    scene.add(mesh);
    const camera = new PerspectiveCamera(60, 1, 1, 10000);
    // Upstream starts at a fixed height of 800 and looks at the horizon, which can fall inside a random mountain or
    // show only fog until the pointer steers. Keep the upstream x and z, start 150 units above the local terrain,
    // and tilt the view down so the nearby ground is visible through the fog.
    camera.position.set(100, heightAt(100, -800) + 150, -800);
    camera.lookAt(new Vector3(-100, camera.position.y - 115, -800));
    const controls = flyCamera(canvas, camera, 150);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            controls.update(seconds);
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); },
    };
}
