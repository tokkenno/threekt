import { createPaintingComparison } from './createPaintingComparison';

/** Compares linear and nearest sampling across eight explicitly colored mipmap levels. */
export function createExample(canvas: HTMLCanvasElement) {
    return createPaintingComparison(canvas, true);
}
