package net.bjoernpetersen.musicbot.spi.plugin

import kotlinx.coroutines.CompletableDeferred

/**
 * A feedback channel back to the player to send signals about playback state changes
 * outside of the bot's control.
 *
 * For example, if the user paused the official Spotify client directly, the Spotify Playback may
 * detect this and signal the new [PlaybackState.PAUSE] state.
 *
 * The listener may be called with the current state even if it is unchanged. The player won't react
 * if the state hasn't actually changed.
 *
 * This listener does **not** have to be called when one of the [Playback.play], [Playback.pause]
 * or [Playback.close] methods were called.
 *
 * ### Note
 * If the playback has finished, don't call this listener, but rather release all waiting threads
 * from the [Playback.waitForFinish] method instead.
 */
typealias PlaybackStateListener = suspend (PlaybackState) -> Unit

/**
 * A kind of Playback state.
 */
enum class PlaybackState {

    /**
     * The Playback is playing.
     */
    PLAY,
    /**
     * The playback is paused, but not stopped, finished, or broken.
     */
    PAUSE,
    /**
     * The playback is broken and won't be able to continue playing.
     *
     * The player will react to this by closing the playback and moving on to the next song.
     */
    BROKEN
}

/**
 * A plugin that provides playback objects for some media/input type.
 *
 * Note that this interface is relatively worthless by itself, because it doesn't have
 * a `createPlayback` method. The reason for that is, that the signature of such a method is highly
 * dependent on the capabilities of the media/input format a PlaybackFactory can handle.
 *
 * Have a look at the `predefined` subpackage for some subtypes that are sensible to implement or
 * depend on.
 */
interface PlaybackFactory : Plugin

/**
 * Playback for a single song. Playback should not start before [play] is called the first time.
 */
interface Playback {

    /**
     * Provides the playback with a PlaybackStateListener.
     * The listener can be used to tell the player about external pause/resume events.
     */
    fun setPlaybackStateListener(listener: PlaybackStateListener) = Unit

    /**
     * Resumes the playback. This might be called if the playback is already playing.
     *
     * This is also called to initially start playing.
     */
    fun play()

    /**
     * Pauses the playback. This might be called if the playback is already paused.
     */
    fun pause()

    suspend fun waitForFinish()

    @Throws(Exception::class)
    suspend fun close()
}

/**
 * Abstract Playback implementation providing a [lock] and an associated [done] condition,
 * as well as an implementation for the [waitForFinish] method and a [markDone] method.
 *
 * @param lock A lock which will be used for critical code
 * @param done A condition which will come true when this Playback finishes
 */
abstract class AbstractPlayback protected constructor() : Playback {

    protected val done = CompletableDeferred<Unit>()

    protected var playbackListener: PlaybackStateListener? = null
        private set

    override fun setPlaybackStateListener(listener: PlaybackStateListener) {
        playbackListener = listener
    }

    protected fun isDone(): Boolean = done.isCompleted

    /**
     * Waits for the [done] condition.
     */
    override suspend fun waitForFinish() {
        done.await()
    }

    /**
     * Signals all threads waiting for the [done] condition.
     */
    protected fun markDone() {
        done.complete(Unit)
    }

    @Throws(Exception::class)
    override suspend fun close() {
        markDone()
    }
}
