package com.apkdv.clipdock.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.apkdv.clipdock.theme.ClipTheme
import com.apkdv.clipdock.theme.Mono

@Composable
private fun CardHeader(
    type: ClipType,
    trailing: String,
    pinned: Boolean = false,
) {
    val c = ClipTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        TypeBadge(type)
        Spacer(Modifier.weight(1f))
        if (pinned) {
            Box(Modifier.size(12.dp).background(c.coral, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(6.dp))
        }
        Text(trailing, style = MaterialTheme.typography.labelMedium, color = c.inkFaint)
    }
}

@Composable
fun CodeClipCard(
    code: String,
    source: String,
    pinned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ClipTheme.colors
    ClipCardShell(onClick, modifier) {
        CardHeader(ClipType.CODE, trailing = "", pinned = pinned)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(c.codeBlock)
                .padding(horizontal = 10.dp, vertical = 9.dp),
        ) {
            Text(code, style = Mono, color = Color(0xFF3A372F), maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(8.dp))
        Text(source, style = MaterialTheme.typography.labelMedium, color = c.ink3)
    }
}

@Composable
fun LinkClipCard(
    title: String,
    description: String,
    domain: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ClipTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = c.surface,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(96.dp).background(c.accentLink.bg.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                TypeBadge(ClipType.LINK, Modifier.align(Alignment.TopStart).padding(8.dp))
            }
            Column(Modifier.padding(11.dp)) {
                Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                    color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                    color = c.ink2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(Color(0xFF24292E), RoundedCornerShape(3.dp)))
                    Spacer(Modifier.width(5.dp))
                    Text(domain, style = MaterialTheme.typography.labelMedium, color = c.ink3)
                }
            }
        }
    }
}

@Composable
fun ColorClipCard(
    color: Color,
    hex: String,
    rgb: String,
    source: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ClipTheme.colors
    ClipCardShell(onClick, modifier) {
        CardHeader(ClipType.COLOR, trailing = source)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(10.dp)).background(color))
        Spacer(Modifier.height(9.dp))
        Text(hex, style = Mono.copy(fontWeight = FontWeight.SemiBold, fontSize = TextUnit(14f, TextUnitType.Sp)), color = c.ink)
        Spacer(Modifier.height(5.dp))
        Text(rgb, style = Mono, color = c.ink3)
    }
}

@Composable
fun TextClipCard(
    text: String,
    source: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ClipTheme.colors
    ClipCardShell(onClick, modifier) {
        CardHeader(ClipType.TEXT, trailing = time)
        Spacer(Modifier.height(9.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Color(0xFF3A372F),
            maxLines = 5, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Text(source, style = MaterialTheme.typography.labelMedium, color = c.ink3)
    }
}
