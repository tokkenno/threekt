import { DomInputSurface, TrackballControls } from 'threekt/controls';
import { Matrix4, Object3D, PerspectiveCamera, Quaternion, Vector3, type Camera } from 'threekt';

function matrixCss(matrix: Matrix4, camera: boolean) {
    const elements = Array.from(matrix.elements, (value, index) => {
        const flip = camera ? index % 4 === 1 : index >= 4 && index <= 7;
        return Math.abs(value) < 1e-10 ? 0 : flip ? -value : value;
    });
    return `matrix3d(${elements.join(',')})`;
}

/** Projects DOM elements using the same view and world matrices as the WebGL camera. */
export function exampleCss3d(canvas: HTMLCanvasElement, background?: string, behindCanvas = false) {
    const layer = document.createElement('div');
    layer.className = 'css3d-example';
    if (background) layer.style.background = background;
    if (behindCanvas) canvas.before(layer);
    else canvas.after(layer);
    const view = document.createElement('div');
    const cameraElement = document.createElement('div');
    view.append(cameraElement);
    layer.append(view);
    const entries = new Map<Object3D, { element: HTMLElement; sprite: boolean }>();
    const matrix = new Matrix4();
    const position = new Vector3(), scale = new Vector3();
    const rotation = new Quaternion();
    const cameraRotation = new Quaternion();
    return {
        layer,
        add(element: HTMLElement, sprite = false) {
            const node = new Object3D();
            element.classList.add('css3d-object');
            cameraElement.append(element);
            entries.set(node, { element, sprite });
            return node;
        },
        remove(node: Object3D) {
            entries.get(node)?.element.remove();
            entries.delete(node);
            node.parent?.remove(node);
        },
        render(scene: Object3D, camera: Camera) {
            const { clientWidth: width, clientHeight: height } = canvas;
            if (!width || !height) return;
            scene.updateMatrixWorld();
            camera.updateMatrixWorld();
            camera.getWorldQuaternion(cameraRotation);
            const focal = camera.projectionMatrix.elements[5] * height / 2;
            const perspective = camera instanceof PerspectiveCamera;
            const projection = camera.projectionMatrix.elements;
            const centerX = width * (1 + (perspective ? -projection[8] : projection[12])) / 2;
            const centerY = height * (1 + (perspective ? projection[9] : -projection[13])) / 2;
            view.style.transform = `translate(${centerX - width / 2}px,${centerY - height / 2}px)`;
            const cameraTransform = perspective ? `perspective(${focal}px) translateZ(${focal}px)` : `scale(${focal})`;
            cameraElement.style.transform = `${cameraTransform} ${matrixCss(camera.matrixWorldInverse, true)} translate(${width / 2}px,${height / 2}px)`;
            for (const [node, { element, sprite }] of entries) {
                let ancestor: Object3D | null = node;
                while (ancestor && ancestor !== scene && ancestor.visible) ancestor = ancestor.parent ?? null;
                element.hidden = ancestor !== scene || !scene.visible;
                if (element.hidden) continue;
                matrix.copyFrom(node.matrixWorld);
                if (sprite) {
                    matrix.decompose(position, rotation, scale);
                    matrix.compose(position, cameraRotation, scale);
                }
                element.style.transform = `translate(-50%,-50%) ${matrixCss(matrix, false)}`;
            }
        },
        dispose() { layer.remove(); entries.clear(); },
    };
}

export function cssTrackball(canvas: HTMLCanvasElement, camera: Camera) {
    return new TrackballControls(camera, new DomInputSurface(canvas));
}

export function fitCssCamera(canvas: HTMLCanvasElement, camera: PerspectiveCamera) {
    if (!canvas.clientWidth || !canvas.clientHeight) return false;
    const aspect = canvas.clientWidth / canvas.clientHeight;
    if (camera.aspect !== aspect) {
        camera.aspect = aspect;
        camera.updateProjectionMatrix();
    }
    return true;
}

export function cssPanel(width: number, height: number, color: string) {
    const element = document.createElement('div');
    Object.assign(element.style, { width: width + 'px', height: height + 'px', background: color });
    return element;
}
