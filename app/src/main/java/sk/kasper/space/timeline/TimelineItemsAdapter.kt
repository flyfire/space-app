package sk.kasper.space.timeline

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import sk.kasper.space.R
import sk.kasper.space.databinding.FragmentTimelineLabelListItemBinding
import sk.kasper.space.databinding.FragmentTimelineLaunchListItemBinding
import sk.kasper.space.timeline.TimelineListItem.Companion.LABEL_TYPE
import sk.kasper.space.timeline.TimelineListItem.Companion.LAUNCH_TYPE
import sk.kasper.space.utils.HorizontalSpaceItemDecoration
import sk.kasper.space.utils.RoundedSquareLetterProvider
import sk.kasper.space.utils.toPixels


class TimelineItemsAdapter(context: Context, val listener: LaunchListItemViewModel.OnListInteractionListener): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    private val timelineItems: MutableList<TimelineListItem> = mutableListOf()

    private val circleLetterProvider = RoundedSquareLetterProvider(context)

    private val tagRecycledViewPool = RecyclerView.RecycledViewPool()

    fun setTimelineItems(items: List<TimelineListItem>) {
        val diffResult = DiffUtil.calculateDiff(DiffUtilCallback(items, timelineItems))
        timelineItems.clear()
        timelineItems.addAll(items)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int) = timelineItems[position].getType()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LabelViewHolder -> {
                holder.update(timelineItems[position] as LabelListItem)
            }
            is LaunchViewHolder -> {
                holder.update(timelineItems[position] as LaunchListItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                LABEL_TYPE -> {
                    LabelViewHolder(inflater.inflate(R.layout.fragment_timeline_label_list_item, parent, false))
                }
                LAUNCH_TYPE -> {
                    LaunchViewHolder(inflater.inflate(R.layout.fragment_timeline_launch_list_item, parent, false))
                }
                else -> throw IllegalArgumentException("Unknown view type: $viewType")
            }

    override fun getItemCount() = timelineItems.size

    class LabelViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val viewModel = LabelListItemViewModel()

        init {
            FragmentTimelineLabelListItemBinding.bind(itemView).viewModel = viewModel
        }

        fun update(item: LabelListItem) {
            viewModel.labelListItem = item
        }
    }

    inner class LaunchViewHolder(view: View): RecyclerView.ViewHolder(view) {
        private val viewModel = LaunchListItemViewModel(listener)
        private val binding = FragmentTimelineLaunchListItemBinding.bind(view)
        private val tagAdapter = TagAdapter()

        init {
            binding.viewModel = viewModel
            binding.tagsView.setRecycledViewPool(tagRecycledViewPool)
            binding.tagsView.adapter = tagAdapter
            binding.tagsView.addItemDecoration(HorizontalSpaceItemDecoration(R.dimen.launch_tag_horizontal_space.toPixels(view.context)))
            binding.provider = circleLetterProvider
        }

        fun update(item: LaunchListItem) {
            viewModel.launchListItem = item
        }

    }

    class DiffUtilCallback(private val newItems: List<TimelineListItem>, private val oldItems: List<TimelineListItem>)
        : DiffUtil.Callback() {

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            if (oldItem.getType() == newItem.getType()) {
                when {
                    oldItem.getType() == LABEL_TYPE -> return oldItem == newItem
                    oldItem.getType() == LAUNCH_TYPE -> (oldItem as LaunchListItem).let {
                        (newItem as LaunchListItem).let {
                            return oldItem.id == newItem.id
                        }
                    }
                    else -> return false
                }
            } else {
                return false
            }
        }

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldItems[oldItemPosition] == newItems[newItemPosition]

    }

}