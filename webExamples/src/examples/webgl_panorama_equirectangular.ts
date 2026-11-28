import { Color, Mesh, MeshBasicMaterial, PerspectiveCamera, Scene, Side, SphereGeometry, Vector3 } from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import panoramaUrl from './assets/2294472375_24a3b8ef46_o.jpg?url';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(75, 1, 1, 1100);
    const scene = new Scene();
    const texture = await loadImageTexture(panoramaUrl);
    const material = new MeshBasicMaterial(Color.fromHex(0xffffff), texture);
    // Mirror the mesh and select its interior instead of baking the reflection into the vertices.
    material.side = Side.Back;
    const panorama = new Mesh(new SphereGeometry(500, 60, 40), material);
    panorama.scale.x = -1;
    scene.add(panorama);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));

    const events = new AbortController();
    const touchAction = canvas.style.touchAction;
    canvas.style.touchAction = 'none';
    let lon = 0, lat = 0;
    let drag: { id: number; x: number; y: number; lon: number; lat: number } | null = null;
    const target = new Vector3();
    let previousTime = 0;

    canvas.addEventListener('pointerdown', (event) => {
        if (!event.isPrimary || event.button !== 0) return;
        drag = { id: event.pointerId, x: event.clientX, y: event.clientY, lon, lat };
        canvas.setPointerCapture(event.pointerId);
    }, { signal: events.signal });
    canvas.addEventListener('pointermove', (event) => {
        if (drag?.id !== event.pointerId) return;
        lon = (drag.x - event.clientX) * 0.1 + drag.lon;
        lat = (event.clientY - drag.y) * 0.1 + drag.lat;
    }, { signal: events.signal });
    for (const type of ['pointerup', 'pointercancel', 'lostpointercapture'] as const) {
        canvas.addEventListener(type, (event) => {
            if (drag?.id === event.pointerId) drag = null;
        }, { signal: events.signal });
    }
    canvas.addEventListener('wheel', (event) => {
        event.preventDefault();
        camera.fov = Math.max(10, Math.min(75, camera.fov + event.deltaY * 0.05));
        camera.updateProjectionMatrix();
    }, { signal: events.signal, passive: false });

    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            if (drag === null) lon += 6 * (seconds - previousTime);
            previousTime = seconds;
            lat = Math.max(-85, Math.min(85, lat));
            const phi = (90 - lat) * Math.PI / 180;
            const theta = lon * Math.PI / 180;
            target.set(500 * Math.sin(phi) * Math.cos(theta), 500 * Math.cos(phi), 500 * Math.sin(phi) * Math.sin(theta));
            camera.lookAt(target);
            renderer.render(scene, camera);
        },
        dispose() {
            if (drag && canvas.hasPointerCapture(drag.id)) canvas.releasePointerCapture(drag.id);
            events.abort();
            canvas.style.touchAction = touchAction;
            renderer.dispose();
        },
    };
}
