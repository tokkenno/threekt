import { Color, DirectionalLight, FogExp2, GridHelper, Mesh, MeshPhongMaterial,
    PerspectiveCamera, Scene, SphereGeometry, Vector3, WebGL2Context, WebGLRenderer } from 'threekt';
import song from './assets/sounds/358232_j_s_song.ogg?url';
import ambience from './assets/sounds/Project_Utopia.ogg?url';
import { badCatSound, exampleAudio, loadAudioBuffer, playAudioBuffer, spatialAudio } from './exampleAudio';
import { fitPerspective } from './fitPerspective';
import { flyCamera } from './flyCamera';

export function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    scene.fog = new FogExp2(Color.fromHex(0x000000), 0.0025);
    const light = new DirectionalLight(Color.fromHex(0xffffff), 3);
    light.position.set(0, 0.5, 1);
    scene.add(light);
    scene.add(new GridHelper(1000, 10, Color.fromHex(0x444444), Color.fromHex(0x444444)));
    const geometry = new SphereGeometry(20, 32, 16);
    const sources = [[-250, 0, 0xffaa00], [250, 0, 0xff2200], [0, -250, 0x6622aa]].map(([x, z, tint]) => {
        const material = new MeshPhongMaterial(Color.fromHex(tint));
        material.flatShading = true;
        material.shininess = 0;
        const node = new Mesh(geometry, material);
        node.position.set(x, 30, z);
        scene.add(node);
        return { node, material, tint };
    });
    const camera = new PerspectiveCamera(55, 1, 1, 10000);
    camera.position.set(0, 90, 480);
    camera.lookAt(new Vector3(0, 30, 0));
    const flight = flyCamera(canvas, camera, 100);
    const spatial = spatialAudio();
    const nodes: { panner: PannerNode; analyser: AnalyserNode; bins: Uint8Array<ArrayBuffer> }[] = [];
    const audio = exampleAudio(canvas, async context => {
        const [first, second, background] = await Promise.all([song, badCatSound, ambience].map(url => loadAudioBuffer(context, url)));
        if (context.state === 'closed') return;
        const master = context.createGain();
        master.gain.value = 0.55;
        master.connect(context.destination);
        nodes.length = 0;
        for (const { node } of sources) {
            const panner = new PannerNode(context, { refDistance: 20, panningModel: 'HRTF' });
            const analyser = new AnalyserNode(context, { fftSize: 32 });
            analyser.connect(panner).connect(master);
            spatial.updatePanner(panner, node);
            nodes.push({ panner, analyser, bins: new Uint8Array(analyser.frequencyBinCount) });
        }
        playAudioBuffer(context, first, nodes[0].analyser, true);
        playAudioBuffer(context, second, nodes[1].analyser, true);
        const oscillator = new OscillatorNode(context, { frequency: 144 });
        const oscillatorGain = new GainNode(context, { gain: 0.35 });
        oscillator.connect(oscillatorGain).connect(nodes[2].analyser);
        oscillator.start();
        const backgroundGain = new GainNode(context, { gain: 0.35 });
        backgroundGain.connect(master);
        playAudioBuffer(context, background, backgroundGain, true);
    }, flight.panel);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            flight.update(seconds);
            if (audio.context?.state === 'running') {
                spatial.updateListener(audio.context, camera);
                nodes.forEach(({ analyser, bins }, i) => {
                    analyser.getByteFrequencyData(bins);
                    const average = bins.reduce((sum, value) => sum + value, 0) / (bins.length * 255);
                    sources[i].material.emissive.setHex(sources[i].tint).multiplyScalar(average * 0.7);
                });
            }
            renderer.render(scene, camera);
        },
        dispose() { audio.dispose(); flight.dispose(); renderer.dispose(); },
    };
}
