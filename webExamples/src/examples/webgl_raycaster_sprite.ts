import {
    Color, DomInputSurface, Euler, Group, Object3D, OrbitControls, PerspectiveCamera, Raycaster, Scene, Sprite,
    SpriteMaterial, Vector2, Vector3, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { fitPerspective } from './fitPerspective';
import { setPointer } from './raycastPointer';

/** Port of Three.js examples/webgl_raycaster_sprite.html. Hover a sprite to highlight it. */
export function createExample(canvas: HTMLCanvasElement) {
    let selectedObject: Sprite | null = null;
    const raycaster = new Raycaster();
    const pointer = new Vector2();

    const scene = new Scene();
    scene.background = Color.fromHex(0xffffff);
    const group = new Group();
    scene.add(group);

    const camera = new PerspectiveCamera(50, 1, 1, 1000);
    camera.position.set(15, 15, 15);
    camera.lookAt(new Vector3());
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 15;
    controls.maxDistance = 250;

    const blue = Color.fromStyle('#69f');
    const sprite1 = new Sprite(new SpriteMaterial(blue));
    sprite1.position.set(6, 5, 5);
    sprite1.scale.set(2, 5, 1);
    group.add(sprite1);

    const sprite2Material = new SpriteMaterial(blue);
    sprite2Material.sizeAttenuation = false;
    sprite2Material.rotation = Math.PI / 3 * 4;
    const sprite2 = new Sprite(sprite2Material);
    sprite2.position.set(8, -2, 2);
    sprite2.center.set(0.5, 0);
    sprite2.scale.set(0.1, 0.5, 0.1);
    group.add(sprite2);

    const group2 = new Object3D();
    group2.scale.set(1, 2, 1);
    group2.position.set(-5, 0, 0);
    group2.quaternion.setFromEuler(new Euler(Math.PI / 2, 0, 0));
    group.add(group2);

    const sprite3Material = new SpriteMaterial(blue);
    sprite3Material.rotation = Math.PI / 3;
    const sprite3 = new Sprite(sprite3Material);
    sprite3.position.set(0, 2, 5);
    sprite3.scale.set(10, 2, 3);
    sprite3.center.set(-0.1, 0);
    group2.add(sprite3);

    const events = new AbortController();
    canvas.addEventListener('pointermove', event => {
        if (selectedObject) {
            selectedObject.material.color.setStyle('#69f');
            selectedObject = null;
        }
        setPointer(canvas, event, pointer);
        raycaster.setFromCamera(pointer, camera);
        const intersects = raycaster.intersectObject(group, true);
        const hit = intersects.find(result => result.object3d instanceof Sprite);
        if (hit) {
            selectedObject = hit.object3d as Sprite;
            selectedObject.material.color.setStyle('#f00');
        }
    }, { signal: events.signal });

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render() {
            if (fitPerspective(canvas, camera, renderer)) renderer.render(scene, camera);
        },
        dispose() {
            events.abort();
            controls.dispose();
            renderer.dispose();
        },
    };
}
