package com.example.securekeep.intruderdetection.IntruderServices

import android.util.Log
import com.mailjet.client.ClientOptions
import com.mailjet.client.MailjetClient
import com.mailjet.client.MailjetRequest
import com.mailjet.client.MailjetResponse

object MailjetService {

    // Initialize the Mailjet client with your API credentials
    private const val API_KEY = "your_api_key"
    private const val API_SECRET = "your_api_secret_key"

    // Public Mailjet client instance
    private val client: MailjetClient by lazy {
        MailjetClient(
            ClientOptions.builder()
                .apiKey(API_KEY)
                .apiSecretKey(API_SECRET)
                .build()
        )
    }

    /**
     * Sends an email using the Mailjet client.
     *
     * @param request MailjetRequest containing the email details.
     * @return MailjetResponse? The response from Mailjet.
     */
    fun sendEmail(request: MailjetRequest): MailjetResponse? {
        return try {
            val response = client.post(request)
            if (response.status == 200) {
                Log.d("MailjetService", "Email sent successfully: ${response.data}")
            } else {
                Log.e("MailjetService", "Failed to send email: Status ${response.status}, Data: ${response.data}")
            }
            response
        } catch (e: Exception) {
            Log.e("MailjetService", "Exception occurred: ", e) // Log full stack trace
            null
        }
    }
}
