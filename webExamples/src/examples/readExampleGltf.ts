import { BufferGeometry, Float32BufferAttribute, Uint16BufferAttribute, Uint32BufferAttribute } from 'threekt';

// The bundled assets use float attributes and 16- or 32-bit indices in one buffer.
export type ExampleGltf = {
    accessors: { bufferView: number; byteOffset?: number; count: number; componentType: number; type: string }[];
    bufferViews: { byteOffset?: number; byteStride?: number; byteLength?: number }[];
    meshes: { name: string; primitives: {
        attributes: { POSITION: number; NORMAL?: number; TEXCOORD_0?: number; COLOR_0?: number };
        indices: number;
        material?: number;
        targets?: { POSITION: number; NORMAL: number }[];
    }[] }[];
    nodes: { mesh: number; name: string; rotation: [number, number, number, number]; scale: [number, number, number];
        children?: number[]; matrix?: number[]; translation?: [number, number, number] }[];
    materials: { pbrMetallicRoughness: { baseColorFactor?: [number, number, number, number] };
        doubleSided?: boolean; alphaMode?: string; emissiveFactor?: [number, number, number] }[];
};

export function readExampleGltfGeometry(document: ExampleGltf, binary: ArrayBuffer, meshIndex: number, start = 0, primitiveIndex = 0) {
    const primitive = document.meshes[meshIndex].primitives[primitiveIndex];
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
    if (index.componentType === 5123) {
        geometry.setIndex(new Uint16BufferAttribute(new Int16Array(binary, offset(index), index.count), 1));
    } else if (index.componentType === 5125) {
        geometry.setIndex(new Uint32BufferAttribute(new Int32Array(binary, offset(index), index.count), 1));
    } else throw new Error('Expected glTF unsigned short or unsigned int indices.');
    if (primitive.attributes.NORMAL !== undefined) geometry.setAttribute('normal', readVector(primitive.attributes.NORMAL));
    else geometry.computeVertexNormals();
    if (primitive.attributes.TEXCOORD_0 !== undefined) geometry.setAttribute('uv', readVector(primitive.attributes.TEXCOORD_0, 2));
    if (primitive.attributes.COLOR_0 !== undefined) {
        const index = primitive.attributes.COLOR_0;
        geometry.setAttribute('color', readVector(index, document.accessors[index].type === 'VEC4' ? 4 : 3));
    }
    if (primitive.targets) {
        geometry.morphTargetsRelative = true;
        geometry.setMorphAttribute('position', primitive.targets.map(target => readVector(target.POSITION)));
        geometry.setMorphAttribute('normal', primitive.targets.map(target => readVector(target.NORMAL)));
    }
    return geometry;
}
