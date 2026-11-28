import {
    BoxGeometry, Color, DomInputSurface, EdgesGeometry, ExtrudeGeometry, HemisphereLight, LineBasicMaterial,
    LineSegments, Mesh, MeshLambertMaterial, OrbitControls, Path, PerspectiveCamera, Scene, Shape,
    Vector2,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { cssPanel, exampleCss3d } from './exampleCss3d';
import { fitPerspective } from './fitPerspective';

function rectangle(w: number, h: number) {
    return [new Vector2(-w / 2, -h / 2), new Vector2(w / 2, -h / 2), new Vector2(w / 2, h / 2),
        new Vector2(-w / 2, h / 2), new Vector2(-w / 2, -h / 2)];
}

export function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    const camera = new PerspectiveCamera(50, 1, 1, 10000);
    camera.position.set(-1000, 500, 1500);
    const css = exampleCss3d(canvas, '#f0f0f0', true);
    const oldStyle = canvas.style.cssText;
    Object.assign(canvas.style, { position: 'relative', background: 'transparent' });
    const width = 1024, height = 768, border = 32;
    const screen = cssPanel(width, height, '#000');
    const iframe = document.createElement('iframe');
    iframe.title = 'Animation keyframes';
    iframe.src = '?example=misc_animation_keys&embed=1';
    screen.append(iframe);
    scene.add(css.add(screen));

    const roomMaterial = new LineBasicMaterial(Color.fromHex(0x000000));
    roomMaterial.transparent = true;
    roomMaterial.opacity = 0.2;
    scene.add(new LineSegments(new EdgesGeometry(new BoxGeometry(4000, 2000, 4000, 10, 5, 10)), roomMaterial));
    scene.add(new HemisphereLight(Color.fromHex(0xffffff), Color.fromHex(0x444444), 4));
    const shape = new Shape(rectangle(width + border * 2, height + border * 2));
    shape.holes.asJsArrayView().push(new Path(rectangle(width, height)));
    const options = Object.assign(new ExtrudeGeometry.Options(), { depth: border, bevelEnabled: false });
    const material = new MeshLambertMaterial(Color.fromHex(0x2200ff));
    const frame = new Mesh(new ExtrudeGeometry([shape], options), material);
    frame.position.z = -border / 2;
    const back = new Mesh(new BoxGeometry(width + 2 * border, height + 2 * border, 8), material);
    back.position.z = -border / 2 - 4;
    scene.add(frame);
    // The DOM screen sits behind the WebGL canvas. The solid backing appears when viewed from behind.
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.enableDamping = true;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    renderer.setClearColor(Color.fromHex(0x000000), 0);
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            back.visible = camera.position.z < 0;
            if (back.visible && back.parent === null) scene.add(back);
            renderer.render(scene, camera);
            css.render(scene, camera);
        },
        dispose() { renderer.dispose(); controls.dispose(); css.dispose(); canvas.style.cssText = oldStyle; },
    };
}
