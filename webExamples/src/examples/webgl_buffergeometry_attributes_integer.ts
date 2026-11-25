import {
    BufferGeometry, Color, Euler, Float32BufferAttribute, Fog, GLSLVersion, GpuType, Int16BufferAttribute,
    Mesh, PerspectiveCamera, Scene, ShaderMaterial, Side, Uniform, WebGL2Context, WebGLRenderer,
} from 'threekt';
import crateUrl from './assets/crate.gif?url';
import checkerUrl from './assets/FloorsCheckerboard_S_Diffuse.jpg?url';
import grassUrl from './assets/grasslight-big.jpg?url';
import { loadImageTexture } from './exampleControls';
import { fitPerspective } from './fitPerspective';

const vertexShader = `
in int textureIndex;
flat out int vIndex;
out vec2 vUv;

void main() {
    vIndex = textureIndex;
    vUv = uv;
    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
}
`;

const fragmentShader = `
flat in int vIndex;
in vec2 vUv;
uniform sampler2D uTextures[3];
out vec4 outColor;

void main() {
    if (vIndex == 0) outColor = texture(uTextures[0], vUv);
    else if (vIndex == 1) outColor = texture(uTextures[1], vUv);
    else outColor = texture(uTextures[2], vUv);
}
`;

export async function createExample(canvas: HTMLCanvasElement) {
    const textures = await Promise.all([crateUrl, checkerUrl, grassUrl].map(loadImageTexture));
    for (const texture of textures) texture.colorSpace = null;

    const camera = new PerspectiveCamera(27, 1, 1, 3500);
    camera.position.z = 2500;
    const scene = new Scene();
    scene.background = Color.fromHex(0x050505);
    scene.fog = new Fog(Color.fromHex(0x050505), 2000, 3500);
    const triangles = 10000;
    const positions = new Float32Array(triangles * 9);
    const uvs = new Float32Array(triangles * 6);
    const textureIndices = new Int16Array(triangles * 3);

    for (let triangle = 0; triangle < triangles; triangle++) {
        const x = Math.random() * 800 - 400;
        const y = Math.random() * 800 - 400;
        const z = Math.random() * 800 - 400;
        for (let vertex = 0; vertex < 3; vertex++) {
            const offset = triangle * 9 + vertex * 3;
            positions[offset] = x + Math.random() * 50 - 25;
            positions[offset + 1] = y + Math.random() * 50 - 25;
            positions[offset + 2] = z + Math.random() * 50 - 25;
            textureIndices[triangle * 3 + vertex] = triangle % textures.length;
        }
        uvs.set([0, 0, 0.5, 1, 1, 0], triangle * 6);
    }

    const textureIndex = new Int16BufferAttribute(textureIndices, 1);
    textureIndex.gpuType = GpuType.Int;
    const geometry = new BufferGeometry()
        .setAttribute('position', new Float32BufferAttribute(positions, 3))
        .setAttribute('uv', new Float32BufferAttribute(uvs, 2))
        .setAttribute('textureIndex', textureIndex);
    geometry.computeBoundingSphere();

    const material = new ShaderMaterial(vertexShader, fragmentShader);
    material.uniforms.set('uTextures', new Uniform(textures));
    material.glslVersion = GLSLVersion.GLSL3;
    material.side = Side.Double;
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
        dispose() { renderer.dispose(); },
    };
}
