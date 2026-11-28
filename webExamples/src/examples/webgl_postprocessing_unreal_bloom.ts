import {
    AmbientLight, Box3, Color, DirectionalLight, Group, Mesh, MeshPhongMaterial, PerspectiveCamera, Quaternion,
    Scene, Side, Vector3,
} from 'threekt';
import { DomInputSurface, OrbitControls } from 'threekt/controls';
import { WebGL2Context, WebGLRenderer } from 'threekt/render-gl';
import model from './assets/PrimaryIonDrive.glb?url';
import { exampleBloom } from './exampleBloom';
import { loadExampleGlb } from './exampleGlb';
import { fitPerspective } from './fitPerspective';
import { readExampleGltfGeometry, type ExampleGltf } from './readExampleGltf';

type IonDrive = ExampleGltf & {
    scenes: { nodes: number[] }[];
    scene: number;
    animations: { channels: { sampler: number; target: { node: number; path: string } }[];
        samplers: { input: number; output: number }[] }[];
};

export async function createExample(canvas: HTMLCanvasElement) {
    const { json,buffer,start } = await loadExampleGlb<IonDrive>(model);
    const materials = json.materials.map((source,i) => {
        const [r,g,b,a] = source.pbrMetallicRoughness.baseColorFactor ?? [1,1,1,1];
        const material = new MeshPhongMaterial(new Color(r,g,b));
        material.side = source.doubleSided ? Side.Double : Side.Front;
        material.opacity = a;
        material.transparent = source.alphaMode === 'BLEND';
        material.depthWrite = !material.transparent;
        material.vertexColors = true;
        const [er,eg,eb] = source.emissiveFactor ?? [0,0,0];
        material.emissive.set(er,eg,eb).multiplyScalar(i===2 ? 5 : 2.5);
        return material;
    });
    const meshes = json.meshes.map((mesh,index) => mesh.primitives.map((primitive,id) => {
        const geometry = readExampleGltfGeometry(json,buffer,index,start,id);
        return { geometry, material: materials[primitive.material ?? 0] };
    }));
    const nodes = json.nodes.map(source => {
        const node = new Group();
        if (source.matrix) node.matrix.fromArray(Float64Array.from(source.matrix)).decompose(node.position,node.quaternion,node.scale);
        if (source.translation) node.position.set(...source.translation);
        if (source.rotation) node.quaternion.set(...source.rotation);
        if (source.scale) node.scale.set(...source.scale);
        if (source.mesh !== undefined) for (const {geometry,material} of meshes[source.mesh]) node.add(new Mesh(geometry,material));
        return node;
    });
    json.nodes.forEach((node,index) => {
        for (const child of node.children ?? []) nodes[index].add(nodes[child]);
    });
    const root = new Group();
    for (const index of json.scenes[json.scene ?? 0].nodes) root.add(nodes[index]);
    const box = new Box3().setFromObject(root);
    const center = box.getCenter(new Vector3());
    const size = box.getSize(new Vector3());
    const scale = 4.6 / Math.max(size.x,size.y,size.z);
    root.scale.setScalar(scale);
    root.position.copyFrom(center).multiplyScalar(-scale);
    const scene = new Scene();
    scene.add(root);
    scene.add(new AmbientLight(Color.fromHex(0xbbc5ef),0.9));
    const light = new DirectionalLight(Color.fromHex(0xffffff),2);
    light.position.set(-3,4,-2);
    scene.add(light);

    const read = (id:number,width:number) => {
        const acc = json.accessors[id], view = json.bufferViews[acc.bufferView];
        return new Float32Array(buffer,start+(view.byteOffset ?? 0)+(acc.byteOffset ?? 0),acc.count*width);
    };
    const animation = json.animations[0];
    const channels = animation.channels.filter(channel => channel.target.path === 'rotation').map(({sampler,target}) => {
        const {input,output} = animation.samplers[sampler];
        return { target:nodes[target.node], time:read(input,1), values:read(output,4) };
    });
    const q = new Quaternion();
    const camera = new PerspectiveCamera(40,1,0.1,100);
    camera.position.set(-5,2.5,-4.5);
    const controls = new OrbitControls(camera,new DomInputSurface(canvas));
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const bloom = exampleBloom(canvas,renderer,false);
    return {
        render(seconds:number) {
            if (!fitPerspective(canvas,camera,renderer)) return;
            controls.update();
            for (const {target,time,values} of channels) {
                const t = seconds % time[time.length-1];
                const upper = Math.max(1,time.findIndex(value => value > t));
                const a=(upper-1)*4,b=upper*4;
                target.quaternion.set(values[a],values[a+1],values[a+2],values[a+3]);
                q.set(values[b],values[b+1],values[b+2],values[b+3]);
                target.quaternion.slerp(q,(t-time[upper-1])/(time[upper]-time[upper-1]));
            }
            bloom.render(scene,camera);
        },
        dispose() { bloom.dispose(); controls.dispose(); renderer.dispose(); },
    };
}
