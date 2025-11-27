package com.example.a3310_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.a3310_project.ui.theme._3310_ProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _3310_ProjectTheme {
                // ✅ Lifted login state
                var isLoggedIn by rememberSaveable { mutableStateOf(false) }
                var currentUser by rememberSaveable { mutableStateOf<Profile?>(null) }

                _3310_ProjectApp(
                    isLoggedIn = isLoggedIn,
                    currentUser = currentUser,
                    onLogin = { user ->
                        currentUser = user
                        isLoggedIn = true
                    },
                    onLogout = {
                        currentUser = null
                        isLoggedIn = false
                    }
                )
            }
        }
    }
}

@Composable
fun _3310_ProjectApp(
    modifier: Modifier = Modifier,
    isLoggedIn: Boolean,
    currentUser: Profile?,
    onLogin: (Profile) -> Unit,
    onLogout: () -> Unit
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val userPreferences = remember { UserPreferences(context) }

    // 🔥 Load login state from DataStore only once
    LaunchedEffect(Unit) {
        userPreferences.loggedInUserIdFlow.collect { userId ->
            if (userId != null) {
                val profile = dbHelper.getProfile(userId)
                if (profile != null) {
                    onLogin(profile)
                }
            } else {
                onLogout()
            }
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> MainScreen(
                    modifier = Modifier.padding(innerPadding),
                    isLoggedIn = isLoggedIn,
                    currentUser = currentUser,
                    onLogin = onLogin,
                    onLogout = onLogout
                )

                AppDestinations.LISTINGS -> ListingsScreen(
                    modifier = Modifier.padding(innerPadding),
                    userId = currentUser?.userId ?: "jessicanguyen"
                )

                AppDestinations.PROFILE -> ProfileScreen(
                    modifier = Modifier.padding(innerPadding),
                    isLoggedIn = isLoggedIn
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    LISTINGS("My Tickets", Icons.Default.Menu),
    PROFILE("Profile", Icons.Default.AccountBox),
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    _3310_ProjectTheme {
        MainScreen(
            isLoggedIn = false,
            currentUser = null,
            onLogin = {},
            onLogout = {}
        )
    }
}
