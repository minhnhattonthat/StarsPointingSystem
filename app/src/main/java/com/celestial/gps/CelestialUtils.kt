package com.celestial.gps

fun getLocation(ra1: Double, dec1: Double, azimuth1: Double, altitude1: Double,
                ra2: Double, dec2: Double, azimuth2: Double, altitude2: Double,
                day: Int): Model.Location? {

    val gst = calculateGst(day)
    val hourAngle1 = calculateHourAngle(altitude1, azimuth1, dec1)
    val hourAngle2 = calculateHourAngle(altitude2, azimuth2, dec2)

    val longitude = calculateLongitude(gst, ra1, hourAngle1)
    val latitude = calculateLatitude(altitude1, dec1, hourAngle1, altitude2, dec2, hourAngle2)

    return Model.Location(longitude, latitude)
}

fun calculateHourAngle(altitude: Double, azimuth: Double, dec: Double): Double {
    return Math.asin(Math.cos(altitude) * Math.sin(azimuth)/Math.cos(dec))
}

fun calculateGst(day: Int): Double {
    return 6.6208844 + 0.0657098244 * day + 1.00273791 * 8
}

fun calculateLongitude(gst: Double, ra: Double, hourAngle: Double): Double {
    return gst - ra - hourAngle
}

@Suppress("UnnecessaryVariable")
fun calculateLatitude(altitude1: Double, dec1: Double, hourAngle1: Double,
                      altitude2: Double, dec2: Double, hourAngle2: Double): Double {

    val a1 = Math.cos(Math.toRadians(90.0) - dec1)
    val b1 = Math.sin(Math.toRadians(90.0) - dec1) * Math.cos(Math.toRadians(180.0) - hourAngle1)
    val c1 = altitude1

    val a2 = Math.cos(Math.toRadians(90.0) - dec2)
    val b2 = Math.sin(Math.toRadians(90.0) - dec2) * Math.cos(Math.toRadians(180.0) - hourAngle2)
    val c2 = altitude2

    val x = ((c2 * b1) - (b2 * c1)) / ((a2 * b1) - (b2 * a1))

    return Math.toRadians(90.0) - Math.acos(x)

    // c1 = a1x + b1y => y = (c1 - a1.x)/ b1
    // c2 = a2x + b2y => c2 = a2x + b2.(c1 - a1.x)/b1 => c2 = a2.x + b2.c1/b1 - b2.a1.x/b1
    // => c2 - b2.c1/b1 = (a2 - b2.a1/b1)x
    // => (c2.b1 - b2.c1)/b1 = (a2.b1 - b2.a1).x/b1
    // => c2.b1 - b2.c1 = (a2.b1 - b2.a1).x
    // => (c2.b1 - b2.c1) / (a2.b1 - b2.a1) = x
}