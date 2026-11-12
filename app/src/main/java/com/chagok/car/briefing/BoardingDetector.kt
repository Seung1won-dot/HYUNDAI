package com.chagok.car.briefing

/**
 * 다음 사람이 차에 타는 순간을 알아낸다.
 *
 * 규칙: 주차한 뒤 운전석 문이 닫히는 것을 센다.
 * 첫 번째는 주차한 사람이 내리며 닫은 것이고, 두 번째가 다음 사람이 타며 닫은 것이다.
 * 앱이 새로 켜진 경우(차 전원이 다시 들어온 경우)에는 이미 내린 뒤이므로 첫 번째 닫힘을 탑승으로 본다.
 * 쪽지 한 장은 한 번만 읽어 준다.
 */
class BoardingDetector(unreadFromBefore: Boolean = false) {
    private var armed = unreadFromBefore
    private var closesNeeded = if (unreadFromBefore) 1 else 2
    private var doorWasOpen = false

    /** 새 쪽지가 저장됐을 때. */
    fun onParked() {
        armed = true
        closesNeeded = 2
    }

    /** 쪽지를 이미 읽어 줬거나(버튼), 그냥 출발했을 때. */
    fun onHandedOver() { armed = false }

    /** 운전석 문 상태가 바뀔 때마다 호출. 지금 쪽지를 읽어 줘야 하면 true. */
    fun onDriverDoor(open: Boolean): Boolean {
        val closedNow = doorWasOpen && !open
        doorWasOpen = open
        if (!armed || !closedNow) return false
        closesNeeded -= 1
        if (closesNeeded > 0) return false
        armed = false
        return true
    }
}
