package com.gwl.vitalandroid.ui.main

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gwl.vitalandroid.R
import com.gwl.vitalandroid.dataBind
import com.gwl.vitalandroid.health_connect.ExerciseSession

class ActivityListAdapter : RecyclerView.Adapter<ActivityListItemViewHolder>() {

    var activityList: List<ExerciseSession>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityListItemViewHolder {
        return ActivityListItemViewHolder(dataBind(parent, R.layout.item_activity_list))
    }

    override fun getItemId(position: Int): Long {
        return activityList?.get(position)?.id?.toLong() ?: position.toLong()
    }

    override fun getItemCount(): Int {
        return activityList?.size ?: 0
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: ActivityListItemViewHolder, position: Int) {
        holder.bind(activityList?.get(position), onItemClickListener)
    }
}