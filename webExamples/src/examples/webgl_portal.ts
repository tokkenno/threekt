import { createReflectiveRoom } from './createReflectiveRoom';

export function createExample(canvas: HTMLCanvasElement) {
    return createReflectiveRoom(canvas, true);
}
