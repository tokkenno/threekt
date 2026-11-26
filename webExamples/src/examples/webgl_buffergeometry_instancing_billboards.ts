import {
    CircleGeometry, ColorSpace, Euler, InstancedBufferAttribute, InstancedBufferGeometry, Mesh, PerspectiveCamera, RawShaderMaterial,
    Scene, Uniform, WebGL2Context, WebGLRenderer,
} from 'threekt';
import circleUrl from './assets/circle.png?url';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

const vertexShader = `
precision highp float;
uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;
uniform float time;

attribute vec3 position;
attribute vec2 uv;
attribute vec3 translate;

varying vec2 vUv;
varying float vScale;

void main() {

    vec4 mvPosition = modelViewMatrix * vec4( translate, 1.0 );
    vec3 trTime = vec3(translate.x + time,translate.y + time,translate.z + time);
    float scale =  sin( trTime.x * 2.1 ) + sin( trTime.y * 3.2 ) + sin( trTime.z * 4.3 );
    vScale = scale;
    scale = scale * 10.0 + 10.0;
    mvPosition.xyz += position * scale;
    vUv = uv;
    gl_Position = projectionMatrix * mvPosition;

}
`;

const fragmentShader = `
precision highp float;

uniform sampler2D map;

varying vec2 vUv;
varying float vScale;

// HSL to RGB Conversion helpers
vec3 HUEtoRGB(float H){
    H = mod(H,1.0);
    float R = abs(H * 6.0 - 3.0) - 1.0;
    float G = 2.0 - abs(H * 6.0 - 2.0);
    float B = 2.0 - abs(H * 6.0 - 4.0);
    return clamp(vec3(R,G,B),0.0,1.0);
}

vec3 HSLtoRGB(vec3 HSL){
    vec3 RGB = HUEtoRGB(HSL.x);
    float C = (1.0 - abs(2.0 * HSL.z - 1.0)) * HSL.y;
    return (RGB - 0.5) * C + HSL.z;
}

void main() {
    vec4 diffuseColor = texture2D( map, vUv );
    gl_FragColor = vec4( diffuseColor.xyz * HSLtoRGB(vec3(vScale/5.0, 1.0, 0.5)), diffuseColor.w );

    if ( diffuseColor.w < 0.5 ) discard;
}
`;

/** Port of Three.js examples/webgl_buffergeometry_instancing_billboards.html, without the Stats panel. */
export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(50, 1, 1, 5000);
    camera.position.z = 1400;
    const scene = new Scene();

    const circleGeometry = new CircleGeometry(1, 6);
    const geometry = new InstancedBufferGeometry();
    // Three.js shares the attributes object; Threekt shares the same attributes and index by reference.
    geometry.setIndex(circleGeometry.index);
    for (const name of circleGeometry.attributeNames) geometry.setAttribute(name, circleGeometry.getAttribute(name)!);

    const particleCount = 75000;
    const translateArray = new Float32Array(particleCount * 3);
    for (let i = 0; i < translateArray.length; i++) translateArray[i] = Math.random() * 2 - 1;
    geometry.setAttribute('translate', new InstancedBufferAttribute(translateArray, 3));

    const time = new Uniform(0.0);
    const material = new RawShaderMaterial(vertexShader, fragmentShader);
    // TextureLoader leaves untagged textures without color conversion; ImageTexture defaults to sRGB.
    const map = await loadImageTexture(circleUrl);
    map.colorSpace = ColorSpace.None;
    material.uniforms.set('map', new Uniform(map));
    material.uniforms.set('time', time);

    const mesh = new Mesh(geometry, material);
    mesh.scale.set(500, 500, 500);
    scene.add(mesh);
    const rotation = new Euler();

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const t = seconds * 1000 * 0.0005;
            time.value = t;
            mesh.quaternion.setFromEuler(rotation.set(t * 0.2, t * 0.4, 0));
            renderer.render(scene, camera);
        },
        dispose() {
            renderer.dispose();
        },
    };
}
