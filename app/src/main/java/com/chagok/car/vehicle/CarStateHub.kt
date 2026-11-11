package com.chagok.car.vehicle

import com.chagok.car.model.CarState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 차 상태가 모이는 한 곳.
 * 실제 차량(Pleos Vehicle SDK)과 화면의 테스트 패널이 모두 여기에 값을 쓴다.
 */
class CarStateHub {
    private val _state = MutableStateFlow(CarState())
    val state: StateFlow<CarState> = _state.asStateFlow()

    fun update(change: (CarState) -> CarState) = _state.update(change)
}
