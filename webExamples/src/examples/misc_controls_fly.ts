import {
    AmbientLight, BufferGeometry, Color, DirectionalLight, Float32BufferAttribute, Mesh, MeshLambertMaterial,
    PerspectiveCamera, Points, PointsMaterial, Scene,
} from 'threekt';
import { SphereGeometry } from 'threekt/geometry';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import earthUrl from './assets/planets/earth_atmos_2048.jpg?url';
import cloudUrl from './assets/planets/earth_clouds_1024.png?url';
import moonUrl from './assets/planets/moon_1024.jpg?url';
import { fitPerspective } from './fitPerspective';
import { flyCamera } from './flyCamera';
import { loadImageTexture } from './loadImageTexture';

export async function createExample(canvas: HTMLCanvasElement) {
    const [earthMap, cloudMap, moonMap] = await Promise.all([earthUrl, cloudUrl, moonUrl].map(loadImageTexture));
    const radius = 6371;
    const camera = new PerspectiveCamera(25, 1, 50, 1e7);
    camera.position.z = radius * 5;
    const controls = flyCamera(canvas, camera, radius, true);
    const scene = new Scene();
    scene.add(new AmbientLight(Color.fromHex(0x626778), 0.8));
    const sun = new DirectionalLight(Color.fromHex(0xffffff), 3);
    sun.position.set(-1, 0, 1);
    scene.add(sun);
    const white = Color.fromHex(0xffffff);
    const geometry = new SphereGeometry(radius, 100, 50);
    const earthMaterial = new MeshLambertMaterial(white);
    earthMaterial.map = earthMap;
    const earth = new Mesh(geometry, earthMaterial);
    const cloudMaterial = new MeshLambertMaterial(white);
    cloudMaterial.map = cloudMap;
    cloudMaterial.transparent = true;
    cloudMaterial.depthWrite = false;
    const clouds = new Mesh(geometry, cloudMaterial);
    clouds.scale.setScalar(1.005);
    const moonMaterial = new MeshLambertMaterial(white);
    moonMaterial.map = moonMap;
    const moon = new Mesh(geometry, moonMaterial);
    moon.scale.setScalar(0.23);
    moon.position.set(radius * 4, radius * 0.4, -radius * 4);
    for (const mesh of [earth, clouds, moon]) scene.add(mesh);

    const stars = new Float32Array(6000 * 3);
    for (let i = 0; i < stars.length; i += 3) {
        const y = Math.random() * 2 - 1;
        const phi = Math.random() * Math.PI * 2;
        const horizontal = Math.sqrt(1 - y * y);
        const distance = radius * (80 + Math.random() * 30);
        stars.set([horizontal * Math.cos(phi) * distance, y * distance, horizontal * Math.sin(phi) * distance], i);
    }
    const starMaterial = new PointsMaterial(Color.fromHex(0x999999));
    starMaterial.size = 1;
    starMaterial.sizeAttenuation = false;
    scene.add(new Points(new BufferGeometry().setAttribute('position', new Float32BufferAttribute(stars, 3)), starMaterial));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update(seconds);
            earth.rotation.y = seconds * 0.03;
            clouds.rotation.y = seconds * 0.0375;
            moon.rotation.y = seconds * 0.02;
            renderer.render(scene, camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); },
    };
}
