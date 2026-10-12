package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class JeduScreen(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    TUTOR("AI Tutor", Icons.Default.Psychology),
    MIND_MAPS("Mind Maps", Icons.Default.AccountTree),
    TRANSLATE("Translate", Icons.Default.Translate),
    DOCUMENTS("E-Books", Icons.AutoMirrored.Filled.MenuBook),
    OFFLINE("Offline", Icons.Default.OfflinePin),
    VIDEOS("Videos", Icons.Default.SmartDisplay),
    QUIZZES("Quizzes", Icons.Default.AssignmentTurnedIn),
    LIBRARY("Library", Icons.Default.FolderSpecial),
    STAFF("Staff", Icons.Default.School),
    ADMIN("Admin", Icons.Default.AdminPanelSettings),
    PROFILE("Profile", Icons.Default.Person),
    ONBOARDING("Welcome", Icons.Default.Celebration),
    MIND_MAP_PROCESS("Generate Map", Icons.Default.UploadFile),
    MIND_MAP_VIEWER("Explore Map", Icons.Default.AccountTree)
}

@Composable
fun JeduBottomNavBar(
    currentScreen: JeduScreen,
    onScreenSelected: (JeduScreen) -> Unit
) {
    val navItems = listOf(
        JeduScreen.HOME,
        JeduScreen.TUTOR,
        JeduScreen.TRANSLATE,
        JeduScreen.DOCUMENTS,
        JeduScreen.PROFILE
    )

    NavigationBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        navItems.forEach { screen ->
            val isSelected = currentScreen == screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(screen) },
                icon = {
                    Icon(
                        screen.icon,
                        contentDescription = screen.title
                    )
                },
                label = { Text(screen.title) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
