import {
    InstancedMesh, Mesh, Object3D, PerspectiveCamera, Scene, ShaderMaterial, Side, SphereGeometry,
    Uniform, Uniforms, Vector3, WebGL2Context, WebGLRenderer, WebGLRenderTarget,
} from 'threekt';
import { cubeLookup, loadExampleCubeMap } from './exampleCubeMap';
import { followPointer } from './exampleControls';
import { fitPerspective } from './fitPerspective';
import { encodeSrgb, fullscreenPass } from './fullscreenPass';

const worldPosition = `
    varying vec3 vWorld, vNormal;
    void main() {
        mat4 transform = modelMatrix;
        #ifdef USE_INSTANCING
        transform *= instanceMatrix;
        #endif
        vWorld = (transform * vec4(position, 1.0)).xyz;
        vNormal = normalize(mat3(transform) * normal);
        gl_Position = projectionMatrix * viewMatrix * vec4(vWorld, 1.0);
    }
`;

export async function createStereoExample(canvas: HTMLCanvasElement, effect: 'stereo' | 'anaglyph' | 'parallaxbarrier') {
    const stereo = effect === 'stereo';
    const uniforms = await loadExampleCubeMap(stereo ? 'Park3Med' : 'pisa');
    const scene = new Scene();
    const background = new ShaderMaterial(worldPosition, `
        varying vec3 vWorld;
        ${cubeLookup}
        void main() { gl_FragColor = vec4(sampleCube(vWorld - cameraPosition), 1.0); }
    `, uniforms);
    background.side = Side.Back;
    scene.add(new Mesh(new SphereGeometry(45, 32, 16), background));

    const material = new ShaderMaterial(worldPosition, `
        varying vec3 vWorld, vNormal;
        ${cubeLookup}
        void main() {
            vec3 eye = normalize(vWorld - cameraPosition);
            vec3 n = normalize(vNormal);
            gl_FragColor = vec4(sampleCube(${stereo ? 'refract(eye, n, 0.95)' : 'reflect(eye, n)'}), 1.0);
        }
    `, uniforms);
    const count = 500;
    const spheres = new InstancedMesh(new SphereGeometry(0.1, 32, 16), material, count);
    const scales = Float32Array.from({ length: count }, () => 1 + Math.random() * 3);
    const depths = Float32Array.from({ length: count }, () => Math.random() * 10 - 5);
    const dummy = new Object3D();
    scene.add(spheres);

    const camera = new PerspectiveCamera(60, 1, 0.01, 100);
    camera.position.z = 3;
    const pointer = followPointer(canvas, camera, scene);
    const eye = new PerspectiveCamera(60, 1, 0.01, 100);
    const right = new Vector3();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));
    const leftTarget = new WebGLRenderTarget();
    const rightTarget = new WebGLRenderTarget();
    const pass = fullscreenPass(`
        uniform sampler2D leftEye, rightEye;
        ${encodeSrgb}
        void main() {
            ${stereo ? `
                vec2 uv = vec2(fract(vUv.x * 2.0), vUv.y);
                vec3 color = vUv.x < 0.5 ? texture2D(leftEye, uv).rgb : texture2D(rightEye, uv).rgb;
            ` : `
                vec3 leftColor = texture2D(leftEye, vUv).rgb;
                vec3 rightColor = texture2D(rightEye, vUv).rgb;
                ${effect === 'anaglyph' ? `
                    mat3 leftMatrix = mat3(0.4561,-0.0401,-0.0152,0.5005,-0.0378,-0.0206,0.1764,-0.0158,-0.0055);
                    mat3 rightMatrix = mat3(-0.0435,0.3785,-0.0721,-0.0879,0.7342,-0.1129,-0.0016,-0.0185,1.2264);
                    vec3 color = clamp(leftMatrix * leftColor + rightMatrix * rightColor, 0.0, 1.0);
                ` : 'vec3 color = mod(floor(gl_FragCoord.y), 2.0) == 0.0 ? leftColor : rightColor;'}
            `}
            gl_FragColor = vec4(encodeSrgb(color), 1.0);
        }
    `, new Uniforms().set('leftEye', new Uniform(leftTarget.texture)).set('rightEye', new Uniform(rightTarget.texture)));
    const renderEye = (target: WebGLRenderTarget, offset: number) => {
        target.setSize(stereo ? Math.max(1, Math.floor(canvas.width / 2)) : canvas.width, canvas.height);
        eye.aspect = target.width / target.height;
        // The same point projects to the center of both views at the convergence distance.
        eye.filmOffset = -offset * eye.getFilmWidth() / (effect === 'anaglyph' ? 3 : camera.focus);
        eye.updateProjectionMatrix();
        eye.position.copyFrom(camera.position).addScaledVector(right, offset);
        eye.quaternion.copyFrom(camera.quaternion);
        renderer.setRenderTarget(target);
        renderer.render(scene, eye);
    };

    return {
        render(seconds: number) {
            if (!fitPerspective(canvas, camera, renderer)) return;
            pointer.update();
            camera.position.x = Math.max(-8, Math.min(8, camera.position.x));
            camera.position.y = Math.max(-8, Math.min(8, camera.position.y));
            camera.lookAt(scene.position);
            right.set(1, 0, 0).applyQuaternion(camera.quaternion);
            for (let i = 0; i < count; i++) {
                dummy.position.set(5 * Math.cos(seconds * 0.6 + i), 5 * Math.sin(seconds * 0.6 + i * 1.1), depths[i]);
                dummy.scale.setScalar(scales[i]);
                dummy.updateMatrix();
                spheres.setMatrixAt(i, dummy.matrix);
            }
            spheres.instanceMatrix.markUpdated();
            renderEye(leftTarget, -0.032);
            renderEye(rightTarget, 0.032);
            renderer.setRenderTarget(null);
            renderer.render(pass.scene, pass.camera);
        },
        dispose() { pointer.dispose(); renderer.dispose(); },
    };
}
