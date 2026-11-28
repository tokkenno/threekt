import { Color, ImageTexture, Mesh, MeshBasicMaterial, PerspectiveCamera, Scene, Wrapping } from 'threekt';
import { PlaneGeometry } from 'threekt/geometry';

function backgroundTexture() {
    const canvas = document.createElement('canvas');
    canvas.width = canvas.height = 128;
    const context = canvas.getContext('2d')!;
    context.fillStyle = '#ddd';
    context.fillRect(0, 0, 128, 128);
    context.fillStyle = '#555';
    context.fillRect(0, 0, 64, 64);
    context.fillStyle = '#999';
    context.fillRect(32, 32, 32, 32);
    context.fillStyle = '#555';
    context.fillRect(64, 64, 64, 64);
    context.fillStyle = '#777';
    context.fillRect(96, 96, 32, 32);
    const map = new ImageTexture(canvas);
    map.wrapS = map.wrapT = Wrapping.Repeat;
    map.repeat.set(64, 32);
    return map;
}

export function generateLabelMaterial(text: string, background = 'rgba(0, 0, 0, 0.95)', font = 'bold 12pt arial', left = 10) {
    const canvas = document.createElement('canvas');
    canvas.width = 128;
    canvas.height = 32;
    const context = canvas.getContext('2d')!;
    context.fillStyle = background;
    context.fillRect(0, 0, 128, 32);
    context.fillStyle = 'white';
    context.font = font;
    context.fillText(text, left, 22);
    const material = new MeshBasicMaterial(Color.fromHex(0xffffff), new ImageTexture(canvas));
    material.transparent = true;
    return material;
}

export function addBlendingBackground(scene: Scene, camera: PerspectiveCamera) {
    const mapBg = backgroundTexture();
    const backgroundMaterial = new MeshBasicMaterial(Color.fromHex(0xffffff), mapBg);
    backgroundMaterial.depthWrite = false;
    const background = new Mesh(new PlaneGeometry(2, 2), backgroundMaterial);
    // Fill the camera view with a textured plane behind the swatches.
    background.position.z = camera.position.z - camera.far * 0.99;
    scene.add(background);

    return () => {
        const halfHeight = Math.tan(camera.fov * Math.PI / 360) * (camera.position.z - background.position.z) / camera.zoom;
        background.scale.set(camera.aspect * halfHeight, halfHeight, 1);
        const time = Date.now() * 0.00025;
        mapBg.offset.set((time * -0.01 * mapBg.repeat.x) % 1, (time * -0.01 * mapBg.repeat.y) % 1);
    };
}
