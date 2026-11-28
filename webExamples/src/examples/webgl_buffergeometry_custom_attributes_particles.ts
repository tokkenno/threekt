import {
    Blending, BufferGeometry, Color, ColorSpace, Euler, Float32BufferAttribute, ImageTexture,
    PerspectiveCamera, Points, Scene, Uniform, Uniforms, Usage,
} from 'threekt';
import { ShaderMaterial, WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import sparkUrl from './assets/spark1.png?url';
import { fitPerspective } from './fitPerspective';

const vertexShader = `
    attribute float size;
    varying vec3 vColor;

    void main() {
        vColor = color;
        vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);
        gl_PointSize = size * (300.0 / -mvPosition.z);
        gl_Position = projectionMatrix * mvPosition;
    }
`;

const fragmentShader = `
    uniform sampler2D pointTexture;
    varying vec3 vColor;

    void main() {
        gl_FragColor = vec4(vColor, 1.0);
        gl_FragColor = gl_FragColor * texture2D(pointTexture, gl_PointCoord);
    }
`;

/** Port of Three.js examples/webgl_buffergeometry_custom_attributes_particles.html, without Stats. */
export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(40, 1, 1, 10000);
    camera.position.z = 300;
    const scene = new Scene();

    const image = new Image();
    image.src = sparkUrl;
    await image.decode();
    const pointTexture = new ImageTexture(image);
    pointTexture.colorSpace = ColorSpace.None;

    const uniforms = new Uniforms().set('pointTexture', new Uniform(pointTexture));
    const material = new ShaderMaterial(vertexShader, fragmentShader, uniforms);
    material.blending = Blending.Additive;
    material.depthTest = false;
    material.transparent = true;
    material.vertexColors = true;

    const particles = 100000;
    const radius = 200;
    const positions = new Float32Array(particles * 3);
    const colors = new Float32Array(particles * 3);
    const sizes = new Float32Array(particles).fill(20);
    const color = new Color(1, 1, 1);

    for (let i = 0; i < particles; i++) {
        const offset = i * 3;
        positions[offset] = (Math.random() * 2 - 1) * radius;
        positions[offset + 1] = (Math.random() * 2 - 1) * radius;
        positions[offset + 2] = (Math.random() * 2 - 1) * radius;
        color.setHSL(i / particles, 1, 0.5);
        colors[offset] = color.r;
        colors[offset + 1] = color.g;
        colors[offset + 2] = color.b;
    }

    const size = new Float32BufferAttribute(sizes, 1).setUsage(Usage.DynamicDraw);
    const geometry = new BufferGeometry()
        .setAttribute('position', new Float32BufferAttribute(positions, 3))
        .setAttribute('color', new Float32BufferAttribute(colors, 3))
        .setAttribute('size', size);
    geometry.computeBoundingSphere();

    const particleSystem = new Points(geometry, material);
    scene.add(particleSystem);
    const rotation = new Euler();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));

    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const time = Date.now() * 0.005;
            particleSystem.quaternion.setFromEuler(rotation.set(0, 0, 0.01 * time));

            for (let i = 0; i < sizes.length; i++) {
                sizes[i] = 10 * (1 + Math.sin(0.1 * i + time));
            }
            size.markUpdated();
            renderer.render(scene, camera);
        },
        dispose() {
            renderer.dispose();
        },
    };
}
