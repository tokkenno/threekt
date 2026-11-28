import {
    AmbientLight, Color, DirectionalLight, Mesh, MeshLambertMaterial, PerspectiveCamera, Scene, Timer,
} from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { PlaneGeometry, SphereGeometry } from 'threekt/geometry';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { AudioListener, AudioLoader, PositionalAudio } from 'threekt/audio';
import ping from './assets/sounds/ping_pong.mp3?url';
import { exampleAudio } from './exampleAudio';
import { fitPerspective } from './fitPerspective';

export function createExample(canvas: HTMLCanvasElement) {
    const speed = 2.5;
    const height = 3;
    const offset = 0.5;
    const camera = new PerspectiveCamera(45, 1, 0.1, 100);
    camera.position.set(7, 3, 7);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 1;
    controls.maxDistance = 25;
    const scene = new Scene();
    scene.background = Color.fromHex(0x202736);
    scene.add(new AmbientLight(Color.fromHex(0xcccccc)));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 2.5);
    light.position.set(0, 5, 5);
    scene.add(light);
    const floor = new Mesh(new PlaneGeometry(10, 10), new MeshLambertMaterial(Color.fromHex(0x4676b6)));
    floor.rotation.x = -Math.PI / 2;
    scene.add(floor);
    const geometry = new SphereGeometry(0.3, 32, 16);
    geometry.translate(0, 0.3, 0);
    const material = new MeshLambertMaterial(Color.fromHex(0xcccccc));
    const balls = Array.from({ length: 5 }, (_, index) => {
        const ball = new Mesh(geometry, material);
        const angle = index / 5 * Math.PI * 2;
        ball.position.set(3 * Math.cos(angle), 0, 3 * Math.sin(angle));
        scene.add(ball);
        return { ball, down: false, audio: undefined as PositionalAudio | undefined };
    });
    const audio = exampleAudio(canvas, async () => {
        const listener = new AudioListener();
        camera.add(listener);
        const buffer = await new AudioLoader().loadAsync(ping);
        for (const entry of balls) {
            entry.audio = new PositionalAudio(listener);
            entry.audio.setBuffer(buffer);
            entry.ball.add(entry.audio);
        }
    });
    const timer = new Timer();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            timer.update();
            const time = timer.getElapsed();
            balls.forEach((entry, i) => {
                const previousHeight = entry.ball.position.y;
                entry.ball.position.y = Math.abs(Math.sin(i * offset + time * speed) * height);
                if (entry.ball.position.y < previousHeight) {
                    entry.down = true;
                } else if (entry.down) {
                    // Play the sound with perfect timing when the ball hits the surface.
                    if (audio.running) entry.audio?.play();
                    entry.down = false;
                }
            });
            renderer.render(scene, camera);
        },
        dispose() {
            audio.dispose();
            controls.dispose();
            renderer.dispose();
        },
    };
}
