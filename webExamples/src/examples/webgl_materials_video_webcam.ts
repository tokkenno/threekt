import {
    Color, DomInputSurface, Mesh, MeshBasicMaterial, OrbitControls, PerspectiveCamera, PlaneGeometry,
    Scene, VideoTexture,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const video = document.createElement('video');
    video.muted = video.playsInline = true;
    const texture = new VideoTexture(video);
    const material = new MeshBasicMaterial(Color.fromHex(0xffffff), texture);
    const geometry = new PlaneGeometry(8, 4.5);
    const camera = new PerspectiveCamera(60, 1, 0.1, 100);
    camera.position.z = 0.01;
    const scene = new Scene();
    for (let i = 1; i <= 128; i++) {
        const phi = Math.acos(-1 + 2 * i / 128);
        const mesh = new Mesh(geometry, material);
        mesh.position.setFromSphericalCoords(32, phi, Math.sqrt(128 * Math.PI) * phi);
        mesh.lookAt(camera.position);
        scene.add(mesh);
    }
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.enableZoom = false;
    controls.enablePan = false;

    const panel = document.createElement('aside');
    panel.className = 'example-panel';
    const start = document.createElement('button');
    start.textContent = 'Start webcam';
    const status = document.createElement('output');
    panel.append(start, status);
    canvas.parentElement?.append(panel);

    let stream: MediaStream | undefined;
    let disposed = false;
    start.addEventListener('click', async () => {
        start.disabled = true;
        status.textContent = '';
        try {
            if (!navigator.mediaDevices) throw new Error('The webcam requires HTTPS or localhost.');
            const candidate = await navigator.mediaDevices.getUserMedia({
                video: { width: 1280, height: 720, facingMode: 'user' },
            });
            if (disposed) {
                candidate.getTracks().forEach(track => track.stop());
                return;
            }
            stream = candidate;
            video.srcObject = stream;
            await video.play();
            if (!disposed) panel.remove();
        } catch (error) {
            stream?.getTracks().forEach(track => track.stop());
            stream = undefined;
            video.srcObject = null;
            if (!disposed) {
                status.textContent = `Could not start the webcam. ${String(error)}`;
                start.disabled = false;
            }
        }
    });

    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer) || video.readyState < video.HAVE_CURRENT_DATA) return;
            texture.update();
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() {
            disposed = true;
            panel.remove();
            stream?.getTracks().forEach(track => track.stop());
            video.pause();
            video.srcObject = null;
            controls.dispose();
            renderer.dispose();
        },
    };
}
