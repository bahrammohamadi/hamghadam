package ir.hamghadam.app.ui

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.hamghadam.app.LockUiState
import ir.hamghadam.app.data.AppSettings
import ir.hamghadam.app.data.DEFAULT_PROFILE_ID
import ir.hamghadam.app.data.LocalProfile
import ir.hamghadam.app.ui.theme.AccentColor
import ir.hamghadam.app.ui.theme.HamghadamTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * C-01 regression: opening the account panel must render for a fresh install,
 * where [profiles] contains no row at all (the implicit first account is not
 * stored). Before 0.6.2 this threw `NoSuchElementException` from `first()`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "fa-rIR-w360dp-h780dp-xxhdpi")
class ProfileScreenCrashTest {

    @get:Rule
    val rule = createComposeRule()

    private val implicitAccount = LocalProfile(id = DEFAULT_PROFILE_ID, name = "حساب من", emoji = "🌿")

    private fun actions() = ProfileActions(
        onSwitch = {},
        onCreate = { _, _, _ -> },
        onUpdate = { _, _, _, _ -> },
        onDelete = {},
        onEnableLock = { true },
        onDisableLock = { true },
        onLockAfter = {},
        onSecureScreen = {},
        onAccent = { _: AccentColor -> },
        onCompact = {},
        onExport = { _: Uri, _: String? -> },
        onImport = { _: Uri, _: String? -> },
        onOpenSettings = {},
        onBack = {},
    )

    @Test
    fun accountPanelRendersWithNoStoredProfile() {
        rule.setContent {
            HamghadamTheme(darkTheme = false) {
                ProfileScreen(
                    profiles = emptyList(),
                    activeId = DEFAULT_PROFILE_ID,
                    defaultProfile = implicitAccount,
                    settings = AppSettings(),
                    lockState = LockUiState(),
                    lockAfterMinutes = 1,
                    secureScreen = false,
                    actions = actions(),
                )
            }
        }

        rule.onNodeWithText("حساب و پنل کاربری").assertExists()
        assertTrue(
            "حساب اصلی باید حتی وقتی فهرست خالی است دیده شود",
            rule.onAllNodesWithText("حساب من").fetchSemanticsNodes().isNotEmpty(),
        )
        rule.onNodeWithText("حساب روی این دستگاه", substring = true).assertExists()
    }
}
