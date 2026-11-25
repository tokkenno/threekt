import {
    BufferGeometry, Color, DomInputSurface, Float32BufferAttribute, ImageTexture, OrbitControls, PerspectiveCamera,
    Points, PointsMaterial, Scene, Usage, WebGL2Context, WebGLRenderer,
} from 'threekt';

const waveNames = ['Onda plana', 'Onda cilíndrica', 'Onda esférica'];
const waveDescriptions = [
    'La perturbación se propaga en una dirección. Los puntos de cada plano se desplazan juntos.',
    'La perturbación se aleja de un eje y pierde amplitud a medida que crece el radio.',
    'La perturbación se expande en todas las direcciones desde el centro.',
];
const colors = ['#007fff', '#f00', '#0b0', '#0cc', '#f0f', '#ff007f'];

function positionsOn(lattice: boolean) {
    if (!lattice) return Float32Array.from({ length: 20 ** 3 * 3 }, () => 20 * Math.random() - 10);
    const values = new Float32Array(21 ** 3 * 3);
    let offset = 0;
    for (let x = -10; x <= 10; x++) {
        for (let y = -10; y <= 10; y++) {
            for (let z = -10; z <= 10; z++) {
                values[offset++] = x;
                values[offset++] = y;
                values[offset++] = z;
            }
        }
    }
    return values;
}

function animateWave(base: Float32Array, values: Float32Array, wave: number, time: number) {
    for (let i = 0; i < base.length; i += 3) {
        const x = base[i], y = base[i + 1], z = base[i + 2];
        if (wave === 0) {
            values[i] = x + Math.sin(x - time);
        } else {
            const radius = wave === 1 ? Math.hypot(x, y) : Math.hypot(x, y, z);
            const scale = radius < 0.1 ? 0 : wave === 1
                ? 1.5 * Math.sin(radius - time) / (radius * Math.sqrt(radius))
                : 3 * Math.sin(radius - time) / (radius * radius);
            values[i] = x + scale * x;
            values[i + 1] = y + scale * y;
            if (wave === 2) values[i + 2] = z + scale * z;
        }
    }
}

export async function createExample(canvas: HTMLCanvasElement) {
    const article = document.createElement('article');
    article.className = 'wave-article';
    const title = document.createElement('h1');
    title.textContent = 'Ondas en tres dimensiones';
    article.append(title);
    const previousStyle = canvas.style.cssText;
    canvas.style.position = 'fixed';
    canvas.style.inset = '0';
    const nav = canvas.parentElement?.querySelector('nav');
    const previousNavZ = nav?.style.zIndex;
    if (nav) nav.style.zIndex = '2';
    canvas.after(article);
    const renderer = new WebGLRenderer(new WebGL2Context(canvas, false));
    const clearScene = new Scene();
    clearScene.background = Color.fromHex(0xffffff);
    const clearCamera = new PerspectiveCamera(75, 1, 0.1, 100);

    const views = waveNames.flatMap((name, wave) => {
        const heading = document.createElement('h2');
        heading.textContent = name;
        const description = document.createElement('p');
        description.textContent = waveDescriptions[wave];
        const pair = document.createElement('div');
        pair.className = 'wave-pair';
        article.append(heading, description, pair);

        return [true, false].map((lattice, index) => {
            const figure = document.createElement('figure');
            const caption = document.createElement('figcaption');
            caption.textContent = lattice ? 'Red regular' : 'Distribución aleatoria';
            const viewport = document.createElement('div');
            viewport.className = 'wave-view';
            figure.append(viewport, caption);
            pair.append(figure);

            const sprite = document.createElement('canvas');
            sprite.width = sprite.height = 128;
            const context = sprite.getContext('2d')!;
            context.fillStyle = colors[wave * 2 + index];
            context.arc(64, 64, 64, 0, 2 * Math.PI);
            context.fill();

            const scene = new Scene();
            scene.background = Color.fromHex(0xffffff);
            const base = positionsOn(lattice);
            const values = base.slice();
            const position = new Float32BufferAttribute(values, 3).setUsage(Usage.DynamicDraw);
            const geometry = new BufferGeometry().setAttribute('position', position);
            const material = new PointsMaterial();
            material.size = 0.25;
            material.map = new ImageTexture(sprite);
            material.transparent = true;
            material.alphaTest = 0.1;
            scene.add(new Points(geometry, material));
            const camera = new PerspectiveCamera(75, 1, 0.1, 100);
            camera.position.z = 24;
            const controls = new OrbitControls(camera, new DomInputSurface(viewport));
            return { viewport, scene, camera, controls, wave, base, values, position };
        });
    });

    return {
        render(seconds: number) {
            const width = canvas.clientWidth, height = canvas.clientHeight;
            if (!width || !height) return;
            renderer.setSize(width, height, window.devicePixelRatio);
            renderer.resetViewport();
            renderer.setScissorTest(false);
            renderer.render(clearScene, clearCamera);
            renderer.setScissorTest(true);
            for (const view of views) {
                const rect = view.viewport.getBoundingClientRect();
                if (rect.bottom <= 0 || rect.top >= height || !rect.width || !rect.height) continue;
                const left = Math.round(rect.left);
                const bottom = Math.round(height - rect.bottom);
                const w = Math.round(rect.width);
                const h = Math.round(rect.height);
                renderer.setViewport(left, bottom, w, h);
                renderer.setScissor(left, bottom, w, h);
                if (view.camera.aspect !== w / h) {
                    view.camera.aspect = w / h;
                    view.camera.updateProjectionMatrix();
                }
                animateWave(view.base, view.values, view.wave, seconds * 12);
                view.position.markUpdated();
                renderer.render(view.scene, view.camera);
            }
            renderer.setScissorTest(false);
        },
        dispose() {
            views.forEach(view => view.controls.dispose());
            article.remove();
            canvas.style.cssText = previousStyle;
            if (nav) nav.style.zIndex = previousNavZ ?? '';
            renderer.dispose();
        },
    };
}
