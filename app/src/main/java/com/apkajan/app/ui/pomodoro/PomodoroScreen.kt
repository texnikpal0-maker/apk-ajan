package com.apkajan.app.ui.pomodoro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apkajan.app.R
import com.apkajan.app.domain.PomodoroPhase
import com.apkajan.app.ui.components.BackTopBar
import com.apkajan.app.ui.components.SectionCard
import com.apkajan.app.ui.components.Stepper
import com.apkajan.app.ui.main.MainViewModel
import com.apkajan.app.ui.theme.Danger
import com.apkajan.app.ui.theme.Ok
import com.apkajan.app.ui.theme.Tomato
import kotlinx.coroutines.delay

@Composable
fun PomodoroScreen(vm: MainViewModel, onBack: () -> Unit) {
    val pomo by vm.pomodoro.collectAsStateWithLifecycle()
    val work by vm.workMinutes.collectAsStateWithLifecycle()
    val brk by vm.breakMinutes.collectAsStateWithLifecycle()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pomo.running, pomo.endAt) {
        while (pomo.running) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }

    val totalMs = if (pomo.running) pomo.phaseTotalMillis else work * 60_000L
    val remainingMs = if (pomo.running) pomo.remainingMillis(now) else totalMs
    val progress = if (totalMs > 0) remainingMs.toFloat() / totalMs else 0f
    val secs = (remainingMs + 999) / 1000
    val phaseColor = if (pomo.phase == PomodoroPhase.WORK) Tomato else Ok
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(stringResource(R.string.pomodoro), onBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dairesel geri sayım
            Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 18.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke)
                    )
                    drawArc(
                        color = phaseColor, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(
                            when {
                                !pomo.running -> R.string.pomodoro_idle
                                pomo.phase == PomodoroPhase.WORK -> R.string.pomodoro_work
                                else -> R.string.pomodoro_break
                            }
                        ),
                        color = if (pomo.running) phaseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "%02d:%02d".format(secs / 60, secs % 60),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (pomo.running) {
                        Text(
                            stringResource(R.string.pomodoro_cycles, pomo.completedCycles),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (pomo.running) {
                Button(
                    onClick = { vm.stopPomodoro() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) { Text("■  " + stringResource(R.string.pomodoro_stop), fontSize = 16.sp) }
            } else {
                Button(
                    onClick = { vm.startPomodoro() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Tomato)
                ) { Text("▶  " + stringResource(R.string.pomodoro_start), fontSize = 16.sp) }
            }

            SectionCard(title = stringResource(R.string.pomodoro_work_minutes)) {
                Stepper(value = work, onChange = vm::setWorkMinutes, step = 5, min = 5, max = 120)
            }
            SectionCard(title = stringResource(R.string.pomodoro_break_minutes)) {
                Stepper(value = brk, onChange = vm::setBreakMinutes, step = 1, min = 1, max = 30)
            }
            Text(
                stringResource(R.string.pomodoro_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
