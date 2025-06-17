package ru.ridecorder.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import ru.ridecorder.NavRoutes
import ru.ridecorder.R
import ru.ridecorder.config.AppConfig
import ru.ridecorder.di.ContextResourceProvider
import ru.ridecorder.ui.common.FeedWorkoutItem
import ru.ridecorder.ui.common.LoadingScreen
import ru.ridecorder.ui.common.NoInternetScreen
import ru.ridecorder.ui.helpers.ConvertHelper

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    navController: NavController,
    userId: Int? = null,
    onShareProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()
    val navBackStackEntry = remember { navController.currentBackStackEntry }

    LaunchedEffect(Unit) {
        if (uiState.isLoading) {
            viewModel.loadUserProfile(userId)
            viewModel.loadUserWorkouts(userId)
        }
    }

    LaunchedEffect(navBackStackEntry) {
        val reload = navBackStackEntry?.savedStateHandle?.remove<Boolean>("reload")
        if (reload == true) {
            viewModel.loadUserProfile(userId)
        }
    }

    if (uiState.isError) {
        NoInternetScreen(onRetry = { viewModel.loadUserProfile(userId) })
        return
    } else if (uiState.isLoading) {
        LoadingScreen()
        return
    }

    Scaffold(
        topBar = {
            TopAppBarWithActions(
                uiState,
                onEditProfileClick = {
                    navController.navigate(NavRoutes.ProfileEdit.route)
                },
                onLogoutClick = onLogoutClick,
                userId = userId,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Аватар
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if(uiState.avatarUrl == null) {
                        AsyncImage(
                            model = "${AppConfig.serverUrl}/avatars/placeholder.jpg",
                            contentDescription = stringResource(id = R.string.avatar),
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        AsyncImage(
                            model = uiState.avatarUrl,
                            contentDescription = stringResource(id = R.string.avatar),
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Имя пользователя
            item {
                Text(
                    text = uiState.userName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (uiState.userLogin.isNotBlank()) {
                item {
                    Text(
                        text = "@" + uiState.userLogin,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Дата регистрации
            item {
                Text(
                    text = stringResource(id = R.string.registration_date, uiState.registrationDate),
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Подписчики и подписки
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = stringResource(id = R.string.followers, uiState.followerCount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            navController.navigate(
                                NavRoutes.UserList.createRoute(userId = userId, showFollowing = false)
                            )
                        },
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(id = R.string.following, uiState.followingCount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            navController.navigate(
                                NavRoutes.UserList.createRoute(userId = userId, showFollowing = true)
                            )
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Кнопки действий (Добавить друзей / Подписаться, Поделиться)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (userId == null) {
                        Button(
                            onClick = { navController.navigate(NavRoutes.SearchFriends.route) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = stringResource(id = R.string.friends_add))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(id = R.string.friends_add))
                        }
                    } else {
                        if (uiState.isFollowing) {
                            Button(
                                onClick = { viewModel.unsubscribeFromUser(userId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.PersonRemove, contentDescription = stringResource(id = R.string.unsubscribe))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(id = R.string.unsubscribe))
                            }
                        } else {
                            Button(
                                onClick = { viewModel.subscribeToUser(userId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = stringResource(id = R.string.subscribe))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(id = R.string.subscribe))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onShareProfileClick,
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = stringResource(id = R.string.share))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.share))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { HorizontalDivider() }
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Обзор и статистика
            item {
                Text(
                    text = stringResource(id = R.string.overview),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatCard(stringResource(id = R.string.stat_trainings), "${uiState.totalTrainings}", Icons.Filled.FitnessCenter)
                        StatCard(stringResource(id = R.string.stat_kilometers), "${uiState.totalDistance} ${stringResource(id = R.string.unit_kilometers)}", Icons.AutoMirrored.Filled.DirectionsBike)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatCard(stringResource(id = R.string.stat_time), uiState.totalTime, Icons.Default.Schedule)
                        StatCard(stringResource(id = R.string.stat_avg_speed), ConvertHelper.formatSpeed(
                            ContextResourceProvider(context), uiState.averageSpeed), Icons.Filled.Speed)
                    }
                }
            }

            // Список тренировок (только если userId != null)
            if (userId != null) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { HorizontalDivider() }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item {
                    Text(
                        text = stringResource(id = R.string.workouts_section),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
                items(uiState.workouts) { workout ->
                    FeedWorkoutItem(
                        workout = workout,
                        onLikeClick = { viewModel.toggleLike(workout) },
                        onItemClick = { navController.navigate(NavRoutes.Workout.createRoute(null, workout.id)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBarWithActions(
    profileUiState: MyProfileUiState,
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    userId: Int? = null,
    onBackClick: () -> Unit
) {
    val headerText = if (userId == null) {
        stringResource(id = R.string.profile_screen_title)
    } else {
        // Собираем заголовок для чужого профиля – например, "Профиль @username" или "Профиль #id"
        if (profileUiState.userLogin.isNotBlank())
            stringResource(id = R.string.profile_title_prefix, "@" + profileUiState.userLogin)
        else if (profileUiState.userName.isNotBlank())
            stringResource(id = R.string.profile_title_prefix, profileUiState.userName)
        else
            stringResource(id = R.string.profile_title_prefix, "#$userId")
    }

    TopAppBar(
        title = {
            Text(text = headerText)
        },
        actions = {
            if(userId == null){
                IconButton(onClick = onEditProfileClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(id = R.string.edit_profile)
                    )
                }
                IconButton(onClick = onLogoutClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = stringResource(id = R.string.logout)
                    )
                }
            }
            else {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(id = R.string.more)
                    )
                }
            }
        },
        navigationIcon = {
            if(userId != null){
                IconButton(onClick = {onBackClick()}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                }
            }
        }
    )
}

/**
 * Пример карточки с иконкой, заголовком и значением.
 */
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .width(150.dp)
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )

            // Заголовок
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.Gray
            )
            // Значение
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
