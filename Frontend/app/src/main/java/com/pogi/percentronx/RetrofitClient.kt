@file:Suppress("unused")

package com.pogi.percentronx

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

object retrofitClient {

    const val baseUrl = "https://capstoneproject-production-2a06.up.railway.app/"
    private var applicationContext: Context? = null

    private val cookieStore = HashMap<String, MutableList<Cookie>>()

    fun initialize(context: Context) {
        applicationContext = context.applicationContext

        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val sessionId = sharedPreferences.getString("session_id", null)
        if (!sessionId.isNullOrEmpty()) {
            addSessionCookie(sessionId)
            Log.d("RetrofitClient", "Loaded saved session ID: $sessionId")
        }
    }

    fun addSessionCookie(sessionId: String) {
        try {
            val url = baseUrl.toHttpUrl()

            val cookie = Cookie.Builder()
                .name("session_id")
                .value(sessionId)
                .domain(url.host)
                .path("/")
                .build()

            val cookies = cookieStore[url.host] ?: mutableListOf()
            cookies.removeAll { it.name == "session_id" }
            cookies.add(cookie)

            cookieStore[url.host] = cookies

            applicationContext?.let {
                val sharedPreferences = it.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                sharedPreferences.edit().putString("session_id", sessionId).apply()
            }

            Log.d("RetrofitClient", "Session cookie added: $sessionId for host ${url.host}")
        } catch (e: Exception) {
            Log.e("RetrofitClient", "Error adding session cookie: ${e.message}", e)
        }
    }

    private fun getSessionId(): String? {
        try {
            val url = baseUrl.toHttpUrl()
            return cookieStore[url.host]?.find { it.name == "session_id" }?.value
        } catch (e: Exception) {
            Log.e("RetrofitClient", "Error getting session ID: ${e.message}", e)
            return null
        }
    }

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            Log.d("RetrofitClient", "Saving cookies from ${url.host}: $cookies")

            val existingCookies = cookieStore[url.host] ?: mutableListOf()

            for (cookie in cookies) {
                existingCookies.removeAll { it.name == cookie.name }
                existingCookies.add(cookie)

                if (cookie.name == "session_id") {
                    applicationContext?.let {
                        val sharedPreferences = it.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit().putString("session_id", cookie.value).apply()
                        Log.d("RetrofitClient", "Session ID saved to preferences: ${cookie.value}")
                    }
                }
            }

            cookieStore[url.host] = existingCookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val cookies = cookieStore[url.host] ?: mutableListOf()
            Log.d("RetrofitClient", "Loading cookies for ${url.host}: $cookies")
            return cookies
        }
    }

    class ChatMessageDeserializer : JsonDeserializer<ChatMessage> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): ChatMessage {
            val jsonObject = json.asJsonObject
            Log.d("Deserializer", "Raw JSON: $jsonObject")

            val id = jsonObject.get("id")?.asInt ?: 0
            val senderId = jsonObject.get("senderId")?.asInt ?: 0
            val receiverId = jsonObject.get("receiverId")?.asInt ?: 0
            val senderType = jsonObject.get("senderType")?.asString ?: "unknown"
            val content = jsonObject.get("content")?.asString ?: ""
            val timestamp = jsonObject.get("timestamp")?.asString ?: ""
            val isRead = jsonObject.get("isRead")?.asBoolean ?: false

            Log.d("Deserializer", "Parsed: id=$id, sender=$senderId, type=$senderType, content='$content'")

            return ChatMessage(
                id = id,
                senderId = senderId,
                receiverId = receiverId,
                senderType = senderType,
                content = content,
                timestamp = timestamp,
                isRead = isRead
            )
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val sessionInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()

        val sessionId = getSessionId() ?: run {
            applicationContext?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                ?.getString("session_id", null)
        }

        if (sessionId != null) {
            Log.d("RetrofitClient", "Adding session cookie to request: $sessionId")
        } else {
            Log.d("RetrofitClient", "No session cookie found for request")
        }

        val modifiedRequest = if (sessionId != null) {
            originalRequest.newBuilder()
                .header("Cookie", "session_id=$sessionId")
                .build()
        } else {
            originalRequest
        }

        chain.proceed(modifiedRequest)
    }

    private val gson = GsonBuilder()
        .setLenient()
        .registerTypeAdapter(ChatMessage::class.java, ChatMessageDeserializer())
        .create()


    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(sessionInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.MINUTES)
        .writeTimeout(60, TimeUnit.MINUTES)
        .cookieJar(cookieJar)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    fun getFullImageUrl(relativeUrl: String): String {
        if (relativeUrl.startsWith("http")) {
            return relativeUrl
        }

        val formattedUrl = if (!relativeUrl.startsWith("/")) "/$relativeUrl" else relativeUrl
        return baseUrl.removeSuffix("/") + formattedUrl
    }

    fun createHttpClient(
        connectTimeoutSec: Long = 60,
        readTimeoutMin: Long = 30,
        writeTimeoutMin: Long = 60
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(sessionInterceptor)
            .connectTimeout(connectTimeoutSec, TimeUnit.SECONDS)
            .readTimeout(readTimeoutMin, TimeUnit.MINUTES)
            .writeTimeout(writeTimeoutMin, TimeUnit.MINUTES)
            .cookieJar(cookieJar)
            .build()
    }
}