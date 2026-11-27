import { RenderTargetType, PerspectiveCamera, Uniform, Uniforms, WebGLRenderTarget,
    type Camera, type Scene, type WebGLRenderer } from 'threekt';
import { fullscreenPass, texturePass } from './fullscreenPass';

// The 32-sample jitter sequence from Three.js SSAARenderPass, in sixteenths of a pixel.
const offsets = [
    [-4,-7],[-7,-5],[-3,-5],[-5,-4],[-1,-4],[-2,-2],[-6,-1],[-4,0],
    [-7,1],[-1,2],[-6,3],[-3,3],[-7,6],[-3,6],[-5,7],[-1,7],
    [5,-7],[1,-6],[6,-5],[4,-4],[2,-3],[7,-2],[1,-1],[4,-1],
    [2,1],[6,2],[0,4],[4,4],[2,5],[7,5],[5,7],[3,7],
];
const smallerPatterns = [
    [[0, 0]], [[4, 4], [-4, -4]], [[-2, -6], [6, -2], [-6, 2], [2, 6]],
    [[1, -3], [-1, 3], [5, 1], [-3, -5], [-5, 5], [-7, -1], [3, 7], [7, -7]],
    [[1, 1], [-1, -3], [-3, 2], [4, -1], [-5, -2], [2, 5], [5, 3], [3, -5],
        [-2, 6], [0, -7], [-4, -6], [-6, 4], [-8, 0], [7, -4], [6, 7], [-7, -8]],
];

export function exampleSupersampling(renderer: WebGLRenderer) {
    const renderTarget = new WebGLRenderTarget();
    let read = new WebGLRenderTarget(1, 1, RenderTargetType.Float, false);
    let write = new WebGLRenderTarget(1, 1, RenderTargetType.Float, false);
    const previous = new Uniform(read.texture);
    const weight = new Uniform(1);
    const average = fullscreenPass(`
        uniform sampler2D previousFrame, sampleFrame;
        uniform float weight;
        void main() {
            vec4 sampleColor = texture2D(sampleFrame, vUv);
            if (weight >= 1.0) gl_FragColor = sampleColor;
            else gl_FragColor = mix(texture2D(previousFrame, vUv), sampleColor, weight);
        }
    `, new Uniforms().set('previousFrame', previous).set('sampleFrame', new Uniform(renderTarget.texture)).set('weight', weight));
    const display = texturePass(read.texture);
    let count = 0;
    let totalWeight = 0;
    return {
        get count() { return count; },
        resize(width: number, height: number) {
            if (renderTarget.width === width && renderTarget.height === height) return;
            for (const target of [renderTarget, read, write]) target.setSize(width, height);
            count = totalWeight = 0;
        },
        reset() { count = totalWeight = 0; },
        sample(scene: Scene, camera: Camera, totalSamples: number, unbiased = false) {
            if (count >= totalSamples) return;
            const pattern = smallerPatterns[Math.log2(totalSamples)] ?? offsets;
            const [x, y] = pattern[count];
            const elements = camera.projectionMatrix.elements;
            const index = camera instanceof PerspectiveCamera ? 8 : 12;
            const originalX = elements[index], originalY = elements[index + 1];
            const direction = index === 8 ? -1 : 1;
            elements[index] += x / 8 / renderTarget.width * direction;
            elements[index + 1] += y / 8 / renderTarget.height * direction;
            try {
                renderer.setRenderTarget(renderTarget);
                renderer.render(scene, camera);
            } finally {
                elements[index] = originalX;
                elements[index + 1] = originalY;
            }
            const contribution = unbiased ? 1 + (count + 0.5 - totalSamples / 2) / 32 / totalSamples : 1;
            totalWeight += contribution;
            weight.value = contribution / totalWeight;
            previous.value = read.texture;
            renderer.setRenderTarget(write);
            renderer.render(average.scene, average.camera);
            [read, write] = [write, read];
            count++;
        },
        present() {
            display.input.value = read.texture;
            renderer.setRenderTarget(null);
            renderer.render(display.scene, display.camera);
        },
    };
}
