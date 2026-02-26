package com.example.hexaplayer.ui.player

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import com.example.hexaplayer.R
import com.example.hexaplayer.adapter.SongAdapter
import com.example.hexaplayer.databinding.FragmentPlayerBinding
import com.example.hexaplayer.ui.queue.QueueFragment
import com.example.hexaplayer.util.themeColor
import com.example.hexaplayer.util.toTimeString
import com.example.hexaplayer.viewmodel.MusicViewModel
import kotlin.math.abs

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MusicViewModel by activityViewModels()

    private var isUserSeeking = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        setupControls()
        setupAlbumArtGestures()
        observeViewModel()
    }

    // ── Gestures ─────────────────────────────────────────────────────────────

    private fun setupAlbumArtGestures() {
        val gestureDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?, e2: MotionEvent,
                    velocityX: Float, velocityY: Float
                ): Boolean {
                    if (e1 == null) return false
                    val deltaX = e2.x - e1.x
                    val deltaY = e2.y - e1.y

                    if (deltaY > 120 && velocityY > 300 && abs(velocityY) > abs(velocityX)) {
                        parentFragmentManager.popBackStack()
                        return true
                    }

                    if (abs(deltaX) > 80 && abs(velocityX) > 200 && abs(velocityX) > abs(velocityY)) {
                        val toLeft = deltaX < 0
                        animateArtSwipe(toLeft) {
                            if (toLeft) viewModel.playNext() else viewModel.playPrevious()
                        }
                        return true
                    }

                    return false
                }
            }
        )
        binding.ivAlbumArt.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun animateArtSwipe(toLeft: Boolean, onSwipe: () -> Unit) {
        val iv = binding.ivAlbumArt
        val dir = if (toLeft) -1f else 1f
        val offset = iv.width.toFloat().coerceAtLeast(80f) * 0.45f

        iv.animate()
            .translationX(dir * offset)
            .alpha(0f)
            .setDuration(140)
            .withEndAction {
                onSwipe()
                iv.translationX = -dir * offset
                iv.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    // ── Controls ──────────────────────────────────────────────────────────────

    private fun setupControls() {
        binding.btnClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnQueue.setOnClickListener { QueueFragment().show(parentFragmentManager, "queue") }
        binding.btnPlayPause.setOnClickListener { viewModel.togglePlayPause() }
        binding.btnNext.setOnClickListener { viewModel.playNext() }
        binding.btnPrevious.setOnClickListener { viewModel.playPrevious() }
        binding.btnShuffle.setOnClickListener { viewModel.toggleShuffle() }
        binding.btnRepeat.setOnClickListener { viewModel.toggleRepeat() }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = viewModel.duration.value ?: 0L
                    binding.tvCurrentTime.text = (duration * progress / 1000L).toTimeString()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) { isUserSeeking = true }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val duration = viewModel.duration.value ?: 0L
                viewModel.seekTo(duration * seekBar.progress / 1000L)
                isUserSeeking = false
            }
        })
    }

    // ── ViewModel observers ───────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.currentSong.observe(viewLifecycleOwner) { song ->
            if (song == null) return@observe
            binding.tvTitle.text = song.title
            binding.tvArtist.text = song.artist
            binding.tvAlbum.text = song.album
            loadAlbumArtWithPalette(song.albumId)
        }

        viewModel.isPlaying.observe(viewLifecycleOwner) { playing ->
            binding.btnPlayPause.setImageResource(
                if (playing) R.drawable.ic_pause else R.drawable.ic_play_arrow
            )
        }

        viewModel.currentPosition.observe(viewLifecycleOwner) { pos ->
            if (!isUserSeeking) {
                val duration = viewModel.duration.value ?: 0L
                binding.tvCurrentTime.text = pos.toTimeString()
                if (duration > 0) binding.seekBar.progress = (pos * 1000 / duration).toInt()
            }
        }

        viewModel.duration.observe(viewLifecycleOwner) { dur ->
            binding.tvTotalTime.text = dur.toTimeString()
        }

        viewModel.shuffleMode.observe(viewLifecycleOwner) { shuffle ->
            val accent = requireContext().themeColor(com.google.android.material.R.attr.colorPrimary)
            val muted = ContextCompat.getColor(requireContext(), R.color.colorTextSecondary)
            binding.btnShuffle.setColorFilter(if (shuffle) accent else muted)
        }

        viewModel.repeatMode.observe(viewLifecycleOwner) { mode ->
            val accent = requireContext().themeColor(com.google.android.material.R.attr.colorPrimary)
            val muted = ContextCompat.getColor(requireContext(), R.color.colorTextSecondary)
            when (mode) {
                Player.REPEAT_MODE_OFF -> {
                    binding.btnRepeat.setImageResource(R.drawable.ic_repeat)
                    binding.btnRepeat.setColorFilter(muted)
                }
                Player.REPEAT_MODE_ALL -> {
                    binding.btnRepeat.setImageResource(R.drawable.ic_repeat)
                    binding.btnRepeat.setColorFilter(accent)
                }
                Player.REPEAT_MODE_ONE -> {
                    binding.btnRepeat.setImageResource(R.drawable.ic_repeat_one)
                    binding.btnRepeat.setColorFilter(accent)
                }
            }
        }
    }

    // ── Palette background ────────────────────────────────────────────────────

    private fun loadAlbumArtWithPalette(albumId: Long) {
        val uri = SongAdapter.albumArtUri(albumId)
        requireContext().imageLoader.enqueue(
            ImageRequest.Builder(requireContext())
                .data(uri)
                .allowHardware(false)  // required for Palette to access pixels
                .target(binding.ivAlbumArt)
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .listener(
                    onSuccess = { _, result ->
                        val bmp = (result.drawable as? BitmapDrawable)?.bitmap
                        if (bmp != null) applyPaletteBackground(bmp)
                        else clearBackground()
                    },
                    onError = { _, _ -> clearBackground() }
                )
                .build()
        )
    }

    private fun applyPaletteBackground(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            if (_binding == null) return@generate
            val bgColor = ContextCompat.getColor(requireContext(), R.color.colorBackground)
            val swatch = palette?.darkVibrantSwatch
                ?: palette?.vibrantSwatch
                ?: palette?.darkMutedSwatch
                ?: palette?.dominantSwatch
            if (swatch != null) {
                val topColor = ColorUtils.blendARGB(swatch.rgb, bgColor, 0.30f)
                val midColor = ColorUtils.blendARGB(swatch.rgb, bgColor, 0.68f)
                binding.root.background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(topColor, midColor, bgColor)
                )
            } else {
                clearBackground()
            }
        }
    }

    private fun clearBackground() {
        _binding?.root?.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.colorBackground)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
