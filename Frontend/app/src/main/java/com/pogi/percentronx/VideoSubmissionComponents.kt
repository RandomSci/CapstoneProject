@file:Suppress("UNUSED_VARIABLE", "SpellCheckingInspection", "unused")

package com.pogi.percentronx

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun VideoFileIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Icon(
        imageVector = Icons.Default.FavoriteBorder,
        contentDescription = "Video File",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun PlayCircleFilledIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.3f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play Video",
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

fun formatFeedbackTime(dateString: String): String {
    try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateString) ?: return "recently"
        val now = Date()
        val diff = now.time - date.time

        return when {
            diff < 1000 * 60 -> "just now"
            diff < 1000 * 60 * 60 -> "${diff / (1000 * 60)} minutes ago"
            diff < 1000 * 60 * 60 * 24 -> "${diff / (1000 * 60 * 60)} hours ago"
            diff < 1000 * 60 * 60 * 24 * 7 -> "${diff / (1000 * 60 * 60 * 24)} days ago"
            else -> {
                val outputFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                "on ${outputFormat.format(date)}"
            }
        }
    } catch (e: Exception) {
        Log.e("Dashboard", "Error formatting date: ${e.message}")
        return "recently"
    }
}

@SuppressLint("ComposableNaming")
@Composable
fun VideoStatusIcon(
    status: String,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
): ImageVector {
    return when (status) {
        "Pending" -> Icons.Default.Check
        "Reviewed", "Feedback Provided" -> Icons.Default.CheckCircle
        else -> Icons.Default.Info
    }
}

@SuppressLint("ComposableNaming")
@Composable
fun VideoStatusColor(status: String): Color {
    return when (status) {
        "Pending" -> MaterialTheme.colorScheme.primary
        "Reviewed", "Feedback Provided" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun VideoUploadDialog(
    exercise: Exercise,
    planId: Int,
    onDismiss: () -> Unit,
    onVideoUploaded: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = retrofitClient.instance

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var notes by remember { mutableStateOf("") }
    var tempVideoFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var videoDuration by remember { mutableStateOf<Long?>(null) }
    var videoFileSize by remember { mutableStateOf<Long?>(null) }


    val animatedProgress by animateFloatAsState(
        targetValue = uploadProgress,
        label = "UploadProgress"
    )


    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        videoUri = uri
        errorMessage = null 
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {

                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)


                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val duration = durationStr?.toLongOrNull() ?: 0L
                    videoDuration = duration / 1000 


                    val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    videoFileSize = fileDescriptor?.statSize
                    fileDescriptor?.close()

                    withContext(Dispatchers.Main) {
                        Log.d("VideoUpload", "Selected video: duration=${videoDuration}s, size=${videoFileSize?.let { it / (1024 * 1024) }}MB")
                    }

                    retriever.release()
                } catch (e: Exception) {
                    Log.e("VideoUpload", "Error getting video metadata", e)
                }
            }
        }

        Log.d("VideoUpload", "Video selected from gallery: $uri")
    }


    fun uploadVideo() {
        if (videoUri == null) {
            Toast.makeText(context, "Please select or record a video first", Toast.LENGTH_SHORT).show()
            return
        }

        if (notes.isBlank()) {
            Toast.makeText(context, "Please add notes for your therapist", Toast.LENGTH_SHORT).show()
            return
        }


        if (videoFileSize != null && videoFileSize!! > 500 * 1024 * 1024) { 
            errorMessage = "Warning: This video is very large (${videoFileSize!! / (1024 * 1024)}MB). Upload may take a long time. Consider using a shorter or lower quality video."
            Toast.makeText(
                context,
                "Warning: This is a large video file. Upload may take a while.",
                Toast.LENGTH_LONG
            ).show()
        }

        isUploading = true
        uploadProgress = 0f
        errorMessage = null

        coroutineScope.launch {
            try {

                val contentType = context.contentResolver.getType(videoUri!!) ?: "video/*"


                val videoPart = MultipartBody.Part.createFormData(
                    "video",
                    "video_${System.currentTimeMillis()}.mp4",
                    ProgressRequestBody(
                        context = context,
                        uri = videoUri!!,
                        contentType = contentType,
                        onProgressUpdate = { progress ->

                            uploadProgress = progress
                        }
                    )
                )


                val exerciseIdPart = exercise.exerciseId.toString()
                    .toRequestBody("text/plain".toMediaTypeOrNull())
                val planIdPart = planId.toString()
                    .toRequestBody("text/plain".toMediaTypeOrNull())
                val notesPart = notes
                    .toRequestBody("text/plain".toMediaTypeOrNull())


                Log.d("VideoUpload", "Making API call with params: exercise_id=${exercise.exerciseId}, plan_id=${planId}")
                Log.d("VideoUpload", "Video details: size=${videoFileSize?.let { it / (1024 * 1024) }}MB, duration=${videoDuration}s")

                val response = withContext(Dispatchers.IO) {
                    try {
                        val customClient = if (videoFileSize != null && videoFileSize!! > 100 * 1024 * 1024) {
                            val connectTimeout = 90L
                            val readTimeout = 60L
                            val writeTimeout = 90L

                            retrofitClient.createHttpClient(
                                connectTimeoutSec = connectTimeout,
                                readTimeoutMin = readTimeout,
                                writeTimeoutMin = writeTimeout
                            )
                        } else {
                            null
                        }

                        if (customClient != null) {
                            Log.d("VideoUpload", "Using custom client with extended timeouts for large video")
                            Retrofit.Builder()
                                .baseUrl(retrofitClient.baseUrl)
                                .client(customClient)
                                .addConverterFactory(GsonConverterFactory.create())
                                .build()
                                .create(ApiService::class.java)
                                .uploadExerciseVideo(
                                    exerciseId = exerciseIdPart,
                                    treatmentPlanId = planIdPart,
                                    notes = notesPart,
                                    video = videoPart
                                )
                        } else {

                            apiService.uploadExerciseVideo(
                                exerciseId = exerciseIdPart,
                                treatmentPlanId = planIdPart,
                                notes = notesPart,
                                video = videoPart
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("VideoUpload", "API call failed", e)
                        errorMessage = "Upload failed: ${e.message}"
                        null
                    }
                }


                if (response != null && response.status == "success") {
                    Log.d("VideoUpload", "Upload successful: ${response.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Video uploaded successfully for review!",
                            Toast.LENGTH_LONG
                        ).show()

                        delay(500)
                        onVideoUploaded()
                        onDismiss()
                    }
                } else {
                    val errorMsg = response?.message ?: "Unknown error occurred"
                    Log.e("VideoUpload", "Upload failed: $errorMsg")
                    errorMessage = "Upload failed: $errorMsg"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Upload failed: $errorMsg",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("VideoUpload", "Error uploading video", e)
                e.printStackTrace()
                errorMessage = "Error: ${e.message}"
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error uploading video: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                isUploading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isUploading) onDismiss()
        },
        title = {
            Column {
                Text(
                    "Upload Exercise Video",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "Exercise: ${exercise.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Upload a video of yourself performing this exercise for your therapist to review and provide feedback.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (videoUri != null) {

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(videoUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Video thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )


                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video selected",
                            modifier = Modifier.size(48.dp),
                            tint = Color.White.copy(alpha = 0.7f)
                        )


                        if (videoDuration != null || videoFileSize != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.7f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(4.dp)
                            ) {
                                val duration = videoDuration?.let {
                                    val minutes = it / 60
                                    val seconds = it % 60
                                    if (minutes > 0) "$minutes min $seconds sec" else "$seconds sec"
                                } ?: "Unknown"

                                val size = videoFileSize?.let {
                                    val mb = it / (1024 * 1024)
                                    "$mb MB"
                                } ?: "Unknown"

                                Text(
                                    "$duration • $size",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {

                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Select video",
                            modifier = Modifier.size(48.dp),
                            tint = Color.White.copy(alpha = 0.5f)
                        )

                        Text(
                            "No video selected",
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch("video/*")
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null
                        )
                        Text("Gallery")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))


                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes for your therapist (Required)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    isError = notes.isBlank() && videoUri != null
                )

                if (notes.isBlank() && videoUri != null) {
                    Text(
                        "Please provide notes for your therapist",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))


                errorMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }


                AnimatedVisibility(visible = isUploading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Uploading video... ${(uploadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = animatedProgress,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))


                        if (videoFileSize != null && videoFileSize!! > 50 * 1024 * 1024) { 
                            Text(
                                "Large video - upload may take several minutes. Please keep the app open.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { uploadVideo() },
                enabled = videoUri != null && !isUploading && notes.isNotBlank()
            ) {
                Text("Upload Video")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoUploadScreen(
    exerciseId: Int,
    planId: Int,
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = retrofitClient.instance

    var exercise by remember { mutableStateOf<Exercise?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(key1 = exerciseId) {
        try {
            isLoading = true
            val exerciseDetails = withContext(Dispatchers.IO) {
                apiService.getExerciseDetails(exerciseId)
            }

            exercise = Exercise(
                exerciseId = exerciseDetails.exerciseId,
                planExerciseId = planId,
                name = exerciseDetails.name,
                description = exerciseDetails.description,
                videoUrl = exerciseDetails.videoUrl,
                imageUrl = null,
                videoType = exerciseDetails.videoType,
                sets = exerciseDetails.planInstances.firstOrNull()?.sets ?: 0,
                repetitions = exerciseDetails.planInstances.firstOrNull()?.repetitions ?: 0,
                frequency = exerciseDetails.planInstances.firstOrNull()?.frequency ?: "",
                duration = exerciseDetails.duration,
                completed = exerciseDetails.planInstances.firstOrNull()?.completed ?: false,
                thumbnailUrl = exerciseDetails.thumbnailUrl
            )
            isLoading = false
        } catch (e: Exception) {
            Log.e("VideoUpload", "Error loading exercise details: ${e.message}")
            errorMessage = "Failed to load exercise details: ${e.message}"
            isLoading = false
        }
    }

    var showUploadDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Exercise Video") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            errorMessage!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                exercise != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            exercise!!.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "${exercise!!.sets} sets × ${exercise!!.repetitions} reps",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(24.dp))


                        if (exercise!!.videoUrl != null) {
                            Text(
                                "Reference Video",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                                    .clickable {

                                        exercise!!.videoUrl?.let {
                                            navController.navigateToVideo(
                                                it,
                                                exercise!!.videoType
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (exercise!!.thumbnailUrl != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(exercise!!.thumbnailUrl?.let {
                                                retrofitClient.getFullImageUrl(
                                                    it
                                                )
                                            })
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Video thumbnail",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }


                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play video",
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "Watch the reference video first to ensure you understand the correct form.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                        }


                        if (exercise!!.description.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        "Description",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        exercise!!.description,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }


                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "Tips for Recording",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Column {
                                    TipItem("Find good lighting so your therapist can clearly see your form")
                                    TipItem("Position your camera to show your full body and range of motion")
                                    TipItem("Perform the exercise at a controlled pace")
                                    TipItem("Record 1-2 repetitions of the exercise for review")
                                    TipItem("Explain any difficulty or pain you experience while recording")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))


                        Button(
                            onClick = { showUploadDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "UPLOAD VIDEO NOW",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }


    if (showUploadDialog && exercise != null) {
        VideoUploadDialog(
            exercise = exercise!!,
            planId = planId,
            onDismiss = { showUploadDialog = false },
            onVideoUploaded = {

                navController.popBackStack()
            }
        )
    }
}


@Composable
fun rememberPermissionState(permission: String): PermissionState {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val status = if (ContextCompat.checkSelfPermission(context, permission) ==
        PackageManager.PERMISSION_GRANTED) {
        PermissionStatus.Granted
    } else {
        PermissionStatus.Denied
    }

    return remember { PermissionState(status, launcher, permission) }
}

class PermissionState(
    var status: PermissionStatus,
    private val launcher: ManagedActivityResultLauncher<String, Boolean>,
    private val permission: String
) {
    fun launchPermissionRequest() {
        launcher.launch(permission)
    }
}

sealed class PermissionStatus {
    object Granted : PermissionStatus()
    object Denied : PermissionStatus()

    val isGranted: Boolean
        get() = this is Granted
}

object FileUtils {
    /**
     * Get a file from a URI using efficient buffered copying
     * @param context Android context
     * @param uri Source URI
     * @param progressCallback Optional callback for progress updates
     * @return The resulting File or null if failed
     */
    fun getFileFromUri(
        context: Context,
        uri: Uri,
        progressCallback: ((Float) -> Unit)? = null
    ): File? {
        try {

            val contentType = context.contentResolver.getType(uri)
            var fileSize = -1L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex("_size")
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }


            val extension = when {
                contentType?.contains("mp4") == true -> ".mp4"
                contentType?.contains("quicktime") == true -> ".mov"
                contentType?.contains("avi") == true -> ".avi"
                contentType?.contains("x-matroska") == true -> ".mkv"
                else -> ".mp4"  
            }

            val outputFile = File(context.cacheDir, "video_${System.currentTimeMillis()}$extension")


            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesRead: Int
                    var totalBytesRead = 0L


                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead


                        if (progressCallback != null && fileSize > 0) {
                            val progress = totalBytesRead.toFloat() / fileSize.toFloat()
                            progressCallback(progress)
                        }
                    }
                }
            } ?: run {
                Log.e("FileUtils", "Could not open input stream for URI: $uri")
                return null
            }

            Log.d("FileUtils", "File successfully copied from URI: ${outputFile.absolutePath}, size: ${outputFile.length()}")
            return outputFile

        } catch (e: IOException) {
            Log.e("FileUtils", "Error copying file from URI: ${e.message}")
            e.printStackTrace()
            return null
        } catch (e: Exception) {
            Log.e("FileUtils", "Unexpected error: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
}

@Composable
fun TipItem(tip: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            tip,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


@Composable
fun VideoSubmissionsTab(
    details: ExerciseDetails,
    navController: NavController
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        val activePlanInstance = details.planInstances.find { it.planStatus == "Active" }

        if (activePlanInstance != null) {

            val exercise = Exercise(
                exerciseId = details.exerciseId,
                planExerciseId = activePlanInstance.planExerciseId,
                name = details.name,
                description = details.description,
                videoUrl = details.videoUrl,
                imageUrl = null,
                videoType = details.videoType,
                sets = activePlanInstance.sets,
                repetitions = activePlanInstance.repetitions,
                frequency = activePlanInstance.frequency,
                duration = details.duration,
                completed = activePlanInstance.completed,
                thumbnailUrl = details.thumbnailUrl
            )

            VideoSubmissionsSection(exercise, navController)
        } else {

            Text(
                "You need an active treatment plan to submit videos for this exercise.",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    navController.navigate("activity")
                }
            ) {
                Text("View Treatment Plans")
            }
        }
    }
}

@Composable
fun VideoSubmissionsSection(
    exercise: Exercise,
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = retrofitClient.instance

    var videoSubmissions by remember { mutableStateOf<List<VideoSubmission>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(key1 = exercise.exerciseId) {
        try {
            isLoading = true
            val submissions = withContext(Dispatchers.IO) {
                apiService.getUserVideoSubmissions()
            }

            videoSubmissions = submissions.filter { it.exercise_id == exercise.exerciseId }
            Log.d("VideoSubmissions", "Loaded ${videoSubmissions.size} submissions for exercise ${exercise.exerciseId}")
            isLoading = false
        } catch (e: Exception) {
            Log.e("VideoSubmissions", "Error loading submissions: ${e.message}")
            e.printStackTrace()
            errorMessage = "Failed to load video submissions: ${e.message}"
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            "Your Video Submissions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Text(
                    errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            videoSubmissions.isEmpty() -> {
                Text(
                    "You haven't submitted any videos for this exercise yet. Record yourself performing this exercise for your therapist to review and provide feedback.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(videoSubmissions) { submission ->
                        VideoSubmissionItem(
                            submission = submission,
                            onClick = {
                                navController.navigate("video_submission_details/${submission.submission_id}")
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))


        Button(
            onClick = {
                navController.navigate("upload_exercise_video/${exercise.exerciseId}/${exercise.planExerciseId}")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Create,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Submit New Video")
        }
    }
}

@Composable
fun VideoSubmissionItem(
    submission: VideoSubmission,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))


            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Submitted on ${formatSubmissionDate(submission.submission_date)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))


                val (statusColor, statusText) = when (submission.status) {
                    "Reviewed", "Feedback Provided" ->
                        MaterialTheme.colorScheme.tertiary to "Feedback Available"
                    "Pending" ->
                        MaterialTheme.colorScheme.primary to "Pending Review"
                    else ->
                        MaterialTheme.colorScheme.secondary to submission.status
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }


            Icon(
                imageVector = if (submission.has_feedback)
                    Icons.Default.Check
                else
                    Icons.Default.ArrowForward,
                contentDescription = null,
                tint = if (submission.has_feedback)
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSubmissionDetailsScreen(
    submissionId: Int,
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = retrofitClient.instance

    var submission by remember { mutableStateOf<VideoSubmissionDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(key1 = submissionId) {
        try {
            isLoading = true
            val details = withContext(Dispatchers.IO) {
                apiService.getVideoSubmissionDetails(submissionId)
            }
            submission = details
            Log.d("VideoSubmissionDetails", "Loaded details for submission $submissionId")
            isLoading = false
        } catch (e: Exception) {
            Log.e("VideoSubmissionDetails", "Error loading details: ${e.message}")
            e.printStackTrace()
            errorMessage = "Failed to load submission details: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video Submission") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            errorMessage!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                submission != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .clickable {

                                    navController.navigateToVideo(submission!!.video_url, "mp4")
                                },
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Play video",
                                modifier = Modifier.size(64.dp),
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))


                        Text(
                            "Exercise: ${submission!!.exercise_name}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Treatment Plan: ${submission!!.treatment_plan_name}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            "Submitted on ${formatSubmissionDate(submission!!.submission_date)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))


                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = when (submission!!.status) {
                                    "Pending" -> MaterialTheme.colorScheme.primaryContainer
                                    "Reviewed", "Feedback Provided" -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (submission!!.status) {
                                            "Pending" -> Icons.Default.FavoriteBorder
                                            "Reviewed", "Feedback Provided" -> Icons.Default.CheckCircle
                                            else -> Icons.Default.Info
                                        },
                                        contentDescription = null,
                                        tint = when (submission!!.status) {
                                            "Pending" -> MaterialTheme.colorScheme.primary
                                            "Reviewed", "Feedback Provided" -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        "Status: ${submission!!.status}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (submission!!.notes != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Your Notes:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    submission!!.notes?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontStyle = FontStyle.Italic
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))


                        if (submission!!.therapist_feedback != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            "Therapist Feedback",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (submission!!.feedback_rating != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Rating: ",
                                                style = MaterialTheme.typography.bodyMedium
                                            )

                                            submission!!.feedback_rating?.let {
                                                Text(
                                                    it,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (submission!!.feedback_rating) {
                                                        "Excellent" -> Color(0xFF4CAF50)
                                                        "Good" -> Color(0xFF8BC34A)
                                                        "Needs Improvement" -> Color(0xFFFFC107)
                                                        "Poor" -> Color(0xFFF44336)
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    submission!!.therapist_feedback?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    if (submission!!.feedback_date != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Feedback provided on ${submission!!.feedback_date?.let {
                                                formatSubmissionDate(
                                                    it
                                                )
                                            }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))


                        OutlinedButton(
                            onClick = {
                                navController.navigate("upload_exercise_video/${submission!!.exercise_id}/${submission!!.treatment_plan_id}")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload another Video")
                        }

                        Spacer(modifier = Modifier.height(8.dp))


                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            apiService.deleteVideoSubmission(submissionId)
                                        }
                                        if (result.status == "success") {
                                            Toast.makeText(
                                                context,
                                                "Video submission deleted successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            navController.popBackStack()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to delete: ${result.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("VideoSubmission", "Error deleting submission: ${e.message}")
                                        Toast.makeText(
                                            context,
                                            "Error deleting submission: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Submission")
                        }
                    }
                }
            }
        }
    }
}