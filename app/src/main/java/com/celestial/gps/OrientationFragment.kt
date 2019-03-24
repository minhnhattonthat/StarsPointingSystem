package com.celestial.gps

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.fragment_orientation.*

/**
 * A simple [Fragment] subclass.
 */
class OrientationFragment : Fragment(), SensorEventListener {

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3) //0: azimuth, 1: pitch, 2: roll

    private var isStop = false

    private lateinit var sensorManager: SensorManager

//    private val handler = Handler {
//        azimuth.setText(orientationAngles[0].toString())
//        pitch.setText(orientationAngles[1].toString())
//        roll.setText(orientationAngles[2].toString())
//        return@Handler true
//    }
//
//    private val runnable = Runnable {
//        while (!isStop) {
//            updateOrientationAngles()
//            Thread.sleep(250)
//        }
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_orientation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        orientation_button.setOnClickListener {
            if (AstrometryManager.currentPhoto == 0) {
                AstrometryManager.orientationAngles1[0] = azimuth.text.toString().toFloat()
                AstrometryManager.orientationAngles1[1] = pitch.text.toString().toFloat()
                AstrometryManager.orientationAngles1[2] = roll.text.toString().toFloat()
            } else {
                AstrometryManager.orientationAngles2[0] = azimuth.text.toString().toFloat()
                AstrometryManager.orientationAngles2[1] = pitch.text.toString().toFloat()
                AstrometryManager.orientationAngles2[2] = roll.text.toString().toFloat()
            }

            view.findNavController().navigate(R.id.summaryFragment)
        }

        auto_switch.setOnCheckedChangeListener { _, isChecked ->
            azimuth.isEnabled = !isChecked
            pitch.isEnabled = !isChecked
            roll.isEnabled = !isChecked
        }

        sensorManager = context!!.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        isStop = false

//        handler.post(runnable)
    }

    override fun onResume() {
        super.onResume()

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }

    }

    override fun onPause() {
        super.onPause()

        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this)
    }

    override fun onStop() {
        super.onStop()
        isStop = true
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    private fun updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        // "rotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // "orientationAngles" now has up-to-date information.

        azimuth.setText(orientationAngles[0].toString())
        pitch.setText(orientationAngles[1].toString())
        roll.setText(orientationAngles[2].toString())
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }
        if (auto_switch.isChecked) {
            updateOrientationAngles()
        }
    }
}
