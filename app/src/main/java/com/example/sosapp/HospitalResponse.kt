package com.example.sosapp

data class HospitalResponse(
    val elements: List<Hospital>
)

data class Hospital(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val tags: Tags
)

data class Tags(
    val name: String? = null,
    val amenity: String? = null
)
