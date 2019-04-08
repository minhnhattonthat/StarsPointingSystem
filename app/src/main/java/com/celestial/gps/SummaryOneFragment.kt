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
import kotlinx.android.synthetic.main.fragment_summary_one.*
import java.util.*

/**
 * A simple [Fragment] subclass.
 *
 */
class SummaryOneFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_summary_one, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        upload_button1.setOnClickListener {
            AstrometryManager.currentPhoto = 0
            view.findNavController().navigate(R.id.photoFragment)
        }

        check_button1.setOnClickListener {
            checkStatus()
        }

        calculate_button.setOnClickListener {
            if (checkEnoughData()) {
                calculateLocation()
            }
        }

        if (AstrometryManager.photo != null) {
            Glide.with(this).load(AstrometryManager.photo).into(preview1)
        }
    }

    @Suppress("SENSELESS_COMPARISON")
    private fun checkStatus() {
        val currentJobResult = AstrometryManager.jobResult1
        val currentJobs = AstrometryManager.jobs
        val currentSubId = AstrometryManager.subId

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
                        AstrometryManager.jobs = result?.jobs
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
                        status_1.text = result.status
                        if (result.status == "success") {
                            AstrometryManager.jobResult1 = result
                            Toast.makeText(context, "Solved!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    { error -> showError(error.message) }
                )
        }
    }

    private fun checkEnoughData(): Boolean {
        if (AstrometryManager.photo == null) {
            Toast.makeText(context, "Haven't upload photo", Toast.LENGTH_SHORT).show()
            return false
        }

        if (AstrometryManager.orientationAngles1.isEmpty()) {
            Toast.makeText(context, "Haven't set orientation", Toast.LENGTH_SHORT).show()
            return false
        }

        if (AstrometryManager.jobResult1 == null) {
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
            var azimuth2 = AstrometryManager.orientationAngles1[0].toDouble() + Math.PI / 2
            if (azimuth2 > 2 * Math.PI) {
                azimuth2 -= 2 * Math.PI
            } else if (azimuth2 < 0) {
                azimuth2 += 2 * Math.PI
            }

            val altitude2 = (AstrometryManager.orientationAngles1[1].toDouble() + Math.PI / 2) * (-1)

            val location = getLocationNew(
                ra1 = AstrometryManager.jobResult1?.calibration?.ra!!,
                dec1 = AstrometryManager.jobResult1?.calibration?.dec!!,
                azimuth2 = azimuth2,
                altitude2 = altitude2,
                timeInMillis = Calendar.getInstance().timeInMillis
            )
            longitude.text = location?.longitude.toString()
            latitude.text = location?.latitude.toString()
        }
    }
}
