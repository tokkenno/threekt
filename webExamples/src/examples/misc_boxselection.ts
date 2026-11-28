import {
    AmbientLight, BoxGeometry, Color, DirectionalLight, Mesh, MeshLambertMaterial, PerspectiveCamera, Scene,
    Vector3,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { fitPerspective } from './fitPerspective';

export function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    scene.background = Color.fromHex(0xf0f0f0);
    scene.add(new AmbientLight(Color.fromHex(0xaaaaaa), 2));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 2);
    light.position.set(1, 1, 1);
    scene.add(light);
    const camera = new PerspectiveCamera(70, 1, 0.1, 500);
    camera.position.z = 50;
    const geometry = new BoxGeometry();
    const meshes = Array.from({ length: 200 }, () => {
        const material = new MeshLambertMaterial(Color.fromHex(Math.floor(Math.random() * 0x1000000)));
        const mesh = new Mesh(geometry, material);
        mesh.position.set((Math.random() - 0.5) * 80, (Math.random() - 0.5) * 65, (Math.random() - 0.5) * 80);
        mesh.rotation.set(Math.random() * 2, Math.random() * 2, Math.random() * 2);
        mesh.scale.set(1 + Math.random() * 3, 1 + Math.random() * 3, 1 + Math.random() * 3);
        scene.add(mesh);
        return { mesh, material };
    });

    const selection = document.createElement('div');
    Object.assign(selection.style, { position: 'fixed', pointerEvents: 'none', zIndex: '2',
        border: '1px solid #458aff', background: '#458aff40' });
    selection.hidden = true;
    canvas.after(selection);
    const events = new AbortController();
    const { signal } = events;
    const oldTouchAction = canvas.style.touchAction;
    canvas.style.touchAction = 'none';
    let pointer: number | undefined;
    let startX = 0, startY = 0;
    const projected = new Vector3();
    const update = (x: number, y: number) => {
        const bounds = canvas.getBoundingClientRect();
        const left = Math.min(x, startX), right = Math.max(x, startX);
        const top = Math.min(y, startY), bottom = Math.max(y, startY);
        Object.assign(selection.style, {
            left: left + 'px', top: top + 'px', width: (right - left) + 'px', height: (bottom - top) + 'px',
        });
        camera.updateMatrixWorld(true);
        for (const { mesh, material } of meshes) {
            projected.copyFrom(mesh.position).project(camera);
            const px = bounds.left + (1 + projected.x) * bounds.width / 2;
            const py = bounds.top + (1 - projected.y) * bounds.height / 2;
            const selected = projected.z >= -1 && projected.z <= 1 && px >= left && px <= right && py >= top && py <= bottom;
            material.emissive.setHex(selected ? 0xffffff : 0x000000);
        }
    };
    canvas.addEventListener('pointerdown', event => {
        if (event.button !== 0 || pointer !== undefined) return;
        pointer = event.pointerId;
        canvas.setPointerCapture(pointer);
        startX = event.clientX;
        startY = event.clientY;
        selection.hidden = false;
        update(startX, startY);
    }, { signal });
    canvas.addEventListener('pointermove', event => {
        if (event.pointerId === pointer) update(event.clientX, event.clientY);
    }, { signal });
    canvas.addEventListener('lostpointercapture', event => {
        if (event.pointerId !== pointer) return;
        selection.hidden = true;
        pointer = undefined;
    }, { signal });

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() { events.abort(); selection.remove(); canvas.style.touchAction = oldTouchAction; renderer.dispose(); },
    };
}
