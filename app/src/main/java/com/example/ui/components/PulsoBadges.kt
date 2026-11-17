package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.VerificationTier
import com.example.model.parseVerificationTier
import com.example.ui.theme.PulsoCreatorGold
import com.example.ui.theme.PulsoEmerald
import com.example.ui.theme.PulsoLiveCoral
import com.example.ui.theme.PulsoPartnerEmerald
import com.example.ui.theme.PulsoVerifiedBlue

@Composable
fun VerifiedBadge(
    tierRaw: String,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    val tier = parseVerificationTier(tierRaw)
    if (tier == VerificationTier.NONE) return

    val (tint, icon) = when (tier) {
        VerificationTier.BLUE_VERIFIED -> PulsoVerifiedBlue to Icons.Filled.Verified
        VerificationTier.GOLD_CREATOR -> PulsoCreatorGold to Icons.Rounded.WorkspacePremium
        VerificationTier.EMERALD_PARTNER -> PulsoPartnerEmerald to Icons.Filled.Verified
        VerificationTier.NONE -> return
    }

    Icon(
        imageVector = icon,
        contentDescription = stringResource(R.string.verified_badge_desc),
        tint = tint,
        modifier = modifier.size(size)
    )
}

@Composable
fun VerificationTierPill(tierRaw: String, modifier: Modifier = Modifier) {
    val tier = parseVerificationTier(tierRaw)
    val (label, color) = when (tier) {
        VerificationTier.BLUE_VERIFIED -> "Conta Verificada" to PulsoVerifiedBlue
        VerificationTier.GOLD_CREATOR -> "Criador Ouro" to PulsoCreatorGold
        VerificationTier.EMERALD_PARTNER -> "Parceiro Oficial" to PulsoPartnerEmerald
        VerificationTier.NONE -> "Não Verificado" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (tier != VerificationTier.NONE) {
                VerifiedBadge(tierRaw = tierRaw, size = 14.dp)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
fun E2EEShieldPill(
    verified: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (verified) PulsoEmerald else MaterialTheme.colorScheme.secondary
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = stringResource(R.string.e2ee_shield_desc),
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = if (verified) "E2EE Verificada" else "E2EE Ativa",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
fun PulsoAvatar(
    name: String,
    colorHex: Long,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    isLive: Boolean = false
) {
    val initials = name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "P" }

    val baseColor = Color(colorHex)

    Box(modifier = modifier.size(size)) {
        val ringModifier = if (isLive) {
            Modifier
                .size(size)
                .border(
                    width = 2.5.dp,
                    brush = Brush.linearGradient(listOf(PulsoLiveCoral, PulsoCreatorGold)),
                    shape = CircleShape
                )
                .padding(3.dp)
        } else {
            Modifier.size(size)
        }

        Box(
            modifier = ringModifier
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(baseColor, baseColor.copy(alpha = 0.72f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36f).sp
            )
        }

        if (isLive) {
            Surface(
                color = PulsoLiveCoral,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = "LIVE",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        } else if (isOnline) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(PulsoEmerald)
            )
        }
    }
}
