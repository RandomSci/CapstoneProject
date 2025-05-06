package com.pogi.percentronx

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController

@OptIn(UnstableApi::class) @RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ErrorHandlingExerciseDetailScreen(
    exerciseId: Int,
    navController: NavController
) {
    var hasError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf("") }

    val handleError = { e: Exception ->
        Log.e("ExerciseDetail", "Error in ExerciseDetailScreen: ${e.message}", e)
        hasError = true
        errorMessage = e.message ?: "Unknown error occurred"
    }

    if (hasError) {
        ErrorScreen(
            message = errorMessage,
            onRetry = { hasError = false },
            onBack = { navController.popBackStack() },
            onExternal = {
                val intent = Intent(Settings.ACTION_SETTINGS)
                context.startActivity(intent)
            })

    }else {
        LaunchedEffect(key1 = exerciseId) {
            try {
            } catch (e: Exception) {
                handleError(e)
            }
        }

        ExerciseDetailScreen(
            exerciseId = exerciseId,
            navController = navController
        )
    }
}

