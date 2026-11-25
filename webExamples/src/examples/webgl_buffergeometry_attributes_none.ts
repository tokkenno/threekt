import {
    BufferGeometry, Color, Euler, Fog, GLSLVersion, Mesh, PerspectiveCamera, RawShaderMaterial, Scene, Side, Uniform,
    WebGL2Context, WebGLRenderer,
} from 'threekt';
import { fitPerspective } from './fitPerspective';

const vertexShader = `
uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;

uniform float seed;

const uint ieeeMantissa = 0x007FFFFFu;
const uint ieeeOne = 0x3F800000u;

uint hash(uint x) {
    x += ( x << 10u );
    x ^= ( x >>  6u );
    x += ( x <<  3u );
    x ^= ( x >> 11u );
    x += ( x << 15u );
    return x;
}

uint hash(uvec2 v) { return hash( v.x ^ hash(v.y) ); }

float hashNoise(vec2 xy) {
    uint m = hash(floatBitsToUint(xy));

    m &= ieeeMantissa;
    m |= ieeeOne;

    return uintBitsToFloat( m ) - 1.0;
}

float pseudoRandom(float lower, float delta, in vec2 xy) {
    return lower + delta*hashNoise(xy);
}

vec3 pseudoRandomVec3(float lower, float upper, int index) {
    float delta = upper - lower;
    float x = pseudoRandom(lower, delta, vec2(index, 0));
    float y = pseudoRandom(lower, delta, vec2(index, 1));
    float z = pseudoRandom(lower, delta, vec2(index, 2));
    return vec3(x, y, z);
}

out vec3 vColor;

void main() {
    const float scale = 1.0/64.0;
    vec3 position = pseudoRandomVec3(-1.0, +1.0, gl_VertexID/3) + scale * pseudoRandomVec3(-1.0, +1.0, gl_VertexID);
    vec3 color = pseudoRandomVec3(0.25, 1.0, gl_VertexID/3);
    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0 );
    vColor = color;
}
`;

const fragmentShader = `
precision mediump float;

in vec3 vColor;

out vec4 fColor;

void main() {
    fColor = vec4(vColor, 1);
}
`;

/**
 * Port of Three.js examples/webgl_buffergeometry_attributes_none.html. The geometry has no attributes:
 * the vertex shader derives every position and color from gl_VertexID. Threekt does not frustum-cull
 * yet, so the upstream `frustumCulled = false` is not needed.
 */
export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(27, 1, 1, 3500);
    camera.position.z = 4;

    const scene = new Scene();
    scene.background = Color.fromHex(0x050505);
    scene.fog = new Fog(Color.fromHex(0x050505), 2000, 3500);

    const triangleCount = 10000;
    const vertexCountPerTriangle = 3;
    const vertexCount = triangleCount * vertexCountPerTriangle;

    const geometry = new BufferGeometry();
    geometry.setDrawRange(0, vertexCount);

    const material = new RawShaderMaterial(vertexShader, fragmentShader);
    material.uniforms.set('seed', new Uniform(42));
    material.side = Side.Double;
    material.glslVersion = GLSLVersion.GLSL3;

    const mesh = new Mesh(geometry, material);
    scene.add(mesh);
    const rotation = new Euler();

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            mesh.quaternion.setFromEuler(rotation.set(seconds * 0.25, seconds * 0.5, 0));
            renderer.render(scene, camera);
        },
        dispose() {
            renderer.dispose();
        },
    };
}
