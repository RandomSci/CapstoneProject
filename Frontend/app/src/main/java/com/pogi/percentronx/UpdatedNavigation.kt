@file:androidx.annotation.OptIn(UnstableApi::class)
package com.pogi.percentronx

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@androidx.annotation.OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UpdatedNavigationGraph() {
    val navController = rememberNavController()
    var status by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasTherapist by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    val apiService = retrofitClient.instance

    suspend fun checkHasTherapist() {
        try {
            val therapist = try {
                apiService.getUserTherapist()
            } catch (e: Exception) {
                Log.e("API", "Error getting therapist: ${e.message}")
                null
            }

            if (therapist != null) {
                Log.d("API", "User has a therapist directly assigned")
                hasTherapist = true
                return
            }

            try {
                val appointments = apiService.getUserAppointments()
                hasTherapist = appointments.isNotEmpty()
                Log.d("API", "Appointments check: Found ${appointments.size} appointments")
            } catch (e: Exception) {
                Log.e("API", "Error checking appointments: ${e.message}")
                e.printStackTrace()
                hasTherapist = false
            }
        } catch (e: Exception) {
            Log.e("API", "Failed to get therapist info: ${e.message}")
            e.printStackTrace()
            hasTherapist = false
        }
    }

    suspend fun updateUserStatus() {
        try {
            val response = apiService.getStatus()
            status = response.status
            Log.d("API", "Status received: $status")
            if (status == "valid") {
                checkHasTherapist()
            } else {
                hasTherapist = false
            }
        } catch (e: Exception) {
            status = "invalid"
            hasTherapist = false
            Log.e("API", "Authentication failure: ${e.message}")
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(key1 = Unit) {
        Log.d("Navigation", "Initial loading of user status")
        updateUserStatus()
    }

    if (isLoading) {
        LoadingScreen()
    } else {
        Scaffold(
            bottomBar = {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                val showBottomBar = currentRoute in bottomNavItems.map { it.route }

                if (showBottomBar) {
                    BottomNavigationBar(navController)
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = if (status == "valid") "main" else "profile"
                ) {
                    composable(
                        "main",
                        enterTransition = {
                            slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
                        },
                        exitTransition = {
                            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                        }
                    ) {
                        MainScreen(navController = navController)
                    }

                    composable(
                        "dashboard",
                        enterTransition = {
                            slideInHorizontally(initialOffsetX = { it }) + fadeIn()
                        },
                        exitTransition = {
                            slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        }
                    ) {
                        LaunchedEffect(Unit) {
                            if (status == "valid") {
                                Log.d("Navigation", "Refreshing therapist check on dashboard entry")
                                checkHasTherapist()
                            }
                        }

                        UpdatedDashboard(
                            navController = navController,
                            apiService = apiService,
                            isLoggedIn = status == "valid"
                        )
                    }

                    composable(
                        "activity",
                        enterTransition = {
                            slideInHorizontally(initialOffsetX = { it }) + fadeIn()
                        },
                        exitTransition = {
                            slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        }
                    ) {
                        Activity(
                            isLoggedIn = status == "valid",
                            navController = navController
                        )
                    }

                    composable("profile") {
                        Profile(
                            onAuthStateChanged = { newStatus ->
                                status = newStatus
                                if (newStatus == "valid") {
                                    coroutineScope.launch {
                                        checkHasTherapist()
                                    }
                                    navController.navigate("main") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }

                    composable("therapist_finder") {
                        TherapistFinderScreen(navController)
                    }

                    composable(
                        "therapist_details/{therapistId}",
                        arguments = listOf(
                            navArgument("therapistId") { type = NavType.IntType }
                        )
                    ) { backStackEntry ->
                        val therapistId = backStackEntry.arguments?.getInt("therapistId") ?: 0
                        TherapistDetailsScreen(navController, therapistId)
                    }

                    composable(
                        "request_appointment/{therapistId}",
                        arguments = listOf(
                            navArgument("therapistId") { type = NavType.IntType }
                        )
                    ) { backStackEntry ->
                        val therapistId = backStackEntry.arguments?.getInt("therapistId") ?: 0
                        RequestAppointmentScreen(
                            navController = navController,
                            therapistId = therapistId,
                            onAppointmentBooked = {
                                Log.d("Navigation", "Appointment booked callback triggered")
                                coroutineScope.launch {
                                    checkHasTherapist()
                                    navController.navigate("dashboard") {
                                        popUpTo("therapist_finder") {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }

                    composable(
                        "book_appointment/{therapistId}/{slotId}",
                        arguments = listOf(
                            navArgument("therapistId") { type = NavType.IntType },
                            navArgument("slotId") { type = NavType.IntType }
                        )
                    ) { backStackEntry ->
                        val therapistId = backStackEntry.arguments?.getInt("therapistId") ?: 0
                        RequestAppointmentScreen(
                            navController = navController,
                            therapistId = therapistId,
                            onAppointmentBooked = {
                                Log.d("Navigation", "Appointment booked callback triggered")
                                coroutineScope.launch {
                                    checkHasTherapist()
                                    navController.navigate("dashboard") {
                                        popUpTo("therapist_finder") {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }

                    composable(
                        "therapist_chat/{therapistId}",
                        arguments = listOf(
                            navArgument("therapistId") { type = NavType.IntType }
                        )
                    ) { backStackEntry ->
                        val therapistId = backStackEntry.arguments?.getInt("therapistId") ?: 0
                        TherapistChatScreen(
                            navController = navController,
                            therapistId = therapistId,
                            apiService = apiService
                        )
                    }

                    composable(
                        "exercise_detail/{exerciseId}",
                        arguments = listOf(
                            navArgument("exerciseId") { type = NavType.IntType }
                        )
                    ) { backStackEntry ->
                        val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: 0
                        Log.d("Navigation", "Loading exercise detail for ID: $exerciseId")


                        ErrorHandlingExerciseDetailScreen(
                            exerciseId = exerciseId,
                            navController = navController
                        )
                    }


                    addUnifiedVideoPlayerRoute(navController)


                    addVideoSubmissionRoutes(navController)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun NavGraphBuilder.addVideoSubmissionRoutes(navController: NavController) {

    composable(
        route = "upload_exercise_video/{exerciseId}/{planId}",
        arguments = listOf(
            navArgument("exerciseId") { type = NavType.IntType },
            navArgument("planId") { type = NavType.IntType }
        )
    ) { backStackEntry ->
        val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: 0
        val planId = backStackEntry.arguments?.getInt("planId") ?: 0
        VideoUploadScreen(
            exerciseId = exerciseId,
            planId = planId,
            navController = navController
        )
    }


    composable(
        route = "video_submission_details/{submissionId}",
        arguments = listOf(
            navArgument("submissionId") { type = NavType.IntType }
        )
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.getInt("submissionId") ?: 0
        VideoSubmissionDetailsScreen(
            submissionId = submissionId,
            navController = navController
        )
    }


    composable(route = "video_submissions/feedback") {
        AllVideoFeedbackScreen(navController = navController)
    }


    composable(route = "video_submissions/pending") {
        PendingVideoSubmissionsScreen(navController = navController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllVideoFeedbackScreen(navController: NavController) {
    val apiService = retrofitClient.instance
    var submissions by remember { mutableStateOf<List<VideoSubmission>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(key1 = Unit) {
        try {
            isLoading = true
            val allSubmissions = withContext(Dispatchers.IO) {
                apiService.getUserVideoSubmissions()
            }
            submissions = allSubmissions.filter { it.has_feedback }
            isLoading = false
        } catch (e: Exception) {
            Log.e("VideoFeedback", "Error loading submissions: ${e.message}")
            errorMessage = "Failed to load video feedback: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Therapist Feedback") },
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
                    ErrorMessage(
                        message = errorMessage!!,
                        onRetry = {
                            isLoading = true
                            errorMessage = null
                        }
                    )
                }
                submissions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "No Feedback Yet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Your therapist hasn't provided feedback on any of your video submissions yet. Check back later!",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                "Your therapist has provided feedback on ${submissions.size} video submissions.",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        items(submissions) { submission ->
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingVideoSubmissionsScreen(navController: NavController) {
    val apiService = retrofitClient.instance
    var submissions by remember { mutableStateOf<List<VideoSubmission>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(key1 = Unit) {
        try {
            isLoading = true
            val allSubmissions = withContext(Dispatchers.IO) {
                apiService.getUserVideoSubmissions()
            }
            submissions = allSubmissions.filter { it.status == "Pending" }
            isLoading = false
        } catch (e: Exception) {
            Log.e("PendingVideos", "Error loading submissions: ${e.message}")
            errorMessage = "Failed to load pending videos: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Video Reviews") },
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
                    ErrorMessage(
                        message = errorMessage!!,
                        onRetry = {
                            isLoading = true
                            errorMessage = null
                        }
                    )
                }
                submissions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "No Pending Submissions",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "You don't have any video submissions pending review by your therapist.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                "You have ${submissions.size} video submissions awaiting review by your therapist.",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        items(submissions) { submission ->
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
        }
    }
}

fun NavGraphBuilder.addUnifiedVideoPlayerRoute(navController: NavHostController) {
    composable(
        route = "video_player?url={url}&type={type}",
        arguments = listOf(
            navArgument("url") {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument("type") {
                type = NavType.StringType
                defaultValue = "youtube"
            }
        )
    ) { backStackEntry ->
        val url = backStackEntry.arguments?.getString("url") ?: ""
        val type = backStackEntry.arguments?.getString("type") ?: "youtube"

        Log.d("Navigation", "Video player route - URL: $url, Type: $type")

        if (url.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Invalid video URL",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = { navController.popBackStack() }) {
                        Text("Go Back")
                    }
                }
            }
        } else {

            SafeVideoPlayer(
                url = url,
                videoType = type,
                navController = navController
            )
        }
    }
}

fun NavController.navigateToVideo(url: String, type: String = "youtube") {
    if (url.isEmpty()) {
        Log.e("Navigation", "Empty URL provided to navigateToVideo")
        return
    }

    Log.d("Navigation", "Navigating to video: $url of type $type")

    val encodedUrl = if (type.equals("youtube", ignoreCase = true)) {
        if (url.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            Log.d("Navigation", "URL is already a YouTube ID: $url")
            url
        } else {
            val videoId = extractYoutubeVideoId(url)
            if (videoId != null) {
                Log.d("Navigation", "Successfully extracted YouTube ID: $videoId")
                videoId
            } else {
                Log.d("Navigation", "Failed to extract YouTube ID, using full URL")
                java.net.URLEncoder.encode(url, "UTF-8")
            }
        }
    } else {
        java.net.URLEncoder.encode(url, "UTF-8")
    }

    if (encodedUrl.isEmpty()) {
        Log.e("Navigation", "Failed to process URL: $url")
        return
    }

    try {
        Log.d("Navigation", "Navigating to video_player with URL: $encodedUrl, type: $type")
        this.navigate("video_player?url=$encodedUrl&type=$type") {
            launchSingleTop = true
        }
    } catch (e: Exception) {
        Log.e("Navigation", "Error navigating to video: ${e.message}", e)
    }
}