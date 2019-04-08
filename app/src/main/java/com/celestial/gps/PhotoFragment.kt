package com.celestial.gps


import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.celestial.gps.CameraManager.ImageSaver
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_photo.*
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

/**
 * A simple [Fragment] subclass.
 */
class PhotoFragment : BaseFragment() {

    private var mCurrentPhotoPath: String = ""

    private var choiceDialog: AlertDialog? = null

    private var photo: File? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val builder = AlertDialog.Builder(context)
        builder.setSingleChoiceItems(R.array.photo_array, -1) { dialog, which ->
            when (which) {
                0 -> selectPhoto()
                1 -> takePhoto()
            }
            dialog.dismiss()
        }
        choiceDialog = builder.create()

        browse_button.setOnClickListener {
            choiceDialog?.show()
        }

        upload_button.setOnClickListener {
            if (checkUpload()) {
                upload()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                photo = ImageSaver.lastTakenImage
            } else if (requestCode == REQUEST_LIBRARY && data?.data != null) {
                photo = FileUtils.getFile(context, data.data)
            }
            Glide.with(this).load(photo).into(photo_preview)
        }
    }

    private fun checkUpload(): Boolean {
        if (photo == null) {
            Toast.makeText(context!!, "Need to select a photo.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun upload() {
        val uploadRequest =
            AstrometryModel.UploadRequest("y", "d", AstrometryManager.sessionKey, "d")

        disposable = astrometryService.upload(
            getStringAsPart(Gson().toJson(uploadRequest)),
            getFileAsPart(photo)
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result -> uploadSuccess(result.subId) },
                { error ->
                    Log.d("Error", "timeout?", error)
                    showError(error.message)
                }
            )
    }

    private fun uploadSuccess(subId: Int) {
        if (AstrometryManager.currentPhoto == FIRST_PHOTO || AstrometryManager.solvingMode == ONE_PHOTO) {
            AstrometryManager.subId = subId
            AstrometryManager.jobs = emptyList()
            AstrometryManager.jobResult1 = null
            AstrometryManager.photo = photo
        } else {
            AstrometryManager.subId2 = subId
            AstrometryManager.jobs2 = emptyList()
            AstrometryManager.jobResult2 = null
            AstrometryManager.photo2 = photo
        }

        Toast.makeText(context, "Upload successfully", Toast.LENGTH_SHORT).show()
        disposable?.dispose()
        upload_button.findNavController().navigate(R.id.orientationFragment)
    }

    private fun selectPhoto() {
        if (ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity!!,
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
                context!!,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity!!, arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
            return
        }
//        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
//            // Ensure that there's a camera activity to handle the intent
//            takePictureIntent.resolveActivity(activity!!.packageManager)?.also {
//                val photoFile: File? = try {
//                    createImageFile()
//                } catch (ex: IOException) {
//                    // Error occurred while creating the File
//                    null
//                }
//                // Continue only if the File was successfully created
//                photoFile?.also {
//                    val photoURI = FileProvider.getUriForFile(
//                        context!!,
//                        "com.celestial.gps",
//                        it
//                    )
//                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
//                    startActivityForResult(takePictureIntent, REQUEST_CAMERA)
//                }
//            }
//        }
//        Intent("android.intent.action.ACTION_IMAGE_CAPTURE").also { takePictureIntent ->
//            val photoFile: File? = try {
//                createImageFile()
//            } catch (ex: IOException) {
//                // Error occurred while creating the File
//                null
//            }
//            // Continue only if the File was successfully created
//            photoFile?.also {
//                val photoURI = FileProvider.getUriForFile(
//                    context!!,
//                    "com.celestial.gps",
//                    it
//                )
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
//                startActivityForResult(takePictureIntent, REQUEST_CAMERA)
//            }
//        }
        Intent(context, FullscreenActivity::class.java).also { takePictureIntent ->
            startActivityForResult(takePictureIntent, REQUEST_CAMERA)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat.getDateTimeInstance().format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = activity!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    private fun getStringAsPart(req: String): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), req)
    }

    private fun getFileAsPart(file: File?): MultipartBody.Part {
        val inputStream = FileInputStream(file)
        val buf = ByteArray(inputStream.available())
        while (inputStream.read(buf) != -1) {
        }
        val body = RequestBody.create(MediaType.parse("application/octet-stream"), buf)

        return MultipartBody.Part.createFormData("file", file?.name, body)
    }
}
