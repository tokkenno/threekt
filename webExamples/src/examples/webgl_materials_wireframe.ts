import {
    BufferGeometry, Color, Float32BufferAttribute, Mesh, MeshBasicMaterial, PerspectiveCamera, Scene, Side,
    Uniform,
} from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { ShaderMaterial, WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import modelUrl from './assets/WaltHeadLo_buffergeometry.json?url';
import { examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';

const vertexShader = `
attribute vec3 center;
varying vec3 vCenter;

void main() {

    vCenter = center;

    gl_Position = projectionMatrix * modelViewMatrix * vec4( position, 1.0 );

}
`;

const fragmentShader = `
uniform float thickness;

varying vec3 vCenter;

void main() {

    vec3 afwidth = fwidth( vCenter.xyz );

    vec3 edge3 = smoothstep( ( thickness - 1.0 ) * afwidth, thickness * afwidth, vCenter.xyz );

    float edge = 1.0 - min( min( edge3.x, edge3.y ), edge3.z );

    gl_FragColor.rgb = gl_FrontFacing ? vec3( 0.9, 0.9, 1.0 ) : vec3( 0.4, 0.4, 0.5 );
    gl_FragColor.a = edge;

}
`;

export async function createExample(canvas: HTMLCanvasElement) {
    const response = await fetch(modelUrl);
    if (!response.ok) throw new Error('Could not load the head geometry.');
    const model: { data: { attributes: { position: { array: number[] } } } } = await response.json();
    const positions = new Float32Array(model.data.attributes.position.array);
    const centers = new Float32Array(positions.length);
    for (let i = 0; i < positions.length / 3; i++) centers[i * 3 + i % 3] = 1;
    const geometry = new BufferGeometry()
        .setAttribute('position', new Float32BufferAttribute(positions, 3))
        .setAttribute('center', new Float32BufferAttribute(centers, 3));

    const lineMaterial = new MeshBasicMaterial(Color.fromHex(0xe0e0ff));
    lineMaterial.wireframe = true;
    const lines = new Mesh(geometry, lineMaterial);
    lines.position.x = -40;
    const thickness = new Uniform(1);
    const material = new ShaderMaterial(vertexShader, fragmentShader);
    material.uniforms.set('thickness', thickness);
    material.side = Side.Double;
    material.alphaToCoverage = true;
    const edges = new Mesh(geometry, material);
    edges.position.x = 40;
    const scene = new Scene();
    scene.add(lines);
    scene.add(edges);

    const camera = new PerspectiveCamera(40, 1, 1, 500);
    camera.position.z = 200;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.enablePan = false;
    controls.enableZoom = false;

    const panel = examplePanel(canvas);
    const label = document.createElement('label');
    label.className = 'example-setting';
    label.textContent = 'Edge thickness';
    const slider = document.createElement('input');
    slider.type = 'range';
    slider.min = '0';
    slider.max = '4';
    slider.step = '0.05';
    slider.value = '1';
    slider.addEventListener('input', () => { thickness.value = slider.valueAsNumber; });
    label.append(slider);
    panel.append(label);

    return {
        render() {
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() {
            panel.remove();
            controls.dispose();
            renderer.dispose();
        },
    };
}
