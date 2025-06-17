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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.component.shapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.compose.common.rememberVerticalLegend
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import ru.ridecorder.domain.analysis.GraphDataPoint
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.LegendItem
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import ru.ridecorder.R
import java.text.DecimalFormat

private val LegendLabelKey = ExtraStore.Key<Set<String>>()

@Composable
fun CompareChartCard(
    title: String,
    firstData: List<GraphDataPoint>,
    secondData: List<GraphDataPoint>,
    yUnit: String = "",
    xTitle: String = "",
) {
    val legendItemLabelComponent = rememberTextComponent(vicoTheme.textColor)
    val lineColors = listOf(Color(0xFF916CDA), Color(0xFFD877D8))
    val keys = setOf(
        stringResource(id = R.string.first_training),
        stringResource(id = R.string.second_training)
    )


    if(firstData.isEmpty() || secondData.isEmpty()){
        return
    }
    val yDecimalFormat = if (yUnit.isBlank()) {
        DecimalFormat("#.##")
    } else {
        DecimalFormat("#.## '$yUnit'")
    }

    // Создаем форматтер для оси, добавляющий единицы измерения к значению
    val markerValueFormatter = DefaultCartesianMarker.ValueFormatter.default(yDecimalFormat)
    val startAxisValueFormatter = CartesianValueFormatter.decimal(yDecimalFormat)

    val axisTitleComponent = rememberTextComponent(MaterialTheme.colorScheme.onSurfaceVariant)


    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(Unit) {
        modelProducer.runTransaction {
            lineSeries {
                series(x = firstData.map { it.x.toInt() }, y = firstData.map { it.y })
                series(x = secondData.map { it.x.toInt() }, y = secondData.map { it.y })
            }
            extras { extraStore -> extraStore[LegendLabelKey] = keys }
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
                    rememberLineCartesianLayer(LineCartesianLayer.LineProvider.series(
                        lineColors.map { color ->
                            LineCartesianLayer.rememberLine(
                                fill = LineCartesianLayer.LineFill.single(fill(color))
                            )
                        }
                    )),
                    startAxis = VerticalAxis.rememberStart(valueFormatter = startAxisValueFormatter),
                    bottomAxis = if(xTitle.isBlank()) HorizontalAxis.rememberBottom()
                    else HorizontalAxis.rememberBottom(
                        title = xTitle,
                        titleComponent = axisTitleComponent,
                    ),
                    marker = rememberMarker(
                        valueFormatter = markerValueFormatter
                    ),
                    legend =
                    rememberVerticalLegend(
                        items = { extraStore ->
                            extraStore[LegendLabelKey].forEachIndexed { index, label ->
                                add(
                                    LegendItem(
                                        shapeComponent(fill(lineColors[index]), CorneredShape.Pill),
                                        legendItemLabelComponent,
                                        label,
                                    )
                                )
                            }
                        },
                        padding = insets(top = 16.dp),
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