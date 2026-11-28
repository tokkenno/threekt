import {
    AmbientLight, CatmullRomCurve3, Color, DomInputSurface, ExtrudeGeometry, Mesh, MeshLambertMaterial,
    PerspectiveCamera, PointLight, Scene, Shape, TrackballControls, Vector2, Vector3,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { fitPerspective } from './fitPerspective';

export function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(45, 1, 1, 1000);
    camera.position.z = 500;
    const scene = new Scene();
    scene.background = Color.fromHex(0x222222);
    scene.add(new AmbientLight(Color.fromHex(0x666666)));
    const light = new PointLight(Color.fromHex(0xffffff), 3, 0, 0);
    light.position.copyFrom(camera.position);
    scene.add(light);

    const closed = new CatmullRomCurve3([
        new Vector3(-60, -100, 60), new Vector3(-60, 20, 60), new Vector3(-60, 120, 60),
        new Vector3(60, 20, -60), new Vector3(60, -100, -60),
    ], true, CatmullRomCurve3.CurveType.CatmullRom);
    const random = new CatmullRomCurve3(Array.from({ length: 10 }, (_, i) =>
        new Vector3((i - 4.5) * 50, Math.random() * 100 - 50, Math.random() * 100 - 50)));
    const polygon = (count: number, radiusAt: (i: number) => number) => new Shape(Array.from({ length: count }, (_, i) => {
        const angle = i / count * 2 * Math.PI;
        return new Vector2(Math.cos(angle) * radiusAt(i), Math.sin(angle) * radiusAt(i));
    }));
    const triangle = polygon(3, () => 20);
    const star = polygon(10, i => i % 2 === 0 ? 20 : 10);
    const red = new MeshLambertMaterial(Color.fromHex(0xb00000));
    const orange = new MeshLambertMaterial(Color.fromHex(0xff8000));

    const closedOptions = new ExtrudeGeometry.Options();
    Object.assign(closedOptions, { steps: 100, bevelEnabled: false, extrudePath: closed });
    const randomOptions = new ExtrudeGeometry.Options();
    Object.assign(randomOptions, { steps: 200, bevelEnabled: false, extrudePath: random });
    const bevelOptions = new ExtrudeGeometry.Options();
    Object.assign(bevelOptions, { depth: 20, steps: 1, bevelEnabled: true, bevelThickness: 2, bevelSize: 4, bevelSegments: 1 });
    scene.add(new Mesh(new ExtrudeGeometry([triangle], closedOptions), red));
    scene.add(new Mesh(new ExtrudeGeometry([star], randomOptions), orange));
    const beveled = Mesh.withMaterials(new ExtrudeGeometry([star], bevelOptions), [red, orange]);
    beveled.position.set(50, 100, 50);
    scene.add(beveled);

    const controls = new TrackballControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 200;
    controls.maxDistance = 500;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            renderer.render(scene, camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); },
    };
}
