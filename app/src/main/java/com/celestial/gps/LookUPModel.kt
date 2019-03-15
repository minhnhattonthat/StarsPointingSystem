package com.celestial.gps

object LookUPModel {
    data class Star(
            val category: Category,
            val coordsys: String,
            val dec: Dec,
            val equinox: String,
            val jd: Double,
            val image: Image,
            val ra: Ra,
            val target: Target
    )

    data class Target(
            val alt: String,
            val name: String
    )

    data class Image(
            val href: String,
            val src: String
    )

    data class Category(
            val avmcode: String,
            val avmdesc: String
    )

    data class Dec(
            val d: String,
            val decimal: Double,
            val m: String,
            val s: String
    )

    data class Ra(
            val decimal: Double,
            val h: String,
            val m: String,
            val s: String
    )

    data class Galactic(
            val lat: Double,
            val lon: Double
    )
}
