import { PerspectiveCamera, Scene, Vector3 } from 'threekt';
import spriteUrl from './assets/css-sprite.png?url';
import { cssTrackball, exampleCss3d, fitCssCamera } from './exampleCss3d';

export function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    const camera = new PerspectiveCamera(75, 1, 1, 5000);
    camera.position.set(600, 400, 1500);
    const css = exampleCss3d(canvas, '#081019');
    const controls = cssTrackball(canvas, camera);
    const count = 512;
    const sprites = Array.from({ length: count }, (_, i) => {
        const image = new Image();
        image.src = spriteUrl;
        image.alt = '';
        const node = css.add(image, true);
        scene.add(node);
        const x = i % 16 * 150, z = Math.floor(i / 16) * 150;
        const random = new Vector3(Math.random() * 4000 - 2000, Math.random() * 4000 - 2000, Math.random() * 4000 - 2000);
        const phi = Math.acos(-1 + 2 * i / count), theta = Math.sqrt(count * Math.PI) * phi;
        return { node, targets: [
            new Vector3(x - 1125, (Math.sin(x * 0.5) + Math.sin(z * 0.5)) * 200, z - 2325),
            new Vector3(i % 8 * 150 - 525, Math.floor(i / 8) % 8 * 150 - 525, Math.floor(i / 64) * 150 - 525),
            random,
            new Vector3(750 * Math.cos(theta) * Math.sin(phi), 750 * Math.sin(theta) * Math.sin(phi), 750 * Math.cos(phi)),
        ] };
    });
    return {
        render(seconds: number) {
            if (!fitCssCamera(canvas, camera)) return;
            controls.update();
            const step = seconds / 6;
            const index = Math.floor(step);
            const t = Math.min(1, (step - index) * 2);
            const eased = t * t * (3 - 2 * t);
            for (const { node, targets } of sprites) {
                node.position.copyFrom(targets[(index + 3) % 4]).lerp(targets[index % 4], eased);
                node.scale.setScalar(1 + 0.3 * Math.sin(node.position.x * 0.002 + seconds * 2));
            }
            css.render(scene, camera);
        },
        dispose() { controls.dispose(); css.dispose(); },
    };
}
