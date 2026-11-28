import {
    Color, DirectionalLight, FogExp2, Mesh, MeshPhongMaterial, PerspectiveCamera, Scene, Vector3,
} from 'threekt';
import { GridHelper } from 'threekt/helpers';
import { SphereGeometry } from 'threekt/geometry';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { Audio, AudioAnalyser, AudioListener, PositionalAudio } from 'threekt/audio';
import song from './assets/sounds/358232_j_s_song.ogg?url';
import utopia from './assets/sounds/Project_Utopia.ogg?url';
import { addSliders } from './addSliders';
import { addSelect } from './exampleControls';
import { badCatSound, exampleAudio, loopingMediaElement } from './exampleAudio';
import { fitPerspective } from './fitPerspective';
import { flyCamera } from './flyCamera';

export function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    scene.fog = new FogExp2(Color.fromHex(0x000000), 0.0025);
    const light = new DirectionalLight(Color.fromHex(0xffffff), 3);
    light.position.set(0, 0.5, 1).normalize();
    scene.add(light);
    const helper = new GridHelper(1000, 10, Color.fromHex(0x444444), Color.fromHex(0x444444));
    helper.position.y = 0.1;
    scene.add(helper);
    const sphere = new SphereGeometry(20, 32, 16);
    const materials = [0xffaa00, 0xff2200, 0x6622aa].map(color => {
        const material = new MeshPhongMaterial(Color.fromHex(color));
        material.flatShading = true;
        material.shininess = 0;
        return material;
    });
    const meshes = [[-250, 0], [250, 0], [0, -250]].map(([x, z], i) => {
        const mesh = new Mesh(sphere, materials[i]);
        mesh.position.set(x, 30, z);
        scene.add(mesh);
        return mesh;
    });
    const camera = new PerspectiveCamera(50, 1, 1, 10000);
    camera.position.set(0, 25, 0);
    camera.lookAt(new Vector3(0, 25, -1));
    const flight = flyCamera(canvas, camera, 70);
    const elements: HTMLAudioElement[] = [];
    const analysers: AudioAnalyser[] = [];
    let removeControls: (() => void) | undefined;
    const audio = exampleAudio(canvas, async context => {
        const listener = new AudioListener();
        camera.add(listener);
        const mediaSource = async (url: string) => {
            const element = loopingMediaElement(url);
            elements.push(element);
            await element.play();
            return context.createMediaElementSource(element);
        };
        const sound1 = new PositionalAudio(listener);
        sound1.setNodeSource(await mediaSource(song));
        sound1.setRefDistance(20);
        meshes[0].add(sound1);
        const sound2 = new PositionalAudio(listener);
        sound2.setNodeSource(await mediaSource(badCatSound));
        sound2.setRefDistance(20);
        meshes[1].add(sound2);
        // Threekt does not wrap oscillators, so the example creates one on the browser context and wraps it.
        const oscillator = (context.context as globalThis.AudioContext).createOscillator();
        oscillator.type = 'sine';
        oscillator.frequency.setValueAtTime(144, context.currentTime);
        oscillator.start(0);
        const sound3 = new PositionalAudio(listener);
        sound3.setNodeSource(context.wrapNode(oscillator));
        sound3.setRefDistance(20);
        sound3.setVolume(0.5);
        meshes[2].add(sound3);
        analysers.push(...[sound1, sound2, sound3].map(sound => new AudioAnalyser(sound, 32)));
        const sound4 = new Audio(listener);
        sound4.setNodeSource(await mediaSource(utopia));
        sound4.setVolume(0.5);

        const volume = (label: string, value: number, change: (value: number) => void) =>
            ({ label, min: 0, max: 1, step: 0.01, value, change });
        const removeVolumes = addSliders(canvas, [
            volume('Master', listener.getMasterVolume(), value => listener.setMasterVolume(value)),
            volume('First sphere', sound1.getVolume(), value => sound1.setVolume(value)),
            volume('Second sphere', sound2.getVolume(), value => sound2.setVolume(value)),
            // Volumes fade in over 10 ms, so read the targets instead of the current gains.
            volume('Third sphere', 0.5, value => sound3.setVolume(value)),
            volume('Ambient', 0.5, value => sound4.setVolume(value)),
        ], flight.panel);
        const removeFrequency = addSliders(canvas, [{ label: 'Generator frequency', min: 50, max: 5000, step: 1,
            value: oscillator.frequency.value,
            change: value => oscillator.frequency.setValueAtTime(value, context.currentTime) }], flight.panel);
        const waves = { sine: 'Sine', square: 'Square', sawtooth: 'Sawtooth', triangle: 'Triangle' };
        addSelect(flight.panel, 'Wave type', waves, oscillator.type, value => { oscillator.type = value as OscillatorType; });
        const waveLabel = flight.panel.lastElementChild;
        removeControls = () => { removeVolumes(); removeFrequency(); waveLabel?.remove(); oscillator.stop(); };
    }, flight.panel);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            flight.update(seconds);
            if (audio.running) {
                analysers.forEach((analyser, i) => { materials[i].emissive.b = analyser.getAverageFrequency() / 256; });
            }
            renderer.render(scene, camera);
        },
        dispose() {
            elements.forEach(element => element.pause());
            removeControls?.();
            audio.dispose();
            flight.dispose();
            renderer.dispose();
        },
    };
}
