import {
    BoxGeometry, BufferGeometry, CatmullRomCurve3, Color, ColorSpace, DataTexture, DomInputSurface, Float32BufferAttribute,
    InstancedBufferAttribute, InstancedMesh, LineBasicMaterial, LineLoop, Mesh, MeshBasicMaterial, OrbitControls,
    PerspectiveCamera, Scene, ShaderMaterial, Uniform, Uniforms, Vector3, WebGL2Context, WebGLRenderer,
} from 'threekt';
import { FontLoader, TextGeometry } from 'threekt/loaders';
import fontUrl from './assets/fonts/helvetiker_regular.typeface.json?url';
import { dragMeshes } from './dragMeshes';
import { fitPerspective } from './fitPerspective';
import { encodeSrgb } from './fullscreenPass';

const samples = 256;

export async function createCurveModifierExample(canvas: HTMLCanvasElement, instanced: boolean) {
    const response = await fetch(fontUrl);
    if (!response.ok) throw new Error('Could not load the curve font.');
    const font = new FontLoader().parse(await response.text());
    const parameters = Object.assign(new TextGeometry.Parameters(), { font, size: 0.2, depth: 0.05,
        curveSegments: 8, bevelEnabled: true, bevelThickness: 0.02, bevelSize: 0.01, bevelSegments: 3 });
    const text = new TextGeometry('Hello three.js!', parameters);
    text.rotateX(Math.PI);

    const scene = new Scene();
    scene.background = Color.fromHex(0x17202a);
    const handleGeometry = new BoxGeometry(0.12, 0.12, 0.12);
    const handleMaterial = new MeshBasicMaterial(Color.fromHex(0xffffff));
    const curves = Array.from({ length: instanced ? 2 : 1 }, (_, i) => {
        const y = instanced ? i - 0.5 : 0;
        const sign = i ? -1 : 1;
        const handles = [[sign, -1],[sign, 1],[-sign, 1],[-sign,-1]].map(([x,z]) => {
            const mesh = new Mesh(handleGeometry, handleMaterial);
            mesh.position.set(x,y,z);
            scene.add(mesh);
            return mesh;
        });
        const curve = new CatmullRomCurve3(handles.map(handle => handle.position), true);
        const attribute = new Float32BufferAttribute(new Float32Array(samples * 3), 3);
        const line = new LineLoop(new BufferGeometry().setAttribute('position', attribute),
            new LineBasicMaterial(Color.fromHex(i ? 0x60c7ff : 0x54ff6f)));
        line.frustumCulled = false;
        scene.add(line);
        return { curve, handles, attribute, length: 1, range: 1, center: new Vector3() };
    });
    const bytes = new Int8Array(samples * 8 * curves.length * 4);
    const texture = new DataTexture(bytes, samples, 8 * curves.length);
    texture.colorSpace = ColorSpace.None;

    const pack = (sample: number, row: number, vector: Vector3) => {
        const slot = (row * samples + sample) * 4;
        for (const [i, component] of [vector.x, vector.y, vector.z].entries()) {
            const packed = Math.round(Math.max(0, Math.min(1, component * 0.5 + 0.5)) * 65535);
            bytes[slot + i] = packed >> 8;
            bytes[slot + 4 * samples + i] = packed & 255;
        }
    };
    const refresh = () => {
        curves.forEach((entry, i) => {
            const { curve, attribute, handles, center } = entry;
            curve.updateArcLengths();
            entry.length = Math.max(0.001, curve.getLength());
            center.set(0, 0, 0);
            for (const handle of handles) center.add(handle.position);
            center.multiplyScalar(1 / handles.length);
            entry.range = entry.length + 0.01;
            const frames = curve.computeFrenetFrames(samples, true);
            const point = new Vector3();
            for (let j = 0; j < samples; j++) {
                curve.getPointAt(j / samples, point);
                attribute.setXYZ(j, point.x, point.y, point.z);
                pack(j, i * 8, point.sub(center).multiplyScalar(1 / entry.range));
                pack(j, i * 8 + 2, frames.tangents[j]);
                pack(j, i * 8 + 4, frames.normals[j]);
                pack(j, i * 8 + 6, frames.binormals[j]);
            }
            attribute.markUpdated();
        });
        texture.markUpdated();
    };
    refresh();
    const time = new Uniform(0);
    const range0 = new Uniform(curves[0].range), range1 = new Uniform(curves[1]?.range ?? 1);
    const length0 = new Uniform(curves[0].length), length1 = new Uniform(curves[1]?.length ?? 1);
    const material = new ShaderMaterial(`
        uniform sampler2D spine;
        uniform float offset, range0, range1, length0, length1;
        uniform vec3 center0, center1;
        varying vec3 vNormal, colorTint;
        #ifdef USE_INSTANCING
        attribute vec2 placement;
        #endif
        vec3 unpackVector(int i, int row) {
            vec3 high = texelFetch(spine, ivec2(i, row), 0).rgb;
            vec3 low = texelFetch(spine, ivec2(i, row+1), 0).rgb;
            return (high * 256.0 + low) / 257.0 * 2.0 - 1.0;
        }
        vec3 sampleVector(float x, int row) {
            int i = int(floor(x));
            return mix(unpackVector(i % ${samples},row), unpackVector((i+1) % ${samples},row), fract(x));
        }
        void main() {
            vec2 instance = vec2(0.0);
            colorTint = vec3(0.32,0.82,0.98);
            #ifdef USE_INSTANCING
            instance = placement;
            colorTint = instanceColor;
            #endif
            bool first = instance.x < 0.5;
            int row = int(instance.x) * 8;
            float distance = first ? length0 : length1;
            float p = fract(offset + instance.y + position.x / distance) * float(${samples});
            vec3 point = sampleVector(p,row) * (first ? range0 : range1) + (first ? center0 : center1);
            mat3 basis = mat3(normalize(sampleVector(p,row+2)),normalize(sampleVector(p,row+4)),normalize(sampleVector(p,row+6)));
            point += basis * vec3(0,position.yz);
            vNormal = normalMatrix * basis * normal;
            gl_Position = projectionMatrix * modelViewMatrix * vec4(point,1);
        }
    `, `
        varying vec3 vNormal, colorTint;
        ${encodeSrgb}
        void main() {
            float light = max(0.0,dot(normalize(vNormal),normalize(vec3(0.4,0.6,1.0))));
            gl_FragColor = vec4(encodeSrgb(colorTint * (0.25 + 0.8 * light)),1);
        }
    `, new Uniforms().set('spine', new Uniform(texture)).set('offset', time)
        .set('center0', new Uniform(curves[0].center)).set('center1', new Uniform(curves[1]?.center ?? new Vector3()))
        .set('range0', range0).set('range1', range1).set('length0', length0).set('length1', length1));
    let mesh: Mesh;
    if (instanced) {
        const placement = new Float32Array(16);
        const colored = new InstancedMesh(text, material, 8);
        for (let i = 0; i < colored.count; i++) {
            placement.set([i % curves.length, i / colored.count], i * 2);
            colored.setColorAt(i, new Color(1, 1, 1).setHSL(i / colored.count, 0.65, 0.55));
        }
        text.setAttribute('placement', new InstancedBufferAttribute(placement, 2));
        mesh = colored;
    } else mesh = new Mesh(text, material);
    mesh.frustumCulled = false;
    scene.add(mesh);

    const camera = new PerspectiveCamera(48, 1, 0.1, 100);
    camera.position.set(2.8, 3.4, 4.2);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    let dragging = false;
    let curveChanged = false;
    const stopDrag = dragMeshes(canvas, camera, curves.flatMap(curve => curve.handles), {
        begin(handle) { dragging = true; controls.enabled = false; return [handle]; },
        end() { dragging = false; curveChanged = true; controls.enabled = true; },
    });
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            controls.update();
            if (dragging || curveChanged) {
                curveChanged = false;
                refresh();
                range0.value = curves[0].range;
                length0.value = curves[0].length;
                range1.value = curves[1]?.range ?? 1;
                length1.value = curves[1]?.length ?? 1;
            }
            time.value = seconds * 0.06;
            renderer.render(scene, camera);
        },
        dispose() { stopDrag(); controls.dispose(); renderer.dispose(); },
    };
}
