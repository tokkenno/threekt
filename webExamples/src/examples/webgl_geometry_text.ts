import {
    Color, DirectionalLight, Euler, Fog, Group, Mesh, MeshBasicMaterial, MeshPhongMaterial, PerspectiveCamera,
    PlaneGeometry, PointLight, Scene, Vector3, WebGL2Context, WebGLRenderer, type Material,
} from 'threekt';
import { FontLoader, TextGeometry, type Font } from 'threekt/loaders';
import { examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';

const fontUrls = import.meta.glob('./assets/fonts/**/*.typeface.json', { query: '?url', import: 'default', eager: true }) as
    Record<string, string>;

/**
 * Port of Three.js examples/webgl_geometry_text.html. Type to replace the text and Backspace to delete;
 * drag horizontally to turn it. Buttons replace lil-gui, and keydown events replace the deprecated keypress.
 */
export function createExample(canvas: HTMLCanvasElement) {
    let firstLetter = true;
    let text = 'three.kt';
    let bevelEnabled = true;
    let font: Font | undefined;
    let fontName = 'optimer'; // helvetiker, optimer, gentilis, droid sans, droid serif
    let fontWeight = 'bold'; // regular, bold
    const depth = 20, size = 70, hover = 30, curveSegments = 4, bevelThickness = 2, bevelSize = 1.5;
    const mirror = true;
    const fontNames = ['helvetiker', 'optimer', 'gentilis', 'droid/droid_sans', 'droid/droid_serif'];
    let fontIndex = 1;

    const camera = new PerspectiveCamera(30, 1, 1, 1500);
    camera.position.set(0, 400, 700);
    const cameraTarget = new Vector3(0, 150, 0);
    const scene = new Scene();
    scene.background = Color.fromHex(0x000000);
    scene.fog = new Fog(Color.fromHex(0x000000), 250, 1400);

    const dirLight = new DirectionalLight(Color.fromHex(0xffffff), 0.4);
    dirLight.position.set(0, 0, 1).normalize();
    scene.add(dirLight);
    const pointLight = new PointLight(Color.fromHex(0xffffff), 4.5, 0, 0);
    pointLight.color.setHSL(Math.random(), 1, 0.5);
    pointLight.position.set(0, 100, 90);
    scene.add(pointLight);

    const front = new MeshPhongMaterial(Color.fromHex(0xffffff));
    front.flatShading = true;
    const materials: Material[] = [front, new MeshPhongMaterial(Color.fromHex(0xffffff))]; // Front and side.

    const group = new Group();
    group.position.y = 100;
    scene.add(group);

    const planeMaterial = new MeshBasicMaterial(Color.fromHex(0xffffff));
    planeMaterial.opacity = 0.5;
    planeMaterial.transparent = true;
    const plane = new Mesh(new PlaneGeometry(10000, 10000), planeMaterial);
    plane.position.y = 100;
    plane.quaternion.setFromEuler(new Euler(-Math.PI / 2, 0, 0));
    scene.add(plane);

    let textMesh1: Mesh | undefined;
    let textMesh2: Mesh | undefined;
    const fonts = new Map<string, Font>();

    async function loadFont() {
        const key = `${fontName}_${fontWeight}`;
        let loaded = fonts.get(key);
        if (!loaded) {
            const url = fontUrls[`./assets/fonts/${key}.typeface.json`];
            loaded = new FontLoader().parse(await (await fetch(url)).text());
            fonts.set(key, loaded);
        }
        // Ignore fonts that finish loading after a newer selection.
        if (key !== `${fontName}_${fontWeight}`) return;
        font = loaded;
        refreshText();
    }

    function createText() {
        const textGeo = new TextGeometry(text, Object.assign(new TextGeometry.Parameters(), {
            font, size, depth, curveSegments, bevelThickness, bevelSize, bevelEnabled,
        }));
        textGeo.computeBoundingBox();
        const centerOffset = -0.5 * (textGeo.boundingBox!.max.x - textGeo.boundingBox!.min.x);
        textMesh1 = Mesh.withMaterials(textGeo, materials);
        textMesh1.position.set(centerOffset, hover, 0);
        group.add(textMesh1);
        if (mirror) {
            textMesh2 = Mesh.withMaterials(textGeo, materials);
            textMesh2.position.set(centerOffset, -hover, depth);
            textMesh2.quaternion.setFromEuler(new Euler(Math.PI, Math.PI * 2, 0));
            group.add(textMesh2);
        }
    }

    function refreshText() {
        if (textMesh1) group.remove(textMesh1);
        if (mirror && textMesh2) group.remove(textMesh2);
        if (!text || !font) return;
        createText();
    }

    const events = new AbortController();
    const signal = events.signal;
    window.addEventListener('keydown', event => {
        const target = event.target as HTMLElement | null;
        if (target && (target.tagName === 'INPUT' || target.tagName === 'SELECT' || target.tagName === 'BUTTON' || target.isContentEditable)) return;
        if (event.ctrlKey || event.metaKey || event.altKey) return;
        if (event.key !== 'Backspace' && event.key.length !== 1) return;
        if (firstLetter) {
            firstLetter = false;
            text = '';
        }
        event.preventDefault();
        text = event.key === 'Backspace' ? text.substring(0, text.length - 1) : text + event.key;
        refreshText();
    }, { signal });

    // Horizontal drags turn the text with easing.
    let targetRotation = 0;
    let groupRotation = 0;
    let targetRotationOnPointerDown = 0;
    let pointerXOnPointerDown = 0;
    let activePointer: number | undefined;
    const previousTouchAction = canvas.style.touchAction;
    canvas.style.touchAction = 'none';
    canvas.addEventListener('pointerdown', event => {
        if (!event.isPrimary) return;
        activePointer = event.pointerId;
        canvas.setPointerCapture(event.pointerId);
        pointerXOnPointerDown = event.clientX;
        targetRotationOnPointerDown = targetRotation;
    }, { signal });
    canvas.addEventListener('pointermove', event => {
        if (event.pointerId === activePointer) {
            targetRotation = targetRotationOnPointerDown + (event.clientX - pointerXOnPointerDown) * 0.02;
        }
    }, { signal });
    canvas.addEventListener('lostpointercapture', event => {
        if (event.pointerId === activePointer) activePointer = undefined;
    }, { signal });

    const panel = examplePanel(canvas);
    panel.append('Type to change the text. Drag to turn it.');
    const button = (label: string, action: () => void) => {
        const element = document.createElement('button');
        element.type = 'button';
        element.textContent = label;
        element.style.display = 'block';
        element.style.width = '100%';
        element.style.marginTop = '0.35rem';
        element.addEventListener('click', () => { action(); element.blur(); });
        panel.append(element);
    };
    button('Change color', () => pointLight.color.setHSL(Math.random(), 1, 0.5));
    button('Change font', () => {
        fontIndex++;
        fontName = fontNames[fontIndex % fontNames.length];
        void loadFont();
    });
    button('Change weight', () => {
        fontWeight = fontWeight === 'bold' ? 'regular' : 'bold';
        void loadFont();
    });
    button('Change bevel', () => {
        bevelEnabled = !bevelEnabled;
        refreshText();
    });

    void loadFont();
    const rotation = new Euler();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            groupRotation += (targetRotation - groupRotation) * 0.05;
            group.quaternion.setFromEuler(rotation.set(0, groupRotation, 0));
            camera.lookAt(cameraTarget);
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
