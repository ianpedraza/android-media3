package com.ianpedraza.media3.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.ianpedraza.media3.R
import com.ianpedraza.media3.databinding.ActivityMainBinding
import com.ianpedraza.media3.utils.viewBinding

@UnstableApi
class MainActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityMainBinding::inflate)

    private var player: ExoPlayer? = null

    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    private val playbackStateListener: Player.Listener = playbackStateListener()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun initializePlayer() {
        // Chooses tracks between media Items for adaptive mediaItems
        val trackSelector = DefaultTrackSelector(this).apply {
            // Instruct it to choose only sd or lower, this is to save expensive data
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build()
            .also { exoPlayer ->
                binding.videoView.player = exoPlayer

                val mediaItem = MediaItem.fromUri(getString(R.string.media_url_mp4))
                exoPlayer.setMediaItem(mediaItem)

                val secondMediaItem = MediaItem.fromUri(getString(R.string.media_url_mp3))
                exoPlayer.addMediaItem(secondMediaItem)

                /**
                 * DASH -> MimeTypes.APPLICATION_MPD
                 * HLS -> MimeTypes.APPLICATION_M3U8
                 * SmoothStreaming -> MimeTypes.APPLICATION_SS
                 */

                val adaptiveMediaItem = MediaItem.Builder()
                    .setUri(getString(R.string.media_url_dash))
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .build()

                exoPlayer.addMediaItem(adaptiveMediaItem)
            }

        player?.apply {
            this.playWhenReady = this@MainActivity.playWhenReady
            seekTo(currentItem, playbackPosition)
            addListener(playbackStateListener)
            prepare()
        }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playWhenReady = exoPlayer.playWhenReady
            currentItem = exoPlayer.currentMediaItemIndex
            playbackPosition = exoPlayer.currentPosition
            exoPlayer.removeListener(playbackStateListener)
            exoPlayer.release()
        }
        player = null
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.videoView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        private const val TAG = "MainActivity"

        private fun playbackStateListener() = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateString: String = when (playbackState) {
                    ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                    ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                    ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
                    ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                    else -> "UNKNOWN_STATE             -"
                }
                Log.d(TAG, "onPlaybackStateChanged: $stateString")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val playingString: String = if (isPlaying) "PLAYING" else "NOT PLAYING"
                Log.d(TAG, "player is currently $playingString")
            }

            override fun onRenderedFirstFrame() {
                // initial frame was rendered
            }

            override fun onEvents(player: Player, events: Player.Events) {
                // Reports all events in a single iteration

                // Example
                if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) || events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                    // Update the UI
                    Log.d(TAG, "player UI needs to be updated")
                }

                // Example
                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    // skip to 5 seconds whenever we change the track
                    player.seekTo(5000L)
                }
            }
        }
    }
}
