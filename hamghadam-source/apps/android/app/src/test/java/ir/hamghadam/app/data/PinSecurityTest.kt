package ir.hamghadam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** App-lock rules: PIN format, PBKDF2 hashing and the failure back-off schedule. */
class PinSecurityTest {

    @Test
    fun pinMustBeFourToEightDigits() {
        assertFalse(isPinValid("123"))
        assertTrue(isPinValid("1234"))
        assertTrue(isPinValid("12345678"))
        assertFalse(isPinValid("123456789"))
    }

    @Test
    fun pinRejectsNonDigitsAndEmptyInput() {
        assertFalse(isPinValid(""))
        assertFalse(isPinValid("12a4"))
        assertFalse(isPinValid("12 34"))
        assertFalse(isPinValid("1234!"))
    }

    @Test
    fun persianAndArabicDigitsAreNormalisedToAscii() {
        assertEquals("1234", normalizePin("۱۲۳۴"))
        assertEquals("5678", normalizePin("٥٦٧٨"))
        assertEquals("12", normalizePin("a1 2"))
        assertTrue(isPinValid("۱۲۳۴"))
        assertEquals(4, normalizePin("۱۲۳۴").length)
    }

    @Test
    fun hashIsDeterministicForTheSameSalt() {
        val salt = PinHasher.newSalt()
        assertArrayEqualsSafe(
            PinHasher.derive("1357", salt, iterations = 1_000),
            PinHasher.derive("1357", salt, iterations = 1_000),
        )
    }

    @Test
    fun differentSaltsProduceDifferentHashes() {
        val first = PinHasher.derive("1357", PinHasher.newSalt(), iterations = 1_000)
        val second = PinHasher.derive("1357", PinHasher.newSalt(), iterations = 1_000)
        assertNotEquals(first.toList(), second.toList())
    }

    @Test
    fun verifyAcceptsTheRightPinAndRejectsEverythingElse() {
        val salt = PinHasher.newSalt()
        val expected = PinHasher.derive("246810", salt, iterations = 1_000)
        assertTrue(PinHasher.verify("246810", salt, expected, iterations = 1_000))
        assertFalse(PinHasher.verify("246811", salt, expected, iterations = 1_000))
        assertFalse(PinHasher.verify("", salt, expected, iterations = 1_000))
    }

    @Test
    fun saltsAreAlwaysSixteenRandomBytes() {
        val a = PinHasher.newSalt()
        val b = PinHasher.newSalt()
        assertEquals(PinHasher.SALT_BYTES, a.size)
        assertNotEquals(a.toList(), b.toList())
    }

    @Test
    fun hashIsAlwaysTheRequestedKeyLength() {
        val hash = PinHasher.derive("9999", PinHasher.newSalt(), iterations = 500)
        assertEquals(PinHasher.KEY_BITS / 8, hash.size)
    }

    @Test
    fun lockoutGrowsThenCapsAtOneMinute() {
        assertEquals(0L, PinHasher.lockoutSeconds(0))
        assertEquals(2L, PinHasher.lockoutSeconds(1))
        assertEquals(4L, PinHasher.lockoutSeconds(2))
        assertEquals(8L, PinHasher.lockoutSeconds(3))
        assertEquals(16L, PinHasher.lockoutSeconds(4))
        assertEquals(32L, PinHasher.lockoutSeconds(5))
        assertEquals(60L, PinHasher.lockoutSeconds(6))
        assertEquals(60L, PinHasher.lockoutSeconds(50))
    }

    private fun assertArrayEqualsSafe(expected: ByteArray, actual: ByteArray) {
        assertEquals(expected.toList(), actual.toList())
    }
}
