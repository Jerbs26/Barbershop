package com.example.barbershop.util

import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object OtpEmailSender {

    // Sender credentials — uses a Gmail app password so 2FA doesn't block the send
    private val SENDER_EMAIL    = "ardientejerby26@gmail.com"
    private val SENDER_PASSWORD = "yvzupxqjuonbthex"

    // Generates a random 6-digit OTP for account verification
    fun generateOtp(): String {
        return (100000..999999).random().toString()
    }

    fun sendOtp(recipientEmail: String, otp: String) {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.ssl.trust", "smtp.gmail.com")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
        })

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(SENDER_EMAIL))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
            subject = "Your Barber Shop OTP Code"
            setText("Your OTP verification code is: $otp\n\nThis code expires in 5 minutes.")
        }

        Transport.send(message)
    }

    fun sendBookingConfirmation(
        recipientEmail: String,
        username: String,
        barber: String,
        service: String,
        date: String,
        timeSlot: String,
        amount: String
    ) {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.ssl.trust", "smtp.gmail.com")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
        })

        // HTML email body
        val htmlBody = """
            <html>
            <body style="font-family: Arial, sans-serif; font-size: 15px; color: #222;">
                <p>Hello <b>$username</b>,</p>

                <p>Great news! Your booking has been <b>CONFIRMED</b> by our admin.</p>

                <p><b>Booking Details:</b></p>
                <hr style="width:320px; margin-left:0;">
                <table style="border-collapse: collapse;">
                    <tr><td style="padding: 3px 8px 3px 0;"><b>Barber</b></td><td>: $barber</td></tr>
                    <tr><td style="padding: 3px 8px 3px 0;"><b>Service</b></td><td>: $service</td></tr>
                    <tr><td style="padding: 3px 8px 3px 0;"><b>Date</b></td><td>: $date</td></tr>
                    <tr><td style="padding: 3px 8px 3px 0;"><b>Time</b></td><td>: $timeSlot</td></tr>
                    <tr><td style="padding: 3px 8px 3px 0;"><b>Amount</b></td><td>: <b>$amount</b></td></tr>
                </table>
                <hr style="width:320px; margin-left:0;">

                <p>Please <b>arrive 5 minutes early</b>. See you soon!</p>

                <p>— <b>Urban Razor Team</b></p>
            </body>
            </html>
        """.trimIndent()

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(SENDER_EMAIL))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
            subject = "Your Booking is Confirmed – Urban Razor"
            // Make sure to set content type as HTML, not plain text
            setContent(htmlBody, "text/html; charset=utf-8")
        }

        Transport.send(message)
    }

    fun sendOrderReadyNotification(
        recipientEmail: String,
        customerName: String,
        orderId: String,
        items: String,
        total: String
    ) {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.ssl.trust", "smtp.gmail.com")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
        })

        val htmlBody = """
            <html>
            <body style="font-family: Arial, sans-serif; font-size: 15px; color: #222;">
                <p>Hello <b>$customerName</b>,</p>

                <p>Your order is now <b>READY FOR PICKUP!</b></p>

                <p><b>Order Details:</b></p>
                <hr style="width:320px; margin-left:0;">
                <table style="border-collapse: collapse;">
                    <tr><td style="padding: 3px 8px 3px 0;"><b>Order ID</b></td><td>: $orderId</td></tr>
                    <tr><td style="padding: 3px 8px 3px 0;"><b>Items</b></td><td>: $items</td></tr>
                    <tr><td style="padding: 3px 8px 3px 0;"><b>Total</b></td><td>: <b>$total</b></td></tr>
                </table>
                <hr style="width:320px; margin-left:0;">

                <p>Please <b>visit our shop</b> to collect your order.</p>

                <p>— <b>The Urban Razor Team</b></p>
            </body>
            </html>
        """.trimIndent()

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(SENDER_EMAIL))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
            subject = "Your Order is Ready for Pickup – Urban Razor "
            setContent(htmlBody, "text/html; charset=utf-8")
        }

        Transport.send(message)
    }
}