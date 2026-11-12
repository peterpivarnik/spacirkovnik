package sk.spacirkovnik.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivationTest {

    @Test
    fun legacyTrueUnlocks() = assertTrue(isActivationActive(true))

    @Test
    fun legacyFalseDoesNotUnlock() = assertFalse(isActivationActive(false))

    @Test
    fun activeObjectUnlocks() = assertTrue(
        isActivationActive(mapOf("active" to true, "type" to "prize", "note" to "Výhra – Dni Petržalky 2026"))
    )

    @Test
    fun inactiveObjectDoesNotUnlock() =
        assertFalse(isActivationActive(mapOf("active" to false, "type" to "purchase")))

    @Test
    fun objectWithoutActiveDoesNotUnlock() =
        assertFalse(isActivationActive(mapOf("type" to "prize")))

    @Test
    fun otherValuesDoNotUnlock() {
        assertFalse(isActivationActive(null))
        assertFalse(isActivationActive("true"))
        assertFalse(isActivationActive(1L))
    }
}
