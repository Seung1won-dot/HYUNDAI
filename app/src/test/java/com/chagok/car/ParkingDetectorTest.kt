package com.chagok.car

import com.chagok.car.model.Gear
import com.chagok.car.parking.MemoText
import com.chagok.car.parking.ParkingDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkingDetectorTest {
    @Test fun `주행한 뒤 P단에 넣으면 묻는다`() {
        val d = ParkingDetector()
        assertFalse(d.onGear(Gear.DRIVE))
        assertTrue(d.onGear(Gear.PARK))
    }

    @Test fun `시동만 걸고 P단에 있으면 묻지 않는다`() {
        val d = ParkingDetector()
        assertFalse(d.onGear(Gear.PARK))
    }

    @Test fun `같은 주차에 두 번 묻지 않는다`() {
        val d = ParkingDetector()
        d.onGear(Gear.DRIVE)
        assertTrue(d.onGear(Gear.PARK))
        assertFalse(d.onGear(Gear.NEUTRAL))
        assertFalse(d.onGear(Gear.PARK))
    }

    @Test fun `후진 주차도 주행으로 본다`() {
        val d = ParkingDetector()
        d.onGear(Gear.REVERSE)
        assertTrue(d.onGear(Gear.PARK))
    }

    @Test fun `거절과 수락을 가려낸다`() {
        assertTrue(MemoText.isDecline("아니 괜찮아"))
        assertTrue(MemoText.isDecline("  "))
        assertFalse(MemoText.isDecline("B2 기둥 14"))
        assertTrue(MemoText.isAccept("응"))
        assertFalse(MemoText.isAccept("응급실 앞"))
        assertEquals("지하 2층 B구역", MemoText.clean(" 지하 2층 B구역. "))
    }
}
