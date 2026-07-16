package com.fronterait.saludfamiliar.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fronterait.saludfamiliar.ui.screens.HomeScreen
import com.fronterait.saludfamiliar.ui.screens.PersonDetailScreen

@Composable
fun AppNavigation(viewModel: AppViewModel = viewModel()) {
    val navController = rememberNavController()

    // Transiciones consistentes de ida y vuelta: la pantalla nueva se desliza
    // desde la derecha con un fundido, en lugar del corte brusco por defecto.
    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(300)) { it / 4 } + fadeOut(tween(300)) }
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToPerson = { personId ->
                    navController.navigate("personDetail/$personId")
                }
            )
        }

        composable(
            route = "personDetail/{personId}",
            arguments = listOf(navArgument("personId") { type = NavType.LongType })
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getLong("personId") ?: 0L
            PersonDetailScreen(
                personId = personId,
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
