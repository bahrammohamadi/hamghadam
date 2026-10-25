package ir.hamghadam.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class QuickAddParserTest {

    /** 2026-10-25, i.e. 1405/08/03 — a Sunday. */
    private val today = JalaliDate(1405, 8, 3)

    private fun parse(text: String) = QuickAddParser.parse(text, today)

    @Test
    fun `tomorrow with morning hour`() {
        val result = parse("فردا ساعت ۹ صبح جلسه با تیم")
        assertEquals("جلسه با تیم", result.title)
        assertEquals(JalaliDate(1405, 8, 4), result.date)
        assertEquals(LocalTime.of(9, 0), result.time)
    }

    @Test
    fun `today keyword`() {
        val result = parse("امروز خرید نان")
        assertEquals("خرید نان", result.title)
        assertEquals(today, result.date)
        assertNull(result.time)
    }

    @Test
    fun `day after tomorrow keeps colon time and high priority`() {
        val result = parse("پس‌فردا ۱۵:۳۰ جلسه !!")
        assertEquals("جلسه", result.title)
        assertEquals(JalaliDate(1405, 8, 5), result.date)
        assertEquals(LocalTime.of(15, 30), result.time)
        assertEquals(QuickAddParser.PRIORITY_HIGH, result.priority)
    }

    @Test
    fun `weekday resolves to next occurrence`() {
        // 1405/08/03 is Sunday, so the next Monday is the following day.
        val result = parse("دوشنبه ورزش")
        assertEquals(JalaliDate(1405, 8, 4), result.date)
        assertEquals("ورزش", result.title)

        val thursday = parse("پنجشنبه ساعت ۹:۰۰ دندانپزشک")
        assertEquals(JalaliDate(1405, 8, 7), thursday.date)
        assertEquals(LocalTime.of(9, 0), thursday.time)
        assertEquals("دندانپزشک", thursday.title)
    }

    @Test
    fun `relative day counts`() {
        val result = parse("۳ روز بعد پرداخت قبض")
        assertEquals(JalaliDate(1405, 8, 6), result.date)
        assertEquals("پرداخت قبض", result.title)
    }

    @Test
    fun `relative week month and year`() {
        assertEquals(JalaliDate(1405, 8, 10), parse("هفته بعد سفر").date)
        assertEquals(JalaliDate(1405, 9, 3), parse("ماه بعد تمدید اشتراک").date)
        assertEquals(JalaliDate(1406, 8, 3), parse("سال بعد مالیات").date)
        assertEquals(JalaliDate(1405, 8, 17), parse("۲ هفته بعد پروژه").date)
    }

    @Test
    fun `day and month name rolls to next year when passed`() {
        val result = parse("۱۵ مهر تولد مادر")
        assertEquals(JalaliDate(1406, 7, 15), result.date)
        assertEquals("تولد مادر", result.title)

        val future = parse("۱۴ آبان ساعت ۱۰:۱۵ جلسه")
        assertEquals(JalaliDate(1405, 8, 14), future.date)
        assertEquals(LocalTime.of(10, 15), future.time)
    }

    @Test
    fun `month name before day is supported`() {
        val result = parse("مهر ۱۵ تولد مادر")
        assertEquals(JalaliDate(1406, 7, 15), result.date)
        assertEquals("تولد مادر", result.title)
    }

    @Test
    fun `full numeric date`() {
        val result = parse("1405/08/10 گزارش ماهانه")
        assertEquals(JalaliDate(1405, 8, 10), result.date)
        assertEquals("گزارش ماهانه", result.title)
    }

    @Test
    fun `weekday together with explicit date is consumed once`() {
        val result = parse("سه شنبه ۱۲ آبان ۱۴۰۵ قرارداد")
        assertEquals(JalaliDate(1405, 8, 12), result.date)
        assertEquals("قرارداد", result.title)
    }

    @Test
    fun `time without date leaves the date empty`() {
        val result = parse("ساعت ۲۱ مطالعه")
        assertNull(result.date)
        assertEquals(LocalTime.of(21, 0), result.time)
        assertEquals("مطالعه", result.title)
    }

    @Test
    fun `half past and quarters`() {
        val result = parse("۹ و نیم شب دارو")
        assertEquals(LocalTime.of(21, 30), result.time)
        assertEquals("دارو", result.title)
    }

    @Test
    fun `noon and midnight words`() {
        assertEquals(LocalTime.NOON, parse("ظهر ناهار با علی").time)
        assertEquals(LocalTime.MIDNIGHT, parse("نیمه‌شب بیدار شدن").time)
    }

    @Test
    fun `numbers in the title are not treated as dates or times`() {
        val result = parse("خرید ۳ عدد تخم‌مرغ")
        assertNull(result.date)
        assertNull(result.time)
        assertEquals("خرید ۳ عدد تخم‌مرغ", result.title)
        assertTrue(result.isPlainText)
    }

    @Test
    fun `priority markers are removed from the title`() {
        val high = parse("!!! کار مهم")
        assertEquals(QuickAddParser.PRIORITY_HIGH, high.priority)
        assertEquals("کار مهم", high.title)

        val low = parse("! کار کم‌اهمیت")
        assertEquals(QuickAddParser.PRIORITY_LOW, low.priority)
        assertEquals("کار کم‌اهمیت", low.title)
    }

    @Test
    fun `arabic letter variants and latin digits are accepted`() {
        val result = parse("فردا ساعت 9 جلسه")
        assertEquals(LocalTime.of(9, 0), result.time)
        assertEquals(JalaliDate(1405, 8, 4), result.date)
    }

    @Test
    fun `plain sentence has no matches`() {
        val result = parse("برنامه‌ریزی هفته")
        assertNull(result.date)
        assertNull(result.time)
        assertTrue(result.matches.isEmpty())
        assertEquals("برنامه‌ریزی هفته", result.title)
    }
}
