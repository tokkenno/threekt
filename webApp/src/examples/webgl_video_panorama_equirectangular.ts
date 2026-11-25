import {
    Color, DomInputSurface, Mesh, MeshBasicMaterial, OrbitControls, PerspectiveCamera, Scene, Side,
    SphereGeometry, VideoTexture, WebGL2Context, WebGLRenderer,
} from 'threekt';
import webmUrl from './assets/pano.webm?url';
import mp4Url from './assets/pano.mp4?url';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const video = document.createElement('video');
    video.muted = video.loop = video.playsInline = true;
    video.src = video.canPlayType('video/webm') ? webmUrl : mp4Url;
    await video.play();

    const texture = new VideoTexture(video);
    const material = new MeshBasicMaterial(Color.fromHex(0xffffff), texture);
    material.side = Side.Back;
    const panorama = new Mesh(new SphereGeometry(5, 60, 40), material);
    panorama.scale.x = -1;
    const scene = new Scene();
    scene.add(panorama);

    const camera = new PerspectiveCamera(75, 1, 0.25, 10);
    camera.position.x = 0.5;
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.enableZoom = false;
    controls.enablePan = false;
    controls.rotateSpeed = -0.25;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            texture.update();
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() {
            controls.dispose();
            renderer.dispose();
            video.pause();
            video.removeAttribute('src');
            video.load();
        },
    };
}
