package com.pogi.percentronx

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)


val bottomNavItems = listOf(
    BottomNavItem("main", "Home", Icons.Filled.Home),
    BottomNavItem("dashboard", "Dashboard", Icons.Filled.Settings),
    BottomNavItem("activity", "Activity", Icons.Filled.List),
    BottomNavItem("profile", "Profile", Icons.Filled.Person)
)

@Composable
fun MainScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Home Screen",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Welcome to APR-CV",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Navigate using the bottom bar",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}


@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 5.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Loading APR-CV...",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun LoggedInProfileScreen(onLogoutSuccess: () -> Unit = {}) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var userData by remember { mutableStateOf<User_Data?>(null) }

    LaunchedEffect(Unit) {
        retrofitClient.instance.getUserInfo().enqueue(object : Callback<User_Data> {
            override fun onResponse(call: Call<User_Data>, response: Response<User_Data>) {
                if (response.isSuccessful) {
                    userData = response.body()
                } else {
                    Log.e("API", "Failed to fetch user info: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<User_Data>, t: Throwable) {
                Log.e("API", "Network error: ${t.message}")
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Profile",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(150.dp)
                .padding(8.dp)
                .graphicsLayer {
                    rotationZ = 360f
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                size.minDimension / 2
                val strokeWidth = size.minDimension * 0.05f

                val primaryColor = Color.Green

                drawArc(
                    color = primaryColor,
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
            }

            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = Color.LightGray,
                border = BorderStroke(3.dp, Color.Black)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(64.dp),
                        tint = Color.DarkGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "User Information",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Blue
                    )

                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.Blue
                    )
                }

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                InfoRow(
                    icon = Icons.Default.Person,
                    label = "Username:",
                    value = userData?.username ?: "Loading..."
                )

                Spacer(modifier = Modifier.height(8.dp))

                InfoRow(
                    icon = Icons.Default.Email,
                    label = "Email:",
                    value = userData?.email ?: "Loading..."
                )

                Spacer(modifier = Modifier.height(8.dp))

                InfoRow(
                    icon = Icons.Filled.DateRange,
                    label = "Joined:",
                    value = userData?.joined ?: "Loading..."
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Your Computer Vision Stats",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Blue
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Filled.Face,
                        value = "24",
                        label = "Scans"
                    )

                    StatItem(
                        icon = Icons.Default.CheckCircle,
                        value = "98%",
                        label = "Accuracy"
                    )

                    StatItem(
                        icon = Icons.Filled.Star,
                        value = "Pro",
                        label = "Level"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Logout"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to logout from APR-CV?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        retrofitClient.instance.logout().enqueue(object : Callback<Status> {
                            override fun onResponse(call: Call<Status>, response: Response<Status>) {
                                if (response.isSuccessful) {
                                    Log.d("API", "Logged out successfully")
                                    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                                    prefs.edit().remove("session_cookie").apply()
                                    onLogoutSuccess()
                                } else {
                                    Log.e("API", "Logout failed: ${response.errorBody()?.string()}")
                                }
                            }

                            override fun onFailure(call: Call<Status>, t: Throwable) {
                                Log.e("API", "Logout error: ${t.message}")
                            }
                        })
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showLogoutDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatItem(icon: ImageVector, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun LoginForm(
    onSignUpClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    rememberCoroutineScope()

    val isUsernameValid = username.isNotEmpty()
    val isPasswordValid = password.length >= 6
    val isFormValid = isUsernameValid && isPasswordValid

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            isError = username.isNotEmpty() && !isUsernameValid,
            singleLine = true
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            isError = password.isNotEmpty() && !isPasswordValid,
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it }
                )
                Text("Remember me")
            }

            TextButton(onClick = onForgotPasswordClick) {
                Text("Forgot Password?")
            }
        }

        Button(
            onClick = {
                isLoading = true
                statusMessage = ""

                val loginRequest = Login(
                    username = username,
                    password = password,
                    remember_me = rememberMe
                )

                retrofitClient.instance.loginUser(loginRequest).enqueue(object : Callback<Status> {
                    override fun onResponse(call: Call<Status>, response: Response<Status>) {
                        isLoading = false

                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            if (responseBody?.status == "valid") {
                                val cookies = response.headers().values("Set-Cookie")
                                val sessionCookie = cookies.firstOrNull { it.startsWith("session_id=") }

                                if (sessionCookie != null) {
                                    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                                    prefs.edit().putString("session_cookie", sessionCookie).apply()
                                }

                                Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                statusMessage = "Login failed: Invalid credentials"
                            }
                        } else {
                            try {
                                val errorBody = response.errorBody()?.string()
                                val errorObj = Gson().fromJson(errorBody, ErrorResponse::class.java)
                                statusMessage = errorObj?.detail ?: "Login failed: ${response.code()}"
                            } catch (e: Exception) {
                                statusMessage = "Login failed: ${response.code()}"
                            }
                        }
                    }

                    override fun onFailure(call: Call<Status>, t: Throwable) {
                        isLoading = false
                        statusMessage = "Connection error: ${t.localizedMessage}"
                        Log.e("LoginForm", "API call failed", t)
                    }
                })
            },
            enabled = isFormValid && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Login")
            }
        }

        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSignUpClick) {
            Text("Don't have an account? Sign up")
        }
    }
}


@Composable
fun AuthScreen(onLoginSuccess: () -> Unit = {}) {
    var isLogin by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "")

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(3000, easing = LinearEasing)
                                ), label = ""
                            )

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                rotate(rotation) {
                                    for (i in 0 until 8) {
                                        rotate(i * 45f) {
                                            drawCircle(
                                                color = lerp(primaryColor, secondaryColor, i / 8f),
                                                radius = size.minDimension * 0.09f,
                                                center = Offset(0f, -size.minDimension * 0.38f)
                                            )
                                        }
                                    }
                                }
                            }
                            Surface(
                                modifier = Modifier.size(70.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(
                                    width = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Build,
                                        contentDescription = "APR-CV Logo",
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "APR-CV",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "Computer Vision Platform",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                TabRow(
                    selectedTabIndex = if (isLogin) 1 else 0,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .clip(RoundedCornerShape(50)),
                    indicator = {},
                    divider = {}
                ) {
                    Tab(
                        selected = !isLogin,
                        onClick = { isLogin = false },
                        modifier = Modifier
                            .background(
                                color = if (!isLogin)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(
                                    topStart = 50.dp,
                                    bottomStart = 50.dp,
                                    topEnd = 0.dp,
                                    bottomEnd = 0.dp
                                )
                            )
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Sign Up",
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = if (!isLogin)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Tab(
                        selected = isLogin,
                        onClick = { isLogin = true },
                        modifier = Modifier
                            .background(
                                color = if (isLogin)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(
                                    topStart = 0.dp,
                                    bottomStart = 0.dp,
                                    topEnd = 50.dp,
                                    bottomEnd = 50.dp
                                )
                            )
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            "Login",
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = if (isLogin)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedContent(
                    targetState = isLogin,
                    transitionSpec = {
                        if (targetState) {
                            (slideInHorizontally(initialOffsetX = { it }) + fadeIn()).togetherWith(
                                slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                            )
                        } else {
                            (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()).togetherWith(
                                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                            )
                        }
                    }, label = ""
                ) { isLoginState ->
                    if (isLoginState) {
                        LoginForm(
                            onForgotPasswordClick = { showForgotPasswordDialog = true },
                            onSignUpClick = { isLogin = false },
                            onLoginSuccess = onLoginSuccess
                        )
                    } else {
                        SignUpForm(
                            onLoginClick = { isLogin = true }
                        )
                    }
                }
            }
        }
    }

    ForgotPasswordDialog(
        isVisible = showForgotPasswordDialog,
        onDismiss = { showForgotPasswordDialog = false },
        onPasswordResetRequested = { email ->
            val requestMap = mapOf("email" to email)
            retrofitClient.instance.resetPassword(requestMap).enqueue(object : Callback<Status> {
                override fun onResponse(call: Call<Status>, response: Response<Status>) {
                    Log.d("API", "Password reset email sent")
                }

                override fun onFailure(call: Call<Status>, t: Throwable) {
                    Log.e("API", "Password reset error: ${t.message}")
                }
            })
        }
    )
}

@Composable
fun SignUpForm(
    onLoginClick: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val isUsernameValid = username.length >= 3
    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.length >= 6
    val doPasswordsMatch = password == confirmPassword
    val isFormValid = isUsernameValid && isEmailValid && isPasswordValid && doPasswordsMatch && confirmPassword.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create Account", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            isError = username.isNotEmpty() && !isUsernameValid,
            supportingText = {
                if (username.isNotEmpty() && !isUsernameValid) {
                    Text("Username must be at least 3 characters")
                }
            },
            singleLine = true
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            isError = email.isNotEmpty() && !isEmailValid,
            supportingText = {
                if (email.isNotEmpty() && !isEmailValid) {
                    Text("Please enter a valid email address")
                }
            },
            singleLine = true
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            isError = password.isNotEmpty() && !isPasswordValid,
            supportingText = {
                if (password.isNotEmpty() && !isPasswordValid) {
                    Text("Password must be at least 6 characters")
                }
            },
            singleLine = true
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            isError = confirmPassword.isNotEmpty() && !doPasswordsMatch,
            supportingText = {
                if (confirmPassword.isNotEmpty() && !doPasswordsMatch) {
                    Text("Passwords do not match")
                }
            },
            singleLine = true
        )

        Button(
            onClick = {
                isLoading = true
                statusMessage = ""
                val registerRequest = Register(
                    username = username,
                    email = email,
                    password = password
                )
                retrofitClient.instance.registerUser(registerRequest).enqueue(object : Callback<Status> {
                    override fun onResponse(call: Call<Status>, response: Response<Status>) {
                        isLoading = false  

                        if (response.isSuccessful) {
                            statusMessage = "Registration successful! Please log in."
                            Handler(Looper.getMainLooper()).postDelayed({
                                onLoginClick()
                            }, 2000)
                        } else {
                            try {
                                val errorBody = response.errorBody()?.string()
                                val errorObj = Gson().fromJson(errorBody, ErrorResponse::class.java)
                                statusMessage = errorObj?.detail ?: "Registration failed: ${response.code()}"
                            } catch (e: Exception) {
                                statusMessage = "Registration failed: ${response.code()}"
                            }
                        }
                    }

                    override fun onFailure(call: Call<Status>, t: Throwable) {
                        isLoading = false  
                        statusMessage = "Connection error: ${t.localizedMessage}"
                        Log.e("SignUpForm", "API call failed", t)
                    }
                })
            },
            enabled = isFormValid && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Create Account")
            }
        }

        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                color = if (statusMessage.contains("successful"))
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onLoginClick) {
            Text("Already have an account? Login")
        }
    }
}

@Composable
fun ForgotPasswordDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onPasswordResetRequested: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var isEmailValid by remember { mutableStateOf(false) }

    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    "Reset Password",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        "Enter your email address and we'll send you a link to reset your password.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
                        },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        isError = email.isNotEmpty() && !isEmailValid,
                        supportingText = {
                            if (email.isNotEmpty() && !isEmailValid) {
                                Text("Please enter a valid email address")
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isEmailValid) {
                            onPasswordResetRequested(email)
                            onDismiss()
                        }
                    },
                    enabled = isEmailValid
                ) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    var isLoggedIn by remember { mutableStateOf(false) }

    LaunchedEffect(navBackStackEntry) {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        isLoggedIn = prefs.getString("session_cookie", null) != null
    }

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        listOf(
            BottomNavItem("main", "Home", Icons.Filled.Home),
            BottomNavItem("dashboard", "Dashboard", Icons.Filled.Settings),
            BottomNavItem("activity", "Activity", Icons.Filled.List)
        ).forEach { item ->
            val selected = currentRoute == item.route

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        val profileItem = if (isLoggedIn) {
            BottomNavItem("profile", "Profile", Icons.Filled.Person)
        } else {
            BottomNavItem("profile", "Login", Icons.Filled.Lock)
        }

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = profileItem.icon,
                    contentDescription = profileItem.title
                )
            },
            label = { Text(profileItem.title) },
            selected = currentRoute == "profile",
            onClick = {
                navController.navigate("profile") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
fun Profile(onAuthStateChanged: (String) -> Unit = {}) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var isLoggedIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        isLoggedIn = prefs.getString("session_cookie", null) != null
        isLoading = false
    }

    if (isLoading) {
        LoadingScreen()
    } else if (isLoggedIn) {
        LoggedInProfileScreen(
            onLogoutSuccess = {
                isLoggedIn = false
                onAuthStateChanged("invalid")
            }
        )
    } else {
        AuthScreen(
            onLoginSuccess = {
                isLoggedIn = true
                onAuthStateChanged("valid")
            }
        )
    }
}