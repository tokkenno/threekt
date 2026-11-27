import {
    BufferGeometry, Color, DomInputSurface, Line, LineBasicMaterial, Mesh, MeshBasicMaterial, Object3D, OrbitControls,
    PerspectiveCamera, Scene, ShapeGeometry, Side, WebGL2Context, WebGLRenderer, type Path,
} from 'threekt';
import { FontLoader } from 'threekt/loaders';
import fontUrl from './assets/fonts/helvetiker_regular.typeface.json?url';
import { fitPerspective } from './fitPerspective';

/** Port of Three.js examples/webgl_geometry_text_shapes.html: flat text from font shapes, with outlines. */
export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(45, 1, 1, 10000);
    camera.position.set(0, -400, 600);
    const scene = new Scene();
    scene.background = Color.fromHex(0xf0f0f0);

    const font = new FontLoader().parse(await (await fetch(fontUrl)).text());
    const color = Color.fromHex(0x006699);
    const matDark = new LineBasicMaterial(color);
    matDark.side = Side.Double;
    const matLite = new MeshBasicMaterial(color);
    matLite.transparent = true;
    matLite.opacity = 0.4;
    matLite.side = Side.Double;

    const message = '   Three.kt\nSimple text.';
    const shapes = font.generateShapes(message, 100);
    const geometry = new ShapeGeometry(shapes);
    geometry.computeBoundingBox();
    const xMid = -0.5 * (geometry.boundingBox!.max.x - geometry.boundingBox!.min.x);
    geometry.translate(xMid, 0, 0);

    // Filled text.
    const text = new Mesh(geometry, matLite);
    text.position.z = -150;
    scene.add(text);

    // Outlines of the shapes and their holes.
    const outlines: Path[] = [...shapes];
    for (const shape of shapes) outlines.push(...shape.holes.asJsReadonlyArrayView());
    const lineText = new Object3D();
    for (const shape of outlines) {
        const lineGeometry = new BufferGeometry().setFromPoints(shape.getPoints());
        lineGeometry.translate(xMid, 0, 0);
        lineText.add(new Line(lineGeometry, matDark));
    }
    scene.add(lineText);

    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.target.set(0, 0, 0);
    controls.update();

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() {
            controls.dispose();
            renderer.dispose();
        },
    };
}
