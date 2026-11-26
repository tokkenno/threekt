import { ColorSpace, Mesh, PerspectiveCamera, Scene, ShaderMaterial, TorusGeometry, Uniform, Uniforms, Vector2, Vector3,
    WebGL2Context, WebGLRenderer, WebGLRenderTarget, Wrapping } from 'threekt';
import cloud from './assets/lava/cloud.png?url';
import lava from './assets/lava/lavatile.jpg?url';
import { fitPerspective } from './fitPerspective';
import { encodeSrgb, fullscreenPass } from './fullscreenPass';
import { loadImageTexture } from './loadImageTexture';

const vertex = `
uniform vec2 uvScale;
varying vec2 vUv;

void main()
{

    vUv = uvScale * uv;
    vec4 mvPosition = modelViewMatrix * vec4( position, 1.0 );
    gl_Position = projectionMatrix * mvPosition;

}
`;
const fragment = `
uniform float time;

uniform float fogDensity;
uniform vec3 fogColor;

uniform sampler2D texture1;
uniform sampler2D texture2;

varying vec2 vUv;

void main( void ) {

    vec2 position = - 1.0 + 2.0 * vUv;

    vec4 noise = texture2D( texture1, vUv );
    vec2 T1 = vUv + vec2( 1.5, - 1.5 ) * time * 0.02;
    vec2 T2 = vUv + vec2( - 0.5, 2.0 ) * time * 0.01;

    T1.x += noise.x * 2.0;
    T1.y += noise.y * 2.0;
    T2.x -= noise.y * 0.2;
    T2.y += noise.z * 0.2;

    float p = texture2D( texture1, T1 * 2.0 ).a;

    vec4 color = texture2D( texture2, T2 * 2.0 );
    vec4 temp = color * ( vec4( p, p, p, p ) * 2.0 ) + ( color * color - 0.1 );

    if( temp.r > 1.0 ) { temp.bg += clamp( temp.r - 2.0, 0.0, 100.0 ); }
    if( temp.g > 1.0 ) { temp.rb += temp.g - 1.0; }
    if( temp.b > 1.0 ) { temp.rg += temp.b - 1.0; }

    gl_FragColor = temp;

    float depth = gl_FragCoord.z / gl_FragCoord.w;
    const float LOG2 = 1.442695;
    float fogFactor = exp2( - fogDensity * fogDensity * depth * depth * LOG2 );
    fogFactor = 1.0 - clamp( fogFactor, 0.0, 1.0 );

    gl_FragColor = mix( gl_FragColor, vec4( fogColor, gl_FragColor.w ), fogFactor );

}
`;

export async function createExample(canvas: HTMLCanvasElement) {
    const [noise, tile] = await Promise.all([cloud, lava].map(loadImageTexture));
    noise.colorSpace = ColorSpace.None;
    for (const texture of [noise, tile]) texture.wrapS = texture.wrapT = Wrapping.Repeat;
    const time = new Uniform(1);
    const material = new ShaderMaterial(vertex, fragment, new Uniforms()
        .set('time', time).set('uvScale', new Uniform(new Vector2(3, 1)))
        .set('fogDensity', new Uniform(0.45)).set('fogColor', new Uniform(new Vector3()))
        .set('texture1', new Uniform(noise)).set('texture2', new Uniform(tile)));
    const scene = new Scene();
    const camera = new PerspectiveCamera(35, 1, 1, 3000);
    camera.position.z = 4;
    const mesh = new Mesh(new TorusGeometry(0.65, 0.3, 30, 30), material);
    scene.add(mesh);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const target = new WebGLRenderTarget();
    const step = new Vector2();
    const pass = fullscreenPass(`
        uniform sampler2D tDiffuse;
        uniform vec2 pixel;
        ${encodeSrgb}
        void main() {
            vec3 original = texture2D(tDiffuse, vUv).rgb;
            vec3 halo = vec3(0.0);
            for (int x = -3; x <= 3; x++) for (int y = -3; y <= 3; y++) {
                vec3 tap = texture2D(tDiffuse, vUv + vec2(x,y) * pixel * 3.0).rgb;
                halo += max(tap - 0.2, 0.0);
            }
            gl_FragColor = vec4(encodeSrgb(original + halo * (1.25 / 49.0)), 1.0);
        }
    `, new Uniforms().set('tDiffuse', new Uniform(target.texture)).set('pixel', new Uniform(step)));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            time.value = 1 + seconds * 0.6;
            mesh.rotation.set(0.3 + seconds * 0.05, seconds * 0.12, 0);
            target.setSize(canvas.width, canvas.height);
            step.set(1 / canvas.width, 1 / canvas.height);
            renderer.setRenderTarget(target);
            renderer.render(scene, camera);
            renderer.setRenderTarget(null);
            renderer.render(pass.scene, pass.camera);
        },
        dispose() { renderer.dispose(); },
    };
}
