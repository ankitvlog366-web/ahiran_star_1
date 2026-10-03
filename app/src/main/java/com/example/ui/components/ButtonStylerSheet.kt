package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonStylerSheet(
    block: PostBlock,
    onDismiss: () -> Unit,
    onSave: (updatedBlock: PostBlock) -> Unit
) {
    var labelText by remember { mutableStateOf(block.text.ifBlank { "Click Here" }) }
    var buttonUrl by remember { mutableStateOf(block.buttonUrl) }
    var bgColor by remember { mutableStateOf(block.buttonBgColor) }
    var textColor by remember { mutableStateOf(block.buttonTextColor) }
    var strokeColor by remember { mutableStateOf(block.buttonStrokeColor) }
    var strokeWidth by remember { mutableStateOf(block.buttonStrokeWidth) }
    var cornerRadius by remember { mutableStateOf(block.buttonCornerRadius) }
    var shadowElevation by remember { mutableStateOf(block.buttonShadowElevation) }
    var buttonSize by remember { mutableStateOf(block.buttonSize) }

    val colorPalette = listOf(
        0xFFFF5722L, // Blogger Orange
        0xFFE53935L, // Red
        0xFF8E24AAL, // Purple
        0xFF1E88E5L, // Blue
        0xFF00897BL, // Teal
        0xFF43A047L, // Green
        0xFF212121L, // Black
        0xFF6D4C41L, // Brown
        0xFFFFB300L  // Amber
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
                .testTag("button_styler_sheet")
        ) {
            Text(
                text = "Customize Button & Live Preview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // LIVE WYSIWYG PREVIEW BOX
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LIVE BLOGGER BUTTON PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val previewBg = Color(bgColor)
                    val previewText = Color(textColor)
                    val previewStroke = Color(strokeColor)

                    val widthModifier = when (buttonSize) {
                        "SMALL" -> Modifier.wrapContentWidth()
                        "MEDIUM" -> Modifier.widthIn(min = 160.dp)
                        "LARGE" -> Modifier.widthIn(min = 220.dp)
                        else -> Modifier.fillMaxWidth()
                    }

                    Box(
                        modifier = Modifier
                            .then(widthModifier)
                            .shadow(shadowElevation.dp, shape = RoundedCornerShape(cornerRadius.dp))
                            .clip(RoundedCornerShape(cornerRadius.dp))
                            .background(previewBg)
                            .then(
                                if (strokeWidth > 0) Modifier.border(
                                    strokeWidth.dp,
                                    previewStroke,
                                    RoundedCornerShape(cornerRadius.dp)
                                ) else Modifier
                            )
                            .padding(
                                horizontal = when (buttonSize) {
                                    "SMALL" -> 16.dp
                                    "LARGE" -> 32.dp
                                    else -> 24.dp
                                },
                                vertical = when (buttonSize) {
                                    "SMALL" -> 10.dp
                                    "LARGE" -> 18.dp
                                    else -> 14.dp
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelText.ifBlank { "Click Here" },
                            color = previewText,
                            fontWeight = FontWeight.Bold,
                            fontSize = when (buttonSize) {
                                "SMALL" -> 14.sp
                                "LARGE" -> 18.sp
                                else -> 16.sp
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button Label & URL inputs
            OutlinedTextField(
                value = labelText,
                onValueChange = { labelText = it },
                label = { Text("Button Text / Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = buttonUrl,
                onValueChange = { buttonUrl = it },
                label = { Text("Destination URL (https://...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Background Color Picker
            Text(
                text = "Button Background Color",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colorPalette.forEach { col ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(col))
                            .border(
                                if (bgColor == col) 3.dp else 1.dp,
                                if (bgColor == col) MaterialTheme.colorScheme.primary else Color.LightGray,
                                CircleShape
                            )
                            .clickable { bgColor = col }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text Color Options
            Text(
                text = "Button Text Color",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = textColor == 0xFFFFFFFFL,
                    onClick = { textColor = 0xFFFFFFFFL },
                    label = { Text("White Text") }
                )
                FilterChip(
                    selected = textColor == 0xFF212121L,
                    onClick = { textColor = 0xFF212121L },
                    label = { Text("Black Text") }
                )
                FilterChip(
                    selected = textColor == 0xFFFFD54FL,
                    onClick = { textColor = 0xFFFFD54FL },
                    label = { Text("Gold Text") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stroke / Border Width
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stroke: ${strokeWidth}px",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(100.dp)
                )
                Slider(
                    value = strokeWidth.toFloat(),
                    onValueChange = { strokeWidth = it.toInt() },
                    valueRange = 0f..6f,
                    steps = 5,
                    modifier = Modifier.weight(1f)
                )
            }

            // Stroke Color selection if stroke > 0
            if (strokeWidth > 0) {
                Text(
                    text = "Stroke Color",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0xFFFFFFFFL, 0xFF000000L, 0xFFE64A19L, 0xFF1976D2L, 0xFF388E3CL).forEach { sCol ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(sCol))
                                .border(
                                    if (strokeColor == sCol) 2.5.dp else 1.dp,
                                    if (strokeColor == sCol) MaterialTheme.colorScheme.primary else Color.Gray,
                                    CircleShape
                                )
                                .clickable { strokeColor = sCol }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Corner Radius Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Radius: ${cornerRadius}px",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(100.dp)
                )
                Slider(
                    value = cornerRadius.toFloat(),
                    onValueChange = { cornerRadius = it.toInt() },
                    valueRange = 0f..32f,
                    modifier = Modifier.weight(1f)
                )
            }

            // Shadow / Elevation Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shadow: ${shadowElevation}dp",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(100.dp)
                )
                Slider(
                    value = shadowElevation.toFloat(),
                    onValueChange = { shadowElevation = it.toInt() },
                    valueRange = 0f..12f,
                    steps = 11,
                    modifier = Modifier.weight(1f)
                )
            }

            // Button Size Chips
            Text(
                text = "Button Size",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("FULL" to "Full Width", "MEDIUM" to "Medium", "LARGE" to "Large", "SMALL" to "Small").forEach { (sKey, sLabel) ->
                    FilterChip(
                        selected = buttonSize == sKey,
                        onClick = { buttonSize = sKey },
                        label = { Text(sLabel) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        val updated = block.copy(
                            text = labelText,
                            buttonUrl = buttonUrl,
                            buttonBgColor = bgColor,
                            buttonTextColor = textColor,
                            buttonStrokeColor = strokeColor,
                            buttonStrokeWidth = strokeWidth,
                            buttonCornerRadius = cornerRadius,
                            buttonShadowElevation = shadowElevation,
                            buttonSize = buttonSize
                        )
                        onSave(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply Style")
                }
            }
        }
    }
}
