import {
    Color, ColorSpace, Euler, ExtrudeGeometry, Group, Line, LineBasicMaterial, Mesh, MeshPhongMaterial, Path,
    PerspectiveCamera, PointLight, Points, PointsMaterial, BufferGeometry, Scene, Shape, ShapeGeometry, Side, Vector2,
    WebGL2Context, WebGLRenderer, Wrapping, type Object3D, type Texture,
} from 'threekt';
import uvGridUrl from './assets/uv_grid_opengl.jpg?url';
import { examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

/**
 * Port of Three.js examples/webgl_geometry_shapes.html. Each shape appears, front to back, as a textured
 * flat shape, a colored flat shape, an extrusion, its outline, its evenly spaced outline, and both outlines
 * as points. Drag horizontally to turn the group.
 */
export async function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    scene.background = Color.fromHex(0xf0f0f0);
    const camera = new PerspectiveCamera(50, 1, 1, 1000);
    camera.position.set(0, 150, 500);
    scene.add(camera);
    camera.add(new PointLight(Color.fromHex(0xffffff), 2.5, 0, 0));

    const group = new Group();
    group.position.y = 50;
    scene.add(group);

    const texture: Texture = await loadImageTexture(uvGridUrl);
    texture.colorSpace = ColorSpace.SRGB;
    texture.wrapS = texture.wrapT = Wrapping.Repeat;
    texture.repeat.set(0.008, 0.008);

    const rotation = new Euler();
    function place(object: Object3D, x: number, y: number, z: number, rx: number, ry: number, rz: number, s: number) {
        object.position.set(x, y, z);
        object.quaternion.setFromEuler(rotation.set(rx, ry, rz));
        object.scale.set(s, s, s);
        group.add(object);
    }

    function addShape(shape: Shape, options: ExtrudeGeometry.Options, color: number,
        x: number, y: number, z: number, rx: number, ry: number, rz: number, s: number) {
        // Flat shape with texture.
        const textured = new MeshPhongMaterial();
        textured.side = Side.Double;
        textured.map = texture;
        place(new Mesh(new ShapeGeometry([shape]), textured), x, y, z - 175, rx, ry, rz, s);
        // Flat shape.
        const flat = new MeshPhongMaterial(Color.fromHex(color));
        flat.side = Side.Double;
        place(new Mesh(new ShapeGeometry([shape]), flat), x, y, z - 125, rx, ry, rz, s);
        // Extruded shape.
        place(new Mesh(new ExtrudeGeometry([shape], options), new MeshPhongMaterial(Color.fromHex(color))), x, y, z - 75, rx, ry, rz, s);
        addLineShape(shape, color, x, y, z, rx, ry, rz, s);
    }

    function addLineShape(shape: Path, color: number, x: number, y: number, z: number, rx: number, ry: number, rz: number, s: number) {
        // Lines and points from the sampled outline and from evenly spaced points.
        shape.autoClose = true;
        const points = shape.getPoints();
        const spacedPoints = shape.getSpacedPoints(50);
        const geometryPoints = new BufferGeometry().setFromPoints(points);
        const geometrySpacedPoints = new BufferGeometry().setFromPoints(spacedPoints);
        place(new Line(geometryPoints, new LineBasicMaterial(Color.fromHex(color))), x, y, z - 25, rx, ry, rz, s);
        place(new Line(geometrySpacedPoints, new LineBasicMaterial(Color.fromHex(color))), x, y, z + 25, rx, ry, rz, s);
        const pointsMaterial = () => {
            const material = new PointsMaterial(Color.fromHex(color));
            material.size = 4;
            return material;
        };
        place(new Points(geometryPoints, pointsMaterial()), x, y, z + 75, rx, ry, rz, s);
        place(new Points(geometrySpacedPoints, pointsMaterial()), x, y, z + 125, rx, ry, rz, s);
    }

    // California.
    const californiaPts = [
        [610, 320], [450, 300], [392, 392], [266, 438], [190, 570], [190, 600], [160, 620], [160, 650], [180, 640],
        [165, 680], [150, 670], [90, 737], [80, 795], [50, 835], [64, 870], [60, 945], [300, 945], [300, 743],
        [600, 473], [626, 425], [600, 370], [610, 320],
    ].map(([px, py]) => new Vector2(px, py).multiplyScalar(0.25));
    const californiaShape = new Shape(californiaPts);

    // Triangle.
    const triangleShape = new Shape().moveTo(80, 20).lineTo(40, 80).lineTo(120, 80).lineTo(80, 20) as Shape;

    // Heart.
    const x = 0, y = 0;
    const heartShape = new Shape().moveTo(x + 25, y + 25)
        .bezierCurveTo(x + 25, y + 25, x + 20, y, x, y)
        .bezierCurveTo(x - 30, y, x - 30, y + 35, x - 30, y + 35)
        .bezierCurveTo(x - 30, y + 55, x - 10, y + 77, x + 25, y + 95)
        .bezierCurveTo(x + 60, y + 77, x + 80, y + 55, x + 80, y + 35)
        .bezierCurveTo(x + 80, y + 35, x + 80, y, x + 50, y)
        .bezierCurveTo(x + 35, y, x + 25, y + 25, x + 25, y + 25) as Shape;

    // Square.
    const sqLength = 80;
    const squareShape = new Shape().moveTo(0, 0).lineTo(0, sqLength).lineTo(sqLength, sqLength).lineTo(sqLength, 0)
        .lineTo(0, 0) as Shape;

    // Rounded rectangle.
    const roundedRectShape = new Shape();
    ((ctx: Path, rx: number, ry: number, width: number, height: number, radius: number) => {
        ctx.moveTo(rx, ry + radius);
        ctx.lineTo(rx, ry + height - radius);
        ctx.quadraticCurveTo(rx, ry + height, rx + radius, ry + height);
        ctx.lineTo(rx + width - radius, ry + height);
        ctx.quadraticCurveTo(rx + width, ry + height, rx + width, ry + height - radius);
        ctx.lineTo(rx + width, ry + radius);
        ctx.quadraticCurveTo(rx + width, ry, rx + width - radius, ry);
        ctx.lineTo(rx + radius, ry);
        ctx.quadraticCurveTo(rx, ry, rx, ry + radius);
    })(roundedRectShape, 0, 0, 50, 50, 20);

    // Track.
    const trackShape = new Shape().moveTo(40, 40).lineTo(40, 160).absarc(60, 160, 20, Math.PI, 0, true)
        .lineTo(80, 40).absarc(60, 40, 20, 2 * Math.PI, Math.PI, true) as Shape;

    // Circle.
    const circleRadius = 40;
    const circleShape = new Shape().moveTo(0, circleRadius)
        .quadraticCurveTo(circleRadius, circleRadius, circleRadius, 0)
        .quadraticCurveTo(circleRadius, -circleRadius, 0, -circleRadius)
        .quadraticCurveTo(-circleRadius, -circleRadius, -circleRadius, 0)
        .quadraticCurveTo(-circleRadius, circleRadius, 0, circleRadius) as Shape;

    // Fish.
    const fishShape = new Shape().moveTo(x, y)
        .quadraticCurveTo(x + 50, y - 80, x + 90, y - 10)
        .quadraticCurveTo(x + 100, y - 10, x + 115, y - 40)
        .quadraticCurveTo(x + 115, y, x + 115, y + 40)
        .quadraticCurveTo(x + 100, y + 10, x + 90, y + 10)
        .quadraticCurveTo(x + 50, y + 80, x, y) as Shape;

    // Arc with a hole.
    const arcShape = new Shape().moveTo(50, 10).absarc(10, 10, 40, 0, Math.PI * 2, false) as Shape;
    const holePath = new Path().moveTo(20, 10).absarc(10, 10, 10, 0, Math.PI * 2, true);
    arcShape.holes.asJsArrayView().push(holePath);

    // Smiley.
    const smileyShape = new Shape().moveTo(80, 40).absarc(40, 40, 40, 0, Math.PI * 2, false) as Shape;
    const smileyEye1Path = new Path().moveTo(35, 20).absellipse(25, 20, 10, 10, 0, Math.PI * 2, true);
    const smileyEye2Path = new Path().moveTo(65, 20).absarc(55, 20, 10, 0, Math.PI * 2, true);
    const smileyMouthPath = new Path().moveTo(20, 40)
        .quadraticCurveTo(40, 60, 60, 40)
        .bezierCurveTo(70, 45, 70, 50, 60, 60)
        .quadraticCurveTo(40, 80, 20, 60)
        .quadraticCurveTo(5, 50, 20, 40);
    smileyShape.holes.asJsArrayView().push(smileyEye1Path, smileyEye2Path, smileyMouthPath);

    // Spline shape.
    const splineShape = new Shape().moveTo(0, 0)
        .splineThru([new Vector2(70, 20), new Vector2(80, 90), new Vector2(-30, 70), new Vector2(0, 0)]) as Shape;

    const extrudeSettings = Object.assign(new ExtrudeGeometry.Options(), {
        depth: 8, bevelEnabled: true, bevelSegments: 2, steps: 2, bevelSize: 1, bevelThickness: 1,
    });

    // addShape(shape, options, color, x, y, z, rx, ry, rz, scale)
    addShape(californiaShape, extrudeSettings, 0xf08000, -300, -100, 0, 0, 0, 0, 1);
    addShape(triangleShape, extrudeSettings, 0x8080f0, -180, 0, 0, 0, 0, 0, 1);
    addShape(roundedRectShape, extrudeSettings, 0x008000, -150, 150, 0, 0, 0, 0, 1);
    addShape(trackShape, extrudeSettings, 0x008080, 200, -100, 0, 0, 0, 0, 1);
    addShape(squareShape, extrudeSettings, 0x0040f0, 150, 100, 0, 0, 0, 0, 1);
    addShape(heartShape, extrudeSettings, 0xf00000, 60, 100, 0, 0, 0, Math.PI, 1);
    addShape(circleShape, extrudeSettings, 0x00f000, 120, 250, 0, 0, 0, 0, 1);
    addShape(fishShape, extrudeSettings, 0x404040, -60, 200, 0, 0, 0, 0, 1);
    addShape(smileyShape, extrudeSettings, 0xf000f0, -200, 250, 0, 0, 0, Math.PI, 1);
    addShape(arcShape, extrudeSettings, 0x804000, 150, 0, 0, 0, 0, 0, 1);
    addShape(splineShape, extrudeSettings, 0x808080, -50, -100, 0, 0, 0, 0, 1);
    addLineShape(holePath, 0x804000, 150, 0, 0, 0, 0, 0, 1);
    for (const hole of smileyShape.holes.asJsReadonlyArrayView()) addLineShape(hole, 0xf000f0, -200, 250, 0, 0, 0, Math.PI, 1);

    // Horizontal drags turn the group with easing.
    let targetRotation = 0;
    let groupRotation = 0;
    let targetRotationOnPointerDown = 0;
    let pointerXOnPointerDown = 0;
    let activePointer: number | undefined;
    const events = new AbortController();
    const previousTouchAction = canvas.style.touchAction;
    canvas.style.touchAction = 'none';
    canvas.addEventListener('pointerdown', event => {
        if (!event.isPrimary) return;
        activePointer = event.pointerId;
        canvas.setPointerCapture(event.pointerId);
        pointerXOnPointerDown = event.clientX;
        targetRotationOnPointerDown = targetRotation;
    }, { signal: events.signal });
    canvas.addEventListener('pointermove', event => {
        if (event.pointerId !== activePointer) return;
        targetRotation = targetRotationOnPointerDown + (event.clientX - pointerXOnPointerDown) * 0.02;
    }, { signal: events.signal });
    canvas.addEventListener('lostpointercapture', event => {
        if (event.pointerId === activePointer) activePointer = undefined;
    }, { signal: events.signal });

    const panel = examplePanel(canvas);
    panel.append('Drag horizontally to turn the shapes.');

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            groupRotation += (targetRotation - groupRotation) * 0.05;
            group.quaternion.setFromEuler(rotation.set(0, groupRotation, 0));
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() {
            events.abort();
            canvas.style.touchAction = previousTouchAction;
            panel.remove();
            renderer.dispose();
        },
    };
}
