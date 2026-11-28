import {
    AmbientLight, CameraHelper, CatmullRomCurve3, CinquefoilKnot, Color, DecoratedTorusKnot4a, DecoratedTorusKnot4b,
    DecoratedTorusKnot5a, DecoratedTorusKnot5c, DirectionalLight, DomInputSurface, FigureEightPolynomialKnot, GrannyKnot,
    HeartCurve, HelixCurve, KnotCurve, Mesh, MeshBasicMaterial, MeshLambertMaterial, Object3D, OrbitControls,
    PerspectiveCamera, Scene, SphereGeometry, TorusKnot, TrefoilKnot, TrefoilPolynomialKnot, TubeGeometry, Vector3,
    VivianiCurve, type Curve,
} from 'threekt';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import { examplePanel } from './exampleControls';

const v = (x: number, y: number, z: number) => new Vector3(x, y, z);

/**
 * Port of Three.js examples/webgl_geometry_extrude_splines.html. HTML controls replace lil-gui. The spline
 * camera rides along the tube, offset along its Frenet frame, and can be viewed directly or as a helper.
 */
export function createExample(canvas: HTMLCanvasElement) {
    const direction = new Vector3();
    const binormal = new Vector3();
    const normal = new Vector3();
    const position = new Vector3();
    const lookAt = new Vector3();

    const pipeSpline = new CatmullRomCurve3([
        v(0, 10, -10), v(10, 0, -10), v(20, 0, 0), v(30, 0, 10), v(30, 0, 20), v(20, 0, 30), v(10, 0, 30),
        v(0, 0, 30), v(-10, 10, 30), v(-10, 20, 30), v(0, 30, 30), v(10, 30, 30), v(20, 30, 15), v(10, 30, 10),
        v(0, 30, 10), v(-10, 20, 10), v(-10, 10, 10), v(0, 0, 10), v(10, -10, 10), v(20, -15, 10), v(30, -15, 10),
        v(40, -15, 10), v(50, -15, 10), v(60, 0, 10), v(70, 0, 0), v(80, 0, 0), v(90, 0, 0), v(100, 0, 0),
    ]);
    const sampleClosedSpline = new CatmullRomCurve3([
        v(0, -40, -40), v(0, 40, -40), v(0, 140, -40), v(0, 40, 40), v(0, -40, 40),
    ], true, CatmullRomCurve3.CurveType.CatmullRom);

    // Curve instances by name.
    const splines: Record<string, Curve<Vector3>> = {
        GrannyKnot: new GrannyKnot(),
        HeartCurve: new HeartCurve(3.5),
        VivianiCurve: new VivianiCurve(70),
        KnotCurve: new KnotCurve(),
        HelixCurve: new HelixCurve(),
        TrefoilKnot: new TrefoilKnot(),
        TorusKnot: new TorusKnot(20),
        CinquefoilKnot: new CinquefoilKnot(20),
        TrefoilPolynomialKnot: new TrefoilPolynomialKnot(14),
        FigureEightPolynomialKnot: new FigureEightPolynomialKnot(),
        DecoratedTorusKnot4a: new DecoratedTorusKnot4a(),
        DecoratedTorusKnot4b: new DecoratedTorusKnot4b(),
        DecoratedTorusKnot5a: new DecoratedTorusKnot5a(),
        DecoratedTorusKnot5c: new DecoratedTorusKnot5c(),
        PipeSpline: pipeSpline,
        SampleClosedSpline: sampleClosedSpline,
    };

    const params = {
        spline: 'GrannyKnot',
        scale: 4,
        extrusionSegments: 100,
        radiusSegments: 3,
        closed: true,
        animationView: false,
        lookAhead: false,
        cameraHelper: false,
    };

    const material = new MeshLambertMaterial(Color.fromHex(0xff00ff));
    const wireframeMaterial = new MeshBasicMaterial(Color.fromHex(0x000000));
    wireframeMaterial.opacity = 0.3;
    wireframeMaterial.wireframe = true;
    wireframeMaterial.transparent = true;

    const camera = new PerspectiveCamera(50, 1, 0.01, 10000);
    camera.position.set(0, 50, 500);
    const scene = new Scene();
    scene.background = Color.fromHex(0xf0f0f0);
    scene.add(new AmbientLight(Color.fromHex(0xffffff)));
    const light = new DirectionalLight(Color.fromHex(0xffffff), 1.5);
    light.position.set(0, 0, 1);
    scene.add(light);

    const parent = new Object3D();
    scene.add(parent);
    const splineCamera = new PerspectiveCamera(84, 1, 0.01, 1000);
    parent.add(splineCamera);
    const cameraHelper = new CameraHelper(splineCamera);
    scene.add(cameraHelper);

    let tubeGeometry!: TubeGeometry;
    let mesh: Mesh | undefined;
    function addTube() {
        if (mesh) parent.remove(mesh);
        const extrudePath = splines[params.spline];
        tubeGeometry = new TubeGeometry(extrudePath, params.extrusionSegments, 2, params.radiusSegments, params.closed);
        mesh = new Mesh(tubeGeometry, material);
        mesh.add(new Mesh(tubeGeometry, wireframeMaterial));
        parent.add(mesh);
        setScale();
    }
    function setScale() {
        mesh?.scale.set(params.scale, params.scale, params.scale);
    }
    addTube();

    // Debug camera marker.
    const cameraEye = new Mesh(new SphereGeometry(5), new MeshBasicMaterial(Color.fromHex(0xdddddd)));
    parent.add(cameraEye);
    function animateCamera() {
        cameraHelper.visible = params.cameraHelper;
        cameraEye.visible = params.cameraHelper;
    }
    animateCamera();

    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 100;
    controls.maxDistance = 2000;

    // Controls in place of lil-gui.
    const panel = examplePanel(canvas);
    const heading = (text: string) => {
        const p = document.createElement('p');
        p.textContent = text;
        p.style.fontWeight = '600';
        panel.append(p);
    };
    const row = (label: string, input: HTMLElement, output?: HTMLElement) => {
        const element = document.createElement('label');
        element.className = 'example-setting';
        element.append(label);
        if (output) element.append(output);
        element.append(input);
        panel.append(element);
    };
    const select = (name: keyof typeof params, options: string[], change: () => void) => {
        const input = document.createElement('select');
        for (const option of options) input.add(new Option(option, option));
        input.value = String(params[name]);
        input.style.gridColumn = '1 / -1';
        input.addEventListener('change', () => { (params as Record<string, unknown>)[name] = input.value; change(); });
        row(name, input);
    };
    const slider = (name: keyof typeof params, min: number, max: number, step: number, change: () => void) => {
        const input = document.createElement('input');
        input.type = 'range';
        input.min = String(min);
        input.max = String(max);
        input.step = String(step);
        input.value = String(params[name]);
        const output = document.createElement('span');
        output.textContent = input.value;
        input.addEventListener('input', () => { output.textContent = input.value; });
        input.addEventListener('change', () => { (params as Record<string, unknown>)[name] = input.valueAsNumber; change(); });
        row(name, input, output);
    };
    const toggle = (name: keyof typeof params, change: () => void) => {
        const input = document.createElement('input');
        input.type = 'checkbox';
        input.checked = Boolean(params[name]);
        input.addEventListener('change', () => { (params as Record<string, unknown>)[name] = input.checked; change(); });
        row(name, input);
    };
    heading('Geometry');
    select('spline', Object.keys(splines), addTube);
    slider('scale', 2, 10, 2, setScale);
    slider('extrusionSegments', 50, 500, 50, addTube);
    slider('radiusSegments', 2, 12, 1, addTube);
    toggle('closed', addTube);
    heading('Camera');
    toggle('animationView', animateCamera);
    toggle('lookAhead', animateCamera);
    toggle('cameraHelper', animateCamera);

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            const width = canvas.clientWidth;
            const height = canvas.clientHeight;
            if (width === 0 || height === 0) return;
            const aspect = width / height;
            if (camera.aspect !== aspect) {
                camera.aspect = splineCamera.aspect = aspect;
                camera.updateProjectionMatrix();
                splineCamera.updateProjectionMatrix();
            }
            renderer.setSize(width, height, window.devicePixelRatio);

            // Animate the camera along the spline.
            const time = Date.now();
            const looptime = 20 * 1000;
            const t = (time % looptime) / looptime;
            const path = tubeGeometry.path;
            path.getPointAt(t, position);
            position.multiplyScalar(params.scale);

            // Interpolate the binormal between frames.
            const segments = tubeGeometry.tangents.length;
            const pickt = t * segments;
            const pick = Math.floor(pickt);
            const pickNext = (pick + 1) % segments;
            binormal.subVectors(tubeGeometry.binormals[pickNext], tubeGeometry.binormals[pick]);
            binormal.multiplyScalar(pickt - pick).add(tubeGeometry.binormals[pick]);
            path.getTangentAt(t, direction);
            const offset = 15;
            normal.copyFrom(binormal).cross(direction);

            // Move the camera by the offset along its normal.
            position.add(normal.copy().multiplyScalar(offset));
            splineCamera.position.copyFrom(position);
            cameraEye.position.copyFrom(position);

            // Use the arc length to stabilize the look-ahead target.
            path.getPointAt((t + 30 / path.getLength()) % 1, lookAt);
            lookAt.multiplyScalar(params.scale);

            // Orient the camera with the normal as its up direction.
            if (!params.lookAhead) lookAt.copyFrom(position).add(direction);
            splineCamera.matrix.lookAt(splineCamera.position, lookAt, normal);
            splineCamera.quaternion.setFromRotationMatrix(splineCamera.matrix);

            cameraHelper.update();
            renderer.render(scene, params.animationView ? splineCamera : camera);
        },
        dispose() {
            panel.remove();
            controls.dispose();
            renderer.dispose();
        },
    };
}
