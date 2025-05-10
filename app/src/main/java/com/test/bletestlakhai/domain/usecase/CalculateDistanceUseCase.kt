package com.test.bletestlakhai.domain.usecase

import kotlin.math.pow

class CalculateDistanceUseCase {
    /**
     * Розраховує відстань на основі RSSI.
     * @param rssi Поточне значення RSSI (може бути відфільтроване).
     * @param rssiAtOneMeter RSSI на відстані одного метра (потрібно калібрувати для вашого пристрою).
     * @param pathLossExponent Коефіцієнт втрати сигналу (залежить від середовища).
     * @return Розрахункова відстань в метрах.
     */
    operator fun invoke(rssi: Double, rssiAtOneMeter: Int = -59, pathLossExponent: Double = 2.5): Double {
        if (rssi == 0.0) {
            return -1.0 // Неможливо розрахувати, якщо RSSI 0
        }
        return 10.0.pow((rssiAtOneMeter - rssi) / (10 * pathLossExponent))
    }
}