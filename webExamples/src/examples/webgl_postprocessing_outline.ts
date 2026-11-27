import { AmbientLight, Color, DirectionalLight, DomInputSurface, Group, Mesh, MeshBasicMaterial,
    MeshLambertMaterial, OBJLoader, OrbitControls, PerspectiveCamera, PlaneGeometry, Raycaster,
    Scene, SphereGeometry, TorusGeometry, Uniform, Uniforms, Vector2, WebGL2Context, WebGLRenderer, WebGLRenderTarget } from 'threekt';
import treeUrl from './assets/tree.obj?url';
import { addSliders } from './addSliders';
import { addToggle, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';
import { encodeSrgb, fullscreenPass } from './fullscreenPass';
import { setPointer } from './raycastPointer';

export async function createExample(canvas: HTMLCanvasElement) {
    const response = await fetch(treeUrl);
    if (!response.ok) throw new Error('Could not load the tree.');
    const tree = new OBJLoader().parse(await response.text());
    tree.scale.setScalar(0.27);
    tree.position.set(0, -1.5, -2);
    const scene = new Scene(), maskScene = new Scene();
    scene.background = Color.fromHex(0x303740);
    scene.add(tree);
    const group = new Group();
    scene.add(group);
    scene.add(new AmbientLight(Color.fromHex(0xaaaaaa), 1.25));
    const lamp = new DirectionalLight(Color.fromHex(0xddffdd), 2);
    lamp.position.set(5, 10, 4);
    scene.add(lamp);
    const spheres = Array.from({ length: 20 }, (_, i) => {
        const mesh = new Mesh(new SphereGeometry(0.35 + Math.random() * 0.3, 20, 12),
            new MeshLambertMaterial(new Color(1, 1, 1).setHSL(i / 20, 0.7, 0.55)));
        mesh.position.set(Math.random() * 6 - 3, Math.random() * 3 - 1, Math.random() * 5 - 2.5);
        group.add(mesh);
        return mesh;
    });
    const ring = new Mesh(new TorusGeometry(1, 0.3, 16, 64), new MeshLambertMaterial(Color.fromHex(0xffaaff)));
    ring.position.set(-1, 0, -3);
    group.add(ring);
    const floor = new Mesh(new PlaneGeometry(12, 12), new MeshLambertMaterial(Color.fromHex(0x666e76)));
    floor.rotation.x = -Math.PI / 2;
    floor.position.y = -1.5;
    scene.add(floor);

    const selectables = [...spheres, ring];
    tree.traverse(node => { if (node instanceof Mesh) selectables.push(node); });
    const camera = new PerspectiveCamera(45, 1, 0.1, 100);
    camera.position.set(0, 1, 9);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 5;
    controls.maxDistance = 20;
    controls.enableDamping = true;
    const pointer = new Vector2(2, 2);
    const events = new AbortController();
    canvas.addEventListener('pointermove', event => setPointer(canvas, event, pointer), { signal: events.signal });
    canvas.addEventListener('pointerleave', () => pointer.set(2, 2), { signal: events.signal });
    const ray = new Raycaster();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const source = new WebGLRenderTarget(1, 1, undefined, true, true);
    const mask = new WebGLRenderTarget(1, 1, undefined, true, true);
    const silhouette = new Mesh(spheres[0].geometry, new MeshBasicMaterial(Color.fromHex(0xffffff)));
    silhouette.matrixAutoUpdate = false;
    silhouette.frustumCulled = false;
    maskScene.add(silhouette);
    const pixel = new Vector2();
    const thickness = new Uniform(2), strength = new Uniform(3), glow = new Uniform(0), pulse = new Uniform(1);
    const pattern = new Uniform(false);
    const pass = fullscreenPass(`
        uniform sampler2D sourceColor, sourceDepth, selection, selectionDepth;
        uniform vec2 pixel;
        uniform float thickness, strength, glow, pulse;
        uniform bool pattern;
        ${encodeSrgb}
        void main() {
            vec3 color = texture2D(sourceColor, vUv).rgb;
            float mask = texture2D(selection, vUv).r;
            float edge = 0.0, hidden = 0.0, soft = 0.0;
            for (int i = 0; i < 8; i++) {
                vec2 offset = vec2(cos(float(i) * 0.7854), sin(float(i) * 0.7854)) * pixel;
                vec2 uv = vUv + offset * thickness;
                float hit = texture2D(selection, uv).r;
                float covered = step(texture2D(sourceDepth, uv).r + 0.00005, texture2D(selectionDepth, uv).r);
                edge = max(edge, hit);
                hidden = max(hidden, hit * covered);
                soft = max(soft, texture2D(selection, vUv + offset * (2.0 + thickness)).r);
            }
            vec3 edgeColor = mix(vec3(1.0, 0.83, 0.35), vec3(0.3, 0.06, 0.025), hidden);
            color += edgeColor * ((edge * min(strength * 0.42, 2.0) + soft * glow) * (1.0 - mask)) * pulse;
            if (pattern) color += vec3(0.18, 0.08, 0.01) * mask * step(0.65, sin((gl_FragCoord.x + gl_FragCoord.y) * 0.8));
            gl_FragColor = vec4(encodeSrgb(color), 1.0);
        }
    `, new Uniforms().set('sourceColor', new Uniform(source.texture)).set('sourceDepth', new Uniform(source.depthTexture))
        .set('selection', new Uniform(mask.texture)).set('selectionDepth', new Uniform(mask.depthTexture))
        .set('pixel', new Uniform(pixel)).set('thickness', thickness).set('strength', strength)
        .set('glow', glow).set('pulse', pulse).set('pattern', pattern));
    const panel = examplePanel(canvas);
    let period = 0;
    addToggle(panel, 'Selection pattern', false, value => { pattern.value = value; });
    addSliders(canvas, [
        { label: 'Edge thickness', min: 1, max: 4, step: 0.1, value: 2, change: value => { thickness.value = value; } },
        { label: 'Edge strength', min: 0.1, max: 10, step: 0.1, value: 3, change: value => { strength.value = value; } },
        { label: 'Edge glow', min: 0, max: 1, step: 0.01, value: 0, change: value => { glow.value = value; } },
        { label: 'Pulse period', min: 0, max: 5, step: 0.1, value: 0, change: value => { period = value; } },
    ], panel);
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            group.rotation.y = seconds * 0.25;
            ring.rotation.x = seconds * 0.6;
            for (const target of [source, mask]) target.setSize(canvas.width, canvas.height);
            pixel.set(1 / canvas.width, 1 / canvas.height);
            pulse.value = period ? 0.55 + 0.45 * Math.cos(seconds / period * Math.PI * 2) : 1;
            renderer.setRenderTarget(source);
            renderer.render(scene, camera);
            ray.setFromCamera(pointer, camera);
            const hit = ray.intersectObjects(selectables, false)[0]?.object;
            silhouette.visible = hit instanceof Mesh;
            if (hit instanceof Mesh) {
                silhouette.geometry = hit.geometry;
                silhouette.matrix.copyFrom(hit.matrixWorld);
                silhouette.matrixWorldNeedsUpdate = true;
            }
            renderer.setRenderTarget(mask);
            renderer.render(maskScene, camera);
            renderer.setRenderTarget(null);
            renderer.render(pass.scene, pass.camera);
        },
        dispose() { events.abort(); panel.remove(); controls.dispose(); renderer.dispose(); },
    };
}
