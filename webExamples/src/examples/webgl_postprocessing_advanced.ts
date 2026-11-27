import { Color, DirectionalLight, Mesh, MeshBasicMaterial, MeshPhongMaterial, PerspectiveCamera, Scene,
    Uniform, Uniforms, Vector2, WebGL2Context, WebGLRenderer, WebGLRenderTarget } from 'threekt';
import head from './assets/lee-perry-smith/LeePerrySmith.glb?url';
import castle from './assets/cube/SwedishRoyalCastle/pz.jpg?url';
import { fitPerspective } from './fitPerspective';
import { encodeSrgb, fullscreenPass } from './fullscreenPass';
import { loadImageTexture } from './loadImageTexture';
import { readExampleGltfGeometry, type ExampleGltf } from './readExampleGltf';

export async function createExample(canvas: HTMLCanvasElement) {
    const [buffer, texture] = await Promise.all([fetch(head).then(response => response.arrayBuffer()), loadImageTexture(castle)]);
    const length = new DataView(buffer).getUint32(12, true);
    const json = JSON.parse(new TextDecoder().decode(new Uint8Array(buffer, 20, length))) as ExampleGltf;
    const geometry = readExampleGltfGeometry(json, buffer, 0, 28 + length);
    geometry.center();
    const material = new MeshPhongMaterial(Color.fromHex(0xc4a383));
    material.shininess = 12;
    const model = new Mesh(geometry, material);
    const silhouette = new Mesh(geometry, new MeshBasicMaterial());
    const modelScene = new Scene(), maskScene = new Scene();
    const light = new DirectionalLight(Color.fromHex(0xffffff), 3);
    light.position.set(0, -0.1, 1);
    modelScene.add(light);
    modelScene.add(model);
    maskScene.add(silhouette);
    const camera = new PerspectiveCamera(37, 1, 0.1, 100);
    camera.position.z = 12;
    const rendered = new WebGLRenderTarget(), mask = new WebGLRenderTarget();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const time = new Uniform(0);
    const size = new Vector2();
    const pass = fullscreenPass(`
        uniform sampler2D model, silhouette, backdrop;
        uniform vec2 size;
        uniform float time;
        ${encodeSrgb}
        vec3 source(vec2 uv) {
            vec3 back = texture2D(backdrop, uv).rgb;
            back += texture2D(backdrop, uv + vec2(0.009,0.0)).rgb;
            back += texture2D(backdrop, uv - vec2(0.009,0.0)).rgb;
            back += texture2D(backdrop, uv + vec2(0.0,0.009)).rgb;
            return mix(back / 4.0, texture2D(model, uv).rgb, texture2D(silhouette, uv).r);
        }
        void main() {
            vec2 uv = fract(vUv * 2.0);
            vec3 color = encodeSrgb(source(uv));
            float luma = dot(color, vec3(0.299,0.587,0.114));
            float noise = fract(sin(dot(uv * size, vec2(12.9898,78.233)) + time * 19.0) * 43758.5453) - 0.5;
            if (vUv.y < 0.5 && vUv.x < 0.5) {
                color = vec3(luma + noise * 0.25);
            } else if (vUv.y < 0.5) {
                vec2 cell = uv * size * 0.55;
                float dots = sin(cell.x + cell.y) * sin(cell.x - cell.y);
                vec3 tint = mix(vec3(1.0,0.75,0.5), vec3(1.0,0.8,0.8), texture2D(silhouette, uv).r);
                color = tint * clamp(luma * 1.5 - 0.25 + dots * 0.32, 0.0, 1.0);
            } else if (vUv.x < 0.5) {
                color = vec3(dot(color,vec3(0.393,0.769,0.189)),dot(color,vec3(0.349,0.686,0.168)),
                    dot(color,vec3(0.272,0.534,0.131))) + noise * 0.16;
            } else {
                vec3 halo = max(source(uv + vec2(0.018,0.0)) + source(uv - vec2(0.018,0.0)), 0.0) * 0.18;
                vec3 bleach = mix(2.0 * luma * color, 1.0 - 2.0 * (1.0-luma) * (1.0-color), step(0.5, luma));
                color = mix(color, bleach, 0.74) + halo + noise * 0.17;
            }
            color *= 1.0 - 0.68 * smoothstep(0.18, 0.72, length(uv - 0.5));
            gl_FragColor = vec4(color, 1.0);
        }
    `, new Uniforms().set('model', new Uniform(rendered.texture)).set('silhouette', new Uniform(mask.texture))
        .set('backdrop', new Uniform(texture)).set('time', time).set('size', new Uniform(size)));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const w = Math.max(1, Math.floor(canvas.width / 2)), h = Math.max(1, Math.floor(canvas.height / 2));
            size.set(w, h);
            time.value = seconds;
            model.rotation.y = silhouette.rotation.y = -seconds * 0.4;
            rendered.setSize(w, h);
            mask.setSize(w, h);
            renderer.setRenderTarget(rendered);
            renderer.render(modelScene, camera);
            renderer.setRenderTarget(mask);
            renderer.render(maskScene, camera);
            renderer.setRenderTarget(null);
            renderer.render(pass.scene, pass.camera);
        },
        dispose: () => renderer.dispose(),
    };
}
