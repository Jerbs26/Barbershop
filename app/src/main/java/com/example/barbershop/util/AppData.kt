package com.example.barbershop.util

import android.content.Context
import android.util.Log

object AppData {

    // These hold the live data that the rest of the app reads from
    val bookingsList = mutableListOf<MutableMap<String, String>>()
    val ordersList   = mutableListOf<MutableMap<String, String>>()
    val feedbackList = mutableListOf<MutableMap<String, String>>()
    val barbersList  = mutableListOf<MutableMap<String, String>>()
    val servicesList = mutableListOf<MutableMap<String, String>>()   // now loaded from DB
    val productsList = mutableListOf<MutableMap<String, String>>()

    // Just an alias for reload
    fun load(context: Context, onDone: (() -> Unit)? = null) = reload(context, onDone)

    // Kicks off a background thread to reload everything, then calls onDone when finished
    fun reload(context: Context, onDone: (() -> Unit)? = null) {
        Thread {
            reloadSync(context)
            onDone?.invoke()
        }.start()
    }

    // Does the actual data loading synchronously
    fun reloadSync(context: Context) {
        try {
            val db = Database(context)

            // Load services first since bookings and other data depend on them
            try {
                val services = db.getAllServices()
                synchronized(servicesList) {
                    servicesList.clear()
                    services.forEach { row ->
                        servicesList.add(mutableMapOf(
                            "id"       to (row["id"]       ?: ""),
                            "name"     to (row["name"]     ?: ""),
                            "desc"     to (row["desc"]     ?: ""),
                            "price"    to (row["price"]    ?: ""),
                            "duration" to (row["duration"] ?: "60 minutes")
                        ))
                    }
                }
            } catch (e: Exception) { Log.e("AppData", "services reload failed: ${e.message}") }

            // Load barbers
            try {
                val barbers = db.getAllBarbers()
                synchronized(barbersList) {
                    barbersList.clear()
                    barbers.forEach { row ->
                        barbersList.add(mutableMapOf(
                            "id"        to (row["id"]        ?: ""),
                            "name"      to (row["name"]      ?: ""),
                            "specialty" to (row["specialty"] ?: ""),
                            "desc"      to (row["desc"]      ?: ""),
                            "exp"       to (row["exp"]       ?: ""),
                            "status"    to (row["status"]    ?: "Active")
                        ))
                    }
                }
            } catch (e: Exception) { Log.e("AppData", "barbers reload failed: ${e.message}") }

            // Load shop products
            try {
                val products = db.getAllProducts()
                synchronized(productsList) {
                    productsList.clear()
                    products.forEach { row ->
                        productsList.add(mutableMapOf(
                            "id"    to (row["id"]    ?: ""),
                            "name"  to (row["name"]  ?: ""),
                            "desc"  to (row["desc"]  ?: ""),
                            "price" to (row["price"] ?: ""),
                            "stock" to (row["stock"] ?: "0")
                        ))
                    }
                }
            } catch (e: Exception) { Log.e("AppData", "products reload failed: ${e.message}") }

            // Load bookings
            try {
                val bookings = db.getAllBookings()
                synchronized(bookingsList) {
                    bookingsList.clear()
                    bookings.forEach { row ->
                        val email    = row["email"] ?: ""
                        val username = row["username"]?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
                        bookingsList.add(mutableMapOf(
                            "id"         to (row["id"]         ?: ""),
                            "username"   to username,
                            "email"      to email,
                            "barber"     to (row["barber"]     ?: ""),
                            "service"    to (row["service"]    ?: ""),
                            "date"       to (row["date"]       ?: ""),
                            "time"       to (row["time"]       ?: ""),
                            "amount"     to (row["amount"]     ?: ""),
                            "status"     to (row["status"]     ?: "Pending"),
                            "screenshot" to (row["screenshot"] ?: "")
                        ))
                    }
                }
            } catch (e: Exception) { Log.e("AppData", "bookings reload failed: ${e.message}") }

            // Load orders
            try {
                val orders = db.getAllOrders()
                synchronized(ordersList) {
                    ordersList.clear()
                    orders.forEach { row ->
                        val email    = row["email"] ?: ""
                        val customer = row["customer"]?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
                        ordersList.add(mutableMapOf(
                            "id"         to (row["id"]         ?: ""),
                            "customer"   to customer,
                            "email"      to email,
                            "items"      to (row["items"]      ?: ""),
                            "total"      to (row["total"]      ?: ""),
                            "date"       to (row["date"]       ?: ""),
                            // Orders come in as Confirmed by default, not Pending
                            "status"     to (row["status"]     ?: "Confirmed"),
                            "screenshot" to (row["screenshot"] ?: "")
                        ))
                    }
                }
            } catch (e: Exception) { Log.e("AppData", "orders reload failed: ${e.message}") }

            // Load ratings/feedback
            try {
                val ratings = db.getAllRatings()
                synchronized(feedbackList) {
                    feedbackList.clear()
                    ratings.forEach { row ->
                        feedbackList.add(mutableMapOf(
                            "id"       to (row["id"]       ?: ""),
                            "date"     to (row["date"]     ?: ""),
                            "customer" to (row["customer"] ?: row["user"] ?: ""),
                            "user"     to (row["user"]     ?: ""),
                            "barber"   to (row["barber"]   ?: ""),
                            "service"  to (row["service"]  ?: ""),
                            "rating"   to (row["score"]    ?: "0"),
                            "comment"  to (row["comment"]  ?: "")
                        ))
                    }
                }
            } catch (e: Exception) { Log.e("AppData", "feedback reload failed: ${e.message}") }

            db.close()

        } catch (e: Exception) {
            // If we can't even open the DB, log it and bail out gracefully
            Log.e("AppData", "DB connection failed: ${e.message}")
        }
    }

    // Updates the booking status in memory immediately, then persists it to the DB in the background
    fun updateBookingStatus(context: Context, bookingId: String, newStatus: String) {
        // Update the in-memory list right away so the UI reflects it instantly
        bookingsList.find { it["id"] == bookingId }?.set("status", newStatus)
        Thread {
            try { Database(context).use { it.updateBookingStatus(bookingId.toLongOrNull() ?: return@Thread, newStatus) } }
            catch (e: Exception) { Log.e("AppData", "updateBookingStatus: ${e.message}") }
        }.start()
    }

    // Same pattern as updateBookingStatus
    fun updateOrderStatus(context: Context, orderId: String, newStatus: String) {
        ordersList.find { it["id"] == orderId }?.set("status", newStatus)
        Thread {
            try { Database(context).use { it.updateOrderStatus(orderId, newStatus) } }
            catch (e: Exception) { Log.e("AppData", "updateOrderStatus: ${e.message}") }
        }.start()
    }
}