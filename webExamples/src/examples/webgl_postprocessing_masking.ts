import { BoxGeometry, Color, Mesh, MeshBasicMaterial, PerspectiveCamera, Scene, TorusGeometry,
    Uniform, Uniforms, WebGL2Context, WebGLRenderer, WebGLRenderTarget } from 'threekt';
import fruit from './assets/758px-Canestra_di_frutta_(Caravaggio).jpg?url';
import photograph from './assets/2294472375_24a3b8ef46_o.jpg?url';
import { fitPerspective } from './fitPerspective';
import { encodeSrgb, fullscreenPass } from './fullscreenPass';
import { loadImageTexture } from './loadImageTexture';

export async function createExample(canvas: HTMLCanvasElement) {
    const [fruitMap, photoMap] = await Promise.all([fruit, photograph].map(loadImageTexture));
    const camera = new PerspectiveCamera(50, 1, 1, 1000);
    camera.position.z = 14;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const material = new MeshBasicMaterial(Color.fromHex(0xffffff));
    const targets = [new WebGLRenderTarget(), new WebGLRenderTarget()];
    const scenes = [new Scene(), new Scene()];
    const box = new Mesh(new BoxGeometry(4, 4, 4), material);
    const ring = new Mesh(new TorusGeometry(3, 1, 16, 32), material);
    scenes[0].add(box);
    scenes[1].add(ring);
    const pass = fullscreenPass(`
        uniform sampler2D boxMask, ringMask, fruit, photo;
        ${encodeSrgb}
        void main() {
            vec3 color = mix(vec3(0.745), texture2D(fruit, vUv).rgb, texture2D(boxMask, vUv).r);
            color = mix(color, texture2D(photo, vUv).rgb, texture2D(ringMask, vUv).r);
            gl_FragColor = vec4(encodeSrgb(color), 1.0);
        }
    `, new Uniforms().set('boxMask', new Uniform(targets[0].texture)).set('ringMask', new Uniform(targets[1].texture))
        .set('fruit', new Uniform(fruitMap)).set('photo', new Uniform(photoMap)));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            box.position.set(Math.cos(seconds / 1.5) * 2, Math.sin(seconds) * 2, 0);
            box.rotation.set(seconds, seconds * 0.7, 0);
            ring.position.set(Math.sin(seconds) * 2, Math.cos(seconds / 1.5) * 2, 0);
            ring.rotation.set(seconds * 0.6, seconds * 0.8, 0);
            for (const [i, target] of targets.entries()) {
                target.setSize(canvas.width, canvas.height);
                renderer.setRenderTarget(target);
                renderer.render(scenes[i], camera);
            }
            renderer.setRenderTarget(null);
            renderer.render(pass.scene, pass.camera);
        },
        dispose() { renderer.dispose(); },
    };
}
