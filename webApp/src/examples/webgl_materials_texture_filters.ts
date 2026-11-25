import { createPaintingComparison } from './createPaintingComparison';

/** Compares linear and nearest filtering with the upstream painting and checkerboard floor. */
export function createExample(canvas: HTMLCanvasElement) {
    return createPaintingComparison(canvas, false);
}
