package com.apkdv.clipdock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apkdv.clipdock.theme.AccentPair
import com.apkdv.clipdock.theme.ClipTheme

@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = ClipTheme.colors
    Column(modifier) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = c.ink3,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 8.dp),
        )
        Surface(shape = RoundedCornerShape(18.dp), color = c.surface, shadowElevation = 2.dp) {
            Column(content = content)
        }
    }
}

@Composable
private fun RowIcon(icon: ClipDockIconKind, accent: AccentPair) {
    Box(
        Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(accent.bg),
        contentAlignment = Alignment.Center,
    ) { ClipDockSymbol(icon, Modifier.size(16.dp), color = accent.fg) }
}

@Composable
private fun RowScaffold(
    icon: ClipDockIconKind,
    accent: AccentPair,
    label: String,
    divider: Boolean,
    trailing: @Composable RowScope.() -> Unit,
) {
    val c = ClipTheme.colors
    Column {
        Row(
            Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowIcon(icon, accent)
            Spacer(Modifier.width(13.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = c.ink, modifier = Modifier.weight(1f))
            trailing()
        }
        if (divider) Box(Modifier.padding(start = 15.dp).fillMaxWidth().height(0.5.dp).background(c.divider))
    }
}

@Composable
fun SettingsToggleRow(
    icon: ClipDockIconKind,
    accent: AccentPair,
    label: String,
    checked: Boolean,
    divider: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    RowScaffold(icon, accent, label, divider) {
        ClipSwitch(checked, onCheckedChange)
    }
}

@Composable
fun SettingsValueRow(
    icon: ClipDockIconKind,
    accent: AccentPair,
    label: String,
    value: String,
    divider: Boolean = true,
    onClick: () -> Unit = {},
) {
    val c = ClipTheme.colors
    Box(Modifier.clickable { onClick() }) {
        RowScaffold(icon, accent, label, divider) {
            if (value.isNotBlank()) {
                Text(value, style = MaterialTheme.typography.bodyLarge, color = c.ink3)
                Spacer(Modifier.width(4.dp))
            }
            ClipDockSymbol(ClipDockIconKind.Chevron, Modifier.size(18.dp), color = Color(0xFFCFC7B8))
        }
    }
}

@Composable
fun ClipSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = ClipTheme.colors
    Box(
        Modifier.size(width = 50.dp, height = 30.dp).clip(RoundedCornerShape(999.dp))
            .background(if (checked) c.coral else Color(0xFFD8D0C1))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier.padding(horizontal = 3.dp).size(24.dp)
                .clip(RoundedCornerShape(999.dp)).background(Color.White),
        )
    }
}
