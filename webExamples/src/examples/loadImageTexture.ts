import { ImageTexture } from 'threekt';

/** Decodes a browser image before offering it to the renderer. */
export async function loadImageTexture(url: string) {
    const image = new Image();
    image.src = url;
    await image.decode();
    return new ImageTexture(image);
}
