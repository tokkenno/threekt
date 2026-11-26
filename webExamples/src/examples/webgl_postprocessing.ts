import { Uniform, Uniforms, Vector2, WebGL2Context, WebGLRenderer, WebGLRenderTarget } from 'threekt';
import { createPostprocessingCloud } from './createPostprocessingCloud';
import { fitPerspective } from './fitPerspective';
import { fullscreenPass } from './fullscreenPass';
import dots from './shaders/DotScreenShader.glsl?raw';
import rgbShift from './shaders/RGBShiftShader.glsl?raw';

export function createExample(canvas: HTMLCanvasElement) {
    const { scene, camera, cloud } = createPostprocessingCloud(false);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const target = new WebGLRenderTarget();
    const dotted = new WebGLRenderTarget(1, 1, undefined, false);
    const size = new Vector2();
    const dotPass = fullscreenPass(dots, new Uniforms()
        .set('tDiffuse', new Uniform(target.texture)).set('tSize', new Uniform(size))
        .set('center', new Uniform(new Vector2(0.5, 0.5))).set('angle', new Uniform(1.57)).set('scale', new Uniform(4)));
    const shiftPass = fullscreenPass(rgbShift, new Uniforms().set('tDiffuse', new Uniform(dotted.texture))
        .set('amount', new Uniform(0.0015)).set('angle', new Uniform(0)));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            cloud.rotation.set(seconds * 0.3, seconds * 0.6, 0);
            target.setSize(canvas.width, canvas.height);
            dotted.setSize(canvas.width, canvas.height);
            size.set(canvas.width, canvas.height);
            renderer.setRenderTarget(target);
            renderer.render(scene, camera);
            renderer.setRenderTarget(dotted);
            renderer.render(dotPass.scene, dotPass.camera);
            renderer.setRenderTarget(null);
            renderer.render(shiftPass.scene, shiftPass.camera);
        },
        dispose() { renderer.dispose(); },
    };
}
