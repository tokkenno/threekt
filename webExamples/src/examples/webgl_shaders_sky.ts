import { BoxGeometry, SphereGeometry } from 'threekt/geometry';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { Mesh, PerspectiveCamera, Scene, Side, Uniform, Uniforms, Vector3 } from 'threekt';
import { ShaderMaterial, WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { addSliders } from './addSliders';
import { addToggle, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';
import { encodeSrgb } from './fullscreenPass';
import vertex from './shaders/Sky.vertex.glsl?raw';
import fragment from './shaders/Sky.fragment.glsl?raw';

export function createExample(canvas: HTMLCanvasElement) {
    const sun = new Vector3();
    const uniforms = new Uniforms().set('sunPosition', new Uniform(sun));
    const parameters = [
        { name: 'turbidity', label: 'Turbidity', min: 0, max: 20, step: 0.1, value: 10 },
        { name: 'rayleigh', label: 'Rayleigh', min: 0, max: 4, step: 0.001, value: 3 },
        { name: 'mieCoefficient', label: 'Mie coefficient', min: 0, max: 0.1, step: 0.001, value: 0.005 },
        { name: 'mieDirectionalG', label: 'Mie direction', min: 0, max: 0.99, step: 0.001, value: 0.7 },
        { name: 'exposure', label: 'Exposure', min: 0, max: 1, step: 0.0001, value: 0.05 },
        { name: 'cloudCoverage', label: 'Cloud coverage', min: 0, max: 1, step: 0.01, value: 0.4 },
        { name: 'cloudDensity', label: 'Cloud density', min: 0, max: 1, step: 0.01, value: 0.4 },
        { name: 'cloudElevation', label: 'Cloud elevation', min: 0, max: 1, step: 0.01, value: 0.5 },
    ];
    const panel = examplePanel(canvas);
    const sliders = parameters.map(param => {
        const uniform = new Uniform(param.value);
        uniforms.set(param.name, uniform);
        return { ...param, change: (value: number) => { uniform.value = value; } };
    });
    let elevation = 65, azimuth = 0;
    sliders.push(
        { name: 'elevation', label: 'Sun elevation', min: 0, max: 90, step: 0.1, value: 65, change: value => { elevation = value; } },
        { name: 'azimuth', label: 'Sun azimuth', min: -180, max: 180, step: 0.1, value: 0, change: value => { azimuth = value; } },
    );
    addSliders(canvas, sliders, panel);
    const disc = new Uniform(1);
    addToggle(panel, 'Sun disc', true, value => { disc.value = Number(value); });
    const time = new Uniform(0);
    uniforms.set('showSunDisc', disc).set('time', time)
        .set('cloudScale', new Uniform(0.0002)).set('cloudSpeed', new Uniform(0.00002));
    const scene = new Scene();
    const material = new ShaderMaterial(vertex, encodeSrgb + fragment, uniforms);
    material.side = Side.Back;
    material.depthWrite = false;
    const sky = new Mesh(new BoxGeometry(450000, 450000, 450000), material);
    scene.add(sky);
    const reflected = new ShaderMaterial('#define REFLECTION\n' + vertex, '#define REFLECTION\n' + encodeSrgb + fragment, uniforms);
    scene.add(new Mesh(new SphereGeometry(400, 64, 32), reflected));
    const camera = new PerspectiveCamera(60, 1, 100, 2e6);
    camera.position.set(0, 100, 2000);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.maxDistance = 10000;
    controls.minDistance = 500;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            sun.setFromSphericalCoords(1, (90 - elevation) * Math.PI / 180, azimuth * Math.PI / 180);
            time.value = seconds;
            renderer.render(scene, camera);
        },
        dispose() { panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
