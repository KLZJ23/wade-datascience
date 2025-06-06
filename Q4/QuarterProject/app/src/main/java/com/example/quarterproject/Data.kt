package com.example.quarterproject

import kotlinx.serialization.Serializable

@Serializable
data class DetectedObject(
    val label: String,
    val boundingBox: BoundingBox
)

@Serializable
data class BoundingBox(
    val x_min: Float,
    val y_min: Float,
    val x_max: Float,
    val y_max: Float
)