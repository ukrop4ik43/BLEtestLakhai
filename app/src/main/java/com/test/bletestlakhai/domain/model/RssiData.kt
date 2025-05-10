package com.test.bletestlakhai.domain.model

data class RssiData(
    val rawRssi: Int,
    val filteredRssi: Double,
    val distance: Double
)