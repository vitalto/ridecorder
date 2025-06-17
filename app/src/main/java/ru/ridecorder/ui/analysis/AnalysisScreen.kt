package ru.ridecorder.ui.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.ridecorder.R
import ru.ridecorder.di.ContextResourceProvider
import ru.ridecorder.domain.analysis.AnalysisStats
import ru.ridecorder.ui.common.ColumnChartCard
import ru.ridecorder.ui.common.DateRangePickerModal
import ru.ridecorder.ui.common.LoadingScreen
import ru.ridecorder.ui.helpers.ConvertHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState
    var showDatePicker by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.analysis_title)) },
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PaddingValues(
                        start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                        top = paddingValues.calculateTopPadding(),
                        end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                    ))
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Добавляем PeriodSelector в верхнюю часть экрана
                PeriodSelector(
                    selectedPeriod = uiState.selectedPeriod,
                    customDataRange = uiState.customDataRange,
                    onPeriodSelected = {
                        if (it == Period.CUSTOM) showDatePicker = true
                        else viewModel.selectPeriod(it)
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                when {
                    uiState.isLoading -> LoadingScreen(Modifier.weight(1f))
                    uiState.stats != null -> {
                        StatsGrid(stats = uiState.stats!!)

                        GraphsSection(uiState.stats!!)
                    }

                    else -> ErrorMessage(Modifier.weight(1f))
                }
            }
            if (showDatePicker) {
                DateRangePickerModal(
                    onDateRangeSelected = { dateRange ->
                        val (startDate, endDate) = dateRange
                        if (startDate != null && endDate != null) {
                            viewModel.selectPeriod(Period.CUSTOM)
                            viewModel.setCustomPeriod(dateRange)
                        }
                        showDatePicker = false
                    },
                    onDismiss = { showDatePicker = false }
                )
            }
        }
    )
}

@Composable
private fun PeriodSelector(
    selectedPeriod: Period,
    customDataRange: Pair<Long?, Long?>?,
    onPeriodSelected: (Period) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Text(
                customDataRange?.let {
                    "${ConvertHelper.formatTimestamp(ContextResourceProvider(context), it.first, true)} - ${ConvertHelper.formatTimestamp(ContextResourceProvider(context), it.second, true)}"
                } ?: stringResource(id = selectedPeriod.displayNameRes)
            )
            Icon(
                imageVector = when (selectedPeriod) {
                    Period.WEEK -> Icons.Default.CalendarToday
                    Period.MONTH -> Icons.Default.CalendarMonth
                    Period.CUSTOM -> Icons.Default.DateRange
                },
                contentDescription = null,
                modifier = Modifier.padding(start = 5.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Period.entries.forEach { period ->
                DropdownMenuItem(
                    text =
                    {
                        Text(stringResource(id = period.displayNameRes))
                    },
                    onClick = {
                        onPeriodSelected(period)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(
    stats: AnalysisStats,
    modifier: Modifier = Modifier
) {
    val statPairs = stats.toList(ContextResourceProvider(LocalContext.current)).chunked(2)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        statPairs.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                pair.forEach { (title, value) ->
                    StatCard(
                        title = title,
                        value = value,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Добавляем заполнитель для нечетных элементов
                if (pair.size % 2 != 0) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}


@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun ErrorMessage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(id = R.string.no_data_message), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun GraphsSection(stats: AnalysisStats) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            stringResource(id = R.string.graphs_section_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        ColumnChartCard(
            stringResource(id = R.string.chart_calories),
            stats.caloriesGraph,
            stringResource(id = R.string.unit_calories),
            stringResource(id = R.string.unit_training_no)
        )
        Spacer(Modifier.height(16.dp))
        ColumnChartCard(
            stringResource(id = R.string.chart_speed_avg),
            stats.averageSpeedGraph,
            stringResource(id = R.string.unit_speed_kph),
            stringResource(id = R.string.unit_training_no)
        )
        Spacer(Modifier.height(16.dp))
        ColumnChartCard(
            stringResource(id = R.string.chart_distance),
            stats.distanceGraph,
            stringResource(id = R.string.unit_distance_meters),
            stringResource(id = R.string.unit_training_no)
        )
        Spacer(Modifier.height(16.dp))
        ColumnChartCard(
            stringResource(id = R.string.chart_altitude_gain),
            stats.altitudeGainGraph,
            stringResource(id = R.string.unit_altitude_meters),
            stringResource(id = R.string.unit_training_no)
        )
        Spacer(Modifier.height(16.dp))
        ColumnChartCard(
            stringResource(id = R.string.chart_pace),
            stats.paceGraph,
            stringResource(id = R.string.unit_pace),
            stringResource(id = R.string.unit_training_no)
        )
        Spacer(Modifier.height(16.dp))
        ColumnChartCard(
            stringResource(id = R.string.chart_time_in_motion),
            stats.timeInMotionGraph,
            stringResource(id = R.string.unit_time_hours),
            stringResource(id = R.string.unit_training_no)
        )
        Spacer(Modifier.height(16.dp))
        ColumnChartCard(
            stringResource(id = R.string.chart_heart_rate_avg),
            stats.averageHeartRateGraph,
            stringResource(id = R.string.unit_heart_rate),
            stringResource(id = R.string.unit_training_no)
        )
    }
}
