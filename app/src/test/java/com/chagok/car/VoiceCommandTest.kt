package com.chagok.car

import com.chagok.car.model.PlacePin
import com.chagok.car.place.VoiceCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceCommandTest {
    private fun pin(name: String) = PlacePin(name, name, "딸", 0, 36.0, 127.0)

    @Test fun `여기 다음에 오자는 이름 없는 장소 저장`() {
        assertEquals(VoiceCommand.SavePlace(null), VoiceCommand.parse("여기 다음에 오자"))
        assertEquals(VoiceCommand.SavePlace(null), VoiceCommand.parse("여기 저장해 줘."))
    }

    @Test fun `이름을 함께 말하면 이름까지 받는다`() {
        assertEquals(VoiceCommand.SavePlace("빵집"), VoiceCommand.parse("여기 빵집으로 저장해 줘"))
        assertEquals(VoiceCommand.SavePlace("할머니 댁"), VoiceCommand.parse("여기를 할머니 댁이라고 기억해"))
    }

    @Test fun `안내해 줘와 가자는 길 안내`() {
        assertEquals(VoiceCommand.Navigate("빵집"), VoiceCommand.parse("그 빵집 안내해 줘"))
        assertEquals(VoiceCommand.Navigate("학교"), VoiceCommand.parse("학교로 안내해줘"))
        assertEquals(VoiceCommand.Navigate("학교"), VoiceCommand.parse("학교 가자"))
    }

    @Test fun `전해 줘는 다음 사람에게 남기는 한마디`() {
        assertEquals(VoiceCommand.LeaveWord("트렁크에 수박 있어"), VoiceCommand.parse("트렁크에 수박 있다고 전해 줘"))
        assertEquals(VoiceCommand.LeaveWord("주유했어"), VoiceCommand.parse("주유했다고 전해줘."))
        assertEquals(VoiceCommand.LeaveWord("조금 늦는다"), VoiceCommand.parse("조금 늦는다고 전해 줘"))
        assertEquals(VoiceCommand.LeaveWord("주유했어"), VoiceCommand.parse("아빠한테 전해 줘, 주유했어"))
        assertEquals(VoiceCommand.LeaveWord("뒷자리에 우산 있어"), VoiceCommand.parse("쪽지 남겨 줘 뒷자리에 우산 있어"))
    }

    @Test fun `전할 말 안에 가자나 저장이 있어도 한마디로 받는다`() {
        assertEquals(VoiceCommand.LeaveWord("내일 마트 가자"), VoiceCommand.parse("내일 마트 가자고 전해 줘"))
    }

    @Test fun `그 밖의 말은 모르는 명령`() {
        assertEquals(VoiceCommand.Unknown, VoiceCommand.parse("오늘 날씨 어때"))
    }

    @Test fun `저장한 장소를 이름으로 찾는다`() {
        val pins = listOf(pin("성심당 빵집"), pin("학교"))
        assertEquals("성심당 빵집", VoiceCommand.findPin(pins, "빵집")?.name)
        assertEquals("학교", VoiceCommand.findPin(pins, "학 교")?.name)
        assertNull(VoiceCommand.findPin(pins, "수영장"))
    }

    @Test fun `받침에 맞춰 으로와 로를 붙인다`() {
        assertEquals("빵집으로", VoiceCommand.toward("빵집"))
        assertEquals("학교로", VoiceCommand.toward("학교"))
        assertEquals("서울로", VoiceCommand.toward("서울"))
    }
}
