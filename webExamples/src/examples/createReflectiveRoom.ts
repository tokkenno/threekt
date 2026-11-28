import {
    AmbientLight, CircleGeometry, Color, CylinderGeometry, DirectionalLight, DomInputSurface, Group,
    IcosahedronGeometry, Matrix4, Mesh, MeshBasicMaterial, MeshPhongMaterial, OrbitControls, PerspectiveCamera,
    Plane, PlaneGeometry, Scene, SphereGeometry, Uniform, Uniforms, Vector3,
    type BufferGeometry
} from 'threekt';
import { ShaderMaterial, WebGL2Context, WebGLRenderer, WebGLRenderTarget } from 'threekt/render-gl';
import { addToggle, examplePanel } from './exampleControls';
import { fitPerspective } from './fitPerspective';
import { encodeSrgb } from './fullscreenPass';

export function createReflectiveRoom(canvas: HTMLCanvasElement, portals: boolean) {
    const scene = new Scene();
    scene.background = Color.fromHex(0x141618);
    scene.add(new AmbientLight(Color.fromHex(0xffffff), 1.35));
    const light = new DirectionalLight(Color.fromHex(0xffedd9), 3);
    light.position.set(1, 2, 4);
    scene.add(light);
    const roomGeometry = new PlaneGeometry(100.1, 100.1);
    for (const [x,y,z,rx,ry,color] of [
        [0,0,0,-Math.PI/2,0,0xe4d8cf], [0,100,0,Math.PI/2,0,0xe9e9e9],
        [0,50,-50,0,0,0x7777ff], [0,50,50,0,Math.PI,0xe99ee9],
        [50,50,0,0,-Math.PI/2,0x339950], [-50,50,0,0,Math.PI/2,0xc94343],
    ]) {
        const wall = new Mesh(roomGeometry, new MeshPhongMaterial(Color.fromHex(color)));
        wall.position.set(x,y,z);
        wall.rotation.set(rx,ry,0);
        scene.add(wall);
    }
    const camera = new PerspectiveCamera(48, 1, 1, 500);
    camera.position.set(0, 75, 128);
    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.target.set(0, 43, 0);
    controls.enablePan = false;
    controls.minDistance = 48;
    controls.maxDistance = 180;
    controls.maxPolarAngle = 1.8;
    controls.minAzimuthAngle = -0.72;
    controls.maxAzimuthAngle = 0.72;
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const scratch = new PerspectiveCamera(48, 1, 0.5, 500);
    const matrix = new Matrix4();
    const makePanel = (geometry: BufferGeometry, tint: Color) => {
        const target = new WebGLRenderTarget();
        const projection = new Matrix4();
        const material = portals ? new MeshBasicMaterial(Color.fromHex(0xffffff), target.texture) : new ShaderMaterial(`
            uniform mat4 reflectedViewProjection;
            varying vec4 reflected;
            void main() {
                reflected = reflectedViewProjection * modelMatrix * vec4(position,1);
                gl_Position = projectionMatrix * modelViewMatrix * vec4(position,1);
            }
        `, `
            uniform sampler2D reflection;
            uniform vec3 tint;
            varying vec4 reflected;
            ${encodeSrgb}
            void main() {
                vec2 uv = reflected.xy / reflected.w * 0.5 + 0.5;
                gl_FragColor = vec4(encodeSrgb(texture2D(reflection,uv).rgb * tint),1);
            }
        `, new Uniforms().set('reflectedViewProjection', new Uniform(projection)).set('reflection', new Uniform(target.texture))
            .set('tint', new Uniform(tint)));
        const mesh = new Mesh(geometry, material);
        scene.add(mesh);
        return { mesh, target, projection };
    };
    const first = makePanel(portals ? new PlaneGeometry(35,35) : new CircleGeometry(36,64), new Color(0.75,0.84,0.87));
    const second = makePanel(portals ? new PlaneGeometry(35,35) : new PlaneGeometry(66,66), new Color(0.88,0.89,0.96));
    if (portals) {
        first.mesh.position.set(-29,26,0);
        second.mesh.position.set(29,26,0);
    } else {
        first.mesh.position.y = 0.6;
        first.mesh.rotation.x = -Math.PI / 2;
        second.mesh.position.set(0,51,-49.6);
    }

    const ballGeometry = new IcosahedronGeometry(5.5, 0);
    const moving = [0xffffff,0xf9b735].slice(0, portals ? 2 : 1).map(hex => {
        const mat = new MeshPhongMaterial(Color.fromHex(hex));
        mat.flatShading = true;
        mat.emissive.setHex(0x434344);
        const mesh = new Mesh(ballGeometry,mat);
        scene.add(mesh);
        return mesh;
    });
    if (!portals) {
        const fixed = new Group();
        const shellMaterial = new MeshPhongMaterial(Color.fromHex(0xffffff));
        const shell = new Mesh(new SphereGeometry(14,24,24,Math.PI/2,Math.PI*2,0,Math.PI*2/3),shellMaterial);
        const lid = new Mesh(new CylinderGeometry(12,12,0.25,32),shellMaterial);
        lid.position.y = -7;
        fixed.add(shell);
        fixed.add(lid);
        fixed.position.set(0,29,0);
        scene.add(fixed);
    }

    const position = new Vector3(), direction = new Vector3(), normal = new Vector3(), up = new Vector3();
    const cornerA = new Vector3(), cornerB = new Vector3(), cornerC = new Vector3(), right = new Vector3();
    const va = new Vector3(), vb = new Vector3(), vc = new Vector3();
    const clip = new Plane();
    const panel = examplePanel(canvas);
    const active = [true,true];
    if (!portals) {
        addToggle(panel, 'Floor mirror', true, value => { active[0] = value; });
        addToggle(panel, 'Wall mirror', true, value => { active[1] = value; });
    } else panel.remove();

    function portalView(from: typeof first, to: typeof first) {
        from.mesh.worldToLocal(position.copyFrom(camera.position));
        position.x = -position.x;
        position.z = -position.z;
        scratch.position.copyFrom(to.mesh.localToWorld(position));
        to.mesh.localToWorld(cornerA.set(17.5,-17.5,0));
        to.mesh.localToWorld(cornerB.set(-17.5,-17.5,0));
        to.mesh.localToWorld(cornerC.set(17.5,17.5,0));
        right.subVectors(cornerB,cornerA).normalize();
        up.subVectors(cornerC,cornerA).normalize();
        normal.crossVectors(right,up).normalize();
        va.subVectors(cornerA,scratch.position);
        vb.subVectors(cornerB,scratch.position);
        vc.subVectors(cornerC,scratch.position);
        const distance = -va.dot(normal);
        if (distance <= 0.1) return;
        const near = distance + 0.03;
        const f = near / distance;
        scratch.quaternion.setFromRotationMatrix(matrix.makeBasis(right,up,normal));
        scratch.projectionMatrix.makePerspective(va.dot(right)*f,vb.dot(right)*f,vc.dot(up)*f,va.dot(up)*f,near,500);
        scratch.projectionMatrixInverse.copyFrom(scratch.projectionMatrix).invert();
        renderer.setRenderTarget(from.target);
        renderer.render(scene,scratch);
    }
    function mirrorView(entry: typeof first) {
        normal.set(0,0,1).applyQuaternion(entry.mesh.quaternion);
        const origin = entry.mesh.position;
        position.copyFrom(camera.position).sub(origin).reflect(normal).add(origin);
        camera.getWorldDirection(direction).reflect(normal);
        up.set(0,1,0).applyQuaternion(camera.quaternion).reflect(normal);
        scratch.position.copyFrom(position);
        scratch.up.copyFrom(up);
        scratch.lookAt(position.add(direction));
        scratch.projectionMatrix.copyFrom(camera.projectionMatrix);
        scratch.projectionMatrixInverse.copyFrom(camera.projectionMatrixInverse);
        scratch.updateMatrixWorld();
        entry.projection.multiplyMatrices(scratch.projectionMatrix,scratch.matrixWorldInverse);
        clip.setFromNormalAndCoplanarPoint(normal,origin);
        renderer.clippingPlanes = [clip];
        renderer.setRenderTarget(entry.target);
        renderer.render(scene,scratch);
        renderer.clippingPlanes = [];
    }

    return {
        render(seconds: number) {
            if (!fitPerspective(canvas,camera,renderer)) return;
            controls.update();
            camera.updateMatrixWorld();
            scene.updateMatrixWorld();
            moving.forEach((mesh,i) => {
                const phase = seconds * 0.83 + i * Math.PI;
                mesh.position.set(Math.sin(phase)*38,25 + Math.cos(phase*1.4)*15,Math.cos(phase)*32);
                mesh.rotation.set(seconds,seconds*0.7,0);
            });
            for (const entry of [first,second]) {
                entry.mesh.visible = false;
                entry.target.setSize(portals ? 384 : canvas.width, portals ? 384 : canvas.height);
            }
            if (portals) {
                portalView(first,second);
                portalView(second,first);
            } else {
                if (active[0]) mirrorView(first);
                if (active[1]) mirrorView(second);
            }
            first.mesh.visible = active[0];
            second.mesh.visible = active[1];
            renderer.setRenderTarget(null);
            renderer.render(scene,camera);
        },
        dispose() { controls.dispose(); renderer.dispose(); panel.remove(); },
    };
}
