import { Color, Mesh, PerspectiveCamera, Scene, Uniform, Uniforms } from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { ShaderMaterial, WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import model from './assets/lee-perry-smith/LeePerrySmith.glb?url';
import { loadExampleGlb } from './exampleGlb';
import { fitPerspective } from './fitPerspective';
import { readExampleGltfGeometry } from './readExampleGltf';

export async function createExample(canvas: HTMLCanvasElement) {
    const glb = await loadExampleGlb(model);
    const geometry = readExampleGltfGeometry(glb.json, glb.buffer, 0, glb.start);
    geometry.center();
    const box = geometry.boundingBox!;
    const scale = 3.6 / (box.max.y - box.min.y);
    const scene = new Scene();
    scene.background = Color.fromHex(0x16191d);
    const time = new Uniform(0);
    for (const amount of [2, -2]) {
        const material = new ShaderMaterial(`
            uniform float time, amount;
            varying vec3 n;
            void main() {
                float theta = sin(time + position.y) * amount * 0.5;
                float c = cos(theta), s = sin(theta);
                mat3 twist = mat3(c,0,s, 0,1,0, -s,0,c);
                n = normalMatrix * (normal * twist);
                gl_Position = projectionMatrix * modelViewMatrix * vec4(position * twist, 1.0);
            }
        `, `
            varying vec3 n;
            void main() { gl_FragColor = vec4(normalize(n) * 0.5 + 0.5, 1.0); }
        `, new Uniforms().set('time', time).set('amount', new Uniform(amount)));
        const mesh = new Mesh(geometry, material);
        mesh.position.x = amount * 1.12;
        mesh.scale.setScalar(scale);
        scene.add(mesh);
    }
    const camera = new PerspectiveCamera(32, 1, 0.1, 100);
    camera.position.z = 12;
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            time.value = seconds;
            renderer.render(scene, camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); },
    };
}
