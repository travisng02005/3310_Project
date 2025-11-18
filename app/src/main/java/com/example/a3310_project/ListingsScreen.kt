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
import androidx.compose.ui.platform.LocalContext
import android.database.sqlite.SQLiteDatabase
import android.content.Context
import android.database.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UserEntry(
    val id: Int,
    val userId: String,
    val name: String,
    val description: String? = null
)
class DatabaseHelper(private val context: Context) {

    // Open your existing database
    private fun getDatabase(): SQLiteDatabase {
        // Replace with your actual database path/name
        val dbPath = context.getDatabasePath("example_db.db").path
        return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
    }

    // Get all entries for a specific user
    fun getEntriesByUserId(userId: String): List<UserEntry> {
        val db = getDatabase()
        val entries = mutableListOf<UserEntry>()

        // Direct SQL query
        val cursor: Cursor = db.rawQuery(
            "SELECT id, userId, name, description FROM example_table WHERE userId = ?",
            arrayOf(userId)
        )

        // Parse each row
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndex("id"))
            val userIdCol = cursor.getString(cursor.getColumnIndex("userId"))
            val name = cursor.getString(cursor.getColumnIndex("name"))
            val description = cursor.getString(cursor.getColumnIndex("description"))

            entries.add(
                UserEntry(
                    id = id,
                    userId = userIdCol,
                    name = name,
                    description = description
                )
            )
        }

        cursor.close()
        db.close()

        return entries
    }

    // Update an entry
    fun updateEntry(entry: UserEntry): Boolean {
        val db = getDatabase()

        val updateQuery = """
            UPDATE example_table
            SET name = ?, description = ?
            WHERE id = ?
        """

        return try {
            db.execSQL(updateQuery, arrayOf(entry.name, entry.description, entry.id))
            db.close()
            true
        } catch (e: Exception) {
            db.close()
            false
        }
    }

    // Delete an entry
    fun deleteEntry(entryId: Int): Boolean {
        val db = getDatabase()

        return try {
            db.execSQL("DELETE FROM example_table WHERE id = ?", arrayOf(entryId))
            db.close()
            true
        } catch (e: Exception) {
            db.close()
            false
        }
    }

    // Insert new entry
    fun insertEntry(userId: String, name: String, description: String): Boolean {
        val db = getDatabase()

        return try {
            db.execSQL(
                "INSERT INTO example_table (userId, name, description) VALUES (?, ?, ?)",
                arrayOf(userId, name, description)
            )
            db.close()
            true
        } catch (e: Exception) {
            db.close()
            false
        }
    }
}
@Composable
fun ListingsScreen(modifier: Modifier = Modifier, userId: String) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }

    var entries by remember { mutableStateOf<List<UserEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var entryToEdit by remember { mutableStateOf<UserEntry?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        scope.launch {
            isLoading = true
            entries = withContext(Dispatchers.IO) {
                dbHelper.getEntriesByUserId(userId)
            }
            isLoading = false
        }
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
        Box(modifier = modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "My Entries",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp)
                )

                Text(
                    text = "Total: ${entries.size}",
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No entries found")
                            Text(
                                text = "Click + to add your first entry",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                } else {
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

            entryToEdit?.let { entry ->
                EditDialog(
                    entry = entry,
                    onDismiss = { entryToEdit = null },
                    onSave = { updatedEntry ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                dbHelper.updateEntry(updatedEntry)
                            }
                            entries = withContext(Dispatchers.IO) {
                                dbHelper.getEntriesByUserId(userId)
                            }
                            entryToEdit = null
                        }
                    },
                    onDelete = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                dbHelper.deleteEntry(entry.id)
                            }
                            entries = withContext(Dispatchers.IO) {
                                dbHelper.getEntriesByUserId(userId)
                            }
                            entryToEdit = null
                        }
                    }
                )
            }

            if (showAddDialog) {
                AddEntryDialog(
                    onDismiss = { showAddDialog = false },
                    onAdd = { name, description ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                dbHelper.insertEntry(userId, name, description)
                            }
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
}
@Composable
fun EntryCard(
    entry: UserEntry,
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
                    text = "ID: ${entry.id}",
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
    entry: UserEntry,
    onDismiss: () -> Unit,
    onSave: (UserEntry) -> Unit,
    onDelete: () -> Unit  // ← Add delete callback
) {
    var editedName by remember { mutableStateOf(entry.name) }
    var editedDescription by remember { mutableStateOf(entry.description ?: "") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Entry #${entry.id}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editedDescription,
                    onValueChange = { editedDescription = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Delete button at bottom of dialog
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
                            description = editedDescription
                        )
                    )
                },
                enabled = editedName.isNotBlank()
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
            title = { Text("Delete Entry?") },
            text = { Text("Are you sure you want to delete \"${entry.name}\"? This action cannot be undone.") },
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
    onAdd: (String, String) -> Unit  // name, description
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

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
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter description") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(name, description)
                },
                enabled = name.isNotBlank()
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