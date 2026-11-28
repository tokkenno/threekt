import {
    ColorSpace, Color, Euler, Float32BufferAttribute, Mesh, PerspectiveCamera, Scene, Uniform, Usage, Wrapping,
} from 'threekt';
import { SphereGeometry } from 'threekt/geometry';
import { ShaderMaterial, WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import waterUrl from './assets/water.jpg?url';
import { loadImageTexture } from './exampleControls';
import { fitPerspective } from './fitPerspective';

const vertexShader = `
uniform float amplitude;

attribute float displacement;

varying vec3 vNormal;
varying vec2 vUv;

void main() {

	vNormal = normal;
	vUv = ( 0.5 + amplitude ) * uv + vec2( amplitude );

	vec3 newPosition = position + amplitude * normal * vec3( displacement );
	gl_Position = projectionMatrix * modelViewMatrix * vec4( newPosition, 1.0 );

}
`;

const fragmentShader = `
varying vec3 vNormal;
varying vec2 vUv;

uniform vec3 color;
uniform sampler2D colorTexture;

void main() {

	vec3 light = vec3( 0.5, 0.2, 1.0 );
	light = normalize( light );

	float dProd = dot( vNormal, light ) * 0.5 + 0.5;

	vec4 tcolor = texture2D( colorTexture, vUv );
	vec4 gray = vec4( vec3( tcolor.r * 0.3 + tcolor.g * 0.59 + tcolor.b * 0.11 ), 1.0 );

	gl_FragColor = gray * vec4( vec3( dProd ) * vec3( color ), 1.0 );

}
`;

export async function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    scene.background = Color.fromHex(0x050505);
    const camera = new PerspectiveCamera(30, 1, 1, 10000);
    camera.position.z = 300;

    const texture = await loadImageTexture(waterUrl);
    texture.colorSpace = ColorSpace.None;
    texture.wrapS = texture.wrapT = Wrapping.Repeat;
    const color = Color.fromHex(0xff2200);
    const amplitude = new Uniform(1);
    const material = new ShaderMaterial(vertexShader, fragmentShader);
    material.uniforms.set('amplitude', amplitude);
    material.uniforms.set('color', new Uniform(color));
    material.uniforms.set('colorTexture', new Uniform(texture));

    const widthSegments = 128;
    const heightSegments = 64;
    const geometry = new SphereGeometry(50, widthSegments, heightSegments);
    const count = (widthSegments + 1) * (heightSegments + 1);
    const displacement = new Float32Array(count);
    const noise = Float32Array.from(displacement, () => Math.random() * 5);
    const attribute = new Float32BufferAttribute(displacement, 1).setUsage(Usage.DynamicDraw);
    geometry.setAttribute('displacement', attribute);
    const sphere = new Mesh(geometry, material);
    scene.add(sphere);

    const rotation = new Euler();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const rotationPhase = (Date.now() * 0.0001) % (16 * Math.PI);
    let lastTime = 0;
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const angle = rotationPhase + seconds * 0.1;
            sphere.quaternion.setFromEuler(rotation.set(0, angle, angle));
            amplitude.value = 2.5 * Math.sin(angle * 0.125);
            color.offsetHSL((seconds - lastTime) * 0.03, 0, 0);
            lastTime = seconds;
            for (let i = 0; i < count; i++) {
                noise[i] = Math.max(-5, Math.min(5, noise[i] + 0.5 * (0.5 - Math.random())));
                displacement[i] = Math.sin(0.1 * i + seconds * 10) + noise[i];
            }
            attribute.markUpdated();
            renderer.render(scene, camera);
        },
        dispose() {
            renderer.dispose();
        },
    };
}
