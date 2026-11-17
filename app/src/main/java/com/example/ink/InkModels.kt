package com.example.ink

data class InkPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,
)

data class InkStroke(
    val points: List<InkPoint>,
)
