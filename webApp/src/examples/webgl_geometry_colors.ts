import {
    Color, ColorSpace, DirectionalLight, Euler, Float32BufferAttribute, IcosahedronGeometry,
    ImageTexture, Mesh, MeshBasicMaterial, MeshPhongMaterial, PerspectiveCamera, PlaneGeometry,
    Scene, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { fitPerspective } from './fitPerspective';
import { followPointer } from './followPointer';

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(20, 1, 1, 10000);
    camera.position.z = 1800;
    const scene = new Scene();
    scene.background = Color.fromHex(0xffffff);
    const light = new DirectionalLight(Color.fromHex(0xffffff), 3);
    light.position.set(0, 0, 1);
    scene.add(light);

    const shadow = document.createElement('canvas');
    shadow.width = shadow.height = 128;
    const context = shadow.getContext('2d')!;
    const gradient = context.createRadialGradient(64, 64, 0, 64, 64, 64);
    gradient.addColorStop(0.1, '#d2d2d2');
    gradient.addColorStop(1, '#ffffff');
    context.fillStyle = gradient;
    context.fillRect(0, 0, 128, 128);
    const shadowMaterial = new MeshBasicMaterial(Color.fromHex(0xffffff), new ImageTexture(shadow));
    const shadowGeometry = new PlaneGeometry(300, 300);

    const faceMaterial = new MeshPhongMaterial();
    faceMaterial.flatShading = faceMaterial.vertexColors = true;
    faceMaterial.shininess = 0;
    const wireframe = new MeshBasicMaterial(Color.fromHex(0));
    wireframe.wireframe = wireframe.transparent = true;
    const color = new Color(1, 1, 1);
    const radius = 200;

    [-400, 400, 0].forEach((x, index) => {
        const shadowMesh = new Mesh(shadowGeometry, shadowMaterial);
        shadowMesh.position.set(x, -250, 0);
        shadowMesh.quaternion.setFromEuler(new Euler(-Math.PI / 2, 0, 0));
        scene.add(shadowMesh);

        const geometry = new IcosahedronGeometry(radius, 1);
        const positions = geometry.getAttribute('position')!;
        const colors = new Float32Array(positions.count * 3);
        for (let i = 0; i < positions.count; i++) {
            const y = (positions.getY(i) / radius + 1) / 2;
            if (index === 0) color.setHSL(y, 1, 0.5, ColorSpace.SRGB);
            else if (index === 1) color.setHSL(0, y, 0.5, ColorSpace.SRGB);
            else color.setRGB(1, 0.8 - y, 0, ColorSpace.SRGB);
            colors.set([color.r, color.g, color.b], i * 3);
        }
        geometry.setAttribute('color', new Float32BufferAttribute(colors, 3));
        const mesh = new Mesh(geometry, faceMaterial);
        mesh.add(new Mesh(geometry, wireframe));
        mesh.position.x = x;
        if (index === 0) mesh.quaternion.setFromEuler(new Euler(-1.87, 0, 0));
        scene.add(mesh);
    });

    const pointer = followPointer(canvas);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            pointer.update(camera, scene.position);
            renderer.render(scene, camera);
        },
        dispose() {
            pointer.dispose();
            renderer.dispose();
        },
    };
}
