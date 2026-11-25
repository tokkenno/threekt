import { BufferGeometry, Color, Float32BufferAttribute, Usage } from 'threekt';

/** Equivalent to merging vertices after retaining only position, for the point-cloud examples. */
export function uniquePositions(geometry: BufferGeometry): number[] {
    const position = geometry.getAttribute('position')!;
    const index = geometry.index;
    const seen = new Set<string>();
    const vertices: number[] = [];
    for (let i = 0; i < (index?.length ?? position.count); i++) {
        const vertex = index?.[i] ?? i;
        const x = position.getX(vertex), y = position.getY(vertex), z = position.getZ(vertex);
        const key = [x, y, z].map(value => Math.trunc(value * 10000 + 0.5)).join(',');
        if (seen.has(key)) continue;
        seen.add(key);
        vertices.push(x, y, z);
    }
    return vertices;
}

export function customPointGeometry(
    vertices: number[], innerCount: number, colorFor: (color: Color, index: number, y: number) => void,
) {
    const positions = Float32Array.from(vertices);
    const colors = new Float32Array(vertices.length);
    const sizes = new Float32Array(vertices.length / 3);
    const color = Color.fromHex(0xffffff);
    for (let i = 0; i < sizes.length; i++) {
        colorFor(color, i, positions[i * 3 + 1]);
        colors.set([color.r, color.g, color.b], i * 3);
        sizes[i] = i < innerCount ? 10 : 40;
    }
    const size = new Float32BufferAttribute(sizes, 1).setUsage(Usage.DynamicDraw);
    const geometry = new BufferGeometry()
        .setAttribute('position', new Float32BufferAttribute(positions, 3))
        .setAttribute('ca', new Float32BufferAttribute(colors, 3))
        .setAttribute('size', size);
    return { geometry, position: positions, size, sizes };
}
