import {
    Color, Euler, Fog, ImageTexture, TextureFilter, Mesh, MeshBasicMaterial, PerspectiveCamera, Scene,
    Wrapping,
} from 'threekt';
import { PlaneGeometry } from 'threekt/geometry';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import paintingUrl from './assets/758px-Canestra_di_frutta_(Caravaggio).jpg?url';
import { followPointer } from './exampleControls';
import { fitPerspective } from './fitPerspective';

export function createTextureComparison(
    canvas: HTMLCanvasElement, camera: PerspectiveCamera, renderer: WebGLRenderer, scenes: [Scene, Scene],
    labels: [string, string], clampHeight = false,
) {
    const pointer = followPointer(canvas, camera, scenes[0], 200, clampHeight ? 50 : -Infinity, clampHeight ? 1000 : Infinity);
    const legend = document.createElement('div');
    legend.className = 'example-comparison-legend';
    for (const label of labels) {
        const item = document.createElement('span');
        item.textContent = label;
        legend.append(item);
    }
    canvas.after(legend);

    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            pointer.update();
            const width = canvas.clientWidth;
            const height = canvas.clientHeight;
            const middle = Math.floor(width / 2);
            renderer.setScissorTest(true);
            try {
                renderer.setScissor(0, 0, Math.max(0, middle - 1), height);
                renderer.render(scenes[0], camera);
                renderer.setScissor(middle + 1, 0, Math.max(0, width - middle - 1), height);
                renderer.render(scenes[1], camera);
            } finally {
                renderer.setScissorTest(false);
            }
        },
        dispose() {
            legend.remove();
            pointer.dispose();
            renderer.dispose();
        },
    };
}

export async function createPaintingComparison(canvas: HTMLCanvasElement, mipmaps: HTMLCanvasElement[]) {
    const image = new Image();
    image.src = paintingUrl;
    await image.decode();
    const [floorImage] = mipmaps;
    const manual = mipmaps.length > 1;
    const geometry = new PlaneGeometry(100, 100);
    const rotation = new Euler(-Math.PI / 2, 0, 0);
    const scenes = [false, true].map(nearest => {
        const scene = new Scene();
        scene.background = Color.fromHex(0);
        scene.fog = new Fog(Color.fromHex(0), 1500, 4000);
        const color = Color.fromHex(nearest ? 0xffccaa : 0xffffff);

        const floorTexture = new ImageTexture(floorImage);
        floorTexture.repeat.set(1000, 1000);
        floorTexture.wrapS = floorTexture.wrapT = Wrapping.Repeat;
        if (manual) floorTexture.mipmaps = mipmaps;
        if (nearest) {
            floorTexture.magFilter = TextureFilter.Nearest;
            floorTexture.minFilter = manual ? TextureFilter.NearestMipmapNearest : TextureFilter.Nearest;
            floorTexture.generateMipmaps = false;
        }
        const floor = new Mesh(geometry, new MeshBasicMaterial(color, floorTexture));
        floor.quaternion.setFromEuler(rotation);
        floor.scale.setScalar(1000);
        floor.position.y = -1.117 * image.height / 2;
        scene.add(floor);

        const texture = new ImageTexture(image);
        texture.minFilter = nearest ? TextureFilter.Nearest : TextureFilter.Linear;
        texture.magFilter = nearest ? TextureFilter.Nearest : TextureFilter.Linear;
        const painting = new Mesh(geometry, new MeshBasicMaterial(color, texture));
        painting.scale.set(image.width / 100, image.height / 100, 1);
        scene.add(painting);

        const frame = new Mesh(geometry, new MeshBasicMaterial(Color.fromHex(0)));
        frame.position.z = -10;
        frame.scale.set(1.1 * image.width / 100, 1.1 * image.height / 100, 1);
        scene.add(frame);

        const material = new MeshBasicMaterial(Color.fromHex(0));
        material.transparent = true;
        material.opacity = 0.75;
        const shadow = new Mesh(geometry, material);
        shadow.position.set(0, -1.1 * image.height / 2, -1.1 * image.height / 2);
        shadow.quaternion.setFromEuler(rotation);
        shadow.scale.set(1.1 * image.width / 100, 1.1 * image.height / 100, 1);
        scene.add(shadow);
        return scene;
    });

    const camera = new PerspectiveCamera(35, 1, 1, 5000);
    camera.position.z = 1500;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return createTextureComparison(canvas, camera, renderer, [scenes[0], scenes[1]], [
        'Linear filtering · trilinear floor',
        manual ? 'Nearest filtering · nearest mip level' : 'Nearest filtering · no mipmaps',
    ]);
}

export function checkerboard(size: number, color: string): HTMLCanvasElement {
    const canvas = document.createElement('canvas');
    canvas.width = canvas.height = size;
    const context = canvas.getContext('2d')!;
    context.fillStyle = '#444';
    context.fillRect(0, 0, size, size);
    context.fillStyle = color;
    const half = size / 2;
    context.fillRect(0, 0, half, half);
    context.fillRect(half, half, half, half);
    return canvas;
}
