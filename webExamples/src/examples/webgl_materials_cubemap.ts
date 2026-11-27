import { Mesh, OBJLoader } from 'threekt';
import walt from './assets/walt/WaltHead.obj?url';
import { createCubemapModels } from './createCubemapModels';

export async function createExample(canvas: HTMLCanvasElement) {
    const response = await fetch(walt);
    if (!response.ok) throw new Error('Could not load WaltHead.');
    const model = new OBJLoader().parse(await response.text());
    const mesh = model.children.asJsReadonlyArrayView().find(node => node instanceof Mesh);
    if (!mesh) throw new Error('WaltHead does not contain a mesh.');
    return createCubemapModels(canvas, mesh.geometry, false);
}
