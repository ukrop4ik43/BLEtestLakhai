package com.test.bletestlakhai.domain.usecase

class KalmanFilter {
    private var estimatedRSSI: Double? = null
    private var estimationError: Double = 1.0
    private val processNoise: Double = 0.008
    private val measurementNoise: Double = 0.1

    fun update(measuredRSSI: Double): Double {
        if (estimatedRSSI == null) {
            estimatedRSSI = measuredRSSI
        } else {
            estimationError += processNoise

            val kalmanGain = estimationError / (estimationError + measurementNoise)

            estimatedRSSI = estimatedRSSI!! + kalmanGain * (measuredRSSI - estimatedRSSI!!)

            estimationError *= (1 - kalmanGain)
        }
        return estimatedRSSI!!
    }

    fun reset() {
        estimatedRSSI = null
        estimationError = 1.0
    }
}

class FilterRssiUseCase(private val kalmanFilter: KalmanFilter = KalmanFilter()) {
    operator fun invoke(measuredRSSI: Double): Double {
        return kalmanFilter.update(measuredRSSI)
    }
    fun reset() {
        kalmanFilter.reset()
    }
}