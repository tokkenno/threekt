import { BufferGeometry, Float32BufferAttribute, Uint16BufferAttribute } from 'threekt';

// The bundled assets use float attributes and 16-bit indices in one buffer.
export type ExampleGltf = {
    accessors: { bufferView: number; byteOffset?: number; count: number; componentType: number; type: string }[];
    bufferViews: { byteOffset?: number; byteStride?: number; byteLength?: number }[];
    meshes: { name: string; primitives: {
        attributes: { POSITION: number; NORMAL?: number; TEXCOORD_0?: number };
        indices: number;
        targets?: { POSITION: number; NORMAL: number }[];
    }[] }[];
    nodes: { mesh: number; name: string; rotation: [number, number, number, number]; scale: [number, number, number] }[];
    materials: { pbrMetallicRoughness: { baseColorFactor?: [number, number, number, number] } }[];
};

export function readExampleGltfGeometry(document: ExampleGltf, binary: ArrayBuffer, meshIndex: number, start = 0) {
    const primitive = document.meshes[meshIndex].primitives[0];
    const offset = (accessor: ExampleGltf['accessors'][number]) =>
        start + (document.bufferViews[accessor.bufferView].byteOffset ?? 0) + (accessor.byteOffset ?? 0);
    const readVector = (index: number, size = 3) => {
        const accessor = document.accessors[index];
        if (accessor.type !== `VEC${size}` || accessor.componentType !== 5126) throw new Error(`Expected a glTF float VEC${size}.`);
        const stride = document.bufferViews[accessor.bufferView].byteStride ?? size * 4;
        const start = offset(accessor);
        if (stride === size * 4) return new Float32BufferAttribute(new Float32Array(binary, start, accessor.count * size), size);
        const source = new DataView(binary, start);
        const values = new Float32Array(accessor.count * size);
        for (let i = 0; i < accessor.count; i++) {
            for (let component = 0; component < size; component++) values[i * size + component] =
                source.getFloat32(i * stride + component * 4, true);
        }
        return new Float32BufferAttribute(values, size);
    };
    const geometry = new BufferGeometry().setAttribute('position', readVector(primitive.attributes.POSITION));
    const index = document.accessors[primitive.indices];
    if (index.componentType !== 5123) throw new Error('Expected glTF unsigned short indices.');
    geometry.setIndex(new Uint16BufferAttribute(new Int16Array(binary, offset(index), index.count), 1));
    if (primitive.attributes.NORMAL !== undefined) geometry.setAttribute('normal', readVector(primitive.attributes.NORMAL));
    else geometry.computeVertexNormals();
    if (primitive.attributes.TEXCOORD_0 !== undefined) geometry.setAttribute('uv', readVector(primitive.attributes.TEXCOORD_0, 2));
    if (primitive.targets) {
        geometry.morphTargetsRelative = true;
        geometry.setMorphAttribute('position', primitive.targets.map(target => readVector(target.POSITION)));
        geometry.setMorphAttribute('normal', primitive.targets.map(target => readVector(target.NORMAL)));
    }
    return geometry;
}
