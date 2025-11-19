package com.example.a3310_project

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class TicketEntry(
    val id: Int,
    val userId: String,
    val name: String,
    val price: Float,
    val description: String? = null,
)
data class Profile(
    val name: String,
    val userId: String,
    val password: String
)
class DatabaseSchema(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        const val DATABASE_NAME = "example_app.db"
        const val DATABASE_VERSION = 3
        const val TICKETS_TABLE_NAME = "ticket_entries"
        const val PROFILES_TABLE_NAME = "account_entries"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create profile table
        val createProfileTableQuery = """
        CREATE TABLE $PROFILES_TABLE_NAME (
            name TEXT NOT NULL,
            userId TEXT PRIMARY KEY,
            password TEXT NOT NULL
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
            FOREIGN KEY (userId) REFERENCES $PROFILES_TABLE_NAME(userId)
                ON DELETE CASCADE
                ON UPDATE CASCADE
        )
    """
        db.execSQL(createTicketTableQuery)

        // Insert sample data
        insertSampleData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop old tables and recreate
        db.execSQL("DROP TABLE IF EXISTS $PROFILES_TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $TICKETS_TABLE_NAME")
        onCreate(db)
    }

    private fun insertSampleData(db: SQLiteDatabase) {
        // Sample users - column order: name, userId, password (matching CREATE TABLE)
        db.execSQL("INSERT INTO $PROFILES_TABLE_NAME (name, userId, password) VALUES ('John Doe', 'john_doe', 'password123')")
        db.execSQL("INSERT INTO $PROFILES_TABLE_NAME (name, userId, password) VALUES ('Jane Smith', 'jane_smith', 'password123')")
        db.execSQL("INSERT INTO $PROFILES_TABLE_NAME (name, userId, password) VALUES ('Mike Johnson', 'music_lover', 'password123')")
        db.execSQL("INSERT INTO $PROFILES_TABLE_NAME (name, userId, password) VALUES ('Jessica Nguyen', 'jessicanguyen', 'password124')")

        // Sample tickets - price as INTEGER (no decimals)
        db.execSQL(
            """
        INSERT INTO $TICKETS_TABLE_NAME (name, description, price, userId) 
        VALUES ('Coldplay Concert', 'Amazing night with Coldplay live in concert', 120, 'john_doe')
    """
        )
        db.execSQL(
            """
        INSERT INTO $TICKETS_TABLE_NAME (name, description, price, userId) 
        VALUES ('Taylor Swift Tour', 'Eras Tour - Best seats available', 250, 'jane_smith')
    """
        )
        db.execSQL(
            """
        INSERT INTO $TICKETS_TABLE_NAME (name, description, price, userId) 
        VALUES ('Local Music Festival', 'Weekend pass for all stages', 75, 'music_lover')
    """
        )
        db.execSQL(
            """
        INSERT INTO $TICKETS_TABLE_NAME (name, description, price, userId) 
        VALUES ('Rock Band Live', 'Greatest rock hits live performance', 90, 'jessicanguyen')
    """
        )
        db.execSQL(
            """
        INSERT INTO $TICKETS_TABLE_NAME (name, description, price, userId) 
        VALUES ('Jazz Night', 'Smooth jazz evening with local artists', 45, 'jessicanguyen')
    """
        )
    }
}

// database helper functions
// to use helper functions: include
//      val dbHelper = remember { DatabaseHelper(context) }
// then call functions using dbHelper.{function(arguments)}
// dm me if you have problems :)
class DatabaseHelper(@Suppress("unused") private val context: Context) {

    // profiles table helper functions //

    private val dbHelper = DatabaseSchema(context)
    
    // Open the database (creates it if it doesn't exist)
    private fun getDatabase(): SQLiteDatabase {
        return dbHelper.writableDatabase
    }

    fun insertProfile(profile: Profile): Boolean {
        val db = getDatabase()
        return try {
            db.execSQL(
                "INSERT INTO ${DatabaseSchema.PROFILES_TABLE_NAME} (name, userId, password) VALUES (?, ?, ?)",
                arrayOf(profile.name, profile.userId, profile.password)
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }

    fun getProfile(userId: String): Profile? {
        val db = getDatabase()
        val cursor = db.rawQuery(
            "SELECT name, userId, password FROM ${DatabaseSchema.PROFILES_TABLE_NAME} WHERE userId = ?",
            arrayOf(userId)
        )

        var profile: Profile? = null
        if (cursor.moveToFirst()) {
            profile = Profile(
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                userId = cursor.getString(cursor.getColumnIndexOrThrow("userId")),
                password = cursor.getString(cursor.getColumnIndexOrThrow("password"))
            )
        }

        cursor.close()
        db.close()
        return profile
    }

    fun getAllProfiles(): List<Profile> {
        val db = getDatabase()
        val profiles = mutableListOf<Profile>()
        val cursor = db.rawQuery(
            "SELECT name, userId, password FROM ${DatabaseSchema.PROFILES_TABLE_NAME}",
            null
        )

        while (cursor.moveToNext()) {
            profiles.add(
                Profile(
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    userId = cursor.getString(cursor.getColumnIndexOrThrow("userId")),
                    password = cursor.getString(cursor.getColumnIndexOrThrow("password"))
                )
            )
        }

        cursor.close()
        db.close()
        return profiles
    }

    fun updateProfile(profile: Profile): Boolean {
        val db = getDatabase()
        return try {
            db.execSQL(
                "UPDATE ${DatabaseSchema.PROFILES_TABLE_NAME} SET name = ?, password = ? WHERE userId = ?",
                arrayOf(profile.name, profile.password, profile.userId)
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }

    fun deleteProfile(userId: String): Boolean {
        val db = getDatabase()
        return try {
            db.execSQL(
                "DELETE FROM ${DatabaseSchema.PROFILES_TABLE_NAME} WHERE userId = ?",
                arrayOf(userId)
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }

    // tickets table helper functions //
    fun getAllTickets(): List<TicketEntry> {
        val db = getDatabase()
        val entries = mutableListOf<TicketEntry>()

        val cursor = db.rawQuery(
            "SELECT id, userId, name, price, description FROM ${DatabaseSchema.TICKETS_TABLE_NAME}",
            null
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

    fun updateTicket(entry: TicketEntry): Boolean {
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

    fun deleteTicket(entryId: Int): Boolean {
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

    fun insertTicket(userId: String, name: String, price: String, description: String): Boolean {
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
    fun searchTicketEntries(userId: String, query: String): List<TicketEntry> {
        val db = getDatabase()
        val entries = mutableListOf<TicketEntry>()

        val cursor = db.rawQuery(
            """
            SELECT id, userId, name, price, description 
            FROM ${DatabaseSchema.TICKETS_TABLE_NAME} 
            WHERE userId = ? AND (name LIKE ? OR description LIKE ?)
            ORDER BY id DESC
            """,
            arrayOf(userId, "%$query%", "%$query%")
        )

        while (cursor.moveToNext()) {
            entries.add(
                TicketEntry(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    userId = cursor.getString(cursor.getColumnIndexOrThrow("userId")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    price = cursor.getFloat(cursor.getColumnIndexOrThrow("price")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
                )
            )
        }

        cursor.close()
        db.close()
        return entries
    }
}

