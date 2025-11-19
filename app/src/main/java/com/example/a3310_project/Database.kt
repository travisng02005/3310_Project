package com.example.a3310_project

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext

data class TicketEntry(
    val id: Int,
    val userId: String,
    val name: String,
    val price: Float,
    val description: String? = null,
)
class DatabaseSchema(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    
    companion object {
        const val DATABASE_NAME = "example_app.db"
        const val DATABASE_VERSION = 1
        const val TICKETS_TABLE_NAME = "ticket_entries"
        const val PROFILES_TABLE_NAME = "account_entries"
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        // Create profile table
        val createProfileTableQuery = """
        CREATE TABLE $PROFILES_TABLE_NAME (
            username TEXT PRIMARY KEY,
            password TEXT NOT NULL,
            description TEXT
        )
    """
        db.execSQL(createProfileTableQuery)

        // Create tickets table with foreign key reference
        val createTicketTableQuery = """
        CREATE TABLE $TICKETS_TABLE_NAME (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            userId TEXT NOT NULL,
            name TEXT NOT NULL,
            price INTEGER NOT NULL,
            description TEXT,
            FOREIGN KEY (userId) REFERENCES $PROFILES_TABLE_NAME(username)
                ON DELETE CASCADE
                ON UPDATE CASCADE
        )
    """
        db.execSQL(createTicketTableQuery)
        
        // Insert sample data
        //insertSampleData(db)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop old tables and recreate
        db.execSQL("DROP TABLE IF EXISTS $PROFILES_TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $TICKETS_TABLE_NAME")
        onCreate(db)
    }

//    data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
//    private fun insertSampleData(db: SQLiteDatabase) {
//        val sampleEntries = listOf(
//            Quad("user123", "taylor swift", "100", "Seat 78259"),
//            Quad("user123", "sab carpenter", "200",  "Seat 5983"),
//            Quad("user123", "clairo", "400",  "Seat 09852"),
//            Quad("user123", "the marias", "300", "Seat 5346"),
//            Quad("user123", "iu", "500", "Seat 3523"),
//
//            Quad("user456", "kali uchis", "150", "Seat 583"),
//            Quad("user456", "kali uchis", "529", "Seat 451"),
//            Quad("user456", "taylor swift", "560", "Seat 3032"),
//
//            // Sample entries for user789
//            Quad("user789", "mariah carey", "420", "Seat 9520"),
//            Quad("user789", "laufey", "250", "Seat 593")
//        )
//
//        for ((userId, name, price, description) in sampleEntries) {
//            db.execSQL(
//                "INSERT INTO $TICKETS_TABLE_NAME (userId, name, price, description) VALUES (?, ?, ?, ?)",
//                arrayOf(userId, name, price, description)
//            )
//        }
//    }

}

// database helper functions
class DatabaseHelper(@Suppress("unused") private val context: Context) {
    
    private val dbHelper = DatabaseSchema(context)
    
    // Open the database (creates it if it doesn't exist)
    private fun getDatabase(): SQLiteDatabase {
        return dbHelper.writableDatabase
    }
    
    // Get all entries for a specific user
    fun getEntriesByUserId(userId: String): List<TicketEntry> {
        val db = getDatabase()
        val entries = mutableListOf<TicketEntry>()
        
        val cursor = db.rawQuery(
            "SELECT id, userId, name, price, description FROM ${DatabaseSchema.TICKETS_TABLE_NAME} WHERE userId = ?",
            arrayOf(userId)
        )
        
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val userIdCol = cursor.getString(cursor.getColumnIndexOrThrow("userId"))
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val price = cursor.getFloat(cursor.getColumnIndexOrThrow("price"))
            val description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
            
            entries.add(
                TicketEntry(
                    id = id,
                    userId = userIdCol,
                    name = name,
                    price = price,
                    description = description
                )
            )
        }
        
        cursor.close()
        db.close()
        
        return entries
    }
    
    // Update an entry
    fun updateEntry(entry: TicketEntry): Boolean {
        val db = getDatabase()
        
        return try {
            db.execSQL(
                "UPDATE ${DatabaseSchema.TICKETS_TABLE_NAME} SET name = ?, description = ? , price = ? WHERE id = ?",
                arrayOf(entry.name, entry.description, entry.price.toString(), entry.id.toString())
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }
    
    // Delete an entry
    fun deleteEntry(entryId: Int): Boolean {
        val db = getDatabase()
        
        return try {
            db.execSQL(
                "DELETE FROM ${DatabaseSchema.TICKETS_TABLE_NAME} WHERE id = ?",
                arrayOf(entryId)
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }
    
    // Insert new entry
    fun insertEntry(userId: String, name: String, price: String, description: String): Boolean {
        val db = getDatabase()
        
        return try {
            db.execSQL(
                "INSERT INTO ${DatabaseSchema.TICKETS_TABLE_NAME} (userId, name, price, description) VALUES (?, ?, ?, ?)",
                arrayOf(userId, name, price, description)
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }
    fun searchEntries(query: String): List<TicketEntry> {
        val entries = mutableListOf<TicketEntry>()
        val db = getDatabase()
        val cursor = db.rawQuery(
            "SELECT * FROM entries WHERE name LIKE ? ORDER BY id DESC",
            arrayOf("%$query%")
        )

        // Parse cursor into entries list
        cursor.use {
            while (it.moveToNext()) {
                // Add entries to list
            }
        }
        return entries
    }
}
