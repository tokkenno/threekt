import {
    BufferGeometry, Color, Float32BufferAttribute, Line, LineBasicMaterial, PerspectiveCamera, Scene, Usage,
} from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { FramebufferTexture, WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { fitPerspective } from './fitPerspective';
import { texturePass } from './fullscreenPass';

function gosper(size: number) {
    const rules: Record<string, string> = { A: 'A+BF++BF-FA--FAFA-BF+', B: '-FA+BFBF++BF+FA--FA-B' };
    let pattern = 'A';
    for (let step = 0; step < 4; step++) pattern = pattern.replace(/[AB]/g, token => rules[token]);
    const positions = [0, 0, 0];
    let x = 0;
    let y = 0;
    let angle = 0;
    for (const token of pattern) {
        if (token === '+') angle += Math.PI / 3;
        if (token === '-') angle -= Math.PI / 3;
        if (token === 'F') {
            x += size * Math.cos(angle);
            y -= size * Math.sin(angle);
            positions.push(x, y, 0);
        }
    }
    return new Float32Array(positions);
}

export function createExample(canvas: HTMLCanvasElement) {
    const position = gosper(8);
    const colors = new Float32BufferAttribute(new Float32Array(position.length), 3);
    colors.setUsage(Usage.DynamicDraw);
    const geometry = new BufferGeometry().setAttribute('position', new Float32BufferAttribute(position, 3))
        .setAttribute('color', colors).center();
    const lineMaterial = new LineBasicMaterial();
    lineMaterial.vertexColors = true;
    const main = new Scene();
    const line = new Line(geometry, lineMaterial);
    line.scale.setScalar(0.05);
    main.add(line);
    const camera = new PerspectiveCamera(70, 1, 1, 1000);
    camera.position.z = 20;
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.enablePan = false;
    const texture = new FramebufferTexture(128, 128);
    const inset = texturePass(texture, false);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    renderer.autoClear = false;
    const color = new Color(0, 0, 0);
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            for (let i = 0; i < colors.count; i++) {
                color.setHSL((i - seconds * 1500) / colors.count, 1, 0.5);
                colors.setXYZ(i, color.r, color.g, color.b);
            }
            colors.markUpdated();
            renderer.clear();
            renderer.render(main, camera);
            if (canvas.width < texture.width || canvas.height < texture.height) return;
            renderer.copyFramebufferToTexture(texture, Math.floor((canvas.width - texture.width) / 2),
                Math.floor((canvas.height - texture.height) / 2));
            renderer.setViewport(0, canvas.clientHeight - 128, 128, 128);
            renderer.render(inset.scene, inset.camera);
            renderer.resetViewport();
        },
        dispose() { controls.dispose(); renderer.dispose(); },
    };
}
