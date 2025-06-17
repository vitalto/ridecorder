package ru.ridecorder.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import ru.ridecorder.domain.analysis.GraphDataPoint
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import java.text.DecimalFormat


@Composable
fun ChartCard(
    title: String,
    data: List<GraphDataPoint>,
    yUnit: String = "",
    xTitle: String = ""
) {
    if(data.isEmpty()){
        return
    }
    val yDecimalFormat = if (yUnit.isBlank()) {
        DecimalFormat("#.##")
    } else {
        DecimalFormat("#.## '$yUnit'")
    }

    val axisTitleComponent = rememberTextComponent(MaterialTheme.colorScheme.onSurfaceVariant)

    // Создаем форматтер для оси, добавляющий единицы измерения к значению
    val markerValueFormatter = DefaultCartesianMarker.ValueFormatter.default(yDecimalFormat)
    val startAxisValueFormatter = CartesianValueFormatter.decimal(yDecimalFormat)

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(Unit) {
        modelProducer.runTransaction {
            lineSeries { series(x = data.map { it.x.toInt() }, y = data.map { it.y }) }
        }
    }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            CartesianChartHost(
                rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(valueFormatter = startAxisValueFormatter),
                    bottomAxis = if(xTitle.isBlank()) HorizontalAxis.rememberBottom()
                    else HorizontalAxis.rememberBottom(
                        title = xTitle,
                        titleComponent = axisTitleComponent,
                    ),
                    marker = rememberMarker(
                        valueFormatter = markerValueFormatter
                    ),
                ),
                modelProducer,
                zoomState = rememberVicoZoomState(initialZoom = Zoom.Content),
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
            )
        }
    }
}