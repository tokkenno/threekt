import { createInteractiveCubes } from './createInteractiveCubes';

export async function createExample(canvas: HTMLCanvasElement) {
    return createInteractiveCubes(canvas, false);
}
