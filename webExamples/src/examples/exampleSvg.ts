import { Matrix4, Vector3, type Camera, type Object3D } from 'threekt';

export const svgNamespace = 'http://www.w3.org/2000/svg';

export function exampleSvg(canvas: HTMLCanvasElement, background: string) {
    const svg = document.createElementNS(svgNamespace, 'svg');
    svg.classList.add('css3d-example');
    svg.style.background = background;
    canvas.after(svg);
    const viewProjection = new Matrix4();
    const transform = new Matrix4();
    let width = 0, height = 0;
    return {
        svg,
        begin(camera: Camera) {
            width = canvas.clientWidth;
            height = canvas.clientHeight;
            svg.setAttribute('width', String(width));
            svg.setAttribute('height', String(height));
            svg.setAttribute('viewBox', `0 0 ${width} ${height}`);
            camera.updateMatrixWorld();
            viewProjection.multiplyMatrices(camera.projectionMatrix, camera.matrixWorldInverse);
        },
        setObject(node: Object3D) {
            node.updateWorldMatrix(true, false);
            transform.multiplyMatrices(viewProjection, node.matrixWorld);
        },
        project(point: Vector3) {
            point.applyMatrix4(transform);
            point.x = (point.x + 1) * width / 2;
            point.y = (1 - point.y) * height / 2;
            return point;
        },
        dispose() { svg.remove(); },
    };
}
