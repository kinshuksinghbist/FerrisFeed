package com.ferrisfeed.coreui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Trap card: the single most common compiler error / misconception for this reel.
 * Collapsible so it does not steal attention from the main explainer, but the
 * warning header is always visible (learning science: pre-expose the pitfall).
 */
@Composable
fun TrapCard(
    trap: String,
    compilerMessage: String? = null,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Common trap",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse trap" else "Expand trap",
                    )
                }
            }
            Text(
                text = trap,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            AnimatedVisibility(visible = expanded && compilerMessage != null) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = compilerMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = CodeFontFamily),
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}

@Preview(name = "TrapCard dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun TrapCardPreview() {
    FerrisFeedTheme(darkTheme = true) {
        TrapCard(
            trap = "Using s after takes(s) — String was moved.",
            compilerMessage = "error[E0382]: borrow of moved value: `s`",
            modifier = Modifier.padding(16.dp),
            initiallyExpanded = true,
        )
    }
}
