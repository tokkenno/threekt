import { Uniform, Uniforms, Vector2, type Camera, type Scene } from 'threekt';
import { RenderTargetType, WebGLRenderTarget, type WebGLRenderer } from 'threekt/render-gl';
import { addSliders } from './addSliders';
import { examplePanel } from './exampleControls';
import { encodeSrgb, fullscreenPass } from './fullscreenPass';

export function exampleBloom(canvas: HTMLCanvasElement, renderer: WebGLRenderer, selective: boolean) {
    const type = RenderTargetType.Float;
    const sceneColor = new WebGLRenderTarget(1,1,type);
    // Three horizontal/vertical separable scales give the halo both a tight edge and a wide falloff.
    const stages = Array.from({ length: 3 }, () => ({
        horizontal: new WebGLRenderTarget(1,1,type,false), vertical: new WebGLRenderTarget(1,1,type,false),
    }));
    const selection = selective ? new WebGLRenderTarget(1,1,type) : sceneColor;
    const threshold = new Uniform(selective ? 0.15 : 0.7), strength = new Uniform(1.5);
    const radius = new Uniform(0.4), exposure = new Uniform(1);
    // Float targets can be nearest-only. Interpolate their filter taps explicitly.
    const bilinear = `
        vec3 filterColor(sampler2D map, vec2 uv) {
            ivec2 upper = textureSize(map,0) - 1;
            vec2 p = clamp(uv * vec2(upper+1) - 0.5,vec2(0),vec2(upper));
            ivec2 a = ivec2(floor(p));
            ivec2 b = min(a+1,upper);
            vec2 f = fract(p);
            return mix(
                mix(texelFetch(map,a,0).rgb,texelFetch(map,ivec2(b.x,a.y),0).rgb,f.x),
                mix(texelFetch(map,ivec2(a.x,b.y),0).rgb,texelFetch(map,b,0).rgb,f.x),
                f.y);
        }
    `;
    const input = new Uniform(selection.texture);
    const step = new Vector2();
    const extract = new Uniform(true);
    const blur = fullscreenPass(`
        uniform sampler2D image;
        uniform vec2 delta;
        uniform float threshold;
        uniform bool extract;
        ${bilinear}
        vec3 sampleColor(vec2 uv) {
            vec3 color = filterColor(image,uv);
            if (extract) color *= smoothstep(threshold, threshold+0.25, max(color.r,max(color.g,color.b)));
            return color;
        }
        void main() {
            vec3 color = sampleColor(vUv) * 0.227027;
            color += (sampleColor(vUv + delta*1.384615) + sampleColor(vUv - delta*1.384615)) * 0.316216;
            color += (sampleColor(vUv + delta*3.230769) + sampleColor(vUv - delta*3.230769)) * 0.070270;
            gl_FragColor = vec4(color,1);
        }
    `, new Uniforms().set('image',input).set('delta',new Uniform(step)).set('threshold',threshold).set('extract',extract));
    const output = fullscreenPass(`
        uniform sampler2D base, closeHalo, middleHalo, wideHalo;
        uniform float strength, exposure, radius;
        ${encodeSrgb}
        ${bilinear}
        void main() {
            vec3 halo = filterColor(closeHalo,vUv) * mix(0.75,0.25,radius)
                      + filterColor(middleHalo,vUv) * 0.55
                      + filterColor(wideHalo,vUv) * mix(0.27,0.95,radius);
            vec3 color = (texture2D(base,vUv).rgb + halo * strength) * exposure;
            color = clamp((color*(2.51*color+0.03))/(color*(2.43*color+0.59)+0.14),0.0,1.0);
            gl_FragColor = vec4(encodeSrgb(color),1);
        }
    `, new Uniforms().set('base',new Uniform(sceneColor.texture)).set('closeHalo',new Uniform(stages[0].vertical.texture))
        .set('middleHalo',new Uniform(stages[1].vertical.texture)).set('wideHalo',new Uniform(stages[2].vertical.texture))
        .set('strength',strength).set('exposure',exposure).set('radius',radius));
    const panel = examplePanel(canvas);
    addSliders(canvas, [
        { label: 'Threshold', min: 0, max: 1, step: 0.01, value: Number(threshold.value), change: value => { threshold.value = value; } },
        { label: 'Strength', min: 0, max: 3, step: 0.01, value: 1.5, change: value => { strength.value = value; } },
        { label: 'Radius', min: 0, max: 1, step: 0.01, value: 0.4, change: value => { radius.value = value; } },
        { label: 'Exposure', min: 0.1, max: 2, step: 0.01, value: 1, change: value => { exposure.value = value; } },
    ],panel);
    return {
        render(scene: Scene, camera: Camera, renderSelection?: () => void) {
            sceneColor.setSize(canvas.width,canvas.height);
            renderer.setRenderTarget(sceneColor);
            renderer.render(scene,camera);
            if (renderSelection) {
                selection.setSize(canvas.width,canvas.height);
                renderer.setRenderTarget(selection);
                renderSelection();
            }
            stages.forEach(({horizontal,vertical},i) => {
                const width = Math.max(1,canvas.width >> (i+1)), height = Math.max(1,canvas.height >> (i+1));
                horizontal.setSize(width,height);
                vertical.setSize(width,height);
                input.value = i ? stages[i-1].vertical.texture : selection.texture;
                extract.value = i === 0;
                step.set(1/width,0);
                renderer.setRenderTarget(horizontal);
                renderer.render(blur.scene,blur.camera);
                input.value = horizontal.texture;
                extract.value = false;
                step.set(0,1/height);
                renderer.setRenderTarget(vertical);
                renderer.render(blur.scene,blur.camera);
            });
            renderer.setRenderTarget(null);
            renderer.render(output.scene,output.camera);
        },
        dispose() { panel.remove(); },
    };
}
