package com.apkdv.clipdock.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ClipShapes = Shapes(
    extraSmall = RoundedCornerShape(9.dp),
    small      = RoundedCornerShape(11.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

object ClipRadius {
    val chip = 999.dp
    val card = 16.dp
    val tile = 11.dp
    val codeBlock = 9.dp
    val badge = 8.dp
}

object ClipSpacing {
    val screenH = 20.dp
    val cardGap = 10.dp
    val cardPad = 11.dp
    val sectionTop = 18.dp
    val tabBarBottom = 28.dp
}

object ClipElevation {
    val card = 6.dp
    val floating = 12.dp
}
