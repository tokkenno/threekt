import {
    BoxGeometry, CylinderGeometry, IcosahedronGeometry, LatheGeometry, OctahedronGeometry, PlaneGeometry,
    SphereGeometry, TorusGeometry, TorusKnotGeometry, Vector2, type BufferGeometry,
} from 'threekt';

function drawUVs(geometry: BufferGeometry, size: number) {
    const chart = document.createElement('canvas');
    chart.width = chart.height = size;
    const ctx = chart.getContext('2d')!;
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, size, size);
    ctx.strokeStyle = '#383838';
    ctx.lineWidth = 1;
    const uv = geometry.getAttribute('uv')!;
    const index = geometry.getIndex();
    const count = index?.count ?? uv.count;
    const coord = (i: number) => [8 + uv.getX(i) * (size - 16), size - 8 - uv.getY(i) * (size - 16)] as const;
    ctx.font = '10px monospace';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    for (let slot = 0; slot + 2 < count; slot += 3) {
        const ids = [0, 1, 2].map(offset => index?.getX(slot + offset) ?? slot + offset);
        const points = ids.map(coord);
        ctx.beginPath();
        ctx.moveTo(...points[0]);
        ctx.lineTo(...points[1]);
        ctx.lineTo(...points[2]);
        ctx.closePath();
        ctx.stroke();
        const center = points.reduce(([x, y], point) => [x + point[0] / 3, y + point[1] / 3], [0, 0]);
        ctx.fillStyle = '#c02';
        ctx.fillText(String(slot / 3), center[0], center[1]);
        ctx.fillStyle = '#0648a0';
        points.forEach(([x, y], i) => ctx.fillText(String(ids[i]), 0.82 * x + 0.18 * center[0], 0.82 * y + 0.18 * center[1]));
    }
    return chart;
}

export function createExample(canvas: HTMLCanvasElement) {
    const profile = Array.from({ length: 10 }, (_, i) => new Vector2(Math.sin(i * 0.2) * 15 + 50, (i - 5) * 2));
    const examples: [string, BufferGeometry][] = [
        ['Plane', new PlaneGeometry(100, 100, 4, 4)],
        ['Sphere', new SphereGeometry(75, 12, 6)],
        ['Icosahedron', new IcosahedronGeometry(30, 1)],
        ['Octahedron', new OctahedronGeometry(30, 2)],
        ['Cylinder', new CylinderGeometry(25, 75, 100, 10, 5)],
        ['Box', new BoxGeometry(100, 100, 100, 4, 4, 4)],
        ['Lathe', new LatheGeometry(profile, 8)],
        ['Torus', new TorusGeometry(50, 20, 8, 8)],
        ['Torus knot', new TorusKnotGeometry(50, 10, 12, 6)],
    ];
    const grid = document.createElement('div');
    grid.className = 'uv-grid';
    for (const [name, geometry] of examples) {
        const figure = document.createElement('figure');
        const caption = document.createElement('figcaption');
        caption.textContent = name;
        const chart = drawUVs(geometry, 512);
        chart.setAttribute('aria-label', `${name} UV map, with face and vertex indices`);
        figure.append(caption, chart);
        grid.append(figure);
    }
    canvas.after(grid);
    return { render() {}, dispose() { grid.remove(); } };
}
