package com.celestial.gps

fun getLocation(
    ra1: Double, dec1: Double, azimuth1: Double, altitude1: Double,
    ra2: Double, dec2: Double, azimuth2: Double, altitude2: Double,
    timeInMillis: Long
): Model.Location? {

    val lst = calculateLst(timeInMillis)
    val hourAngle1 = calculateHourAngle(altitude1, azimuth1, dec1)
    val hourAngle2 = calculateHourAngle(altitude2, azimuth2, dec2)

    val longitude = calculateLongitude(lst, ra1, hourAngle1)
    val latitude = calculateLatitude(altitude1, dec1, hourAngle1, altitude2, dec2, hourAngle2)

    return Model.Location(longitude, latitude)
}

fun calculateHourAngle(altitude: Double, azimuth: Double, dec: Double): Double {
    var hourAngle = Math.asin((Math.cos(altitude) * Math.sin(azimuth)) / Math.cos(dec))
    if (hourAngle > 0) hourAngle *= (-1)
    return hourAngle
}

fun calculateGst(timeInMillis: Long): Double {
    val J2000 = 946728000000

//    val timeInMillis = 1553520720000
    val tu = (timeInMillis - J2000) / (36525.0 * 86400000)
    var gmst =
        24110.54841 + (8640184.812866 + (0.093104 - 0.0000062 * tu) * tu) * tu + (timeInMillis % 86400000) * 0.001
    gmst = (gmst / 13750.987) % (2 * Math.PI)
    if (gmst < 0) gmst += 2 * Math.PI

    return Math.toDegrees(gmst)
//    return 6.6208844 + 0.0657098244 * 84 + 1.00273791 * 8
}

fun calculateLst(timeInMillis: Long): Double {
    val gst = calculateGst(timeInMillis)
    return gst + 105
}

fun calculateLongitude(lst: Double, ra: Double, hourAngle: Double): Double {
    return lst - Math.toDegrees(ra) - Math.toDegrees(hourAngle)
}

@Suppress("UnnecessaryVariable")
fun calculateLatitude(
    altitude1: Double, dec1: Double, hourAngle1: Double,
    altitude2: Double, dec2: Double, hourAngle2: Double
): Double {

    val a1 = Math.cos(Math.toRadians(90.0) - dec1)
    val b1 = Math.sin(Math.toRadians(90.0) - dec1) * Math.cos(Math.toRadians(180.0) - hourAngle1)
    val c1 = altitude1

    val a2 = Math.cos(Math.toRadians(90.0) - dec2)
    val b2 = Math.sin(Math.toRadians(90.0) - dec2) * Math.cos(Math.toRadians(180.0) - hourAngle2)
    val c2 = altitude2

    val x = ((b2 * c1) - (c2 * b1)) / ((b2 * a1) - (a2 * b1))

    return Math.toDegrees(Math.toRadians(90.0) - Math.acos(x))

    // c1 = a1x + b1y => y = (c1 - a1.x)/ b1
    // c2 = a2x + b2y => c2 = a2x + b2.(c1 - a1.x)/b1 => c2 = a2.x + b2.c1/b1 - b2.a1.x/b1
    // => c2 - b2.c1/b1 = (a2 - b2.a1/b1)x
    // => (c2.b1 - b2.c1)/b1 = (a2.b1 - b2.a1).x/b1
    // => c2.b1 - b2.c1 = (a2.b1 - b2.a1).x
    // => (c2.b1 - b2.c1) / (a2.b1 - b2.a1) = x
}

fun getLocationNew(
    ra1: Double,
    dec1: Double,
    azimuth2: Double,
    altitude2: Double,
    timeInMillis: Long
): Model.Location? {
    var ra2 = ra1 + Math.toRadians(240.960164)
    ra2 = if (ra2 < 0) ra2 - 2 * Math.PI else ra2

    val dec2 = dec1 + Math.toRadians(21.40824178)

    var azimuth1 = azimuth2 - Math.toRadians(240.960164)
    azimuth1 = if (azimuth1 < 0) azimuth1 + 2 * Math.PI else azimuth1

    val altitude1 = altitude2 - Math.toRadians(21.40824178)

    return getLocation(ra1, dec1, azimuth1, altitude1, ra2, dec2, azimuth2, altitude2, timeInMillis)
}