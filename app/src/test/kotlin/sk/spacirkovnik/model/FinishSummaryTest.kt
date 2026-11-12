package sk.spacirkovnik.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinishSummaryTest {

    private val screens = listOf(
        GameScreen(type = ScreenType.CONTINUE, imageUrl = ""),
        GameScreen(type = ScreenType.NAVIGATION, imageUrl = "https://x/uvod.jpg"),
        GameScreen(type = ScreenType.QUESTION),
        GameScreen(type = ScreenType.NAVIGATION),
        GameScreen(type = ScreenType.QUESTION),
        GameScreen(type = ScreenType.QUESTION),
    )
    private val minute = 60_000L

    @Test
    fun countsStopsAndQuestions() {
        val s = finishSummary("Rybár", screens, startedAtMs = 0, nowMs = 85 * minute)
        assertEquals(2, s.stops)
        assertEquals(3, s.questions)
        assertEquals(85, s.durationMinutes)
        assertEquals("https://x/uvod.jpg", s.imageUrl)
    }

    @Test
    fun unknownOrUnrealisticDurationIsHidden() {
        assertNull(finishSummary("T", screens, startedAtMs = null, nowMs = 0).durationMinutes)
        assertNull(finishSummary("T", screens, startedAtMs = 0, nowMs = 30_000).durationMinutes)
        assertNull(finishSummary("T", screens, startedAtMs = 0, nowMs = 26 * 60 * minute).durationMinutes)
    }

    @Test
    fun slovakPluralForms() {
        val f = { n: Int -> slovakPlural(n, "zastávka", "zastávky", "zastávok") }
        assertEquals("zastávok", f(0))
        assertEquals("zastávka", f(1))
        assertEquals("zastávky", f(2))
        assertEquals("zastávky", f(4))
        assertEquals("zastávok", f(5))
        assertEquals("zastávok", f(11))
    }

    @Test
    fun durationText() {
        assertEquals("45 min", formatDuration(45))
        assertEquals("1 h", formatDuration(60))
        assertEquals("1 h 25 min", formatDuration(85))
    }
}
