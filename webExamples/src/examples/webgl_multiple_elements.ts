import { BoxGeometry, CylinderGeometry, PolyhedronGeometry, SphereGeometry } from 'threekt/geometry';
import {
    Color, DirectionalLight, Euler, HemisphereLight, Mesh, MeshPhongMaterial, PerspectiveCamera, Scene,
} from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';

function dodecahedron() {
    const t = (1 + Math.sqrt(5)) / 2;
    const r = 1 / t;
    const vertices = new Float64Array([
        // (±1, ±1, ±1)
        -1, -1, -1, -1, -1, 1,
        -1, 1, -1, -1, 1, 1,
        1, -1, -1, 1, -1, 1,
        1, 1, -1, 1, 1, 1,

        // (0, ±1/φ, ±φ)
        0, -r, -t, 0, -r, t,
        0, r, -t, 0, r, t,

        // (±1/φ, ±φ, 0)
        -r, -t, 0, -r, t, 0,
        r, -t, 0, r, t, 0,

        // (±φ, 0, ±1/φ)
        -t, 0, -r, t, 0, -r,
        -t, 0, r, t, 0, r
    ]);

    const indices = new Int32Array([
        3, 11, 7,   3, 7, 15,   3, 15, 13,
        7, 19, 17,  7, 17, 6,   7, 6, 15,
        17, 4, 8,   17, 8, 10,  17, 10, 6,
        8, 0, 16,   8, 16, 2,   8, 2, 10,
        0, 12, 1,   0, 1, 18,   0, 18, 16,
        6, 10, 2,   6, 2, 13,   6, 13, 15,
        2, 16, 18,  2, 18, 3,   2, 3, 13,
        18, 1, 9,   18, 9, 11,  18, 11, 3,
        4, 14, 12,  4, 12, 0,   4, 0, 8,
        11, 9, 5,   11, 5, 19,  11, 19, 7,
        19, 5, 14,  19, 14, 4,  19, 4, 17,
        1, 12, 14,  1, 14, 5,   1, 5, 9
    ]);
    return new PolyhedronGeometry(vertices, indices, 0.5);
}

export function createExample(canvas: HTMLCanvasElement) {
    const list = document.createElement('article');
    list.className = 'element-scenes';
    canvas.after(list);
    const geometries = [new BoxGeometry(1, 1, 1), new SphereGeometry(0.5, 12, 8),
        dodecahedron(), new CylinderGeometry(0.5, 0.5, 1, 12, 1)];
    const views = Array.from({ length: 40 }, (_, index) => {
        const cell = document.createElement('figure');
        const viewport = document.createElement('div');
        const label = document.createElement('figcaption');
        label.textContent = ['Box', 'Sphere', 'Dodecahedron', 'Cylinder'][index % geometries.length];
        cell.append(viewport, label);
        list.append(cell);

        const scene = new Scene();
        scene.background = Color.fromHex(0xf4f4f4);
        const camera = new PerspectiveCamera(50, 1, 1, 10);
        camera.position.set(1.5, 1, 2);
        const controls = new OrbitControls(camera, new DomInputSurface(viewport));
        controls.enablePan = controls.enableZoom = false;
        const material = new MeshPhongMaterial(new Color(1, 1, 1).setHSL(Math.random(), 1, 0.65));
        const mesh = new Mesh(geometries[index % geometries.length], material);
        scene.add(mesh);
        scene.add(new HemisphereLight(Color.fromHex(0xaaaaaa), Color.fromHex(0x444444), 3));
        const light = new DirectionalLight(Color.fromHex(0xffffff), 1.5);
        light.position.set(1, 1, 1);
        scene.add(light);
        return { viewport, scene, camera, controls, mesh };
    });

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const clear = new Scene();
    const rotation = new Euler();
    clear.background = Color.fromHex(0xffffff);
    return {
        render(seconds: number) {
            const width = canvas.clientWidth, height = canvas.clientHeight;
            if (!width || !height) return;
            const bounds = canvas.getBoundingClientRect();
            renderer.setSize(width, height, window.devicePixelRatio);
            renderer.resetViewport();
            renderer.render(clear, views[0].camera);
            renderer.setScissorTest(true);
            for (const view of views) {
                const rect = view.viewport.getBoundingClientRect();
                if (rect.bottom <= bounds.top || rect.top >= bounds.bottom || !rect.width || !rect.height) continue;
                const left = Math.round(rect.left - bounds.left);
                const bottom = Math.round(bounds.bottom - rect.bottom);
                const w = Math.round(rect.width), h = Math.round(rect.height);
                renderer.setViewport(left, bottom, w, h);
                renderer.setScissor(left, bottom, w, h);
                view.mesh.quaternion.setFromEuler(rotation.set(0, seconds * 0.5, 0));
                renderer.render(view.scene, view.camera);
            }
            renderer.setScissorTest(false);
        },
        dispose() {
            list.remove();
            views.forEach(view => view.controls.dispose());
            renderer.dispose();
        },
    };
}
