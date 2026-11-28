import { BoxGeometry, CylinderGeometry, PlaneGeometry } from 'threekt/geometry';
import {
    BufferGeometry, Color, Float32BufferAttribute, Group, Object3D, PerspectiveCamera, Vector3,
    type BufferAttribute,
} from 'threekt';
import qrUrl from './assets/geometry-json/QRCode_buffergeometry.json?url';
import { cssTrackball, fitCssCamera } from './exampleCss3d';
import { exampleSvg, svgNamespace } from './exampleSvg';
import { loadExampleGeometry } from './loadExampleGeometry';

export async function createExample(canvas: HTMLCanvasElement) {
    const camera = new PerspectiveCamera(75, 1, 1, 10000);
    camera.position.set(0, 0, 950);
    const svg = exampleSvg(canvas, '#f0f0f0');
    const controls = cssTrackball(canvas, camera);
    controls.minDistance = 350;
    const root = new Group();
    const entries: { node: Object3D; position: BufferAttribute; index: BufferAttribute | null | undefined; colors: string[]; alpha: number }[] = [];
    const color = new Color(1, 1, 1);
    function addGeometry(geometry: BufferGeometry, hex: number, alpha = 1) {
        const node = new Object3D();
        const position = geometry.getAttribute('position')!;
        const index = geometry.getIndex();
        const colors = geometry.getAttribute('color');
        const styles = Array.from({ length: Math.floor((index?.count ?? position.count) / 3) }, (_, i) => {
            if (!colors) return color.setHex(hex).getStyle();
            const v = index?.getX(i * 3) ?? i * 3;
            return color.setRGB(colors.getX(v), colors.getY(v), colors.getZ(v)).getStyle();
        });
        entries.push({ node, position, index, colors: styles, alpha });
        return node;
    }
    function place(geometry: BufferGeometry, tint: number, x: number, y: number, z: number, alpha = 1) {
        const node = addGeometry(geometry, tint, alpha);
        node.position.set(x, y, z);
        root.add(node);
        return node;
    }
    place(await loadExampleGeometry(qrUrl), 0x101010, 0, 0, 0);
    place(new BoxGeometry(160, 160, 160), 0x2233ff, 380, 0, 0, 0.5).rotation.set(0.4, 0.6, 0);
    place(new BoxGeometry(140, 140, 140), 0xffa136, 330, 310, 0).rotation.set(0.2, 0.5, 0);
    place(new PlaneGeometry(180, 180), 0x52ba69, 0, -340, 0);
    place(new CylinderGeometry(20, 80, 200, 10), 0xb145e2, -370, 0, 0).rotation.x = -Math.PI / 2;

    const positions = new Float32Array(900);
    const colors = new Float32Array(900);
    for (let i = 0; i < positions.length; i += 9) {
        const x = Math.random() * 1000 - 500, y = Math.random() * 800 - 400, z = Math.random() * 900 - 450;
        color.setHSL(Math.random(), 0.62, 0.57);
        for (let j = 0; j < 9; j += 3) {
            positions.set([x + Math.random() * 95, y + Math.random() * 95, z + Math.random() * 95], i + j);
            colors.set([color.r, color.g, color.b], i + j);
        }
    }
    const cloud = addGeometry(new BufferGeometry().setAttribute('position', new Float32BufferAttribute(positions, 3))
        .setAttribute('color', new Float32BufferAttribute(colors, 3)), 0);
    root.add(cloud);

    const markers = Array.from({ length: 20 }, (_, i) => {
        const node = new Object3D();
        node.position.set(Math.random() * 900 - 450, Math.random() * 700 - 350, Math.random() * 650 - 325);
        root.add(node);
        const circle = document.createElementNS(svgNamespace, 'circle');
        circle.setAttribute('r', i < 10 ? '16' : '5');
        circle.setAttribute('fill', i < 10 ? '#c32e36' : color.setHSL(Math.random(), 0.6, 0.5).getStyle());
        circle.setAttribute('stroke', '#202020');
        return { node, circle };
    });
    const drawing = document.createElementNS(svgNamespace, 'g');
    svg.svg.append(drawing, ...markers.map(marker => marker.circle));
    const points = [new Vector3(), new Vector3(), new Vector3()];
    let previous = -1;
    return {
        render(seconds: number) {
            if (seconds - previous < 1 / 24 || !fitCssCamera(canvas, camera)) return;
            previous = seconds;
            controls.update();
            svg.begin(camera);
            cloud.rotation.set(seconds * 0.6, seconds * 0.22, 0);
            const triangles: { depth: number; markup: string }[] = [];
            for (const { node, position, index, colors: faces, alpha } of entries) {
                svg.setObject(node);
                faces.forEach((fill, face) => {
                    for (let slot = 0; slot < 3; slot++) {
                        const vertex = index?.getX(face * 3 + slot) ?? face * 3 + slot;
                        svg.project(points[slot].set(position.getX(vertex), position.getY(vertex), position.getZ(vertex)));
                    }
                    if (points.some(point => point.z < -1 || point.z > 1)) return;
                    const polygon = points.map(p => `${p.x.toFixed(2)},${p.y.toFixed(2)}`).join(' ');
                    triangles.push({ depth: (points[0].z + points[1].z + points[2].z) / 3,
                        markup: `<polygon points="${polygon}" fill="${fill}" stroke="${fill}" stroke-width="0.3" opacity="${alpha}"/>` });
                });
            }
            drawing.innerHTML = triangles.sort((a, b) => b.depth - a.depth).map(face => face.markup).join('');
            for (const { node, circle } of markers) {
                svg.setObject(node);
                const point = svg.project(points[0].set(0, 0, 0));
                circle.style.display = Math.abs(point.z) > 1 ? 'none' : '';
                circle.setAttribute('cx', String(point.x));
                circle.setAttribute('cy', String(point.y));
            }
        },
        dispose() { controls.dispose(); svg.dispose(); },
    };
}
