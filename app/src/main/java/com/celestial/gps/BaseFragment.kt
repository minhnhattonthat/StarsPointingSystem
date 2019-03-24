package com.celestial.gps

import android.widget.Toast
import androidx.fragment.app.Fragment
import io.reactivex.disposables.Disposable

open class BaseFragment: Fragment(){

    var disposable: Disposable? = null

    val astrometryService by lazy {
        AstrometryService.create()
    }

    fun showError(error: String?) {
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        disposable?.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }
}