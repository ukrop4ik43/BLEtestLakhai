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
            // Prediction
            estimationError += processNoise

            // Kalman Gain
            val kalmanGain = estimationError / (estimationError + measurementNoise)

            // Update Estimate
            estimatedRSSI = estimatedRSSI!! + kalmanGain * (measuredRSSI - estimatedRSSI!!)

            // Update Estimation Error
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