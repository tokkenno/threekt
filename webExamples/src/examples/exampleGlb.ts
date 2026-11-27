import { loadImageTexture } from './loadImageTexture';
import type { ExampleGltf } from './readExampleGltf';

export async function loadExampleGlb<T extends ExampleGltf = ExampleGltf>(url: string) {
    const response = await fetch(url);
    if (!response.ok) throw new Error(`Could not load GLB (HTTP ${response.status}).`);
    const buffer = await response.arrayBuffer();
    const length = new DataView(buffer).getUint32(12, true);
    const json = JSON.parse(new TextDecoder().decode(new Uint8Array(buffer, 20, length))) as T;
    return { buffer, start: 28 + length, json };
}

export async function loadGlbImage(glb: Awaited<ReturnType<typeof loadExampleGlb>>, image: { bufferView: number; mimeType: string }) {
    const view = glb.json.bufferViews[image.bufferView];
    const data = new Uint8Array(glb.buffer, glb.start + (view.byteOffset ?? 0), view.byteLength);
    const url = URL.createObjectURL(new Blob([data], { type: image.mimeType }));
    try {
        const texture = await loadImageTexture(url);
        texture.flipY = false;
        return texture;
    } finally {
        URL.revokeObjectURL(url);
    }
}
