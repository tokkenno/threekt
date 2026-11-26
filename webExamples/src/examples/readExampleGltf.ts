import { BufferGeometry, Float32BufferAttribute, Uint16BufferAttribute } from 'threekt';

// The two bundled assets use tightly packed float attributes and 16-bit indices in one buffer.
export type ExampleGltf = {
    accessors: { bufferView: number; byteOffset?: number; count: number; componentType: number; type: string }[];
    bufferViews: { byteOffset?: number }[];
    meshes: { name: string; primitives: {
        attributes: { POSITION: number; NORMAL?: number };
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
    const readVector = (index: number) => {
        const accessor = document.accessors[index];
        if (accessor.type !== 'VEC3' || accessor.componentType !== 5126) throw new Error('Expected a glTF float VEC3.');
        return new Float32BufferAttribute(new Float32Array(binary, offset(accessor), accessor.count * 3), 3);
    };
    const geometry = new BufferGeometry().setAttribute('position', readVector(primitive.attributes.POSITION));
    const index = document.accessors[primitive.indices];
    if (index.componentType !== 5123) throw new Error('Expected glTF unsigned short indices.');
    geometry.setIndex(new Uint16BufferAttribute(new Int16Array(binary, offset(index), index.count), 1));
    if (primitive.attributes.NORMAL !== undefined) geometry.setAttribute('normal', readVector(primitive.attributes.NORMAL));
    else geometry.computeVertexNormals();
    if (primitive.targets) {
        geometry.morphTargetsRelative = true;
        geometry.setMorphAttribute('position', primitive.targets.map(target => readVector(target.POSITION)));
        geometry.setMorphAttribute('normal', primitive.targets.map(target => readVector(target.NORMAL)));
    }
    return geometry;
}
