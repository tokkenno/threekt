import { Uniform, Uniforms, Vector2, type PerspectiveCamera, type Scene } from 'threekt';
import { WebGL2Context, WebGLRenderer, WebGLRenderTarget } from 'threekt/render-gl';
import { encodeSrgb, fullscreenPass } from './fullscreenPass';

export function exampleDepthOfField(canvas: HTMLCanvasElement, camera: PerspectiveCamera, autofocus: boolean) {
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const target = new WebGLRenderTarget(1, 1, undefined, true, true);
    const focus = new Uniform(500), aperture = new Uniform(0.00005), maxBlur = new Uniform(0.011);
    const automatic = new Uniform(autofocus), showFocus = new Uniform(false);
    const focusPoint = new Vector2(0.5, 0.5);
    const aspect = new Uniform(1);
    const pass = fullscreenPass(`
        uniform sampler2D colorMap, depthMap;
        uniform float cameraNear, cameraFar, focus, aperture, maxBlur, aspect;
        uniform bool automatic, showFocus;
        uniform vec2 focusPoint;
        ${encodeSrgb}
        float linearDepth(vec2 uv) {
            float z = texture2D(depthMap, uv).r;
            return cameraNear * cameraFar / (cameraFar - z * (cameraFar - cameraNear));
        }
        void main() {
            float focal = automatic ? linearDepth(focusPoint) : focus;
            float distance = linearDepth(vUv);
            float size = min(abs(distance - focal) * aperture, maxBlur);
            vec3 color = texture2D(colorMap, vUv).rgb;
            for (int ring = 1; ring <= 3; ring++) for (int tap = 0; tap < 8; tap++) {
                float angle = (float(tap) + float(ring) * 0.3) * 0.785398163;
                vec2 offset = vec2(cos(angle) / aspect, sin(angle)) * size * float(ring) / 3.0;
                color += texture2D(colorMap, vUv + offset).rgb;
            }
            color /= 25.0;
            if (showFocus) color = mix(vec3(0.95, 0.45, 0.05), color, smoothstep(0.0, 8.0, abs(distance-focal)));
            gl_FragColor = vec4(encodeSrgb(color), 1.0);
        }
    `, new Uniforms().set('colorMap', new Uniform(target.texture)).set('depthMap', new Uniform(target.depthTexture))
        .set('cameraNear', new Uniform(camera.near)).set('cameraFar', new Uniform(camera.far)).set('focus', focus)
        .set('aperture', aperture).set('maxBlur', maxBlur).set('aspect', aspect).set('automatic', automatic)
        .set('showFocus', showFocus).set('focusPoint', new Uniform(focusPoint)));
    return {
        renderer, focus, aperture, maxBlur, automatic, showFocus, focusPoint,
        render(scene: Scene) {
            target.setSize(canvas.width, canvas.height);
            aspect.value = canvas.width / canvas.height;
            renderer.setRenderTarget(target);
            renderer.render(scene, camera);
            renderer.setRenderTarget(null);
            renderer.render(pass.scene, pass.camera);
        },
    };
}
