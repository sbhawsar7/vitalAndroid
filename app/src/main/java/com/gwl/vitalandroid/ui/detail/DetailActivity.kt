package com.gwl.vitalandroid.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.ViewDataBinding
import com.gwl.vitalandroid.R
import com.gwl.vitalandroid.base.BaseActivity
import com.gwl.vitalandroid.databinding.ActivityDetailBinding
import com.gwl.vitalandroid.health_connect.ExerciseSession
import com.gwl.vitalandroid.ui.ID
import com.gwl.vitalandroid.ui.NAME
import com.gwl.vitalandroid.ui.TITLE
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DetailActivity : BaseActivity() {
    override val layoutResId: Int get() = R.layout.activity_detail
    override val bindingInflater: (LayoutInflater) -> ViewDataBinding get() = ActivityDetailBinding::inflate
    override val binding: ActivityDetailBinding get() = super.binding as ActivityDetailBinding

    private val viewModel: DetailActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialise()
    }

    override fun onResume() {
        super.onResume()
    }

    private fun initialise() {
        val exerciseId = intent.extras?.getString(ID)
        val exerciseName = intent.extras?.getString(NAME)
        val exerciseTitle = intent.extras?.getString(TITLE)
        exerciseId?.let {
            binding.progressBar.visibility = View.VISIBLE
            viewModel.startToReadHealthConnectData(exerciseId)
        }
        viewModel.healthSessionData.observe(this) {
            binding.progressBar.visibility = View.GONE
            binding.excercise = ExerciseSession()
            binding.excercise?.id = exerciseId ?: ""
            binding.excercise?.exerciseName = exerciseName ?: ""
            binding.excercise?.title = exerciseTitle ?: ""
            binding.excercise?.exerciseSessionData = it
        }
    }
}