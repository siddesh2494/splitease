package com.splitease.app.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.splitease.app.ui.group.GroupDashboardScreen
import com.splitease.app.ui.home.HomeScreen
import com.splitease.app.ui.home.HomeViewModel
import com.splitease.app.ui.settle.SettleUpScreen
import kotlinx.serialization.Serializable

// Type-safe routes (Navigation 2.8+): arguments are plain constructor params.
@Serializable object HomeRoute
@Serializable data class GroupRoute(val groupId: String)
@Serializable data class SettleRoute(val groupId: String)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavHost(initialJoinCode: String?) {
    val nav = rememberNavController()
    val homeVm: HomeViewModel = hiltViewModel()
    var handledCode by rememberSaveable { mutableStateOf(false) }

    // Deep link: splitease://join/ABC123 -> open the group if it exists locally.
    LaunchedEffect(initialJoinCode) {
        if (initialJoinCode != null && !handledCode) {
            handledCode = true
            homeVm.join(initialJoinCode)?.let { nav.navigate(GroupRoute(it)) }
        }
    }

    SharedTransitionLayout {
        // NavHost animates transitions and supports predictive back out of the box
        // (android:enableOnBackInvokedCallback="true" in the manifest).
        NavHost(navController = nav, startDestination = HomeRoute) {
            composable<HomeRoute> {
                HomeScreen(
                    viewModel = homeVm,
                    onOpenGroup = { nav.navigate(GroupRoute(it)) },
                    sharedScope = this@SharedTransitionLayout,
                    animatedScope = this,
                )
            }
            composable<GroupRoute> { entry ->
                val route = entry.toRoute<GroupRoute>()
                GroupDashboardScreen(
                    onBack = { nav.popBackStack() },
                    onSettle = { nav.navigate(SettleRoute(route.groupId)) },
                    sharedScope = this@SharedTransitionLayout,
                    animatedScope = this,
                )
            }
            composable<SettleRoute> {
                SettleUpScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}
