package com.example.hexaplayer.ui.queue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hexaplayer.R
import com.example.hexaplayer.adapter.QueueAdapter
import com.example.hexaplayer.databinding.FragmentQueueBinding
import com.example.hexaplayer.viewmodel.MusicViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class QueueFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentQueueBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MusicViewModel by activityViewModels()

    private val queueAdapter = QueueAdapter(
        onItemClick = { index ->
            viewModel.playQueueItemAt(index)
            dismiss()
        },
        onRemove = { index ->
            viewModel.removeFromQueue(index)
        },
        onMove = { from, to ->
            viewModel.moveQueueItem(from, to)
        }
    )

    private val dragCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
    ) {
        private var dragFrom = -1
        private var dragTo = -1

        override fun isLongPressDragEnabled() = false

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition
            if (dragFrom == -1) dragFrom = from
            dragTo = to
            queueAdapter.moveItem(from, to)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                viewModel.moveQueueItem(dragFrom, dragTo)
            }
            dragFrom = -1
            dragTo = -1
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQueueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemTouchHelper = ItemTouchHelper(dragCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvQueue)
        queueAdapter.itemTouchHelper = itemTouchHelper

        binding.rvQueue.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQueue.adapter = queueAdapter

        viewModel.shuffleMode.observe(viewLifecycleOwner) { shuffled ->
            queueAdapter.dragEnabled = !shuffled
            binding.tvShuffleNote.visibility = if (shuffled) View.VISIBLE else View.GONE
        }

        viewModel.currentQueue.observe(viewLifecycleOwner) { songs ->
            val index = viewModel.currentQueueIndex.value ?: 0
            queueAdapter.setData(songs, index)
            updateTitle(songs.size)
            val isEmpty = songs.isEmpty()
            binding.tvEmptyQueue.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvQueue.visibility = if (isEmpty) View.GONE else View.VISIBLE
            if (!isEmpty && index >= 0 && index < songs.size) {
                binding.rvQueue.scrollToPosition(index)
            }
        }

        viewModel.currentQueueIndex.observe(viewLifecycleOwner) { index ->
            val songs = viewModel.currentQueue.value ?: return@observe
            queueAdapter.setData(songs, index)
            if (index >= 0 && index < songs.size) {
                binding.rvQueue.scrollToPosition(index)
            }
        }
    }

    private fun updateTitle(count: Int) {
        binding.tvQueueTitle.text = if (count == 0) {
            getString(R.string.queue)
        } else {
            "${getString(R.string.queue)} · $count songs"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
