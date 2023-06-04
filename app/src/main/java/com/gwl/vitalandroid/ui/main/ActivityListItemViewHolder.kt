package com.gwl.vitalandroid.ui.main

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.gwl.vitalandroid.BR
import com.gwl.vitalandroid.health_connect.ExerciseSession

open class ActivityListItemViewHolder(private val dataBinding: ViewDataBinding) :
    RecyclerView.ViewHolder(dataBinding.root) {

    fun bind(categoryItem: ExerciseSession?, categoryClickListener: OnItemClickListener?) {
        dataBinding.setVariable(BR.item, categoryItem)
        dataBinding.setVariable(BR.listener, categoryClickListener)
    }
}

interface OnItemClickListener {
    fun onItemClick(item: ExerciseSession)
}
