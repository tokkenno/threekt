import {
    AmbientLight, BufferGeometry, Color, DirectionalLight, Float32BufferAttribute, Group, Line, LineBasicMaterial,
    Mesh, MeshLambertMaterial, NURBSCurve, NURBSSurface, NURBSVolume, ParametricGeometry, PerspectiveCamera,
    Scene, Side, Vector3, Vector4, WebGL2Context, WebGLRenderer, Wrapping,
} from 'threekt';
import gridUrl from './assets/uv_grid_opengl.jpg';
import { fitPerspective } from './fitPerspective';
import { loadImageTexture } from './loadImageTexture';

export async function createExample(canvas: HTMLCanvasElement) {
    const texture = await loadImageTexture(gridUrl);
    texture.wrapS = texture.wrapT = Wrapping.Repeat;
    texture.anisotropy = 16;
    const material = new MeshLambertMaterial();
    material.map = texture;
    material.side = Side.Double;

    const camera = new PerspectiveCamera(50, 1, 1, 2000);
    camera.position.set(0, 150, 750);
    const scene = new Scene();
    scene.background = Color.fromHex(0xf0f0f0);
    scene.add(new AmbientLight(Color.fromHex(0xffffff)));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 3);
    light.position.set(1, 1, 1);
    scene.add(light);
    const group = new Group();
    group.position.y = 50;
    scene.add(group);

    const points = Array.from({ length: 20 }, () =>
        new Vector4(Math.random() * 400 - 200, Math.random() * 400, Math.random() * 400 - 200, 1));
    const knots = Float64Array.from([0, 0, 0, 0, ...points.map((_, i) => Math.min((i + 1) / 17, 1))]);
    const curve = new NURBSCurve(3, knots, points);
    const addLine = (points: readonly (Vector3 | Vector4)[], opacity: number) => {
        const vertices = new Float32Array(points.length * 3);
        points.forEach(({ x, y, z }, i) => vertices.set([x, y, z], i * 3));
        const geometry = new BufferGeometry().setAttribute('position', new Float32BufferAttribute(vertices, 3));
        const lineMaterial = new LineBasicMaterial(Color.fromHex(0x333333));
        lineMaterial.opacity = opacity;
        lineMaterial.transparent = opacity < 1;
        const line = new Line(geometry, lineMaterial);
        line.position.y = -100;
        group.add(line);
    };
    addLine(curve.getPoints(200).asJsReadonlyArrayView(), 1);
    addLine(points, 0.25);

    const uKnots = new Float64Array([0, 0, 0, 1, 1, 1]);
    const vKnots = new Float64Array([0, 0, 0, 0, 1, 1, 1, 1]);
    const wKnots = new Float64Array([0, 0, 1, 1]);
    const xs = [-200, 0, 200];
    const ys = [-200, -100, 100, 200];
    const heights = [[100, -200, 250, -100], [0, -100, 150, 0], [-100, 200, -250, 100]];
    const surface = new NURBSSurface(2, 3, uKnots, vKnots, xs.map((x, i) => ys.map((y, j) =>
        new Vector4(x, y, heights[i][j], i === 1 && (j === 1 || j === 2) ? 5 : 1))));
    const volume = new NURBSVolume(2, 3, 1, uKnots, vKnots, wKnots, xs.map(x => ys.map(y =>
        (x < 200 || y === -200 ? [-200, 200] : [0, 100]).map(z => new Vector4(x, y, z, 1)))));

    const addSurface = (evaluate: (u: number, v: number, target: Vector3) => void, x: number, scale: number) => {
        const mesh = new Mesh(new ParametricGeometry(evaluate, 20, 20), material);
        mesh.position.set(x, 100, 0);
        mesh.scale.setScalar(scale);
        group.add(mesh);
    };
    addSurface((u, v, target) => surface.getPoint(u, v, target), -400, 1);
    for (const w of [0, 0.5, 1]) addSurface((u, v, target) => volume.getPoint(u, v, w, target), 400, 0.5);
    addSurface((u, w, target) => volume.getPoint(u, 1, w, target), 400, 0.5);
    addSurface((v, w, target) => volume.getPoint(0, v, w, target), 400, 0.5);

    const events = new AbortController();
    const { signal } = events;
    const touchAction = canvas.style.touchAction;
    canvas.style.touchAction = 'none';
    let pointer: number | undefined;
    let startX = 0;
    let startRotation = 0;
    let targetRotation = 0;
    canvas.addEventListener('pointerdown', event => {
        if (event.button !== 0 || pointer !== undefined) return;
        pointer = event.pointerId;
        startX = event.clientX;
        startRotation = targetRotation;
        canvas.setPointerCapture(pointer);
    }, { signal });
    canvas.addEventListener('pointermove', event => {
        if (event.pointerId === pointer) targetRotation = startRotation + (event.clientX - startX) * 0.02;
    }, { signal });
    canvas.addEventListener('lostpointercapture', event => {
        if (event.pointerId === pointer) pointer = undefined;
    }, { signal });

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    let lastAspect = 0;
    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            if (camera.aspect !== lastAspect) {
                // Fit the side displays inside the gallery's narrower canvas.
                lastAspect = camera.aspect;
                const halfWidth = 620;
                camera.position.z = Math.max(750, 200 + halfWidth / (Math.tan(Math.PI * 25 / 180) * lastAspect));
                camera.far = Math.max(2000, camera.position.z + 1000);
                camera.updateProjectionMatrix();
            }
            group.rotation.y += (targetRotation - group.rotation.y) * 0.05;
            renderer.render(scene, camera);
        },
        dispose() {
            if (pointer !== undefined && canvas.hasPointerCapture(pointer)) canvas.releasePointerCapture(pointer);
            events.abort();
            canvas.style.touchAction = touchAction;
            renderer.dispose();
        },
    };
}
