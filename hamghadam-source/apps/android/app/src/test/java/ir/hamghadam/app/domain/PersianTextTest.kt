package ir.hamghadam.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PersianTextTest {

    @Test
    fun `digits convert both ways`() {
        assertEquals("۱۴۰۵/۰۸/۰۳", PersianText.toPersianDigits("1405/08/03"))
        assertEquals("1405/08/03", PersianText.toLatinDigits("۱۴۰۵/۰۸/۰۳"))
        assertEquals("2026", PersianText.toLatinDigits("٢٠٢٦"))
        assertEquals("abc", PersianText.toPersianDigits("abc"))
    }

    @Test
    fun `arabic letters are normalised`() {
        assertEquals("یک", PersianText.normalizeDisplay("يك"))
        assertEquals("کار", PersianText.normalizeDisplay("كار"))
        assertEquals("مدرسه", PersianText.normalizeDisplay("مدرسة"))
    }

    @Test
    fun `matching normalisation removes zero width non joiners and case`() {
        assertEquals("برنامه ریزی", PersianText.normalizeForMatching("برنامه‌ریزی"))
        assertEquals("todo list", PersianText.normalizeForMatching("  ToDo   List "))
        assertEquals("1405/08/03", PersianText.normalizeForMatching("۱۴۰۵/۰۸/۰۳"))
    }

    @Test
    fun `clock and numbers respect the digit option`() {
        assertEquals("۰۹:۰۵", PersianText.clock(9, 5))
        assertEquals("09:05", PersianText.clock(9, 5, persianDigits = false))
        assertEquals("۷", PersianText.number(7))
        assertEquals("7", PersianText.number(7, persianDigits = false))
        assertEquals("۱۴۰۵", PersianText.number(1405))
    }

    @Test
    fun `title cleanup trims punctuation and spacing`() {
        assertEquals("جلسه با تیم", PersianText.cleanTitle("  جلسه   با تیم  "))
        assertEquals("جلسه", PersianText.cleanTitle("جلسه ،"))
        assertEquals("خرید", PersianText.cleanTitle("- خرید -"))
    }
}
