package com.apkdv.clipdock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.apkdv.clipdock.ui.components.ClipDockIconKind
import com.apkdv.clipdock.ui.components.ClipDockSymbol
import com.apkdv.clipdock.ui.components.ClipType
import com.apkdv.clipdock.ui.components.TypeBadge
import com.apkdv.clipdock.theme.ClipTheme
import com.apkdv.clipdock.theme.Mono

@Composable
fun ClipDetailScreen(
    onBack: () -> Unit,
    onCopy: () -> Unit,
    onPin: () -> Unit = {},
    onShare: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = ClipTheme.colors
    Column(modifier.fillMaxSize().background(c.background)) {

        // ── 顶部返回栏 ──
        Row(
            Modifier.fillMaxWidth()
                .background(c.background.copy(alpha = 0.9f))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(ClipDockIconKind.X, "返回", onBack)
            Spacer(Modifier.weight(1f))
            Text("详情", style = MaterialTheme.typography.titleMedium, color = c.ink)
            Spacer(Modifier.weight(1f))
            CircleIconButton(ClipDockIconKind.More, "更多", {})
        }

        // ── 可滚动内容 ──
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                TypeBadge(ClipType.LINK)
                Spacer(Modifier.width(8.dp))
                Text("复制自 Safari", style = MaterialTheme.typography.bodySmall, color = c.ink2)
            }
            Spacer(Modifier.height(8.dp))

            // OG 大封面占位
            Box(
                Modifier.fillMaxWidth().height(172.dp).clip(RoundedCornerShape(16.dp))
                    .background(c.accentLink.bg.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) { Text("OG 封面 · 1200×630", style = Mono, color = c.accentLink.fg.copy(alpha = 0.6f)) }

            Spacer(Modifier.height(16.dp))
            Text("ClipDock", style = MaterialTheme.typography.titleLarge, color = c.ink)
            Spacer(Modifier.height(8.dp))
            Text(
                "A local-first clipboard shelf. 在 macOS 上找回、预览、固定并复用剪贴板内容，支持与手机同步、P2P 下载文件。",
                style = MaterialTheme.typography.bodyMedium, color = c.ink2,
            )

            // URL 行
            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(13.dp), color = c.surface, shadowElevation = 1.dp) {
                Row(
                    Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(14.dp).background(Color(0xFF24292E), RoundedCornerShape(4.dp)))
                    Spacer(Modifier.width(10.dp))
                    Text("github.com/ClipDock", style = Mono, color = Color(0xFF3A372F),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // 元信息表
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = c.surface, shadowElevation = 1.dp) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    MetaRow("复制时间", "今天 12:06", divider = true)
                    MetaRow("同步空间", "已同步 · 3 台设备", divider = false, online = true)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── 底部操作栏 ──
        Row(
            Modifier.fillMaxWidth()
                .background(c.surface.copy(alpha = 0.94f))
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 主操作：复制
            Surface(
                onClick = onCopy,
                shape = RoundedCornerShape(14.dp),
                color = c.coral,
                modifier = Modifier.weight(1f).height(50.dp),
            ) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    ClipDockSymbol(ClipDockIconKind.Copy, Modifier.size(17.dp), color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("复制", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
            SquareIconButton(ClipDockIconKind.Pin, "固定", onPin, tint = c.coral)
            SquareIconButton(ClipDockIconKind.Share, "分享", onShare, tint = c.ink2)
            SquareIconButton(ClipDockIconKind.Trash, "删除", onDelete, tint = Color(0xFFC2756B))
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String, divider: Boolean, online: Boolean = false) {
    val c = ClipTheme.colors
    Column {
        Row(
            Modifier.fillMaxWidth().height(46.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = c.ink2)
            Spacer(Modifier.weight(1f))
            if (online) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(c.online))
                Spacer(Modifier.width(6.dp))
            }
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = c.ink)
        }
        if (divider) Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.divider))
    }
}

@Composable
private fun CircleIconButton(icon: ClipDockIconKind, cd: String, onClick: () -> Unit) {
    val c = ClipTheme.colors
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(c.surface).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { ClipDockSymbol(icon, Modifier.size(18.dp), color = c.ink2) }
}

@Composable
private fun SquareIconButton(icon: ClipDockIconKind, cd: String, onClick: () -> Unit, tint: Color) {
    val c = ClipTheme.colors
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = c.surface,
        shadowElevation = 1.dp, modifier = Modifier.size(50.dp)) {
        Box(contentAlignment = Alignment.Center) { ClipDockSymbol(icon, Modifier.size(20.dp), color = tint) }
    }
}
