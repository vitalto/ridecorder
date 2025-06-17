package ru.ridecorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import ru.ridecorder.ui.auth.LoginScreen
import ru.ridecorder.ui.auth.RegistrationScreen
import ru.ridecorder.ui.theme.RidecorderTheme
import ru.ridecorder.ui.auth.StartScreen
import ru.ridecorder.ui.common.LoadingScreen
import ru.ridecorder.ui.helpers.requestNotificationPermissionIfNeeded
import ru.ridecorder.ui.main.MainScreen
import ru.ridecorder.ui.profile.ProfileEditScreen
import ru.ridecorder.ui.profile.ProfileScreen
import ru.ridecorder.ui.profile.SearchFriendsScreen
import ru.ridecorder.ui.profile.UserListScreen
import ru.ridecorder.ui.workouts.DetailedWorkoutScreen
import ru.ridecorder.ui.recording.RecordingSaveScreen
import ru.ridecorder.ui.recording.RecordingScreen
import ru.ridecorder.ui.recording.RecordingSettingsScreen
import ru.ridecorder.ui.workouts.ChooseWorkoutScreen
import ru.ridecorder.ui.workouts.CompareWorkoutsScreen
import ru.ridecorder.ui.workouts.WorkoutEditScreen


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            RidecorderTheme(dynamicColor = false) {
                val navController = rememberNavController()
                val mainViewModel: MainActivityViewModel = hiltViewModel()

                val token by mainViewModel.token.collectAsState()
                val isLoading by mainViewModel.isLoading.collectAsState()

                if (isLoading) {
                    LoadingScreen()
                    return@RidecorderTheme
                }

                val startDestination = if (token.isNullOrEmpty()) {
                    NavRoutes.Start.route
                } else {
                    NavRoutes.Main.route
                }
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                ) {
                    composable(NavRoutes.Start.route) {
                        requestNotificationPermissionIfNeeded(this@MainActivity)
                        StartScreen(
                            onJoinClick = {
                                navController.navigate(NavRoutes.Registration.route)
                            },
                            onLoginClick = {
                                navController.navigate(NavRoutes.Login.route)
                            }
                        )
                    }
                    composable(NavRoutes.Registration.route) {
                        RegistrationScreen(
                            onBackClick = {
                                goBack(navController)
                            },
                            onSuccess = {
                                initMainScreen(navController);
                            }
                        )
                    }
                    composable(NavRoutes.Login.route) {
                        LoginScreen(
                            onBackClick = {
                                goBack(navController)
                            },
                            onSuccess = {
                                initMainScreen(navController);
                            }
                        )
                    }
                    composable(NavRoutes.Recording.route,
                        enterTransition = {
                            slideInVertically(
                                initialOffsetY = { fullHeight -> fullHeight },
                                animationSpec = tween(durationMillis = 300)
                            )
                        },
                        exitTransition = {
                            slideOutVertically(
                                targetOffsetY = { fullHeight -> fullHeight },
                                animationSpec = tween(durationMillis = 300)
                            )
                        }) {
                        RecordingScreen(
                            onStopClick = { workoutId ->
                                navController.navigate(
                                    NavRoutes.RecordingSave.createRoute(
                                        workoutId
                                    )
                                )
                            },
                            onExitClick = {
                                navController.popBackStack(
                                    NavRoutes.Main.route,
                                    inclusive = false
                                )
                            },
                            onSettingsClick = {
                                navController.navigate(NavRoutes.RecordingSettings.route)
                            })
                    }
                    composable(NavRoutes.RecordingSettings.route){
                        RecordingSettingsScreen(onBackClick = {
                            navController.popBackStack(
                                NavRoutes.Recording.route,
                                inclusive = false
                            )
                        })
                    }
                    composable(NavRoutes.RecordingSave.route) {
                        val workoutId = navController.currentBackStackEntry
                            ?.arguments
                            ?.getString("workoutId")
                            ?.toLongOrNull()

                        if (workoutId != null) {
                            RecordingSaveScreen(
                                workoutId = workoutId,
                                onDeleteClick = {
                                    // Переход обратно на основной экран записи
                                    navController.navigate(NavRoutes.Recording.route) {
                                        popUpTo(NavRoutes.RecordingSave.createRoute(workoutId)) {
                                            inclusive = true
                                        }
                                    }
                                },
                                onBackClick = {
                                    navController.popBackStack(
                                        NavRoutes.Recording.route,
                                        inclusive = false
                                    )
                                },
                                onSaveClick = {
                                    navController.popBackStack(
                                        NavRoutes.Main.route,
                                        inclusive = false
                                    )
                                }
                            )
                        }
                    }
                    composable(NavRoutes.Main.route) {
                        MainScreen(
                            navController,
                            onRecordClick = {
                                navController.navigate(NavRoutes.Recording.route)
                            },
                            onLogoutClick = {
                                mainViewModel.clearLocalData()
                                navController.navigate(NavRoutes.Start.route) {
                                    popUpTo(0) // reset stack
                                }
                            })
                    }
                    composable(NavRoutes.Workout.route, arguments = listOf(
                        navArgument("workoutId") {
                            nullable = true
                            type = NavType.StringType
                            defaultValue = null
                        },
                        navArgument("serverWorkoutId") {
                            nullable = true
                            type = NavType.StringType
                            defaultValue = null
                        }
                    )) { backStackEntry ->
                        val serverWorkoutId = backStackEntry.arguments?.getString("serverWorkoutId")?.toLongOrNull()
                        val workoutId = backStackEntry.arguments?.getString("workoutId")?.toLongOrNull()

                        val isUserWorkout = serverWorkoutId == null
                        val id = serverWorkoutId ?: workoutId

                        id?.let {
                            DetailedWorkoutScreen(
                                workoutId = it,
                                isUserWorkout = isUserWorkout,
                                onBack = { goBack(navController) },
                                onEditWorkout = { editWorkoutId ->
                                    if(isUserWorkout)
                                        navController.navigate(NavRoutes.WorkoutEdit.createRoute(editWorkoutId))
                                },
                                onCompare = {
                                    navController.navigate(
                                        NavRoutes.ChooseWorkout.createRoute(
                                            workoutId = if (isUserWorkout) it else null,
                                            serverWorkoutId = if (isUserWorkout) null else it.toInt()
                                        )
                                    )
                                }
                            )
                        }
                    }


                    composable(NavRoutes.WorkoutEdit.route) { backStackEntry ->
                        val workoutId =
                            backStackEntry.arguments?.getString("workoutId")?.toLongOrNull()
                        workoutId?.let {
                            WorkoutEditScreen(workoutId = workoutId,
                                onBack = {
                                    goBack(navController)
                                },
                                onDelete = {
                                    navController.popBackStack(NavRoutes.Main.route, false)
                                })
                        }
                    }
                    composable(NavRoutes.ProfileEdit.route) {
                        ProfileEditScreen(onBack = { isNeedReload ->
                            if(isNeedReload){
                                navController.previousBackStackEntry?.savedStateHandle?.set("reload", true)
                            }
                            goBack(navController)
                        })
                    }

                    composable(NavRoutes.UserList.route, arguments = listOf(
                        navArgument("userId") {
                            nullable = true  // Разрешаем null
                            type = NavType.StringType
                            defaultValue = null
                        },
                        navArgument("showFollowing") {
                            type = NavType.BoolType
                        }
                    )){ backStackEntry ->
                        val userIdArg = backStackEntry.arguments?.getString("userId")
                        val userId = userIdArg?.toIntOrNull()  // Преобразуем в Int, если не "null"
                        val showFollowing = backStackEntry.arguments?.getBoolean("showFollowing") ?: false
                        UserListScreen(
                            userId = userId,
                            showFollowing = showFollowing,
                            onBackClick = {
                                goBack(navController)
                            },
                            onUserClick = {
                                navController.navigate(NavRoutes.Profile.createRoute(it.id))
                            }
                        )
                    }

                    composable(NavRoutes.SearchFriends.route) {
                        SearchFriendsScreen(
                            onBackClick = {
                                navController.previousBackStackEntry?.savedStateHandle?.set("reload", true)
                                goBack(navController)
                            },
                            onUserClick = {
                                navController.navigate(NavRoutes.Profile.createRoute(it.id))
                            })
                    }

                    composable(NavRoutes.Profile.route, arguments = listOf(
                        navArgument("userId") {
                            nullable = true  // Разрешаем null
                            type = NavType.StringType
                            defaultValue = null
                        }
                    )){ backStackEntry ->
                        val userIdArg = backStackEntry.arguments?.getString("userId")
                        val userId = userIdArg?.toIntOrNull()  // Преобразуем в Int, если не "null"
                        ProfileScreen(
                            userId = userId,
                            navController = navController,
                            onLogoutClick = {},
                            onShareProfileClick = {},
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(NavRoutes.ChooseWorkout.route, arguments = listOf(
                        navArgument("workoutId") {
                            nullable = true
                            type = NavType.StringType
                            defaultValue = null
                        },
                        navArgument("serverWorkoutId") {
                            nullable = true
                            type = NavType.StringType
                            defaultValue = null
                        }
                    )) { backStackEntry ->
                        val serverWorkoutId = backStackEntry.arguments?.getString("serverWorkoutId")?.toLongOrNull()
                        val workoutId = backStackEntry.arguments?.getString("workoutId")?.toLongOrNull()

                        if (serverWorkoutId != null || workoutId != null) {
                            ChooseWorkoutScreen(
                                onBack = { goBack(navController) },
                                onWorkoutSelected = { selectedWorkoutId ->
                                    navController.navigate(
                                        NavRoutes.CompareWorkouts.createRoute(
                                            firstWorkoutId = workoutId,
                                            firstServerWorkoutId = serverWorkoutId?.toInt(),
                                            secondWorkoutId = selectedWorkoutId,
                                            secondServerWorkoutId = null
                                        )
                                    )
                                }
                            )
                        }
                    }

                    composable(NavRoutes.CompareWorkouts.route, arguments = listOf(
                        navArgument("firstWorkoutId") {
                            nullable = true
                            type = NavType.StringType
                            defaultValue = null
                        },
                        navArgument("firstServerWorkoutId") {
                            nullable = true
                            type = NavType.StringType
                            defaultValue = null
                        },
                        navArgument("secondWorkoutId") {
                            nullable = true
                            type = NavType.StringType
                            defaultValue = null
                        },
                        navArgument("secondServerWorkoutId") {
                            nullable = true
                            type = NavType.StringType
                            defaultValue = null
                        }
                    )) { backStackEntry ->
                        val firstServerWorkoutId = backStackEntry.arguments?.getString("firstServerWorkoutId")?.toLongOrNull()
                        val firstWorkoutId = backStackEntry.arguments?.getString("firstWorkoutId")?.toLongOrNull()
                        val secondServerWorkoutId = backStackEntry.arguments?.getString("secondServerWorkoutId")?.toLongOrNull()
                        val secondWorkoutId = backStackEntry.arguments?.getString("secondWorkoutId")?.toLongOrNull()

                        // Проверяем, что оба идентификатора не null
                        if ((firstServerWorkoutId != null || firstWorkoutId != null) &&
                            (secondServerWorkoutId != null || secondWorkoutId != null)) {

                            CompareWorkoutsScreen(
                                firstWorkoutId = firstServerWorkoutId ?: firstWorkoutId!!,
                                secondWorkoutId = secondServerWorkoutId ?: secondWorkoutId!!,
                                firstIsUserWorkout = firstServerWorkoutId == null,
                                secondIsUserWorkout = secondServerWorkoutId == null,
                                onBack = { goBack(navController) }
                            )
                        }
                    }
                }

            }
        }
    }

    private fun goBack(navController: NavController){
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }
    private fun initMainScreen(navController: NavController){
        navController.navigate(NavRoutes.Main.route) {
            popUpTo(0) // reset stack
        }
    }
}



sealed class NavRoutes(val route: String) {
    data object Start : NavRoutes("start_screen")
    data object Registration : NavRoutes("registration_screen")
    data object Login : NavRoutes("login_screen")
    data object Recording : NavRoutes("recording_screen")
    data object RecordingSave : NavRoutes("recording_save_screen/{workoutId}") {
        fun createRoute(workoutId: Long) = "recording_save_screen/$workoutId"
    }
    data object RecordingSettings : NavRoutes("recording_settings_screen")

    data object Workout : NavRoutes("workout_screen?id={workoutId}&serverId={serverWorkoutId}") {
        fun createRoute(workoutId: Long?, serverWorkoutId: Int?): String{
            val workoutIdParam = workoutId?.toString() ?: "null"
            val serverWorkoutIdParam = serverWorkoutId?.toString() ?: "null"
            return "workout_screen?id=$workoutIdParam&serverId=$serverWorkoutIdParam"
        }
    }
    data object WorkoutEdit : NavRoutes("workout_edit_screen/{workoutId}") {
        fun createRoute(workoutId: Long) = "workout_edit_screen/$workoutId"
    }
    data object ChooseWorkout : NavRoutes("choose_workout_screen?id={workoutId}&serverId={serverWorkoutId}") {
        fun createRoute(workoutId: Long?, serverWorkoutId: Int?): String{
            val workoutIdParam = workoutId?.toString() ?: "null"
            val serverWorkoutIdParam = serverWorkoutId?.toString() ?: "null"
            return "choose_workout_screen?id=$workoutIdParam&serverId=$serverWorkoutIdParam"
        }
    }
    data object CompareWorkouts : NavRoutes("compare_workouts_screen?firstId={firstWorkoutId}&firstServerId={firstServerWorkoutId}&secondId={secondWorkoutId}&secondServerId={secondServerWorkoutId}") {
        fun createRoute(firstWorkoutId: Long?, firstServerWorkoutId: Int?, secondWorkoutId: Long?, secondServerWorkoutId: Int?): String{
            val firstWorkoutIdParam = firstWorkoutId?.toString() ?: "null"
            val firstServerWorkoutIdParam = firstServerWorkoutId?.toString() ?: "null"
            val secondWorkoutIdParam = secondWorkoutId?.toString() ?: "null"
            val secondServerWorkoutIdParam = secondServerWorkoutId?.toString() ?: "null"
            return "compare_workouts_screen?firstId=$firstWorkoutIdParam&firstServerId=$firstServerWorkoutIdParam&secondId=$secondWorkoutIdParam&secondServerId=$secondServerWorkoutIdParam"
        }
    }
    data object ProfileEdit : NavRoutes("profile_edit_screen")
    data object UserList : NavRoutes("user_list_screen?userId={userId}&showFollowing={showFollowing}"){
        fun createRoute(userId: Int?, showFollowing: Boolean): String {
            val userIdParam = userId?.toString() ?: "null"
            return "user_list_screen?userId=$userIdParam&showFollowing=$showFollowing"
        }
    }
    data object Profile : NavRoutes("profile_screen?userId={userId}"){
        fun createRoute(userId: Int?): String {
            val userIdParam = userId?.toString() ?: "null"
            return "profile_screen?userId=$userIdParam"
        }
    }
    data object SearchFriends : NavRoutes("search_friends_screen")

    data object Main: NavRoutes("main_screen")

}