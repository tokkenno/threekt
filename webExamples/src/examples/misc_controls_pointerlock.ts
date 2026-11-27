import {
    BoxGeometry, Color, Euler, Float32BufferAttribute, Fog, HemisphereLight, Mesh, MeshBasicMaterial, MeshPhongMaterial,
    PerspectiveCamera, PlaneGeometry, Raycaster, Scene, Vector3, WebGL2Context, WebGLRenderer,
    type BufferGeometry,
} from 'threekt';
import { examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';

function addColors(geometry: BufferGeometry) {
    const position = geometry.getAttribute('position')!;
    const values = new Float32Array(position.count * 3);
    const color = new Color(0, 0, 0);
    for (let i = 0; i < position.count; i++) {
        color.setHSL(Math.random() * 0.3 + 0.5, 0.75, Math.random() * 0.25 + 0.75);
        values.set([color.r, color.g, color.b], i * 3);
    }
    geometry.setAttribute('color', new Float32BufferAttribute(values, 3));
}

export function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(75, 1, 1, 1000);
    camera.position.y = 10;
    const scene = new Scene();
    const white = Color.fromHex(0xffffff);
    scene.background = white;
    scene.fog = new Fog(white, 0, 750);
    scene.add(new HemisphereLight(Color.fromHex(0xeeeeff), Color.fromHex(0x777788), 2.5));

    const floorGeometry = new PlaneGeometry(2000, 2000, 100, 100).rotateX(-Math.PI / 2);
    const positions = floorGeometry.getAttribute('position')!;
    for (let i = 0; i < positions.count; i++) {
        positions.setXYZ(i, positions.getX(i) + Math.random() * 20 - 10, Math.random() * 2,
            positions.getZ(i) + Math.random() * 20 - 10);
    }
    const floor = floorGeometry.toNonIndexed();
    addColors(floor);
    const floorMaterial = new MeshBasicMaterial();
    floorMaterial.vertexColors = true;
    scene.add(new Mesh(floor, floorMaterial));

    const geometry = new BoxGeometry(20, 20, 20).toNonIndexed();
    addColors(geometry);
    const blocks = Array.from({ length: 500 }, () => {
        const material = new MeshPhongMaterial(new Color(0, 0, 0).setHSL(Math.random() * 0.2 + 0.5, 0.75, 0.75));
        material.specular.setHex(0xffffff);
        material.flatShading = material.vertexColors = true;
        const box = new Mesh(geometry, material);
        box.position.set(Math.floor(Math.random() * 20 - 10) * 20, Math.floor(Math.random() * 20) * 20 + 10,
            Math.floor(Math.random() * 20 - 10) * 20);
        // Keep the player's starting point free.
        if (box.position.x === 0 && box.position.z === 0) box.position.x = 20;
        scene.add(box);
        return box;
    });
    scene.updateMatrixWorld(true);

    const events = new AbortController();
    const { signal } = events;
    const panel = examplePanel(canvas);
    const button = document.createElement('button');
    button.textContent = 'Enter first-person view';
    const instructions = document.createElement('p');
    instructions.textContent = 'WASD or arrows to move, mouse to look, Space to jump, Esc to release.';
    panel.append(instructions, button);
    const keys = new Set<string>();
    let locked = false;
    let canJump = false;
    const rotation = new Euler(0, 0, 0, Euler.Order.YXZ);
    const velocity = new Vector3();
    const direction = new Vector3();
    const movement = new Vector3();
    const ray = new Raycaster(new Vector3(), new Vector3(0, -1, 0), 0, 10);
    button.addEventListener('click', async () => {
        try {
            await canvas.requestPointerLock();
        } catch {
            instructions.textContent = 'The browser could not capture the pointer. Try again to enter first-person view.';
        }
    }, { signal });
    document.addEventListener('pointerlockchange', () => {
        locked = document.pointerLockElement === canvas;
        panel.hidden = locked;
        keys.clear();
    }, { signal });
    document.addEventListener('mousemove', event => {
        if (!locked) return;
        rotation.y -= event.movementX * 0.002;
        rotation.x = Math.max(-Math.PI / 2, Math.min(Math.PI / 2, rotation.x - event.movementY * 0.002));
        camera.quaternion.setFromEuler(rotation);
    }, { signal });
    document.addEventListener('keydown', event => {
        if (!locked || !/^(Key[WASD]|Arrow(Up|Down|Left|Right)|Space)$/.test(event.code)) return;
        event.preventDefault();
        keys.add(event.code);
        if (event.code === 'Space' && canJump && !event.repeat) {
            velocity.y += 350;
            canJump = false;
        }
    }, { signal });
    document.addEventListener('keyup', event => keys.delete(event.code), { signal });
    window.addEventListener('blur', () => keys.clear(), { signal });
    const pressed = (a: string, b: string) => Number(keys.has(a) || keys.has(b));
    let previous = 0;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            const delta = Math.min(seconds - previous, 0.05);
            previous = seconds;
            if (locked) {
                ray.ray.origin.copyFrom(camera.position);
                ray.ray.origin.y -= 10;
                const onBlock = ray.intersectObjects(blocks, false).length > 0;
                velocity.x -= velocity.x * 10 * delta;
                velocity.z -= velocity.z * 10 * delta;
                velocity.y -= 980 * delta;
                direction.set(pressed('KeyD', 'ArrowRight') - pressed('KeyA', 'ArrowLeft'), 0,
                    pressed('KeyS', 'ArrowDown') - pressed('KeyW', 'ArrowUp')).normalize();
                velocity.x += direction.x * 400 * delta;
                velocity.z += direction.z * 400 * delta;
                if (onBlock && velocity.y <= 0) {
                    velocity.y = 0;
                    canJump = true;
                }
                const c = Math.cos(rotation.y), s = Math.sin(rotation.y);
                movement.set(c * velocity.x + s * velocity.z, velocity.y, -s * velocity.x + c * velocity.z);
                camera.position.add(movement.multiplyScalar(delta));
                if (camera.position.y < 10) {
                    velocity.y = 0;
                    camera.position.y = 10;
                    canJump = true;
                }
            }
            renderer.render(scene, camera);
        },
        dispose() {
            events.abort();
            if (document.pointerLockElement === canvas) document.exitPointerLock();
            panel.remove();
            renderer.dispose();
        },
    };
}
