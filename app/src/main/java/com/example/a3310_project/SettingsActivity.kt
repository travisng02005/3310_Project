package com.example.a3310_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.a3310_project.ui.theme._3310_ProjectTheme
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val userPreferences = remember { UserPreferences(context) }
            var darkMode by remember { mutableStateOf("system") }

            // Load saved dark mode preference
            LaunchedEffect(Unit) {
                userPreferences.darkModeFlow.collect { mode ->
                    darkMode = mode
                }
            }

            // Determine if dark theme should be used
            val isDarkTheme = when (darkMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            _3310_ProjectTheme(darkTheme = isDarkTheme) {
                SettingsScreen(
                    darkMode = darkMode,
                    onDarkModeChange = { newMode ->
                        darkMode = newMode
                    },
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    darkMode: String,
    onDarkModeChange: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var username by remember { mutableStateOf("") }
    var showThemeDialog by remember { mutableStateOf(false) }

    // Load username
    LaunchedEffect(Unit) {
        userPreferences.usernameFlow.collect { name ->
            username = name
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile picture (clickable to change)
            Image(
                painter = painterResource(id = R.drawable.profile_placeholder),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.2f))
                    .clickable {
                        // TODO: Add image picker logic here
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Username editor
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save button for username
            Button(
                onClick = {
                    coroutineScope.launch {
                        userPreferences.saveUsername(username)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Save Display Name")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Appearance Section
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Dark Mode Setting
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showThemeDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = when (darkMode) {
                                "dark" -> "Dark"
                                "light" -> "Light"
                                else -> "System default"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Account Section
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Logout option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        coroutineScope.launch {
                            userPreferences.clearLoggedInUser()
                        }
                        onBackClick()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Log out",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.selectableGroup()) {
                    ThemeOption(
                        text = "System default",
                        selected = darkMode == "system",
                        onClick = {
                            onDarkModeChange("system")
                            coroutineScope.launch {
                                userPreferences.saveDarkMode("system")
                            }
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        text = "Light",
                        selected = darkMode == "light",
                        onClick = {
                            onDarkModeChange("light")
                            coroutineScope.launch {
                                userPreferences.saveDarkMode("light")
                            }
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        text = "Dark",
                        selected = darkMode == "dark",
                        onClick = {
                            onDarkModeChange("dark")
                            coroutineScope.launch {
                                userPreferences.saveDarkMode("dark")
                            }
                            showThemeDialog = false
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ThemeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null // handled by selectable modifier
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text)
    }
}
