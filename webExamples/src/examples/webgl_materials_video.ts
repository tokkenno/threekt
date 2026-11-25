import {
    Box, Color, ColorSpace, DirectionalLight, Euler, Float32BufferAttribute, Mesh, MeshPhongMaterial, PerspectiveCamera,
    Scene, VideoTexture, WebGL2Context, WebGLRenderer,
} from 'threekt';
// Sintel, Blender Foundation, from the Three.js example.
import videoUrl from './assets/sintel.webm?url';
import { followPointer } from './exampleControls';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const video = document.createElement('video');
    video.loop = video.muted = video.playsInline = true;
    video.src = videoUrl;
    await video.play();
    video.currentTime = 3;
    const texture = new VideoTexture(video);
    texture.colorSpace = ColorSpace.SRGB;
    const scene = new Scene();
    const camera = new PerspectiveCamera(40, 1, 1, 10000);
    camera.position.z = 500;
    const light = new DirectionalLight(Color.fromHex(0xffffff), 3);
    light.position.set(0.5, 1, 1);
    scene.add(light);
    const columns = 20, rows = 10;
    const width = 480 / columns, height = 204 / rows;
    const tiles: { mesh: Mesh; material: MeshPhongMaterial; startX: number; startY: number;
        dx: number; dy: number; hue: number; saturation: number }[] = [];
    const rotation = new Euler();
    for (let x = 0; x < columns; x++) {
        for (let y = 0; y < rows; y++) {
            const geometry = new Box(width, height, width);
            const uv = (geometry.getAttribute('uv') as Float32BufferAttribute).array;
            for (let i = 0; i < uv.length; i += 2) {
                uv[i] = (uv[i] + x) / columns;
                uv[i + 1] = (uv[i + 1] + y) / rows;
            }
            const material = new MeshPhongMaterial();
            material.shininess = 0;
            material.map = texture;
            const mesh = new Mesh(geometry, material);
            const startX = (x - (columns - 1) / 2) * width, startY = (y - (rows - 1) / 2) * height;
            const dx = 0.5 - Math.random(), dy = 0.5 - Math.random();
            scene.add(mesh);
            tiles.push({ mesh, material, startX, startY, dx, dy, hue: x / columns, saturation: 1 - y / rows });
        }
    }
    const pointer = followPointer(canvas, camera, scene);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    let start: number | undefined;
    return {
        render(seconds: number) {
            start ??= seconds;
            const elapsed = seconds - start;
            const phase = elapsed % 32;
            const progress = Math.max(0, Math.min(phase - 3, 32 - phase)) * 0.06;
            for (const { mesh, material, startX, startY, dx, dy, hue, saturation } of tiles) {
                material.color.setHSL((hue + elapsed * 0.05) % 1, saturation, 0.5);
                mesh.quaternion.setFromEuler(rotation.set(progress * dx * 9, progress * dy * 9, 0));
                mesh.position.set(startX - progress * dx * 135, startY + progress * dy * 135, progress * dx * 270);
            }
            texture.update();
            pointer.update();
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() {
            video.pause();
            video.removeAttribute('src');
            video.load();
            pointer.dispose();
            renderer.dispose();
        },
    };
}
