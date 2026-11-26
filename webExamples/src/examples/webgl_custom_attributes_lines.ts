import {
    Blending, Color, Float32BufferAttribute, FontLoader, Line, PerspectiveCamera, Scene, ShaderMaterial,
    TextGeometry, Uniform, Uniforms, Usage, WebGL2Context, WebGLRenderer,
} from 'threekt';
import fontUrl from './assets/fonts/helvetiker_bold.typeface.json?url';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const response = await fetch(fontUrl);
    if (!response.ok) throw new Error('Could not load Helvetiker Bold.');
    const font = new FontLoader().parse(await response.text());
    const geometry = new TextGeometry('three.js', Object.assign(new TextGeometry.Parameters(), {
        font, size: 50, depth: 15, curveSegments: 10, bevelThickness: 5, bevelSize: 1.5,
        bevelEnabled: true, bevelSegments: 10,
    }));
    geometry.center();
    const count = geometry.getAttribute('position')!.count;
    const displacement = new Float32BufferAttribute(new Float32Array(count * 3), 3);
    displacement.setUsage(Usage.DynamicDraw);
    const colors = new Float32Array(count * 3);
    const color = new Color(1, 1, 1);
    for (let i = 0; i < count; i++) {
        color.setHSL(i / count, 0.5, 0.5);
        colors.set([color.r, color.g, color.b], i * 3);
    }
    geometry.setAttribute('displacement', displacement);
    geometry.setAttribute('customColor', new Float32BufferAttribute(colors, 3));

    const amplitude = new Uniform(5);
    const tint = new Color(1, 1, 1);
    const material = new ShaderMaterial(`
        uniform float amplitude;
        attribute vec3 displacement, customColor;
        varying vec3 vColor;
        void main() {
            vColor = customColor;
            gl_Position = projectionMatrix * modelViewMatrix * vec4(position + amplitude * displacement, 1.0);
        }
    `, `
        uniform vec3 tint;
        varying vec3 vColor;
        void main() { gl_FragColor = vec4(vColor * tint, 0.3); }
    `, new Uniforms().set('amplitude', amplitude).set('tint', new Uniform(tint)));
    material.blending = Blending.Additive;
    material.depthTest = false;
    material.transparent = true;

    const scene = new Scene();
    scene.background = Color.fromHex(0x050505);
    const line = new Line(geometry, material);
    line.rotation.x = 0.2;
    scene.add(line);
    const camera = new PerspectiveCamera(30, 1, 1, 10000);
    camera.position.z = 400;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    let previous = 0;
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            line.rotation.y = 0.25 * seconds;
            amplitude.value = Math.sin(0.5 * seconds) * 5;
            tint.setHSL((0.5 + seconds * 0.03) % 1, 0.5, 0.65);
            const step = Math.sqrt(Math.min(0.05, seconds - previous) * 60);
            previous = seconds;
            for (let i = 0; i < displacement.array.length; i++) {
                displacement.array[i] += (0.5 - Math.random()) * 0.3 * step;
            }
            displacement.markUpdated();
            renderer.render(scene, camera);
        },
        dispose() { renderer.dispose(); },
    };
}
