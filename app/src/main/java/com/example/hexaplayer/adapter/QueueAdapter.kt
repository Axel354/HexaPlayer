package com.example.hexaplayer.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.hexaplayer.R
import com.example.hexaplayer.data.Song
import com.example.hexaplayer.databinding.ItemQueueSongBinding

class QueueAdapter(
    private val onItemClick: (index: Int) -> Unit,
    private val onRemove: (index: Int) -> Unit,
    private val onMove: (from: Int, to: Int) -> Unit
) : RecyclerView.Adapter<QueueAdapter.ViewHolder>() {

    private var songs: MutableList<Song> = mutableListOf()
    private var currentIndex: Int = -1
    var dragEnabled: Boolean = true

    var itemTouchHelper: ItemTouchHelper? = null

    fun setData(newSongs: List<Song>, newCurrentIndex: Int) {
        songs = newSongs.toMutableList()
        currentIndex = newCurrentIndex
        notifyDataSetChanged()
    }

    fun moveItem(from: Int, to: Int) {
        val item = songs.removeAt(from)
        songs.add(to, item)
        if (currentIndex == from) {
            currentIndex = to
        } else if (from < currentIndex && to >= currentIndex) {
            currentIndex--
        } else if (from > currentIndex && to <= currentIndex) {
            currentIndex++
        }
        notifyItemMoved(from, to)
    }

    inner class ViewHolder(val binding: ItemQueueSongBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemQueueSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    override fun getItemCount() = songs.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]
        val isCurrent = position == currentIndex
        with(holder.binding) {
            tvTitle.text = song.title
            tvArtist.text = song.artist
            ivAlbumArt.load(SongAdapter.albumArtUri(song.albumId)) {
                placeholder(R.drawable.ic_music_note)
                error(R.drawable.ic_music_note)
            }

            playingIndicator.visibility =
                if (isCurrent) android.view.View.VISIBLE else android.view.View.INVISIBLE
            ivNowPlaying.visibility =
                if (isCurrent) android.view.View.VISIBLE else android.view.View.GONE
            tvTitle.setTextColor(
                root.context.getColor(
                    if (isCurrent) R.color.colorPrimary else R.color.colorText
                )
            )

            root.setOnClickListener { onItemClick(holder.bindingAdapterPosition) }
            btnRemove.setOnClickListener { onRemove(holder.bindingAdapterPosition) }

            ivDragHandle.visibility = if (dragEnabled) android.view.View.VISIBLE else android.view.View.GONE
            ivDragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN && dragEnabled) {
                    itemTouchHelper?.startDrag(holder)
                }
                false
            }
        }
    }
}
