package com.apkdv.clipdock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apkdv.clipdock.theme.ClipTheme

@Composable
fun ClipCardShell(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 11.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = ClipTheme.colors.surface,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun ImageClipCard(
    name: String,
    dimensions: String,
    sizeLabel: String,
    downloaded: Boolean,
    onClick: () -> Unit,
    onDownload: () -> Unit,
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
                Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .background(c.accentImage.bg.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                TypeBadge(
                    ClipType.IMAGE,
                    Modifier.align(Alignment.TopStart).padding(8.dp),
                )
                if (!downloaded) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(Color(0xBC2B2926))
                                .clickable { onDownload() },
                            contentAlignment = Alignment.Center,
                        ) {
                            ClipDockSymbol(ClipDockIconKind.Download, Modifier.size(16.dp), color = Color.White)
                        }
                        Spacer(Modifier.height(7.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xBC2B2926))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) { Text(sizeLabel, color = Color.White, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
                Text(name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                    color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text("$dimensions · $sizeLabel", style = MaterialTheme.typography.labelMedium, color = c.ink3)
            }
        }
    }
}

@Composable
fun FileClipCard(
    name: String,
    ext: String,
    sizeLabel: String,
    downloaded: Boolean,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ClipTheme.colors
    ClipCardShell(onClick, modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TypeBadge(ClipType.FILE)
            Spacer(Modifier.weight(1f))
            Text("", style = MaterialTheme.typography.labelMedium, color = c.ink3)
        }
        Spacer(Modifier.height(11.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp, 46.dp).clip(RoundedCornerShape(7.dp))
                    .background(c.accentFile.bg)
                    .border(1.dp, c.accentFile.fg.copy(alpha = 0.3f), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(ext, style = MaterialTheme.typography.labelSmall, color = c.accentFile.fg) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                    color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text(
                    if (downloaded) "$sizeLabel · 已缓存到本机" else "$sizeLabel · 未下载",
                    style = MaterialTheme.typography.labelMedium, color = c.ink3,
                )
            }
        }
        if (!downloaded) {
            Spacer(Modifier.height(11.dp))
            Row(
                Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(9.dp))
                    .background(c.coralSoft).clickable { onDownload() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ClipDockSymbol(ClipDockIconKind.Download, Modifier.size(13.dp), color = c.coralInk)
                Spacer(Modifier.width(6.dp))
                Text("通过 P2P 下载", style = MaterialTheme.typography.labelLarge, color = c.coralInk)
            }
        }
    }
}
