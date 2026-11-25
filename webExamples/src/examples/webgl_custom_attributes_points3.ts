import {
    Box, Color, Euler, Matrix4, PerspectiveCamera, Points, Quaternion, Scene, ShaderMaterial,
    Uniform, Vector3, WebGL2Context, WebGLRenderer, Wrapping,
} from 'threekt';
import ballUrl from './assets/ball.png?url';
import { customPointGeometry, uniquePositions } from './customPointGeometry';
import { loadImageTexture } from './exampleControls';
import { fitPerspective } from './fitPerspective';

const vertexShader = `
attribute float size;
attribute vec3 ca;

varying vec3 vColor;

void main() {

    vColor = ca;

    vec4 mvPosition = modelViewMatrix * vec4( position, 1.0 );

    gl_PointSize = size * ( 150.0 / -mvPosition.z );

    gl_Position = projectionMatrix * mvPosition;

}
`;

const fragmentShader = `
uniform vec3 color;
uniform sampler2D pointTexture;

varying vec3 vColor;

void main() {

    vec4 outColor = texture2D( pointTexture, gl_PointCoord );

    if ( outColor.a < 0.5 ) discard;

    gl_FragColor = outColor * vec4( color * vColor, 1.0 );

    float depth = gl_FragCoord.z / gl_FragCoord.w;
    const vec3 fogColor = vec3( 0.0 );

    float fogFactor = smoothstep( 200.0, 600.0, depth );
    gl_FragColor = mix( gl_FragColor, vec4( fogColor, gl_FragColor.w ), fogFactor );

}
`;

export async function createExample(canvas: HTMLCanvasElement) {
    const vertices: number[] = [];
    for (let i = 0; i < 100000; i++) {
        const x = Math.random() * 200 - 100;
        const y = Math.random() * 200 - 100;
        const z = Math.random() * 200 - 100;
        if (Math.max(Math.abs(x), Math.abs(y), Math.abs(z)) > 60) vertices.push(x, y, z);
    }
    const innerCount = vertices.length / 3;

    const horizontal = uniquePositions(new Box(200, 20, 20, 50, 5, 5));
    const vertical = uniquePositions(new Box(20, 240, 20, 5, 60, 5));
    const matrix = new Matrix4();
    const position = new Vector3();
    const vertex = new Vector3();
    const rotation = new Euler();
    const quaternion = new Quaternion();
    const scale = new Vector3(1, 1, 1);

    function addBeam(beam: number[], x: number, y: number, z: number, angle: number) {
        matrix.compose(position.set(x, y, z), quaternion.setFromEuler(rotation.set(0, angle, 0)), scale);
        for (let i = 0; i < beam.length; i += 3) {
            vertex.set(beam[i], beam[i + 1], beam[i + 2]).applyMatrix4(matrix);
            vertices.push(vertex.x, vertex.y, vertex.z);
        }
    }

    for (const y of [-110, 110]) {
        for (const side of [-110, 110]) {
            addBeam(horizontal, 0, y, side, 0);
            addBeam(horizontal, side, y, 0, Math.PI / 2);
        }
    }
    for (const x of [-110, 110]) {
        for (const z of [-110, 110]) addBeam(vertical, x, 0, z, 0);
    }

    const { geometry, size, sizes } = customPointGeometry(vertices, innerCount, (color, i) => {
        color.setHSL(i < innerCount ? 0.5 + 0.2 * i / innerCount : 0.1, 1, 0.5);
    });
    const texture = await loadImageTexture(ballUrl);
    texture.colorSpace = null;
    texture.wrapS = texture.wrapT = Wrapping.Repeat;
    const material = new ShaderMaterial(vertexShader, fragmentShader);
    material.uniforms.set('pointTexture', new Uniform(texture));
    material.uniforms.set('color', new Uniform(Color.fromHex(0xffffff)));
    const points = new Points(geometry, material);
    const scene = new Scene();
    scene.add(points);
    const camera = new PerspectiveCamera(40, 1, 1, 1000);
    camera.position.z = 500;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));

    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const time = seconds * 10;
            points.quaternion.setFromEuler(rotation.set(0, 0.02 * time, 0.02 * time));
            for (let i = 0; i < innerCount; i++) sizes[i] = Math.max(0, 26 + 32 * Math.sin(0.1 * i + 0.6 * time));
            size.markUpdated();
            renderer.render(scene, camera);
        },
        dispose() { renderer.dispose(); },
    };
}
