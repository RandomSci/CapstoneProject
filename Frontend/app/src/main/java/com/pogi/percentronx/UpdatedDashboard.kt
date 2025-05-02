@file:Suppress("SpellCheckingInspection")

package com.pogi.percentronx

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UpdatedDashboard(
    navController: NavController,
    apiService: ApiService,
    isLoggedIn: Boolean = false
) {
    var hasTherapist by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var therapist by remember { mutableStateOf<Therapist?>(null) }
    var appointments by remember { mutableStateOf<List<Appointments>>(emptyList()) }
    var nextAppointment by remember { mutableStateOf<Appointments?>(null) }
    val errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            coroutineScope.launch {
                try {
                    val therapistResponse = apiService.getUserTherapist()
                    therapist = therapistResponse
                    Log.d("Dashboard", "Fetched therapist: ${therapist?.first_name} ${therapist?.last_name}")
                    Log.d("Dashboard", "Therapist photo URL: ${therapist?.photoUrl}, profile image: ${therapist?.profile_image}")
                    hasTherapist = true

                    val appointmentsResponse = apiService.getUserAppointments()

                    Log.d("Dashboard", "Fetched ${appointmentsResponse.size} appointments")
                    if (appointmentsResponse.isNotEmpty()) {
                        appointmentsResponse.forEachIndexed { index, appt ->
                            Log.d("Dashboard", "Appointment $index: ID=${appt.appointment_id}, Date=${appt.appointment_date}, Status=${appt.status}")
                        }
                    } else {
                        Log.d("Dashboard", "No appointments returned from API")
                    }

                    appointments = appointmentsResponse

                    val upcomingAppointments = appointmentsResponse
                        .filter {
                            val appointmentDate = parseAppointmentDate(it.appointment_date)
                            val today = LocalDate.now()
                            val isUpcoming = appointmentDate != null && (appointmentDate.isAfter(today) || appointmentDate.isEqual(today))
                            Log.d("Dashboard", "Appointment ${it.appointment_id} date: ${it.appointment_date}, isUpcoming: $isUpcoming")
                            isUpcoming
                        }

                    Log.d("Dashboard", "Found ${upcomingAppointments.size} upcoming appointments")

                    nextAppointment = upcomingAppointments.minByOrNull { it.appointment_date }
                    if (nextAppointment != null) {
                        Log.d("Dashboard", "Next appointment: ID=${nextAppointment?.appointment_id}, Date=${nextAppointment?.appointment_date}")
                    } else {
                        Log.d("Dashboard", "No next appointment found")
                    }
                } catch (e: Exception) {
                    Log.e("Dashboard", "Error loading data", e)
                    hasTherapist = false
                } finally {
                    isLoading = false
                }
            }
        } else {
            isLoading = false
        }
    }

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
                    text = "Please log in or sign up to view your dashboard",
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
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }
                    ) {
                        Text("Log In")
                    }

                    OutlinedButton(
                        onClick = {
                            navController.navigate("profile") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }
                    ) {
                        Text("Sign Up")
                    }
                }
            }
        }
    } else if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            errorMessage?.let {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                "Dashboard",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasTherapist)
                        MaterialTheme.colorScheme.surfaceVariant
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (hasTherapist && therapist != null) {
                        Text(
                            text = "Your Therapist",
                            style = MaterialTheme.typography.titleLarge,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            val context = LocalContext.current
                            val photoUrl = therapist?.photoUrl
                            val profileImage = therapist?.profile_image
                            val therapistId = therapist?.id ?: 0

                            val imageUrl = when {
                                !photoUrl.isNullOrEmpty() -> retrofitClient.getFullImageUrl(photoUrl)

                                !profileImage.isNullOrEmpty() -> {
                                    val constructedUrl = "/static/assets/images/user/$profileImage"
                                    retrofitClient.getFullImageUrl(constructedUrl)
                                }

                                therapistId > 0 -> {
                                    val constructedUrl = "/static/assets/images/user/therapist_$therapistId.jpg"
                                    retrofitClient.getFullImageUrl(constructedUrl)
                                }

                                else -> null
                            }

                            if (imageUrl != null) {
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
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Therapist Photo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Dr. ${therapist?.first_name} ${therapist?.last_name}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Specialty: ${therapist?.specialties?.joinToString(", ") ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Next Appointment:",
                            style = MaterialTheme.typography.labelLarge
                        )

                        Text(
                            text = if (nextAppointment != null) {
                                "${formatDate(nextAppointment?.appointment_date ?: "")}, ${nextAppointment?.appointment_time}"
                            } else {
                                "No upcoming appointments"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    navController.navigate("therapist_details/${therapist?.id}")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text("VIEW DETAILS")
                            }

                            Button(
                                onClick = {
                                    navController.navigate("therapist_chat/${therapist?.id}")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                Text("CHAT")
                            }
                        }
                    } else {
                        Text(
                            text = "Find a Therapist",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Connect with mental health professionals to support your wellness journey",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                navController.navigate("therapist_finder")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Find Therapist Icon",
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                "FIND A THERAPIST",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
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
                        text = "My Appointments",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (hasTherapist && appointments.isNotEmpty()) {
                        val upcomingAppointments = appointments
                            .filter {
                                val appointmentDate = parseAppointmentDate(it.appointment_date)
                                val today = LocalDate.now()
                                appointmentDate != null && (appointmentDate.isAfter(today) || appointmentDate.isEqual(today))
                            }
                            .sortedBy { it.appointment_date }
                            .take(2)

                        if (upcomingAppointments.isNotEmpty()) {
                            upcomingAppointments.forEach { appointment ->
                                AppointmentListItem(
                                    therapistName = "Dr. ${therapist?.first_name} ${therapist?.last_name}",
                                    date = formatDate(appointment.appointment_date),
                                    time = appointment.appointment_time,
                                    status = appointment.status
                                )

                                if (upcomingAppointments.size > 1 && appointment != upcomingAppointments.last()) {
                                    Divider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "No Appointments",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "No upcoming appointments",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "No Appointments",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "No appointments scheduled",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (hasTherapist) {
                                navController.navigate("request_appointment/${therapist?.id}")
                            } else {
                                navController.navigate("therapist_finder")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Schedule Appointment"
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            if (hasTherapist) "SCHEDULE APPOINTMENT" else "FIND A THERAPIST"
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDate(isoDate: String): String {
    return try {
        val date = try {
            LocalDate.parse(isoDate)
        } catch (e: Exception) {
            try {
                val formatter = DateTimeFormatter.ISO_DATE
                LocalDate.parse(isoDate, formatter)
            } catch (e2: Exception) {
                try {
                    if (isoDate.contains("T")) {
                        val dateTime = java.time.LocalDateTime.parse(isoDate)
                        LocalDate.of(dateTime.year, dateTime.month, dateTime.dayOfMonth)
                    } else {
                        val dateTimeParts = isoDate.split(" ")
                        if (dateTimeParts.size > 1) {
                            val dateStr = dateTimeParts[0]
                            try {
                                LocalDate.parse(dateStr)
                            } catch (e3: Exception) {
                                Log.e("Dashboard", "Failed all date parsing attempts for: $isoDate")
                                return isoDate
                            }
                        } else {
                            Log.e("Dashboard", "Unrecognized date format: $isoDate")
                            return isoDate
                        }
                    }
                } catch (e3: Exception) {
                    Log.e("Dashboard", "Failed to parse complex date: $isoDate", e3)
                    return isoDate
                }
            }
        }

        date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
    } catch (e: Exception) {
        Log.e("Dashboard", "Date formatting error: ${e.message}", e)
        isoDate
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun parseAppointmentDate(dateString: String): LocalDate? {
    return try {
        try {
            LocalDate.parse(dateString)
        } catch (e: Exception) {
            try {
                if (dateString.contains("T")) {
                    val dateTime = java.time.LocalDateTime.parse(dateString)
                    LocalDate.of(dateTime.year, dateTime.month, dateTime.dayOfMonth)
                } else {
                    val datePart = dateString.split(" ")[0]
                    LocalDate.parse(datePart)
                }
            } catch (e2: Exception) {
                Log.e("Dashboard", "Failed to parse date: $dateString", e2)
                null
            }
        }
    } catch (e: Exception) {
        Log.e("Dashboard", "Date parsing error: ${e.message}", e)
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCardItem(
    title: String,
    backgroundColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .height(100.dp)
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentListItem(
    therapistName: String,
    date: String,
    time: String,
    status: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = "Appointment",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = therapistName,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "$date at $time",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        FilterChip(
            onClick = { },
            label = { Text(status) },
            selected = false,
            colors = FilterChipDefaults.filterChipColors(
                containerColor = when (status.lowercase()) {
                    "confirmed", "completed", "attended" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    "pending", "scheduled" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                },
                labelColor = when (status.lowercase()) {
                    "confirmed", "completed", "attended" -> MaterialTheme.colorScheme.primary
                    "pending", "scheduled" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
            )
        )
    }
}

@Composable
fun ProgressIndicator(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}