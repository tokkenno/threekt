import { Quaternion, Vector3, type Camera, type Object3D } from 'threekt';
import { examplePanel } from './exampleControls';

export function exampleAudio(canvas: HTMLCanvasElement, initialize: (context: AudioContext) => Promise<void>, parent?: HTMLElement) {
    const panel = parent ?? examplePanel(canvas);
    const button = document.createElement('button');
    button.textContent = 'Play audio';
    const status = document.createElement('p');
    status.setAttribute('role', 'status');
    status.hidden = true;
    panel.append(button, status);
    let context: AudioContext | undefined;
    let ready = false;
    let disposed = false;
    button.addEventListener('click', async () => {
        button.disabled = true;
        status.hidden = true;
        try {
            context ??= new AudioContext();
            if (context.state === 'running' && ready) {
                await context.suspend();
                button.textContent = 'Resume audio';
                return;
            }
            await context.resume();
            if (!ready) {
                button.textContent = 'Loading audio…';
                await initialize(context);
                ready = true;
            }
            button.textContent = 'Pause audio';
        } catch (error) {
            if (disposed) return;
            status.textContent = String(error);
            status.hidden = false;
            const failed = context;
            context = undefined;
            if (failed && failed.state !== 'closed') await failed.close();
            ready = false;
            button.textContent = 'Retry audio';
        } finally {
            button.disabled = false;
        }
    });
    return {
        get context() { return ready ? context : undefined; },
        dispose() {
            disposed = true;
            button.remove();
            status.remove();
            if (!parent) panel.remove();
            if (context && context.state !== 'closed') void context.close();
        },
    };
}

export async function loadAudioBuffer(context: AudioContext, url: string) {
    const response = await fetch(url);
    if (!response.ok) throw new Error('Could not load sound (HTTP ' + response.status + ').');
    return context.decodeAudioData(await response.arrayBuffer());
}

export function playAudioBuffer(context: AudioContext, buffer: AudioBuffer, destination: AudioNode, loop = false, when = 0) {
    const source = context.createBufferSource();
    source.buffer = buffer;
    source.loop = loop;
    source.connect(destination);
    source.start(when);
    return source;
}

export function spatialAudio() {
    const position = new Vector3(), forward = new Vector3(), up = new Vector3();
    const orientation = new Quaternion();
    return {
        updateListener(context: AudioContext, camera: Camera) {
            camera.getWorldPosition(position);
            camera.getWorldQuaternion(orientation);
            forward.set(0, 0, -1).applyQuaternion(orientation);
            up.set(0, 1, 0).applyQuaternion(orientation);
            const listener = context.listener;
            listener.positionX.value = position.x;
            listener.positionY.value = position.y;
            listener.positionZ.value = position.z;
            listener.forwardX.value = forward.x;
            listener.forwardY.value = forward.y;
            listener.forwardZ.value = forward.z;
            listener.upX.value = up.x;
            listener.upY.value = up.y;
            listener.upZ.value = up.z;
        },
        updatePanner(panner: PannerNode, node: Object3D) {
            node.getWorldPosition(position);
            node.getWorldQuaternion(orientation);
            forward.set(0, 0, 1).applyQuaternion(orientation);
            panner.positionX.value = position.x;
            panner.positionY.value = position.y;
            panner.positionZ.value = position.z;
            panner.orientationX.value = forward.x;
            panner.orientationY.value = forward.y;
            panner.orientationZ.value = forward.z;
        },
    };
}

export const badCatSound = new URL('./assets/sounds/376737_Skullbeatz___Bad_Cat_Maste.ogg', import.meta.url).href;
