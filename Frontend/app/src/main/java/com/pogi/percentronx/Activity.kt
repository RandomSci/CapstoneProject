@file:OptIn(ExperimentalMaterial3Api::class)

package com.pogi.percentronx

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Activity(
    isLoggedIn: Boolean = false,
    navController: NavController
) {
    if (!isLoggedIn) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Login Required",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Please log in or sign up to view your activity",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            navController.navigate("profile") {
                                popUpTo("activity") { inclusive = true }
                            }
                        }
                    ) {
                        Text("Log In")
                    }

                    OutlinedButton(
                        onClick = {
                            navController.navigate("profile") {
                                popUpTo("activity") { inclusive = true }
                            }
                        }
                    ) {
                        Text("Sign Up")
                    }
                }
            }
        }
    } else {
        ActivityContent(navController)
    }
}

@Composable
fun ExerciseCompletionDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onConfirm: (ExerciseProgressRequest) -> Unit
) {
    var setsCompleted by remember { mutableStateOf(exercise.sets.toString()) }
    var repsCompleted by remember { mutableStateOf(exercise.repetitions.toString()) }
    var painLevel by remember { mutableStateOf("0") }
    var difficultyLevel by remember { mutableStateOf("0") }
    var durationMinutes by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    val buttonEnabled = setsCompleted.isNotBlank() &&
            setsCompleted.toIntOrNull() != null &&
            repsCompleted.isNotBlank() &&
            repsCompleted.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "Complete Exercise",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    exercise.name,
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
                    "Please provide details about your session:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = setsCompleted,
                    onValueChange = { setsCompleted = it },
                    label = { Text("Sets Completed (Target: ${exercise.sets})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = repsCompleted,
                    onValueChange = { repsCompleted = it },
                    label = { Text("Repetitions Completed (Target: ${exercise.repetitions})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Pain Level (0-10): $painLevel",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = painLevel.toFloatOrNull() ?: 0f,
                    onValueChange = { painLevel = it.toInt().toString() },
                    valueRange = 0f..10f,
                    steps = 9
                )

                Text(
                    text = "Difficulty Level (0-10): $difficultyLevel",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = difficultyLevel.toFloatOrNull() ?: 0f,
                    onValueChange = { difficultyLevel = it.toInt().toString() },
                    valueRange = 0f..10f,
                    steps = 9
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it },
                    label = { Text("Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val progressRequest = ExerciseProgressRequest(
                        sets_completed = setsCompleted.toIntOrNull() ?: exercise.sets,
                        repetitions_completed = repsCompleted.toIntOrNull() ?: exercise.repetitions,
                        duration_seconds = durationMinutes.toIntOrNull()?.let { it * 60 },
                        pain_level = painLevel.toIntOrNull(),
                        difficulty_level = difficultyLevel.toIntOrNull(),
                        notes = notes.ifEmpty { null }
                    )
                    onConfirm(progressRequest)
                },
                enabled = buttonEnabled
            ) {
                Text("Save Progress")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@SuppressLint("RememberReturnType")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ActivityContent(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = retrofitClient.instance

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Treatment Plans", "Exercises", "Progress")
    var treatmentPlansState by remember { mutableStateOf<ResourceState<List<TreatmentPlan>>>(ResourceState.Loading) }
    var progressState by remember { mutableStateOf<ResourceState<UserProgress>>(ResourceState.Loading) }
    var exercisesState by remember { mutableStateOf<ResourceState<List<Exercise>>>(ResourceState.Loading) }
    var refreshTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val refreshAllData = {
        refreshTimestamp = System.currentTimeMillis()
    }

    LaunchedEffect(key1 = selectedTabIndex, key2 = refreshTimestamp) {
        when (selectedTabIndex) {
            0 -> {
                if (treatmentPlansState is ResourceState.Loading || refreshTimestamp > 0) {
                    try {
                        Log.d("Activity", "Loading treatment plans...")
                        val treatmentPlans = withContext(Dispatchers.IO) {
                            apiService.getUserTreatmentPlans()
                        }
                        treatmentPlansState = ResourceState.Success(treatmentPlans)
                        val allExercises = treatmentPlans.flatMap { it.exercises }
                        exercisesState = ResourceState.Success(allExercises)
                    } catch (e: Exception) {
                        Log.e("Activity", "Error loading treatment plans: Find a therapist first")
                        treatmentPlansState = ResourceState.Error("Error loading treatment plans: Find a therapist first")
                    }
                }
            }
            1 -> {
                if (exercisesState is ResourceState.Loading || refreshTimestamp > 0) {
                    try {
                        if (treatmentPlansState !is ResourceState.Success) {
                            val treatmentPlans = withContext(Dispatchers.IO) {
                                apiService.getUserTreatmentPlans()
                            }
                            treatmentPlansState = ResourceState.Success(treatmentPlans)
                            val allExercises = treatmentPlans.flatMap { it.exercises }
                            exercisesState = ResourceState.Success(allExercises)
                        } else {
                            val treatmentPlans = (treatmentPlansState as ResourceState.Success<List<TreatmentPlan>>).data
                            val refreshedPlans = withContext(Dispatchers.IO) {
                                apiService.getUserTreatmentPlans()
                            }
                            treatmentPlansState = ResourceState.Success(refreshedPlans)
                            val allExercises = refreshedPlans.flatMap { it.exercises }
                            exercisesState = ResourceState.Success(allExercises)
                        }
                    } catch (e: Exception) {
                        Log.e("Activity", "Error loading exercises: Find a therapist first")
                        exercisesState = ResourceState.Error("Error loading exercises: Find a therapist first")
                    }
                }
            }
            2 -> {
                if (progressState is ResourceState.Loading || refreshTimestamp > 0) {
                    try {
                        val progressData = withContext(Dispatchers.IO) {
                            apiService.getUserExercisesProgress()
                        }
                        progressState = ResourceState.Success(progressData)
                        if (treatmentPlansState !is ResourceState.Success) {
                            val treatmentPlans = withContext(Dispatchers.IO) {
                                apiService.getUserTreatmentPlans()
                            }
                            treatmentPlansState = ResourceState.Success(treatmentPlans)
                        }
                    } catch (e: Exception) {
                        Log.e("Activity", "Error loading progress data: Find a therapist first")
                        progressState = ResourceState.Error("Error loading progress data: Find a therapist first")
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Activity Center",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { refreshAllData() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {
                        Divider(
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTabIndex == index)
                                        FontWeight.Bold
                                    else
                                        FontWeight.Normal
                                )
                            },
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            icon = {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Default.DateRange  
                                        1 -> Icons.Default.Person     
                                        else -> Icons.Default.CheckCircle  
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        when (selectedTabIndex) {
            0 -> {
                when (val state = treatmentPlansState) {
                    is ResourceState.Loading -> LoadingIndicator()
                    is ResourceState.Success -> TreatmentPlansList(state.data, navController)
                    is ResourceState.Error -> ErrorMessage(
                        message = state.message,
                        onRetry = { refreshAllData() }
                    )
                }
            }
            1 -> {
                when (val state = exercisesState) {
                    is ResourceState.Loading -> LoadingIndicator()
                    is ResourceState.Success -> ExercisesList(
                        exercises = state.data,
                        navController = navController
                    )
                    is ResourceState.Error -> ErrorMessage(
                        message = state.message,
                        onRetry = { refreshAllData() }
                    )
                }
            }
            2 -> {
                when (val state = progressState) {
                    is ResourceState.Loading -> LoadingIndicator()
                    is ResourceState.Success -> ProgressTracker(state.data, treatmentPlansState)
                    is ResourceState.Error -> ErrorMessage(
                        message = state.message,
                        onRetry = { refreshAllData() }
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Unable to load data",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("Retry")
            }
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading data...")
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
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
                text = message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
            }) {
                Text("Retry")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TreatmentPlansList(treatmentPlans: List<TreatmentPlan>, navController: NavController) {
    if (treatmentPlans.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No treatment plans found. Ask your therapist to create a plan for you.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(treatmentPlans) { plan ->
                TreatmentPlanCard(plan, navController)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TreatmentPlanCard(plan: TreatmentPlan, navController: NavController) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (plan.status) {
                "Active" -> MaterialTheme.colorScheme.primaryContainer
                "Completed" -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    plan.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                FilterChip(
                    selected = false,
                    onClick = { },
                    label = { Text(plan.status) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = when (plan.status) {
                            "Active" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            "Completed" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        },
                        labelColor = when (plan.status) {
                            "Active" -> MaterialTheme.colorScheme.primary
                            "Completed" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = plan.progress,
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    "${(plan.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Therapist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    "Dr. ${plan.therapistName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Date",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(4.dp))

                val startDate = plan.startDate?.let { LocalDate.parse(it) }?.format(DateTimeFormatter.ofPattern("MMM d"))
                val endDate = plan.endDate?.let { LocalDate.parse(it) }?.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

                Text(
                    text = if (startDate != null && endDate != null) "$startDate - $endDate" else "Ongoing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (plan.description != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    plan.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${plan.exercises.size} exercises",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Button(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Show Less" else "Show More")
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    plan.exercises.forEach { exercise ->
                        ExerciseItem(exercise, navController)

                        if (exercise != plan.exercises.last()) {
                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseItem(exercise: Exercise, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate("exercise_detail/${exercise.exerciseId}")
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (exercise.thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(retrofitClient.getFullImageUrl(exercise.thumbnailUrl))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Exercise thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (exercise.videoUrl != null) {
                    OutlinedButton(onClick = {
                        val encodedUrl = java.net.URLEncoder.encode(exercise.videoUrl, "UTF-8")
                        navController.navigate("video_player?url=$encodedUrl&type=${exercise.videoType}")
                    }) {
                        Text("Play")
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Exercise",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${exercise.sets} sets × ${exercise.repetitions} reps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    exercise.frequency,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (exercise.completed)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (exercise.completed) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ExercisesList(exercises: List<Exercise>, navController: NavController) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Pending", "Completed")
    val coroutineScope = rememberCoroutineScope()
    val apiService = retrofitClient.instance
    val context = LocalContext.current
    var exercisesList by remember { mutableStateOf(exercises) }
    val refreshExercises = {
        coroutineScope.launch {
            try {
                val treatmentPlans = withContext(Dispatchers.IO) {
                    apiService.getUserTreatmentPlans()
                }
                val allExercises = treatmentPlans.flatMap { it.exercises }
                exercisesList = allExercises
                Log.d("ExercisesList", "Exercises refreshed: ${exercisesList.size} items")
            } catch (e: Exception) {
                Log.e("ExercisesList", "Failed to refresh exercises: ${e.message}")
                Toast.makeText(
                    context,
                    "Failed to refresh exercises: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "My Exercises",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = { refreshExercises() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Exercises",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    leadingIcon = when (filter) {
                        "All" -> null
                        "Pending" -> {
                            {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        "Completed" -> {
                            {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        else -> null
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
        val filteredExercises = when (selectedFilter) {
            "Pending" -> exercisesList.filter { !it.completed }
            "Completed" -> exercisesList.filter { it.completed }
            else -> exercisesList
        }
        if (filteredExercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = when (selectedFilter) {
                            "Pending" -> Icons.Default.Person
                            "Completed" -> Icons.Default.CheckCircle
                            else -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "No ${selectedFilter.lowercase()} exercises found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedFilter == "All") {
                        Text(
                            "Your exercises will appear here once they've been assigned",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    } else {
                        Button(
                            onClick = { selectedFilter = "All" },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("View All Exercises")
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${filteredExercises.size} ${selectedFilter.lowercase()} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredExercises) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        navController = navController,
                        onStatusChange = { _ ->
                            Log.d("ExercisesList", "Status change detected for ${exercise.name}, refreshing list")
                            refreshExercises()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseCard(
    exercise: Exercise,
    navController: NavController,
    onStatusChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isCompleted by remember { mutableStateOf(exercise.completed) }
    var isLoading by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    if (showCompletionDialog) {
        ExerciseCompletionDialog(
            exercise = exercise,
            onDismiss = { showCompletionDialog = false },
            onConfirm = { progressRequest ->
                isLoading = true
                coroutineScope.launch {
                    try {
                        val apiService = retrofitClient.instance
                        val progressResult = apiService.addExerciseProgress(
                            planExerciseId = exercise.planExerciseId,
                            progressRequest = progressRequest
                        )

                        Log.d("Exercise", "Progress logged: ${progressResult.progressId}")
                        val result = apiService.updateExerciseStatus(
                            planExerciseId = exercise.planExerciseId,
                            completed = true
                        )

                        Log.d("Exercise", "Status updated: ${result.status}, message: ${result.message}")
                        isCompleted = true
                        onStatusChange(true)

                        Toast.makeText(
                            context,
                            "Exercise completed and progress saved!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.e("Exercise", "Failed to save progress: ${e.message}")
                        Toast.makeText(
                            context,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        isLoading = false
                        showCompletionDialog = false
                    }
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted)
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                            else
                                MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (exercise.thumbnailUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(retrofitClient.getFullImageUrl(exercise.thumbnailUrl))
                                .crossfade(true)
                                .build(),
                            contentDescription = "Exercise thumbnail",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        if (isCompleted) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    } else {
                        Icon(
                            imageVector = if (isCompleted)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.Person,
                            contentDescription = "Exercise",
                            tint = if (isCompleted) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${exercise.sets} sets × ${exercise.repetitions} reps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                exercise.frequency,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (exercise.videoUrl != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                .padding(4.dp)
                                .clickable {
                                    navController.navigateToVideo(exercise.videoUrl, exercise.videoType)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play video",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            navController.navigate("exercise_detail/${exercise.exerciseId}")
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            "Details",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Button(
                        onClick = { expanded = !expanded },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            if (expanded) "Hide" else "View",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (exercise.description.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                exercise.description,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (exercise.videoUrl != null) {
                        if (exercise.videoType.equals("youtube", ignoreCase = true)) {
                            YouTubeThumbnailWithPlayButton(
                                videoUrl = exercise.videoUrl,
                                onClick = {
                                    navController.navigateToVideo(exercise.videoUrl, exercise.videoType)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                exercise.thumbnailUrl?.let {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(retrofitClient.getFullImageUrl(it))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Video thumbnail",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                        .clickable {
                                            navController.navigateToVideo(exercise.videoUrl, exercise.videoType)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                if (!isCompleted) {
                                    showCompletionDialog = true
                                } else {
                                    isLoading = true
                                    coroutineScope.launch {
                                        try {
                                            val apiService = retrofitClient.instance
                                            val result = apiService.updateExerciseStatus(
                                                planExerciseId = exercise.planExerciseId,
                                                completed = false
                                            )
                                            Log.d("Exercise", "Status updated: ${result.status}, message: ${result.message}")
                                            isCompleted = false
                                            onStatusChange(false)

                                            Toast.makeText(
                                                context,
                                                "Exercise marked as pending",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } catch (e: Exception) {
                                            Log.e("Exercise", "Failed to update status: ${e.message}")
                                            Toast.makeText(
                                                context,
                                                "Error: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCompleted)
                                    MaterialTheme.colorScheme.errorContainer
                                else
                                    MaterialTheme.colorScheme.primary
                            ),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (isCompleted)
                                        Icons.Default.Close
                                    else
                                        Icons.Default.CheckCircle,
                                    contentDescription = if (isCompleted)
                                        "Mark as pending"
                                    else
                                        "Mark as completed"
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    if (isCompleted)
                                        "Mark as Pending"
                                    else
                                        "Mark as Completed",
                                    color = if (isCompleted)
                                        MaterialTheme.colorScheme.onErrorContainer
                                    else
                                        MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        if (exercise.videoUrl != null) {
                            OutlinedButton(
                                onClick = {
                                    navController.navigateToVideo(exercise.videoUrl, exercise.videoType)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play")
                            }
                        }
                    }
                }
            }
            if (isCompleted) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = 1f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ExerciseDetailScreen(
    exerciseId: Int,
    navController: NavController
) {
    val coroutineScope = rememberCoroutineScope()
    val apiService = retrofitClient.instance
    val context = LocalContext.current
    var exerciseDetailsState by remember { mutableStateOf<ResourceState<ExerciseDetails>>(ResourceState.Loading) }
    var exerciseHistoryState by remember { mutableStateOf<ResourceState<ExerciseHistory>>(ResourceState.Loading) }
    LaunchedEffect(key1 = exerciseId) {
        coroutineScope.launch {
            try {
                val details = withContext(Dispatchers.IO) {
                    apiService.getExerciseDetails(exerciseId)
                }
                exerciseDetailsState = ResourceState.Success(details)
                val history = withContext(Dispatchers.IO) {
                    apiService.getExerciseHistory(exerciseId)
                }
                exerciseHistoryState = ResourceState.Success(history)

            } catch (e: Exception) {
                Log.e("ExerciseDetail", "Error loading data: ${e.message}")
                exerciseDetailsState = ResourceState.Error("Error loading exercise details: ${e.message}")
                exerciseHistoryState = ResourceState.Error("Error loading exercise history: ${e.message}")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (val state = exerciseDetailsState) {
            is ResourceState.Loading -> {
                LoadingIndicator()
            }
            is ResourceState.Success -> {
                val details = state.data
                val historyData = when (val historyState = exerciseHistoryState) {
                    is ResourceState.Success -> historyState.data
                    else -> null
                }

                ExerciseDetailContent(
                    details = details,
                    history = historyData,
                    onCompleteExercise = { planExerciseId, isCompleted ->
                        coroutineScope.launch {
                            try {
                                apiService.updateExerciseStatus(
                                    planExerciseId = planExerciseId,
                                    completed = isCompleted
                                )
                                val refreshedDetails = withContext(Dispatchers.IO) {
                                    apiService.getExerciseDetails(exerciseId)
                                }
                                exerciseDetailsState = ResourceState.Success(refreshedDetails)

                                val refreshedHistory = withContext(Dispatchers.IO) {
                                    apiService.getExerciseHistory(exerciseId)
                                }
                                exerciseHistoryState = ResourceState.Success(refreshedHistory)
                                Toast.makeText(
                                    context,
                                    if (isCompleted) "Exercise marked as completed" else "Exercise marked as pending",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } catch (e: Exception) {
                                Log.e("ExerciseDetail", "Error updating status: ${e.message}")
                                Toast.makeText(
                                    context,
                                    "Error updating exercise status: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onLogProgress = { planExerciseId, progressRequest ->
                        coroutineScope.launch {
                            try {
                                val result = apiService.addExerciseProgress(
                                    planExerciseId = planExerciseId,
                                    progressRequest = progressRequest
                                )
                                val refreshedHistory = withContext(Dispatchers.IO) {
                                    apiService.getExerciseHistory(exerciseId)
                                }
                                exerciseHistoryState = ResourceState.Success(refreshedHistory)
                                Toast.makeText(
                                    context,
                                    "Progress logged successfully",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } catch (e: Exception) {
                                Log.e("ExerciseDetail", "Error logging progress: ${e.message}")
                                Toast.makeText(
                                    context,
                                    "Error logging progress: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    navController = navController
                )
            }
            is ResourceState.Error -> {
                ErrorMessage(state.message)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ExerciseDetailContent(
    details: ExerciseDetails,
    history: ExerciseHistory?,
    onCompleteExercise: (Int, Boolean) -> Unit,
    onLogProgress: (Int, ExerciseProgressRequest) -> Unit,
    navController: NavController
) {

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Progress", "History", "Videos")
    val context = LocalContext.current


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = details.name,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            FilterChip(
                selected = false,
                onClick = { },
                label = { Text(details.difficulty) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    labelColor = MaterialTheme.colorScheme.secondary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (details.categoryName != null) {
                FilterChip(
                    selected = false,
                    onClick = { },
                    label = { Text(details.categoryName) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        labelColor = MaterialTheme.colorScheme.tertiary
                    )
                )
            }
        }
        if (details.videoUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                when (details.videoType.lowercase()) {
                    "youtube" -> {
                        YouTubeThumbnailWithPlayButton(
                            videoUrl = details.videoUrl,
                            onClick = {
                                val encodedUrl = java.net.URLEncoder.encode(details.videoUrl, "UTF-8")
                                Log.d("ExerciseDetail", "Navigating to video player with URL: $encodedUrl")
                                navController.navigate("video_player?url=$encodedUrl&type=${details.videoType}")
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "mp4", "mkv" -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (details.thumbnailUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(retrofitClient.getFullImageUrl(details.thumbnailUrl))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Video thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .clickable {
                                        val encodedUrl = java.net.URLEncoder.encode(details.videoUrl, "UTF-8")
                                        navController.navigate("video_player?url=$encodedUrl&type=${details.videoType}")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        when (selectedTabIndex) {
            0 -> {
                ExerciseOverviewTab(details, onCompleteExercise)
            }
            1 -> {
                ExerciseProgressTab(details, history, onLogProgress)
            }
            2 -> {
                ExerciseHistoryTab(history)
            }
            3 -> {
                VideoSubmissionsTab(details, navController)
            }
        }
    }
}

@Composable
fun ExerciseOverviewTab(
    details: ExerciseDetails,
    onCompleteExercise: (Int, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Description",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = details.description.ifEmpty { "No description available." },
            style = MaterialTheme.typography.bodyMedium
        )
        if (details.instructions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Instructions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = details.instructions,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Current Plans",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (details.planInstances.isEmpty()) {
            Text(
                text = "This exercise is not in any of your active treatment plans.",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
        } else {
            details.planInstances.forEach { planInstance ->
                PlanInstanceCard(
                    planInstance = planInstance,
                    onCompleteExercise = onCompleteExercise
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun PlanInstanceCard(
    planInstance: PlanExerciseInstance,
    onCompleteExercise: (Int, Boolean) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(planInstance.completed) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = retrofitClient.instance
    if (showCompletionDialog) {
        val tempExercise = Exercise(
            exerciseId = 0, 
            planExerciseId = planInstance.planExerciseId,
            name = "Exercise in ${planInstance.planName}",
            description = "",
            videoUrl = null,
            imageUrl = null,
            videoType = "",
            sets = planInstance.sets,
            repetitions = planInstance.repetitions,
            frequency = planInstance.frequency,
            duration = planInstance.duration,
            completed = planInstance.completed,
            thumbnailUrl = null
        )

        ExerciseCompletionDialog(
            exercise = tempExercise,
            onDismiss = { showCompletionDialog = false },
            onConfirm = { progressRequest ->
                isLoading = true
                coroutineScope.launch {
                    try {
                        val progressResult = apiService.addExerciseProgress(
                            planExerciseId = planInstance.planExerciseId,
                            progressRequest = progressRequest
                        )

                        Log.d("Exercise", "Progress logged: ${progressResult.progressId}")
                        val result = apiService.updateExerciseStatus(
                            planExerciseId = planInstance.planExerciseId,
                            completed = true
                        )

                        Log.d("Exercise", "Status updated: ${result.status}")
                        isCompleted = true
                        onCompleteExercise(planInstance.planExerciseId, true)

                        Toast.makeText(
                            context,
                            "Exercise completed and progress saved!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.e("Exercise", "Failed to save progress: ${e.message}")
                        Toast.makeText(
                            context,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        isLoading = false
                        showCompletionDialog = false
                    }
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = planInstance.planName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Status: ${planInstance.planStatus}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${planInstance.sets} sets × ${planInstance.repetitions} reps",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = planInstance.frequency,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        if (!isCompleted) {
                            showCompletionDialog = true
                        } else {
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val result = apiService.updateExerciseStatus(
                                        planExerciseId = planInstance.planExerciseId,
                                        completed = false
                                    )
                                    Log.d("Exercise", "Status updated to pending: ${result.status}")
                                    isCompleted = false
                                    onCompleteExercise(planInstance.planExerciseId, false)

                                    Toast.makeText(
                                        context,
                                        "Exercise marked as pending",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    Log.e("Exercise", "Failed to update status: ${e.message}")
                                    Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (isCompleted) "Mark as Pending" else "Mark as Completed"
                        )
                    }
                }
            }
            if (!planInstance.notes.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = planInstance.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ExerciseProgressTab(
    details: ExerciseDetails,
    history: ExerciseHistory?,
    onLogProgress: (Int, ExerciseProgressRequest) -> Unit
) {
    var selectedPlanExerciseId by remember { mutableStateOf<Int?>(null) }
    val activePlanInstances = details.planInstances.filter { it.planStatus == "Active" }
    LaunchedEffect(activePlanInstances) {
        if (selectedPlanExerciseId == null && activePlanInstances.isNotEmpty()) {
            selectedPlanExerciseId = activePlanInstances.first().planExerciseId
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Log detailed progress for your exercise session.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))
        if (activePlanInstances.size > 1) {
            Text(
                text = "Select Treatment Plan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activePlanInstances.forEach { planInstance ->
                    FilterChip(
                        selected = selectedPlanExerciseId == planInstance.planExerciseId,
                        onClick = { selectedPlanExerciseId = planInstance.planExerciseId },
                        label = { Text(planInstance.planName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
        selectedPlanExerciseId?.let { planExerciseId ->
            val selectedPlan = details.planInstances.find { it.planExerciseId == planExerciseId }

            if (selectedPlan != null) {
                ExerciseProgressForm(
                    planInstance = selectedPlan,
                    onSubmit = { progressRequest ->
                        onLogProgress(planExerciseId, progressRequest)
                    }
                )
            }
        } ?: run {
            if (activePlanInstances.isEmpty()) {
                Text(
                    text = "This exercise is not in any of your active treatment plans.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Progress Summary",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (history != null) {
            ProgressSummaryCard(history.stats)
        } else {
            Text(
                text = "No progress data available yet.",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
fun ExerciseProgressForm(
    planInstance: PlanExerciseInstance,
    onSubmit: (ExerciseProgressRequest) -> Unit
) {
    var setsCompleted by remember { mutableStateOf(planInstance.sets.toString()) }
    var repsCompleted by remember { mutableStateOf(planInstance.repetitions.toString()) }
    var painLevel by remember { mutableStateOf("0") }
    var difficultyLevel by remember { mutableStateOf("0") }
    var durationMinutes by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    val buttonEnabled = setsCompleted.isNotBlank() &&
            setsCompleted.toIntOrNull() != null &&
            repsCompleted.isNotBlank() &&
            repsCompleted.toIntOrNull() != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Log Exercise Session",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = setsCompleted,
                onValueChange = { setsCompleted = it },
                label = { Text("Sets Completed (Target: ${planInstance.sets})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = repsCompleted,
                onValueChange = { repsCompleted = it },
                label = { Text("Repetitions Completed (Target: ${planInstance.repetitions})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pain Level (0-10): $painLevel",
                style = MaterialTheme.typography.bodyMedium
            )

            Slider(
                value = painLevel.toFloatOrNull() ?: 0f,
                onValueChange = { painLevel = it.toInt().toString() },
                valueRange = 0f..10f,
                steps = 9
            )
            Text(
                text = "Difficulty Level (0-10): $difficultyLevel",
                style = MaterialTheme.typography.bodyMedium
            )

            Slider(
                value = difficultyLevel.toFloatOrNull() ?: 0f,
                onValueChange = { difficultyLevel = it.toInt().toString() },
                valueRange = 0f..10f,
                steps = 9
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = durationMinutes,
                onValueChange = { durationMinutes = it },
                label = { Text("Duration (minutes)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val progressRequest = ExerciseProgressRequest(
                        sets_completed = setsCompleted.toIntOrNull() ?: 0,
                        repetitions_completed = repsCompleted.toIntOrNull(),
                        duration_seconds = durationMinutes.toIntOrNull()?.let { it * 60 },
                        pain_level = painLevel.toIntOrNull(),
                        difficulty_level = difficultyLevel.toIntOrNull(),
                        notes = notes.ifEmpty { null }
                    )

                    onSubmit(progressRequest)
                    setsCompleted = planInstance.sets.toString()
                    repsCompleted = planInstance.repetitions.toString()
                    painLevel = "0"
                    difficultyLevel = "0"
                    durationMinutes = "0"
                    notes = ""
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = buttonEnabled
            ) {
                Text("Log Progress")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("DefaultLocale")
@Composable
fun ProgressSummaryCard(stats: ExerciseStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Completions",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = stats.totalCompletions.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            stats.averagePain?.let { pain ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Average Pain Level",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = String.format("%.1f/10", pain),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
            stats.averageDifficulty?.let { difficulty ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Average Difficulty",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = String.format("%.1f/10", difficulty),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
            stats.firstCompleted?.let { first ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "First Completed",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = formatDate2(first),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            stats.lastCompleted?.let { last ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Last Completed",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = formatDate2(last),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ExerciseHistoryTab(history: ExerciseHistory?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (history == null || history.progressHistory.isEmpty()) {
            Text(
                text = "No exercise history recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
            return
        }
        Text(
            text = "Exercise History",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))
        history.progressHistory.forEach { entry ->
            ProgressEntryCard(entry)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProgressEntryCard(entry: ProgressEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.completionDate?.let { formatDate2(it) } ?: "Unknown date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Plan: ${entry.planName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sets completed: ${entry.setsCompleted ?: 0}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Reps completed: ${entry.repetitionsCompleted ?: 0}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                entry.painLevel?.let {
                    Text(
                        text = "Pain level: $it/10",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                entry.difficultyLevel?.let {
                    Text(
                        text = "Difficulty: $it/10",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            entry.durationSeconds?.let {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Duration: ${formatDuration(it)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (!entry.notes.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Notes: ${entry.notes}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatDate2(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (e: Exception) {
        dateString
    }
}
fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        "$minutes min ${remainingSeconds}s"
    } else {
        "${remainingSeconds}s"
    }
}

@Composable
fun ProgressTracker(userProgress: UserProgress, treatmentPlansState: ResourceState<List<TreatmentPlan>>) {
    val treatmentPlans = when (treatmentPlansState) {
        is ResourceState.Success -> treatmentPlansState.data
        else -> emptyList()
    }
    val overallProgress = userProgress.completionRate
    val activePlans = treatmentPlans.count { it.status == "Active" }
    val completedPlans = treatmentPlans.count { it.status == "Completed" }
    val totalExercises = treatmentPlans.sumOf { it.exercises.size }
    val completedExercises = userProgress.donutData["Completed"] ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Overall Treatment Progress",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    )
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    )

                    Text(
                        "${(overallProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProgressStat(
                        value = "$activePlans",
                        label = "Active Plans"
                    )

                    ProgressStat(
                        value = "$completedExercises/$totalExercises",
                        label = "Exercises Done"
                    )

                    ProgressStat(
                        value = "$completedPlans",
                        label = "Completed Plans"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Daily Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    for (day in listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")) {
                        val value = userProgress.weeklyStats[day] ?: 0
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(((value.coerceAtLeast(1) * 5).coerceAtMost(80)).dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                day,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                val exercisesDueToday = treatmentPlans.flatMap { plan ->
                    plan.exercises.filter { exercise ->
                        exercise.frequency.contains("daily") && !exercise.completed
                    }
                }

                if (exercisesDueToday.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "All caught up for today!",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                "Great job completing your exercises",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        "Due today: ${exercisesDueToday.size} exercises",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    exercisesDueToday.take(3).forEach { exercise ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    exercise.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    "${exercise.sets} sets × ${exercise.repetitions} reps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                }
                            ) {
                                Text("Do Now")
                            }
                        }

                        if (exercise != exercisesDueToday.take(3).last()) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }

                    if (exercisesDueToday.size > 3) {
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {  },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("See all ${exercisesDueToday.size} exercises")
                        }
                    }
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Weekly Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WeeklyStat(
                        value = "${(userProgress.completionRate * 100).toInt()}%",
                        label = "Completion Rate"
                    )

                    WeeklyStat(
                        value = "5",
                        label = "Streak Days"
                    )

                    WeeklyStat(
                        value = "12",
                        label = "Hours Active"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val completedPct = userProgress.donutData["Completed"]?.toFloat() ?: 0f
                        val partialPct   = userProgress.donutData["Partial"]?.toFloat()   ?: 0f
                        val missedPct    = userProgress.donutData["Missed"]?.toFloat()    ?: 0f
                        val total        = completedPct + partialPct + missedPct

                        val completedAngle = 360f * (completedPct / total)
                        val partialAngle   = 360f * (partialPct   / total)
                        val missedAngle    = 360f * (missedPct    / total)

                        drawArc(
                            color     = Color(0xFF4CAF50),       
                            startAngle= 0f,
                            sweepAngle= completedAngle,
                            useCenter = true,
                            size      = size
                        )
                        drawArc(
                            color     = Color(0xFFFFC107),       
                            startAngle= completedAngle,
                            sweepAngle= partialAngle,
                            useCenter = true,
                            size      = size
                        )
                        drawArc(
                            color     = Color(0xFFF44336),       
                            startAngle= completedAngle + partialAngle,
                            sweepAngle= missedAngle,
                            useCenter = true,
                            size      = size
                        )
                        drawCircle(
                            color  = Color.White,                
                            radius = size.minDimension / 4
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DonutLegendItem(
                        color = MaterialTheme.colorScheme.primary,
                        label = "Completed",
                        value = "${userProgress.donutData["Completed"] ?: 0}"
                    )

                    DonutLegendItem(
                        color = MaterialTheme.colorScheme.tertiary,
                        label = "Partial",
                        value = "${userProgress.donutData["Partial"] ?: 0}"
                    )

                    DonutLegendItem(
                        color = MaterialTheme.colorScheme.error,
                        label = "Missed",
                        value = "${userProgress.donutData["Missed"] ?: 0}"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

            }
        }
    }
}

@Composable
fun DonutLegendItem(color: Color, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProgressStat(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun WeeklyStat(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}