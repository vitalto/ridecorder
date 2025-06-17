package ru.ridecorder.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ru.ridecorder.R
import ru.ridecorder.ui.analysis.AnalysisScreen
import ru.ridecorder.ui.feed.FeedScreen
import ru.ridecorder.ui.profile.ProfileScreen
import ru.ridecorder.ui.workouts.MyWorkoutsScreen

// --------------------
// Определяем вкладки нижней панели
// --------------------
enum class BottomTab(@StringRes val titleResId: Int, val icon: @Composable () -> Unit) {
    FEED(
        R.string.tab_feed,
        { Icon(Icons.Filled.DynamicFeed, contentDescription = stringResource(id = R.string.tab_feed)) }
    ),
    WORKOUTS(
        R.string.tab_workouts,
        { Icon(Icons.Filled.FitnessCenter, contentDescription = stringResource(id = R.string.tab_workouts)) }
    ),
    RECORD(
        R.string.tab_record,
        { Icon(Icons.Filled.FiberManualRecord, contentDescription = stringResource(id = R.string.tab_record)) }
    ),
    ANALYSIS(
        R.string.tab_analysis,
        { Icon(Icons.Filled.Analytics, contentDescription = stringResource(id = R.string.tab_analysis)) }
    ),
    PROFILE(
        R.string.tab_profile,
        { Icon(Icons.Filled.Person, contentDescription = stringResource(id = R.string.tab_profile)) }
    )
}

@Composable
fun MainScreen(navController: NavController, onRecordClick: () -> Unit, onLogoutClick: () -> Unit, viewModel: MainViewModel = hiltViewModel()) {
    // Какая вкладка выбрана (для отображения обычного контента)
    var selectedTab by rememberSaveable { mutableStateOf(BottomTab.WORKOUTS) }

    // Главное содержимое - кладём Scaffold в Box,
    // а сверху будем анимированно показывать экран записи
    Box(modifier = Modifier.fillMaxSize()) {
        // 1) Scaffold с TopAppBar и нижней панелью (NavigationBar)
        Scaffold(
            topBar = {},
            bottomBar = {
                NavigationBar {
                    // Порядок: Друзья | Тренировки | Запись (центр) | Анализ | Профиль
                    BottomTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = (tab == selectedTab),
                            onClick = {
                                if (tab == BottomTab.RECORD) {
                                    onRecordClick()
                                } else {
                                    selectedTab = tab
                                }
                            },
                            icon = { tab.icon() },
                            label = { Text(stringResource(id = tab.titleResId)) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            // Контент выбранной вкладки (простая заглушка)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PaddingValues(
                        // Убираем лишний top padding, т.к. topBar = {}
                        start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                        end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                        bottom = innerPadding.calculateBottomPadding()
                    )),
                contentAlignment = Alignment.Center
            ) {
                when (selectedTab) {
                    BottomTab.FEED -> FeedScreen(navController = navController)
                    BottomTab.WORKOUTS -> MyWorkoutsScreen(navController = navController)
                    BottomTab.RECORD -> {
                        // Экран записи мы показываем отдельным слоем.
                    }
                    BottomTab.ANALYSIS -> AnalysisScreen()
                    BottomTab.PROFILE -> {
                        ProfileScreen(
                            navController = navController,
                            onShareProfileClick = {},
                            onLogoutClick = onLogoutClick,
                            onBackClick = {
                                navController.popBackStack()
                            },
                        )
                    }
                }
            }
        }
    }
}