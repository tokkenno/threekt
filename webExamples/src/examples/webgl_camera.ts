import {
    BufferGeometry, CameraHelper, Color, Euler, Float32BufferAttribute, Group, Mesh, MeshBasicMaterial,
    OrthographicCamera, PerspectiveCamera, Points, PointsMaterial, Scene, SphereGeometry, WebGL2Context, WebGLRenderer,
} from 'threekt';

export async function createExample(canvas: HTMLCanvasElement) {
    const scene = new Scene();
    const observer = new PerspectiveCamera(50, 1, 1, 10000);
    observer.position.z = 2500;
    const perspective = new PerspectiveCamera(50, 1, 150, 1000);
    const orthographic = new OrthographicCamera(-300, 300, 300, -300, 150, 1000);
    const rig = new Group();
    const turn = new Euler(0, Math.PI, 0);
    for (const camera of [perspective, orthographic]) {
        camera.quaternion.setFromEuler(turn);
        rig.add(camera);
    }
    scene.add(rig);
    const perspectiveHelper = new CameraHelper(perspective);
    const orthographicHelper = new CameraHelper(orthographic);
    scene.add(perspectiveHelper);
    scene.add(orthographicHelper);

    function sphere(radius: number, hex: number) {
        const material = new MeshBasicMaterial(Color.fromHex(hex));
        material.wireframe = true;
        return new Mesh(new SphereGeometry(radius, 16, 8), material);
    }
    const object = sphere(100, 0xffffff);
    const satellite = sphere(50, 0x00ff00);
    satellite.position.y = 150;
    object.add(satellite);
    scene.add(object);
    const marker = sphere(5, 0x0000ff);
    marker.position.z = 150;
    rig.add(marker);

    const stars = Float32Array.from({ length: 30000 }, () => 2000 * (0.5 - Math.random()));
    const geometry = new BufferGeometry().setAttribute('position', new Float32BufferAttribute(stars, 3));
    scene.add(new Points(geometry, new PointsMaterial(Color.fromHex(0x888888))));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    renderer.setScissorTest(true);
    const events = new AbortController();
    const panel = document.createElement('aside');
    panel.className = 'example-panel';
    const select = document.createElement('select');
    select.setAttribute('aria-label', 'Projection');
    select.add(new Option('Perspective (P)', 'perspective'));
    select.add(new Option('Orthographic (O)', 'orthographic'));
    panel.append(select);
    canvas.parentElement?.append(panel);
    document.addEventListener('keydown', event => {
        if (event.code === 'KeyO') select.value = 'orthographic';
        if (event.code === 'KeyP') select.value = 'perspective';
    }, { signal: events.signal });
    const black = Color.fromHex(0), grey = Color.fromHex(0x111111);

    return {
        render(seconds: number) {
            const width = canvas.clientWidth, height = canvas.clientHeight;
            if (width < 2 || !height) return;
            renderer.setSize(width, height, window.devicePixelRatio);
            const middle = Math.floor(width / 2);
            const r = seconds * 0.5;
            object.position.set(700 * Math.cos(r), 700 * Math.sin(r), 700 * Math.sin(r));
            satellite.position.x = 70 * Math.cos(2 * r);
            satellite.position.z = 70 * Math.sin(r);
            rig.lookAt(object.position);
            observer.aspect = (width - middle) / height;
            observer.updateProjectionMatrix();
            const ortho = select.value === 'orthographic';
            const active = ortho ? orthographic : perspective;
            const helper = ortho ? orthographicHelper : perspectiveHelper;
            if (ortho) {
                orthographic.left = -300 * middle / height;
                orthographic.right = -orthographic.left;
            } else {
                perspective.aspect = middle / height;
                perspective.fov = 35 + 30 * Math.sin(r / 2);
            }
            active.far = object.position.length();
            active.updateProjectionMatrix();
            perspectiveHelper.visible = orthographicHelper.visible = false;
            renderer.setClearColor(black);
            renderer.setViewport(0, 0, middle, height);
            renderer.setScissor(0, 0, middle, height);
            renderer.render(scene, active);

            helper.update();
            helper.visible = true;
            renderer.setClearColor(grey);
            renderer.setViewport(middle, 0, width - middle, height);
            renderer.setScissor(middle, 0, width - middle, height);
            renderer.render(scene, observer);
        },
        dispose() { events.abort(); panel.remove(); renderer.dispose(); },
    };
}
