package com.gwl.vitalandroid

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun Activity.showInstallHealthConnectAlert() {
    val healthConnectPackage = "com.google.android.apps.healthdata"
    if (!isFinishing) {
        CoroutineScope(Dispatchers.Main).launch {
            android.app.AlertDialog.Builder(this@showInstallHealthConnectAlert)
                .setTitle(R.string.alert)
                .setMessage(R.string.install_health_connect)
                .setPositiveButton(R.string.install) { _, _ ->
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=$healthConnectPackage")
                            )
                        )
                    } catch (e: Exception) {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$healthConnectPackage")
                            )
                        )
                    }
                }.setNegativeButton(R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }
    }
}

fun Double.roundTo(numFractionDigits: Int): Double {
    val factor = 10.0.pow(numFractionDigits.toDouble())
    return (this * factor).roundToInt() / factor
}


fun dataBind(
    parent: ViewGroup,
    layoutId: Int
): ViewDataBinding {
    return DataBindingUtil.inflate(
        LayoutInflater.from(parent.context), layoutId, parent, false
    )
}
