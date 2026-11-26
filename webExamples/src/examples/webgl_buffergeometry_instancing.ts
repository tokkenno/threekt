import {
    Euler, Float32BufferAttribute, InstancedBufferAttribute, InstancedBufferGeometry, Mesh, PerspectiveCamera,
    RawShaderMaterial, Scene, Side, Uniform, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { addSliders } from './addSliders';
import { fitPerspective } from './fitPerspective';

const vertexShader = `
precision highp float;

uniform float sineTime;

uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;

attribute vec3 position;
attribute vec3 offset;
attribute vec4 color;
attribute vec4 orientationStart;
attribute vec4 orientationEnd;

varying vec3 vPosition;
varying vec4 vColor;

void main(){
    vPosition = offset * max( abs( sineTime * 2.0 + 1.0 ), 0.5 ) + position;
    vec4 orientation = normalize( mix( orientationStart, orientationEnd, sineTime ) );
    vec3 vcV = cross( orientation.xyz, vPosition );
    vPosition = vcV * ( 2.0 * orientation.w ) + ( cross( orientation.xyz, vcV ) * 2.0 + vPosition );

    vColor = color;

    gl_Position = projectionMatrix * modelViewMatrix * vec4( vPosition, 1.0 );
}
`;

const fragmentShader = `
precision highp float;

uniform float time;

varying vec3 vPosition;
varying vec4 vColor;

void main() {
    vec4 color = vec4( vColor );
    color.r += sin( vPosition.x * 10.0 + time ) * 0.5;

    gl_FragColor = color;
}
`;

/** Port of Three.js examples/webgl_buffergeometry_instancing.html. A slider replaces lil-gui. */
export function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(50, 1, 1, 10);
    camera.position.z = 2;
    const scene = new Scene();

    /** Writes a random normalized 4D vector at [offset], as upstream does with `Vector4.normalize()`. */
    const randomUnit4 = (target: Float32Array, offset: number) => {
        const v = [Math.random() * 2 - 1, Math.random() * 2 - 1, Math.random() * 2 - 1, Math.random() * 2 - 1];
        const length = Math.hypot(...v) || 1;
        target.set(v.map(component => component / length), offset);
    };
    const instances = 50000;
    const offsets = new Float32Array(instances * 3);
    const colors = new Float32Array(instances * 4);
    const orientationsStart = new Float32Array(instances * 4);
    const orientationsEnd = new Float32Array(instances * 4);
    for (let i = 0; i < instances; i++) {
        offsets.set([Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5], i * 3);
        colors.set([Math.random(), Math.random(), Math.random(), Math.random()], i * 4);
        randomUnit4(orientationsStart, i * 4);
        randomUnit4(orientationsEnd, i * 4);
    }

    const geometry = new InstancedBufferGeometry();
    geometry.instanceCount = instances;
    geometry.setAttribute('position', new Float32BufferAttribute(new Float32Array([0.025, -0.025, 0, -0.025, 0.025, 0, 0, 0, 0.025]), 3));
    geometry.setAttribute('offset', new InstancedBufferAttribute(offsets, 3));
    geometry.setAttribute('color', new InstancedBufferAttribute(colors, 4));
    geometry.setAttribute('orientationStart', new InstancedBufferAttribute(orientationsStart, 4));
    geometry.setAttribute('orientationEnd', new InstancedBufferAttribute(orientationsEnd, 4));

    const time = new Uniform(1.0);
    const sineTime = new Uniform(1.0);
    const material = new RawShaderMaterial(vertexShader, fragmentShader);
    material.uniforms.set('time', time);
    material.uniforms.set('sineTime', sineTime);
    material.side = Side.Double;
    material.transparent = true;

    const mesh = new Mesh(geometry, material);
    scene.add(mesh);
    const rotation = new Euler();

    const removePanel = addSliders(canvas, [{
        label: 'Instance count', min: 0, max: instances, step: 1, value: instances,
        change: value => { geometry.instanceCount = value; },
    }]);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const milliseconds = seconds * 1000;
            mesh.quaternion.setFromEuler(rotation.set(0, milliseconds * 0.0005, 0));
            time.value = milliseconds * 0.005;
            sineTime.value = Math.sin(time.value * 0.05);
            renderer.render(scene, camera);
        },
        dispose() {
            removePanel();
            renderer.dispose();
        },
    };
}
