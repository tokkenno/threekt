import { Mesh, OrthographicCamera, PlaneGeometry, Scene, Uniform, Uniforms, type Texture } from 'threekt';
import { ShaderMaterial } from 'threekt/render-gl';

const vertex = `
    varying vec2 vUv;
    void main() {
        vUv = uv;
        gl_Position = vec4(position.xy, 0.0, 1.0);
    }
`;

export const encodeSrgb = `
    vec3 encodeSrgb(vec3 c) {
        c = max(c, vec3(0.0));
        return mix(12.92 * c, 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055, step(vec3(0.0031308), c));
    }
`;

export function fullscreenPass(fragment: string, uniforms = new Uniforms()) {
    const material = new ShaderMaterial(vertex, 'varying vec2 vUv;\n' + fragment, uniforms);
    material.depthTest = false;
    material.depthWrite = false;
    const mesh = new Mesh(new PlaneGeometry(2, 2), material);
    const scene = new Scene();
    scene.add(mesh);
    return { camera: new OrthographicCamera(-1, 1, 1, -1, 0, 1), scene, material, mesh };
}

export function texturePass(texture: Texture, encode = true) {
    const input = new Uniform(texture);
    return {
        ...fullscreenPass(`
            uniform sampler2D tDiffuse;
            ${encodeSrgb}
            void main() {
                vec4 color = texture2D(tDiffuse, vUv);
                gl_FragColor = ${encode ? 'vec4(encodeSrgb(color.rgb), color.a)' : 'color'};
            }
        `, new Uniforms().set('tDiffuse', input)),
        input,
    };
}
