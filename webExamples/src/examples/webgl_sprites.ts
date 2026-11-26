import {
    Color, ColorSpace, Euler, Fog, Group, OrthographicCamera, PerspectiveCamera, Scene, Sprite, SpriteMaterial,
    WebGL2Context, WebGLRenderer,
} from 'threekt';
import sprite0Url from './assets/sprite0.png?url';
import sprite1Url from './assets/sprite1.png?url';
import sprite2Url from './assets/sprite2.png?url';
import { loadImageTexture } from './loadImageTexture';

/** Port of Three.js examples/webgl_sprites.html. */
export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(60, 1, 1, 2100);
    camera.position.z = 1500;
    const cameraOrtho = new OrthographicCamera(-1, 1, 1, -1, 1, 10);
    cameraOrtho.position.z = 10;
    const scene = new Scene();
    scene.fog = new Fog(Color.fromHex(0x000000), 1500, 2100);
    const sceneOrtho = new Scene();

    const [mapA, mapB, mapC] = await Promise.all([sprite0Url, sprite1Url, sprite2Url].map(loadImageTexture));
    for (const map of [mapA, mapB, mapC]) map.colorSpace = ColorSpace.SRGB;

    // HUD sprites in the corners and the center, sized in pixels.
    const hudMaterial = new SpriteMaterial(Color.fromHex(0xffffff), mapA);
    const image = mapA.image as HTMLImageElement;
    const hudSprites = [[0, 1], [1, 1], [0, 0], [1, 0], [0.5, 0.5]].map(([x, y]) => {
        const sprite = new Sprite(hudMaterial);
        sprite.center.set(x, y);
        sprite.scale.set(image.width, image.height, 1);
        sceneOrtho.add(sprite);
        return sprite;
    });
    const [spriteTL, spriteTR, spriteBL, spriteBR, spriteC] = hudSprites;

    const amount = 200;
    const radius = 500;
    const group = new Group();
    const sprites: Sprite[] = [];
    // Three.js clones one material per sprite; the C sprites share mapC and its offset and repeat.
    mapC.offset.set(-0.5, -0.5);
    mapC.repeat.set(2, 2);
    for (let a = 0; a < amount; a++) {
        const x = Math.random() - 0.5;
        const y = Math.random() - 0.5;
        const z = Math.random() - 0.5;
        let material: SpriteMaterial;
        if (z < 0) {
            material = new SpriteMaterial(Color.fromHex(0xffffff), mapB);
        } else {
            material = new SpriteMaterial(Color.fromHex(0xffffff), mapC);
            material.color.setHSL(0.5 * Math.random(), 0.75, 0.5);
        }
        const sprite = new Sprite(material);
        sprite.position.set(x, y, z).normalize().multiplyScalar(radius);
        group.add(sprite);
        sprites.push(sprite);
    }
    scene.add(group);
    const rotation = new Euler();

    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    // Draw the HUD over the sprite sphere.
    renderer.autoClear = false;
    let width = 0;
    let height = 0;
    return {
        render() {
            if (canvas.clientWidth === 0 || canvas.clientHeight === 0) return;
            if (canvas.clientWidth !== width || canvas.clientHeight !== height) {
                width = canvas.clientWidth;
                height = canvas.clientHeight;
                camera.aspect = width / height;
                camera.updateProjectionMatrix();
                cameraOrtho.left = -width / 2;
                cameraOrtho.right = width / 2;
                cameraOrtho.top = height / 2;
                cameraOrtho.bottom = -height / 2;
                cameraOrtho.updateProjectionMatrix();
                spriteTL.position.set(-width / 2, height / 2, 1);
                spriteTR.position.set(width / 2, height / 2, 1);
                spriteBL.position.set(-width / 2, -height / 2, 1);
                spriteBR.position.set(width / 2, -height / 2, 1);
                spriteC.position.set(0, 0, 1);
                renderer.setSize(width, height, window.devicePixelRatio);
            }

            const time = Date.now() / 1000;
            sprites.forEach((sprite, i) => {
                const material = sprite.material;
                const scale = Math.sin(time + sprite.position.x * 0.01) * 0.3 + 1.0;
                const spriteImage = material.map!.image as HTMLImageElement;
                material.rotation += 0.1 * (i / sprites.length);
                sprite.scale.set(scale * spriteImage.width, scale * spriteImage.height, 1.0);
                if (material.map !== mapC) material.opacity = Math.sin(time + sprite.position.x * 0.01) * 0.4 + 0.6;
            });
            group.quaternion.setFromEuler(rotation.set(time * 0.5, time * 0.75, time * 1.0));

            renderer.clear();
            renderer.render(scene, camera);
            renderer.clearDepth();
            renderer.render(sceneOrtho, cameraOrtho);
        },
        dispose() {
            renderer.dispose();
        },
    };
}
