package com.celestial.gps


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_test.*

/**
 * A simple [Fragment] subclass.
 *
 */
class TestFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calculate_button.setOnClickListener {
            val location: Model.Location

            val ra1 = Math.toRadians(ra1_text.text.toString().toDouble())
            val dec1 = Math.toRadians(dec1_text.text.toString().toDouble())
            val azimuth1 = Math.toRadians(azimuth1_text.text.toString().toDouble())
            val altitude1 = Math.toRadians(altitude1_text.text.toString().toDouble())

            val ra2 = Math.toRadians(ra2_text.text.toString().toDouble())
            val dec2 = Math.toRadians(dec2_text.text.toString().toDouble())
            val azimuth2 = Math.toRadians(azimuth2_text.text.toString().toDouble())
            val altitude2 = Math.toRadians(altitude2_text.text.toString().toDouble())

            val timeInMillis = timestamp_text.text.toString().toLong()

            location = if (mode_switch.isChecked) {
                getLocation(
                    ra1,
                    dec1,
                    azimuth1,
                    altitude1,
                    ra2,
                    dec2,
                    azimuth2,
                    altitude2,
                    timeInMillis
                )!!
            } else {
                getLocationNew(
                    ra1 = ra1,
                    dec1 = dec1,
                    azimuth2 = azimuth1,
                    altitude2 = altitude1,
                    timeInMillis = timeInMillis
                )!!
            }

            latitude.text = location.latitude.toString()
            longitude.text = location.longitude.toString()
        }
    }
}
