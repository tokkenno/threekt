import { checkerboard, createPaintingComparison } from './textureComparison';

export function createExample(canvas: HTMLCanvasElement) {
    const colors = ['#f00', '#0f0', '#00f', '#400', '#040', '#004', '#044', '#404'];
    return createPaintingComparison(canvas, colors.map((color, level) => checkerboard(128 >> level, color)));
}
