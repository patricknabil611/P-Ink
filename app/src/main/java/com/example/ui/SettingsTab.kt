package com.example.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.AppSettings
import com.example.data.WritingPadSize
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CharcoalSurfaceVariant
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanOnContainer
import com.example.ui.theme.CyanOnPrimary
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldDone
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoseErrorContainer
import com.example.ui.theme.RoseOnErrorContainer
import com.example.ui.theme.SlateSurface
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsTab(
    settings: AppSettings,
    syncState: SyncUiState,
    onSignInWithGoogle: (Context) -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
    onSetCloudSyncEnabled: (Boolean) -> Unit,
    onSelectPadBackground: (String) -> Unit,
    onSelectPenColor: (String) -> Unit,
    onSetShowGuideLines: (Boolean) -> Unit,
    onSetAutoConvertEnabled: (Boolean) -> Unit,
    onSetWritingPadSize: (WritingPadSize) -> Unit,
    onSetCompactLists: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_header),
                style = MaterialTheme.typography.headlineMedium,
                color = IceWhite,
            )

            // 1. Google Sync Account
            SettingsCard(
                icon = Icons.Rounded.AccountCircle,
                title = stringResource(R.string.settings_google_sync_title),
                testTag = "settings_google_sync_card",
            ) {
                if (syncState.isSignedIn) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CyanContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = EmeraldDone,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = syncState.userName?.takeIf { it.isNotBlank() }
                                    ?: syncState.userEmail
                                    ?: stringResource(R.string.settings_signed_in_badge),
                                style = MaterialTheme.typography.titleMedium,
                                color = IceWhite,
                            )
                            if (!syncState.userEmail.isNullOrBlank()) {
                                Text(
                                    text = syncState.userEmail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CoolSlate,
                                )
                            }
                            syncState.lastSyncTimeMs?.let { syncedMs ->
                                val timeText = remember(syncedMs) {
                                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(syncedMs))
                                }
                                Text(
                                    text = "${stringResource(R.string.settings_signed_in_badge)} • $timeText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldDone,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsToggleRow(
                        title = stringResource(R.string.settings_auto_sync),
                        description = stringResource(R.string.settings_auto_sync_desc),
                        checked = settings.cloudSyncEnabled,
                        onCheckedChange = onSetCloudSyncEnabled,
                        testTag = "settings_auto_sync_switch",
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onSyncNow,
                            enabled = !syncState.isSyncing,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanPrimary,
                                contentColor = CyanOnPrimary,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("sync_now_button"),
                        ) {
                            if (syncState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = CyanOnPrimary,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.settings_syncing),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.CloudSync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.settings_sync_now),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = IceWhite,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("sign_out_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.settings_sign_out),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.settings_google_sync_subtitle_off),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoolSlate,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onSignInWithGoogle(context) },
                        enabled = !syncState.isSigningIn,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = CyanOnPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_signin_button"),
                    ) {
                        if (syncState.isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = CyanOnPrimary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_signing_in),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_sign_in_google),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                val syncErrorText = syncState.customError
                    ?: syncState.errorRes?.let { stringResource(it) }
                if (!syncErrorText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = RoseErrorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, RoseError.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                    ) {
                        Text(
                            text = syncErrorText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = RoseOnErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            // 2. Colors Settings
            SettingsCard(
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.settings_colors_title),
                testTag = "settings_colors_card",
            ) {
                Text(
                    text = stringResource(R.string.settings_colors_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoolSlate,
                )
                Spacer(modifier = Modifier.height(12.dp))
                InkColorSettingsSection(
                    settings = settings,
                    onSelectBackground = onSelectPadBackground,
                    onSelectPenColor = onSelectPenColor,
                )
            }

            // 3. View Settings
            SettingsCard(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.settings_view_title),
                testTag = "settings_view_card",
            ) {
                SettingsToggleRow(
                    title = stringResource(R.string.settings_show_guides),
                    description = stringResource(R.string.settings_show_guides_desc),
                    checked = settings.showGuideLines,
                    onCheckedChange = onSetShowGuideLines,
                    testTag = "settings_guides_switch",
                )
                HorizontalDivider(
                    color = BorderSubtle,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                SettingsToggleRow(
                    title = stringResource(R.string.settings_auto_convert),
                    description = stringResource(R.string.settings_auto_convert_desc),
                    checked = settings.autoConvertEnabled,
                    onCheckedChange = onSetAutoConvertEnabled,
                    testTag = "settings_autoconvert_switch",
                )
                HorizontalDivider(
                    color = BorderSubtle,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Text(
                    text = stringResource(R.string.settings_pad_size),
                    style = MaterialTheme.typography.titleMedium,
                    color = IceWhite,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PadSizeChip(
                        label = stringResource(R.string.settings_pad_size_compact),
                        selected = settings.writingPadSize == WritingPadSize.COMPACT,
                        onClick = { onSetWritingPadSize(WritingPadSize.COMPACT) },
                        testTag = "pad_size_compact",
                        modifier = Modifier.weight(1f),
                    )
                    PadSizeChip(
                        label = stringResource(R.string.settings_pad_size_standard),
                        selected = settings.writingPadSize == WritingPadSize.STANDARD,
                        onClick = { onSetWritingPadSize(WritingPadSize.STANDARD) },
                        testTag = "pad_size_standard",
                        modifier = Modifier.weight(1f),
                    )
                    PadSizeChip(
                        label = stringResource(R.string.settings_pad_size_tall),
                        selected = settings.writingPadSize == WritingPadSize.TALL,
                        onClick = { onSetWritingPadSize(WritingPadSize.TALL) },
                        testTag = "pad_size_tall",
                        modifier = Modifier.weight(1f),
                    )
                }
                HorizontalDivider(
                    color = BorderSubtle,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
                SettingsToggleRow(
                    title = stringResource(R.string.settings_compact_lists),
                    description = stringResource(R.string.settings_compact_lists_desc),
                    checked = settings.compactLists,
                    onCheckedChange = onSetCompactLists,
                    testTag = "settings_compact_switch",
                )
            }

            // 4. About App
            SettingsCard(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.settings_about_title),
                testTag = "settings_about_card",
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(CyanPrimary, PinkAccent)))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = IceWhite,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.settings_about_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoolSlate,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_about_eraser_tip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyanOnContainer,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AboutMetaPill(
                        label = stringResource(R.string.settings_about_version_label),
                        value = BuildConfig.VERSION_NAME,
                        modifier = Modifier.weight(1f),
                    )
                    AboutMetaPill(
                        label = stringResource(R.string.settings_about_build_label),
                        value = BuildConfig.VERSION_CODE.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    testTag: String,
    content: @Composable () -> Unit,
) {
    Surface(
        color = SlateSurface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .testTag(testTag),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = IceWhite,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = IceWhite,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = CoolSlate,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyanOnPrimary,
                checkedTrackColor = CyanPrimary,
                uncheckedThumbColor = CoolSlate,
                uncheckedTrackColor = CharcoalSurfaceVariant,
            ),
            modifier = Modifier.testTag(testTag),
        )
    }
}

@Composable
private fun PadSizeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (selected) CyanContainer else CharcoalSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (selected) CyanPrimary else BorderSubtle,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) CyanOnContainer else CoolSlate,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun AboutMetaPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = ObsidianBg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MutedSlate,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = IceWhite,
            )
        }
    }
}
