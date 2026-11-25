import {
    Blending, Box, BoxHelper, BufferGeometry, DomInputSurface, Euler, Float32BufferAttribute, Group, LineBasicMaterial,
    LineSegments, Mesh, OrbitControls, PerspectiveCamera, Points, PointsMaterial, Scene, Usage, Vector3, WebGL2Context,
    WebGLRenderer,
} from 'threekt';
import { fitPerspective } from './fitPerspective';

type ParticleData = { velocity: Vector3; numConnections: number };

/**
 * Port of Three.js examples/webgl_buffergeometry_drawrange.html, without the Stats panel. An HTML panel
 * replaces lil-gui with the same settings.
 */
export async function createExample(canvas: HTMLCanvasElement) {
    const particlesData: ParticleData[] = [];

    const maxParticleCount = 1000;
    let particleCount = 500;
    const r = 800;
    const rHalf = r / 2;

    const effectController = {
        showDots: true,
        showLines: true,
        minDistance: 150,
        limitConnections: false,
        maxConnections: 20,
        particleCount: 500,
    };

    const camera = new PerspectiveCamera(45, 1, 1, 4000);
    camera.position.z = 1750;

    const controls = new OrbitControls(camera, new DomInputSurface(canvas));
    controls.minDistance = 1000;
    controls.maxDistance = 3000;

    const scene = new Scene();

    const group = new Group();
    scene.add(group);

    const helper = new BoxHelper(new Mesh(new Box(r, r, r)));
    (helper.material as LineBasicMaterial).color.setHex(0x474747);
    helper.material.blending = Blending.Additive;
    helper.material.transparent = true;
    group.add(helper);

    const segments = maxParticleCount * maxParticleCount;

    const positions = new Float32Array(segments * 3);
    const colors = new Float32Array(segments * 3);

    const pMaterial = new PointsMaterial();
    pMaterial.size = 3;
    pMaterial.blending = Blending.Additive;
    pMaterial.transparent = true;
    pMaterial.sizeAttenuation = false;

    const particles = new BufferGeometry();
    const particlePositions = new Float32Array(maxParticleCount * 3);

    for (let i = 0; i < maxParticleCount; i++) {
        const x = Math.random() * r - r / 2;
        const y = Math.random() * r - r / 2;
        const z = Math.random() * r - r / 2;

        particlePositions[i * 3] = x;
        particlePositions[i * 3 + 1] = y;
        particlePositions[i * 3 + 2] = z;

        // add it to the geometry
        particlesData.push({
            velocity: new Vector3(-1 + Math.random() * 2, -1 + Math.random() * 2, -1 + Math.random() * 2),
            numConnections: 0,
        });
    }

    particles.setDrawRange(0, particleCount);
    const particlePositionAttribute = new Float32BufferAttribute(particlePositions, 3);
    particles.setAttribute('position', particlePositionAttribute.setUsage(Usage.DynamicDraw));

    // create the particle system
    const pointCloud = new Points(particles, pMaterial);
    group.add(pointCloud);

    const geometry = new BufferGeometry();
    const positionAttribute = new Float32BufferAttribute(positions, 3);
    const colorAttribute = new Float32BufferAttribute(colors, 3);
    geometry.setAttribute('position', positionAttribute.setUsage(Usage.DynamicDraw));
    geometry.setAttribute('color', colorAttribute.setUsage(Usage.DynamicDraw));

    geometry.computeBoundingSphere();

    geometry.setDrawRange(0, 0);

    const material = new LineBasicMaterial();
    material.vertexColors = true;
    material.blending = Blending.Additive;
    material.transparent = true;

    const linesMesh = new LineSegments(geometry, material);
    group.add(linesMesh);

    const panel = createPanel(effectController, maxParticleCount, {
        showDots: (value) => { pointCloud.visible = value; },
        showLines: (value) => { linesMesh.visible = value; },
        particleCount: (value) => {
            particleCount = value;
            particles.setDrawRange(0, particleCount);
        },
    });
    canvas.parentElement?.append(panel);

    const rotation = new Euler();
    const renderer = new WebGLRenderer(new WebGL2Context(canvas));

    function animate() {
        let vertexpos = 0;
        let colorpos = 0;
        let numConnected = 0;

        for (let i = 0; i < particleCount; i++) particlesData[i].numConnections = 0;

        for (let i = 0; i < particleCount; i++) {
            // get the particle
            const particleData = particlesData[i];

            particlePositions[i * 3] += particleData.velocity.x;
            particlePositions[i * 3 + 1] += particleData.velocity.y;
            particlePositions[i * 3 + 2] += particleData.velocity.z;

            if (particlePositions[i * 3 + 1] < -rHalf || particlePositions[i * 3 + 1] > rHalf)
                particleData.velocity.y = -particleData.velocity.y;

            if (particlePositions[i * 3] < -rHalf || particlePositions[i * 3] > rHalf)
                particleData.velocity.x = -particleData.velocity.x;

            if (particlePositions[i * 3 + 2] < -rHalf || particlePositions[i * 3 + 2] > rHalf)
                particleData.velocity.z = -particleData.velocity.z;

            if (effectController.limitConnections && particleData.numConnections >= effectController.maxConnections)
                continue;

            // Check collision
            for (let j = i + 1; j < particleCount; j++) {
                const particleDataB = particlesData[j];
                if (effectController.limitConnections && particleDataB.numConnections >= effectController.maxConnections)
                    continue;

                const dx = particlePositions[i * 3] - particlePositions[j * 3];
                const dy = particlePositions[i * 3 + 1] - particlePositions[j * 3 + 1];
                const dz = particlePositions[i * 3 + 2] - particlePositions[j * 3 + 2];
                const dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (dist < effectController.minDistance) {
                    particleData.numConnections++;
                    particleDataB.numConnections++;

                    const alpha = 1.0 - dist / effectController.minDistance;

                    positions[vertexpos++] = particlePositions[i * 3];
                    positions[vertexpos++] = particlePositions[i * 3 + 1];
                    positions[vertexpos++] = particlePositions[i * 3 + 2];

                    positions[vertexpos++] = particlePositions[j * 3];
                    positions[vertexpos++] = particlePositions[j * 3 + 1];
                    positions[vertexpos++] = particlePositions[j * 3 + 2];

                    colors[colorpos++] = alpha;
                    colors[colorpos++] = alpha;
                    colors[colorpos++] = alpha;

                    colors[colorpos++] = alpha;
                    colors[colorpos++] = alpha;
                    colors[colorpos++] = alpha;

                    numConnected++;
                }
            }
        }

        linesMesh.geometry.setDrawRange(0, numConnected * 2);
        positionAttribute.markUpdated();
        colorAttribute.markUpdated();
        particlePositionAttribute.markUpdated();
    }

    return {
        render() {
            if (!fitPerspective(canvas, camera, renderer)) return;
            animate();

            const time = Date.now() * 0.001;
            group.quaternion.setFromEuler(rotation.set(0, time * 0.1, 0));

            renderer.render(scene, camera);
        },
        dispose() {
            panel.remove();
            controls.dispose();
            renderer.dispose();
        },
    };
}

type Settings = {
    showDots: boolean;
    showLines: boolean;
    minDistance: number;
    limitConnections: boolean;
    maxConnections: number;
    particleCount: number;
};

/** Builds the settings panel that replaces lil-gui. Inputs write straight into [settings]. */
function createPanel(
    settings: Settings,
    maxParticleCount: number,
    onChange: { showDots: (value: boolean) => void; showLines: (value: boolean) => void; particleCount: (value: number) => void },
) {
    const panel = document.createElement('aside');
    panel.className = 'example-panel';
    const instructions = document.createElement('p');
    instructions.textContent = 'Drag to orbit and scroll to zoom.';
    panel.append(instructions);

    const labels = {
        showDots: 'Show points',
        showLines: 'Show lines',
        limitConnections: 'Limit connections',
        minDistance: 'Connection distance',
        maxConnections: 'Max connections',
        particleCount: 'Particle count',
    };

    const checkbox = (name: 'showDots' | 'showLines' | 'limitConnections', change?: (value: boolean) => void) => {
        const label = document.createElement('label');
        label.className = 'example-setting';
        const input = document.createElement('input');
        input.type = 'checkbox';
        input.checked = settings[name];
        input.addEventListener('change', () => {
            settings[name] = input.checked;
            change?.(input.checked);
        });
        const text = document.createElement('span');
        text.textContent = labels[name];
        label.append(text, input);
        panel.append(label);
    };

    const range = (
        name: 'minDistance' | 'maxConnections' | 'particleCount', min: number, max: number, step: number | 'any',
        change?: (value: number) => void,
    ) => {
        const label = document.createElement('label');
        label.className = 'example-setting';
        const text = document.createElement('span');
        text.textContent = labels[name];
        const output = document.createElement('output');
        output.textContent = String(settings[name]);
        const input = document.createElement('input');
        input.type = 'range';
        input.min = String(min);
        input.max = String(max);
        input.step = String(step);
        input.value = String(settings[name]);
        input.addEventListener('input', () => {
            settings[name] = input.valueAsNumber;
            output.textContent = step === 'any' ? input.valueAsNumber.toFixed(1) : String(input.valueAsNumber);
            change?.(input.valueAsNumber);
        });
        label.append(text, output, input);
        panel.append(label);
    };

    checkbox('showDots', onChange.showDots);
    checkbox('showLines', onChange.showLines);
    range('minDistance', 10, 300, 'any');
    checkbox('limitConnections');
    range('maxConnections', 0, 30, 1);
    range('particleCount', 0, maxParticleCount, 1, onChange.particleCount);
    return panel;
}
