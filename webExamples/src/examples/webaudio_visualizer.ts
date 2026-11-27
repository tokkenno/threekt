import { DataTexture, Uniform, Uniforms, WebGL2Context, WebGLRenderer } from 'threekt';
import { badCatSound, exampleAudio, loadAudioBuffer, playAudioBuffer } from './exampleAudio';
import { fullscreenPass } from './fullscreenPass';

export function createExample(canvas: HTMLCanvasElement) {
    const fftSize = 128;
    const magnitudes = new Uint8Array(fftSize / 2);
    const pixels = new Int8Array(magnitudes.length * 4);
    const texture = new DataTexture(pixels, magnitudes.length, 1);
    let analyser: AnalyserNode | undefined;
    const audio = exampleAudio(canvas, async context => {
        const buffer = await loadAudioBuffer(context, badCatSound);
        if (context.state === 'closed') return;
        analyser = context.createAnalyser();
        analyser.fftSize = fftSize;
        analyser.connect(context.destination);
        playAudioBuffer(context, buffer, analyser, true);
    });
    const pass = fullscreenPass(`
        uniform sampler2D tAudioData;
        void main() {
            float frequency = texture2D(tAudioData, vec2(vUv.x, 0.0)).r;
            float line = step(vUv.y, frequency + 0.0125) * step(frequency, vUv.y);
            gl_FragColor = vec4(mix(vec3(0.125), vec3(1.0, 1.0, 0.0), line), 1.0);
        }
    `, new Uniforms().set('tAudioData', new Uniform(texture)));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!canvas.clientWidth || !canvas.clientHeight) return;
            renderer.setSize(canvas.clientWidth, canvas.clientHeight, window.devicePixelRatio);
            if (analyser && audio.context?.state === 'running') {
                analyser.getByteFrequencyData(magnitudes);
                for (let i = 0; i < magnitudes.length; i++) pixels[i * 4] = magnitudes[i];
                texture.markUpdated();
            }
            renderer.render(pass.scene, pass.camera);
        },
        dispose() { audio.dispose(); renderer.dispose(); },
    };
}
