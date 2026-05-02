package com.example.barbershop.util

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Database(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME    = "barbershop.db"
        const val DATABASE_VERSION = 9
        const val TABLE_USERS        = "users"
        const val COL_USER_ID        = "id"
        const val COL_USER_EMAIL     = "email"
        const val COL_USER_PASSWORD  = "password"
        const val COL_USER_NAME      = "name"

        const val TABLE_ADMINS       = "admins"
        const val COL_ADMIN_ID       = "id"
        const val COL_ADMIN_EMAIL    = "email"
        const val COL_ADMIN_PASSWORD = "password"
        const val COL_ADMIN_NAME     = "name"

        const val TABLE_BOOKINGS          = "bookings"
        const val COL_BOOKING_ID          = "id"
        const val COL_BOOKING_USER        = "user_email"
        const val COL_BOOKING_BARBER      = "barber"
        const val COL_BOOKING_SERVICE     = "service"
        const val COL_BOOKING_DATE        = "date"
        const val COL_BOOKING_TIME        = "time_slot"
        const val COL_BOOKING_AMOUNT      = "total_amount"
        const val COL_BOOKING_STATUS      = "status"
        const val COL_BOOKING_SCREENSHOT  = "payment_screenshot"

        const val TABLE_ORDERS         = "orders"
        const val COL_ORDER_ID         = "id"
        const val COL_ORDER_USER       = "user_email"
        const val COL_ORDER_ITEMS      = "items"
        const val COL_ORDER_TOTAL      = "total_amount"
        const val COL_ORDER_DATE       = "order_date"
        const val COL_ORDER_STATUS     = "status"
        const val COL_ORDER_SCREENSHOT = "payment_screenshot"

        const val TABLE_RATINGS      = "ratings"
        const val COL_RATING_ID      = "id"
        const val COL_RATING_USER    = "user_email"
        const val COL_RATING_BARBER  = "barber"
        const val COL_RATING_SCORE   = "score"
        const val COL_RATING_COMMENT = "comment"
        const val COL_RATING_DATE    = "date"
        const val COL_RATING_SERVICE = "service"

        const val TABLE_PRODUCTS      = "products"
        const val COL_PRODUCT_ID      = "id"
        const val COL_PRODUCT_NAME    = "name"
        const val COL_PRODUCT_DESC    = "description"
        const val COL_PRODUCT_PRICE   = "price"
        const val COL_PRODUCT_STOCK   = "stock"

        const val TABLE_BARBERS        = "barbers"
        const val COL_BARBER_ID        = "id"
        const val COL_BARBER_NAME      = "name"
        const val COL_BARBER_SPECIALTY = "specialty"
        const val COL_BARBER_DESC      = "description"
        const val COL_BARBER_EXP       = "experience"
        const val COL_BARBER_STATUS    = "status"

        const val TABLE_SCHEDULES      = "schedules"
        const val COL_SCHEDULE_ID      = "id"
        const val COL_SCHEDULE_BARBER  = "barber_name"
        const val COL_SCHEDULE_DAY     = "day_of_week"
        const val COL_SCHEDULE_TYPE    = "type"

        const val TABLE_SERVICES        = "services"
        const val COL_SERVICE_ID        = "id"
        const val COL_SERVICE_NAME      = "name"
        const val COL_SERVICE_DESC      = "description"
        const val COL_SERVICE_PRICE     = "price"
        const val COL_SERVICE_DURATION  = "duration"

        val BARBER_NAME_MAP = mapOf(
            "jerbs" to "Tyson"
        )
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_USERS (
                $COL_USER_ID       INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_EMAIL    TEXT UNIQUE NOT NULL,
                $COL_USER_PASSWORD TEXT NOT NULL,
                $COL_USER_NAME     TEXT
            )""".trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_ADMINS (
                $COL_ADMIN_ID       INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ADMIN_EMAIL    TEXT UNIQUE NOT NULL,
                $COL_ADMIN_PASSWORD TEXT NOT NULL,
                $COL_ADMIN_NAME     TEXT
            )""".trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_BOOKINGS (
                $COL_BOOKING_ID         INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_BOOKING_USER       TEXT NOT NULL,
                $COL_BOOKING_BARBER     TEXT NOT NULL,
                $COL_BOOKING_SERVICE    TEXT NOT NULL,
                $COL_BOOKING_DATE       TEXT NOT NULL,
                $COL_BOOKING_TIME       TEXT NOT NULL,
                $COL_BOOKING_AMOUNT     TEXT NOT NULL,
                $COL_BOOKING_STATUS     TEXT DEFAULT 'Pending',
                $COL_BOOKING_SCREENSHOT TEXT DEFAULT ''
            )""".trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_ORDERS (
                $COL_ORDER_ID         INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ORDER_USER       TEXT NOT NULL,
                $COL_ORDER_ITEMS      TEXT NOT NULL,
                $COL_ORDER_TOTAL      TEXT NOT NULL,
                $COL_ORDER_DATE       TEXT NOT NULL,
                $COL_ORDER_STATUS     TEXT DEFAULT 'Confirmed',
                $COL_ORDER_SCREENSHOT TEXT DEFAULT ''
            )""".trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_RATINGS (
                $COL_RATING_ID      INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_RATING_USER    TEXT NOT NULL,
                $COL_RATING_BARBER  TEXT NOT NULL,
                $COL_RATING_SCORE   INTEGER NOT NULL,
                $COL_RATING_COMMENT TEXT,
                $COL_RATING_DATE    TEXT NOT NULL,
                $COL_RATING_SERVICE TEXT DEFAULT ''
            )""".trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_PRODUCTS (
                $COL_PRODUCT_ID    INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PRODUCT_NAME  TEXT NOT NULL,
                $COL_PRODUCT_DESC  TEXT,
                $COL_PRODUCT_PRICE TEXT NOT NULL,
                $COL_PRODUCT_STOCK INTEGER DEFAULT 0
            )""".trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_BARBERS (
                $COL_BARBER_ID        INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_BARBER_NAME      TEXT NOT NULL,
                $COL_BARBER_SPECIALTY TEXT,
                $COL_BARBER_DESC      TEXT,
                $COL_BARBER_EXP       TEXT,
                $COL_BARBER_STATUS    TEXT DEFAULT 'Active'
            )""".trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_SCHEDULES (
                $COL_SCHEDULE_ID     INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SCHEDULE_BARBER TEXT NOT NULL,
                $COL_SCHEDULE_DAY    TEXT NOT NULL,
                $COL_SCHEDULE_TYPE   TEXT DEFAULT 'available',
                UNIQUE($COL_SCHEDULE_BARBER, $COL_SCHEDULE_DAY)
            )""".trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_SERVICES (
                $COL_SERVICE_ID       INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SERVICE_NAME     TEXT NOT NULL,
                $COL_SERVICE_DESC     TEXT DEFAULT '',
                $COL_SERVICE_PRICE    TEXT NOT NULL,
                $COL_SERVICE_DURATION TEXT DEFAULT '60 minutes'
            )""".trimIndent())

        insertDefaultAdmin(db)
        insertDefaultProducts(db)
        insertDefaultBarbers(db)
        insertDefaultServices(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_RATINGS ADD COLUMN $COL_RATING_SERVICE TEXT DEFAULT ''")
            } catch (_: Exception) {}
        }

        if (oldVersion < 3) {
            BARBER_NAME_MAP.forEach { (oldName, newName) ->
                try {
                    db.execSQL(
                        "UPDATE $TABLE_BOOKINGS SET $COL_BOOKING_BARBER = ? WHERE $COL_BOOKING_BARBER = ?",
                        arrayOf(newName, oldName)
                    )
                } catch (e: Exception) {
                    Log.e("DB Migration", "Failed bookings update for $oldName: ${e.message}")
                }
                try {
                    db.execSQL(
                        "UPDATE $TABLE_RATINGS SET $COL_RATING_BARBER = ? WHERE $COL_RATING_BARBER = ?",
                        arrayOf(newName, oldName)
                    )
                } catch (e: Exception) {
                    Log.e("DB Migration", "Failed ratings update for $oldName: ${e.message}")
                }
            }
        }

        if (oldVersion < 4) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS $TABLE_PRODUCTS (
                        $COL_PRODUCT_ID    INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_PRODUCT_NAME  TEXT NOT NULL,
                        $COL_PRODUCT_DESC  TEXT,
                        $COL_PRODUCT_PRICE TEXT NOT NULL,
                        $COL_PRODUCT_STOCK INTEGER DEFAULT 0
                    )""".trimIndent())
                insertDefaultProducts(db)
            } catch (e: Exception) {
                Log.e("DB", "Failed to create products table: ${e.message}")
            }
        }

        if (oldVersion < 5) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS $TABLE_BARBERS (
                        $COL_BARBER_ID        INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_BARBER_NAME      TEXT NOT NULL,
                        $COL_BARBER_SPECIALTY TEXT,
                        $COL_BARBER_DESC      TEXT,
                        $COL_BARBER_EXP       TEXT,
                        $COL_BARBER_STATUS    TEXT DEFAULT 'Active'
                    )""".trimIndent())
                insertDefaultBarbers(db)
            } catch (e: Exception) {
                Log.e("DB", "Failed to create barbers table: ${e.message}")
            }
        }

        if (oldVersion < 6) {
            try {
                db.execSQL("ALTER TABLE $TABLE_BOOKINGS ADD COLUMN $COL_BOOKING_SCREENSHOT TEXT DEFAULT ''")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE $TABLE_ORDERS ADD COLUMN $COL_ORDER_SCREENSHOT TEXT DEFAULT ''")
            } catch (_: Exception) {}
        }

        if (oldVersion < 7) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS $TABLE_SCHEDULES (
                        $COL_SCHEDULE_ID     INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_SCHEDULE_BARBER TEXT NOT NULL,
                        $COL_SCHEDULE_DAY    TEXT NOT NULL,
                        $COL_SCHEDULE_TYPE   TEXT DEFAULT 'available',
                        UNIQUE($COL_SCHEDULE_BARBER, $COL_SCHEDULE_DAY)
                    )""".trimIndent())
            } catch (e: Exception) {
                Log.e("DB", "Failed to create schedules table: ${e.message}")
            }
        }

        if (oldVersion < 8) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS $TABLE_SERVICES (
                        $COL_SERVICE_ID       INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_SERVICE_NAME     TEXT NOT NULL,
                        $COL_SERVICE_DESC     TEXT DEFAULT '',
                        $COL_SERVICE_PRICE    TEXT NOT NULL,
                        $COL_SERVICE_DURATION TEXT DEFAULT '60 minutes'
                    )""".trimIndent())
                val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_SERVICES", null)
                val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
                cursor.close()
                if (count == 0) insertDefaultServices(db)
            } catch (e: Exception) {
                Log.e("DB", "Failed to create services table: ${e.message}")
            }
        }

        if (oldVersion < 9) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS $TABLE_ADMINS (
                        $COL_ADMIN_ID       INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_ADMIN_EMAIL    TEXT UNIQUE NOT NULL,
                        $COL_ADMIN_PASSWORD TEXT NOT NULL,
                        $COL_ADMIN_NAME     TEXT
                    )""".trimIndent())
                val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_ADMINS", null)
                val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
                cursor.close()
                if (count == 0) insertDefaultAdmin(db)
            } catch (e: Exception) {
                Log.e("DB", "Failed to create admins table: ${e.message}")
            }
        }
    }

    private fun insertDefaultAdmin(db: SQLiteDatabase) {
        val values = ContentValues().apply {
            put(COL_ADMIN_EMAIL,    "admin001")
            put(COL_ADMIN_PASSWORD, "password")
            put(COL_ADMIN_NAME,     "Admin")
        }
        try {
            db.insert(TABLE_ADMINS, null, values)
        } catch (e: Exception) {
            Log.e("DB", "insertDefaultAdmin: ${e.message}")
        }
    }

    private fun insertDefaultServices(db: SQLiteDatabase) {
        val defaults = listOf(
            Triple("Forget About It", "Skin Head, Quick Massage, Hot Towel",                   "₱180.00") to "50 minutes",
            Triple("Hit & Whack",     "Haircut, Quick Massage, Hot Towel",                     "₱200.00") to "50 minutes",
            Triple("Made Man",        "Haircut, Quick Massage, Hot Towel, Shampoo, Rinse",     "₱250.00") to "60 minutes",
            Triple("The Don",         "Haircut, Four Hand Massage, Hot & Cold Towel, Shampoo", "₱320.00") to "60 minutes",
            Triple("Hair Coloring",   "Full hair coloring service",                            "₱350.00") to "60 minutes"
        )
        defaults.forEach { (info, duration) ->
            val (name, desc, price) = info
            val values = ContentValues().apply {
                put(COL_SERVICE_NAME,     name)
                put(COL_SERVICE_DESC,     desc)
                put(COL_SERVICE_PRICE,    price)
                put(COL_SERVICE_DURATION, duration)
            }
            try {
                db.insert(TABLE_SERVICES, null, values)
            } catch (e: Exception) {
                Log.e("DB", "insertDefaultServices: ${e.message}")
            }
        }
    }

    private fun insertDefaultProducts(db: SQLiteDatabase) {
        val defaults = listOf(
            Triple("Beard",          "A nourishing beard product that softens, moisturizes, and keeps your beard fresh.", "₱40.00")  to 5,
            Triple("Beard Care Oil", "Keeps your beard soft, smooth, and healthy with a refreshing scent.",               "₱200.00") to 0,
            Triple("Beard Oil",      "Nourishing beard oil with natural ingredients",                                     "₱280.00") to 58,
            Triple("Hair Wax",       "Matte finish hair wax",                                                             "₱320.00") to 50,
            Triple("Premium Pomade", "Strong hold pomade for all-day styling",                                            "₱350.00") to 41,
            Triple("Shampoo",        "Professional barber shampoo for all hair types",                                    "₱150.00") to 33
        )
        defaults.forEach { (info, stock) ->
            val (name, desc, price) = info
            val values = ContentValues().apply {
                put(COL_PRODUCT_NAME,  name)
                put(COL_PRODUCT_DESC,  desc)
                put(COL_PRODUCT_PRICE, price)
                put(COL_PRODUCT_STOCK, stock)
            }
            try {
                db.insert(TABLE_PRODUCTS, null, values)
            } catch (e: Exception) {
                Log.e("DB", "insertDefaultProducts: ${e.message}")
            }
        }
    }

    private fun insertDefaultBarbers(db: SQLiteDatabase) {
        val defaults = listOf(
            mapOf(
                "name"      to "Jerby",
                "specialty" to "Contemporary Hairstyle",
                "desc"      to "Young and creative barber specializing in modern cuts and beard styling.",
                "exp"       to "2 years experience",
                "status"    to "Active"
            ),
            mapOf(
                "name"      to "Kevin",
                "specialty" to "Modern Styles & Beard Grooming",
                "desc"      to "Specializes in contemporary hairstyles and beard designs.",
                "exp"       to "7 years experience",
                "status"    to "Active"
            )
        )
        defaults.forEach { b ->
            val values = ContentValues().apply {
                put(COL_BARBER_NAME,      b["name"])
                put(COL_BARBER_SPECIALTY, b["specialty"])
                put(COL_BARBER_DESC,      b["desc"])
                put(COL_BARBER_EXP,       b["exp"])
                put(COL_BARBER_STATUS,    b["status"])
            }
            try {
                db.insert(TABLE_BARBERS, null, values)
            } catch (e: Exception) {
                Log.e("DB", "insertDefaultBarbers: ${e.message}")
            }
        }
    }

    fun loginAdmin(username: String, password: String): Boolean {
        return try {
            val cursor = readableDatabase.query(
                TABLE_ADMINS, null,
                "$COL_ADMIN_EMAIL = ? AND $COL_ADMIN_PASSWORD = ?",
                arrayOf(username.trim().lowercase(), password),
                null, null, null
            )
            val found = cursor.count > 0
            cursor.close()
            found
        } catch (e: Exception) {
            Log.e("DB", "loginAdmin: ${e.message}")
            false
        }
    }

    fun getAdminName(username: String): String? {
        return try {
            val cursor = readableDatabase.query(
                TABLE_ADMINS, arrayOf(COL_ADMIN_NAME),
                "$COL_ADMIN_EMAIL = ?", arrayOf(username.trim().lowercase()),
                null, null, null
            )
            val name = if (cursor.moveToFirst()) cursor.getString(0) else null
            cursor.close()
            name
        } catch (e: Exception) {
            Log.e("DB", "getAdminName: ${e.message}")
            null
        }
    }

    fun updateAdminPassword(username: String, newPassword: String): Boolean {
        return try {
            val values = ContentValues().apply { put(COL_ADMIN_PASSWORD, newPassword) }
            writableDatabase.update(
                TABLE_ADMINS, values,
                "$COL_ADMIN_EMAIL = ?", arrayOf(username.trim().lowercase())
            ) > 0
        } catch (e: Exception) {
            Log.e("DB", "updateAdminPassword: ${e.message}")
            false
        }
    }

    fun setBarberDayType(barberName: String, day: String, type: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(COL_SCHEDULE_BARBER, barberName)
                put(COL_SCHEDULE_DAY,    day)
                put(COL_SCHEDULE_TYPE,   type)
            }
            writableDatabase.insertWithOnConflict(
                TABLE_SCHEDULES, null, values, SQLiteDatabase.CONFLICT_REPLACE
            ) > 0
        } catch (e: Exception) {
            Log.e("DB", "setBarberDayType: ${e.message}")
            false
        }
    }

    fun getRestDays(barberName: String): List<String> {
        return try {
            val cursor = readableDatabase.query(
                TABLE_SCHEDULES, arrayOf(COL_SCHEDULE_DAY),
                "$COL_SCHEDULE_BARBER = ? AND $COL_SCHEDULE_TYPE = ?",
                arrayOf(barberName, "rest"), null, null, null
            )
            val days = mutableListOf<String>()
            while (cursor.moveToNext()) days.add(cursor.getString(0))
            cursor.close()
            days
        } catch (e: Exception) {
            Log.e("DB", "getRestDays: ${e.message}")
            emptyList()
        }
    }

    fun getAllServices(): List<Map<String, String>> {
        return try {
            val cursor = readableDatabase.query(
                TABLE_SERVICES, null, null, null, null, null, "$COL_SERVICE_ID ASC"
            )
            val list = mutableListOf<Map<String, String>>()
            while (cursor.moveToNext()) {
                list.add(mapOf(
                    "id"       to (cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICE_ID))       ?: ""),
                    "name"     to (cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICE_NAME))     ?: ""),
                    "desc"     to (cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICE_DESC))     ?: ""),
                    "price"    to (cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICE_PRICE))    ?: ""),
                    "duration" to (cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICE_DURATION)) ?: "60 minutes")
                ))
            }
            cursor.close()
            list
        } catch (e: Exception) {
            Log.e("DB", "getAllServices: ${e.message}")
            emptyList()
        }
    }

    fun insertService(name: String, desc: String, price: String, duration: String): Long {
        return try {
            val values = ContentValues().apply {
                put(COL_SERVICE_NAME,     name.trim())
                put(COL_SERVICE_DESC,     desc.trim())
                put(COL_SERVICE_PRICE,    price.trim())
                put(COL_SERVICE_DURATION, duration.trim())
            }
            writableDatabase.insert(TABLE_SERVICES, null, values)
        } catch (e: Exception) {
            Log.e("DB", "insertService: ${e.message}")
            -1L
        }
    }

    fun updateService(id: String, name: String, desc: String, price: String, duration: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(COL_SERVICE_NAME,     name.trim())
                put(COL_SERVICE_DESC,     desc.trim())
                put(COL_SERVICE_PRICE,    price.trim())
                put(COL_SERVICE_DURATION, duration.trim())
            }
            writableDatabase.update(TABLE_SERVICES, values, "$COL_SERVICE_ID = ?", arrayOf(id)) > 0
        } catch (e: Exception) {
            Log.e("DB", "updateService: ${e.message}")
            false
        }
    }

    fun deleteService(id: String): Boolean {
        return try {
            writableDatabase.delete(TABLE_SERVICES, "$COL_SERVICE_ID = ?", arrayOf(id)) > 0
        } catch (e: Exception) {
            Log.e("DB", "deleteService: ${e.message}")
            false
        }
    }

    fun getAllBarbers(): List<Map<String, String>> {
        return try {
            val cursor = readableDatabase.query(
                TABLE_BARBERS, null, null, null, null, null, "$COL_BARBER_ID ASC"
            )
            val list = mutableListOf<Map<String, String>>()
            while (cursor.moveToNext()) {
                list.add(mapOf(
                    "id"        to (cursor.getString(cursor.getColumnIndexOrThrow(COL_BARBER_ID))        ?: ""),
                    "name"      to (cursor.getString(cursor.getColumnIndexOrThrow(COL_BARBER_NAME))      ?: ""),
                    "specialty" to (cursor.getString(cursor.getColumnIndexOrThrow(COL_BARBER_SPECIALTY)) ?: ""),
                    "desc"      to (cursor.getString(cursor.getColumnIndexOrThrow(COL_BARBER_DESC))      ?: ""),
                    "exp"       to (cursor.getString(cursor.getColumnIndexOrThrow(COL_BARBER_EXP))       ?: ""),
                    "status"    to (cursor.getString(cursor.getColumnIndexOrThrow(COL_BARBER_STATUS))    ?: "Active")
                ))
            }
            cursor.close()
            list
        } catch (e: Exception) {
            Log.e("DB", "getAllBarbers: ${e.message}")
            emptyList()
        }
    }

    fun insertBarber(name: String, specialty: String, desc: String, exp: String, status: String): Long {
        return try {
            val values = ContentValues().apply {
                put(COL_BARBER_NAME,      name)
                put(COL_BARBER_SPECIALTY, specialty)
                put(COL_BARBER_DESC,      desc)
                put(COL_BARBER_EXP,       exp)
                put(COL_BARBER_STATUS,    status)
            }
            writableDatabase.insert(TABLE_BARBERS, null, values)
        } catch (e: Exception) {
            Log.e("DB", "insertBarber: ${e.message}")
            -1L
        }
    }

    fun updateBarber(id: String, name: String, specialty: String, desc: String, exp: String, status: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(COL_BARBER_NAME,      name)
                put(COL_BARBER_SPECIALTY, specialty)
                put(COL_BARBER_DESC,      desc)
                put(COL_BARBER_EXP,       exp)
                put(COL_BARBER_STATUS,    status)
            }
            writableDatabase.update(TABLE_BARBERS, values, "$COL_BARBER_ID = ?", arrayOf(id)) > 0
        } catch (e: Exception) {
            Log.e("DB", "updateBarber: ${e.message}")
            false
        }
    }

    fun deleteBarber(id: String): Boolean {
        return try {
            writableDatabase.delete(TABLE_BARBERS, "$COL_BARBER_ID = ?", arrayOf(id)) > 0
        } catch (e: Exception) {
            Log.e("DB", "deleteBarber: ${e.message}")
            false
        }
    }

    fun getAllProducts(): List<Map<String, String>> {
        return try {
            val cursor = readableDatabase.query(
                TABLE_PRODUCTS, null, null, null, null, null, "$COL_PRODUCT_ID ASC"
            )
            val list = mutableListOf<Map<String, String>>()
            while (cursor.moveToNext()) {
                list.add(mapOf(
                    "id"    to (cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_ID))    ?: ""),
                    "name"  to (cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_NAME))  ?: ""),
                    "desc"  to (cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_DESC))  ?: ""),
                    "price" to (cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_PRICE)) ?: ""),
                    "stock" to (cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_STOCK)) ?: "0")
                ))
            }
            cursor.close()
            list
        } catch (e: Exception) {
            Log.e("DB", "getAllProducts: ${e.message}")
            emptyList()
        }
    }

    fun insertProduct(name: String, desc: String, price: String, stock: Int): Long {
        return try {
            val values = ContentValues().apply {
                put(COL_PRODUCT_NAME,  name)
                put(COL_PRODUCT_DESC,  desc)
                put(COL_PRODUCT_PRICE, price)
                put(COL_PRODUCT_STOCK, stock)
            }
            writableDatabase.insert(TABLE_PRODUCTS, null, values)
        } catch (e: Exception) {
            Log.e("DB", "insertProduct: ${e.message}")
            -1L
        }
    }

    fun updateProduct(id: String, name: String, desc: String, price: String, stock: Int): Boolean {
        return try {
            val values = ContentValues().apply {
                put(COL_PRODUCT_NAME,  name)
                put(COL_PRODUCT_DESC,  desc)
                put(COL_PRODUCT_PRICE, price)
                put(COL_PRODUCT_STOCK, stock)
            }
            writableDatabase.update(TABLE_PRODUCTS, values, "$COL_PRODUCT_ID = ?", arrayOf(id)) > 0
        } catch (e: Exception) {
            Log.e("DB", "updateProduct: ${e.message}")
            false
        }
    }

    fun deleteProduct(id: String): Boolean {
        return try {
            writableDatabase.delete(TABLE_PRODUCTS, "$COL_PRODUCT_ID = ?", arrayOf(id)) > 0
        } catch (e: Exception) {
            Log.e("DB", "deleteProduct: ${e.message}")
            false
        }
    }

    fun registerUser(email: String, password: String, name: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(COL_USER_EMAIL,    email.trim().lowercase())
                put(COL_USER_PASSWORD, password)
                put(COL_USER_NAME,     name.trim())
            }
            writableDatabase.insertOrThrow(TABLE_USERS, null, values)
            true
        } catch (e: Exception) {
            Log.e("DB", "registerUser: ${e.message}")
            false
        }
    }

    fun loginUser(email: String, password: String): Boolean {
        return try {
            val cursor = readableDatabase.query(
                TABLE_USERS, null,
                "$COL_USER_EMAIL = ? AND $COL_USER_PASSWORD = ?",
                arrayOf(email.trim().lowercase(), password),
                null, null, null
            )
            val found = cursor.count > 0
            cursor.close()
            found
        } catch (e: Exception) {
            Log.e("DB", "loginUser: ${e.message}")
            false
        }
    }

    fun getUserName(email: String): String? {
        return try {
            val cursor = readableDatabase.query(
                TABLE_USERS, arrayOf(COL_USER_NAME),
                "$COL_USER_EMAIL = ?", arrayOf(email.trim().lowercase()),
                null, null, null
            )
            val name = if (cursor.moveToFirst()) cursor.getString(0) else null
            cursor.close()
            name
        } catch (e: Exception) {
            Log.e("DB", "getUserName: ${e.message}")
            null
        }
    }

    fun emailExists(email: String): Boolean {
        return try {
            val cursor = readableDatabase.query(
                TABLE_USERS, null,
                "$COL_USER_EMAIL = ?", arrayOf(email.trim().lowercase()),
                null, null, null
            )
            val exists = cursor.count > 0
            cursor.close()
            exists
        } catch (e: Exception) {
            Log.e("DB", "emailExists: ${e.message}")
            false
        }
    }

    fun updatePassword(email: String, newPassword: String): Boolean {
        return try {
            val values = ContentValues().apply { put(COL_USER_PASSWORD, newPassword) }
            writableDatabase.update(
                TABLE_USERS, values,
                "$COL_USER_EMAIL = ?", arrayOf(email.trim().lowercase())
            ) > 0
        } catch (e: Exception) {
            Log.e("DB", "updatePassword: ${e.message}")
            false
        }
    }

    fun saveBooking(
        userEmail: String, barber: String, service: String,
        date: String, timeSlot: String, totalAmount: String,
        screenshotPath: String = ""
    ): Long {
        return try {
            val values = ContentValues().apply {
                put(COL_BOOKING_USER,       userEmail.trim().lowercase())
                put(COL_BOOKING_BARBER,     barber)
                put(COL_BOOKING_SERVICE,    service)
                put(COL_BOOKING_DATE,       date)
                put(COL_BOOKING_TIME,       timeSlot)
                put(COL_BOOKING_AMOUNT,     totalAmount)
                put(COL_BOOKING_STATUS,     "Pending")
                put(COL_BOOKING_SCREENSHOT, screenshotPath)
            }
            writableDatabase.insert(TABLE_BOOKINGS, null, values)
        } catch (e: Exception) {
            Log.e("DB", "saveBooking: ${e.message}")
            -1L
        }
    }

    fun getBookedSlots(barber: String, date: String): List<String> {
        return try {
            val cursor = readableDatabase.query(
                TABLE_BOOKINGS, arrayOf(COL_BOOKING_TIME),
                "$COL_BOOKING_BARBER = ? AND $COL_BOOKING_DATE = ? AND $COL_BOOKING_STATUS = ?",
                arrayOf(barber, date, "Confirmed"), null, null, null
            )
            val slots = mutableListOf<String>()
            while (cursor.moveToNext()) {
                slots.add(cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKING_TIME)))
            }
            cursor.close()
            slots
        } catch (e: Exception) {
            Log.e("DB", "getBookedSlots: ${e.message}")
            emptyList()
        }
    }

    fun getBookingsByUser(userEmail: String): List<Map<String, String>> {
        return try {
            val cursor = readableDatabase.query(
                TABLE_BOOKINGS, null,
                "$COL_BOOKING_USER = ?", arrayOf(userEmail.trim().lowercase()),
                null, null, "$COL_BOOKING_ID DESC"
            )
            val list = mutableListOf<Map<String, String>>()
            while (cursor.moveToNext()) {
                list.add(mapOf(
                    "id"         to (cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKING_ID))         ?: ""),
                    "barber"     to (cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKING_BARBER))     ?: ""),
                    "service"    to (cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKING_SERVICE))    ?: ""),
                    "date"       to (cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKING_DATE))       ?: ""),
                    "time"       to (cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKING_TIME))       ?: ""),
                    "amount"     to (cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKING_AMOUNT))     ?: ""),
                    "status"     to (cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKING_STATUS))     ?: "Pending"),
                    "screenshot" to (cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKING_SCREENSHOT)) ?: "")
                ))
            }
            cursor.close()
            list
        } catch (e: Exception) {
            Log.e("DB", "getBookingsByUser: ${e.message}")
            emptyList()
        }
    }

    // ✅ FIXED: improved JOIN with NULLIF to guarantee username always shows
    fun getAllBookings(): List<Map<String, String>> {
        return try {
            val cursor = readableDatabase.rawQuery("""
                SELECT b.$COL_BOOKING_ID        AS id,
                       b.$COL_BOOKING_USER       AS email,
                       COALESCE(
                           NULLIF(TRIM(u.$COL_USER_NAME), ''),
                           NULLIF(TRIM(b.$COL_BOOKING_USER), ''),
                           'Unknown'
                       )                         AS username,
                       b.$COL_BOOKING_BARBER     AS barber,
                       b.$COL_BOOKING_SERVICE    AS service,
                       b.$COL_BOOKING_DATE       AS date,
                       b.$COL_BOOKING_TIME       AS time,
                       b.$COL_BOOKING_AMOUNT     AS amount,
                       b.$COL_BOOKING_STATUS     AS status,
                       b.$COL_BOOKING_SCREENSHOT AS screenshot
                FROM $TABLE_BOOKINGS b
                LEFT JOIN $TABLE_USERS u
                       ON TRIM(LOWER(u.$COL_USER_EMAIL)) = TRIM(LOWER(b.$COL_BOOKING_USER))
                ORDER BY b.$COL_BOOKING_ID DESC
            """.trimIndent(), null)
            val list = mutableListOf<Map<String, String>>()
            while (cursor.moveToNext()) {
                list.add(mapOf(
                    "id"         to (cursor.getString(cursor.getColumnIndexOrThrow("id"))         ?: ""),
                    "email"      to (cursor.getString(cursor.getColumnIndexOrThrow("email"))      ?: ""),
                    "username"   to (cursor.getString(cursor.getColumnIndexOrThrow("username"))   ?: ""),
                    "barber"     to (cursor.getString(cursor.getColumnIndexOrThrow("barber"))     ?: ""),
                    "service"    to (cursor.getString(cursor.getColumnIndexOrThrow("service"))    ?: ""),
                    "date"       to (cursor.getString(cursor.getColumnIndexOrThrow("date"))       ?: ""),
                    "time"       to (cursor.getString(cursor.getColumnIndexOrThrow("time"))       ?: ""),
                    "amount"     to (cursor.getString(cursor.getColumnIndexOrThrow("amount"))     ?: ""),
                    "status"     to (cursor.getString(cursor.getColumnIndexOrThrow("status"))     ?: "Pending"),
                    "screenshot" to (cursor.getString(cursor.getColumnIndexOrThrow("screenshot")) ?: "")
                ))
            }
            cursor.close()
            list
        } catch (e: Exception) {
            Log.e("DB", "getAllBookings: ${e.message}")
            emptyList()
        }
    }

    fun updateBookingStatus(bookingId: Long, newStatus: String): Boolean {
        return try {
            val values = ContentValues().apply { put(COL_BOOKING_STATUS, newStatus) }
            writableDatabase.update(
                TABLE_BOOKINGS, values,
                "$COL_BOOKING_ID = ?", arrayOf(bookingId.toString())
            ) > 0
        } catch (e: Exception) {
            Log.e("DB", "updateBookingStatus: ${e.message}")
            false
        }
    }

    fun saveOrder(
        userEmail: String, items: String, totalAmount: String,
        screenshotPath: String = ""
    ): Long {
        return try {
            val values = ContentValues().apply {
                put(COL_ORDER_USER,       userEmail.trim().lowercase())
                put(COL_ORDER_ITEMS,      items)
                put(COL_ORDER_TOTAL,      totalAmount)
                put(COL_ORDER_DATE,       SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()).format(Date()))
                put(COL_ORDER_STATUS,     "Confirmed")
                put(COL_ORDER_SCREENSHOT, screenshotPath)
            }
            writableDatabase.insert(TABLE_ORDERS, null, values)
        } catch (e: Exception) {
            Log.e("DB", "saveOrder: ${e.message}")
            -1L
        }
    }

    fun getOrdersByUser(userEmail: String): List<Map<String, String>> {
        return try {
            val cursor = readableDatabase.query(
                TABLE_ORDERS, null,
                "$COL_ORDER_USER = ?", arrayOf(userEmail.trim().lowercase()),
                null, null, "$COL_ORDER_ID DESC"
            )
            val list = mutableListOf<Map<String, String>>()
            while (cursor.moveToNext()) {
                val rawId = cursor.getString(cursor.getColumnIndexOrThrow(COL_ORDER_ID)) ?: "0"
                list.add(mapOf(
                    "id"         to "PO${rawId.padStart(4, '0')}",
                    "items"      to (cursor.getString(cursor.getColumnIndexOrThrow(COL_ORDER_ITEMS))      ?: ""),
                    "total"      to (cursor.getString(cursor.getColumnIndexOrThrow(COL_ORDER_TOTAL))      ?: ""),
                    "date"       to (cursor.getString(cursor.getColumnIndexOrThrow(COL_ORDER_DATE))       ?: ""),
                    "status"     to (cursor.getString(cursor.getColumnIndexOrThrow(COL_ORDER_STATUS))     ?: "Confirmed"),
                    "screenshot" to (cursor.getString(cursor.getColumnIndexOrThrow(COL_ORDER_SCREENSHOT)) ?: "")
                ))
            }
            cursor.close()
            list
        } catch (e: Exception) {
            Log.e("DB", "getOrdersByUser: ${e.message}")
            emptyList()
        }
    }

    fun getAllOrders(): List<Map<String, String>> {
        return try {
            val cursor = readableDatabase.rawQuery("""
                SELECT o.$COL_ORDER_ID        AS raw_id,
                       o.$COL_ORDER_USER       AS email,
                       COALESCE(
                           NULLIF(TRIM(u.$COL_USER_NAME), ''),
                           NULLIF(TRIM(o.$COL_ORDER_USER), ''),
                           'Unknown'
                       )                       AS customer,
                       o.$COL_ORDER_ITEMS      AS items,
                       o.$COL_ORDER_TOTAL      AS total,
                       o.$COL_ORDER_DATE       AS date,
                       o.$COL_ORDER_STATUS     AS status,
                       o.$COL_ORDER_SCREENSHOT AS screenshot
                FROM $TABLE_ORDERS o
                LEFT JOIN $TABLE_USERS u
                       ON TRIM(LOWER(u.$COL_USER_EMAIL)) = TRIM(LOWER(o.$COL_ORDER_USER))
                ORDER BY o.$COL_ORDER_ID DESC
            """.trimIndent(), null)
            val list = mutableListOf<Map<String, String>>()
            while (cursor.moveToNext()) {
                val rawId = cursor.getString(cursor.getColumnIndexOrThrow("raw_id")) ?: "0"
                list.add(mapOf(
                    "id"         to "PO${rawId.padStart(4, '0')}",
                    "email"      to (cursor.getString(cursor.getColumnIndexOrThrow("email"))      ?: ""),
                    "customer"   to (cursor.getString(cursor.getColumnIndexOrThrow("customer"))   ?: ""),
                    "items"      to (cursor.getString(cursor.getColumnIndexOrThrow("items"))      ?: ""),
                    "total"      to (cursor.getString(cursor.getColumnIndexOrThrow("total"))      ?: ""),
                    "date"       to (cursor.getString(cursor.getColumnIndexOrThrow("date"))       ?: ""),
                    "status"     to (cursor.getString(cursor.getColumnIndexOrThrow("status"))     ?: "Confirmed"),
                    "screenshot" to (cursor.getString(cursor.getColumnIndexOrThrow("screenshot")) ?: "")
                ))
            }
            cursor.close()
            list
        } catch (e: Exception) {
            Log.e("DB", "getAllOrders: ${e.message}")
            emptyList()
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String): Boolean {
        return try {
            val numericId = orderId.removePrefix("PO").trimStart('0').ifEmpty { "0" }
            val values = ContentValues().apply { put(COL_ORDER_STATUS, newStatus) }
            writableDatabase.update(
                TABLE_ORDERS, values,
                "$COL_ORDER_ID = ?", arrayOf(numericId)
            ) > 0
        } catch (e: Exception) {
            Log.e("DB", "updateOrderStatus: ${e.message}")
            false
        }
    }

    fun saveRating(
        userEmail: String, barber: String, score: Int,
        comment: String, service: String = ""
    ): Long {
        return try {
            val values = ContentValues().apply {
                put(COL_RATING_USER,    userEmail.trim().lowercase())
                put(COL_RATING_BARBER,  barber)
                put(COL_RATING_SCORE,   score)
                put(COL_RATING_COMMENT, comment)
                put(COL_RATING_DATE,    SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date()))
                put(COL_RATING_SERVICE, service)
            }
            writableDatabase.insert(TABLE_RATINGS, null, values)
        } catch (e: Exception) {
            Log.e("DB", "saveRating: ${e.message}")
            -1L
        }
    }

    fun hasRated(userEmail: String, barber: String, service: String): Boolean {
        return try {
            val cursor = readableDatabase.query(
                TABLE_RATINGS, null,
                "$COL_RATING_USER = ? AND $COL_RATING_BARBER = ? AND $COL_RATING_SERVICE = ?",
                arrayOf(userEmail.trim().lowercase(), barber, service),
                null, null, null
            )
            val exists = cursor.count > 0
            cursor.close()
            exists
        } catch (e: Exception) {
            Log.e("DB", "hasRated: ${e.message}")
            false
        }
    }

    fun getAllRatings(): List<Map<String, String>> {
        return try {
            val cursor = readableDatabase.rawQuery("""
                SELECT r.$COL_RATING_ID      AS id,
                       r.$COL_RATING_USER    AS user,
                       COALESCE(
                           NULLIF(TRIM(u.$COL_USER_NAME), ''),
                           NULLIF(TRIM(r.$COL_RATING_USER), ''),
                           'Unknown'
                       )                     AS display_name,
                       r.$COL_RATING_BARBER  AS barber,
                       r.$COL_RATING_SCORE   AS score,
                       r.$COL_RATING_COMMENT AS comment,
                       r.$COL_RATING_DATE    AS date,
                       r.$COL_RATING_SERVICE AS service
                FROM $TABLE_RATINGS r
                LEFT JOIN $TABLE_USERS u
                       ON TRIM(LOWER(u.$COL_USER_EMAIL)) = TRIM(LOWER(r.$COL_RATING_USER))
                ORDER BY r.$COL_RATING_ID DESC
            """.trimIndent(), null)
            val list = mutableListOf<Map<String, String>>()
            while (cursor.moveToNext()) {
                list.add(mapOf(
                    "id"       to (cursor.getString(cursor.getColumnIndexOrThrow("id"))           ?: ""),
                    "user"     to (cursor.getString(cursor.getColumnIndexOrThrow("user"))         ?: ""),
                    "customer" to (cursor.getString(cursor.getColumnIndexOrThrow("display_name")) ?: ""),
                    "barber"   to (cursor.getString(cursor.getColumnIndexOrThrow("barber"))       ?: ""),
                    "score"    to (cursor.getString(cursor.getColumnIndexOrThrow("score"))        ?: "0"),
                    "comment"  to (cursor.getString(cursor.getColumnIndexOrThrow("comment"))      ?: ""),
                    "date"     to (cursor.getString(cursor.getColumnIndexOrThrow("date"))         ?: ""),
                    "service"  to (cursor.getString(cursor.getColumnIndexOrThrow("service"))      ?: "")
                ))
            }
            cursor.close()
            list
        } catch (e: Exception) {
            Log.e("DB", "getAllRatings: ${e.message}")
            emptyList()
        }
    }

    fun getRatingsByBarber(barber: String): List<Map<String, String>> {
        return try {
            val cursor = readableDatabase.query(
                TABLE_RATINGS, null,
                "$COL_RATING_BARBER = ?", arrayOf(barber),
                null, null, "$COL_RATING_ID DESC"
            )
            val list = mutableListOf<Map<String, String>>()
            while (cursor.moveToNext()) {
                list.add(mapOf(
                    "user"    to (cursor.getString(cursor.getColumnIndexOrThrow(COL_RATING_USER))    ?: ""),
                    "score"   to (cursor.getString(cursor.getColumnIndexOrThrow(COL_RATING_SCORE))   ?: "0"),
                    "comment" to (cursor.getString(cursor.getColumnIndexOrThrow(COL_RATING_COMMENT)) ?: ""),
                    "date"    to (cursor.getString(cursor.getColumnIndexOrThrow(COL_RATING_DATE))    ?: "")
                ))
            }
            cursor.close()
            list
        } catch (e: Exception) {
            Log.e("DB", "getRatingsByBarber: ${e.message}")
            emptyList()
        }
    }

    fun getRatingsByUser(userEmail: String): List<Map<String, String>> {
        return try {
            val cursor = readableDatabase.query(
                TABLE_RATINGS, null,
                "$COL_RATING_USER = ?", arrayOf(userEmail.trim().lowercase()),
                null, null, "$COL_RATING_ID DESC"
            )
            val list = mutableListOf<Map<String, String>>()
            while (cursor.moveToNext()) {
                list.add(mapOf(
                    "barber"  to (cursor.getString(cursor.getColumnIndexOrThrow(COL_RATING_BARBER))  ?: ""),
                    "score"   to (cursor.getString(cursor.getColumnIndexOrThrow(COL_RATING_SCORE))   ?: "0"),
                    "comment" to (cursor.getString(cursor.getColumnIndexOrThrow(COL_RATING_COMMENT)) ?: ""),
                    "date"    to (cursor.getString(cursor.getColumnIndexOrThrow(COL_RATING_DATE))    ?: "")
                ))
            }
            cursor.close()
            list
        } catch (e: Exception) {
            Log.e("DB", "getRatingsByUser: ${e.message}")
            false as List<Map<String, String>>
        }
    }

    fun deleteRating(ratingId: String): Boolean {
        return try {
            writableDatabase.delete(TABLE_RATINGS, "$COL_RATING_ID = ?", arrayOf(ratingId)) > 0
        } catch (e: Exception) {
            Log.e("DB", "deleteRating: ${e.message}")
            false
        }
    }
}