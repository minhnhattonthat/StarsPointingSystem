package com.celestial.gps

import com.google.gson.annotations.SerializedName

object AstrometryModel {
    data class ApiKey(
        @SerializedName("apikey")
        val apiKey: String
    )

    data class LoginResponse(
        val status: String,
        val message: String,
        val session: String,
        @SerializedName("errormessage")
        val errorMessage: String
    )

    data class UploadRequest(
        @SerializedName("publicly_visible")
        val publiclyVisible: String,
        @SerializedName("allow_modifications")
        val allowModifications: String,
        val session: String,
        @SerializedName("allow_commercial_use")
        val allowCommercialUse: String
    )

    data class UploadResponse(
        val status: String,
        @SerializedName("subid")
        val subId: Int,
        val hash: String
    )

    data class SubmissionStatus(
        val job_calibrations: List<List<Int>>,
        val jobs: List<Int>,
        val processing_finished: String,
        val processing_started: String,
        val user: Int,
        val user_images: List<Int>,
        @SerializedName("errormessage")
        val errorMessage: String
    )

    data class JobResult(
        val calibration: Calibration,
        val machine_tags: List<String>,
        val objects_in_field: List<String>,
        val original_filename: String,
        val status: String,
        val tags: List<String>,
        @SerializedName("errormessage")
        val errorMessage: String
    )

    data class Calibration(
        val dec: Double,
        val orientation: Double,
        val parity: Double,
        val pixscale: Double,
        val ra: Double,
        val radius: Double,
        @SerializedName("errormessage")
        val errorMessage: String
    )
}