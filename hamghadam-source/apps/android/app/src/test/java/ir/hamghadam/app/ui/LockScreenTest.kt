package ir.hamghadam.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.hamghadam.app.LockUiState
import ir.hamghadam.app.data.LocalProfile
import ir.hamghadam.app.ui.theme.HamghadamTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * UI tests for the lock screen, run on the JVM through Robolectric.
 *
 * These are the checks a device would otherwise catch by hand: the keypad is
 * reachable, four taps produce the PIN, a blocked pad refuses input, and the
 * error text is announced.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "fa-rIR-w360dp-h780dp-xxhdpi")
class LockScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val profile = LocalProfile(id = "", name = "مریم", emoji = "🌿")

    private fun render(
        state: LockUiState = LockUiState(enabled = true, locked = true),
        onUnlock: (String) -> Unit = {},
        onLockedOut: () -> Unit = {},
    ) {
        rule.setContent {
            HamghadamTheme(darkTheme = false) {
                LockScreen(state = state, profile = profile, onUnlock = onUnlock, onLockedOut = onLockedOut)
            }
        }
    }

    @Test
    fun keypadShowsAllTenDigitsAndAGreeting() {
        render()
        rule.onNodeWithText("سلام مریم").assertExists()
        for (digit in listOf("۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹", "۰")) {
            rule.onNodeWithText(digit).assertExists()
        }
    }

    @Test
    fun fourTapsProduceTheExpectedPin() {
        val entered = mutableListOf<String>()
        render(onUnlock = { entered += it })
        listOf("۱", "۳", "۵", "۷").forEach { rule.onNodeWithText(it).performClick() }
        assertTrue("هیچ PIN‌ای ارسال نشد", entered.isNotEmpty())
        assertEquals("1357", entered.first())
    }

    @Test
    fun blockedPadDoesNotCallUnlock() {
        var calls = 0
        render(
            state = LockUiState(enabled = true, locked = true, blockedSeconds = 30, failures = 3),
            onUnlock = { calls++ },
        )
        rule.onNodeWithText("۱").performClick()
        rule.onNodeWithText("۲").performClick()
        rule.onNodeWithText("۳").performClick()
        rule.onNodeWithText("۴").performClick()
        assertEquals("در حالت قفل موقت نباید تلاشی ارسال شود", 0, calls)
        rule.onNodeWithText("برای امنیت بیشتر، 30 ثانیه صبر کن", substring = true).assertExists()
    }

    @Test
    fun wrongPinMessageIsVisible() {
        render(state = LockUiState(enabled = true, locked = true, failures = 1, error = "PIN درست نیست"))
        rule.onNodeWithText("PIN درست نیست").assertExists()
    }
}
