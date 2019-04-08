package com.celestial.gps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_login.*

/**
 * A simple [Fragment] subclass.
 */
class LoginFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return  inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        one_photo_mode.setOnClickListener {
            login(it, ONE_PHOTO)
        }

        two_photo_mode.setOnClickListener {
            login(it, TWO_PHOTO)
        }

        test_button.setOnClickListener { view ->
            view.findNavController().navigate(R.id.testFragment)
        }

        orientation_button.setOnClickListener { view ->
            view.findNavController().navigate(R.id.orientationFragment)
        }
    }

    private fun login(view: View, mode: Int) {
        val apiKey = AstrometryModel.ApiKey(ASTROMETRY_API_KEY)

        disposable =
            astrometryService.login(Gson().toJson(apiKey))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { result ->
                        AstrometryManager.solvingMode = mode
                        AstrometryManager.sessionKey = result.session
                        if (mode == ONE_PHOTO) {
                            AstrometryManager.currentPhoto = FIRST_PHOTO
                            view.findNavController().navigate(R.id.summaryOneFragment)
                        } else {
                            view.findNavController().navigate(R.id.summaryFragment)
                        }
                    },
                    { error -> showError(error.message) }
                )
    }
}
