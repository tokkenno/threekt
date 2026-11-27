import { DataTexture, Uniform, Uniforms, WebGL2Context, WebGLRenderer } from 'threekt';
import { Audio, AudioAnalyser, AudioListener, AudioLoader } from 'threekt/audio';
import { badCatSound, exampleAudio } from './exampleAudio';
import { fullscreenPass } from './fullscreenPass';

export function createExample(canvas: HTMLCanvasElement) {
    const fftSize = 128;
    // DataTexture is RGBA, so the magnitudes of the analyser go to the red channel.
    const pixels = new Int8Array(fftSize / 2 * 4);
    const texture = new DataTexture(pixels, fftSize / 2, 1);
    let analyser: AudioAnalyser | undefined;
    const audio = exampleAudio(canvas, async () => {
        const listener = new AudioListener();
        const sound = new Audio(listener);
        // Upstream streams a media element and falls back to AudioLoader on iOS; the loader works everywhere.
        sound.setBuffer(await new AudioLoader().loadAsync(badCatSound));
        sound.setLoop(true).play();
        analyser = new AudioAnalyser(sound, fftSize);
    });
    const pass = fullscreenPass(`
        uniform sampler2D tAudioData;
        void main() {
            vec3 backgroundColor = vec3(0.125, 0.125, 0.125);
            vec3 color = vec3(1.0, 1.0, 0.0);
            float f = texture2D(tAudioData, vec2(vUv.x, 0.0)).r;
            float i = step(vUv.y, f) * step(f - 0.0125, vUv.y);
            gl_FragColor = vec4(mix(backgroundColor, color, i), 1.0);
        }
    `, new Uniforms().set('tAudioData', new Uniform(texture)));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!canvas.clientWidth || !canvas.clientHeight) return;
            renderer.setSize(canvas.clientWidth, canvas.clientHeight, window.devicePixelRatio);
            if (analyser && audio.running) {
                const data = analyser.getFrequencyData();
                for (let i = 0; i < data.length; i++) pixels[i * 4] = data[i];
                texture.markUpdated();
            }
            renderer.render(pass.scene, pass.camera);
        },
        dispose() { audio.dispose(); renderer.dispose(); },
    };
}
