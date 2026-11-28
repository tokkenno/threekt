import { Color, Mesh, MeshBasicMaterial, PerspectiveCamera, Scene, Side, Uniform, Uniforms, Vector3 } from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { SphereGeometry } from 'threekt/geometry';
import { ShaderMaterial, WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { FontLoader, TextGeometry } from 'threekt/loaders';
import fontUrl from './assets/fonts/gentilis_regular.typeface.json?url';
import { fitPerspective } from './fitPerspective';
import { encodeSrgb } from './fullscreenPass';

const vertex = `
    varying vec3 vWorld, vNormal;
    void main() {
        vWorld = (modelMatrix * vec4(position, 1.0)).xyz;
        vNormal = mat3(modelMatrix) * normal;
        gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
    }
`;

export async function createExample(canvas: HTMLCanvasElement) {
    const response = await fetch(fontUrl);
    if (!response.ok) throw new Error('Could not load Gentilis Regular.');
    const font = new FontLoader().parse(await response.text());
    const scene = new Scene();
    scene.background = Color.fromHex(0x444488);
    const geometry = new SphereGeometry(32, 32, 16);
    const lightPosition = new Vector3();
    const light = new Uniform(lightPosition);
    const outline = new ShaderMaterial(`
        void main() {
            gl_Position = projectionMatrix * modelViewMatrix * vec4(position + normal * 1.4, 1.0);
        }
    `, 'void main() { gl_FragColor = vec4(0.015, 0.012, 0.035, 1.0); }');
    outline.side = Side.Back;

    for (let a = 0; a <= 5; a++) for (let b = 0; b <= 5; b++) for (let c = 0; c <= 5; c++) {
        const diffuse = new Color(1, 1, 1).setHSL(a / 5, 0.5, c * 0.1 + 0.1).multiplyScalar(1 - b * 0.04);
        const material = new ShaderMaterial(vertex, `
            uniform vec3 diffuse, lightPosition;
            uniform float bands;
            varying vec3 vWorld, vNormal;
            ${encodeSrgb}
            void main() {
                vec3 toLight = lightPosition - vWorld;
                float coord = dot(normalize(vNormal), normalize(toLight)) * 0.5 + 0.5;
                float gradient = floor(min(coord, 0.99999) * bands) / bands;
                float light = gradient * 2.0 * clamp(1.0 - length(toLight) / 1000.0, 0.0, 1.0);
                gl_FragColor = vec4(encodeSrgb(diffuse * (0.24 + light)), 1.0);
            }
        `, new Uniforms().set('diffuse', new Uniform(diffuse)).set('bands', new Uniform(a + 2)).set('lightPosition', light));
        const sphere = new Mesh(geometry, material);
        sphere.position.set(a * 80 - 200, b * 80 - 200, c * 80 - 200);
        sphere.add(new Mesh(geometry, outline));
        scene.add(sphere);
    }

    const white = new MeshBasicMaterial(Color.fromHex(0xffffff));
    for (const [label, x, z] of [
        ['-gradientMap', -350, 0], ['+gradientMap', 350, 0], ['-diffuse', 0, -300], ['+diffuse', 0, 300],
    ] as const) {
        const parameters = Object.assign(new TextGeometry.Parameters(), { font, size: 20, depth: 1, curveSegments: 1 });
        const mesh = new Mesh(new TextGeometry(label, parameters), white);
        mesh.position.set(x, 0, z);
        scene.add(mesh);
    }
    const particle = new Mesh(new SphereGeometry(4, 8, 8), white);
    scene.add(particle);
    const camera = new PerspectiveCamera(40, 1, 1, 2500);
    camera.position.set(0, 400, 1400);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 200;
    controls.maxDistance = 2000;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            const time = seconds * 0.25;
            lightPosition.set(Math.sin(time * 7) * 300, Math.cos(time * 5) * 400, Math.cos(time * 3) * 300);
            particle.position.copyFrom(lightPosition);
            renderer.render(scene, camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); },
    };
}
