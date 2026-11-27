import { AudioContext, WebAudioContext } from 'threekt/audio';
import { examplePanel } from './exampleControls';

/**
 * Adds a play/pause button that creates the shared Threekt audio context on the first click, as browsers only
 * start audio after a user gesture. [initialize] builds the example's audio objects once the context runs.
 */
export function exampleAudio(canvas: HTMLCanvasElement, initialize: (context: WebAudioContext) => Promise<void>, parent?: HTMLElement) {
    const panel = parent ?? examplePanel(canvas);
    const button = document.createElement('button');
    button.textContent = 'Play audio';
    const status = document.createElement('p');
    status.setAttribute('role', 'status');
    status.hidden = true;
    panel.append(button, status);
    let context: WebAudioContext | undefined;
    let ready = false;
    let disposed = false;
    const native = () => context?.context as globalThis.AudioContext | undefined;
    const close = async () => {
        const closing = native();
        context = undefined;
        ready = false;
        AudioContext.getInstance().setContext(null);
        if (closing && closing.state !== 'closed') await closing.close();
    };
    button.addEventListener('click', async () => {
        button.disabled = true;
        status.hidden = true;
        try {
            if (!context) {
                context = new WebAudioContext();
                AudioContext.getInstance().setContext(context);
            }
            if (native()!.state === 'running' && ready) {
                await native()!.suspend();
                button.textContent = 'Resume audio';
                return;
            }
            await native()!.resume();
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
            await close();
            button.textContent = 'Retry audio';
        } finally {
            button.disabled = false;
        }
    });
    return {
        /** Whether the audio objects exist and the context is producing sound. */
        get running() { return ready && native()?.state === 'running'; },
        dispose() {
            disposed = true;
            button.remove();
            status.remove();
            if (!parent) panel.remove();
            void close();
        },
    };
}

/** Creates a looping media element for `WebAudioContext.createMediaElementSource`, as upstream's `<audio loop>` tags. */
export function loopingMediaElement(url: string) {
    const element = document.createElement('audio');
    element.src = url;
    element.loop = true;
    element.crossOrigin = 'anonymous';
    return element;
}

export const badCatSound = new URL('./assets/sounds/376737_Skullbeatz___Bad_Cat_Maste.ogg', import.meta.url).href;
