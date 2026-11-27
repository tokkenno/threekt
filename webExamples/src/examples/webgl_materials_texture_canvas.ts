import { BoxGeometry, Color, Euler, ImageTexture, Mesh, MeshBasicMaterial, PerspectiveCamera, Scene, WebGL2Context, WebGLRenderer } from 'threekt';
import { fitPerspective } from './fitPerspective';

/** Port of Three.js examples/webgl_materials_texture_canvas.html. */
export async function createExample(canvas: HTMLCanvasElement) {
    const drawingCanvas = document.createElement('canvas');
    drawingCanvas.width = 128;
    drawingCanvas.height = 128;
    drawingCanvas.style.touchAction = 'none';
    drawingCanvas.style.display = 'block';
    drawingCanvas.setAttribute('aria-label', 'Texture drawing canvas');
    const drawing = drawingCanvas.getContext('2d');
    if (!drawing) throw new Error('The 2D canvas context is unavailable.');
    drawing.fillStyle = '#ffffff';
    drawing.fillRect(0, 0, 128, 128);
    drawing.strokeStyle = '#000000';
    drawing.lineWidth = 3;
    drawing.lineCap = 'round';

    const scene = new Scene();
    const camera = new PerspectiveCamera(50, 1, 1, 2000);
    camera.position.set(0, 0, 500);
    const texture = new ImageTexture(drawingCanvas);
    const cube = new Mesh(new BoxGeometry(200, 200, 200), new MeshBasicMaterial(Color.fromHex(0xffffff), texture));
    scene.add(cube);
    const rotation = new Euler();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));

    let pointerId: number | undefined;
    let x = 0;
    let y = 0;
    drawingCanvas.addEventListener('pointerdown', (event) => {
        if (pointerId !== undefined || event.button !== 0) return;
        pointerId = event.pointerId;
        x = event.offsetX;
        y = event.offsetY;
        drawingCanvas.setPointerCapture(event.pointerId);
    });
    drawingCanvas.addEventListener('pointermove', (event) => {
        if (event.pointerId !== pointerId) return;
        drawing.beginPath();
        drawing.moveTo(x, y);
        x = event.offsetX;
        y = event.offsetY;
        drawing.lineTo(x, y);
        drawing.stroke();
        texture.markUpdated();
    });
    drawingCanvas.addEventListener('lostpointercapture', (event) => {
        if (event.pointerId === pointerId) pointerId = undefined;
    });

    const panel = document.createElement('aside');
    panel.className = 'example-panel';
    const instructions = document.createElement('p');
    instructions.textContent = 'Draw in the box to change the cube’s texture.';
    panel.append(instructions, drawingCanvas);
    canvas.parentElement?.append(panel);

    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            cube.quaternion.setFromEuler(rotation.set(seconds * 0.6, seconds * 0.6, 0));
            renderer.render(scene, camera);
        },
        dispose() {
            panel.remove();
            renderer.dispose();
        },
    };
}
