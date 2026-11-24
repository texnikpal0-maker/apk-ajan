package com.apkajan.app.ui.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.apkajan.app.AppContainer
import com.apkajan.app.ApkAjanApp
import com.apkajan.app.R
import com.apkajan.app.data.apps.InstalledApp
import com.apkajan.app.ui.components.BackTopBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppPickerViewModel(private val c: AppContainer) : ViewModel() {
    private val _apps = MutableStateFlow<List<InstalledApp>?>(null)
    val apps: StateFlow<List<InstalledApp>?> = _apps.asStateFlow()
    val selected = c.settings.selectedPackages

    init {
        viewModelScope.launch { _apps.value = c.installedApps.launchableApps() }
    }

    fun toggle(pkg: String) {
        val cur = selected.value
        c.settings.setSelectedPackages(if (pkg in cur) cur - pkg else cur + pkg)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AppPickerViewModel((this[APPLICATION_KEY] as ApkAjanApp).container) }
        }
    }
}

/** "Diğer yüklü uygulamalar": kullanıcı istediği uygulamayı takibe ekler/çıkarır. */
@Composable
fun AppPickerScreen(onBack: () -> Unit) {
    val vm: AppPickerViewModel = viewModel(factory = AppPickerViewModel.Factory)
    val apps by vm.apps.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(stringResource(R.string.other_apps), onBack) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.search_apps)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Text(
                stringResource(R.string.selected_count, selected.size),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val list = apps
            if (list == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filtered = if (query.isBlank()) list else list.filter {
                    it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
                }
                // Ekran açıldığında seçili olanlar en üstte (tıklayınca liste zıplamasın diye sabit)
                val initialSelected = remember(list) { selected }
                val sorted = filtered.sortedByDescending { it.packageName in initialSelected }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(sorted, key = { it.packageName }) { app ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { vm.toggle(app.packageName) }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(
                                checked = app.packageName in selected,
                                onCheckedChange = { vm.toggle(app.packageName) }
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
