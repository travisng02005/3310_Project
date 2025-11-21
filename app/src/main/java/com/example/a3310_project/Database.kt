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

data class PurchasedTicket(
    val id: Int,
    val userId: String,
    val event: String,
    val price: Float,
    val description: String? = null,
    val buyerId: String
)

data class PaymentMethod(
    val userId: String,
    val cardNumber: String,
    val expiry: String,
    val cvv: String
)

data class UpcomingShow(
    val showId: Int,
    val event: String,
    val date: String,
    val time: String
)

data class PaymentVerification(
    val bank: String,
    val firstTwoDigits: String
)

class DatabaseSchema(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        const val DATABASE_NAME = "example_app.db"
        const val DATABASE_VERSION = 4
        const val PROFILES_TABLE_NAME = "account_entries"
        const val TICKETS_TABLE_NAME = "ticket_entries"
        const val PURCHASED_TICKETS_TABLE_NAME = "purchased_tickets"
        const val PM_TABLE_NAME = "payment_methods"
        const val SHOWS_TABLE_NAME = "upcoming_shows"
        const val PMVERIFY_TABLE_NAME = "payment_verification"
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

        // Create upcoming shows table (must be created before purchased tickets)
        val createUpcomingShowsTableQuery = """
        CREATE TABLE $SHOWS_TABLE_NAME (
            showId INTEGER PRIMARY KEY AUTOINCREMENT,
            event TEXT NOT NULL UNIQUE,
            date TEXT NOT NULL,
            time TEXT NOT NULL
        )
        """
        db.execSQL(createUpcomingShowsTableQuery)

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

        // Create purchased tickets table with foreign key references
        val createPurchasedTicketsTableQuery = """
        CREATE TABLE $PURCHASED_TICKETS_TABLE_NAME (
            id INTEGER NOT NULL,
            userId TEXT NOT NULL,
            event TEXT NOT NULL,
            price INTEGER NOT NULL,
            description TEXT,
            buyerId TEXT NOT NULL,
            PRIMARY KEY (buyerId, id),
            FOREIGN KEY (userId) REFERENCES $PROFILES_TABLE_NAME(userId)
                ON DELETE CASCADE
                ON UPDATE CASCADE,
            FOREIGN KEY (event) REFERENCES $SHOWS_TABLE_NAME(event)
                ON DELETE CASCADE
                ON UPDATE CASCADE,
            FOREIGN KEY (buyerId) REFERENCES $PROFILES_TABLE_NAME(userId)
                ON DELETE CASCADE
                ON UPDATE CASCADE
        )
        """
        db.execSQL(createPurchasedTicketsTableQuery)

        // Create payment methods table
        val createPaymentMethodsTableQuery = """
        CREATE TABLE $PM_TABLE_NAME (
            userId TEXT NOT NULL,
            cardNumber TEXT PRIMARY KEY,
            expiry TEXT NOT NULL,
            cvv TEXT NOT NULL,
            FOREIGN KEY (userId) REFERENCES $PROFILES_TABLE_NAME(userId)
                ON DELETE CASCADE
                ON UPDATE CASCADE
        )
        """
        db.execSQL(createPaymentMethodsTableQuery)

        // Create payment verification table (composite primary key)
        val createPaymentVerificationTableQuery = """
        CREATE TABLE $PMVERIFY_TABLE_NAME (
            bank TEXT NOT NULL,
            firstTwoDigits TEXT NOT NULL,
            PRIMARY KEY (bank, firstTwoDigits)
        )
        """
        db.execSQL(createPaymentVerificationTableQuery)

        // Insert sample data
        insertSampleData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop old tables and recreate (in reverse order due to foreign keys)
        db.execSQL("DROP TABLE IF EXISTS $PURCHASED_TICKETS_TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $PM_TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $PMVERIFY_TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $TICKETS_TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $SHOWS_TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $PROFILES_TABLE_NAME")
        onCreate(db)
    }

    private fun insertSampleData(db: SQLiteDatabase) {
        // Sample users - column order: name, userId, password (matching CREATE TABLE)
        db.execSQL("INSERT INTO $PROFILES_TABLE_NAME (name, userId, password) VALUES ('John Doe', 'john_doe', 'password123')")
        db.execSQL("INSERT INTO $PROFILES_TABLE_NAME (name, userId, password) VALUES ('Jane Smith', 'jane_smith', 'password123')")
        db.execSQL("INSERT INTO $PROFILES_TABLE_NAME (name, userId, password) VALUES ('Mike Johnson', 'music_lover', 'password123')")
        db.execSQL("INSERT INTO $PROFILES_TABLE_NAME (name, userId, password) VALUES ('Jessica Nguyen', 'jessicanguyen', 'password124')")

        // Sample upcoming shows (must be inserted before tickets that reference them)
        db.execSQL("INSERT INTO $SHOWS_TABLE_NAME (event, date, time) VALUES ('Coldplay Concert', '2025-12-15', '19:00')")
        db.execSQL("INSERT INTO $SHOWS_TABLE_NAME (event, date, time) VALUES ('Taylor Swift Tour', '2025-12-20', '20:00')")
        db.execSQL("INSERT INTO $SHOWS_TABLE_NAME (event, date, time) VALUES ('Local Music Festival', '2026-01-05', '18:00')")
        db.execSQL("INSERT INTO $SHOWS_TABLE_NAME (event, date, time) VALUES ('Rock Band Live', '2026-01-15', '20:30')")
        db.execSQL("INSERT INTO $SHOWS_TABLE_NAME (event, date, time) VALUES ('Jazz Night', '2026-02-01', '21:00')")

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

        // Sample payment verification data (bank names and their first two digits)
        db.execSQL("INSERT INTO $PMVERIFY_TABLE_NAME (bank, firstTwoDigits) VALUES ('Chase', '40')")
        db.execSQL("INSERT INTO $PMVERIFY_TABLE_NAME (bank, firstTwoDigits) VALUES ('Bank of America', '45')")
        db.execSQL("INSERT INTO $PMVERIFY_TABLE_NAME (bank, firstTwoDigits) VALUES ('Wells Fargo', '47')")
        db.execSQL("INSERT INTO $PMVERIFY_TABLE_NAME (bank, firstTwoDigits) VALUES ('Citibank', '52')")
        db.execSQL("INSERT INTO $PMVERIFY_TABLE_NAME (bank, firstTwoDigits) VALUES ('Capital One', '55')")
    }
}

// database helper functions
// to use helper functions: include
//      val dbHelper = remember { DatabaseHelper(context) }
// then call functions using dbHelper.{function(arguments)}
class DatabaseHelper(@Suppress("unused") private val context: Context) {

    private val dbHelper = DatabaseSchema(context)
    
    // Open the database (creates it if it doesn't exist)
    private fun getDatabase(): SQLiteDatabase {
        return dbHelper.writableDatabase
    }

    // ============================================
    // PROFILES TABLE HELPER FUNCTIONS
    // ============================================
    
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

    // ============================================
    // TICKETS TABLE HELPER FUNCTIONS
    // ============================================
    
    fun getAllTickets(): List<TicketEntry> {
        val db = getDatabase()
        val entries = mutableListOf<TicketEntry>()

        val cursor = db.rawQuery(
            "SELECT id, userId, name, price, description FROM ${DatabaseSchema.TICKETS_TABLE_NAME}",
            null
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
    
    fun getEntriesByUserId(userId: String): List<TicketEntry> {
        val db = getDatabase()
        val entries = mutableListOf<TicketEntry>()
        
        val cursor = db.rawQuery(
            "SELECT id, userId, name, price, description FROM ${DatabaseSchema.TICKETS_TABLE_NAME} WHERE userId = ?",
            arrayOf(userId)
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

    fun updateTicket(entry: TicketEntry): Boolean {
        val db = getDatabase()
        return try {
            db.execSQL(
                "UPDATE ${DatabaseSchema.TICKETS_TABLE_NAME} SET name = ?, description = ?, price = ? WHERE id = ?",
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
                arrayOf(entryId.toString())
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

    // ============================================
    // PURCHASED TICKETS TABLE HELPER FUNCTIONS
    // ============================================
    
    fun insertPurchasedTicket(purchasedTicket: PurchasedTicket): Boolean {
        val db = getDatabase()
        return try {
            db.execSQL(
                """
                INSERT INTO ${DatabaseSchema.PURCHASED_TICKETS_TABLE_NAME} 
                (id, userId, event, price, description, buyerId) 
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                arrayOf(
                    purchasedTicket.id.toString(),
                    purchasedTicket.userId,
                    purchasedTicket.event,
                    purchasedTicket.price.toString(),
                    purchasedTicket.description,
                    purchasedTicket.buyerId
                )
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }

    fun getPurchasedTicketsByBuyerId(buyerId: String): List<PurchasedTicket> {
        val db = getDatabase()
        val tickets = mutableListOf<PurchasedTicket>()
        
        val cursor = db.rawQuery(
            """
            SELECT id, userId, event, price, description, buyerId 
            FROM ${DatabaseSchema.PURCHASED_TICKETS_TABLE_NAME} 
            WHERE buyerId = ?
            """,
            arrayOf(buyerId)
        )
        
        while (cursor.moveToNext()) {
            tickets.add(
                PurchasedTicket(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    userId = cursor.getString(cursor.getColumnIndexOrThrow("userId")),
                    event = cursor.getString(cursor.getColumnIndexOrThrow("event")),
                    price = cursor.getFloat(cursor.getColumnIndexOrThrow("price")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    buyerId = cursor.getString(cursor.getColumnIndexOrThrow("buyerId"))
                )
            )
        }
        
        cursor.close()
        db.close()
        return tickets
    }

    fun getAllPurchasedTickets(): List<PurchasedTicket> {
        val db = getDatabase()
        val tickets = mutableListOf<PurchasedTicket>()
        
        val cursor = db.rawQuery(
            "SELECT id, userId, event, price, description, buyerId FROM ${DatabaseSchema.PURCHASED_TICKETS_TABLE_NAME}",
            null
        )
        
        while (cursor.moveToNext()) {
            tickets.add(
                PurchasedTicket(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    userId = cursor.getString(cursor.getColumnIndexOrThrow("userId")),
                    event = cursor.getString(cursor.getColumnIndexOrThrow("event")),
                    price = cursor.getFloat(cursor.getColumnIndexOrThrow("price")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    buyerId = cursor.getString(cursor.getColumnIndexOrThrow("buyerId"))
                )
            )
        }
        
        cursor.close()
        db.close()
        return tickets
    }

    fun deletePurchasedTicket(buyerId: String, ticketId: Int): Boolean {
        val db = getDatabase()
        return try {
            db.execSQL(
                "DELETE FROM ${DatabaseSchema.PURCHASED_TICKETS_TABLE_NAME} WHERE buyerId = ? AND id = ?",
                arrayOf(buyerId, ticketId.toString())
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }

    // ============================================
    // PAYMENT METHODS TABLE HELPER FUNCTIONS
    // ============================================
    
    fun insertPaymentMethod(paymentMethod: PaymentMethod): Boolean {
        val db = getDatabase()
        return try {
            db.execSQL(
                """
                INSERT INTO ${DatabaseSchema.PM_TABLE_NAME} 
                (userId, cardNumber, expiry, cvv) 
                VALUES (?, ?, ?, ?)
                """,
                arrayOf(
                    paymentMethod.userId,
                    paymentMethod.cardNumber,
                    paymentMethod.expiry,
                    paymentMethod.cvv
                )
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }

    fun getPaymentMethodsByUserId(userId: String): List<PaymentMethod> {
        val db = getDatabase()
        val paymentMethods = mutableListOf<PaymentMethod>()
        
        val cursor = db.rawQuery(
            """
            SELECT userId, cardNumber, expiry, cvv 
            FROM ${DatabaseSchema.PM_TABLE_NAME} 
            WHERE userId = ?
            """,
            arrayOf(userId)
        )
        
        while (cursor.moveToNext()) {
            paymentMethods.add(
                PaymentMethod(
                    userId = cursor.getString(cursor.getColumnIndexOrThrow("userId")),
                    cardNumber = cursor.getString(cursor.getColumnIndexOrThrow("cardNumber")),
                    expiry = cursor.getString(cursor.getColumnIndexOrThrow("expiry")),
                    cvv = cursor.getString(cursor.getColumnIndexOrThrow("cvv"))
                )
            )
        }
        
        cursor.close()
        db.close()
        return paymentMethods
    }

    fun getPaymentMethodByCardNumber(cardNumber: String): PaymentMethod? {
        val db = getDatabase()
        val cursor = db.rawQuery(
            """
            SELECT userId, cardNumber, expiry, cvv 
            FROM ${DatabaseSchema.PM_TABLE_NAME} 
            WHERE cardNumber = ?
            """,
            arrayOf(cardNumber)
        )

        var paymentMethod: PaymentMethod? = null
        if (cursor.moveToFirst()) {
            paymentMethod = PaymentMethod(
                userId = cursor.getString(cursor.getColumnIndexOrThrow("userId")),
                cardNumber = cursor.getString(cursor.getColumnIndexOrThrow("cardNumber")),
                expiry = cursor.getString(cursor.getColumnIndexOrThrow("expiry")),
                cvv = cursor.getString(cursor.getColumnIndexOrThrow("cvv"))
            )
        }

        cursor.close()
        db.close()
        return paymentMethod
    }

    fun updatePaymentMethod(paymentMethod: PaymentMethod): Boolean {
        val db = getDatabase()
        return try {
            db.execSQL(
                """
                UPDATE ${DatabaseSchema.PM_TABLE_NAME} 
                SET expiry = ?, cvv = ? 
                WHERE cardNumber = ?
                """,
                arrayOf(
                    paymentMethod.expiry,
                    paymentMethod.cvv,
                    paymentMethod.cardNumber
                )
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }

    fun deletePaymentMethod(cardNumber: String): Boolean {
        val db = getDatabase()
        return try {
            db.execSQL(
                "DELETE FROM ${DatabaseSchema.PM_TABLE_NAME} WHERE cardNumber = ?",
                arrayOf(cardNumber)
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }

    // ============================================
    // UPCOMING SHOWS TABLE HELPER FUNCTIONS
    // ============================================
    
    fun insertUpcomingShow(event: String, date: String, time: String): Boolean {
        val db = getDatabase()
        return try {
            db.execSQL(
                """
                INSERT INTO ${DatabaseSchema.SHOWS_TABLE_NAME} 
                (event, date, time) 
                VALUES (?, ?, ?)
                """,
                arrayOf(event, date, time)
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }

    fun getAllUpcomingShows(): List<UpcomingShow> {
        val db = getDatabase()
        val shows = mutableListOf<UpcomingShow>()
        
        val cursor = db.rawQuery(
            "SELECT showId, event, date, time FROM ${DatabaseSchema.SHOWS_TABLE_NAME}",
            null
        )
        
        while (cursor.moveToNext()) {
            shows.add(
                UpcomingShow(
                    showId = cursor.getInt(cursor.getColumnIndexOrThrow("showId")),
                    event = cursor.getString(cursor.getColumnIndexOrThrow("event")),
                    date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    time = cursor.getString(cursor.getColumnIndexOrThrow("time"))
                )
            )
        }
        
        cursor.close()
        db.close()
        return shows
    }

    fun getUpcomingShowById(showId: Int): UpcomingShow? {
        val db = getDatabase()
        val cursor = db.rawQuery(
            """
            SELECT showId, event, date, time 
            FROM ${DatabaseSchema.SHOWS_TABLE_NAME} 
            WHERE showId = ?
            """,
            arrayOf(showId.toString())
        )

        var show: UpcomingShow? = null
        if (cursor.moveToFirst()) {
            show = UpcomingShow(
                showId = cursor.getInt(cursor.getColumnIndexOrThrow("showId")),
                event = cursor.getString(cursor.getColumnIndexOrThrow("event")),
                date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                time = cursor.getString(cursor.getColumnIndexOrThrow("time"))
            )
        }

        cursor.close()
        db.close()
        return show
    }

    fun getUpcomingShowByEvent(event: String): UpcomingShow? {
        val db = getDatabase()
        val cursor = db.rawQuery(
            """
            SELECT showId, event, date, time 
            FROM ${DatabaseSchema.SHOWS_TABLE_NAME} 
            WHERE event = ?
            """,
            arrayOf(event)
        )

        var show: UpcomingShow? = null
        if (cursor.moveToFirst()) {
            show = UpcomingShow(
                showId = cursor.getInt(cursor.getColumnIndexOrThrow("showId")),
                event = cursor.getString(cursor.getColumnIndexOrThrow("event")),
                date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                time = cursor.getString(cursor.getColumnIndexOrThrow("time"))
            )
        }

        cursor.close()
        db.close()
        return show
    }

    fun searchUpcomingShows(query: String): List<UpcomingShow> {
        val db = getDatabase()
        val shows = mutableListOf<UpcomingShow>()
        
        val cursor = db.rawQuery(
            """
            SELECT showId, event, date, time 
            FROM ${DatabaseSchema.SHOWS_TABLE_NAME} 
            WHERE event LIKE ?
            """,
            arrayOf("%$query%")
        )
        
        while (cursor.moveToNext()) {
            shows.add(
                UpcomingShow(
                    showId = cursor.getInt(cursor.getColumnIndexOrThrow("showId")),
                    event = cursor.getString(cursor.getColumnIndexOrThrow("event")),
                    date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    time = cursor.getString(cursor.getColumnIndexOrThrow("time"))
                )
            )
        }
        
        cursor.close()
        db.close()
        return shows
    }

    fun updateUpcomingShow(show: UpcomingShow): Boolean {
        val db = getDatabase()
        return try {
            db.execSQL(
                """
                UPDATE ${DatabaseSchema.SHOWS_TABLE_NAME} 
                SET event = ?, date = ?, time = ? 
                WHERE showId = ?
                """,
                arrayOf(
                    show.event,
                    show.date,
                    show.time,
                    show.showId.toString()
                )
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }

    fun deleteUpcomingShow(showId: Int): Boolean {
        val db = getDatabase()
        return try {
            db.execSQL(
                "DELETE FROM ${DatabaseSchema.SHOWS_TABLE_NAME} WHERE showId = ?",
                arrayOf(showId.toString())
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }

    // ============================================
    // PAYMENT VERIFICATION TABLE HELPER FUNCTIONS
    // ============================================
    
    fun insertPaymentVerification(bank: String, firstTwoDigits: String): Boolean {
        val db = getDatabase()
        return try {
            db.execSQL(
                """
                INSERT INTO ${DatabaseSchema.PMVERIFY_TABLE_NAME} 
                (bank, firstTwoDigits) 
                VALUES (?, ?)
                """,
                arrayOf(bank, firstTwoDigits)
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }

    fun getPaymentVerificationByBank(bank: String): List<PaymentVerification> {
        val db = getDatabase()
        val verifications = mutableListOf<PaymentVerification>()
        
        val cursor = db.rawQuery(
            """
            SELECT bank, firstTwoDigits 
            FROM ${DatabaseSchema.PMVERIFY_TABLE_NAME} 
            WHERE bank = ?
            """,
            arrayOf(bank)
        )

        while (cursor.moveToNext()) {
            verifications.add(
                PaymentVerification(
                    bank = cursor.getString(cursor.getColumnIndexOrThrow("bank")),
                    firstTwoDigits = cursor.getString(cursor.getColumnIndexOrThrow("firstTwoDigits"))
                )
            )
        }

        cursor.close()
        db.close()
        return verifications
    }

    fun getPaymentVerificationByFirstTwo(firstTwoDigits: String): List<PaymentVerification> {
        val db = getDatabase()
        val verifications = mutableListOf<PaymentVerification>()
        
        val cursor = db.rawQuery(
            """
            SELECT bank, firstTwoDigits 
            FROM ${DatabaseSchema.PMVERIFY_TABLE_NAME} 
            WHERE firstTwoDigits = ?
            """,
            arrayOf(firstTwoDigits)
        )

        while (cursor.moveToNext()) {
            verifications.add(
                PaymentVerification(
                    bank = cursor.getString(cursor.getColumnIndexOrThrow("bank")),
                    firstTwoDigits = cursor.getString(cursor.getColumnIndexOrThrow("firstTwoDigits"))
                )
            )
        }

        cursor.close()
        db.close()
        return verifications
    }

    fun getAllPaymentVerifications(): List<PaymentVerification> {
        val db = getDatabase()
        val verifications = mutableListOf<PaymentVerification>()
        
        val cursor = db.rawQuery(
            "SELECT bank, firstTwoDigits FROM ${DatabaseSchema.PMVERIFY_TABLE_NAME}",
            null
        )
        
        while (cursor.moveToNext()) {
            verifications.add(
                PaymentVerification(
                    bank = cursor.getString(cursor.getColumnIndexOrThrow("bank")),
                    firstTwoDigits = cursor.getString(cursor.getColumnIndexOrThrow("firstTwoDigits"))
                )
            )
        }
        
        cursor.close()
        db.close()
        return verifications
    }

    fun updatePaymentVerification(bank: String, oldFirstTwo: String, newFirstTwo: String): Boolean {
        val db = getDatabase()
        return try {
            db.execSQL(
                """
                UPDATE ${DatabaseSchema.PMVERIFY_TABLE_NAME} 
                SET firstTwoDigits = ? 
                WHERE bank = ? AND firstTwoDigits = ?
                """,
                arrayOf(newFirstTwo, bank, oldFirstTwo)
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }

    fun deletePaymentVerification(bank: String, firstTwoDigits: String): Boolean {
        val db = getDatabase()
        return try {
            db.execSQL(
                "DELETE FROM ${DatabaseSchema.PMVERIFY_TABLE_NAME} WHERE bank = ? AND firstTwoDigits = ?",
                arrayOf(bank, firstTwoDigits)
            )
            db.close()
            true
        } catch (e: Exception) {
            print("Error: $e")
            db.close()
            false
        }
    }
}
