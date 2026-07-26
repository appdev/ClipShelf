package com.apkdv.clipdock.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── 中性：暖沙 ──────────────────────────────
val Sand        = Color(0xFFF3EEE5) // 页面背景 app background
val SandRaised  = Color(0xFFFFFFFF) // 卡片 / 浮层 surface
val SandSunken  = Color(0xFFEBE5DA) // 搜索框 / 内嵌输入 inset field
val SandCode    = Color(0xFFF4F1EA) // 代码块底 code block
val Hairline    = Color(0xFFEAE3D7) // 分隔线 1px
val Divider     = Color(0xFFEEE7DA) // 列表行内分隔线

val Ink         = Color(0xFF2B2926) // 主文字 primary text
val Ink2        = Color(0xFF8A8377) // 次级文字 secondary
val Ink3        = Color(0xFFA8A296) // 占位 / 三级 tertiary / placeholder
val InkFaint    = Color(0xFFB0A99C) // 时间戳等 timestamps

// ── 强调 ───────────────────────────────────
val Coral       = Color(0xFFCE6A40) // 主色 / 激活态 primary / active
val CoralSoft   = Color(0xFFF6E5DA) // 珊瑚浅底 tint background
val CoralInk    = Color(0xFFB05B30) // 珊瑚浅底上的文字 text on tint
val Online      = Color(0xFF5AA17B) // 在线 / 已同步
val OnlineSoft  = Color(0xFFE6F0EA)
val OnlineInk   = Color(0xFF3F7C5C)

// ── 类型彩色点缀（徽章底色 / 文字色）──────────
data class AccentPair(val bg: Color, val fg: Color)

val AccentText  = AccentPair(Color(0xFFF1E7D0), Color(0xFF9A7320)) // 文本 琥珀
val AccentCode  = AccentPair(Color(0xFFE8E2F6), Color(0xFF6A53B0)) // 代码 紫
val AccentLink  = AccentPair(Color(0xFFDCE7F5), Color(0xFF3E6AAE)) // 链接 蓝
val AccentImage = AccentPair(Color(0xFFD6EBE2), Color(0xFF2E8068)) // 图片 青
val AccentColor = AccentPair(Color(0xFFF6E0DE), Color(0xFFB05451)) // 颜色 玫红
val AccentFile  = AccentPair(Color(0xFFF7E5D2), Color(0xFFB16A31)) // 文件 橙

@Immutable
data class ClipColors(
    val background: Color,
    val surface: Color,
    val sunken: Color,
    val codeBlock: Color,
    val hairline: Color,
    val divider: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val inkFaint: Color,
    val coral: Color,
    val coralSoft: Color,
    val coralInk: Color,
    val online: Color,
    val onlineSoft: Color,
    val onlineInk: Color,
    val accentText: AccentPair,
    val accentCode: AccentPair,
    val accentLink: AccentPair,
    val accentImage: AccentPair,
    val accentColor: AccentPair,
    val accentFile: AccentPair,
)

val LightClipColors = ClipColors(
    background = Sand,
    surface = SandRaised,
    sunken = SandSunken,
    codeBlock = SandCode,
    hairline = Hairline,
    divider = Divider,
    ink = Ink,
    ink2 = Ink2,
    ink3 = Ink3,
    inkFaint = InkFaint,
    coral = Coral,
    coralSoft = CoralSoft,
    coralInk = CoralInk,
    online = Online,
    onlineSoft = OnlineSoft,
    onlineInk = OnlineInk,
    accentText = AccentText,
    accentCode = AccentCode,
    accentLink = AccentLink,
    accentImage = AccentImage,
    accentColor = AccentColor,
    accentFile = AccentFile,
)

// ── 深色模式：暖墨基调（来自设计稿精确值）──────────────────────────────
private val SandDark        = Color(0xFF1A1815)
private val SandRaisedDark  = Color(0xFF242018)
private val SandSunkenDark  = Color(0xFF14120F)
private val SandCodeDark    = Color(0xFF14120F)
private val HairlineDark    = Color(0xFF332E26)
private val DividerDark     = Color(0xFF2C2820)

private val InkDark         = Color(0xFFF1ECE2)
private val Ink2Dark        = Color(0xFFB0A89A)
private val Ink3Dark        = Color(0xFF837C6E)
private val InkFaintDark    = Color(0xFF6B6457)

private val CoralDark       = Color(0xFFE08152)
private val CoralSoftDark   = Color(0xFF3A2519)
private val CoralInkDark    = Color(0xFFE89B6E)
private val OnlineDark      = Color(0xFF6BB78C)
private val OnlineSoftDark  = Color(0xFF1E3127)
private val OnlineInkDark   = Color(0xFF7FC79E)

private val AccentTextDark  = AccentPair(Color(0xFF3A331E), Color(0xFFD9BE74))
private val AccentCodeDark  = AccentPair(Color(0xFF2C2740), Color(0xFFA593E0))
private val AccentLinkDark  = AccentPair(Color(0xFF1F2C3F), Color(0xFF7FA8DE))
private val AccentImageDark = AccentPair(Color(0xFF193029), Color(0xFF6FBF9D))
private val AccentColorDark = AccentPair(Color(0xFF3A2120), Color(0xFFDC8A87))
private val AccentFileDark  = AccentPair(Color(0xFF382818), Color(0xFFD99A5E))

val DarkClipColors = ClipColors(
    background = SandDark,
    surface = SandRaisedDark,
    sunken = SandSunkenDark,
    codeBlock = SandCodeDark,
    hairline = HairlineDark,
    divider = DividerDark,
    ink = InkDark,
    ink2 = Ink2Dark,
    ink3 = Ink3Dark,
    inkFaint = InkFaintDark,
    coral = CoralDark,
    coralSoft = CoralSoftDark,
    coralInk = CoralInkDark,
    online = OnlineDark,
    onlineSoft = OnlineSoftDark,
    onlineInk = OnlineInkDark,
    accentText = AccentTextDark,
    accentCode = AccentCodeDark,
    accentLink = AccentLinkDark,
    accentImage = AccentImageDark,
    accentColor = AccentColorDark,
    accentFile = AccentFileDark,
)

val LocalClipColors = staticCompositionLocalOf { LightClipColors }

object ClipTheme {
    val colors: ClipColors
        @Composable get() = LocalClipColors.current
}
