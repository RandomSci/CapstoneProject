@file:Suppress("UNUSED_PARAMETER")

package com.pogi.percentronx

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@SuppressLint("DefaultLocale")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestAppointmentScreen(
    navController: NavController,
    therapistId: Int,
    onAppointmentBooked: () -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(false) }
    var availableDates by remember { mutableStateOf<List<String>>(emptyList()) }
    var availableTimes by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var appointmentType by remember { mutableStateOf("video") } 
    var notes by remember { mutableStateOf("") }
    var showInsuranceFields by remember { mutableStateOf(false) }
    var insuranceProvider by remember { mutableStateOf("") }
    var insuranceMemberId by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        val dates = mutableListOf<String>()
        val dateFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
        } else {
            null
        }

        for (i in 0 until 7) {
            val date = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDate.now().plusDays(i.toLong()).format(dateFormat)
            } else {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_MONTH, i)
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                String.format("%04d-%02d-%02d", year, month, day)
            }
            dates.add(date)
        }
        availableDates = dates

        availableTimes = listOf(
            "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
            "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM"
        )

        if (dates.isNotEmpty()) {
            selectedDate = dates[0]
        }
    }

    fun submitAppointment() {
        if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
            errorMessage = "Please select a date and time for your appointment."
            return
        }

        isLoading = true
        errorMessage = null
        successMessage = null

        val request = AppointmentRequest(
            therapist_id = therapistId,
            date = selectedDate,
            time = selectedTime,
            type = appointmentType,
            notes = notes.ifEmpty { null },
            insuranceProvider = if (showInsuranceFields && insuranceProvider.isNotEmpty()) insuranceProvider else null,
            insuranceMemberId = if (showInsuranceFields && insuranceMemberId.isNotEmpty()) insuranceMemberId else null
        )

        coroutineScope.launch {
            try {
                val response = retrofitClient.instance.bookAppointment(request)
                if (response.status == "success") {
                    successMessage = "Appointment requested successfully!"

                    onAppointmentBooked()

                    delay(1500)
                    navController.popBackStack()
                } else {
                    errorMessage = "Failed to book appointment: ${response.message}"
                }
            } catch (e: Exception) {
                Log.e("Appointment", "Error booking appointment: ${e.message}")
                errorMessage = "An error occurred while booking your appointment. Please try again."
            } finally {
                isLoading = false
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Appointment") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading) {
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
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Select Date",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableDates) { date ->
                            DateCard(
                                date = date,
                                isSelected = date == selectedDate,
                                onClick = { selectedDate = date }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "Select Time",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableTimes) { time ->
                            TimeCard(
                                time = time,
                                isSelected = time == selectedTime,
                                onClick = { selectedTime = time }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "Appointment Type",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AppointmentTypeButton(
                            type = "video",
                            icon = Icons.Default.Call,
                            label = "Video Call",
                            isSelected = appointmentType == "video",
                            onClick = { appointmentType = "video" }
                        )

                        AppointmentTypeButton(
                            type = "phone",
                            icon = Icons.Default.Call,
                            label = "Phone Call",
                            isSelected = appointmentType == "phone",
                            onClick = { appointmentType = "phone" }
                        )

                        AppointmentTypeButton(
                            type = "in_person",
                            icon = Icons.Default.Person,
                            label = "In Person",
                            isSelected = appointmentType == "in_person",
                            onClick = { appointmentType = "in_person" }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Additional Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = showInsuranceFields,
                            onCheckedChange = { showInsuranceFields = it }
                        )
                        Text("I want to use insurance")
                    }
                    AnimatedVisibility(visible = showInsuranceFields) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = insuranceProvider,
                                onValueChange = { insuranceProvider = it },
                                label = { Text("Insurance Provider") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = insuranceMemberId,
                                onValueChange = { insuranceMemberId = it },
                                label = { Text("Member ID") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    successMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Button(
                        onClick = { submitAppointment() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && selectedDate.isNotEmpty() && selectedTime.isNotEmpty()
                    ) {
                        Text("Request Appointment")
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateCard(date: String, isSelected: Boolean, onClick: () -> Unit) {
    val dateObj = try {
        LocalDate.parse(date)
    } catch (e: Exception) {
        LocalDate.now()
    }

    val dayOfWeek = dateObj.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
    val monthDay = dateObj.format(DateTimeFormatter.ofPattern("MMM d"))

    Card(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayOfWeek,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = monthDay,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TimeCard(time: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AppointmentTypeButton(
    type: String,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}