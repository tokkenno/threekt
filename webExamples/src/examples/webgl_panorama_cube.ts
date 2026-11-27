import {
    BoxGeometry, BufferGeometry, Color, DomInputSurface, Float32BufferAttribute, Group, ImageTexture,
    Mesh, MeshBasicMaterial, OrbitControls, PerspectiveCamera, Scene, Side, WebGL2Context, WebGLRenderer,
} from 'threekt';
import atlasUrl from './assets/sun_temple_stripe.jpg?url';
import { fitPerspective } from './fitPerspective';

export async function createExample(canvas: HTMLCanvasElement) {
    const atlas = new Image();
    atlas.src = atlasUrl;
    await atlas.decode();

    const cube = new BoxGeometry();
    const positions = (cube.getAttribute('position') as Float32BufferAttribute).array;
    const uv = (cube.getAttribute('uv') as Float32BufferAttribute).array;
    const skybox = new Group();
    skybox.scale.z = -1;
    for (let face = 0; face < 6; face++) {
        const tile = document.createElement('canvas');
        tile.width = tile.height = atlas.height;
        tile.getContext('2d')!.drawImage(atlas, face * tile.width, 0, tile.width, tile.height, 0, 0, tile.width, tile.height);
        const geometry = new BufferGeometry().setIndexArray(new Int32Array([0, 2, 1, 2, 3, 1]))
            .setAttribute('position', new Float32BufferAttribute(positions.slice(face * 12, face * 12 + 12), 3))
            .setAttribute('uv', new Float32BufferAttribute(uv.slice(face * 8, face * 8 + 8), 2));
        const material = new MeshBasicMaterial(Color.fromHex(0xffffff), new ImageTexture(tile));
        material.side = Side.Back;
        skybox.add(new Mesh(geometry, material));
    }

    const scene = new Scene();
    scene.add(skybox);
    const camera = new PerspectiveCamera(90, 1, 0.1, 100);
    camera.position.z = 0.01;
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.enableZoom = false;
    controls.enablePan = false;
    controls.enableDamping = true;
    controls.rotateSpeed = -0.25;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() {
            controls.dispose();
            renderer.dispose();
        },
    };
}
