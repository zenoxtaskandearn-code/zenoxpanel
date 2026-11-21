package com.zenox.remotegallery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zenox.remotegallery.core.AppContainer
import com.zenox.remotegallery.ui.navigation.Routes
import com.zenox.remotegallery.ui.screens.connection.ConnectionScreen
import com.zenox.remotegallery.ui.screens.gallery.GalleryScreen
import com.zenox.remotegallery.ui.screens.home.HomeScreen
import com.zenox.remotegallery.ui.screens.onboarding.AccountScreen
import com.zenox.remotegallery.ui.screens.onboarding.ConsentScreen
import com.zenox.remotegallery.ui.screens.onboarding.PermissionsScreen
import com.zenox.remotegallery.ui.screens.onboarding.WelcomeScreen
import com.zenox.remotegallery.ui.screens.remoteaccess.RemoteAccessScreen
import com.zenox.remotegallery.ui.screens.settings.SettingsScreen
import com.zenox.remotegallery.util.PermissionUtils

@Composable
fun RemoteGalleryRoot(appContainer: AppContainer) {
    val settings by appContainer.settingsRepository.settingsFlow.collectAsStateWithLifecycle(initialValue = null)
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    PendingMediaActionHandler(appContainer)

    if (settings == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val s = settings!!
    val hasMediaPerm = PermissionUtils.hasAnyMediaAccess(context)
    val needsConsent = !s.remoteAccessConsentGranted
    val needsRegistration = !s.registered

    val start = when {
        !s.onboardingCompleted -> Routes.Welcome
        !hasMediaPerm -> Routes.Permissions
        needsRegistration -> Routes.Account
        needsConsent -> Routes.Consent
        else -> Routes.Main
    }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.Welcome) {
            WelcomeScreen(onContinue = {
                navController.navigate(Routes.Permissions) { popUpTo(Routes.Welcome) { inclusive = true } }
            }, onSetExplained = { /* handled in screen */ }, settingsRepository = appContainer.settingsRepository)
        }
        composable(Routes.Permissions) {
            PermissionsScreen(
                onContinue = {
                    val next = if (!s.registered) Routes.Account else if (!s.remoteAccessConsentGranted) Routes.Consent else Routes.Main
                    navController.navigate(next) { popUpTo(Routes.Permissions) { inclusive = true } }
                }
            )
        }
        composable(Routes.Account) {
            AccountScreen(appContainer = appContainer, onPaired = {
                navController.navigate(Routes.Consent) { popUpTo(Routes.Account) { inclusive = true } }
            }, onBack = { navController.popBackStack() })
        }
        composable(Routes.Consent) {
            ConsentScreen(appContainer = appContainer, onAccepted = {
                navController.navigate(Routes.Main) { popUpTo(0) { inclusive = true } }
            })
        }
        composable(Routes.Main) {
            MainScaffold(appContainer = appContainer, onRequireOnboarding = { route ->
                navController.navigate(route) { popUpTo(0) { inclusive = true } }
            })
        }
    }
}

@Composable
fun MainScaffold(appContainer: AppContainer, onRequireOnboarding: (String) -> Unit) {
    val nav = rememberNavController()
    val items = listOf(
        Triple(Routes.Home, "Home", Icons.Default.Home),
        Triple(Routes.Gallery, "Gallery", Icons.Default.PhotoLibrary),
        Triple(Routes.Connection, "Device", Icons.Default.Link),
        Triple(Routes.RemoteAccess, "Remote", Icons.Default.Security),
        Triple(Routes.Settings, "Settings", Icons.Default.Settings),
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStack by nav.currentBackStackEntryAsState()
                val current = backStack?.destination?.route
                items.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = current == route,
                        onClick = {
                            nav.navigate(route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = nav, startDestination = Routes.Home) {
                composable(Routes.Home) { HomeScreen(appContainer, onNavigate = { nav.navigate(it) }) }
                composable(Routes.Gallery) { GalleryScreen(appContainer) }
                composable(Routes.Connection) { ConnectionScreen(appContainer) }
                composable(Routes.RemoteAccess) { RemoteAccessScreen(appContainer) }
                composable(Routes.Settings) { SettingsScreen(appContainer, onRequireOnboarding = onRequireOnboarding) }
            }
        }
    }
}
