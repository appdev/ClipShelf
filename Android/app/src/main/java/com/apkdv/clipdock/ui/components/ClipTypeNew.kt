package com.apkdv.clipdock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.apkdv.clipdock.theme.AccentPair
import com.apkdv.clipdock.theme.ClipTheme

enum class ClipType(val label: String) {
    TEXT("文本"),
    CODE("代码"),
    LINK("链接"),
    IMAGE("图片"),
    COLOR("颜色"),
    FILE("文件"),
}

@Composable
fun ClipType.accent(): AccentPair = when (this) {
    ClipType.TEXT  -> ClipTheme.colors.accentText
    ClipType.CODE  -> ClipTheme.colors.accentCode
    ClipType.LINK  -> ClipTheme.colors.accentLink
    ClipType.IMAGE -> ClipTheme.colors.accentImage
    ClipType.COLOR -> ClipTheme.colors.accentColor
    ClipType.FILE  -> ClipTheme.colors.accentFile
}

@Composable
fun TypeBadge(type: ClipType, modifier: Modifier = Modifier) {
    val a = type.accent()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(a.bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = type.label,
            color = a.fg,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        )
    }
}
