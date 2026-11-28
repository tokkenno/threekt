import {
    Blending, BufferGeometry, ColorSpace, Float32BufferAttribute, PerspectiveCamera, Points,
    Scene, TextureFilter, Uniform, Uniforms, Vector3, VideoTexture,
} from 'threekt';
import { ShaderMaterial, WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import webmUrl from './assets/kinect.webm?url';
import mp4Url from './assets/kinect.mp4?url';
import { fitPerspective } from './fitPerspective';
import { addSliders } from './addSliders';

const vertexShader = `
    uniform sampler2D map;

    uniform float width;
    uniform float height;
    uniform float nearClipping, farClipping;

    uniform float pointSize;
    uniform float zOffset;

    varying vec2 vUv;

    const float XtoZ = 1.11146; // tan( 1.0144686 / 2.0 ) * 2.0;
    const float YtoZ = 0.83359; // tan( 0.7898090 / 2.0 ) * 2.0;

    void main() {

        vUv = vec2( position.x / width, position.y / height );

        vec4 color = texture2D( map, vUv );
        float depth = ( color.r + color.g + color.b ) / 3.0;

        // Projection code by @kcmic

        float z = ( 1.0 - depth ) * (farClipping - nearClipping) + nearClipping;

        vec4 pos = vec4(
            ( position.x / width - 0.5 ) * z * XtoZ,
            ( position.y / height - 0.5 ) * z * YtoZ,
            - z + zOffset,
            1.0);

        gl_PointSize = pointSize;
        gl_Position = projectionMatrix * modelViewMatrix * pos;

    }
`;

const fragmentShader = `
    uniform sampler2D map;

    varying vec2 vUv;

    void main() {

        vec4 color = texture2D( map, vUv );
        gl_FragColor = vec4( color.r, color.g, color.b, 0.2 );

    }
`;

export async function createExample(canvas: HTMLCanvasElement) {
    const video = document.createElement('video');
    video.muted = video.loop = video.playsInline = true;
    video.src = video.canPlayType('video/webm') ? webmUrl : mp4Url;
    await video.play();

    const texture = new VideoTexture(video);
    texture.minFilter = TextureFilter.Nearest;
    texture.colorSpace = ColorSpace.None;
    const width = 640, height = 480;
    const positions = new Float32Array(width * height * 3);
    for (let pixel = 0; pixel < width * height; pixel++) {
        positions[pixel * 3] = pixel % width;
        positions[pixel * 3 + 1] = Math.floor(pixel / width);
    }
    const settings = [
        ['nearClipping', 'Plano cercano', 850, 1, 10000],
        ['farClipping', 'Plano lejano', 4000, 1, 10000],
        ['pointSize', 'Point size', 2, 1, 10],
        ['zOffset', 'Offset Z', 1000, 0, 4000],
    ] as const;
    const uniforms = new Uniforms().set('map', new Uniform(texture))
        .set('width', new Uniform(width)).set('height', new Uniform(height));
    const sliders = settings.map(([name, label, value, min, max]) => {
        const uniform = new Uniform(value);
        uniforms.set(name, uniform);
        return { label, value, min, max, step: 1, change: (next: number) => { uniform.value = next; } };
    });
    const material = new ShaderMaterial(vertexShader, fragmentShader, uniforms);
    material.blending = Blending.Additive;
    material.depthTest = material.depthWrite = false;
    material.transparent = true;
    const geometry = new BufferGeometry().setAttribute('position', new Float32BufferAttribute(positions, 3));
    const scene = new Scene();
    scene.add(new Points(geometry, material));
    const camera = new PerspectiveCamera(50, 1, 1, 10000);
    camera.position.z = 500;
    const target = new Vector3(0, 0, -1000);
    const events = new AbortController();
    let mouseX = 0, mouseY = 0;
    canvas.addEventListener('pointermove', event => {
        mouseX = (event.offsetX - canvas.clientWidth / 2) * 8;
        mouseY = (event.offsetY - canvas.clientHeight / 2) * 8;
    }, { signal: events.signal });
    const removePanel = addSliders(canvas, sliders);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            camera.position.x += (mouseX - camera.position.x) * 0.05;
            camera.position.y += (-mouseY - camera.position.y) * 0.05;
            camera.lookAt(target);
            texture.update();
            renderer.render(scene, camera);
        },
        dispose() {
            events.abort();
            removePanel();
            renderer.dispose();
            video.pause();
            video.removeAttribute('src');
            video.load();
        },
    };
}
