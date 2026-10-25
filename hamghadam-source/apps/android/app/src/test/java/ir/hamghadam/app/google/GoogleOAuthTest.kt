package ir.hamghadam.app.google

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleOAuthTest {

    @Test
    fun `authorization url carries the pkce parameters`() {
        val url = GoogleOAuth.authorizeUrl(
            clientId = "123.apps.googleusercontent.com",
            redirectUri = GoogleOAuth.redirectUri(52123),
            challenge = "abc_DEF-123",
            state = "state-1",
        )
        assertTrue(url.startsWith(GoogleOAuth.AUTH_ENDPOINT))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("code_challenge=abc_DEF-123"))
        assertTrue(url.contains("access_type=offline"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("state=state-1"))
        assertTrue(url.contains("redirect_uri=http%3A%2F%2F127.0.0.1%3A52123%2Foauth2callback"))
        assertTrue(url.contains("scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fcalendar.readonly"))
    }

    @Test
    fun `loopback redirect points at the requested port`() {
        assertEquals("http://127.0.0.1:52099/oauth2callback", GoogleOAuth.redirectUri(52099))
    }

    @Test
    fun `query parsing decodes codes and rejects unknown keys`() {
        assertEquals("4/0Abc", GoogleOAuth.parseQueryParameter("code=4%2F0Abc&state=s", "code"))
        assertEquals("a b", GoogleOAuth.parseQueryParameter("error=a+b", "error"))
        assertNull(GoogleOAuth.parseQueryParameter("code=x", "state"))
        assertNull(GoogleOAuth.parseQueryParameter("", "code"))
        assertNull(GoogleOAuth.parseQueryParameter(null, "code"))
    }

    @Test
    fun `free port probe stays inside the private range`() {
        val port = GoogleOAuth.freePort()
        assertTrue(port in 52000..52999)
    }

    @Test
    fun `token expiry needs a small safety margin`() {
        val valid = GoogleOAuth.Token(accessToken = "a", refreshToken = "r", expiresAtMillis = System.currentTimeMillis() + 600_000)
        assertFalse(valid.isExpired)
        val expired = valid.copy(expiresAtMillis = System.currentTimeMillis() + 30_000)
        assertTrue(expired.isExpired)
        assertNotEquals(valid.accessToken, "b")
    }
}

class GoogleCalendarApiTest {

    @Test
    fun `events url asks for single expanded events`() {
        val url = GoogleCalendarApi.eventsUrl("2026-10-01T00:00:00+00:00", "2026-12-01T00:00:00+00:00")
        assertTrue(url.startsWith("https://www.googleapis.com/calendar/v3/calendars/primary/events"))
        assertTrue(url.contains("singleEvents=true"))
        assertTrue(url.contains("orderBy=startTime"))
        assertTrue(url.contains("timeMin=2026-10-01T00%3A00%3A00%2B00%3A00"))
    }

    @Test
    fun `events payload maps all-day and timed events`() {
        val json = """
            {
              "items": [
                {"id": "1", "status": "confirmed", "summary": "جلسه با تیم",
                 "start": {"dateTime": "2026-10-25T09:00:00+00:00"},
                 "end": {"dateTime": "2026-10-25T10:00:00+00:00"},
                 "location": "اتاق ۲", "htmlLink": "https://calendar.google.com/event?eid=1"},
                {"id": "2", "status": "confirmed", "summary": "روز تعطیل",
                 "start": {"date": "2026-10-26"}, "end": {"date": "2026-10-27"}},
                {"id": "3", "status": "cancelled", "start": {"date": "2026-10-27"}},
                {"id": "4", "start": {"date": "2026-10-28"}}
              ]
            }
        """.trimIndent()
        val events = GoogleCalendarApi.parseEvents(json)
        assertEquals(3, events.size)

        val meeting = events.first { it.title == "جلسه با تیم" }
        assertEquals(LocalDate.of(2026, 10, 25).toEpochDay(), meeting.epochDay)
        assertFalse(meeting.allDay)
        assertEquals("اتاق ۲", meeting.location)
        assertTrue(meeting.htmlLink.startsWith("https://calendar.google.com"))

        val holiday = events.first { it.title == "روز تعطیل" }
        assertTrue(holiday.allDay)
        assertEquals(LocalDate.of(2026, 10, 26).toEpochDay(), holiday.epochDay)
        assertEquals(LocalDate.of(2026, 10, 27).toEpochDay(), holiday.endEpochDay)

        assertTrue(events.any { it.title == "رویداد بدون عنوان" })
    }

    @Test
    fun `broken payloads are empty instead of crashing`() {
        assertTrue(GoogleCalendarApi.parseEvents("{}").isEmpty())
        assertTrue(GoogleCalendarApi.parseEvents("not json").isEmpty())
        assertTrue(GoogleCalendarApi.parseEvents("""{"items":[{"id":"x"}]}""").isEmpty())
    }
}
