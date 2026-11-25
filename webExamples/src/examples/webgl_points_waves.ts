import {
    BufferGeometry, Color, Float32BufferAttribute, PerspectiveCamera, Points, Scene, ShaderMaterial,
    Uniform, Usage, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { followPointer } from './exampleControls';
import { fitPerspective } from './fitPerspective';

const vertexShader = `
attribute float scale;

void main() {

	vec4 mvPosition = modelViewMatrix * vec4( position, 1.0 );

	gl_PointSize = scale * ( 300.0 / - mvPosition.z );

	gl_Position = projectionMatrix * mvPosition;

}
`;

const fragmentShader = `
uniform vec3 color;

void main() {

	if ( length( gl_PointCoord - vec2( 0.5, 0.5 ) ) > 0.475 ) discard;

	gl_FragColor = vec4( color, 1.0 );

}
`;

export function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(75, 1, 1, 10000);
    camera.position.z = 1000;
    const scene = new Scene();
    const separation = 100;
    const amountX = 50;
    const amountY = 50;
    const positions = new Float32Array(amountX * amountY * 3);
    const scales = new Float32Array(amountX * amountY).fill(1);
    for (let x = 0; x < amountX; x++) {
        for (let y = 0; y < amountY; y++) {
            const i = (x * amountY + y) * 3;
            positions[i] = (x - amountX / 2) * separation;
            positions[i + 2] = (y - amountY / 2) * separation;
        }
    }
    const position = new Float32BufferAttribute(positions, 3).setUsage(Usage.DynamicDraw);
    const scale = new Float32BufferAttribute(scales, 1).setUsage(Usage.DynamicDraw);
    const geometry = new BufferGeometry().setAttribute('position', position).setAttribute('scale', scale);
    const material = new ShaderMaterial(vertexShader, fragmentShader);
    material.uniforms.set('color', new Uniform(Color.fromHex(0xffffff)));
    scene.add(new Points(geometry, material));

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const pointer = followPointer(canvas, camera, scene);
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            pointer.update();
            const count = seconds * 6;
            for (let x = 0; x < amountX; x++) {
                const waveX = Math.sin((x + count) * 0.3);
                for (let y = 0; y < amountY; y++) {
                    const waveY = Math.sin((y + count) * 0.5);
                    const i = x * amountY + y;
                    positions[i * 3 + 1] = (waveX + waveY) * 50;
                    scales[i] = (waveX + waveY + 2) * 20;
                }
            }
            position.markUpdated();
            scale.markUpdated();
            renderer.render(scene, camera);
        },
        dispose() {
            pointer.dispose();
            renderer.dispose();
        },
    };
}
