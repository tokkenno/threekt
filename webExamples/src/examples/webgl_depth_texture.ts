import {
    Color, DomInputSurface, InstancedMesh, MeshBasicMaterial, Object3D, OrbitControls, PerspectiveCamera, Scene,
    TorusKnotGeometry, Uniform, Uniforms, WebGL2Context, WebGLRenderer, WebGLRenderTarget,
} from 'threekt';
import { fitPerspective } from './fitPerspective';
import { fullscreenPass } from './fullscreenPass';

export function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(70, 1, 0.01, 50);
    camera.position.z = 4;
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    const scene = new Scene();
    const mesh = new InstancedMesh(new TorusKnotGeometry(1, 0.3, 128, 64), new MeshBasicMaterial(Color.fromHex(0x0000ff)), 50);
    const dummy = new Object3D();
    for (let i = 0; i < mesh.count; i++) {
        const angle = Math.random() * 2 * Math.PI;
        const z = Math.random() * 2 - 1;
        const radius = 5 * Math.sqrt(1 - z * z);
        dummy.position.set(Math.cos(angle) * radius, Math.sin(angle) * radius, z * 5);
        dummy.rotation.set(Math.random(), Math.random(), Math.random());
        dummy.updateMatrix();
        mesh.setMatrixAt(i, dummy.matrix);
    }
    scene.add(mesh);
    const target = new WebGLRenderTarget(1, 1, undefined, true, true);
    const depth = fullscreenPass(`
        uniform sampler2D tDepth;
        uniform float cameraNear, cameraFar;
        void main() {
            float z = texture2D(tDepth, vUv).r;
            float viewZ = (cameraNear * cameraFar) / ((cameraFar - cameraNear) * z - cameraFar);
            float depth = (viewZ + cameraNear) / (cameraNear - cameraFar);
            gl_FragColor = vec4(vec3(1.0 - depth), 1.0);
        }
    `, new Uniforms().set('tDepth', new Uniform(target.depthTexture))
        .set('cameraNear', new Uniform(camera.near)).set('cameraFar', new Uniform(camera.far)));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            target.setSize(canvas.width, canvas.height);
            renderer.setRenderTarget(target);
            renderer.render(scene, camera);
            renderer.setRenderTarget(null);
            renderer.render(depth.scene, depth.camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); },
    };
}
