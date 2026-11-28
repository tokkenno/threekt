import {
    Color, Euler, Fog, ImageTexture, Mesh, MeshBasicMaterial, PerspectiveCamera, PlaneGeometry,
    Scene, TextureFilter, Wrapping,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import paintingUrl from './assets/758px-Canestra_di_frutta_(Caravaggio).jpg?url';
import { fitPerspective } from './fitPerspective';
import { followPointer } from './followPointer';
import { loadImageTexture } from './loadImageTexture';
import { renderTextureComparison } from './renderTextureComparison';

function mipmap(size: number, color: string) {
    const canvas = document.createElement('canvas');
    canvas.width = canvas.height = size;
    const context = canvas.getContext('2d')!;
    context.fillStyle = '#444';
    context.fillRect(0, 0, size, size);
    context.fillStyle = color;
    context.fillRect(0, 0, size / 2, size / 2);
    context.fillRect(size / 2, size / 2, size / 2, size / 2);
    return canvas;
}

/** Shared painting and floor used by the two upstream texture filtering comparisons. */
export async function createPaintingComparison(canvas: HTMLCanvasElement, manualMipmaps: boolean) {
    const camera = new PerspectiveCamera(35, 1, 1, 5000);
    camera.position.z = 1500;
    const painting = await loadImageTexture(paintingUrl);
    const image = painting.image as HTMLImageElement;
    const { width, height } = image;
    const levels = manualMipmaps
        ? ['#f00', '#0f0', '#00f', '#400', '#040', '#004', '#044', '#404'].map((color, level) => mipmap(128 >> level, color))
        : [];
    const floorImage = levels[0] ?? mipmap(128, '#fff');
    const geometry = new PlaneGeometry(100, 100);
    const floorRotation = new Euler(-Math.PI / 2, 0, 0);
    const createScene = (nearest: boolean) => {
        const scene = new Scene();
        scene.background = Color.fromHex(0);
        scene.fog = new Fog(Color.fromHex(0), 1500, 4000);
        const tint = Color.fromHex(nearest ? 0xffccaa : 0xffffff);

        const floorTexture = new ImageTexture(floorImage);
        floorTexture.mipmaps = levels;
        floorTexture.repeat.set(1000, 1000);
        floorTexture.wrapS = floorTexture.wrapT = Wrapping.Repeat;
        if (nearest) {
            floorTexture.magFilter = TextureFilter.Nearest;
            floorTexture.minFilter = manualMipmaps ? TextureFilter.NearestMipmapNearest : TextureFilter.Nearest;
            floorTexture.generateMipmaps = manualMipmaps;
        }
        const floor = new Mesh(geometry, new MeshBasicMaterial(tint, floorTexture));
        floor.quaternion.setFromEuler(floorRotation);
        floor.scale.set(1000, 1000, 1000);
        floor.position.y = -1.117 * height / 2;
        scene.add(floor);

        const paintingTexture = nearest ? new ImageTexture(image) : painting;
        paintingTexture.minFilter = paintingTexture.magFilter = nearest ? TextureFilter.Nearest : TextureFilter.Linear;
        const picture = new Mesh(geometry, new MeshBasicMaterial(tint, paintingTexture));
        picture.scale.set(width / 100, height / 100, 1);
        scene.add(picture);

        const frame = new Mesh(geometry, new MeshBasicMaterial(Color.fromHex(0)));
        frame.position.z = -10;
        frame.scale.set(1.1 * width / 100, 1.1 * height / 100, 1);
        scene.add(frame);

        const shadowMaterial = new MeshBasicMaterial(Color.fromHex(0));
        shadowMaterial.opacity = 0.75;
        shadowMaterial.transparent = true;
        const shadow = new Mesh(geometry, shadowMaterial);
        shadow.position.set(0, -1.1 * height / 2, -1.1 * height / 2);
        shadow.quaternion.setFromEuler(floorRotation);
        shadow.scale.set(1.1 * width / 100, 1.1 * height / 100, 1);
        scene.add(shadow);
        return scene;
    };
    const scenes: [Scene, Scene] = [createScene(false), createScene(true)];

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const pointer = followPointer(canvas);
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            pointer.update(camera, scenes[0].position, 200);
            renderTextureComparison(canvas, renderer, camera, scenes);
        },
        dispose() {
            pointer.dispose();
            renderer.dispose();
        },
    };
}
