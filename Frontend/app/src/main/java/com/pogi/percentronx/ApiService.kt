@file:Suppress("unused")

package com.pogi.percentronx

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class Login(
    val username: String,
    val password: String,
    val remember_me: Boolean = false
)

data class Register(
    val username: String,
    val email: String,
    val password: String
)

data class Status(
    val status: String,
    val message: String? = null
)

data class ErrorResponse(
    val detail: String
)

data class User_Data(
    val username: String,
    val email: String,
    val joined: String
)

data class TherapistListItem(
    val id: Int,
    val name: String,
    val photoUrl: String,
    val specialties: List<String>,
    val location: String,
    val rating: Float,
    val reviewCount: Int,
    val distance: Float,
    val nextAvailable: String
)

data class AvailableTimeSlot(
    val id: Int,
    val date: String,
    val time: String,
    val isAvailable: Boolean
)

data class AppointmentRequest(
    val therapist_id: Int,
    val date: String,
    val time: String,
    val type: String,
    val notes: String? = null,
    val insuranceProvider: String? = null,
    val insuranceMemberId: String? = null
)

data class patient_profile(
    val patient_id: Int,
    val therapist_id: Int,
    val first_name: String,
    val last_name: String,
    val email: String,
    val phone: String,
    val date_of_birth: String,
    val address: String,
    val diagnosis: String,
    val status: String,
    val notes: String,
    val created_at: String,
    val updated_at: String,
    val user_id: String,
)

data class Therapist(
    val id: Int,
    val first_name: String,
    val last_name: String,
    val company_email: String,
    val profile_image: String,
    val bio: String,
    val experience_years: Int,
    val specialties: List<String>,
    val education: List<String>,
    val languages: List<String>,
    val address: String,
    val rating: Float,
    val review_count: Int,
    val is_accepting_new_patients: Boolean,
    val average_session_length: Int,
    val name: String = "",
    val photoUrl: String = ""
)

data class Appointments(
    val appointment_id: Int,
    val patient_id: Int,
    val therapist_id: Int,
    val appointment_date: String,
    val appointment_time: String,
    val duration: Int,
    val status: String,
    val notes: String,
    val appointmentType: String,
    val additionalNotes: String,
    val insurance: String,
    val memberId: Int,
    val created_at: String,
    val updated_at: String,
)


data class Review(
    val id: Int,
    val patientName: String,
    val rating: Float,
    val comment: String,
    val date: String
)

data class AppointmentResponse(
    val status: String,
    val message: String
)

data class Patient(
    val id: Int,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val profilePicture: String,
    val therapistId: Int
)

data class MessageToTherapistRequest(
    @SerializedName("therapist_id") val therapistId: Int,
    @SerializedName("content") val content: String,
    @SerializedName("subject") val subject: String = "Chat message"
)

data class MessageRequest(
    val recipient_id: Int,
    val recipient_type: String = "therapist",
    val subject: String,
    val content: String
)

data class MessageResponse(
    val id: Int,
    val status: String,
    val message: String? = null
)

data class UserProgress(
    val completionRate: Float,
    val weeklyStats: Map<String, Int>,
    val donutData: Map<String, Int>
)
data class TreatmentPlan(
    val planId: Int,
    val patientId: Int,
    val therapistId: Int,
    val name: String,
    val description: String?,
    val startDate: String?,
    val endDate: String?,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val therapistName: String,
    val progress: Float,
    val exercises: List<Exercise>
)
data class Exercise(
    val exerciseId: Int,
    val planExerciseId: Int,
    val name: String,
    val description: String,
    val videoUrl: String?,
    val imageUrl: String?,
    val videoType: String = "",
    val sets: Int,
    val repetitions: Int,
    val frequency: String,
    val duration: Int?, 
    val completed: Boolean,
    val thumbnailUrl: String?
)

sealed class ResourceState<out T> {
    data object Loading : ResourceState<Nothing>()
    data class Success<T>(val data: T) : ResourceState<T>()
    data class Error(val message: String) : ResourceState<Nothing>()
}

data class ExerciseAnalytics(
    val mostFrequentExercises: List<ExerciseSummary>,
    val leastFrequentExercises: List<ExerciseSummary>,
    val difficultyTrend: List<DifficultyTrendPoint>,
    val painTrend: List<PainTrendPoint>,
    val categoryDistribution: List<CategoryCount>,
    val timeOfDayPreference: List<TimePreference>
)

data class ExerciseSummary(
    val exerciseId: Int,
    val name: String,
    val difficulty: String?,
    val categoryId: Int?,
    val completionCount: Int,
    val lastCompleted: String?
)

data class DifficultyTrendPoint(
    val date: String,
    val averageDifficulty: Float
)

data class PainTrendPoint(
    val date: String,
    val averagePain: Float
)

data class CategoryCount(
    val category: String,
    val count: Int
)

data class TimePreference(
    val timeOfDay: String,
    val count: Int
)

data class ExerciseHistory(
    val exerciseId: Int,
    val name: String,
    val description: String,
    val videoUrl: String?,
    val videoType: String,
    val difficulty: String,
    val stats: ExerciseStats,
    val planInstances: List<PlanInstance>,
    val progressHistory: List<ProgressEntry>
)

data class ExerciseStats(
    val totalCompletions: Int,
    val averagePain: Float?,
    val averageDifficulty: Float?,
    val firstCompleted: String?,
    val lastCompleted: String?
)

data class PlanInstance(
    val planExerciseId: Int,
    val planId: Int,
    val planName: String,
    val sets: Int,
    val repetitions: Int,
    val frequency: String
)

data class ProgressEntry(
    val progressId: Int,
    val planExerciseId: Int,
    val planId: Int,
    val planName: String,
    val completionDate: String?,
    val setsCompleted: Int?,
    val repetitionsCompleted: Int?,
    val durationSeconds: Int?,
    val painLevel: Int?,
    val difficultyLevel: Int?,
    val notes: String?,
    val createdAt: String?
)


data class PlanExerciseInstance(
    val planExerciseId: Int,
    val planId: Int,
    val planName: String,
    val planStatus: String,
    val sets: Int,
    val repetitions: Int,
    val frequency: String,
    val duration: Int?,
    val notes: String?,
    val completed: Boolean,
    val progressHistory: List<ExerciseProgressEntry>
)

data class ExerciseProgressEntry(
    val completionDate: String,
    val setsCompleted: Int?,
    val repetitionsCompleted: Int?,
    val durationSeconds: Int?,
    val painLevel: Int?,
    val difficultyLevel: Int?,
    val notes: String?
)

data class ExerciseProgressRequest(
    val sets_completed: Int,
    val repetitions_completed: Int? = null,
    val duration_seconds: Int? = null,
    val pain_level: Int? = null,
    val difficulty_level: Int? = null,
    val notes: String? = null
)

data class ExerciseProgressResponse(
    val detail: String,
    val progressId: Int
)

data class PlanProgressSummary(
    val planId: Int,
    val planName: String,
    val startDate: String?,
    val endDate: String?,
    val status: String,
    val daysActive: Int,
    val totalExercises: Int,
    val completedExercises: Int,
    val completionRate: Float,
    val dailyActivity: List<DailyActivity>,
    val exerciseProgress: List<ExerciseProgressSummary>
)

data class ExerciseDetails(
    val exerciseId: Int,
    val name: String,
    val description: String,
    val videoUrl: String?,
    val videoType: String,
    val thumbnailUrl: String?,
    val difficulty: String,
    val categoryId: Int?,
    val categoryName: String?,
    val duration: Int?,
    val instructions: String,
    val planInstances: List<PlanExerciseInstance>
)

data class DailyActivity(
    val date: String,
    val exercisesCompleted: Int
)

data class ExerciseProgressSummary(
    val exerciseId: Int,
    val planExerciseId: Int,
    val name: String,
    val targetSets: Int,
    val targetRepetitions: Int,
    val lastCompleted: String?,
    val completionCount: Int,
    val averagePain: Float?,
    val averageDifficulty: Float?,
    val isCompleted: Boolean
)

data class ExerciseVideoSubmissionResponse(
    val submission_id: Int,
    val status: String,
    val message: String
)

data class VideoSubmission(
    val submission_id: Int,
    val exercise_id: Int,
    val exercise_name: String,
    val treatment_plan_id: Int,
    val treatment_plan_name: String,
    val video_url: String,
    val submission_date: String,
    val status: String,

    @SerializedName("has_feedback")
    private val _hasFeedback: Any? = null
) {
    val has_feedback: Boolean
        get() = when (_hasFeedback) {
            is Boolean -> _hasFeedback
            is Number -> _hasFeedback.toInt() != 0
            else -> false
        }
}
data class VideoSubmissionDetails(
    val submission_id: Int,
    val patient_id: Int,
    val exercise_id: Int,
    val exercise_name: String,
    val treatment_plan_id: Int,
    val treatment_plan_name: String,
    val video_url: String,
    val submission_date: String,
    val notes: String?,
    val status: String,
    val therapist_feedback: String?,
    val feedback_rating: String?,
    val feedback_date: String?
)

interface ApiService {

    @Multipart
    @POST("api/exercises/video-submission")
    suspend fun uploadExerciseVideo(
        @Part("exercise_id") exerciseId: RequestBody,
        @Part("treatment_plan_id") treatmentPlanId: RequestBody,
        @Part("notes") notes: RequestBody?,
        @Part video: MultipartBody.Part
    ): ExerciseVideoSubmissionResponse


    @GET("api/user/video-submissions")
    suspend fun getUserVideoSubmissions(): List<VideoSubmission>


    @GET("api/video-submissions/{submissionId}")
    suspend fun getVideoSubmissionDetails(@Path("submissionId") submissionId: Int): VideoSubmissionDetails


    @DELETE("api/video-submissions/{submissionId}")
    suspend fun deleteVideoSubmission(@Path("submissionId") submissionId: Int): Status

    @GET("api/exercises/{exerciseId}")
    suspend fun getExerciseDetails(@Path("exerciseId") exerciseId: Int): ExerciseDetails

    @POST("api/exercises/{planExerciseId}/progress")
    suspend fun addExerciseProgress(
        @Path("planExerciseId") planExerciseId: Int,
        @Body progressRequest: ExerciseProgressRequest
    ): ExerciseProgressResponse

    @GET("api/treatment-plans/{planId}/progress")
    suspend fun getTreatmentPlanProgress(@Path("planId") planId: Int): PlanProgressSummary

    @GET("api/user/exercise-analytics")
    suspend fun getUserExerciseAnalytics(): ExerciseAnalytics

    @GET("api/user/treatment-plans")
    suspend fun getUserTreatmentPlans(): List<TreatmentPlan>

    @GET("api/user/exercises/progress")
    suspend fun getUserExercisesProgress(): UserProgress

    @POST("api/exercises/{planExerciseId}/update-status")
    suspend fun updateExerciseStatus(
        @Path("planExerciseId") planExerciseId: Int,
        @Query("completed") completed: Boolean
    ): Status

    @GET("api/exercises/{exerciseId}/history")
    suspend fun getExerciseHistory(@Path("exerciseId") exerciseId: Int): ExerciseHistory

    @POST("loginUser")
    fun loginUser(@Body loginData: Login): Call<Status>

    @POST("registerUser")
    fun registerUser(@Body registerData: Register): Call<Status>

    @GET("/")
    suspend fun getStatus(): Status

    @POST("logout")
    fun logout(): Call<Status>

    @POST("reset-password")
    fun resetPassword(@Body email: Map<String, String>): Call<Status>

    @GET("getUserInfo")
    fun getUserInfo(): Call<User_Data>

    @GET("therapists")
    suspend fun getTherapists(): List<TherapistListItem>

    @GET("therapists/{id}")
    suspend fun getTherapistDetails(@Path("id") therapistId: Int): Therapist

    @POST("api/book-appointment")
    suspend fun bookAppointment(@Body request: AppointmentRequest): AppointmentResponse

    @GET("therapists/{id}/availability")
    suspend fun getTherapistAvailability(@Path("id") therapistId: Int): List<AvailableTimeSlot>

    @POST("messages/send")
    suspend fun sendMessage(@Body messageRequest: MessageRequest): MessageResponse

    @POST("therapists/{id}/add_patient")
    suspend fun addPatientToTherapist(
        @Path("id") therapistId: Int,
        @Body patient: Map<String, Any>
    ): Status

    @POST("therapists/{id}/rate")
    suspend fun rateTherapist(
        @Path("id") therapistId: Int,
        @Body rating: Map<String, Any>
    ): Status

    @GET("therapists/{id}/availability")
    suspend fun getTherapistAvailability(
        @Path("id") therapistId: Int,
        @Query("date") date: String? = null
    ): List<AvailableTimeSlot>

    @GET("api/user/patient-profile")
    suspend fun getUserPatientProfile(): patient_profile

    @GET("api/user/therapist")
    suspend fun getUserTherapist(): Therapist

    @GET("api/patients/{patient_id}/appointments")
    suspend fun getPatientAppointments(@Path("patient_id") patientId: Int): List<Appointments>

    @GET("api/appointments/{appointment_id}")
    suspend fun getAppointmentDetails(@Path("appointment_id") appointmentId: Int): Appointments

    @GET("api/user/appointments")
    suspend fun getUserAppointments(): List<Appointments>

    @GET("api/user/appointments/next")
    suspend fun getUserNextAppointment(): Appointments?

    @GET("messages/therapist/{therapist_id}")
    suspend fun getTherapistMessages(@Path("therapist_id") therapistId: Int): List<ChatMessage>

    @POST("messages/{message_id}/read")
    suspend fun markMessageAsRead(@Path("message_id") messageId: Int): Status

    @POST("messages/send-to-therapist")
    suspend fun sendMessageToTherapist(@Body request: MessageToTherapistRequest): MessageResponse
}