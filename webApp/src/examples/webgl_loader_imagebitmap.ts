import { Box, Color, Euler, GridHelper, Group, ImageTexture, Mesh, MeshBasicMaterial, PerspectiveCamera, Scene, Vector3, WebGL2Context, WebGLRenderer } from 'threekt';
import earthUrl from './assets/earth_atmos_2048.jpg?url';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

export async function createExample(canvas: HTMLCanvasElement) {
    const response = await fetch(earthUrl);
    if (!response.ok) throw new Error(`No se pudo cargar la imagen (HTTP ${response.status}).`);
    const bitmap = await createImageBitmap(await response.blob());
    const bitmapCanvas = document.createElement('canvas');
    bitmapCanvas.width = bitmap.width;
    bitmapCanvas.height = bitmap.height;
    try {
        bitmapCanvas.getContext('2d')!.drawImage(bitmap, 0, 0);
    } finally {
        bitmap.close();
    }
    const [imageMaterial, bitmapMaterial] = [
        new MeshBasicMaterial(Color.fromHex(0xff8888), await loadImageTexture(earthUrl)),
        new MeshBasicMaterial(Color.fromHex(0xffffff), new ImageTexture(bitmapCanvas)),
    ];
    const camera = new PerspectiveCamera(30, 1, 1, 1500);
    camera.position.set(0, 4, 7);
    camera.lookAt(new Vector3());
    const scene = new Scene();
    const group = new Group();
    scene.add(group);
    group.add(new GridHelper(4, 12, Color.fromHex(0x888888), Color.fromHex(0x444444)));
    const geometry = new Box();
    const cubes = [300, 600, 900, 1300, 1600, 1900].map((delay, index) => {
        const mesh = new Mesh(geometry, index < 3 ? imageMaterial : bitmapMaterial);
        mesh.position.set(Math.random() * 2 - 1, Math.random() * 2 - 1, Math.random() * 2 - 1);
        mesh.quaternion.setFromEuler(new Euler(Math.random() * 2 * Math.PI, Math.random() * 2 * Math.PI, Math.random() * 2 * Math.PI));
        return { delay, mesh };
    });
    let next = 0;
    const rotation = new Euler();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            while (next < cubes.length && seconds * 1000 >= cubes[next].delay) group.add(cubes[next++].mesh);
            group.quaternion.setFromEuler(rotation.set(0, seconds / 3, 0));
            renderer.render(scene, camera);
        },
        dispose() { renderer.dispose(); },
    };
}
