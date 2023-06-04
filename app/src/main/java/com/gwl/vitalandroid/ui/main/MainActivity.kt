package com.gwl.vitalandroid.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.ViewDataBinding
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.gwl.vitalandroid.R
import com.gwl.vitalandroid.base.BaseActivity
import com.gwl.vitalandroid.databinding.ActivityMainBinding
import com.gwl.vitalandroid.showInstallHealthConnectAlert
import com.gwl.vitalandroid.ui.ID
import com.gwl.vitalandroid.ui.NAME
import com.gwl.vitalandroid.ui.TITLE
import com.gwl.vitalandroid.ui.detail.DetailActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    override val layoutResId: Int get() = R.layout.activity_main
    override val bindingInflater: (LayoutInflater) -> ViewDataBinding get() = ActivityMainBinding::inflate
    override val binding: ActivityMainBinding get() = super.binding as ActivityMainBinding

    private val viewModel: MainActivityViewModel by viewModels()

    private val listAdapter by lazy { ActivityListAdapter() }

    private val requestPermissionLauncher =
        registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { isGranted ->
            if (isGranted.containsAll(viewModel.healthConnectManager.permissions)) {
                viewModel.startToReadHealthConnectData(resources = resources)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialise()
    }

    override fun onResume() {
        super.onResume()
    }

    private fun initialise() {
        binding.viewModel = viewModel
        binding.activityListRecyclerView.adapter = listAdapter
        listAdapter.onItemClickListener = viewModel

        addObservers()

        if (HealthConnectClient.isProviderAvailable(this)) {
            if (viewModel.isPermissionGranted.value == false) {
                requestPermissionLauncher.launch(viewModel.healthConnectManager.permissions)
            } else {
                binding.progressBar.visibility = View.VISIBLE
                viewModel.startToReadHealthConnectData(resources = resources)
            }
        } else {
            binding.txvTitle.setText(R.string.install_health_connect)
            showInstallHealthConnectAlert()
        }
    }

    fun addObservers() {
        viewModel.healthActivities.observe(this) {
            binding.progressBar.visibility = View.GONE

            binding.txvTitle.setText(R.string.activities)
            if (it.isNotEmpty())
                listAdapter.activityList = it
            else
                binding.txvTitle.setText(R.string.no_activities)
        }

        viewModel.onActivityClick.observe(this) {
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra(ID, it.id)
            intent.putExtra(NAME, it.exerciseName)
            intent.putExtra(TITLE, it.title ?: "")
            startActivity(
                intent
            )
        }
    }
}