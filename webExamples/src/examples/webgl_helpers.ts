import {
    BoxHelper, BufferGeometry, Color, EdgesGeometry, Float32BufferAttribute, GridHelper, Group,
    LineBasicMaterial, LineSegments, Mesh, MeshPhongMaterial, OctahedronGeometry, PerspectiveCamera,
    PointLight, Scene, Vector3, WebGL2Context, WebGLRenderer, WireframeGeometry,
} from 'threekt';
import model from './assets/lee-perry-smith/LeePerrySmith.glb?url';
import { fitPerspective } from './fitPerspective';
import { readExampleGltfGeometry, type ExampleGltf } from './readExampleGltf';

function lines(positions: number[], color: number) {
    return new LineSegments(
        new BufferGeometry().setAttribute('position', new Float32BufferAttribute(new Float32Array(positions), 3)),
        new LineBasicMaterial(Color.fromHex(color)),
    );
}

function polarGrid() {
    const radial: number[] = [];
    for (let i = 0; i < 16; i++) {
        const angle = i * 2 * Math.PI / 16;
        radial.push(0, 0, 0, Math.sin(angle) * 200, 0, Math.cos(angle) * 200);
    }
    const group = new Group();
    group.add(lines(radial, 0x0000ff));
    for (let i = 1; i <= 8; i++) {
        const ring: number[] = [];
        for (let j = 0; j < 64; j++) {
            for (const k of [j, j + 1]) {
                const angle = k * 2 * Math.PI / 64;
                ring.push(Math.sin(angle) * i * 25, 0, Math.cos(angle) * i * 25);
            }
        }
        group.add(lines(ring, i % 2 ? 0x808080 : 0x0000ff));
    }
    return group;
}

export async function createExample(canvas: HTMLCanvasElement) {
    const response = await fetch(model);
    if (!response.ok) throw new Error('Could not load the Lee Perry Smith model.');
    const buffer = await response.arrayBuffer();
    const jsonLength = new DataView(buffer).getUint32(12, true);
    const modelData: ExampleGltf = JSON.parse(new TextDecoder().decode(new Uint8Array(buffer, 20, jsonLength)));
    const geometry = readExampleGltfGeometry(modelData, buffer, 0, 28 + jsonLength);
    geometry.computeTangents();
    const camera = new PerspectiveCamera(70, 1, 1, 1000);
    camera.position.z = 460;
    const scene = new Scene();
    const group = new Group();
    group.scale.setScalar(50);
    scene.add(group);
    const [r, g, b] = modelData.materials[0].pbrMetallicRoughness.baseColorFactor!;
    const mesh = new Mesh(geometry, new MeshPhongMaterial(new Color(r, g, b)));
    group.add(mesh);
    const wireMaterial = new LineBasicMaterial();
    wireMaterial.depthTest = false;
    wireMaterial.opacity = 0.25;
    wireMaterial.transparent = true;
    const wire = new LineSegments(new WireframeGeometry(geometry), wireMaterial);
    wire.position.x = 4;
    group.add(wire);
    const edge = new LineSegments(new EdgesGeometry(geometry), wireMaterial);
    edge.position.x = -4;
    group.add(edge);
    scene.updateMatrixWorld(true);
    for (const object of [mesh, wire, edge, group]) scene.add(new BoxHelper(object));

    for (const [attribute, color] of [['normal', 0x00ffff], ['tangent', 0xff8844]] as const) {
        const position = geometry.getAttribute('position')!;
        const direction = geometry.getAttribute(attribute)!;
        const p = new Vector3();
        const d = new Vector3();
        const vectors: number[] = [];
        for (let i = 0; i < position.count; i++) {
            p.set(position.getX(i), position.getY(i), position.getZ(i)).multiplyScalar(50);
            d.set(direction.getX(i), direction.getY(i), direction.getZ(i));
            if (![d.x, d.y, d.z].every(Number.isFinite)) continue;
            vectors.push(p.x, p.y, p.z, p.x + d.x * 5, p.y + d.y * 5, p.z + d.z * 5);
        }
        scene.add(lines(vectors, color));
    }
    scene.add(new BoxHelper(scene));

    const light = new PointLight(Color.fromHex(0xffffff), 180000);
    light.position.set(200, 100, 150);
    scene.add(light);
    const lightMarker = new LineSegments(new WireframeGeometry(new OctahedronGeometry(15)), new LineBasicMaterial());
    light.add(lightMarker);
    const grid = new GridHelper(400, 40, Color.fromHex(0x0000ff), Color.fromHex(0x808080));
    grid.position.set(-150, -150, 0);
    scene.add(grid);
    const polar = polarGrid();
    polar.position.set(200, -150, 0);
    scene.add(polar);

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const time = seconds * 0.5;
            light.position.set(Math.sin(time * 0.7) * 300, Math.cos(time * 0.5) * 400, Math.cos(time * 1.3) * 300);
            renderer.render(scene, camera);
        },
        dispose() { renderer.dispose(); },
    };
}
