package com.test.bletestlakhai.di

import android.system.Os.bind
import com.test.bletestlakhai.data.datasource.AndroidBluetoothDataSource
import com.test.bletestlakhai.data.repository.BluetoothRepositoryImpl
import com.test.bletestlakhai.domain.repository.BluetoothRepository
import com.test.bletestlakhai.domain.usecase.CalculateDistanceUseCase
import com.test.bletestlakhai.domain.usecase.FilterRssiUseCase
import com.test.bletestlakhai.domain.usecase.KalmanFilter
import com.test.bletestlakhai.presentation.viewmodel.BluetoothViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    factory { CalculateDistanceUseCase() }
    factory { KalmanFilter() }
    factory { FilterRssiUseCase() }
    factory { AndroidBluetoothDataSource(get()) }
    single<BluetoothRepository> { BluetoothRepositoryImpl(get(), get(), get()) }
    viewModel { BluetoothViewModel(get(),get()) }
}