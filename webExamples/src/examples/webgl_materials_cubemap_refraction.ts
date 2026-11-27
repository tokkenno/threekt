import { PLYLoader } from 'threekt';
import lucy from './assets/ply/Lucy100k.ply?url';
import { createCubemapModels } from './createCubemapModels';

export async function createExample(canvas: HTMLCanvasElement) {
    const response = await fetch(lucy);
    if (!response.ok) throw new Error('Could not load Lucy.');
    const geometry = new PLYLoader().parse(new Int8Array(await response.arrayBuffer()));
    geometry.computeVertexNormals();
    return createCubemapModels(canvas, geometry, true);
}
