package ru.ridecorder.ui.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ru.ridecorder.NavRoutes
import ru.ridecorder.R
import ru.ridecorder.ui.common.FeedWorkoutItem
import ru.ridecorder.ui.common.NoInternetScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.feed_title)) }
            )
        }
    ) { innerPadding ->
        // Можно взять отступы, если нужно
        val realPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
            top = innerPadding.calculateTopPadding(),
            end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(realPadding)
        ) {
            when {
                uiState.errorMessage != null -> {
                    NoInternetScreen { viewModel.loadFeed() }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = {
                            viewModel.loadFeed()
                        },
                    ) {
                        if (uiState.workouts.isEmpty()) {
                            EmptyFeedScreen()
                        } else {
                            // Отрисовка списка
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.workouts) { workout ->
                                    FeedWorkoutItem(
                                        workout = workout,
                                        onLikeClick = { viewModel.toggleLike(workout) },
                                        onItemClick = {
                                            navController.navigate(NavRoutes.Workout.createRoute(null, workout.id))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Пустая лента – когда ни у кого из подписок нет тренировок
 */
@Composable
fun EmptyFeedScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Image(
                painter = painterResource(id = R.drawable.ic_user_location),
                contentDescription = stringResource(id = R.string.empty_feed_icon_desc),
                modifier = Modifier
                    .size(120.dp)
                    .alpha(0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.empty_feed_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.empty_feed_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
