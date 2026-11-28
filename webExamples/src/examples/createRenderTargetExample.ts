import {
    Color, DirectionalLight, Mesh, MeshBasicMaterial, MeshPhongMaterial, OrthographicCamera, PerspectiveCamera,
    Scene, SphereGeometry, TorusGeometry, Uniform, Uniforms,
} from 'threekt';
import { RenderTargetType, WebGL2Context, WebGLRenderer, WebGLRenderTarget } from 'threekt/render-gl';
import { examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';
import { followPointer } from './followPointer';
import { fullscreenPass, texturePass } from './fullscreenPass';

export function createRenderTargetExample(canvas: HTMLCanvasElement, readback: boolean) {
    const target = new WebGLRenderTarget(1, 1, readback ? RenderTargetType.Float : RenderTargetType.UnsignedByte);
    const renderCamera = new OrthographicCamera(-400, 400, 300, -300, 1, 1000);
    renderCamera.position.z = 500;
    const targetScene = new Scene();
    const time = new Uniform(0);
    const background = fullscreenPass(`
        uniform float time;
        void main() {
            gl_FragColor = vec4(vUv.y < 0.5 ? 0.0 : vUv.x, vUv.x < 0.5 ? 0.0 : vUv.y, time, 1.0);
        }
    `, new Uniforms().set('time', time));
    targetScene.add(background.mesh);
    background.mesh.renderOrder = -1;
    for (const [hex, intensity, z] of [[0xffffff, 3, 1], [0xffd5d5, 4.5, -1]]) {
        const light = new DirectionalLight(Color.fromHex(hex), intensity);
        light.position.set(0, 0, z);
        targetScene.add(light);
    }
    const rings = new TorusGeometry(100, 25, 15, 30);
    const toruses = [[0x9c9c9c, 0xffaa00, 0, 1.5], [0x9c0000, 0xff2200, 150, 0.75]].map(([hex, specular, y, scale]) => {
        const material = new MeshPhongMaterial(Color.fromHex(hex));
        material.specular.setHex(specular);
        material.shininess = 5;
        const mesh = new Mesh(rings, material);
        mesh.position.set(0, y, 100);
        mesh.scale.setScalar(scale);
        targetScene.add(mesh);
        return mesh;
    });

    const display = texturePass(target.texture);
    const camera = new PerspectiveCamera(30, 1, 1, 10000);
    camera.position.z = 220;
    const scene = new Scene();
    if (!readback) {
        const material = new MeshBasicMaterial();
        material.map = target.texture;
        const geometry = new SphereGeometry(10, 64, 32);
        for (let y = -2; y <= 2; y++) {
            for (let x = -2; x <= 2; x++) {
                const sphere = new Mesh(geometry, material);
                sphere.position.set(x * 20, y * 20, 0);
                sphere.rotation.y = -Math.PI / 2;
                scene.add(sphere);
            }
        }
    }
    const pointer = !readback ? followPointer(canvas) : null;
    const panel = readback ? examplePanel(canvas) : null;
    let u = 0.5;
    let v = 0.5;
    const position = (event: PointerEvent) => {
        const bounds = canvas.getBoundingClientRect();
        u = (event.clientX - bounds.left) / bounds.width;
        v = 1 - (event.clientY - bounds.top) / bounds.height;
    };
    if (readback) canvas.addEventListener('pointermove', position);
    const pixel = new Float32Array(4);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    renderer.autoClear = false;
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            target.setSize(canvas.width, canvas.height);
            renderCamera.left = -canvas.clientWidth / 2;
            renderCamera.right = -renderCamera.left;
            renderCamera.top = canvas.clientHeight / 2;
            renderCamera.bottom = -renderCamera.top;
            renderCamera.updateProjectionMatrix();
            toruses[0].rotation.y = -seconds * 1.5;
            toruses[1].rotation.y = toruses[0].rotation.y + Math.PI / 2;
            time.value = 1 - Math.abs((seconds * 0.6) % 2 - 1);

            renderer.setRenderTarget(target);
            renderer.clear();
            renderer.render(targetScene, renderCamera);
            renderer.setRenderTarget(null);
            renderer.clear();
            renderer.render(display.scene, display.camera);
            if (panel) {
                const x = Math.max(0, Math.min(target.width - 1, Math.floor(u * target.width)));
                const y = Math.max(0, Math.min(target.height - 1, Math.floor(v * target.height)));
                renderer.readRenderTargetPixels(target, x, y, 1, 1, pixel);
                panel.textContent = `Linear RGB · ${Array.from(pixel.subarray(0, 3), x => x.toFixed(4)).join(', ')}`;
            } else {
                pointer?.update(camera, scene.position);
                renderer.render(scene, camera);
            }
        },
        dispose() {
            if (readback) canvas.removeEventListener('pointermove', position);
            panel?.remove();
            pointer?.dispose();
            renderer.dispose();
        },
    };
}
