import { ColorSpace, Float32BufferAttribute, ImageTexture, Mesh, MeshBasicMaterial, PlaneGeometry } from 'threekt';
import { ImprovedNoise } from './ImprovedNoise';

export function createTerrain() {
    const size = 256;
    const heights = new Uint8Array(size * size);
    const noise = new ImprovedNoise();
    const slice = Math.random() * 100;
    for (let quality = 1; quality <= 125; quality *= 5) {
        for (let i = 0; i < heights.length; i++) {
            heights[i] += Math.abs(noise.noise((i % size) / quality, Math.floor(i / size) / quality, slice) * quality * 1.75);
        }
    }

    const geometry = new PlaneGeometry(7500, 7500, size - 1, size - 1);
    const positions = geometry.getAttribute('position') as Float32BufferAttribute;
    for (let i = 0; i < heights.length; i++) {
        positions.array[i * 3 + 2] = -positions.array[i * 3 + 1];
        positions.array[i * 3 + 1] = heights[i] * 10;
    }

    const textureCanvas = document.createElement('canvas');
    textureCanvas.width = textureCanvas.height = size;
    const context = textureCanvas.getContext('2d')!;
    const image = context.createImageData(size, size);
    const sample = (x: number, z: number) => heights[
        Math.min(size - 1, Math.max(0, z)) * size + Math.min(size - 1, Math.max(0, x))
    ];
    for (let i = 0; i < heights.length; i++) {
        const x = i % size, z = Math.floor(i / size);
        const dx = sample(x - 2, z) - sample(x + 2, z);
        const dz = sample(x, z - 2) - sample(x, z + 2);
        const shade = (dx + 2 + dz) / (Math.sqrt(3) * Math.hypot(dx, 2, dz));
        const brightness = 0.5 + heights[i] * 0.007;
        image.data.set([
            Math.min(255, Math.max(0, (96 + shade * 128) * brightness)),
            Math.min(255, Math.max(0, (32 + shade * 96) * brightness)),
            Math.min(255, Math.max(0, shade * 96 * brightness)),
            255,
        ], i * 4);
    }
    context.putImageData(image, 0, 0);
    const texture = new ImageTexture(textureCanvas);
    texture.colorSpace = ColorSpace.SRGB;
    const material = new MeshBasicMaterial();
    material.map = texture;
    /** Height of the nearest terrain vertex below the world position (x, z). */
    const heightAt = (x: number, z: number) =>
        sample(Math.round((x / 7500 + 0.5) * (size - 1)), Math.round((z / 7500 + 0.5) * (size - 1))) * 10;
    return { mesh: new Mesh(geometry, material), centerHeight: sample(size / 2, size / 2) * 10, heightAt };
}
