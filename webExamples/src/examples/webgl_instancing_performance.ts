import {
    BufferGeometry, BufferGeometryUtils, Color, DomInputSurface, Float32BufferAttribute, InstancedMesh, Matrix4, Mesh,
    MeshNormalMaterial, OrbitControls, PerspectiveCamera, Quaternion, Scene, Vector3, WebGL2Context, WebGLRenderer,
} from 'threekt';
import modelUrl from './assets/suzanne_buffergeometry.json?url';
import { examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';

type Method = 'INSTANCED' | 'MERGED' | 'NAIVE';

/**
 * Loads a Three.js `BufferGeometry` JSON file with a position attribute and an optional index. Replaces
 * `BufferGeometryLoader` for this model.
 */
async function loadBufferGeometry(url: string) {
    const model: { data: { attributes: { position: { array: number[] } }; index?: { array: number[] } } } =
        await (await fetch(url)).json();
    const geometry = new BufferGeometry();
    geometry.setAttribute('position', new Float32BufferAttribute(new Float32Array(model.data.attributes.position.array), 3));
    if (model.data.index) geometry.setIndexArray(new Int32Array(model.data.index.array));
    return geometry;
}

/** Random position, orientation and uniform scale, as upstream `randomizeMatrix` and `Quaternion.random()`. */
function randomizeMatrix(matrix: Matrix4, position: Vector3, quaternion: Quaternion, scale: Vector3) {
    position.set(Math.random() * 40 - 20, Math.random() * 40 - 20, Math.random() * 40 - 20);
    const theta1 = 2 * Math.PI * Math.random();
    const theta2 = 2 * Math.PI * Math.random();
    const x0 = Math.random();
    const r1 = Math.sqrt(1 - x0);
    const r2 = Math.sqrt(x0);
    quaternion.set(r1 * Math.sin(theta1), r1 * Math.cos(theta1), r2 * Math.sin(theta2), r2 * Math.cos(theta2));
    const s = Math.random();
    scale.set(s, s, s);
    matrix.compose(position, quaternion, scale);
}

function geometryByteLength(geometry: BufferGeometry) {
    const index = geometry.index;
    let total = index ? index.size * index.bytesPerElement : 0;
    for (const name of geometry.attributeNames) {
        const attribute = geometry.getAttribute(name)!;
        total += attribute.size * attribute.bytesPerElement;
    }
    return total;
}

function formatBytes(bytes: number, decimals: number) {
    if (bytes === 0) return '0 bytes';
    const k = 1024;
    const sizes = ['bytes', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(decimals)) + ' ' + sizes[i];
}

/** Port of Three.js examples/webgl_instancing_performance.html. A select and a slider replace lil-gui. */
export async function createExample(canvas: HTMLCanvasElement) {
    const api = { method: 'INSTANCED' as Method, count: 1000 };
    const camera = new PerspectiveCamera(70, 1, 1, 100);
    camera.position.z = 30;
    const scene = new Scene();
    scene.background = Color.fromHex(0xffffff);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.autoRotate = true;

    const geometry = await loadBufferGeometry(modelUrl);
    geometry.computeVertexNormals();
    const material = new MeshNormalMaterial();
    const matrix = new Matrix4();
    const position = new Vector3();
    const quaternion = new Quaternion();
    const scale = new Vector3();
    let merged: BufferGeometry | null = null;

    const panel = examplePanel(canvas);
    const select = document.createElement('select');
    for (const method of ['INSTANCED', 'MERGED', 'NAIVE']) select.add(new Option(method, method));
    const countLabel = document.createElement('label');
    countLabel.className = 'example-setting';
    const countValue = document.createElement('span');
    const count = document.createElement('input');
    count.type = 'range';
    count.min = '1';
    count.max = '10000';
    count.step = '1';
    count.value = String(api.count);
    countLabel.append('Count', countValue, count);
    const stats = document.createElement('p');
    panel.append(select, countLabel, stats);

    function clean() {
        for (const child of scene.children.asJsReadonlyArrayView()) scene.remove(child);
        merged = null;
    }

    function initMesh() {
        clean();
        countValue.textContent = String(api.count);
        const geometryLength = geometryByteLength(geometry);
        let drawCalls = 1;
        let memory = api.count * 16 + geometryLength;
        if (api.method === 'INSTANCED') {
            const mesh = new InstancedMesh(geometry, material, api.count);
            for (let i = 0; i < api.count; i++) {
                randomizeMatrix(matrix, position, quaternion, scale);
                mesh.setMatrixAt(i, matrix);
            }
            scene.add(mesh);
        } else if (api.method === 'MERGED') {
            const geometries: BufferGeometry[] = [];
            for (let i = 0; i < api.count; i++) {
                randomizeMatrix(matrix, position, quaternion, scale);
                geometries.push(geometry.clone().applyMatrix4(matrix));
            }
            merged = BufferGeometryUtils.getInstance().mergeGeometries(geometries)!;
            scene.add(new Mesh(merged, material));
            memory = geometryByteLength(merged);
        } else {
            for (let i = 0; i < api.count; i++) {
                randomizeMatrix(matrix, position, quaternion, scale);
                const mesh = new Mesh(geometry, material);
                mesh.applyMatrix4(matrix);
                scene.add(mesh);
            }
            drawCalls = api.count;
        }
        stats.innerHTML = `<i>GPU draw calls</i>: ${drawCalls}<br><i>GPU memory</i>: ${formatBytes(memory, 2)}`;
    }

    select.addEventListener('change', () => { api.method = select.value as Method; initMesh(); });
    count.addEventListener('change', () => { api.count = count.valueAsNumber; initMesh(); });
    count.addEventListener('input', () => { countValue.textContent = count.value; });
    initMesh();

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            controls.update();
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() {
            panel.remove();
            controls.dispose();
            renderer.dispose();
        },
    };
}
