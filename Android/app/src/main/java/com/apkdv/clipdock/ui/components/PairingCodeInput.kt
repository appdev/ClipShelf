package com.apkdv.clipdock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.apkdv.clipdock.theme.ClipTheme
import com.apkdv.clipdock.theme.JetBrainsMono

@Composable
fun PairingCodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 5,
) {
    val c = ClipTheme.colors
    Box(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(length) { i ->
                val char = code.getOrNull(i)?.uppercaseChar()?.toString() ?: ""
                val focused = i == code.length
                Box(
                    Modifier
                        .weight(1f)
                        .height(52.dp)
                        .background(c.surface, RoundedCornerShape(13.dp))
                        .border(
                            width = if (focused) 1.5.dp else 1.dp,
                            color = if (focused) c.coral else c.hairline,
                            shape = RoundedCornerShape(13.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        char,
                        style = TextStyle(
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center,
                        ),
                        color = c.ink,
                    )
                }
            }
        }
        BasicTextField(
            value = code,
            onValueChange = { if (it.length <= length) onCodeChange(it.uppercase()) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            modifier = Modifier.matchParentSize(),
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            decorationBox = { it() },
        )
    }
}
