package com.apkajan.app.ui.main

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apkajan.app.R
import com.apkajan.app.core.AppLanguage
import com.apkajan.app.core.Permissions
import com.apkajan.app.data.apps.QuickApps
import com.apkajan.app.domain.LimitPolicy
import com.apkajan.app.domain.PomodoroPhase
import com.apkajan.app.ui.components.SectionCard
import com.apkajan.app.ui.components.Stepper
import com.apkajan.app.ui.theme.Danger
import com.apkajan.app.ui.theme.Ok
import com.apkajan.app.ui.theme.Tomato
import com.apkajan.app.ui.theme.Warn
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vm: MainViewModel,
    onLanguageChange: (String) -> Unit,
    onOpenStats: () -> Unit,
    onOpenPomodoro: () -> Unit,
    onOpenApps: () -> Unit
) {
    val language by vm.language.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    AppLanguage.entries.forEach { l ->
                        val selected = l.code == language
                        TextButton(
                            onClick = { onLanguageChange(l.code) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.width(44.dp)
                        ) {
                            Text(
                                l.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CounterCard(vm)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = onOpenPomodoro,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("🍅  " + stringResource(R.string.pomodoro)) }
                FilledTonalButton(
                    onClick = onOpenStats,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("📊  " + stringResource(R.string.statistics)) }
            }
            PomodoroMiniCard(vm, onOpenPomodoro)
            LimitCard(vm)
            AppsCard(vm, onOpenApps)
            PermissionsCard()
            RemoteSourceCard(vm)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CounterCard(vm: MainViewModel) {
    val context = LocalContext.current
    val counter by vm.counter.collectAsStateWithLifecycle()
    val monitoring by vm.monitoring.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }

    val color = when {
        counter.count >= counter.limit -> Danger
        counter.count >= counter.limit * 0.8 -> Warn
        else -> MaterialTheme.colorScheme.primary
    }

    SectionCard(title = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(if (monitoring) Ok else Color.Gray, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(if (monitoring) R.string.status_running else R.string.status_stopped),
                style = MaterialTheme.typography.labelLarge,
                color = if (monitoring) Ok else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.today_counter), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${counter.count}",
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                " / ${counter.limit}",
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = {
                if (counter.count < counter.limit) counter.count.toFloat() / counter.limit
                else LimitPolicy.progressInTier(counter.count, counter.limit)
            },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.next_warning_at, counter.nextWarningAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (monitoring) {
                Button(
                    onClick = { vm.stop() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) { Text("■  " + stringResource(R.string.stop), fontSize = 16.sp) }
            } else {
                val needAcc = stringResource(R.string.need_accessibility)
                Button(
                    onClick = {
                        if (!vm.start()) {
                            Toast.makeText(context, needAcc, Toast.LENGTH_LONG).show()
                            context.startActivity(Permissions.accessibilitySettingsIntent())
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ok)
                ) { Text("▶  " + stringResource(R.string.start), fontSize = 16.sp) }
            }
            OutlinedButton(
                onClick = { confirmReset = true },
                modifier = Modifier.height(52.dp)
            ) { Text(stringResource(R.string.reset)) }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reset_confirm_title)) },
            text = { Text(stringResource(R.string.reset_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { vm.resetCounter(); confirmReset = false }) {
                    Text(stringResource(R.string.reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun PomodoroMiniCard(vm: MainViewModel, onOpen: () -> Unit) {
    val pomo by vm.pomodoro.collectAsStateWithLifecycle()
    if (!pomo.running) return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pomo.endAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val rem = pomo.remainingMillis(now) / 1000
    SectionCard(title = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🍅", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(
                        if (pomo.phase == PomodoroPhase.WORK) R.string.pomodoro_work else R.string.pomodoro_break
                    ),
                    color = Tomato,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "%02d:%02d".format(rem / 60, rem % 60),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = onOpen) { Text(stringResource(R.string.pomodoro)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LimitCard(vm: MainViewModel) {
    val limit by vm.limit.collectAsStateWithLifecycle()
    SectionCard(title = stringResource(R.string.limit_title)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stepper(value = limit, onChange = vm::setLimit, step = 5, min = 5, max = 1000)
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(20, 30, 50, 100).forEach { v ->
                FilterChip(
                    selected = limit == v,
                    onClick = { vm.setLimit(v) },
                    label = { Text("$v") }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.limit_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppsCard(vm: MainViewModel, onOpenApps: () -> Unit) {
    val selected by vm.selectedPackages.collectAsStateWithLifecycle()
    val others = selected.filter { it !in QuickApps.quickPackages }.sorted()
    val notInstalled = stringResource(R.string.not_installed)

    SectionCard(title = stringResource(R.string.apps_title)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickApps.all.forEach { q ->
                val isSel = q.packages.any { it in selected }
                val installed = remember(q) { vm.isInstalled(q) }
                FilterChip(
                    selected = isSel,
                    onClick = { vm.toggleQuickApp(q) },
                    label = { Text(if (installed) q.name else "${q.name} ($notInstalled)") },
                    leadingIcon = if (isSel) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
        }
        if (others.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.other_apps) + ": " + others.joinToString { vm.label(it) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenApps) { Text(stringResource(R.string.choose_apps)) }
    }
}

@Composable
private fun PermissionsCard() {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }

    // refresh değişince yeniden okunur
    val acc = remember(refresh) { Permissions.isAccessibilityEnabled(context) }
    val overlay = remember(refresh) { Permissions.canDrawOverlays(context) }
    val notif = remember(refresh) { Permissions.hasNotificationPermission(context) }
    val battery = remember(refresh) { Permissions.isIgnoringBatteryOptimizations(context) }

    SectionCard(title = stringResource(R.string.permissions_title)) {
        PermissionRow(stringResource(R.string.perm_accessibility), acc) {
            context.startActivity(Permissions.accessibilitySettingsIntent())
        }
        PermissionRow(stringResource(R.string.perm_overlay), overlay) {
            context.startActivity(Permissions.overlaySettingsIntent(context))
        }
        PermissionRow(stringResource(R.string.perm_notifications), notif) {
            if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        PermissionRow(stringResource(R.string.perm_battery), battery) {
            try {
                context.startActivity(Permissions.batteryOptimizationIntent(context))
            } catch (e: Exception) {
                context.startActivity(Permissions.appDetailsIntent(context))
            }
        }
        if (!acc) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.perm_restricted_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { context.startActivity(Permissions.appDetailsIntent(context)) }) {
                Text(stringResource(R.string.open_app_info))
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onGrant: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (granted) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Ok)
        } else {
            FilledTonalButton(onClick = onGrant) { Text(stringResource(R.string.perm_grant)) }
        }
    }
}

@Composable
private fun RemoteSourceCard(vm: MainViewModel) {
    val context = LocalContext.current
    val saved by vm.remoteUrl.collectAsStateWithLifecycle()
    var text by remember(saved) { mutableStateOf(saved) }
    val savedMsg = stringResource(R.string.saved)

    SectionCard(title = stringResource(R.string.remote_title)) {
        Text(
            stringResource(R.string.remote_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.remote_url_label)) },
            placeholder = { Text("https://…/messages.json") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(
            onClick = {
                vm.setRemoteUrl(text)
                Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
            },
            enabled = text.trim() != saved
        ) { Text(stringResource(R.string.save)) }
    }
}
