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
fun ListingsScreen(modifier: Modifier = Modifier, userId: String) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }

    var entries by remember { mutableStateOf<List<TicketEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var entryToEdit by remember { mutableStateOf<TicketEntry?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    LaunchedEffect(userId, searchQuery) {
        isLoading = true
        entries = withContext(Dispatchers.IO) {
            if (searchQuery.isEmpty()) {
                dbHelper.getEntriesByUserId(userId)
            } else {
                dbHelper.searchTicketEntries(userId, searchQuery)
            }
        }
        isLoading = false
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
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
                        EntryCard(
                            entry = entry,
                            onEditClick = { entryToEdit = entry }
                        )
                    }
                }
            }
        }

        // Dialogs
        entryToEdit?.let { entry ->
            EditDialog(
                entry = entry,
                onDismiss = { entryToEdit = null },
                onSave = { updatedEntry ->
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            dbHelper.updateTicket(updatedEntry)
                        }
                        entries = withContext(Dispatchers.IO) {
                            if (searchQuery.isEmpty()) {
                                dbHelper.getEntriesByUserId(userId)
                            } else {
                                dbHelper.searchTicketEntries(userId, searchQuery)
                            }
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
                            if (searchQuery.isEmpty()) {
                                dbHelper.getEntriesByUserId(userId)
                            } else {
                                dbHelper.searchTicketEntries(userId, searchQuery)
                            }
                        }
                        entryToEdit = null
                    }
                }
            )
        }

        if (showAddDialog) {
            AddEntryDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, price, description ->
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            dbHelper.insertTicket(userId, name, price.toString(), description)
                        }
                        searchQuery = ""
                        entries = withContext(Dispatchers.IO) {
                            dbHelper.getEntriesByUserId(userId)
                        }
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun EntryCard(
    entry: TicketEntry,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ticket ID: ${entry.id}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
                entry.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit"
                )
            }
        }
    }
}
@Composable
fun EditDialog(
    entry: TicketEntry,
    onDismiss: () -> Unit,
    onSave: (TicketEntry) -> Unit,
    onDelete: () -> Unit
) {
    var editedName by remember { mutableStateOf(entry.name) }
    var editedPrice by remember { mutableStateOf(entry.price.toString()) }
    var editedDescription by remember { mutableStateOf(entry.description ?: "") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Entry #${entry.id}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Artist") },
                    modifier = Modifier.fillMaxWidth()
                )

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
                    onSave(
                        entry.copy(
                            name = editedName,
                            price = editedPrice.toFloatOrNull() ?: 0f,
                            description = editedDescription,
                        )
                    )
                },
                enabled = editedName.isNotBlank() && editedPrice.toFloatOrNull() != null
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
@Composable
fun AddEntryDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Float, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter entry name") }
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            price = newValue
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
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter description, e.g. genre, location, section number.") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(name, price.toFloatOrNull() ?: 0f, description)
                },
                enabled = name.isNotBlank() && price.toFloatOrNull() != null
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

