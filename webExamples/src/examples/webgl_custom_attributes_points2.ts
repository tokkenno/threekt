import {
    ColorSpace, Color, Euler, Matrix4, PerspectiveCamera, Points, Scene, Uint32BufferAttribute, Uniform,
    Vector3, Wrapping,
} from 'threekt';
import { BoxGeometry, SphereGeometry } from 'threekt/geometry';
import { ShaderMaterial, WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import discUrl from './assets/disc.png?url';
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

    gl_PointSize = size * ( 300.0 / -mvPosition.z );

    gl_Position = projectionMatrix * mvPosition;

}
`;

const fragmentShader = `
uniform vec3 color;
uniform sampler2D pointTexture;

varying vec3 vColor;

void main() {

    vec4 spriteColor = vec4( color * vColor, 1.0 ) * texture2D( pointTexture, gl_PointCoord );

    gl_FragColor = spriteColor;

}
`;

export async function createExample(canvas: HTMLCanvasElement) {
    const radius = 100;
    const sphere = uniquePositions(new SphereGeometry(radius, 68, 38));
    const box = uniquePositions(new BoxGeometry(80, 80, 80, 10, 10, 10));
    const sphereCount = sphere.length / 3;
    const { geometry, position, size, sizes } = customPointGeometry(sphere.concat(box), sphereCount, (color, i, y) => {
        if (i < sphereCount) color.setHSL(0.01 + 0.1 * i / sphereCount, 0.99, (y + radius) / (4 * radius));
        else color.setHSL(0.6, 0.75, 0.25 + y / (2 * radius));
    });

    const texture = await loadImageTexture(discUrl);
    texture.colorSpace = ColorSpace.None;
    texture.wrapS = texture.wrapT = Wrapping.Repeat;
    const material = new ShaderMaterial(vertexShader, fragmentShader);
    material.uniforms.set('pointTexture', new Uniform(texture));
    material.uniforms.set('color', new Uniform(Color.fromHex(0xffffff)));
    material.transparent = true;
    const points = new Points(geometry, material);
    const scene = new Scene();
    scene.add(points);
    const camera = new PerspectiveCamera(45, 1, 1, 10000);
    camera.position.z = 300;
    const index = new Uint32BufferAttribute(new Int32Array(sizes.length), 1);
    geometry.setIndex(index);
    const indices = index.array;
    const order = Array.from(indices, (_, idx) => idx);
    const depths = new Float64Array(indices.length);
    const rotation = new Euler();
    const matrix = new Matrix4();
    const vertex = new Vector3();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));

    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const time = seconds * 5;
            points.quaternion.setFromEuler(rotation.set(0, 0.02 * time, 0.02 * time));
            for (let i = 0; i < sphereCount; i++) sizes[i] = 16 + 12 * Math.sin(0.1 * i + time);
            size.markUpdated();

            camera.updateWorldMatrix(true, false);
            points.updateWorldMatrix(true, false);
            matrix.multiplyMatrices(camera.projectionMatrix, camera.matrixWorldInverse).multiply(points.matrixWorld);
            for (let i = 0; i < depths.length; i++) {
                depths[i] = vertex.set(position[i * 3], position[i * 3 + 1], position[i * 3 + 2]).applyMatrix4(matrix).z;
            }
            order.sort((a, b) => depths[b] - depths[a]);
            indices.set(order);
            index.markUpdated();
            renderer.render(scene, camera);
        },
        dispose() { renderer.dispose(); },
    };
}
