package com.celestial.gps

import java.io.File

private const val ASTROMETRY_API_KEY = "muczweheoermnzwt"

const val FIRST_PHOTO = 0
const val SECOND_PHOTO = 1

object AstrometryManager {

    var sessionKey: String = ""

    var subId: Int = 0
    var subId2: Int = 0

    var jobs: List<Int>? = null
    var jobs2: List<Int>? = null

    var currentPhoto: Int = FIRST_PHOTO

    var photo: File? = null
    var photo2: File? = null

    var jobResult1: AstrometryModel.JobResult? = null
    var jobResult2: AstrometryModel.JobResult? = null

    var orientationAngles1 = FloatArray(3)
    var orientationAngles2 = FloatArray(3)

}