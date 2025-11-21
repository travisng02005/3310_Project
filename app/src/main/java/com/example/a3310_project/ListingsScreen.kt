package com.example.a3310_project

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ListingsScreen(modifier: Modifier = Modifier, userId: String?) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val userPreferences = remember { UserPreferences(context) }

    var entries by remember { mutableStateOf<List<TicketEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var entryToEdit by remember { mutableStateOf<TicketEntry?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showLoginPrompt by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var currentUserId by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Check login status
    LaunchedEffect(Unit) {
        userPreferences.isLoggedInFlow.collect { loggedIn ->
            isLoggedIn = loggedIn
            if (loggedIn) {
                userPreferences.loggedInUserIdFlow.collect { id ->
                    currentUserId = id
                }
            }
        }
    }

    LaunchedEffect(currentUserId, searchQuery) {
        isLoading = true
        entries = withContext(Dispatchers.IO) {
            currentUserId?.let { uid ->
                if (searchQuery.isEmpty()) {
                    dbHelper.getEntriesByUserId(uid)
                } else {
                    dbHelper.searchTicketEntries(uid, searchQuery)
                }
            } ?: emptyList()
        }
        isLoading = false
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isLoggedIn) {
                        showAddDialog = true
                    } else {
                        showLoginPrompt = true
                    }
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Entry")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "My Tickets",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (!isLoggedIn) {
                // Login prompt for non-logged-in users
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Please log in to view your tickets")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Go to the Home screen to log in",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Search your listings") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (searchQuery.isEmpty()) {
                        "Total: ${entries.size}"
                    } else {
                        "Found: ${entries.size} results"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (entries.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (searchQuery.isEmpty()) {
                                Text("No listings found")
                                Text(
                                    text = "Click + to add your first listing",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            } else {
                                Text("No results for \"$searchQuery\"")
                                TextButton(onClick = { searchQuery = "" }) {
                                    Text("Clear search")
                                }
                            }
                        }
                    }
                } else {
                    // List of entries
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(entries, key = { it.id }) { entry ->
                            TicketCard(
                                ticket = entry,
                                onClick = { entryToEdit = entry }
                            )
                        }
                    }
                }
            }
        }

        // Login prompt dialog
        if (showLoginPrompt) {
            AlertDialog(
                onDismissRequest = { showLoginPrompt = false },
                title = { Text("Login Required") },
                text = { Text("You must be logged in to add listings. Please go to the Home screen to login.") },
                confirmButton = {
                    Button(onClick = { showLoginPrompt = false }) {
                        Text("OK")
                    }
                }
            )
        }

        // Dialogs
        entryToEdit?.let { entry ->
            EditDialog(
                entry = entry,
                dbHelper = dbHelper,
                onDismiss = { entryToEdit = null },
                onSave = { updatedEntry ->
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            dbHelper.updateTicket(updatedEntry)
                        }
                        entries = withContext(Dispatchers.IO) {
                            currentUserId?.let { uid ->
                                if (searchQuery.isEmpty()) {
                                    dbHelper.getEntriesByUserId(uid)
                                } else {
                                    dbHelper.searchTicketEntries(uid, searchQuery)
                                }
                            } ?: emptyList()
                        }
                        entryToEdit = null
                    }
                },
                onDelete = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            dbHelper.deleteTicket(entry.id)
                        }
                        entries = withContext(Dispatchers.IO) {
                            currentUserId?.let { uid ->
                                if (searchQuery.isEmpty()) {
                                    dbHelper.getEntriesByUserId(uid)
                                } else {
                                    dbHelper.searchTicketEntries(uid, searchQuery)
                                }
                            } ?: emptyList()
                        }
                        entryToEdit = null
                    }
                }
            )
        }

        if (showAddDialog && isLoggedIn) {
            AddEntryDialog(
                dbHelper = dbHelper,
                onDismiss = { showAddDialog = false },
                onAdd = { name, price, description ->
                    scope.launch {
                        currentUserId?.let { uid ->
                            withContext(Dispatchers.IO) {
                                dbHelper.insertTicket(uid, name, price.toString(), description)
                            }
                            searchQuery = ""
                            entries = withContext(Dispatchers.IO) {
                                dbHelper.getEntriesByUserId(uid)
                            }
                        }
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDialog(
    entry: TicketEntry,
    dbHelper: DatabaseHelper,
    onDismiss: () -> Unit,
    onSave: (TicketEntry) -> Unit,
    onDelete: () -> Unit
) {
    var editedName by remember { mutableStateOf(entry.name) }
    var editedPrice by remember { mutableStateOf(entry.price.toString()) }
    var editedDescription by remember { mutableStateOf(entry.description ?: "") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var eventError by remember { mutableStateOf("") }
    var availableEvents by remember { mutableStateOf<List<UpcomingShow>>(emptyList()) }
    var showEventDropdown by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Load available events
    LaunchedEffect(Unit) {
        availableEvents = withContext(Dispatchers.IO) {
            dbHelper.getAllUpcomingShows()
        }
    }

    // Validate event exists
    fun validateEvent(eventName: String): Boolean {
        val exists = availableEvents.any { it.event.equals(eventName, ignoreCase = true) }
        eventError = if (!exists) "Event must exist in upcoming shows" else ""
        return exists
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Entry #${entry.id}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Event name dropdown
                ExposedDropdownMenuBox(
                    expanded = showEventDropdown,
                    onExpandedChange = { showEventDropdown = it }
                ) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = {
                            editedName = it
                            validateEvent(it)
                        },
                        label = { Text("Event *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        isError = eventError.isNotEmpty(),
                        supportingText = {
                            if (eventError.isNotEmpty()) {
                                Text(eventError)
                            }
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showEventDropdown)
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = showEventDropdown,
                        onDismissRequest = { showEventDropdown = false }
                    ) {
                        availableEvents.forEach { show ->
                            DropdownMenuItem(
                                text = { Text("${show.event} (${show.date})") },
                                onClick = {
                                    editedName = show.event
                                    validateEvent(show.event)
                                    showEventDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = editedPrice,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            editedPrice = newValue
                        }
                        priceError = newValue.toFloatOrNull() == null && newValue.isNotEmpty()
                    },
                    label = { Text("Price") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = priceError,
                    supportingText = {
                        if (priceError) {
                            Text("Please enter a valid price")
                        }
                    },
                    prefix = { Text("$") }
                )

                OutlinedTextField(
                    value = editedDescription,
                    onValueChange = { editedDescription = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Listing")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateEvent(editedName)) {
                        onSave(
                            entry.copy(
                                name = editedName,
                                price = editedPrice.toFloatOrNull() ?: 0f,
                                description = editedDescription,
                            )
                        )
                    }
                },
                enabled = editedName.isNotBlank() && 
                         editedPrice.toFloatOrNull() != null && 
                         eventError.isEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete ticket?") },
            text = { Text("Are you sure you want to delete the listing for \"${entry.name}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryDialog(
    dbHelper: DatabaseHelper,
    onDismiss: () -> Unit,
    onAdd: (String, Float, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceError by remember { mutableStateOf(false) }
    var eventError by remember { mutableStateOf("") }
    var availableEvents by remember { mutableStateOf<List<UpcomingShow>>(emptyList()) }
    var showEventDropdown by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Load available events
    LaunchedEffect(Unit) {
        availableEvents = withContext(Dispatchers.IO) {
            dbHelper.getAllUpcomingShows()
        }
    }

    // Validate event exists
    fun validateEvent(eventName: String): Boolean {
        val exists = availableEvents.any { it.event.equals(eventName, ignoreCase = true) }
        eventError = if (!exists && eventName.isNotEmpty()) "Event must exist in upcoming shows" else ""
        return exists
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Listing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Select an event from the upcoming shows to list your ticket",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Event name dropdown
                ExposedDropdownMenuBox(
                    expanded = showEventDropdown,
                    onExpandedChange = { showEventDropdown = it }
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            validateEvent(it)
                        },
                        label = { Text("Event *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        placeholder = { Text("Select or type event name") },
                        isError = eventError.isNotEmpty(),
                        supportingText = {
                            if (eventError.isNotEmpty()) {
                                Text(eventError)
                            }
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showEventDropdown)
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = showEventDropdown,
                        onDismissRequest = { showEventDropdown = false }
                    ) {
                        if (availableEvents.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No upcoming shows available") },
                                onClick = { }
                            )
                        } else {
                            availableEvents.forEach { show ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(show.event)
                                            Text(
                                                "${show.date} at ${show.time}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        name = show.event
                                        validateEvent(show.event)
                                        showEventDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = price,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            price = newValue
                        }
                        priceError = newValue.toFloatOrNull() == null && newValue.isNotEmpty()
                    },
                    label = { Text("Price *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = priceError,
                    supportingText = {
                        if (priceError) {
                            Text("Please enter a valid price")
                        }
                    },
                    prefix = { Text("$") }
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter description, e.g. section number, seat details") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateEvent(name)) {
                        onAdd(name, price.toFloatOrNull() ?: 0f, description)
                    }
                },
                enabled = name.isNotBlank() && 
                         price.toFloatOrNull() != null && 
                         eventError.isEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
