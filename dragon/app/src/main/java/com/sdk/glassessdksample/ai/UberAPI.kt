package com.sdk.glassessdksample

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import kotlin.coroutines.resume

class UberAPI (
    private val context: Context,
    private val clientId: String,
    private val clientSecret: String,
    private val redirectUri: String
) {
    private val client = OkHttpClient()
    private var accessToken: String? = null

    fun getUberAuthUrl(): String {
        return "https://login.uber.com/oauth/v2/authorize?" +
                "client_id=$clientId&response_type=code&scope=profile%20request&redirect_uri=$redirectUri"
    }

    fun handleRedirectAndStoreToken(code: String): Boolean {
        val token = exchangeCodeForToken(code)
        return if (token != null) {
            accessToken = token
            true
        } else {
            false
        }
    }

    fun exchangeCodeForToken(code: String): String? {
        val body = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("grant_type", "authorization_code")
            .add("redirect_uri", redirectUri)
            .add("code", code)
            .build()

        val request = Request.Builder()
            .url("https://login.uber.com/oauth/v2/token")
            .post(body)
            .build()

        val response = OkHttpClient().newCall(request).execute()
        val json = JSONObject(response.body?.string() ?: return null)
        return json.optString("access_token", null)
    }

    suspend fun orderUber(destination: String): String {
        if (accessToken == null) {
            return "AUTH_REQUIRED:${getUberAuthUrl()}"
        }

        val pickupLocation = getCurrentLocationAsJson() ?: return "Could not get present location"

        val productsUrl =
            "https://api.uber.com/v1.2/products?latitude=${pickupLocation.getDouble("latitude")}&longitude=${pickupLocation.getDouble("longitude")}"

        val productsRequest = Request.Builder()
            .url(productsUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val productsResponse = client.newCall(productsRequest).execute()
        if (!productsResponse.isSuccessful) {
            return "Uber products error: ${productsResponse.code}"
        }

        val productsJson = JSONObject(productsResponse.body?.string() ?: return "No products found")
        if (productsJson.getJSONArray("products").length() == 0) {
            return "No Uber rides available at your location."
        }

        val productId = productsJson.getJSONArray("products").getJSONObject(0).getString("product_id")

        val body = JSONObject().apply {
            put("start_latitude", pickupLocation.getDouble("latitude"))
            put("start_longitude", pickupLocation.getDouble("longitude"))
            put("end_location", destination)
            put("product_id", productId)
        }

        val request = Request.Builder()
            .url("https://api.uber.com/v1.2/requests")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return "No response from Uber."
            if (response.isSuccessful) {
                Log.d("AIClient", "Ordering Uber to $destination")
                val json = JSONObject(responseBody)
                return "Uber ride ordered. Status: ${json.optString("status")}."
            } else {
                return "Uber API error: ${response.code} - $responseBody"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Failed to order Uber: ${e.message}"
        }
        return "Uber ride ordered."
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationAsJson(): JSONObject? {
        return suspendCancellableCoroutine { cont ->
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val address = addresses?.firstOrNull()

                        val locJson = JSONObject().apply {
                            if (address != null) {
                                put("address", address.getAddressLine(0) ?: "Unknown location")
                                put("city", address.locality ?: "")
                                put("state", address.adminArea ?: "")
                                put("country", address.countryName ?: "")
                                put("postal_code", address.postalCode ?: "")
                            } else {
                                put("address", "Unknown location")
                            }
                        }
                        cont.resume(locJson)
                        Log.d("Ossian", "Location data: ${locJson}")
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        cont.resume(null)
                    }
                } else {
                    cont.resume(null)
                }
            }.addOnFailureListener {
                cont.resume(null)
            }
        }
    }
}