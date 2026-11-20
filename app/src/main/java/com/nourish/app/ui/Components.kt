package com.nourish.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.nourish.app.ui.theme.NourishMotion
import com.nourish.app.ui.theme.NourishShape
import com.nourish.app.ui.theme.NourishType
import com.nourish.app.ui.theme.nourishColors
import java.text.NumberFormat

@Composable
internal fun Int.formatted(): String = NumberFormat.getIntegerInstance(LocalLocale.current.platformLocale).format(this)

@Composable
internal fun Heading(text: String, modifier: Modifier = Modifier, style: TextStyle = NourishType.section) {
    Text(text, modifier.semantics { heading() }, color = nourishColors.text, style = style)
}

@Composable
internal fun PrimaryPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: Glyph? = null,
    background: Color = nourishColors.ink,
    foreground: Color = nourishColors.cream,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction)
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(NourishShape.pill)
            .background(background)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = NourishType.strong, color = foreground)
        if (glyph != null) NourishIcon(glyph, foreground)
    }
}

@Composable
internal fun pressScale(interaction: MutableInteractionSource): State<Float> {
    val pressed by interaction.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = if (pressed) NourishMotion.press else NourishMotion.settle,
        label = "button pressure",
    )
}

@Composable
internal fun EmptyState(
    title: String,
    message: String,
    glyph: Glyph,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val colors = nourishColors
    Column(
        modifier
            .fillMaxWidth()
            .clip(NourishShape.panel)
            .background(colors.panel)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.clip(NourishShape.small).background(accent).padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            NourishIcon(glyph, colors.ink)
        }
        Heading(title, style = NourishType.heading)
        Text(message, style = NourishType.body, color = colors.muted)
    }
}
