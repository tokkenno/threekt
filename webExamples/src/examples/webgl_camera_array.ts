import {
    AmbientLight, ArrayCamera, Color, DirectionalLight, Euler, Mesh, MeshPhongMaterial, PerspectiveCamera,
    Scene,
} from 'threekt';
import { CylinderGeometry, PlaneGeometry } from 'threekt/geometry';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';

export async function createExample(canvas: HTMLCanvasElement) {
    const amount = 6;
    const scene = new Scene();
    scene.add(new AmbientLight(Color.fromHex(0x999999)));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 3);
    light.position.set(0.5, 0.5, 1);
    scene.add(light);
    const background = new Mesh(new PlaneGeometry(100, 100), new MeshPhongMaterial(Color.fromHex(0x000066)));
    background.position.z = -1;
    scene.add(background);
    const cylinder = new Mesh(new CylinderGeometry(0.5, 0.5, 1, 32), new MeshPhongMaterial(Color.fromHex(0xff0000)));
    scene.add(cylinder);

    const cameras = Array.from({ length: amount ** 2 }, (_, i) => {
        const camera = new PerspectiveCamera(40, 1, 0.1, 10);
        camera.position.set(((i % amount) / amount - 0.5) * 2, (0.5 - Math.floor(i / amount) / amount) * 2, 3);
        camera.lookAt(scene.position);
        return camera;
    });
    const array = ArrayCamera.create();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const rotation = new Euler();
    let width = 0, height = 0, ratio = 0;

    return {
        render(seconds: number) {
            const w = canvas.clientWidth, h = canvas.clientHeight, dpr = window.devicePixelRatio;
            if (w < amount || h < amount) return;
            if (width !== w || height !== h || ratio !== dpr) {
                width = w; height = h; ratio = dpr;
                renderer.setSize(width, height, ratio);
                const pixelWidth = canvas.width, pixelHeight = canvas.height;
                const viewports = cameras.map((camera, i) => {
                    camera.aspect = width / height;
                    camera.updateProjectionMatrix();
                    const column = i % amount, row = Math.floor(i / amount);
                    const x = Math.floor(column * pixelWidth / amount);
                    const y = Math.floor(row * pixelHeight / amount);
                    const right = Math.floor((column + 1) * pixelWidth / amount);
                    const top = Math.floor((row + 1) * pixelHeight / amount);
                    return new ArrayCamera.Viewport(x, y, right - x, top - y);
                });
                array.setViews(cameras, viewports);
            }
            cylinder.quaternion.setFromEuler(rotation.set(seconds * 0.3, 0, seconds * 0.6));
            renderer.render(scene, array);
        },
        dispose() { renderer.dispose(); },
    };
}
