package com.aitorgf.threekt.audio

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.math.Vector3
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AudioTest {
    private val graph = FakeAudioGraph()

    @BeforeTest
    fun useFakeGraph() = AudioContext.setContext(graph)

    @AfterTest
    fun resetContext() = AudioContext.setContext(null)

    @Test
    fun listenerRoutesThroughOptionalFilter() {
        val listener = AudioListener()
        assertSame(graph, listener.context)
        assertTrue(graph.isConnected(listener.gain, graph.destination))
        val filter = graph.createGain()
        listener.setFilter(filter)
        assertFalse(graph.isConnected(listener.gain, graph.destination))
        assertTrue(graph.isConnected(listener.gain, filter))
        assertTrue(graph.isConnected(filter, graph.destination))
        listener.removeFilter()
        assertTrue(graph.isConnected(listener.gain, graph.destination))
        assertEquals(1, graph.connections.size)
    }

    @Test
    fun playPauseAndResumeTrackProgress() {
        val audio = Audio(AudioListener()).setBuffer(FakeAudioGraph.Buffer(10.0))
        graph.currentTime = 2.0
        audio.play(0.5)
        val first = graph.sources.single()
        assertEquals(Triple(2.5, 0.0, null), first.started)
        assertTrue(graph.isConnected(first, audio.gain))
        graph.currentTime = 4.5
        audio.pause()
        assertFalse(audio.isPlaying)
        assertEquals(0.0, first.stoppedAt)
        audio.play()
        assertEquals(Triple(4.5, 2.0, null), graph.sources[1].started)
    }

    @Test
    fun pausedLoopsWrapTheirProgress() {
        val audio = Audio(AudioListener()).setBuffer(FakeAudioGraph.Buffer(3.0)).setLoop(true).setPlaybackRate(2.0)
        audio.play()
        graph.currentTime = 2.0
        audio.pause().play()
        assertEquals(1.0, graph.sources[1].started!!.second)
    }

    @Test
    fun playingTwiceAndEndingKeepState() {
        val audio = Audio(AudioListener()).setBuffer(FakeAudioGraph.Buffer(1.0))
        audio.play().play()
        assertEquals(1, graph.sources.size)
        graph.sources.single().onEnded!!()
        assertFalse(audio.isPlaying)
        audio.play()
        assertEquals(2, graph.sources.size)
    }

    @Test
    fun nodeSourcesHaveNoPlaybackControl() {
        val audio = Audio(AudioListener())
        val node = graph.createGain()
        audio.setNodeSource(node).play()
        assertEquals(Audio.SourceType.AudioNode, audio.sourceType)
        assertTrue(graph.sources.isEmpty())
        assertTrue(graph.isConnected(node, audio.gain))
    }

    @Test
    fun filtersAreChainedBetweenSourceAndOutput() {
        val audio = Audio(AudioListener())
        val source = graph.createGain()
        val a = graph.createGain()
        val b = graph.createGain()
        audio.setNodeSource(source).setFilters(arrayOf(a, b))
        assertTrue(graph.isConnected(source, a) && graph.isConnected(a, b) && graph.isConnected(b, audio.gain))
        assertFalse(graph.isConnected(source, audio.gain))
        audio.setFilter(null)
        assertTrue(graph.isConnected(source, audio.gain))
        assertFalse(graph.isConnected(source, a))
    }

    @Test
    fun volumesFadeToTheirTarget() {
        val listener = AudioListener().setMasterVolume(0.3)
        val audio = Audio(listener).setVolume(0.6)
        assertEquals(0.3, listener.getMasterVolume())
        assertEquals(0.6, audio.getVolume())
    }

    @Test
    fun clonesCopyBufferPlaybackSettings() {
        val audio = PositionalAudio(AudioListener()).apply { setBuffer(FakeAudioGraph.Buffer(4.0)).setLoop(true) }
        audio.offset = 1.5
        val clone = audio.clone(true)
        assertNotSame(audio.panner, clone.panner)
        assertSame(audio.buffer, clone.buffer)
        assertTrue(clone.getLoop())
        assertEquals(1.5, clone.offset)
    }

    @Test
    fun listenerFollowsItsWorldPose() {
        val listener = AudioListener()
        listener.position.set(1.0, 2.0, 3.0)
        listener.rotation.y = kotlin.math.PI / 2
        graph.currentTime = 5.0
        listener.updateMatrixWorld(true)
        val pose = graph.listener
        assertEquals(1.0, pose.positionX.ramp!!.first)
        assertEquals(3.0, pose.positionZ.ramp!!.first)
        // Turning +90° about Y points the forward axis from -Z to -X.
        assertEquals(-1.0, pose.forwardX.ramp!!.first, 1e-12)
        assertEquals(0.0, pose.forwardZ.ramp!!.first, 1e-12)
        assertEquals(1.0, pose.upY.ramp!!.first, 1e-12)
        assertTrue(pose.positionX.ramp!!.second >= 5.0)
    }

    @Test
    fun positionalAudioMovesItsPannerOnlyWhilePlaying() {
        val audio = PositionalAudio(AudioListener()).apply { setBuffer(FakeAudioGraph.Buffer(1.0)) }
        val panner = audio.panner as FakeAudioGraph.Panner
        assertEquals(PanningModel.HRTF, panner.panningModel)
        assertTrue(graph.isConnected(panner, audio.gain))
        audio.position.set(4.0, 0.0, 0.0)
        audio.updateMatrixWorld(true)
        assertEquals(null, panner.positionX.ramp)
        audio.play()
        assertTrue(graph.isConnected(graph.sources.single(), panner))
        audio.updateMatrixWorld(true)
        assertEquals(4.0, panner.positionX.ramp!!.first)
        assertEquals(1.0, panner.orientationZ.ramp!!.first)
        audio.setDirectionalCone(180.0, 230.0, 0.1).setRefDistance(20.0)
        assertEquals(230.0, panner.coneOuterAngle)
        assertEquals(20.0, audio.getRefDistance())
    }

    @Test
    fun poseUpdatesKeepTheLocalTransform() {
        val camera = Object3D().apply { position.set(3.0, 2.0, 3.0) }
        val listener = AudioListener().apply { position.set(0.0, 1.0, 0.0) }
        camera.add(listener)
        val radio = Object3D().apply { position.y = 0.2; scale.setScalar(20.0) }
        val audio = PositionalAudio(listener).apply { setNodeSource(graph.createGain()) }
        radio.add(audio)
        repeat(3) {
            camera.updateMatrixWorld(true)
            radio.updateMatrixWorld(true)
        }
        assertEquals(Vector3(0.0, 1.0, 0.0), listener.position)
        assertEquals(Vector3(), audio.position)
        assertEquals(Vector3(1.0, 1.0, 1.0), audio.scale)
        assertEquals(Vector3(0.0, 1.0, 0.0), listener.up)
        assertEquals(3.0, graph.listener.positionY.ramp!!.first)
        val panner = audio.panner as FakeAudioGraph.Panner
        assertEquals(0.2, panner.positionY.ramp!!.first)
        assertEquals(0.0, panner.positionX.ramp!!.first)
    }

    @Test
    fun analyserAveragesUnsignedMagnitudes() {
        val audio = Audio(AudioListener())
        val analyser = AudioAnalyser(audio, 8)
        assertEquals(4, analyser.data.size)
        assertTrue(graph.isConnected(audio.gain, analyser.analyser))
        (analyser.analyser as FakeAudioGraph.Analyser).magnitudes = byteArrayOf(0, 100, 200.toByte(), 255.toByte())
        assertEquals(138.75, analyser.getAverageFrequency())
    }
}
