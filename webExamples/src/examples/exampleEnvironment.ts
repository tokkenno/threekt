import { Color, Mesh, Side, SphereGeometry, Uniform, type Scene, type Uniforms } from 'threekt';
import { ShaderMaterial } from 'threekt/render-gl';
import { cubeLookup } from './exampleCubeMap';
import { encodeSrgb } from './fullscreenPass';

const vertex = `
    varying vec3 vWorld, vNormal, vInstanceTint;
    void main() {
        mat4 transform = modelMatrix;
        vInstanceTint = vec3(1.0);
        #ifdef USE_INSTANCING
        transform *= instanceMatrix;
        #endif
        #ifdef USE_INSTANCING_COLOR
        vInstanceTint = instanceColor;
        #endif
        vWorld = (transform * vec4(position, 1.0)).xyz;
        vNormal = normalize(mat3(transform) * normal);
        gl_Position = projectionMatrix * viewMatrix * vec4(vWorld, 1.0);
    }
`;

export function environmentMaterial(uniforms: Uniforms, tint = 0xffffff, ratio = 0, mix = 1, linearOutput = false) {
    return new ShaderMaterial(vertex, `
        varying vec3 vWorld, vNormal, vInstanceTint;
        uniform vec3 tint;
        uniform float ratio, amount;
        ${cubeLookup}
        ${encodeSrgb}
        void main() {
            vec3 normal = normalize(vNormal);
            vec3 eye = normalize(vWorld - cameraPosition);
            vec3 direction = ratio > 0.0 ? refract(eye, normal, ratio) : reflect(eye, normal);
            vec3 base = tint * vInstanceTint;
            vec3 color = mix(base * (0.18 + 0.65 * max(0.0, dot(normal, normalize(vec3(0.5, 0.8, 1.0))))),
                             base * sampleCube(direction), amount);
            if (ratio > 0.0) {
                float grazing = 0.12 + 0.46 * pow(1.0 - max(0.0, dot(-eye, normal)), 4.0);
                color = mix(color, sampleCube(reflect(eye, normal)), grazing);
            }
            gl_FragColor = vec4(${linearOutput ? 'color' : 'encodeSrgb(color)'}, 1.0);
        }
    `, uniforms.set('tint', new Uniform(Color.fromHex(tint))).set('ratio', new Uniform(ratio)).set('amount', new Uniform(mix)));
}

export function addEnvironment(scene: Scene, uniforms: Uniforms, radius: number, linearOutput = false) {
    const material = new ShaderMaterial(vertex, `
        varying vec3 vWorld;
        ${cubeLookup}
        ${encodeSrgb}
        void main() {
            vec3 color = sampleCube(vWorld - cameraPosition);
            gl_FragColor = vec4(${linearOutput ? 'color' : 'encodeSrgb(color)'}, 1.0);
        }
    `, uniforms);
    material.side = Side.Back;
    material.depthWrite = false;
    const dome = new Mesh(new SphereGeometry(radius, 32, 16), material);
    dome.renderOrder = -1;
    scene.add(dome);
    return dome;
}
