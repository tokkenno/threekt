import {
    Blending, BufferGeometry, Color, Euler, Float32BufferAttribute, PerspectiveCamera, Points,
    Scene, ShaderMaterial, Uniform, Usage, WebGL2Context, WebGLRenderer,
} from 'threekt';
import sparkUrl from './assets/spark1.png?url';
import { loadImageTexture } from './exampleControls';
import { fitPerspective } from './fitPerspective';

const vertexShader = `
attribute float size;

varying vec3 vColor;

void main() {

	vColor = color;

	vec4 mvPosition = modelViewMatrix * vec4( position, 1.0 );

	gl_PointSize = size * ( 300.0 / -mvPosition.z );

	gl_Position = projectionMatrix * mvPosition;

}
`;

const fragmentShader = `
uniform sampler2D pointTexture;

varying vec3 vColor;

void main() {

	gl_FragColor = vec4( vColor, 1.0 );

	gl_FragColor = gl_FragColor * texture2D( pointTexture, gl_PointCoord );

}
`;

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(40, 1, 1, 10000);
    camera.position.z = 300;
    const scene = new Scene();
    const sprite = await loadImageTexture(sparkUrl);
    sprite.colorSpace = null;
    const count = 100000;
    const radius = 200;
    const positions = new Float32Array(count * 3);
    const colors = new Float32Array(count * 3);
    const sizes = new Float32Array(count).fill(20);
    const color = Color.fromHex(0xffffff);

    for (let i = 0; i < count; i++) {
        const x = (Math.random() * 2 - 1) * radius;
        positions[i * 3] = x;
        positions[i * 3 + 1] = (Math.random() * 2 - 1) * radius;
        positions[i * 3 + 2] = (Math.random() * 2 - 1) * radius;
        color.setHSL(i / count, 1, 0.5);
        colors[i * 3] = color.r;
        colors[i * 3 + 1] = color.g;
        colors[i * 3 + 2] = color.b;
    }

    const size = new Float32BufferAttribute(sizes, 1).setUsage(Usage.DynamicDraw);
    const geometry = new BufferGeometry()
        .setAttribute('position', new Float32BufferAttribute(positions, 3))
        .setAttribute('color', new Float32BufferAttribute(colors, 3))
        .setAttribute('size', size);
    const material = new ShaderMaterial(vertexShader, fragmentShader);
    material.uniforms.set('pointTexture', new Uniform(sprite));
    material.vertexColors = true;
    material.blending = Blending.Additive;
    material.depthTest = false;
    material.transparent = true;

    const points = new Points(geometry, material);
    scene.add(points);
    const rotation = new Euler();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const time = seconds * 5;
            points.quaternion.setFromEuler(rotation.set(0, 0, 0.01 * time));
            for (let i = 0; i < count; i++) sizes[i] = 10 * (1 + Math.sin(0.1 * i + time));
            size.markUpdated();
            renderer.render(scene, camera);
        },
        dispose() {
            renderer.dispose();
        },
    };
}
