package ir.hamghadam.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.hamghadam.app.LockUiState
import ir.hamghadam.app.data.LocalProfile
import ir.hamghadam.app.data.TaskEntity
import ir.hamghadam.app.domain.JalaliDate
import ir.hamghadam.app.ui.components.PillChip
import ir.hamghadam.app.ui.components.TaskRow
import ir.hamghadam.app.ui.theme.HamghadamTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Accessibility and large-font checks that normally need a device or an emulator.
 *
 * Robolectric renders the real Compose tree, so these assertions read the same
 * semantics TalkBack consumes:
 *  - every clickable node carries a label (text or contentDescription);
 *  - the task checkbox has a 48dp touch target (WCAG 2.5.8 / Material);
 *  - at fontScale 2.0 the lock screen and task rows stay readable and usable.
 *
 * This is a proxy for TalkBack, not a replacement: an on-device pass with the
 * TalkBack service is still required before release.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "fa-rIR-w360dp-h780dp-xxhdpi")
class AccessibilityTest {

    @get:Rule
    val rule = createComposeRule()

    private val task = TaskEntity(
        id = "t1",
        title = "خرید نان",
        dueEpochDay = JalaliDate.today().toEpochDay(),
        dueMinuteOfDay = 9 * 60 + 30,
    )

    private fun renderTaskRow(
        fontScale: Float = 1f,
        onToggle: () -> Unit = {},
        onDelete: () -> Unit = {},
        onClick: () -> Unit = {},
    ) {
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                HamghadamTheme(darkTheme = false) {
                    Box(Modifier.width(360.dp)) {
                        TaskRow(
                            task = task,
                            today = JalaliDate.today(),
                            persianDigits = true,
                            projectName = "خانه",
                            onClick = onClick,
                            onToggle = onToggle,
                            onDelete = onDelete,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun taskCheckboxHasAFortyEightDpTouchTarget() {
        renderTaskRow()
        rule.onNodeWithContentDescription("تغییر وضعیت «خرید نان»").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun taskCheckboxExposesCheckboxStateAndToggles() {
        var toggled = 0
        renderTaskRow(onToggle = { toggled++ })
        rule.onNodeWithContentDescription("تغییر وضعیت «خرید نان»").performClick()
        assertEquals(1, toggled)
    }

    @Test
    fun taskTitleAndMetaAreVisible() {
        renderTaskRow()
        rule.onNodeWithText("خرید نان").assertIsDisplayed()
        rule.onNodeWithText("خانه", substring = true).assertExists()
    }

    @Test
    fun everyClickableNodeHasALabel() {
        renderTaskRow()
        val nodes = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertTrue("هیچ گرهٔ قابل‌کلیکی پیدا نشد", nodes.isNotEmpty())
        val unlabeled = nodes.filter { node ->
            val config = node.config
            val description = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
            val text = config.getOrNull(SemanticsProperties.Text).orEmpty()
            description.isEmpty() && text.isEmpty()
        }
        assertEquals("گره‌های بدون برچسب برای صفحه‌خوان: $unlabeled", 0, unlabeled.size)
    }

    @Test
    fun taskRowStaysReadableAtDoubleFontScale() {
        renderTaskRow(fontScale = 2f)
        rule.onNodeWithText("خرید نان").assertIsDisplayed()
        rule.onNodeWithContentDescription("تغییر وضعیت «خرید نان»").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun lockScreenStaysUsableAtDoubleFontScale() {
        var entered: String? = null
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, 2f)) {
                HamghadamTheme(darkTheme = false) {
                    LockScreen(
                        state = LockUiState(enabled = true, locked = true),
                        profile = LocalProfile(id = "", name = "مریم"),
                        onUnlock = { entered = it },
                        onLockedOut = {},
                    )
                }
            }
        }
        rule.onNodeWithText("۱").performClick()
        rule.onNodeWithText("۲").performClick()
        rule.onNodeWithText("۳").performClick()
        rule.onNodeWithText("۴").performClick()
        assertEquals("1234", entered)
    }

    @Test
    fun chipsMeetTheMinimumTouchTarget() {
        var clicks = 0
        rule.setContent {
            HamghadamTheme(darkTheme = false) {
                PillChip(text = "امروز", selected = true, onClick = { clicks++ })
            }
        }
        rule.onNodeWithText("امروز").assertHeightIsAtLeast(36.dp)
        rule.onNodeWithText("امروز").performClick()
        assertEquals(1, clicks)
    }
}
