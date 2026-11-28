import { BoxGeometry, CircleGeometry, PlaneGeometry } from 'threekt/geometry';
import {
    BufferGeometry, Color, Float32BufferAttribute, ImageTexture, Mesh, MeshBasicMaterial, PerspectiveCamera,
    Raycaster, Scene, Wrapping,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import gridUrl from './assets/uv_grid_opengl.jpg?url';
import { addSliders } from './addSliders';
import { fitPerspective } from './fitPerspective';
import { raycastPointer } from './raycastPointer';

export async function createExample(canvas: HTMLCanvasElement) {
    const image = new Image();
    image.src = gridUrl;
    await image.decode();
    const surface = document.createElement('canvas');
    surface.width = image.naturalWidth;
    surface.height = image.naturalHeight;
    const ctx = surface.getContext('2d')!;
    const maps = [Wrapping.Repeat, Wrapping.MirroredRepeat, Wrapping.Repeat].map(wrapping => {
        const texture = new ImageTexture(surface);
        texture.wrapS = texture.wrapT = wrapping;
        return texture;
    });
    function mark(x: number, y: number) {
        ctx.drawImage(image, 0, 0);
        const px = x * surface.width, py = y * surface.height;
        const extent = Math.round(surface.height / 30);
        const gap = extent / 6;
        ctx.strokeStyle = '#ffff00';
        ctx.lineWidth = extent / 4;
        ctx.beginPath();
        for (const direction of [-1, 1]) {
            for (const slope of [-1, 1]) {
                ctx.moveTo(px + direction * gap, py + direction * slope * gap);
                ctx.lineTo(px + direction * extent, py + direction * slope * extent);
            }
        }
        ctx.stroke();
        maps.forEach(map => map.markUpdated());
    }
    mark(0, 0);
    const scene = new Scene();
    scene.background = Color.fromHex(0xeeeeee);
    const geometries: BufferGeometry[] = [new BoxGeometry(20, 20, 20), new PlaneGeometry(25, 25), new CircleGeometry(25, 40)];
    const meshes = geometries.map((geometry, index) => {
        const uv = (geometry.getAttribute('uv') as Float32BufferAttribute).array;
        for (let i = 0; i < uv.length; i++) uv[i] = (uv[i] - (index === 2 ? 0.25 : 0)) * 2;
        const mesh = new Mesh(geometry, new MeshBasicMaterial(Color.fromHex(0xffffff), maps[index]));
        mesh.position.set([4, -16, 24][index], -5, 0);
        scene.add(mesh);
        return mesh;
    });
    const camera = new PerspectiveCamera(45, 1, 1, 1000);
    camera.position.set(-30, 40, 50);
    camera.lookAt(scene.position);
    const circle = maps[2];
    const removeSliders = addSliders(canvas, [
        { label: 'Offset X', value: 0, max: 5, change: (value: number) => { circle.offset.x = value; } },
        { label: 'Offset Y', value: 0, max: 5, change: (value: number) => { circle.offset.y = value; } },
        { label: 'Repeat X', value: 1, max: 5, change: (value: number) => { circle.repeat.x = value; } },
        { label: 'Repeat Y', value: 1, max: 5, change: (value: number) => { circle.repeat.y = value; } },
        { label: 'Rotation', value: 0, max: Math.PI * 2, change: (value: number) => { circle.rotation = value; } },
    ].map(setting => ({ ...setting, min: 0, step: 0.01 })));
    const panel = canvas.parentElement!.querySelector('.example-panel')!;
    const modes = [Wrapping.Repeat, Wrapping.ClampToEdge, Wrapping.MirroredRepeat];
    for (const axis of ['wrapS', 'wrapT'] as const) {
        const label = document.createElement('label');
        const select = document.createElement('select');
        ['Repeat', 'Clamp to edge', 'Mirrored repeat'].forEach((text, i) => select.add(new Option(text, String(i))));
        select.addEventListener('change', () => { circle[axis] = modes[select.selectedIndex]; });
        label.append(axis === 'wrapS' ? 'Eje U ' : 'Eje V ', select);
        panel.append(label);
    }
    const input = raycastPointer(canvas);
    const raycaster = new Raycaster();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    let x = 0, y = 0;
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            raycaster.setFromCamera(input.pointer, camera);
            const hit = raycaster.intersectObjects(meshes, false)[0];
            if (hit?.uv) {
                const texture = maps[meshes.indexOf(hit.object as Mesh)];
                const uv = texture.transformUv(hit.uv);
                if (uv.x !== x || uv.y !== y) {
                    x = uv.x; y = uv.y;
                    mark(x, y);
                }
            }
            renderer.render(scene, camera);
        },
        dispose() { input.dispose(); removeSliders(); renderer.dispose(); },
    };
}
