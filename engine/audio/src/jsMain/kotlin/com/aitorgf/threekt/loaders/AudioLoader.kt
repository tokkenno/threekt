package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.audio.AudioContext
import com.aitorgf.threekt.audio.AudioGraphBuffer
import com.aitorgf.threekt.audio.WebAudioContext
import kotlinx.browser.window
import org.khronos.webgl.ArrayBuffer
import org.w3c.fetch.Response
import kotlin.js.Promise

/**
 * Loads audio files with `fetch` and decodes them with the shared [AudioContext], which must be a
 * [WebAudioContext].
 *
 * Threekt deviations: upstream extends `Loader` and reads through `FileLoader` and a `LoadingManager`, which Threekt
 * has not ported yet; [path] is the only shared setting. Progress is not reported.
 */
@JsExport
class AudioLoader {
    /** Prefix for every requested URL. */
    var path = ""

    fun setPath(value: String): AudioLoader {
        path = value
        return this
    }

    /** Loads and decodes [url], then calls [onLoad] with the buffer, or [onError] with the failure. */
    fun load(url: String, onLoad: (AudioGraphBuffer) -> Unit, onError: ((Throwable) -> Unit)? = null) {
        loadAsync(url).then(onLoad).catch { error ->
            if (onError != null) onError(error) else console.error(error)
        }
    }

    /** Loads and decodes [url]. */
    fun loadAsync(url: String): Promise<AudioGraphBuffer> =
        window.fetch(path + url).then { response: Response ->
            if (!response.ok) throw IllegalStateException("AudioLoader: HTTP ${response.status} for ${path + url}.")
            response.arrayBuffer()
        }.then { buffer: ArrayBuffer ->
            val context = AudioContext.getContext() as? WebAudioContext
                ?: throw IllegalStateException("AudioLoader: the shared AudioContext is not a WebAudioContext.")
            context.decodeAudioData(buffer)
        }.unsafeCast<Promise<AudioGraphBuffer>>()
}
