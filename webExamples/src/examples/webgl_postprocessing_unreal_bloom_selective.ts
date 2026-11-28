import {
    AmbientLight, Color, DirectionalLight, DomInputSurface, IcosahedronGeometry, Mesh, MeshBasicMaterial,
    MeshPhongMaterial, OrbitControls, PerspectiveCamera, Raycaster, Scene, Vector2
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { exampleBloom } from './exampleBloom';
import { fitPerspective } from './fitPerspective';
import { setPointer } from './raycastPointer';

export function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    scene.add(new AmbientLight(Color.fromHex(0xffffff),1.5));
    const sun = new DirectionalLight(Color.fromHex(0xffffff),2);
    sun.position.set(3,5,4);
    scene.add(sun);
    const geometry = new IcosahedronGeometry(1,5);
    const black = new MeshBasicMaterial(Color.fromHex(0));
    const spheres = Array.from({length:50},(_,i) => {
        const color = new Color(1,1,1).setHSL(i/50,0.7,0.15 + Math.random()*0.1);
        const material = new MeshPhongMaterial(color);
        const mesh = new Mesh(geometry,material);
        mesh.position.set(Math.random()-0.5,Math.random()-0.5,Math.random()-0.5).normalize().multiplyScalar(2+Math.random()*4);
        mesh.scale.setScalar(0.5+Math.random()**2);
        scene.add(mesh);
        return {mesh,material,bloom: i % 4 === 0};
    });
    const meshes = spheres.map(sphere => sphere.mesh);
    const camera = new PerspectiveCamera(40,1,0.1,200);
    camera.position.set(0,0,19);
    const controls = new OrbitControls(camera,new DomInputSurface(canvas));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const bloom = exampleBloom(canvas,renderer,true);
    const ray = new Raycaster(), pointer = new Vector2();
    const events = new AbortController();
    let startX = 0, startY = 0;
    canvas.addEventListener('pointerdown',event => { startX = event.clientX; startY = event.clientY; },{signal:events.signal});
    canvas.addEventListener('pointerup',event => {
        if (event.button !== 0 || Math.hypot(event.clientX-startX,event.clientY-startY)>5) return;
        setPointer(canvas,event,pointer);
        ray.setFromCamera(pointer,camera);
        const hit = ray.intersectObjects(meshes,false)[0]?.object;
        const sphere = spheres.find(candidate => candidate.mesh===hit);
        if (sphere) sphere.bloom = !sphere.bloom;
    },{signal:events.signal});
    return {
        render() {
            if (!fitPerspective(canvas,camera,renderer)) return;
            controls.update();
            bloom.render(scene,camera,() => {
                for (const {mesh,material,bloom} of spheres) {
                    mesh.material = bloom ? material : black;
                    if (bloom) material.emissive.copyFrom(material.color).multiplyScalar(2.2);
                }
                try { renderer.render(scene,camera); }
                finally {
                    for (const {mesh,material} of spheres) {
                        mesh.material = material;
                        material.emissive.setHex(0);
                    }
                }
            });
        },
        dispose() { events.abort(); bloom.dispose(); controls.dispose(); renderer.dispose(); },
    };
}
