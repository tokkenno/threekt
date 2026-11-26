import { ColorSpace, DataTexture, Uniform, Uniforms, WebGL2Context, WebGLRenderer, WebGLRenderTarget } from 'threekt';
import { createPostprocessingCloud } from './createPostprocessingCloud';
import { addToggle, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';
import { fullscreenPass } from './fullscreenPass';
import glitch from './shaders/DigitalGlitch.glsl?raw';

export function createExample(canvas: HTMLCanvasElement) {
    const { scene, camera, cloud } = createPostprocessingCloud(true);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const target = new WebGLRenderTarget();
    const noise = new Int8Array(64 * 64 * 4);
    for (let i = 0; i < noise.length; i += 4) {
        const value = Math.random() * 256;
        noise.set([value, value, value, 255], i);
    }
    const displacement = new DataTexture(noise, 64, 64);
    displacement.colorSpace = ColorSpace.None;
    const byp = new Uniform(1);
    const amount = new Uniform(0);
    const angle = new Uniform(0);
    const seed = new Uniform(0);
    const seedX = new Uniform(0);
    const seedY = new Uniform(0);
    const distortionX = new Uniform(0);
    const distortionY = new Uniform(0);
    const pass = fullscreenPass(glitch, new Uniforms().set('tDiffuse', new Uniform(target.texture))
        .set('tDisp', new Uniform(displacement)).set('byp', byp).set('amount', amount).set('angle', angle)
        .set('seed', seed).set('seed_x', seedX).set('seed_y', seedY).set('distortion_x', distortionX)
        .set('distortion_y', distortionY).set('col_s', new Uniform(0.05)));
    const panel = examplePanel(canvas);
    let wild = false;
    addToggle(panel, 'Go wild', false, value => { wild = value; });
    let next = 1;
    let burstUntil = 0;
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            cloud.rotation.set(seconds * 0.3, seconds * 0.6, 0);
            if (seconds >= next) {
                burstUntil = seconds + 0.25;
                next = seconds + 2 + Math.random() * 2;
            }
            byp.value = wild || seconds < burstUntil ? 0 : 1;
            amount.value = Math.random() / 30;
            angle.value = (Math.random() * 2 - 1) * Math.PI;
            seed.value = Math.random();
            seedX.value = Math.random() * 2 - 1;
            seedY.value = Math.random() * 2 - 1;
            distortionX.value = Math.random();
            distortionY.value = Math.random();
            target.setSize(canvas.width, canvas.height);
            renderer.setRenderTarget(target);
            renderer.render(scene, camera);
            renderer.setRenderTarget(null);
            renderer.render(pass.scene, pass.camera);
        },
        dispose() { panel.remove(); renderer.dispose(); },
    };
}
