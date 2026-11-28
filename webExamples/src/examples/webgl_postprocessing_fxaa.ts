import {
    Color, DirectionalLight, HemisphereLight, InstancedMesh, MeshPhongMaterial, Object3D, PerspectiveCamera,
    Scene, Uniform, Uniforms, Vector2,
} from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { TetrahedronGeometry } from 'threekt/geometry';
import { WebGL2Context, WebGLRenderer, WebGLRenderTarget } from 'threekt/render-gl';
import { fitPerspective } from './fitPerspective';
import { encodeSrgb, fullscreenPass } from './fullscreenPass';
import fxaa from './shaders/FXAAShader.glsl?raw';

export function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    scene.add(new HemisphereLight(Color.fromHex(0xffffff), Color.fromHex(0x8d8d8d)));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 3);
    light.position.set(1, 1, 1);
    scene.add(light);
    const material = new MeshPhongMaterial(Color.fromHex(0xf73232));
    material.flatShading = true;
    const mesh = new InstancedMesh(new TetrahedronGeometry(10), material, 100);
    const dummy = new Object3D();
    for (let i = 0; i < mesh.count; i++) {
        dummy.position.set((Math.random() - 0.5) * 450, (Math.random() - 0.5) * 350, (Math.random() - 0.5) * 400);
        dummy.rotation.set(Math.random() * Math.PI, Math.random() * Math.PI, Math.random() * Math.PI);
        dummy.scale.setScalar(0.5 + Math.random() * 1.5);
        dummy.updateMatrix();
        mesh.setMatrixAt(i, dummy.matrix);
    }
    scene.add(mesh);
    const camera = new PerspectiveCamera(45, 1, 1, 2000);
    camera.position.z = 650;
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const target = new WebGLRenderTarget();
    const resolution = new Vector2();
    const pass = fullscreenPass(`
        ${encodeSrgb}
        #define main fxaaMain
        ${fxaa}
        #undef main
        void main() {
            if (vUv.x < 0.5) gl_FragColor = texture2D(tDiffuse, vUv);
            else fxaaMain();
            gl_FragColor.rgb = encodeSrgb(gl_FragColor.rgb);
            if (abs(vUv.x - 0.5) < resolution.x) gl_FragColor = vec4(0.75, 0.75, 0.75, 1.0);
        }
    `, new Uniforms().set('tDiffuse', new Uniform(target.texture)).set('resolution', new Uniform(resolution)));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            target.setSize(canvas.width, canvas.height);
            resolution.set(1 / canvas.width, 1 / canvas.height);
            renderer.setRenderTarget(target);
            renderer.render(scene, camera);
            renderer.setRenderTarget(null);
            renderer.render(pass.scene, pass.camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); },
    };
}
