package com.apkdv.clipdock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apkdv.clipdock.theme.ClipTheme

enum class ClipTab(val label: String, val icon: ClipDockIconKind) {
    RECENTS("最近", ClipDockIconKind.History),
    PINBOARD("固定", ClipDockIconKind.Pin),
    DEVICES("设备", ClipDockIconKind.Devices),
    SETTINGS("设置", ClipDockIconKind.Settings),
}

@Composable
fun ClipBottomBar(
    current: ClipTab,
    onSelect: (ClipTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ClipTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surface.copy(alpha = 0.92f))
            .navigationBarsPadding()
            .padding(top = 9.dp, bottom = 8.dp, start = 12.dp, end = 12.dp),
    ) {
        ClipTab.entries.forEach { tab ->
            val active = tab == current
            val tint = if (active) c.coral else c.ink3
            Column(
                modifier = Modifier.weight(1f).clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ClipDockSymbol(tab.icon, Modifier.size(24.dp), color = tint)
                Spacer(Modifier.height(4.dp))
                Text(tab.label, color = tint, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
