package com.celestial.gps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_summary.*
import java.util.*

/**
 * A simple [Fragment] subclass.
 */
class SummaryFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        upload_button1.setOnClickListener {
            AstrometryManager.currentPhoto = 0
            view.findNavController().navigate(R.id.photoFragment)
        }

        upload_button2.setOnClickListener {
            AstrometryManager.currentPhoto = 1
            view.findNavController().navigate(R.id.photoFragment)
        }

        check_button1.setOnClickListener {
            checkStatus(FIRST_PHOTO)
        }

        check_button2.setOnClickListener {
            checkStatus(SECOND_PHOTO)
        }

        calculate_button.setOnClickListener {
            if (checkEnoughData()) {
                calculateLocation()
            }
        }

        if (AstrometryManager.photo != null) {
            Glide.with(this).load(AstrometryManager.photo).into(preview1)
        }

        if (AstrometryManager.photo2 != null) {
            Glide.with(this).load(AstrometryManager.photo2).into(preview2)
        }
    }

    @Suppress("SENSELESS_COMPARISON")
    private fun checkStatus(currentPhoto: Int) {
        val currentJobResult =
            if (currentPhoto == FIRST_PHOTO) AstrometryManager.jobResult1 else AstrometryManager.jobResult2
        val currentJobs =
            if (currentPhoto == FIRST_PHOTO) AstrometryManager.jobs else AstrometryManager.jobs2
        val currentSubId =
            if (currentPhoto == FIRST_PHOTO) AstrometryManager.subId else AstrometryManager.subId2

        if (currentSubId == 0) {
            Toast.makeText(context, "Please upload photo first", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentJobs == null || currentJobs.isEmpty() || currentJobs[0] == null) {
            disposable = astrometryService.getSubmissionStatus(currentSubId.toString())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { result ->
                        if (currentPhoto == FIRST_PHOTO) {
                            AstrometryManager.jobs = result?.jobs
                        } else {
                            AstrometryManager.jobs2 = result?.jobs
                        }
                        Toast.makeText(context, "Getting job ids", Toast.LENGTH_SHORT).show()
                        disposable?.dispose()
                    },
                    { error -> showError(error.message) }
                )
            return
        }

        if (currentJobResult == null) {
            astrometryService.getJobResults(currentJobs[0].toString())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { result ->
                        if (currentPhoto == FIRST_PHOTO) {
                            status_1.text = result.status
                            if (result.status == "success") {
                                AstrometryManager.jobResult1 = result
                                Toast.makeText(context, "Solved!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            status_2.text = result.status
                            if (result.status == "success") {
                                AstrometryManager.jobResult2 = result
                                Toast.makeText(context, "Solved!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    { error -> showError(error.message) }
                )
        }
    }

    private fun checkEnoughData(): Boolean {
        if (AstrometryManager.photo == null || AstrometryManager.photo2 == null) {
            Toast.makeText(context, "Haven't upload photo", Toast.LENGTH_SHORT).show()
            return false
        }

        if (AstrometryManager.orientationAngles1.isEmpty() || AstrometryManager.orientationAngles2.isEmpty()) {
            Toast.makeText(context, "Haven't set orientation", Toast.LENGTH_SHORT).show()
            return false
        }

        if (AstrometryManager.jobResult1 == null || AstrometryManager.jobResult2 == null) {
            Toast.makeText(
                context,
                "Haven't finish solving photo or Photo failed to solve",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    private fun calculateLocation() {
        if (checkEnoughData()) {
            val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val azimuth1 = if (AstrometryManager.orientationAngles1[0].toDouble() > 0 ) AstrometryManager.orientationAngles1[0].toDouble() else AstrometryManager.orientationAngles1[0].toDouble() + 2 * Math.PI
            val azimuth2 = if (AstrometryManager.orientationAngles2[0].toDouble() > 0) AstrometryManager.orientationAngles2[0].toDouble() else AstrometryManager.orientationAngles2[0].toDouble()
            val altitude1 = if (AstrometryManager.orientationAngles1[1].toDouble() > 0) AstrometryManager.orientationAngles1[1].toDouble() else AstrometryManager.orientationAngles1[1].toDouble()
            val altitude2 = if (AstrometryManager.orientationAngles2[1].toDouble() > 0) AstrometryManager.orientationAngles2[1].toDouble() else AstrometryManager.orientationAngles2[1].toDouble()
            val location = getLocation(
                ra1 = AstrometryManager.jobResult1?.calibration?.ra!!,
                dec1 = AstrometryManager.jobResult1?.calibration?.dec!!,
                azimuth1 = azimuth1,
                altitude1 = altitude1,
                ra2 = AstrometryManager.jobResult2?.calibration?.ra!!,
                dec2 = AstrometryManager.jobResult2?.calibration?.dec!!,
                azimuth2 = azimuth2,
                altitude2 = altitude2
            )
            longitude.text = location?.longitude.toString()
            latitude.text = location?.latitude.toString()
        }
    }

}
