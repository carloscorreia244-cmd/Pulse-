package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.formatCurrencyBrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationSheet(
    recipientName: String,
    recipientHandle: String,
    recipientTier: String,
    availableBalanceCents: Long,
    onDismiss: () -> Unit,
    onConfirmDonation: (amountCents: Long, note: String) -> Unit
) {
    val presetAmounts = listOf(500L, 1500L, 2500L, 5000L, 10000L)
    var selectedCents by remember { mutableLongStateOf(1500L) }
    var customReaisText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val effectiveCents = customReaisText.replace(",", ".").toDoubleOrNull()?.let {
        (it * 100).toLong()
    } ?: selectedCents

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.VolunteerActivism,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Enviar Doação Direta",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Para $recipientName ($recipientHandle)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                VerifiedBadge(tierRaw = recipientTier, size = 16.dp)
            }

            Text(
                text = "Saldo disponível: ${formatCurrencyBrl(availableBalanceCents)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetAmounts.take(4).forEach { cents ->
                    FilterChip(
                        selected = customReaisText.isBlank() && selectedCents == cents,
                        onClick = {
                            customReaisText = ""
                            selectedCents = cents
                        },
                        label = { Text(formatCurrencyBrl(cents)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = customReaisText,
                onValueChange = { customReaisText = it },
                label = { Text("Outro valor (R$)") },
                placeholder = { Text("Ex: 35,00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("donation_custom_amount_input")
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Mensagem de apoio (criptografada E2EE)") },
                placeholder = { Text("Parabéns pelo trabalho!") },
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("donation_note_input")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { onConfirmDonation(effectiveCents, note) },
                enabled = effectiveCents > 0 && effectiveCents <= availableBalanceCents,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_donation_button")
            ) {
                Text(
                    text = if (effectiveCents > availableBalanceCents) {
                        "Saldo insuficiente"
                    } else {
                        "Doar ${formatCurrencyBrl(effectiveCents)} agora"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
