import {
    BoxGeometry, Color, DirectionalLight, Euler, Mesh, MeshPhongMaterial, OrthographicCamera, PerspectiveCamera,
    Raycaster, Scene,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { raycastPointer } from './raycastPointer';

export function createInteractiveCubes(canvas: HTMLCanvasElement, ortho: boolean) {
    const camera = ortho ? new OrthographicCamera(-25, 25, 25, -25, 0.1, 100) : new PerspectiveCamera(70, 1, 0.1, 100);
    const scene = new Scene();
    scene.background = Color.fromHex(0xf0f0f0);
    const light = new DirectionalLight(Color.fromHex(0xffffff), 3);
    light.position.set(1, 1, 1).normalize();
    scene.add(light);
    const geometry = new BoxGeometry();
    const cubes = Array.from({ length: 2000 }, () => {
        const material = new MeshPhongMaterial(Color.fromHex(Math.floor(Math.random() * 0x1000000)));
        material.specular.setHex(0);
        const mesh = new Mesh(geometry, material);
        mesh.position.set(Math.random() * 40 - 20, Math.random() * 40 - 20, Math.random() * 40 - 20);
        mesh.quaternion.setFromEuler(new Euler(Math.random() * 2 * Math.PI, Math.random() * 2 * Math.PI, Math.random() * 2 * Math.PI));
        mesh.scale.set(Math.random() + 0.5, Math.random() + 0.5, Math.random() + 0.5);
        scene.add(mesh);
        return mesh;
    });
    const input = raycastPointer(canvas);
    const raycaster = new Raycaster();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    let highlighted: Mesh | undefined;

    return {
        render(seconds: number) {
            const width = canvas.clientWidth, height = canvas.clientHeight;
            if (!width || !height) return;
            renderer.setSize(width, height, window.devicePixelRatio);
            const aspect = width / height;
            if (camera instanceof OrthographicCamera) {
                camera.left = -25 * aspect;
                camera.right = 25 * aspect;
            } else {
                camera.aspect = aspect;
            }
            camera.updateProjectionMatrix();
            const theta = seconds * Math.PI / 30;
            const radius = ortho ? 25 : 5;
            camera.position.set(radius * Math.sin(theta), radius * Math.sin(theta), radius * Math.cos(theta));
            camera.lookAt(scene.position);
            raycaster.setFromCamera(input.pointer, camera);
            const mesh = raycaster.intersectObjects(cubes, false)[0]?.object as Mesh | undefined;
            if (mesh !== highlighted) {
                if (highlighted) (highlighted.material as MeshPhongMaterial).emissive.setHex(0);
                if (mesh) (mesh.material as MeshPhongMaterial).emissive.setHex(0xff0000);
                highlighted = mesh;
            }
            renderer.render(scene, camera);
        },
        dispose() { input.dispose(); renderer.dispose(); },
    };
}
