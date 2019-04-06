package com.celestial.gps

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.celestial.gps.databinding.ActivityMainBinding
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private const val REQUEST_CAMERA = 1
private const val REQUEST_LIBRARY = 2
private const val REQUEST_CAMERA_PERMISSION = 5
private const val REQUEST_STORAGE_PERMISSION = 6

private const val ASTROMETRY_API_KEY = "muczweheoermnzwt"

private const val FIRST_PHOTO_1 = 0
private const val SECOND_PHOTO_2 = 1

class MainActivity : AppCompatActivity(), SensorEventListener {

    private var mCurrentPhotoPath: String = ""

    private var sessionKey: String = ""

    private var subId: Int = 0
    private var subId2: Int = 0

    private var jobs: List<Int>? = emptyList()
    private var jobs2: List<Int>? = emptyList()

    private var currentPhoto: Int = FIRST_PHOTO_1

    private var photo: File? = null
    private var photo2: File? = null


    private var jobResult1: AstrometryModel.JobResult? = null
    private var jobResult2: AstrometryModel.JobResult? = null

    private var orientationAngles1 = FloatArray(3)
    private var orientationAngles2 = FloatArray(3)

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3) //0: azimuth, 1: pitch, 2: roll

    private var choiceDialog: AlertDialog? = null

    private val astrometryService by lazy {
        AstrometryService.create()
    }

    var disposable: Disposable? = null

    var binding: ActivityMainBinding? = null

    private lateinit var sensorManager: SensorManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding?.subId = subId
        binding?.subId2 = subId2

        binding?.jobs = jobs
        binding?.jobs2 = jobs2

        val builder = AlertDialog.Builder(this)
        builder.setSingleChoiceItems(R.array.photo_array, -1) { dialog, which ->
            when (which) {
                0 -> selectPhoto()
                1 -> takePhoto()
            }
            dialog.dismiss()
        }
        choiceDialog = builder.create()

        login_button.setOnClickListener {
            login()
        }

        browse_button.setOnClickListener {
            choiceDialog?.show()
        }

        upload_button.setOnClickListener {
            if (checkUpload()) {
                upload()
            }
        }

        check_submission_button.setOnClickListener {
            if (subId == 0) {
                Toast.makeText(this, "No submission", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            getSubmissionStatus()
        }

        check_jobs_button.setOnClickListener {
            if (jobs == null || jobs!!.isEmpty()) {
                Toast.makeText(this, "No jobs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            getJobResults()
        }

        radioButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentPhoto = FIRST_PHOTO_1
                radioButton2.isChecked = false
            }
        }

        radioButton2.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentPhoto = SECOND_PHOTO_2
                radioButton.isChecked = false
            }
        }

        calculate_location_button.setOnClickListener {
            calculateLocation()
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
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

    @SuppressLint("InflateParams")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                updateOrientationAngles()
                if (currentPhoto == FIRST_PHOTO_1) {
                    photo = File(mCurrentPhotoPath)
                    Glide.with(this).load(photo).into(preview)
                    orientationAngles1 = orientationAngles.clone()
                    orientation_values.text = orientationAngles1.toStringDisplay()
                } else {
                    photo2 = File(mCurrentPhotoPath)
                    Glide.with(this).load(photo2).into(preview2)
                    orientationAngles2 = orientationAngles.clone()
                    orientation_values2.text = orientationAngles2.toStringDisplay()
                }
            } else if (requestCode == REQUEST_LIBRARY && data?.data != null) {
                if (currentPhoto == FIRST_PHOTO_1) {
                    photo = FileUtils.getFile(this, data.data)
                    Glide.with(this).load(photo).into(preview)
                } else {
                    photo2 = FileUtils.getFile(this, data.data)
                    Glide.with(this).load(photo2).into(preview2)
                }

                val dialogView =
                    LayoutInflater.from(this).inflate(R.layout.dialog_orientation, null)
                val azimuthText = dialogView.findViewById<TextView>(R.id.azimuth)
                val pitchText = dialogView.findViewById<TextView>(R.id.pitch)
                val rollText = dialogView.findViewById<TextView>(R.id.roll)
                AlertDialog.Builder(this).setView(R.layout.dialog_orientation)
                    .setPositiveButton("OK") { _, _ ->
                        if (azimuthText.text.isEmpty() || pitchText.text.isEmpty() || rollText.text.isEmpty()) {
                            Toast.makeText(this, "Please input all data", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        if (currentPhoto == FIRST_PHOTO_1) {
                            orientationAngles1[0] = azimuthText.text.toString().toFloat()
                            orientationAngles1[1] = pitchText.text.toString().toFloat()
                            orientationAngles1[2] = rollText.text.toString().toFloat()
                            orientation_values.text = orientationAngles1.toStringDisplay()
                        } else {
                            orientationAngles2[0] = azimuthText.text.toString().toFloat()
                            orientationAngles2[1] = pitchText.text.toString().toFloat()
                            orientationAngles2[2] = rollText.text.toString().toFloat()
                            orientation_values2.text = orientationAngles2.toStringDisplay()
                        }
                    }
                    .create().show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
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
    }

    private fun login() {
        val apiKey = AstrometryModel.ApiKey(ASTROMETRY_API_KEY)

        disposable =
            astrometryService.login(Gson().toJson(apiKey))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { result -> loginSuccess(result) },
                    { error -> showError(error.message) }
                )
    }

    private fun loginSuccess(response: AstrometryModel.LoginResponse) {
        if (response.status == "success") {
            this.sessionKey = response.session
            Toast.makeText(this, "Login successfully", Toast.LENGTH_SHORT).show()
            login_button.visibility = View.GONE
            logged_in_text.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, response.errorMessage, Toast.LENGTH_SHORT).show()
        }
        disposable?.dispose()
    }

    private fun checkUpload(): Boolean {
        if (TextUtils.isEmpty(sessionKey)) {
            Toast.makeText(this, "Need to login first.", Toast.LENGTH_SHORT).show()
            return false
        }
        if ((currentPhoto == FIRST_PHOTO_1 && photo == null)
            || currentPhoto == SECOND_PHOTO_2 && photo2 == null
        ) {
            Toast.makeText(this, "Need to select a photo.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun upload() {
        val uploadRequest = AstrometryModel.UploadRequest("y", "d", sessionKey, "d")
        val uploadPhoto = if (currentPhoto == FIRST_PHOTO_1) photo else photo2

        disposable = astrometryService.upload(
            getStringAsPart(Gson().toJson(uploadRequest)),
            getFileAsPart(uploadPhoto)
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result -> uploadSuccess(result.subId) },
                { error -> showError(error.message) }
            )
    }

    private fun uploadSuccess(subId: Int) {
        if (currentPhoto == FIRST_PHOTO_1) {
            this.subId = subId
            this.jobs = emptyList()
            this.jobResult1 = null
            stars_result.text = ""
            submission_id.text = subId.toString()
        } else {
            this.subId2 = subId
            this.jobs2 = emptyList()
            this.jobResult2 = null
            stars_result2.text = ""
            submission_id2.text = subId.toString()
        }

        Toast.makeText(this, "Upload successfully", Toast.LENGTH_SHORT).show()
        disposable?.dispose()
    }

    private fun getFileAsPart(file: File?): MultipartBody.Part {
        val inputStream = FileInputStream(file)
        val buf = ByteArray(inputStream.available())
        while (inputStream.read(buf) != -1) {
        }
        val body = RequestBody.create(MediaType.parse("application/octet-stream"), buf)

        return MultipartBody.Part.createFormData("file", file?.name, body)
    }

    private fun getStringAsPart(req: String): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), req)
    }

    private fun showError(error: String?) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        disposable?.dispose()
    }

    private fun selectPhoto() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
        } else {
            val intentLibrary = Intent(Intent.ACTION_GET_CONTENT)
            intentLibrary.addCategory(Intent.CATEGORY_OPENABLE)
            intentLibrary.type = "image/*"
            intentLibrary.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            startActivityForResult(
                Intent.createChooser(intentLibrary, "Select Picture"),
                REQUEST_LIBRARY
            )
        }
    }

    private fun takePhoto() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
            return
        }
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI = FileProvider.getUriForFile(
                        this,
                        "com.celestial.gps",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_CAMERA)
                }
            }

        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat.getDateTimeInstance().format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    private fun getSubmissionStatus() {
        val currentSubId = if (currentPhoto == FIRST_PHOTO_1) subId else subId2
        disposable = astrometryService.getSubmissionStatus(currentSubId.toString())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result -> showJobs(result) },
                { error -> showError(error.message) }
            )
    }

    private fun showJobs(submissionStatus: AstrometryModel.SubmissionStatus?) {
        if (currentPhoto == FIRST_PHOTO_1) {
            jobs = submissionStatus?.jobs
            jobResult1 = null

            if (!jobs.isNullOrEmpty()) job_ids.text = submissionStatus?.jobs!![0].toString()
        } else {
            jobs2 = submissionStatus?.jobs
            jobResult2 = null

            if (!jobs.isNullOrEmpty()) job_ids2.text = submissionStatus?.jobs!![0].toString()
        }
    }

    private fun getJobResults() {
        if (!checkJobs()) return

        val currentJobs = if (currentPhoto == FIRST_PHOTO_1) jobs else jobs2
//        val jobIds = currentJobs?.map { it.toString() }?.toTypedArray()

//        AlertDialog.Builder(this)
//            .setSingleChoiceItems(jobIds, 0) { dialog, which ->
//                astrometryService.getJobResults(jobIds!![which])
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(
//                        { result -> showResult(result) },
//                        { error -> showError(error.message) }
//                    )
//                dialog.dismiss()
//            }
//            .create().show()

        astrometryService.getJobResults(currentJobs!![0].toString())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result -> showResult(result) },
                { error -> showError(error.message) }
            )
    }

    private fun checkJobs(): Boolean {
        val currentJobs = if (currentPhoto == FIRST_PHOTO_1) jobs else jobs2
        if (currentJobs == null) {
            Toast.makeText(this, "No jobs available", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun showResult(result: AstrometryModel.JobResult?) {
        if (currentPhoto == FIRST_PHOTO_1) {
            if (result?.status == "success") jobResult1 = result
            stars_result.text = result?.status
        } else {
            if (result?.status == "success") jobResult2 = result
            stars_result2.text = result?.status
        }
    }

    private fun calculateLocation() {
        if (checkEnoughData()) {
            val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val location = getLocation(
                ra1 = jobResult1?.calibration?.ra!!,
                dec1 = jobResult1?.calibration?.dec!!,
                azimuth1 = orientationAngles1[0].toDouble(),
                altitude1 = orientationAngles1[1].toDouble(),
                ra2 = jobResult2?.calibration?.ra!!,
                dec2 = jobResult2?.calibration?.dec!!,
                azimuth2 = orientationAngles2[0].toDouble(),
                altitude2 = orientationAngles2[1].toDouble()
            )
            longitude.text = location?.longitude.toString()
            latitude.text = location?.latitude.toString()
        }
    }

    private fun checkEnoughData(): Boolean {
        if (jobResult1 == null || jobResult2 == null) {
            Toast.makeText(this, "Don't have star data", Toast.LENGTH_SHORT).show()
            return false
        }

        if (orientationAngles1.any { it.equals(0) } || orientationAngles1.any { it.equals(0) }) {
            Toast.makeText(this, "Don't have orientation data", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
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
    }
}


fun FloatArray.toStringDisplay(): String {
    if (this.isEmpty()) return "[]"
    var text = "["
    this.forEach {
        val number = Math.round(it * 100f) / 100f
        text = "$text$number, "
    }
    text = text.dropLast(2)
    text = "$text]"
    return text
}