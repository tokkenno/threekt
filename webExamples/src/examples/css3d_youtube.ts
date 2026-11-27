import { PerspectiveCamera, Scene } from 'threekt';
import { cssPanel, cssTrackball, exampleCss3d, fitCssCamera } from './exampleCss3d';

export function createExample(canvas: HTMLCanvasElement) {
    const css = exampleCss3d(canvas, '#102028');
    const scene = new Scene();
    const camera = new PerspectiveCamera(50, 1, 1, 5000);
    camera.position.set(500, 350, 750);
    const controls = cssTrackball(canvas, camera);
    controls.rotateSpeed = 4;
    const videos = ['SJOz3qjfQXU', 'Y2-xZ-1HE-Q', 'IrydklNpcFI', '9ubytEsCaS0'];
    const screens = videos.map((id, i) => {
        const element = cssPanel(480, 360, '#142b35');
        element.style.backfaceVisibility = 'hidden';
        const button = document.createElement('button');
        button.textContent = 'Load YouTube video ' + (i + 1);
        Object.assign(button.style, { width: '100%', height: '100%', background: '#142b35', color: 'white',
            border: '1px solid #668d9b', font: '24px system-ui', cursor: 'pointer', pointerEvents: 'auto' });
        element.append(button);
        button.addEventListener('click', () => {
            const iframe = document.createElement('iframe');
            iframe.title = 'YouTube video ' + (i + 1);
            iframe.src = `https://www.youtube-nocookie.com/embed/${id}?rel=0`;
            iframe.allowFullscreen = true;
            iframe.allow = 'encrypted-media; picture-in-picture';
            iframe.style.pointerEvents = 'auto';
            button.replaceWith(iframe);
        }, { once: true });
        const node = css.add(element);
        const angle = i * Math.PI / 2;
        node.position.set(240 * Math.sin(angle), 0, 240 * Math.cos(angle));
        node.rotation.y = angle;
        scene.add(node);
        return element;
    });
    const blockFrames = (block: boolean) => {
        for (const screen of screens) {
            const iframe = screen.querySelector('iframe');
            if (iframe) iframe.style.pointerEvents = block ? 'none' : 'auto';
        }
    };
    controls.addEventListener('start', () => blockFrames(true));
    controls.addEventListener('end', () => blockFrames(false));
    return {
        render() {
            if (!fitCssCamera(canvas, camera)) return;
            controls.update();
            css.render(scene, camera);
        },
        dispose() { controls.dispose(); css.dispose(); },
    };
}
