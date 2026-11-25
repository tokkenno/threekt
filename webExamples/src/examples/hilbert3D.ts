import { Vector3 } from 'threekt';

const permutations = [
    [0, 3, 4, 7, 6, 5, 2, 1], [0, 7, 6, 1, 2, 5, 4, 3],
    [0, 7, 6, 1, 2, 5, 4, 3], [2, 3, 0, 1, 6, 7, 4, 5],
    [2, 3, 0, 1, 6, 7, 4, 5], [4, 3, 2, 5, 6, 1, 0, 7],
    [4, 3, 2, 5, 6, 1, 0, 7], [6, 5, 2, 1, 0, 3, 4, 7],
];

export function hilbert3D(center: Vector3, size: number, depth: number, order = [0, 1, 2, 3, 4, 5, 6, 7]): Vector3[] {
    const half = size / 2;
    const corners = [
        [-1, 1, -1], [-1, 1, 1], [-1, -1, 1], [-1, -1, -1],
        [1, -1, -1], [1, -1, 1], [1, 1, 1], [1, 1, -1],
    ];
    const points = order.map(index => {
        const [x, y, z] = corners[index];
        return new Vector3(center.x + x * half, center.y + y * half, center.z + z * half);
    });
    return depth === 0 ? points :
        points.flatMap((point, i) => hilbert3D(point, half, depth - 1, permutations[i].map(index => order[index])));
}
