import { BufferGeometry, Float32BufferAttribute } from 'threekt';

export async function loadExampleGeometry(url: string) {
    const response = await fetch(url);
    if (!response.ok) throw new Error('Could not load geometry (HTTP ' + response.status + ').');
    const { data } = await response.json() as {
        data: { attributes: Record<string, { array: number[]; itemSize: number; normalized?: boolean }>;
            index?: { array: number[] } };
    };
    const geometry = new BufferGeometry();
    for (const [name, attribute] of Object.entries(data.attributes)) {
        geometry.setAttribute(name, new Float32BufferAttribute(Float32Array.from(attribute.array),
            attribute.itemSize, attribute.normalized ?? false));
    }
    if (data.index) geometry.setIndexArray(Int32Array.from(data.index.array));
    return geometry;
}
