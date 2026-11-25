import { Vector3 } from 'threekt';

/** Hilbert control points, following the corner order of upstream GeometryUtils.hilbert3D. */
export function hilbert3D(center: Vector3, size: number, iterations = 1, order = [0, 1, 2, 3, 4, 5, 6, 7]): Vector3[] {
    const half = size / 2;
    const corners = [
        [-1, 1, -1], [-1, 1, 1], [-1, -1, 1], [-1, -1, -1],
        [1, -1, -1], [1, -1, 1], [1, 1, 1], [1, 1, -1],
    ];
    const points = order.map(i => {
        const [x, y, z] = corners[i];
        return new Vector3(center.x + x * half, center.y + y * half, center.z + z * half);
    });
    if (iterations <= 0) return points;
    const turns = [
        [0, 3, 4, 7, 6, 5, 2, 1], [0, 7, 6, 1, 2, 5, 4, 3],
        [0, 7, 6, 1, 2, 5, 4, 3], [2, 3, 0, 1, 6, 7, 4, 5],
        [2, 3, 0, 1, 6, 7, 4, 5], [4, 3, 2, 5, 6, 1, 0, 7],
        [4, 3, 2, 5, 6, 1, 0, 7], [6, 5, 2, 1, 0, 3, 4, 7],
    ];
    return points.flatMap((point, i) => hilbert3D(point, half, iterations - 1, turns[i].map(j => order[j])));
}
