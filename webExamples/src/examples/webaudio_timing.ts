import { AmbientLight, Color, DirectionalLight, DomInputSurface, Mesh, MeshLambertMaterial, OrbitControls,
    PerspectiveCamera, PlaneGeometry, Scene, SphereGeometry, WebGL2Context, WebGLRenderer } from 'threekt';
import ping from './assets/sounds/ping_pong.mp3?url';
import { exampleAudio, loadAudioBuffer, playAudioBuffer, spatialAudio } from './exampleAudio';
import { fitPerspective } from './fitPerspective';

export function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(45, 1, 0.1, 100);
    camera.position.set(7, 3, 7);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
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
    const material = new MeshLambertMaterial(Color.fromHex(0xcccccc));
    const balls = Array.from({ length: 5 }, (_, index) => {
        const node = new Mesh(geometry, material);
        const angle = index * Math.PI * 2 / 5;
        node.position.set(3 * Math.cos(angle), 0.3, 3 * Math.sin(angle));
        scene.add(node);
        return { node, phase: index * 0.5, next: 0, panner: undefined as PannerNode | undefined };
    });
    const spatial = spatialAudio();
    const speed = 2.5;
    let start = 0;
    let sample: AudioBuffer | undefined;
    const audio = exampleAudio(canvas, async context => {
        sample = await loadAudioBuffer(context, ping);
        if (context.state === 'closed') return;
        start = context.currentTime + 0.08;
        for (const ball of balls) {
            ball.panner = new PannerNode(context, { refDistance: 4, panningModel: 'HRTF' });
            ball.panner.connect(context.destination);
            spatial.updatePanner(ball.panner, ball.node);
            ball.next = start + ((Math.PI - ball.phase) % Math.PI) / speed;
        }
    });
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            const context = audio.context;
            const time = context ? Math.max(0, context.currentTime - start) : seconds;
            if (context?.state === 'running') spatial.updateListener(context, camera);
            for (const ball of balls) {
                ball.node.position.y = 0.3 + Math.abs(Math.sin(time * speed + ball.phase)) * 3;
                if (!sample || !ball.panner || context?.state !== 'running') continue;
                while (ball.next < context.currentTime - 0.04) ball.next += Math.PI / speed;
                if (ball.next <= context.currentTime + 0.12) {
                    playAudioBuffer(context, sample, ball.panner, false, ball.next);
                    ball.next += Math.PI / speed;
                }
            }
            renderer.render(scene, camera);
        },
        dispose() { audio.dispose(); controls.dispose(); renderer.dispose(); },
    };
}
