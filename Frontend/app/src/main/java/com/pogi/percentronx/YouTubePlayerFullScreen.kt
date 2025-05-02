package com.pogi.percentronx

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

fun extractYoutubeVideoId(url: String): String? {
    Log.d("YouTubePlayer", "Extracting video ID from: $url")
    if (url.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) return url
    val patterns = listOf(
        Regex("(?:https?://)?(?:www\\.)?youtu\\.be/([\\w-]{11}).*"),
        Regex("(?:https?://)?(?:www\\.)?youtube\\.com/watch\\?v=([\\w-]{11}).*"),
        Regex("(?:https?://)?(?:www\\.)?youtube\\.com/embed/([\\w-]{11}).*"),
        Regex("(?:https?://)?(?:www\\.)?youtube\\.com/v/([\\w-]{11}).*"),
        Regex("(?:https?://)?(?:www\\.)?youtube\\.com/shorts/([\\w-]{11}).*")
    )
    for (pattern in patterns) {
        val match = pattern.find(url)
        if (match != null && match.groupValues.size > 1) {
            val id = match.groupValues[1]
            if (id.length == 11) return id
        }
    }
    Log.e("YouTubePlayer", "Failed to extract video ID from: $url")
    return null
}

@Composable
fun YouTubePlayerWithLibrary(url: String, onVideoReady: () -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val playerView = remember { YouTubePlayerView(context) }

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(playerView)

        playerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                extractYoutubeVideoId(url)?.let {
                    youTubePlayer.loadVideo(it, 0f)
                    onVideoReady() 
                }
            }
        })

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(playerView)
            playerView.release()
        }
    }

    AndroidView(
        factory = { playerView },
        modifier = modifier
    )
}


@Composable
fun MinimalYouTubePlayer(url: String, navController: NavController) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    val videoId = remember(url) { extractYoutubeVideoId(url) ?: url }
    val alpha by animateFloatAsState(targetValue = if (isLoading) 0f else 1f)

    var isFullscreen by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                .size(40.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable {
                    isFullscreen = !isFullscreen
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Open Fullscreen in YouTube",
                tint = Color.White
            )
        }


        if (isFullscreen) {
            YouTubePlayerWithLibrary(
                url = url,
                onVideoReady = { isLoading = false },
                modifier = Modifier.fillMaxSize() 
            )
        } else {
            YouTubePlayerWithLibrary(
                url = url,
                onVideoReady = { isLoading = false }
            )
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = Color.Red,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        TextButton(
            onClick = {
                try {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/embed/$videoId?autoplay=1&fs=1")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        setPackage(null)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("YouTubePlayer", "Error opening YouTube: ${e.message}")
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
                .border(1.dp, Color.White, RoundedCornerShape(50))
        ) {
            Text("Open in YouTube", color = Color.White)
        }
    }
}

@Composable
fun YouTubeThumbnailWithPlayButton(
    videoUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }


    val videoId = remember(videoUrl) {
        extractYoutubeVideoId(videoUrl)
    }


    val thumbnailUrls = remember(videoId) {
        if (videoId != null) {
            listOf(
                "https://img.youtube.com/vi/$videoId/maxresdefault.jpg", 
                "https://img.youtube.com/vi/$videoId/sddefault.jpg",     
                "https://img.youtube.com/vi/$videoId/hqdefault.jpg",    
                "https://img.youtube.com/vi/$videoId/mqdefault.jpg",     
                "https://img.youtube.com/vi/$videoId/default.jpg"      
            )
        } else {
            emptyList()
        }
    }


    var currentUrlIndex by remember { mutableIntStateOf(0) }
    val currentThumbnailUrl = if (thumbnailUrls.isNotEmpty() && currentUrlIndex < thumbnailUrls.size) {
        thumbnailUrls[currentUrlIndex]
    } else {
        ""
    }


    val tryNextThumbnail = {
        if (currentUrlIndex < thumbnailUrls.size - 1) {
            currentUrlIndex++
            isLoading = true
            loadError = false
        } else {

            loadError = true
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (currentThumbnailUrl.isNotEmpty() && !loadError) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentThumbnailUrl)
                    .crossfade(true)
                    .listener(
                        onError = { _, _ ->
                            Log.d("YouTubeThumbnail", "Failed to load: $currentThumbnailUrl, trying next...")
                            tryNextThumbnail()
                        },
                        onSuccess = { _, _ ->
                            Log.d("YouTubeThumbnail", "Successfully loaded: $currentThumbnailUrl")
                            isLoading = false
                        }
                    )
                    .build(),
                contentDescription = "Video thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = { isLoading = true },
                onSuccess = { isLoading = false },
                onError = { tryNextThumbnail() }
            )
        } else if (loadError || videoId == null) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1F1F1F)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    modifier = Modifier.size(64.dp),
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
        }


        if (isLoading && !loadError) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }


        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.Red)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Video",
                tint = Color.White,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun SafeVideoPlayer(
    url: String,
    videoType: String,
    navController: NavController
) {
    when (videoType.lowercase()) {
        "youtube" -> {
            MinimalYouTubePlayer(url = url, navController = navController)
        }
        else -> {

            val context = LocalContext.current
            val exoPlayer = remember {
                ExoPlayer.Builder(context).build().apply {
                    try {
                        val mediaItem = MediaItem.fromUri(
                            if (url.startsWith("http")) {
                                url
                            } else {
                                retrofitClient.getFullImageUrl(url)
                            }
                        )
                        setMediaItem(mediaItem)
                        prepare()
                        playWhenReady = true
                    } catch (e: Exception) {
                        Log.e("VideoPlayer", "Error preparing player: ${e.message}")
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {

                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            DisposableEffect(Unit) {
                onDispose {
                    exoPlayer.release()
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onExternal: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onRetry) {
                Text("Try Again")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = onExternal) {
                Text("Open in External App")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = onBack) {
                Text("Go Back")
            }
        }
    }
}