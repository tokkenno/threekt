import {
    AmbientLight, BufferGeometry, BufferGeometryUtils, Color, DirectionalLight, Mesh, MeshLambertMaterial,
    PerspectiveCamera, PlaneGeometry, Scene, Side, TextureFilter, Vector3,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import atlas from './assets/minecraft-atlas.png';
import { loadImageTexture } from './loadImageTexture';
import { ImprovedNoise } from './ImprovedNoise';
import { fitPerspective } from './fitPerspective';
import { flyCamera } from './flyCamera';

const worldSize = 128;
const blockSize = 100;

function createLandscape() {
    const data = new Float64Array(worldSize * worldSize);
    const noise = new ImprovedNoise();
    const seed = Math.random() * 100;
    for (let quality = 2, octave = 0; octave < 4; octave++, quality *= 4) {
        for (let i = 0; i < data.length; i++) {
            data[i] += noise.noise((i % worldSize) / quality, Math.floor(i / worldSize) / quality, seed) * quality;
        }
    }
    const heightAt = (x: number, z: number) => Math.trunc(data[x + z * worldSize] * 0.15) || 0;
    const face = (a: number, b: number) => {
        const geometry = new PlaneGeometry(blockSize, blockSize);
        const uv = geometry.getAttribute('uv')!;
        uv.setY(a, 0.5);
        uv.setY(b, 0.5);
        return geometry;
    };
    const top = face(2, 3).rotateX(-Math.PI / 2).translate(0, 50, 0);
    const sides = [
        { dx: 1, dz: 0, geometry: face(0, 1).rotateY(Math.PI / 2).translate(50, 0, 0) },
        { dx: -1, dz: 0, geometry: face(0, 1).rotateY(-Math.PI / 2).translate(-50, 0, 0) },
        { dx: 0, dz: 1, geometry: face(0, 1).translate(0, 0, 50) },
        { dx: 0, dz: -1, geometry: face(0, 1).rotateY(Math.PI).translate(0, 0, -50) },
    ];
    const faces: BufferGeometry[] = [];
    const half = worldSize / 2;
    for (let z = 0; z < worldSize; z++) {
        for (let x = 0; x < worldSize; x++) {
            const height = heightAt(x, z);
            const append = (geometry: BufferGeometry) => {
                faces.push(geometry.clone().translate((x - half) * blockSize, height * blockSize, (z - half) * blockSize));
            };
            append(top);
            for (const { dx, dz, geometry } of sides) {
                const neighbor = heightAt(x + dx, z + dz);
                if (x + dx < 0 || x + dx >= worldSize || z + dz < 0 || z + dz >= worldSize ||
                    (neighbor !== height && neighbor !== height + 1)) append(geometry);
            }
        }
    }
    return { geometry: BufferGeometryUtils.getInstance().mergeGeometries(faces)!, startHeight: heightAt(half, half) * blockSize + 100 };
}

export async function createExample(canvas: HTMLCanvasElement) {
    const { geometry, startHeight } = createLandscape();
    const texture = await loadImageTexture(atlas);
    texture.magFilter = TextureFilter.Nearest;
    const material = new MeshLambertMaterial();
    material.map = texture;
    material.side = Side.Double;
    const scene = new Scene();
    scene.background = Color.fromHex(0xbfd1e5);
    scene.add(new Mesh(geometry, material));
    scene.add(new AmbientLight(Color.fromHex(0xeeeeee), 3));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 12);
    light.position.set(1, 1, 0.5).normalize();
    scene.add(light);
    const camera = new PerspectiveCamera(60, 1, 1, 20000);
    camera.position.y = startHeight;
    camera.lookAt(new Vector3(0, startHeight - 100, -500));
    const controls = flyCamera(canvas, camera, 1000);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            controls.update(seconds);
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); },
    };
}
