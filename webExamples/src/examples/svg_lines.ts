import { Color, Object3D, PerspectiveCamera, Vector3 } from 'threekt';
import { fitCssCamera } from './exampleCss3d';
import { exampleSvg, svgNamespace } from './exampleSvg';

export function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(33, 1, 0.1, 100);
    camera.position.z = 10;
    const svg = exampleSvg(canvas, '#000');
    const rings = Array.from({ length: 4 }, (_, i) => {
        const node = new Object3D();
        node.scale.setScalar(i === 3 ? 2 : (i + 1) / 3);
        const path = document.createElementNS(svgNamespace, 'path');
        path.setAttribute('stroke', i === 3 ? '#416fff' : Color.fromHex(Math.random() * 0x1000000).getStyle());
        path.setAttribute('stroke-width', i === 3 ? '1' : '10');
        path.setAttribute('fill', 'none');
        if (i === 3) path.setAttribute('stroke-dasharray', '10 10');
        svg.svg.append(path);
        return { node, path };
    });
    const point = new Vector3();
    return {
        render(seconds: number) {
            if (!fitCssCamera(canvas, camera)) return;
            svg.begin(camera);
            rings.forEach(({ node, path }, index) => {
                node.rotation.set(index + 1 + seconds / 3, 0, index + 1 + seconds / 4);
                svg.setObject(node);
                const parts = [];
                for (let i = 0; i <= 50; i++) {
                    const phase = i * Math.PI * 2 / 50;
                    svg.project(point.set(Math.sin(phase), 0, Math.cos(phase)));
                    parts.push(`${i ? 'L' : 'M'}${point.x.toFixed(2)} ${point.y.toFixed(2)}`);
                }
                path.setAttribute('d', parts.join(''));
            });
        },
        dispose: svg.dispose,
    };
}
