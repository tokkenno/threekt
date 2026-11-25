import {
    BufferGeometry, Color, Euler, Float32BufferAttribute, Mesh, PerspectiveCamera, RawShaderMaterial, Scene, Side,
    Uint8BufferAttribute, Uniform, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { fitPerspective } from './fitPerspective';

const vertexShader = `
precision mediump float;
precision mediump int;

uniform mat4 modelViewMatrix; // optional
uniform mat4 projectionMatrix; // optional

attribute vec3 position;
attribute vec4 color;

varying vec3 vPosition;
varying vec4 vColor;

void main() {
    vPosition = position;
    vColor = color;
    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
}
`;

const fragmentShader = `
precision mediump float;
precision mediump int;

uniform float time;

varying vec3 vPosition;
varying vec4 vColor;

void main() {
    vec4 color = vec4(vColor);
    color.r += sin(vPosition.x * 10.0 + time) * 0.5;
    gl_FragColor = color;
}
`;

/** Port of Three.js examples/webgl_buffergeometry_rawshader.html, without the Stats panel. */
export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(50, 1, 1, 10);
    camera.position.z = 2;

    const scene = new Scene();
    scene.background = Color.fromHex(0x101010);

    // Number of triangles times three vertices per triangle.
    const vertexCount = 200 * 3;
    const positions = new Float32Array(vertexCount * 3);
    // Int8Array stores 0–255 as the same unsigned bytes that Uint8BufferAttribute reads.
    const colors = new Int8Array(vertexCount * 4);
    for (let i = 0; i < positions.length; i++) positions[i] = Math.random() - 0.5;
    for (let i = 0; i < colors.length; i++) colors[i] = Math.random() * 255;

    const geometry = new BufferGeometry();
    geometry.setAttribute('position', new Float32BufferAttribute(positions, 3));
    // Normalized bytes map to 0.0–1.0 in the shader.
    geometry.setAttribute('color', new Uint8BufferAttribute(colors, 4, true));

    const time = new Uniform(1.0);
    const material = new RawShaderMaterial(vertexShader, fragmentShader);
    material.uniforms.set('time', time);
    material.side = Side.Double;
    material.transparent = true;

    const mesh = new Mesh(geometry, material);
    scene.add(mesh);
    const rotation = new Euler();

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const milliseconds = seconds * 1000;
            mesh.quaternion.setFromEuler(rotation.set(0, milliseconds * 0.0005, 0));
            time.value = milliseconds * 0.005;
            renderer.render(scene, camera);
        },
        dispose() {
            renderer.dispose();
        },
    };
}
