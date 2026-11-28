import {
    BufferGeometry, Color, ColorSpace, Euler, Float32BufferAttribute, Fog, GpuType,
    ImageTexture, Int16BufferAttribute, Mesh, PerspectiveCamera, Scene, Side,
    Uniform, Uniforms,
} from 'threekt';
import { GLSLVersion, ShaderMaterial, WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import crateUrl from './assets/crate.gif?url';
import checkerboardUrl from './assets/FloorsCheckerboard_S_Diffuse.jpg?url';
import grassUrl from './assets/grasslight-big.jpg?url';
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
        else if (vIndex == 2) outColor = texture(uTextures[2], vUv);
    }
`;

/** Port of Three.js examples/webgl_buffergeometry_attributes_integer.html. */
export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(27, 1, 1, 3500);
    camera.position.z = 2500;
    const scene = new Scene();
    scene.background = Color.fromHex(0x050505);
    scene.fog = new Fog(Color.fromHex(0x050505), 2000, 3500);

    const triangles = 10000;
    const positions = new Float32Array(triangles * 9);
    const uvs = new Float32Array(triangles * 6);
    const textureIndices = new Int16Array(triangles * 3);
    const triangleUvs = [0, 0, 0.5, 1, 1, 0];

    const n = 800, n2 = n / 2;
    const d = 50, d2 = d / 2;
    for (let i = 0; i < triangles; i++) {
        const x = Math.random() * n - n2;
        const y = Math.random() * n - n2;
        const z = Math.random() * n - n2;

        for (let vertex = 0; vertex < 3; vertex++) {
            const offset = i * 9 + vertex * 3;
            positions[offset] = x + Math.random() * d - d2;
            positions[offset + 1] = y + Math.random() * d - d2;
            positions[offset + 2] = z + Math.random() * d - d2;
            textureIndices[i * 3 + vertex] = i % 3;
        }
        uvs.set(triangleUvs, i * 6);
    }

    const textureIndex = new Int16BufferAttribute(textureIndices, 1);
    textureIndex.gpuType = GpuType.Int;
    const geometry = new BufferGeometry()
        .setAttribute('position', new Float32BufferAttribute(positions, 3))
        .setAttribute('uv', new Float32BufferAttribute(uvs, 2))
        .setAttribute('textureIndex', textureIndex);
    geometry.computeBoundingSphere();

    const textures = await Promise.all([crateUrl, checkerboardUrl, grassUrl].map(async (url) => {
        const image = new Image();
        image.src = url;
        await image.decode();
        const texture = new ImageTexture(image);
        texture.colorSpace = ColorSpace.None;
        return texture;
    }));

    const uniforms = new Uniforms().set('uTextures', new Uniform(textures));
    const material = new ShaderMaterial(vertexShader, fragmentShader, uniforms);
    material.side = Side.Double;
    material.glslVersion = GLSLVersion.GLSL3;

    const mesh = new Mesh(geometry, material);
    scene.add(mesh);
    const rotation = new Euler();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));

    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const time = Date.now() * 0.001;
            mesh.quaternion.setFromEuler(rotation.set(time * 0.25, time * 0.5, 0));
            renderer.render(scene, camera);
        },
        dispose() {
            renderer.dispose();
        },
    };
}
