import {
    BufferGeometry, Color, Euler, Float32BufferAttribute, LineSegments, PerspectiveCamera, Scene,
    ShaderMaterial, Usage, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { fitPerspective } from './fitPerspective';

const vertexShader = `
    attribute float visible;
    varying float vVisible;
    attribute vec3 vertColor;
    varying vec3 vColor;

    void main() {
        vColor = vertColor;
        vVisible = visible;
        gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
    }
`;

const fragmentShader = `
    varying float vVisible;
    varying vec3 vColor;

    void main() {
        if (vVisible > 0.0) {
            gl_FragColor = vec4(vColor, 1.0);
        } else {
            discard;
        }
    }
`;

/** Port of Three.js examples/webgl_buffergeometry_selective_draw.html, without Stats. */
export async function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    const camera = new PerspectiveCamera(45, 1, 0.01, 10);
    camera.position.z = 3.5;

    const numLat = 100;
    const numLng = 200;
    const lineCount = numLat * numLng;
    const positions = new Float32Array(lineCount * 6);
    const colors = new Float32Array(lineCount * 6);
    const visible = new Float32Array(lineCount * 2).fill(1);
    const color = new Color(1, 1, 1);

    for (let i = 0; i < numLat; i++) {
        for (let j = 0; j < numLng; j++) {
            const lat = Math.random() * Math.PI / 50 + i / numLat * Math.PI;
            const lng = Math.random() * Math.PI / 50 + j / numLng * 2 * Math.PI;
            const offset = (i * numLng + j) * 6;

            positions.set([Math.sin(lat) * Math.cos(lng), Math.cos(lat), Math.sin(lat) * Math.sin(lng)], offset + 3);
            color.setHSL(lat / Math.PI, 1, 0.2);
            colors.set([color.r, color.g, color.b], offset);
            color.setHSL(lat / Math.PI, 1, 0.7);
            colors.set([color.r, color.g, color.b], offset + 3);
        }
    }

    const visibility = new Float32BufferAttribute(visible, 1).setUsage(Usage.DynamicDraw);
    const geometry = new BufferGeometry()
        .setAttribute('position', new Float32BufferAttribute(positions, 3))
        .setAttribute('vertColor', new Float32BufferAttribute(colors, 3))
        .setAttribute('visible', visibility);
    geometry.computeBoundingSphere();

    const mesh = new LineSegments(geometry, new ShaderMaterial(vertexShader, fragmentShader));
    scene.add(mesh);
    const rotation = new Euler();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));

    const panel = document.createElement('aside');
    panel.className = 'example-panel';
    const status = document.createElement('output');
    const hideLines = document.createElement('button');
    hideLines.textContent = 'Ocultar algunas líneas';
    const showLines = document.createElement('button');
    showLines.textContent = 'Mostrar todas';
    panel.append(status, hideLines, showLines);
    canvas.parentElement?.append(panel);

    let culled = 0;
    const updateCount = () => {
        status.textContent = `1 llamada de dibujo · ${lineCount.toLocaleString('es-ES')} líneas · ${culled.toLocaleString('es-ES')} ocultas`;
    };
    updateCount();

    hideLines.addEventListener('click', () => {
        for (let i = 0; i < visible.length; i += 2) {
            if (Math.random() > 0.75 && visible[i]) {
                culled++;
                visible[i] = 0;
                visible[i + 1] = 0;
            }
        }
        visibility.markUpdated();
        updateCount();
    });
    showLines.addEventListener('click', () => {
        culled = 0;
        visible.fill(1);
        visibility.markUpdated();
        updateCount();
    });

    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const time = Date.now() * 0.001;
            mesh.quaternion.setFromEuler(rotation.set(time * 0.25, time * 0.5, 0));
            renderer.render(scene, camera);
        },
        dispose() {
            panel.remove();
            renderer.dispose();
        },
    };
}
