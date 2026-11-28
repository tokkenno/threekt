import { BoxGeometry } from 'threekt/geometry';
import {
    BufferGeometry, Color, Euler, Float32BufferAttribute, PerspectiveCamera, Points, Raycaster, Scene, Uniform,
    Uniforms, Usage,
} from 'threekt';
import { ShaderMaterial, WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import discUrl from './assets/disc.png?url';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';
import { raycastPointer } from './raycastPointer';

const vertexShader = `
    attribute float size;
    attribute vec3 customColor;

    varying vec3 vColor;

    void main() {

        vColor = customColor;

        vec4 mvPosition = modelViewMatrix * vec4( position, 1.0 );

        gl_PointSize = size * ( 300.0 / -mvPosition.z );

        gl_Position = projectionMatrix * mvPosition;

    }
`;

const fragmentShader = `
    uniform vec3 color;
    uniform sampler2D pointTexture;
    uniform float alphaTest;

    varying vec3 vColor;

    void main() {

        gl_FragColor = vec4( color * vColor, 1.0 );

        gl_FragColor = gl_FragColor * texture2D( pointTexture, gl_PointCoord );

        if ( gl_FragColor.a < alphaTest ) discard;

    }
`;

export async function createExample(canvas: HTMLCanvasElement) {
    const box = new BoxGeometry(200, 200, 200, 16, 16, 16);
    const source = box.getAttribute('position')!;
    const coordinates: number[] = [];
    const unique = new Set<string>();
    for (let i = 0; i < source.count; i++) {
        const vertex = [source.getX(i), source.getY(i), source.getZ(i)];
        const key = vertex.join(',');
        if (!unique.has(key)) {
            unique.add(key);
            coordinates.push(...vertex);
        }
    }
    const count = coordinates.length / 3;
    const particleSize = 20;
    const sizes = new Float32Array(count).fill(particleSize * 0.5);
    const colors = new Float32Array(coordinates.length);
    const color = new Color(1, 1, 1);
    for (let i = 0; i < count; i++) {
        color.setHSL(0.01 + 0.1 * i / count, 1, 0.5);
        colors.set([color.r, color.g, color.b], i * 3);
    }
    const size = new Float32BufferAttribute(sizes, 1).setUsage(Usage.DynamicDraw);
    const geometry = new BufferGeometry()
        .setAttribute('position', new Float32BufferAttribute(Float32Array.from(coordinates), 3))
        .setAttribute('customColor', new Float32BufferAttribute(colors, 3))
        .setAttribute('size', size);
    const uniforms = new Uniforms().set('color', new Uniform(Color.fromHex(0xffffff)))
        .set('pointTexture', new Uniform(await loadImageTexture(discUrl)))
        .set('alphaTest', new Uniform(0.9));
    const particles = new Points(geometry, new ShaderMaterial(vertexShader, fragmentShader, uniforms));
    const scene = new Scene();
    scene.add(particles);
    const camera = new PerspectiveCamera(45, 1, 1, 10000);
    camera.position.z = 250;
    const input = raycastPointer(canvas);
    const raycaster = new Raycaster();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const rotation = new Euler();
    let current = -1;
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            particles.quaternion.setFromEuler(rotation.set(seconds * 0.03, seconds * 0.06, 0));
            raycaster.setFromCamera(input.pointer, camera);
            const id = raycaster.intersectObject(particles)[0]?.index ?? -1;
            if (current !== id) {
                if (current >= 0) sizes[current] = particleSize;
                if (id >= 0) sizes[id] = particleSize * 1.25;
                current = id;
                size.markUpdated();
            }
            renderer.render(scene, camera);
        },
        dispose() { input.dispose(); renderer.dispose(); },
    };
}
