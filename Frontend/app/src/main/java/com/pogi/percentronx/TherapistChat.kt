
package com.pogi.percentronx

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessageResponse(val isSuccess: Boolean, val messageId: Int, val errorMessage: String?)

data class ChatMessage(
    val id: Int,
    val senderId: Int,
    val receiverId: Int,
    val senderType: String,
    val content: String,
    val timestamp: String,
    val isRead: Boolean
) {
    fun isFromCurrentUser(): Boolean {
        return senderType == "user"
    }

    fun getFormattedTime(): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(timestamp) ?: return ""
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            return outputFormat.format(date)
        } catch (e: Exception) {
            return timestamp
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TherapistChatScreen(
    navController: NavController,
    therapistId: Int,
    apiService: ApiService
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userId = sharedPreferences.getInt("user_id", 0)

    Log.d("ChatScreen", "Starting chat with therapist ID: $therapistId, user ID: $userId")

    var therapist by remember { mutableStateOf<Therapist?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var loadAttemptCount by remember { mutableIntStateOf(0) }
    var sendMessageJob by remember { mutableStateOf<Job?>(null) }
    val scrollState = rememberLazyListState()

    suspend fun sendMessageDirectHttpV2(
        therapistId: Int,
        content: String,
        clientMessageId: String,
        appContext: Context
    ): ChatMessageResponse {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val sessionId = prefs.getString("session_id", null)

                if (sessionId.isNullOrEmpty()) {
                    Log.e("DirectHttp", "No session ID available")
                    return@withContext ChatMessageResponse(false, 0, "No session ID available")
                }

                retrofitClient.addSessionCookie(sessionId)

                val jsonBody = JSONObject().apply {
                    put("therapist_id", therapistId)
                    put("content", content)
                    put("client_message_id", clientMessageId)
                }

                val client = retrofitClient.createHttpClient()
                val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

                Log.d("DirectHttp", "Request body: $jsonBody")

                val request = okhttp3.Request.Builder()
                    .url("${retrofitClient.baseUrl}messages/send-to-therapist-v2")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                Log.d("DirectHttp", "Response code: ${response.code}")
                Log.d("DirectHttp", "Response body: $responseBody")

                if (response.isSuccessful) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val id = jsonResponse.optInt("id", 0)
                        val status = jsonResponse.optString("status", "")
                        val message = jsonResponse.optString("message", "")

                        if (id > 0 && status == "valid") {
                            return@withContext ChatMessageResponse(true, id, null)
                        } else {
                            return@withContext ChatMessageResponse(false, 0, message)
                        }
                    } catch (e: JSONException) {
                        Log.e("DirectHttp", "Error parsing response: ${e.message}")
                        return@withContext ChatMessageResponse(false, 0, "Invalid response format")
                    }
                } else {
                    return@withContext ChatMessageResponse(false, 0, "Server returned code ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("DirectHttp", "Error sending message: ${e.message}")
                return@withContext ChatMessageResponse(false, 0, e.message ?: "Unknown error")
            }
        }
    }

    val sendMessage: () -> Unit = sendMessage@{
        if (messageText.isBlank() || isSending) return@sendMessage

        val content = messageText.trim()
        messageText = ""
        isSending = true
        focusManager.clearFocus()

        sendMessageJob = coroutineScope.launch {
            try {
                val tempMessage = ChatMessage(
                    id = -1,
                    senderId = userId,
                    receiverId = therapistId,
                    senderType = "user",
                    content = content,
                    timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        .format(Date()),
                    isRead = false
                )

                messages = messages + tempMessage

                try {
                    val request = MessageToTherapistRequest(
                        therapistId = therapistId,
                        content = content
                    )

                    val response = apiService.sendMessageToTherapist(request)

                    if (response.status == "valid" && response.id > 0) {
                        delay(500)
                        val updatedMessages = apiService.getTherapistMessages(therapistId)
                        messages = updatedMessages

                        if (updatedMessages.isNotEmpty()) {
                            scrollState.animateScrollToItem(updatedMessages.size - 1)
                        }
                    } else {
                        throw Exception("Invalid response from server")
                    }
                } catch (e: Exception) {
                    Log.e("MessageSend", "Error sending message: ${e.message}")

                    messages = messages.map { msg ->
                        if (msg == tempMessage) {
                            msg.copy(content = "$content (Failed to send)")
                        } else msg
                    }

                    Toast.makeText(
                        context,
                        "Failed to send message: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("MessageSend", "Error: ${e.message}")
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSending = false
            }
        }
    }

    @Composable
    fun DisplayChatMessage(message: ChatMessage, isFromUser: Boolean) {
        val displayContent = message.content.replace(Regex("\\[client-id:[^]]+]"), "")

        val formattedTime = message.getFormattedTime()

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isFromUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isFromUser) 16.dp else 4.dp,
                            bottomEnd = if (isFromUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isFromUser)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = displayContent,
                    color = if (isFromUser)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }

    LaunchedEffect(therapistId) {
        try {
            isLoading = true
            Log.d("ChatScreen", "Loading therapist details for ID: $therapistId")
            val therapistData = apiService.getTherapistDetails(therapistId)
            Log.d("ChatScreen", "Therapist data received: ${therapistData.first_name} ${therapistData.last_name}")
            therapist = therapistData
            isLoading = false
        } catch (e: Exception) {
            Log.e("ChatScreen", "Error loading therapist details", e)
            errorMessage = "Error loading therapist details: ${e.message}"
            isLoading = false
        }
    }

    LaunchedEffect(therapistId, loadAttemptCount) {
        if (loadAttemptCount > 3) {
            Log.d("ChatScreen", "Too many load attempts, using test messages")
            if (messages.isEmpty()) {
            }
            return@LaunchedEffect
        }

        try {
            Log.d("ChatScreen", "Loading messages for therapist $therapistId (attempt $loadAttemptCount)")
            val chatMessages = apiService.getTherapistMessages(therapistId)

            Log.d("ChatScreen", "Loaded ${chatMessages.size} messages")
            chatMessages.forEachIndexed { index, message ->
                Log.d("ChatScreen", "Message $index: id=${message.id}, senderType=${message.senderType}, content=${message.content}")
            }

            if (chatMessages.isNotEmpty()) {
                messages = chatMessages
            } else if (loadAttemptCount > 0) {
                Log.d("ChatScreen", "No messages returned after retry, using test messages")
            }
        } catch (e: Exception) {
            Log.e("ChatScreen", "Error loading messages: ${e.message}", e)
            errorMessage = "Error loading messages: ${e.message}"

            loadAttemptCount += 1
        }
    }

    LaunchedEffect(Unit) {
        delay(5000)
        if (messages.isEmpty() && errorMessage == null) {
            Log.d("ChatScreen", "No messages loaded after 5 seconds, using test messages")
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        therapist?.let { theTherapist ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                val imageUrl = if (theTherapist.photoUrl.isNotEmpty()) {
                                    retrofitClient.getFullImageUrl(theTherapist.photoUrl)
                                } else {
                                    retrofitClient.getFullImageUrl("/static/assets/images/user/avatar-1.jpg")
                                }

                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Therapist photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(id = R.drawable.resource_new),
                                    fallback = painterResource(id = R.drawable.resource_new)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                val firstName = theTherapist.first_name
                                val lastName = theTherapist.last_name

                                if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                    Text(
                                        text = "Dr. $firstName $lastName",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                } else {
                                    Text(
                                        text = "Therapist",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }

                                Text(
                                    text = if (isLoading) "Loading..." else "Online",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isLoading)
                                        Color.Gray
                                    else
                                        Color(0xFF4CAF50)
                                )
                            }
                        } ?: run {
                            Text("Chat")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isLoading && messages.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (errorMessage != null && messages.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = errorMessage ?: "An unknown error occurred",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                errorMessage = null
                                loadAttemptCount += 1
                            }) {
                                Text("Retry")
                            }
                        }
                    }
                } else if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "No Messages",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No messages yet. Start the conversation!",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        state = scrollState,
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(messages) { message ->
                            DisplayChatMessage(
                                message = message,
                                isFromUser = message.isFromCurrentUser()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Type a message...") },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (messageText.isNotBlank() && !isSending) {
                                        sendMessage()
                                    }
                                }
                            ),
                            maxLines = 4
                        )

                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank() && !isSending) {
                                    sendMessage()
                                }
                            },
                            enabled = messageText.isNotBlank() && !isSending
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    isFromUser: Boolean
) {
    val formattedTime = message.getFormattedTime()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isFromUser) 16.dp else 4.dp,
                        bottomEnd = if (isFromUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isFromUser)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = if (isFromUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}