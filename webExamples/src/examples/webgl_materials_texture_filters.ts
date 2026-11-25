import { checkerboard, createPaintingComparison } from './textureComparison';

export function createExample(canvas: HTMLCanvasElement) {
    return createPaintingComparison(canvas, [checkerboard(128, '#fff')]);
}
