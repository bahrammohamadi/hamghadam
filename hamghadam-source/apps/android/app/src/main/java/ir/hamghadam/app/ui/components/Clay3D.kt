package ir.hamghadam.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamghadam.app.R

/**
 * The "clay / 3D" layer.
 *
 * Everything here exists to give flat Material surfaces believable depth:
 *  - [Modifier.clay]      two stacked shadows (a tight contact shadow and a wide
 *                         ambient one) plus a top rim highlight;
 *  - [ClayCard]           a pressable surface that sinks ~3% and loses elevation
 *                         while pressed, like a physical key;
 *  - [ClayTile]           the coloured bento tile from the reference design;
 *  - [SectionArt]         a soft-clay illustration that breathes (float loop);
 *  - [ClayBanner]         full-width hero card with an illustration.
 *
 * Motion is intentionally slow (4–6 s) and small (≤6dp): the goal is the feeling
 * of a soft object, not a distracting animation. Everything degrades gracefully
 * when the system has "remove animations" on, because the resting state is the
 * final composition.
 */

/** Linear blend towards [other]; t = 0 keeps this colour. */
fun Color.mix(other: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * k,
        green = green + (other.green - green) * k,
        blue = blue + (other.blue - blue) * k,
        alpha = alpha + (other.alpha - alpha) * k,
    )
}

fun Color.lighten(t: Float): Color = mix(Color.White, t)

fun Color.darken(t: Float): Color = mix(Color.Black, t)

/**
 * Clay surface: depth comes from a wide ambient shadow, a tight contact shadow
 * and a highlight along the top edge. `pressed` flattens the object.
 */
fun Modifier.clay(
    color: Color,
    radius: Dp = 26.dp,
    pressed: Boolean = false,
): Modifier {
    val shape = RoundedCornerShape(radius)
    val lift = if (pressed) 3f else 10f
    return this
        .then(
            if (pressed) Modifier
            else Modifier.shadow(
                elevation = lift.dp,
                shape = shape,
                clip = false,
                ambientColor = color.darken(0.55f).copy(alpha = 0.55f),
                spotColor = color.darken(0.65f).copy(alpha = 0.45f),
            ),
        )
        .shadow(
            elevation = if (pressed) 1.dp else 2.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.18f),
            spotColor = Color.Black.copy(alpha = 0.22f),
        )
        .clip(shape)
        .background(
            Brush.linearGradient(
                0f to color.lighten(if (pressed) 0.02f else 0.14f),
                0.45f to color,
                1f to color.darken(if (pressed) 0.02f else 0.10f),
            ),
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = if (pressed) 0.10f else 0.45f),
                1f to Color.White.copy(alpha = 0f),
            ),
            shape = shape,
        )
}

/** A raised, pressable clay card. */
@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    radius: Dp = 26.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, label = "clayPress")
    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .fillMaxWidth()
            .clay(color = color, radius = radius, pressed = pressed)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        content = content,
    )
}

/**
 * Bento tile from the reference design: one big illustration, a bold value/name
 * and a soft label, on a saturated clay colour with a subtle tilt.
 */
@Composable
fun ClayTile(
    title: String,
    subtitle: String,
    value: String?,
    @DrawableRes art: Int,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 148.dp,
    tilt: Float = 0f,
    artSize: Dp = 78.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "tilePress")
    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = tilt
            }
            .clay(color = color, radius = 28.dp, pressed = pressed)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        // soft inner glow behind the illustration
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(artSize + 34.dp)
                .background(
                    Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)),
                    CircleShape,
                ),
        )
        Image(
            painter = painterResource(art),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 6.dp, start = 4.dp)
                .size(artSize),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, end = 12.dp, bottom = 12.dp),
        ) {
            if (value != null) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = title,
                fontSize = if (value == null) 16.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Soft-clay illustration with a slow float. Used in banners, headers and empty
 * states so every section has its own visual identity.
 */
@Composable
fun SectionArt(
    @DrawableRes art: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    floating: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "artFloat")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (floating) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "artOffset",
    )
    val swing by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (floating) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "artSwing",
    )
    Box(
        modifier = modifier
            .size(size + 18.dp)
            .graphicsLayer {
                translationY = -offset * 6.dp.toPx()
                rotationZ = (swing - 0.5f) * 4.5f
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    Brush.radialGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)),
                    CircleShape,
                ),
        )
        Image(
            painter = painterResource(art),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size),
        )
    }
}

/**
 * Wide hero card: gradient clay background, illustration on one side, copy and
 * an optional action on the other.
 */
@Composable
fun ClayBanner(
    title: String,
    subtitle: String,
    @DrawableRes art: Int,
    startColor: Color,
    endColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    action: String? = null,
    artSize: Dp = 108.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "bannerPress")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clay(color = startColor, radius = 30.dp, pressed = pressed)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(158.dp)
                .background(
                    Brush.linearGradient(
                        listOf(startColor.lighten(0.16f), startColor, endColor.darken(0.05f)),
                    ),
                ),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (action != null) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.22f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(action, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            SectionArt(art = art, size = artSize)
        }
    }
}

/** Compact circular clay button used in headers. */
@Composable
fun ClayIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "iconPress")
    Box(
        modifier = modifier
            .size(46.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clay(color = color, radius = 23.dp, pressed = pressed)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Small "3D" pill used for badges inside clay cards. */
@Composable
fun ClayPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clay(color = color, radius = 14.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

/** Shared illustrations so screens do not repeat resource names. */
object SectionArtwork {
    val HOME = R.drawable.art_home
    val TASKS = R.drawable.art_tasks
    val CALENDAR = R.drawable.art_calendar
    val FOCUS = R.drawable.art_focus
    val HABITS = R.drawable.art_habits
    val PROFILE = R.drawable.art_profile
    val TEMPLATES = R.drawable.art_templates
    val STATS = R.drawable.art_stats
}
